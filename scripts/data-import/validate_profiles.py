#!/usr/bin/env python3
"""Validate relations and provenance without networking or touching user state."""
from pathlib import Path
import json,hashlib,re
ROOT=Path(__file__).resolve().parents[2];HERE=Path(__file__).parent;A=ROOT/'android/app/src/main/assets'
def load(p):return json.loads(p.read_text())
base=load(A/'data.json');extra=load(A/'additional-form-profiles.json')['records'];catalog=base+extra;profiles=load(A/'species-profiles.json')['profiles'];abilities=load(A/'ability-profiles.json')['abilities'];lock=load(HERE/'sources.json')
for source in lock['sources']:assert hashlib.sha256((HERE/'cache'/source['name']).read_bytes()).hexdigest()==source['sha256'],source['name']
assert len(catalog)==len({p['id'] for p in catalog})
assert len(profiles)==len(catalog)
assert {str(p['id']) for p in catalog}==set(profiles)
assert {p['species'] for p in catalog}==set(range(1,1026))
for p in catalog:
 s=profiles[str(p['id'])];assert s['speciesId']==p['species'];assert s['genderRateEighths'] in range(-1,9)
 for r in s['abilities']:assert str(r['id']) in abilities
 for language in ['pt','en','es']:
  assert s['category'][language] and s['growthRate'][language]
 if not s['formResolved']:assert s['heightMeters'] is None and s['weightKg'] is None and s['baseExperience'] is None and not s['abilities']
for p in extra:
 assert p['id']==300000+p['pokeapiId'];assert not p['champions'] and not p['games']
 assert len(p['stats'])==6 and all(v>0 for v in p['stats']);assert 1<=len(p['types'])<=2
 assert p['id'] not in {x['id'] for x in base}
key=lambda x:re.sub('[^a-z0-9]','',x.lower())
lookup={key(n):r for r in abilities.values() for n in [r['name'],r['slug']]}
used={name for p in catalog for name in p['abilities']}
for name in used:
 effect=lookup[key(name)];assert effect['description'];assert not effect['championsSpecific']
 for lang in ['pt','en','es']:assert effect['description'][lang] and 'ZXQ' not in effect['description'][lang]
manifest=load(HERE/'additional-art-manifest.json')
for art in manifest['records']:
 if art['available']:
  assert art['pokeapiId']==art['nativeId']-300000
  assert '/'+str(art['pokeapiId'])+'.png' in art['url']
  assert hashlib.sha256((ROOT/art['asset']).read_bytes()).hexdigest()==art['assetSha256']
 else:assert not (A/'art'/f"{art['nativeId']}.webp").exists()
report={'catalogRecords':len(catalog),'species':1025,'additionalForms':len(extra),'profileForeignKeys':'passed','sourceDigests':'passed','unknownExactProfiles':sum(not p['formResolved'] for p in profiles.values()),'usedAbilityEffects':f'{len(used)}/{len(used)}','exactAdditionalArt':manifest['added'],'neutralAdditionalArt':manifest['missing'],'flavorTextImported':False}
(HERE/'validation-report.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))
