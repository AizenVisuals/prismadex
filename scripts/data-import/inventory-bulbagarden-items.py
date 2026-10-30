#!/usr/bin/env python3
import urllib.request,urllib.parse,json,time
from pathlib import Path
out=Path(__file__).resolve().parents[2]/'research/item-artwork';out.mkdir(exist_ok=True)
headers={'User-Agent':'PrismaDex local offline-art catalog research/0.5'}
for cat in ['Item artwork','Bag sprites']:
 all=[];cont={};page=0
 while True:
  path=out/(cat.replace(' ','-')+f'-{page}.json')
  if path.exists():j=json.loads(path.read_text())
  else:
   p={'action':'query','list':'categorymembers','cmtitle':'Category:'+cat,'cmtype':'file','cmlimit':500,'format':'json',**cont}
   u='https://archives.bulbagarden.net/w/api.php?'+urllib.parse.urlencode(p)
   j=json.load(urllib.request.urlopen(urllib.request.Request(u,headers=headers),timeout=30));path.write_text(json.dumps(j,ensure_ascii=False));time.sleep(.3)
  all+=j.get('query',{}).get('categorymembers',[]);print(cat,page,len(all),flush=True)
  if 'continue' not in j:break
  cont=j['continue'];page+=1
 (out/(cat.replace(' ','-')+'-inventory.json')).write_text(json.dumps(all,ensure_ascii=False,indent=2))
