#!/usr/bin/env python3
"""Import the user-authorized public trainer catalog, never edit a trainer PNG.
No account/session data or source engine is involved. Per-artist credits retained.
"""
from pathlib import Path
import re,json,html,hashlib,struct,urllib.request,concurrent.futures,time,subprocess,sys
ROOT=Path(__file__).resolve().parents[2];HERE=Path(__file__).parent;ASSETS=ROOT/'android/app/src/main/assets/trainers';CACHE=HERE/'cache';CACHE.mkdir(exist_ok=True);ASSETS.mkdir(exist_ok=True)
BASE='https://play.pokemonshowdown.com/sprites/trainers/'
index=CACHE/'trainers-index.html'
# Capture a full timestamped source snapshot on every intentional import.
index.write_bytes(subprocess.check_output(['curl','--fail','--silent','--show-error','--location','--max-time','30',BASE]))
s=index.read_text();records=[]
for filename,body in re.findall(r'<figure id="([^"]+\.png)">(.*?)</figure>',s,re.S):
 if not re.fullmatch(r'[a-z0-9_-]+\.png',filename):raise ValueError(('Unsafe source name',filename))
 credit=re.search(r'<br\s*/?>by\s+(.*?)(?:</figcaption>)',body,re.S)
 artist=html.unescape(re.sub('<[^>]+>','',credit[1])).strip() if credit else None
 records.append({'key':filename[:-4],'filename':filename,'artist':artist,'artistCreditSource':BASE,'url':BASE+filename})
assert len(records)==len({r['key'] for r in records}) and len(records)>=1400
start=time.monotonic();oldbytes=sum(p.stat().st_size for p in ASSETS.glob('*.png'))
def one(r):
 p=ASSETS/r['filename'];before=p.read_bytes() if p.exists() else None
 # Existing files are retained byte-for-byte; fetch current source to verify equality.
 last=None
 for attempt in range(3):
  try:
   b=subprocess.check_output(['curl','--fail','--silent','--show-error','--location','--max-time','25',r['url']],stderr=subprocess.PIPE);break
  except Exception as e:
   last=str(e)
   if attempt<2:time.sleep(.3*(attempt+1))
 else:return {**r,'available':False,'error':last}
 if not b.startswith(b'\x89PNG\r\n\x1a\n'):return {**r,'available':False,'error':'Not a PNG'}
 w,h=struct.unpack('>II',b[16:24]);assert w>0 and h>0
 if len(b)>200000:raise ValueError(('Unexpected oversized trainer',r['key'],len(b)))
 # Do not modify image content, palettes, names, dimensions or file encoding.
 p.write_bytes(b)
 return {**r,'available':True,'bytes':len(b),'width':w,'height':h,'sha256':hashlib.sha256(b).hexdigest(),'existingChanged':before is not None and before!=b,'edited':False}
with concurrent.futures.ThreadPoolExecutor(max_workers=4) as ex:
 results=[]
 for i,r in enumerate(ex.map(one,records),1):
  results.append(r)
  if i%100==0:print(i,'/',len(records),round(time.monotonic()-start),flush=True)
manifest={'schemaVersion':1,'sourceUrl':BASE,'retrievedAt':'2026-10-30','sourceIndexSha256':hashlib.sha256(index.read_bytes()).hexdigest(),'sourceNotice':'Many trainer sprites are fan-created. Credit the listed artist. Do not edit without permission. Underlying Pokémon IP remains with its owners; no blanket open-source image license is claimed.','sourceCount':len(records),'availableCount':sum(r['available'] for r in results),'bytes':sum(r.get('bytes',0) for r in results),'records':results}
assert manifest['bytes']<=6_000_000,manifest['bytes']
(ASSETS/'credit-manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,separators=(',',':'))+'\n');(HERE/'trainer-manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
print('FINAL',manifest['availableCount'],'bytes',manifest['bytes'],'beforeBytes',oldbytes,'failed',sum(not x['available'] for x in results),flush=True)

# Optional source-code regeneration is explicit because Models.kt may be edited
# concurrently by the app integration pass. Original choices keep their position.
if '--write-keys' in sys.argv:
 p=ROOT/'android/app/src/main/java/dev/bloody/prismadex/data/Models.kt'
 source=p.read_text();match=re.search(r'val TrainerSpriteKeys = listOf\((.*?)\n\)',source,re.S)
 previous=re.findall(r'"([^"]+)"',match[1]);available=[r['key'] for r in results if r['available']]
 keys=[k for k in previous if k in available]+[k for k in available if k not in previous]
 lines=['    '+', '.join(json.dumps(k) for k in keys[i:i+8])+',' for i in range(0,len(keys),8)]
 p.write_text(source[:match.start()]+'val TrainerSpriteKeys = listOf(\n'+'\n'.join(lines)+'\n)'+source[match.end():])
 print('TrainerSpriteKeys updated:',len(keys))
