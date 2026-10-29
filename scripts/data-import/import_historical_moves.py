#!/usr/bin/env python3
"""Pinned PokeAPI move scalar history. No lore or flavor text is imported."""
import csv,json,hashlib,urllib.request,argparse,re,unicodedata
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]; HERE=Path(__file__).parent/'historical-moves'; ASSETS=ROOT/'android/app/src/main/assets'
COMMIT='2bf639e5572812a67d2b94903fdcb9d095f5aeb2'
FILES=['move_changelog.csv','version_groups.csv','moves.csv','types.csv']
# Explicit internal game -> source identifier. Validated against source table on each run.
GAMES={1:'red-blue',2:'yellow',3:'gold-silver',4:'crystal',5:'ruby-sapphire',6:'emerald',7:'firered-leafgreen',8:'diamond-pearl',9:'platinum',10:'heartgold-soulsilver',11:'black-white',12:'colosseum',13:'xd',14:'black-2-white-2',15:'x-y',16:'omega-ruby-alpha-sapphire',17:'sun-moon',18:'ultra-sun-ultra-moon',19:'lets-go-pikachu-lets-go-eevee',20:'sword-shield',21:'brilliant-diamond-shining-pearl',22:'legends-arceus',23:'scarlet-violet',24:'legends-za'}
def rows(n):return list(csv.DictReader((HERE/n).open()))
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--fetch',action='store_true');args=parser.parse_args();HERE.mkdir(exist_ok=True)
 sources=[]
 for name in FILES:
  url=f'https://raw.githubusercontent.com/PokeAPI/pokeapi/{COMMIT}/data/v2/csv/{name}';path=HERE/name
  if args.fetch or not path.exists():path.write_bytes(urllib.request.urlopen(url,timeout=30).read())
  sources.append({'name':name,'url':url,'sha256':hashlib.sha256(path.read_bytes()).hexdigest()})
 groups={r['identifier']:r for r in rows('version_groups.csv')};orders={int(r['id']):int(r['order']) for r in groups.values()}
 types={r['id']:r['identifier'] for r in rows('types.csv')};raw=json.loads((ASSETS/'moves.json').read_text());details=json.loads((ASSETS/'move-details.json').read_text());changes={}
 for r in rows('move_changelog.csv'):changes.setdefault(r['move_id'],[]).append(r)
 # Showdown generation overrides fill gaps in PokeAPI's changelog (notably recovery PP).
 # Only literal scalar fields are imported; execution callbacks and effect prose are excluded.
 showdown={}; showdown_commit='1242c1e2d9763ba5b85d33f13aecb89bae19ae3d'
 for gen in range(1,9):
  name=f'showdown-gen{gen}-moves.ts';path=HERE/name
  url=f'https://raw.githubusercontent.com/smogon/pokemon-showdown/{showdown_commit}/data/mods/gen{gen}/moves.ts'
  if args.fetch or not path.exists():path.write_bytes(urllib.request.urlopen(url,timeout=30).read())
  sources.append({'name':name,'url':url,'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'license':'MIT'})
  records={}
  for match in re.finditer(r'\n\t([a-z0-9]+): \{(.*?)\n\t\},',path.read_text(),re.S):
   scalars={}
   for prop in ['basePower','accuracy','pp','priority','type','category']:
    value=re.search(r'^\t\t'+prop+r': ([^,\n]+)',match[2],re.M)
    if not value:continue
    literal=value[1].strip()
    if prop in ['type','category']:
     if literal.startswith(("'",'"')):scalars['power' if prop=='basePower' else prop]=literal.strip("\"'").lower()
    elif re.fullmatch(r'-?\d+',literal):scalars['power' if prop=='basePower' else prop]=int(literal)
    elif literal=='true' and prop=='accuracy':scalars[prop]=None
   if scalars:records[match[1]]=scalars
  showdown[gen]=records
 # Internal IDs belong to the packaged catalog, not PokeAPI (they diverge after Z moves).
 def name_key(value):return re.sub('[^a-z0-9]','',unicodedata.normalize('NFKD',value).lower())
 by_name={}
 for internal_id,record in raw.items():by_name.setdefault(name_key(record['name']),[]).append(internal_id)
 mapped={};unmatched=[]
 for row in rows('moves.csv'):
  candidates=by_name.get(name_key(row['identifier']),[])
  if len(candidates)==1:mapped[row['id']]=candidates[0]
  else:unmatched.append({'sourceMoveId':int(row['id']),'identifier':row['identifier'],'reason':'ambiguous' if candidates else 'no exact name match'})
 assert mapped.get('787')==next(k for k,v in raw.items() if v['name']=='Apple Acid')
 games={}
 physical={'normal','fighting','flying','poison','ground','rock','bug','ghost','steel'}
 for game,identifier in GAMES.items():
  group=groups[identifier];order=int(group['order']);generation=int(group['generation_id']);overrides={}
  for r in rows('moves.csv'):
   source_mid=r['id'];mid=mapped.get(source_mid)
   if mid is None:continue
   attrs={k:(int(r[k]) if r[k] else None) for k in ['power','accuracy','pp','priority']};type_id=r['type_id']
   # Earliest future change carrying a value is the old value in effect at target.
   future=sorted([x for x in changes.get(source_mid,[]) if orders[int(x['changed_in_version_group_id'])]>order],key=lambda x:orders[int(x['changed_in_version_group_id'])])
   for field in attrs:
    old=next((x[field] for x in future if x[field]!=''),None)
    if old is not None:attrs[field]=int(old)
   old_type=next((x['type_id'] for x in future if x['type_id']!=''),None)
   if old_type is not None:type_id=old_type
   attrs['type']=types[type_id]
   category={1:'status',2:'physical',3:'special'}[int(r['damage_class_id'])]
   if generation<4 and category!='status':category='physical' if attrs['type'] in physical else 'special'
   attrs['category']=category
   move_key=re.sub('[^a-z0-9]','',raw[mid]['name'].lower())
   for source_gen in range(8,generation-1,-1):attrs.update(showdown[source_gen].get(move_key,{}))
   if generation<4 and attrs['category']!='status':attrs['category']='physical' if attrs['type'] in physical else 'special'
   base=raw[mid]|{'priority':details.get(mid,{}).get('priority',0)}
   diff={k:v for k,v in attrs.items() if base.get(k)!=v}
   if diff:overrides[mid]=diff
  games[str(game)]={'versionGroupId':int(group['id']),'versionGroup':identifier,'generation':generation,'moves':overrides}
 output={'schemaVersion':1,'sourceCommit':COMMIT,'verifiedAt':'2026-10-29','scope':'scalar-move-history; no historical effect text/flags/target validation','games':games}
 (ASSETS/'historical-move-overrides.json').write_text(json.dumps(output,ensure_ascii=False,separators=(',',':'))+'\n')
 manifest={'commit':COMMIT,'license':'BSD-3-Clause; assets/licenses/pokeapi-BSD-3-Clause.txt','sources':sources,'games':{k:len(v['moves']) for k,v in games.items()},'exactNameMappedMoves':len(mapped),'unmatchedSourceMoves':unmatched,'limitations':['Chronological changelog; exceptional title-specific divergences not guaranteed exhaustive','Historical effects, targets and flags remain unvalidated; do not label complete historical battle data','Learnset filtering is separate; this importer does not grant moves to a species or game']}
 (HERE/'sources.json').write_text(json.dumps(manifest,indent=2)+'\n')
 print(json.dumps(manifest['games']))
if __name__=='__main__':main()
