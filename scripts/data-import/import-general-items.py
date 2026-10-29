#!/usr/bin/env python3
"""Rebuild read-only general reference; never alters items.json or Champions selection."""
import csv,hashlib,json,re,urllib.request
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]; SOURCES=Path(__file__).resolve().parent/'item-sources'
POKE='2bf639e5572812a67d2b94903fdcb9d095f5aeb2'; SHOW='1242c1e2d9763ba5b85d33f13aecb89bae19ae3d'
urls={n:f'https://raw.githubusercontent.com/PokeAPI/pokeapi/{POKE}/data/v2/csv/{n}' for n in ['items.csv','item_names.csv','item_categories.csv']}
urls['showdown-item-text.ts']=f'https://raw.githubusercontent.com/smogon/pokemon-showdown/{SHOW}/data/text/items.ts'
SOURCES.mkdir(exist_ok=True)
for name,url in urls.items():
 p=SOURCES/name
 if not p.exists():p.write_bytes(urllib.request.urlopen(url,timeout=30).read())
expected={'items.csv':'f08cd6dc30b447cb91cbe9232c79052e8521f32f6105bb1489b5bfabf4ca3241','item_names.csv':'c4ad0b446e395c378d8ba75d6900f051cc9ebb99431d4000adeac35f492852f0','item_categories.csv':'8bfcaaf1dfe45d33a8cc374a602f2424e1c37ad68bab222f090cd95c691660d7','showdown-item-text.ts':'344f23db9773f17c7bae6bdec848f27134047661d0f991c9a25763d7161d144c'}
for name,digest in expected.items():
 assert hashlib.sha256((SOURCES/name).read_bytes()).hexdigest()==digest, f'Source digest mismatch: {name}'
key=lambda s:re.sub('[^a-z0-9]','',s.lower())
def rows(n):return list(csv.DictReader((SOURCES/n).open()))
names={int(r['item_id']):r['name'] for r in rows('item_names.csv') if r['local_language_id']=='9'}
categories={int(r['id']):r['identifier'] for r in rows('item_categories.csv')}
text=(SOURCES/'showdown-item-text.ts').read_text();descriptions={}
for k,block in re.findall(r'^\t([a-z0-9]+): \{\n(.*?)^\t\},',text,re.M|re.S):
 m=re.search(r'^\t\tshortDesc: ("(?:[^"\\]|\\.)*"),',block,re.M)
 if m:descriptions[k]=json.loads(m.group(1))
existing=json.loads((ROOT/'android/app/src/main/assets/items.json').read_text())
icons={key(v['name']):int(v['id']) for v in existing.values() if v.get('icon')}
items=[]
for row in rows('items.csv'):
 id=int(row['id']);name=names.get(id,row['identifier']);description=descriptions.get(key(name))
 items.append(dict(id=id,name=name,slug=row['identifier'],category=categories[int(row['category_id'])],flingPower=int(row['fling_power']) if row['fling_power'] else None,flingEffectId=int(row['fling_effect_id']) if row['fling_effect_id'] else None,descriptionEnglish=description,iconId=icons.get(key(name))))
translations=json.loads((Path(__file__).parent/'translations-items.json').read_text())
reviewed=json.loads((Path(__file__).parent/'reviewed-items.json').read_text())
translations.update(reviewed)
nums=lambda value:re.findall(r'\d+(?:[.,/]\d+)*(?:%|×)?',value)
for item in items:
 original=item['descriptionEnglish']
 if original:
  text=translations[original]
  assert all(text.get(lang) for lang in ['pt','es']),item['name']
  assert nums(original)==nums(text['pt'])==nums(text['es']),item['name']
  assert all('ZXQ' not in text[lang] and '▁' not in text[lang] for lang in ['pt','es']),item['name']
  item['description']={'pt':text['pt'],'en':original,'es':text['es']}
  item['translationReview']='reviewed-text-or-template' if original in reviewed else 'machine-assisted'
 else:
  item['description']=None
  item['translationReview']=None
assert len(items)==len({r['id'] for r in items})
report={'items':len(items),'described':sum(bool(r['descriptionEnglish']) for r in items),'descriptionMissing':sum(not r['descriptionEnglish'] for r in items),'localizedDescriptions':sum(bool(r['description']) for r in items),'reviewedDescriptionRows':sum(r['translationReview']=='reviewed-text-or-template' for r in items),'uniqueReviewedSummaries':len(reviewed),'machineOnlyRows':sum(r['translationReview']=='machine-assisted' for r in items),'categories':len({r['category'] for r in items}),'icons':sum(r['iconId'] is not None for r in items),'flingPowerKnown':sum(r['flingPower'] is not None for r in items),'pokeapiCommit':POKE,'showdownCommit':SHOW,'sources':{n:{'url':u,'sha256':hashlib.sha256((SOURCES/n).read_bytes()).hexdigest()} for n,u in urls.items()}}
(ROOT/'android/app/src/main/assets/general-items.json').write_text(json.dumps({'schemaVersion':1,'sources':report,'items':items},ensure_ascii=False,separators=(',',':'))+'\n')
(ROOT/'research/full-audit/general-items-report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
print(json.dumps(report,ensure_ascii=False,indent=2))
