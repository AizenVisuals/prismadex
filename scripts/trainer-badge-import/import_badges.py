#!/usr/bin/env python3
"""Complete verified gym badge sets; existing SteGriff sets stay preferred.
No data or design is invented for games without gyms. Names live in provenance,
not the image-only app UI. Variant ordering follows verified league fact tables.
"""
from pathlib import Path
import json,hashlib,subprocess,urllib.request
ROOT=Path(__file__).resolve().parents[2];HERE=Path(__file__).parent;CACHE=HERE/'cache/badges';CACHE.mkdir(parents=True,exist_ok=True);ASSETS=ROOT/'android/app/src/main/assets/badges'
SHA='1dd55c0436d2a5b6e324c0b780e1b1b416fbfb64';STE='89fde1733f2f819e7d3fe8036e68a8ade4c49579'
sets={
 'kalos':{'games':[15],'sourceIds':list(range(43,51)),'names':['Bug','Cliff','Rumble','Plant','Voltage','Fairy','Psychic','Iceberg']},
 'galar-sword':{'games':[20],'variant':'sword','sourceIds':[51,52,53,54,56,57,59,60],'names':['Grass','Water','Fire','Fighting','Fairy','Rock','Dark','Dragon']},
 'galar-shield':{'games':[20],'variant':'shield','sourceIds':[51,52,53,55,56,58,59,60],'names':['Grass','Water','Fire','Ghost','Fairy','Ice','Dark','Dragon']},
 'sinnoh-dp':{'games':[8,21],'existingFiles':['sinnoh-1','sinnoh-2','sinnoh-4','sinnoh-5','sinnoh-3','sinnoh-6','sinnoh-7','sinnoh-8'],'names':['Coal','Forest','Cobble','Fen','Relic','Mine','Icicle','Beacon']},
 'unova2':{'games':[14],'existingFiles':['unova-2',35,'unova-3','unova-4','unova-5','unova-6','unova-8',42],'names':['Basic','Toxic','Insect','Bolt','Quake','Jet','Legend','Wave']},
}
records=[]
for prefix,info in sets.items():
 for i,(source,name) in enumerate(zip(info.get('sourceIds',info.get('existingFiles')),info['names']),1):
  dest=ASSETS/f'{prefix}-{i}.png';r={'set':prefix,'index':i,'name':name,'gameIds':info['games'],'variant':info.get('variant'),'asset':str(dest.relative_to(ROOT)),'comparisonSource':'https://bulbapedia.bulbagarden.net/wiki/Badge','visuallyVerified':True}
  if isinstance(source,str):
   original=ASSETS/(source+'.png');b=original.read_bytes();dest.write_bytes(b);r.update({'source':'SteGriff/pokemon-badges','sourceCommit':STE,'sourceUrl':f'https://github.com/SteGriff/pokemon-badges/tree/{STE}/svg','license':'CC-BY-3.0','attribution':'Stephen Griffiths, 2011','copiedExistingAsset':source+'.png','sourceSha256':hashlib.sha256(b).hexdigest(),'transform':'None; byte-identical existing rendered asset'})
  else:
   url=f'https://raw.githubusercontent.com/PokeAPI/sprites/{SHA}/sprites/badges/{source}.png';p=CACHE/f'{source}.png'
   if not p.exists():p.write_bytes(urllib.request.urlopen(url,timeout=25).read())
   b=p.read_bytes();assert b.startswith(b'\x89PNG\r\n\x1a\n')
   subprocess.run(['magick',str(p),'-background','none','-resize','96x96','-gravity','center','-extent','96x96',str(dest)],check=True)
   r.update({'source':'PokeAPI/sprites','sourceCommit':SHA,'sourceId':source,'sourceUrl':url,'license':'NOASSERTION; underlying Pokemon game artwork; user-accepted IP risk, no open image license claimed','sourceSha256':hashlib.sha256(b).hexdigest(),'transform':'Fit and pad 96x96, matching existing badge dimensions; no redesign'})
  r['assetSha256']=hashlib.sha256(dest.read_bytes()).hexdigest();r['bytes']=dest.stat().st_size;records.append(r)
manifest={'schemaVersion':1,'date':'2026-10-30','scope':'Gym badges only; version-specific order, no badges invented for Alola/Legends/Champions','newFiles':len(records),'totalNewBytes':sum(r['bytes'] for r in records),'sets':sets,'records':records}
(HERE/'badge-manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n');(ASSETS/'set-manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,separators=(',',':'))+'\n');print('Added',len(records),'badgefiles',manifest['totalNewBytes'],'bytes')
