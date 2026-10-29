#!/usr/bin/env python3
"""Import only verified exact-form images; never substitute the base species."""
from pathlib import Path
import json,urllib.request,urllib.error,hashlib,subprocess,concurrent.futures
ROOT=Path(__file__).resolve().parents[2];HERE=Path(__file__).parent;ASSETS=ROOT/'android/app/src/main/assets';CACHE=HERE/'art-cache';CACHE.mkdir(exist_ok=True)
SHA='1dd55c0436d2a5b6e324c0b780e1b1b416fbfb64'
records=json.loads((ASSETS/'additional-form-profiles.json').read_text())['records']
def one(row):
 pid=row['pokeapiId'];result={'nativeId':row['id'],'pokeapiId':pid,'canonical':row['canonical'],'sourceCommit':SHA}
 for kind,path in [('official-artwork',f'sprites/pokemon/other/official-artwork/{pid}.png'),('sprite',f'sprites/pokemon/{pid}.png')]:
  url=f'https://raw.githubusercontent.com/PokeAPI/sprites/{SHA}/{path}';source=CACHE/f'{pid}-{kind}.png'
  try:
   raw=source.read_bytes() if source.exists() else urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'PrismaDex-data-import'}),timeout=20).read()
   if not raw.startswith(b'\x89PNG\r\n\x1a\n'):continue
   source.write_bytes(raw)
   dimensions=subprocess.check_output(['magick','identify','-format','%w %h',str(source)],text=True).strip()
   dest=ASSETS/'art'/f"{row['id']}.webp";subprocess.run(['magick',str(source),'-resize','256x256>','-strip','-quality','82',str(dest)],check=True,stderr=subprocess.PIPE)
   result.update({'kind':kind,'url':url,'sourceSha256':hashlib.sha256(raw).hexdigest(),'sourceDimensions':dimensions,'asset':str(dest.relative_to(ROOT)),'assetSha256':hashlib.sha256(dest.read_bytes()).hexdigest(),'available':True})
   return result
  except (urllib.error.HTTPError,urllib.error.URLError,TimeoutError,subprocess.CalledProcessError) as e:
   result.setdefault('attempts',[]).append({'url':url,'error':str(e)[:180]})
 result.update({'available':False,'reason':'No exact image at either pinned primary-source path; neutral placeholder required'})
 return result
with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:results=list(pool.map(one,records))
report={'sourceCommit':SHA,'count':len(results),'added':sum(x['available'] for x in results),'missing':sum(not x['available'] for x in results),'records':results}
(HERE/'additional-art-manifest.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print('Added',report['added'],'Missing',report['missing']);print([(x['nativeId'],x['canonical']) for x in results if not x['available']])
