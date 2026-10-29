#!/usr/bin/env python3
"""Create national-only additional exact-form records without mutating saved IDs."""
from pathlib import Path
import csv,json
ROOT=Path(__file__).resolve().parents[2];HERE=Path(__file__).parent;CACHE=HERE/'cache';ASSETS=ROOT/'android/app/src/main/assets'
def rows(n):return list(csv.DictReader((CACHE/n).open()))
source=rows('pokemon.csv');species={int(r['id']):r for r in rows('pokemon_species.csv')}
names={int(r['pokemon_species_id']):r['name'] for r in rows('pokemon_species_names.csv') if r['local_language_id']=='9'}
abilities={int(r['id']):r['identifier'] for r in rows('abilities.csv')};relation={}
for r in rows('pokemon_abilities.csv'):relation.setdefault(int(r['pokemon_id']),[]).append(r)
types={1:'normal',2:'fighting',3:'flying',4:'poison',5:'ground',6:'rock',7:'bug',8:'ghost',9:'steel',10:'fire',11:'water',12:'grass',13:'electric',14:'psychic',15:'ice',16:'dragon',17:'dark',18:'fairy'};type_rows={};stat_rows={}
for r in rows('pokemon_types.csv'):type_rows.setdefault(int(r['pokemon_id']),[]).append(r)
for r in rows('pokemon_stats.csv'):stat_rows.setdefault(int(r['pokemon_id']),{})[int(r['stat_id'])]=int(r['base_stat'])
existing=json.loads((ASSETS/'data.json').read_text());mapped={r['pokeapiId'] for i,r in json.loads((HERE/'form-id-map.json').read_text()).items() if int(i) in {x['id'] for x in existing}};records=[];rejected=[]
for r in source:
 pid=int(r['id'])
 if pid in mapped:continue
 sid=int(r['species_id']);stats=stat_rows.get(pid,{});t=sorted(type_rows.get(pid,[]),key=lambda x:int(x['slot']))
 if set(stats)!={1,2,3,4,5,6} or not t or any(int(x['type_id']) not in types for x in t):
  rejected.append({'pokeapiId':pid,'canonical':r['identifier'],'reason':'Missing complete exact-form types or six base stats'});continue
 base=names[sid];suffix=r['identifier']
 # Source identifier retains exact form identity; names are display prose only.
 if suffix.startswith(species[sid]['identifier']+'-'):suffix=suffix[len(species[sid]['identifier'])+1:]
 label=base+' ('+suffix.replace('-',' ').title()+')'
 records.append({'id':300000+pid,'pokeapiId':pid,'canonical':r['identifier'],'name':label,'species':sid,'baseId':sid,'types':[types[int(x['type_id'])] for x in t],'stats':[stats[i] for i in range(1,7)],'abilities':[abilities[int(a['ability_id'])] for a in sorted(relation.get(pid,[]),key=lambda a:int(a['slot']))],'height':int(r['height'])/10 if r['height'] else None,'weight':int(r['weight'])/10 if r['weight'] else None,'generation':int(species[sid]['generation_id']),'champions':False,'isMega':'mega' in r['identifier'].split('-'),'games':[],'dataScope':'National reference only; game eligibility not imported'})
assert not ({r['id'] for r in records}&{r['id'] for r in existing})
assert len({r['id'] for r in records})==len(records)
result={'schemaVersion':1,'sourceCommit':'2bf639e5572812a67d2b94903fdcb9d095f5aeb2','idNamespace':'300000 + exact PokeAPI pokemon ID; never replace existing native IDs','scope':'National reference only; eligibility intentionally empty','records':records,'rejected':rejected}
(ASSETS/'additional-form-profiles.json').write_text(json.dumps(result,ensure_ascii=False,separators=(',',':'))+'\n');(HERE/'additional-form-report.json').write_text(json.dumps({'records':len(records),'rejected':rejected,'ids':[x['id'] for x in records]},indent=2)+'\n');print('Additional exact forms',len(records),'Rejected',len(rejected))
