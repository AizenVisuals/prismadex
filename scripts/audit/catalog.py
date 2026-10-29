#!/usr/bin/env python3
"""Offline reproducible coverage/integrity audit; never alters user data."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
A=ROOT/'android/app/src/main/assets'
def read(name):return json.loads((A/name).read_text())
def main():
 rows=read('data.json'); moves=read('moves.json');details=read('move-details.json');identities=read('pokemon-identities.json'); roster=read('champions-roster.json');learnsets=read('champions-learnsets.json')
 ids={p['id'] for p in rows}; errors=[]
 for p in rows:
  if len(p.get('stats',[]))!=6 or not all(isinstance(v,int) and v>0 for v in p.get('stats',[])):errors.append(['invalid-stats',p['id']])
  if not 1<=len(p.get('types',[]))<=2:errors.append(['invalid-types',p['id']])
  if not (A/'art'/f"{p['id']}.webp").is_file():errors.append(['missing-art',p['id']])
 for pid,mid in learnsets.items():
  if int(pid) not in ids:errors.append(['unknown-learnset-pokemon',pid])
  for m in mid:
   if str(m) not in moves:errors.append(['unknown-move',pid,m])
 report={'records':len(rows),'uniqueIds':len(ids),'species':len({p.get('species',p['baseId']) for p in rows}),'fields':{key:{'filled':sum(p.get(key) not in (None,[], '') for p in rows),'total':len(rows)} for key in ['name','types','stats','height','weight','abilities']},'moveDefinitions':len(moves),'moveDescriptions':len(details),'identities':len(identities),'championsLearnsetForms':len(learnsets),'errors':errors,'limits':['Populated fields do not establish factual correctness.','Artwork identity requires independent source/visual verification.','Current game eligibility is not inferred from national presence.']}
 out=ROOT/'research/full-audit/integrity.json';out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps({k:report[k] for k in ['records','species','errors']}))
if __name__=='__main__':main()
