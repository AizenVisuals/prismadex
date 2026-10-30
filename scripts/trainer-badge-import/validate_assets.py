#!/usr/bin/env python3
"""Verify every imported image and source hash, offline, without modifying PNGs."""
from pathlib import Path
import json,hashlib,struct,zlib,re
ROOT=Path(__file__).resolve().parents[2];HERE=Path(__file__).parent;ASSETS=ROOT/'android/app/src/main/assets'
def png(path):
 b=path.read_bytes();assert b[:8]==b'\x89PNG\r\n\x1a\n',str(path);i=8;idat=[];iend=False;dimensions=None
 while i<len(b):
  n=struct.unpack('>I',b[i:i+4])[0];kind=b[i+4:i+8];payload=b[i+8:i+8+n];crc=struct.unpack('>I',b[i+8+n:i+12+n])[0];assert zlib.crc32(kind+payload)&0xffffffff==crc,(path,kind)
  if kind==b'IHDR':dimensions=struct.unpack('>II',payload[:8]);assert dimensions[0]>0 and dimensions[1]>0
  if kind==b'IDAT':idat.append(payload)
  if kind==b'IEND':iend=True;break
  i+=n+12
 assert iend and idat and zlib.decompress(b''.join(idat)),str(path)
 return dimensions
trainers=json.loads((HERE/'trainer-manifest.json').read_text());badges=json.loads((HERE/'badge-manifest.json').read_text());artists=set()
for r in trainers['records']:
 assert r['available'];p=ASSETS/'trainers'/r['filename'];assert hashlib.sha256(p.read_bytes()).hexdigest()==r['sha256'];assert png(p)==(r['width'],r['height']);assert not r['edited']
 if r['artist']:artists.add(r['artist'])
for r in badges['records']:
 p=ROOT/r['asset'];assert hashlib.sha256(p.read_bytes()).hexdigest()==r['assetSha256'];assert png(p)==(96,96)
s=(ROOT/'android/app/src/main/java/dev/bloody/prismadex/data/Models.kt').read_text();m=re.search(r'val TrainerSpriteKeys = listOf\((.*?)\n\)',s,re.S);keys=re.findall(r'"([^"]+)"',m[1]);assert set(keys)=={r['key'] for r in trainers['records']};assert len(keys)==len(set(keys))
assert badges['sets']['galar-sword']['sourceIds']==[51,52,53,54,56,57,59,60]
assert badges['sets']['galar-shield']['sourceIds']==[51,52,53,55,56,58,59,60]
report={'trainerPNGs':len(keys),'trainerBytes':trainers['bytes'],'artistNames':sorted(artists),'trainerEdits':False,'existing37Changed':any(r.get('existingChanged') for r in trainers['records']),'badgePNGs':len(list((ASSETS/'badges').glob('*.png'))),'newBadgePNGs':badges['newFiles'],'newBadgeBytes':badges['totalNewBytes'],'pngSignaturesAndCRCs':'passed','sourceHashMatch':'passed','keyCoverage':'passed'}
(HERE/'validation-report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps(report,ensure_ascii=False,indent=2))
