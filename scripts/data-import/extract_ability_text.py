#!/usr/bin/env python3
"""Extract only literal top-level text fields; never evaluates JavaScript."""
from pathlib import Path
import json,re,urllib.request,hashlib
HERE=Path(__file__).resolve().parent;CACHE=HERE/'cache';SHA='1242c1e2d9763ba5b85d33f13aecb89bae19ae3d'
url=f'https://raw.githubusercontent.com/smogon/pokemon-showdown/{SHA}/data/text/abilities.ts'
p=CACHE/'showdown-ability-text.ts'
if not p.exists():p.write_bytes(urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'PrismaDex-data-import'}),timeout=30).read())
source=p.read_text();out={}
for m in re.finditer(r'^\t([a-z0-9]+): \{\n(.*?)^\t\},',source,re.M|re.S):
 row={}
 for field in ['name','shortDesc','desc']:
  value=re.search(r'^\t\t'+field+r': ("(?:[^"\\]|\\.)*"),?$',m[2],re.M)
  if value:row[field]=json.loads(value[1])
 out[m[1]]=row
assert len(out)>300 and out['intimidate']['shortDesc']
(CACHE/'showdown-ability-text.json').write_text(json.dumps(out,ensure_ascii=False,indent=2)+'\n')
(HERE/'ability-source.json').write_text(json.dumps({'commit':SHA,'url':url,'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'license':'MIT; bundled Showdown license','extraction':'Static literal top-level names and effect summaries only; no callbacks or source execution','entries':len(out)},indent=2)+'\n')
print('Extracted',len(out),'literal ability entries')
