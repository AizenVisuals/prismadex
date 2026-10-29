#!/usr/bin/env python3
"""Apply verified current national base stats only; never rewrite Champions overrides."""
from pathlib import Path
import csv,json
ROOT=Path(__file__).resolve().parents[2]; A=ROOT/'android/app/src/main/assets';HERE=Path(__file__).parent
stats={}
for row in csv.DictReader((HERE/'cache/pokemon_stats.csv').open()):stats.setdefault(int(row['pokemon_id']),{})[int(row['stat_id'])]=int(row['base_stat'])
profiles=json.loads((A/'species-profiles.json').read_text())['profiles'];rows=json.loads((A/'data.json').read_text());changes=[];protected=[]
for row in rows:
 values=stats.get(profiles[str(row['id'])].get('pokeapiPokemonId'),{})
 if not all(k in values for k in range(1,7)):continue
 new=[values[k] for k in range(1,7)]
 if row['stats']==new:continue
 if row['champions']:protected.append(row['id']);continue
 changes.append({'id':row['id'],'name':row['name'],'before':row['stats'],'after':new});row['stats']=new
(A/'data.json').write_text(json.dumps(rows,ensure_ascii=False,separators=(',',':'))+'\n')
(HERE/'national-stat-sync-report.json').write_text(json.dumps({'source':'pinned-pokeapi-pokemon_stats.csv','changes':changes,'protectedChampions':protected},indent=2)+'\n')
print('Corrected:',len(changes),'Protected:',protected)
