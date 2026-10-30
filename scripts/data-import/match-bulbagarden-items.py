#!/usr/bin/env python3
"""Exact unique-name matching plus eight reviewed game-qualified PokeAPI slug identities."""
import json,re,unicodedata,collections
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def key(value):return re.sub('[^a-z0-9]','',unicodedata.normalize('NFKD',value).encode('ascii','ignore').decode().lower())
PAT=re.compile(r'^File:Bag (.+?)(?: (SV|BDSP|ZA|LA|SWSH|XY|ORAS|HGSS|DP|III|IV|V|VI|VII|VIII|IX|RS|FRLG|RSE|DPPt|SM|USUM|LGPE|LG|RBY|GSC|Champs))? Sprite\.(png|webp|jpg|gif)$',re.I)
FULL=re.compile(r'^File:Bag (.+) Sprite\.(?:png|webp|jpg|gif)$',re.I)
ORDER=['SV','ZA','BDSP','LA',None,'SWSH','ORAS','XY','HGSS','DP','III','IV','V','VI','VII','VIII','IX','RS','FRLG','RSE','DPPt','SM','USUM','LGPE','LG','RBY','GSC','Champs']
# Explicit independent variant IDs in pinned PokeAPI, not a name-substring or numeric join.
REVIEWED_GAME_SLUGS={'ultra-ball':'SV','laultra-ball':'LA','great-ball':'SV','lagreat-ball':'LA','poke-ball':'SV','lapoke-ball':'LA','heavy-ball':'SV','laheavy-ball':'LA'}
def parse_filename(title,known_names):
    m=PAT.match(title);full=FULL.match(title)
    if not m or 'Pokopia' in m[1]:return None
    # A trailing Roman letter can be part of the item's actual name (Bridge Mail V).
    if full and key(full[1]) in known_names:return key(full[1]),None,'full-item-name'
    return key(m[1]),m[2],'game-tag-stripped'
def match_records(items,files):
    counts=collections.Counter(key(i['name']) for i in items);known=set(counts);index=collections.defaultdict(list)
    for file in files:
        parsed=parse_filename(file['title'],known)
        if parsed:
            name,game,method=parsed;index[name].append({'title':file['title'],'gameTag':game,'pageId':file['pageid'],'matchMethod':method})
    matched=[];excluded=[]
    for item in items:
        if item.get('iconId') is not None:continue
        k=key(item['name']);candidates=index.get(k,[]);expected_game=REVIEWED_GAME_SLUGS.get(item['slug'])
        if expected_game:
            candidates=[f for f in candidates if f['gameTag']==expected_game]
            reason='reviewed-game-variant-source-unavailable'
        elif counts[k]>1:
            excluded.append({'id':item['id'],'name':item['name'],'category':item['category'],'reason':'ambiguous-catalog-name'});continue
        else:reason='no-exact-recognized-file-name'
        if candidates:
            candidates=sorted(candidates,key=lambda f:(ORDER.index(f['gameTag']) if f['gameTag'] in ORDER else len(ORDER),f['title']))
            matched.append({'id':item['id'],'name':item['name'],'slug':item['slug'],'source':candidates[0],'alternateSources':candidates[1:],'reviewedGameVariant':expected_game});continue
        excluded.append({'id':item['id'],'name':item['name'],'category':item['category'],'reason':reason})
    return matched,excluded
if __name__=='__main__':
    out=ROOT/'research/item-artwork'
    items=json.loads((ROOT/'android/app/src/main/assets/general-items.json').read_text())['items'];files=json.loads((out/'Bag-sprites-inventory.json').read_text())
    matched,excluded=match_records(items,files)
    (out/'matched.json').write_text(json.dumps(matched,ensure_ascii=False,indent=2));(out/'unmatched.json').write_text(json.dumps(excluded,ensure_ascii=False,indent=2));print(len(matched),'matched',len(excluded),'excluded')
