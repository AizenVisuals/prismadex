#!/usr/bin/env python3
"""Use the existing local translation runtime; never uploads source data.
Descriptions are community-authored mainline summaries, not Champions mechanics.
Named Pokémon, moves, items, abilities and numerical values are protected.
"""
import json,re,time
from pathlib import Path
import ctranslate2,sentencepiece
ROOT=Path(__file__).resolve().parents[2];HERE=Path(__file__).parent;MODELS=ROOT/'research/translation-tools';ASSETS=ROOT/'android/app/src/main/assets'
texts=json.loads((HERE/'translation-inputs-items.json').read_text());outpath=HERE/'translations-items.json';out=json.loads(outpath.read_text()) if outpath.exists() else {}
names={v['name'] for v in json.loads((ASSETS/'moves.json').read_text()).values()}|{v['name'] for v in json.loads((ASSETS/'items.json').read_text()).values()}|{v['name'] for v in json.loads((ASSETS/'data.json').read_text())}
for a in json.loads((HERE/'cache/showdown-ability-text.json').read_text()).values():
 if a.get('name'):names.add(a['name'])
names|={v['name'] for v in json.loads((ASSETS/'general-items.json').read_text())['items']}
names|={'Pokémon','HP','PP','EVs','IVs','STAB'}
terms={'Pokemon':('Pokémon','Pokémon'),'Special Defense':('Defesa Especial','Defensa Especial'),'Special Attack':('Ataque Especial','Ataque Especial'),'Sp. Atk':('Atq. Esp.','At. Esp.'),'Sp. Def':('Def. Esp.','Def. Esp.'),'Attack':('Ataque','Ataque'),'Defense':('Defesa','Defensa'),'Speed':('Velocidade','Velocidad'),'Ability':('habilidade','habilidad'),'moves':('golpes','movimientos'),'move':('golpe','movimiento')}
for en,pt,es in [('Fire','Fogo','Fuego'),('Water','Água','Agua'),('Grass','Planta','Planta'),('Electric','Elétrico','Eléctrico'),('Ice','Gelo','Hielo'),('Ground','Terra','Tierra'),('Flying','Voador','Volador'),('Rock','Pedra','Roca'),('Bug','Inseto','Bicho'),('Poison','Veneno','Veneno'),('Steel','Aço','Acero'),('Psychic','Psíquico','Psíquico'),('Dark','Sombrio','Siniestro'),('Ghost','Fantasma','Fantasma'),('Fairy','Fada','Hada'),('Dragon','Dragão','Dragón'),('Fighting','Lutador','Lucha'),('Normal','Normal','Normal')]:terms[en+'-type']=('tipo '+pt,'tipo '+es)
pattern=re.compile(r'(?<![\w])(?:'+'|'.join(re.escape(x) for x in sorted(set(terms)|names,key=len,reverse=True))+r')(?![\w])|\d+(?:[.,/]\d+)*(?:%|×)?')
for li,lang in enumerate(['pt','es']):
 sp=sentencepiece.SentencePieceProcessor(model_file=str(MODELS/lang/'sentencepiece.model'));tr=ctranslate2.Translator(str(MODELS/lang),compute_type='int8',intra_threads=2,inter_threads=1)
 needed=[x for x in texts if not out.get(x,{}).get(lang)];start=time.time()
 for offset in range(0,len(needed),24):
  batch=needed[offset:offset+24];masked=[];slots=[]
  for original in batch:
   table=[]
   def protect(m):
    val=m.group();i=len(table);marker='ZXQ'+chr(65+i//26)+chr(65+i%26);table.append((marker,terms.get(val,(val,val))[li]));return marker
   masked.append(pattern.sub(protect,original));slots.append(table)
  result=tr.translate_batch([sp.encode(x,out_type=str) for x in masked],beam_size=2,max_decoding_length=256,max_input_length=256)
  for original,mask,table,r in zip(batch,masked,slots,result):
   translated=sp.decode(r.hypotheses[0]).replace('▁',' ').strip()
   if any(translated.count(k)!=1 for k,_ in table):
    pieces=re.split('('+'|'.join(re.escape(k) for k,_ in table)+')',mask) if table else [mask];lookup=dict(table)
    encoded=[sp.encode(p,out_type=str) for p in pieces if p.strip() and p not in lookup]
    results=iter(tr.translate_batch(encoded,beam_size=2,max_decoding_length=256))
    translated=' '.join(lookup[p] if p in lookup else sp.decode(next(results).hypotheses[0]) for p in pieces if p.strip())
   else:
    for k,v in table:translated=translated.replace(k,v)
   translated=re.sub(r'\s+([.,;:!?])',r'\1',translated)
   if lang=='pt':
    for a,b in [('movimentos','golpes'),('movimento','golpe'),('utilizador','usuário'),('fases','estágios')]:translated=re.sub(r'\b'+a+r'\b',b,translated,flags=re.I)
   assert 'ZXQ' not in translated,(original,translated)
   out.setdefault(original,{})[lang]=translated
  outpath.write_text(json.dumps(out,ensure_ascii=False,separators=(',',':'))+'\n');print(lang,offset+len(batch),'/',len(needed),round(time.time()-start),flush=True)
print('DONE',flush=True)
