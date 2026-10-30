#!/usr/bin/env python3
"""Explicit, exact-name offline reference artwork import. Never changes the166helditem assets.
Requires the cached bounded category inventory+matched list under research/item-artwork.
Stops immediately on HTTP401/403; never switches identity, proxy or authentication.
"""
import json,urllib.request,urllib.parse,urllib.error,time,hashlib,subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];WORK=ROOT/'research/item-artwork';ASSETS=ROOT/'android/app/src/main/assets';CACHE=WORK/'original-images';CACHE.mkdir(exist_ok=True)
DEST=ASSETS/'general-item-art';DEST.mkdir(exist_ok=True)
HEADERS={'User-Agent':'PrismaDex local offline-art reference research/0.5'}
MATCHES=json.loads((WORK/'matched.json').read_text());INFO=WORK/'imageinfo';INFO.mkdir(exist_ok=True)
def fetch(url):
 try:
  with urllib.request.urlopen(urllib.request.Request(url,headers=HEADERS),timeout=40) as r:
   data=r.read();assert len(data)<8_000_000,'Image/payload exceeds8MB bound';return data
 except urllib.error.HTTPError as e:
  if e.code in (401,403):raise SystemExit(f'AccessdeniedHTTP{e.code}; stoppedwithoutbypass: {url}')
  if e.code==429:
   delay=int(e.headers.get('Retry-After','60'));print('Rate limited; stop and resume after',delay,'seconds',flush=True);raise SystemExit(2)
  raise
metadata={}
for cached in INFO.glob('*.json'):
 for page in json.loads(cached.read_text()).get('query',{}).get('pages',{}).values():
  if page.get('imageinfo'):metadata[page['title']]=page['imageinfo'][0]
needed=[m for m in MATCHES if m['source']['title'] not in metadata]
for offset in range(0,len(needed),40):
 batch=needed[offset:offset+40];key=hashlib.sha256('|'.join(m['source']['title'] for m in batch).encode()).hexdigest()[:16];path=INFO/(key+'.json')
 if not path.exists():
  params={'action':'query','titles':'|'.join(m['source']['title'] for m in batch),'prop':'imageinfo','iiprop':'url|size|sha1|extmetadata','format':'json'}
  path.write_bytes(fetch('https://archives.bulbagarden.net/w/api.php?'+urllib.parse.urlencode(params)));time.sleep(.25)
 result=json.loads(path.read_text());assert not result.get('error'),result.get('error')
 for page in result.get('query',{}).get('pages',{}).values():
  if page.get('imageinfo'):metadata[page['title']]=page['imageinfo'][0]
 print('metadata',min(offset+40,len(MATCHES)),'/',len(MATCHES),flush=True)
records=[];failed=[]
try:
 for i,m in enumerate(MATCHES):
  info=metadata.get(m['source']['title'])
  if not info:failed.append(dict(m,reason='source-file-metadata-unavailable'));continue
  original=CACHE/(str(m['id'])+'.png')
  if not original.exists():
   original.write_bytes(fetch(info['url']));time.sleep(.12)
  b=original.read_bytes()
  if info.get('sha1') and hashlib.sha1(b).hexdigest()!=info['sha1']:
   failed.append(dict(m,reason='source-file-changed-or-sha1-mismatch'));continue
  if not (b.startswith(b'\x89PNG\r\n\x1a\n') or b.startswith(b'GIF8') or b.startswith(b'\xff\xd8\xff') or (b.startswith(b'RIFF') and b[8:12]==b'WEBP')):
   failed.append(dict(m,reason='not-recognized-image'));continue
  output=DEST/(str(m['id'])+'.webp')
  if not output.exists():
   subprocess.run(['magick',str(original),'-thumbnail','128x128>','-define','webp:lossless=true',str(output)],check=True,stdout=subprocess.DEVNULL,stderr=subprocess.PIPE)
  record=dict(id=m['id'],name=m['name'],slug=m['slug'],path='general-item-art/'+output.name,sourceTitle=m['source']['title'],sourceGameTag=m['source']['gameTag'],matchMethod=m['source'].get('matchMethod'),reviewedGameVariant=m.get('reviewedGameVariant'),sourceImageUrl=info['url'],descriptionUrl=info['descriptionurl'],originalWidth=info.get('width'),originalHeight=info.get('height'),sourceSha1=info.get('sha1'),originalSha256=hashlib.sha256(b).hexdigest(),assetSha256=hashlib.sha256(output.read_bytes()).hexdigest(),copyright='Pokémon / Nintendo / Creatures / GAME FREAK; original game item art, via Bulbagarden Archives. No blanket redistribution license asserted.')
  records.append(record)
  if (i+1)%25==0:print('images',i+1,'/',len(MATCHES),flush=True)
finally:
 (ASSETS/'general-item-art-map.json').write_text(json.dumps({'schemaVersion':1,'sourceCategory':'https://archives.bulbagarden.net/wiki/Category:Item_artwork','items':records},ensure_ascii=False,separators=(',',':'))+'\n')
 (WORK/'download-failures.json').write_text(json.dumps(failed,ensure_ascii=False,indent=2))
 report={'requested':len(MATCHES),'imported':len(records),'failed':len(failed),'sourceImagesBytes':sum((CACHE/(str(r['id'])+'.png')).stat().st_size for r in records),'assetBytes':sum((ASSETS/r['path']).stat().st_size for r in records),'existingHeldAssetsUnchanged':166,'newReferenceArtRows':len(records),'remainingGenericRows':2223-167-len(records)}
 (WORK/'coverage.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(report,flush=True)
