#!/usr/bin/env python3
import json,hashlib,subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];ASSETS=ROOT/'android/app/src/main/assets';WORK=ROOT/'research/item-artwork'
items={r['id']:r for r in json.loads((ASSETS/'general-items.json').read_text())['items']}
manifest=json.loads((ASSETS/'general-item-art-map.json').read_text());rows=manifest['items'];assert len({r['id'] for r in rows})==len(rows)
checked=[]
for row in rows:
 ref=items[row['id']];assert ref['name']==row['name'] and ref['slug']==row['slug'] and ref.get('iconId') is None
 image=ASSETS/row['path'];assert row['path']==f"general-item-art/{row['id']}.webp"
 assert hashlib.sha256(image.read_bytes()).hexdigest()==row['assetSha256']
 original=WORK/'original-images'/f"{row['id']}.png";b=original.read_bytes();assert hashlib.sha1(b).hexdigest()==row['sourceSha1'];assert hashlib.sha256(b).hexdigest()==row['originalSha256']
 checked.append(image)
result=subprocess.check_output(['magick','identify','-format','%w %h\\n']+[str(p) for p in checked],text=True)
# ImageMagick outputs real newlines for the \\n formatting token.
dimensions=[tuple(map(int,pair.split())) for pair in result.replace('\\n','\n').splitlines()]
assert len(dimensions)==len(rows);assert all(1<=w<=128 and 1<=h<=128 for w,h in dimensions)
old=json.loads((WORK/'held-assets-before.json').read_text());current={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in (ASSETS/'item-art').glob('*') if p.is_file()};assert old==current
report={'validatedImages':len(rows),'identityMismatches':0,'originalSha1Mismatches':0,'assetHashMismatches':0,'maxWidth':max(w for w,h in dimensions),'maxHeight':max(h for w,h in dimensions),'existing166HeldAssetsPreserved':True,'source':'Bulbagarden Archives public files; per-file URLs/hashes retained in bundled map'}
(WORK/'validation.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))
