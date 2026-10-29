#!/usr/bin/env python3
"""Reviewed recurring item-summary grammar; proper names and numeric values stay literal."""
import json,re
from pathlib import Path
HERE=Path(__file__).parent
out=json.loads((HERE/'reviewed-items.json').read_text())
texts=json.loads((HERE/'translation-inputs-items.json').read_text())
types={'Normal':('Normal','Normal'),'Fire':('Fogo','Fuego'),'Water':('Água','Agua'),'Grass':('Planta','Planta'),'Electric':('Elétrico','Eléctrico'),'Ice':('Gelo','Hielo'),'Fighting':('Lutador','Lucha'),'Poison':('Veneno','Veneno'),'Ground':('Terra','Tierra'),'Flying':('Voador','Volador'),'Psychic':('Psíquico','Psíquico'),'Bug':('Inseto','Bicho'),'Rock':('Pedra','Roca'),'Ghost':('Fantasma','Fantasma'),'Dragon':('Dragão','Dragón'),'Dark':('Sombrio','Siniestro'),'Steel':('Aço','Acero'),'Fairy':('Fada','Hada')}
def t(x,l):return types[x][l]
for source in texts:
 if source in out:continue
 pair=None
 if m:=re.fullmatch(r'If held by (?:a|an) (.+), this item allows it to Mega Evolve in battle\.',source):
  n=m[1];pair=(f'Se for segurado por {n}, permite sua Mega Evolução em batalha.',f'Si lo lleva {n}, permite su megaevolución en combate.')
 elif m:=re.fullmatch(r'Teaches certain Pokemon the move (.+)\. One use\.',source):
  n=m[1];pair=(f'Ensina o golpe {n} a determinados Pokémon. Uso único.',f'Enseña el movimiento {n} a ciertos Pokémon. Un solo uso.')
 elif m:=re.fullmatch(r'Can be revived into (.+)\.',source):
  n=m[1];pair=(f'Pode ser revivido como {n}.',f'Puede revivirse como {n}.')
 elif m:=re.fullmatch(r'Evolves (.+) into (.+) when (used|traded)\.',source):
  a,b,trigger=m.groups();pair=(f'Evolui {a} para {b} '+('quando usado.' if trigger=='used' else 'durante uma troca.'),f'Evoluciona a {a} en {b} '+('al usarlo.' if trigger=='used' else 'durante un intercambio.'))
 elif m:=re.fullmatch(r"Holder's (\w+)-type attacks have ([\d.]+×) power\.(?: Judgment is (\w+) type\.)?",source):
  typ,n,judge=m.groups();pair=tuple(f'Os ataques do tipo {t(typ,0)} do portador têm {n} de poder.'+(f' Judgment é do tipo {t(judge,0)}.' if judge else '') if l==0 else f'Los ataques de tipo {t(typ,1)} del portador tienen {n} de potencia.'+(f' Judgment es de tipo {t(judge,1)}.' if judge else '') for l in range(2))
 elif m:=re.fullmatch(r"Holder's Multi-Attack is (\w+) type\.",source):
  typ=m[1];pair=(f'Multi-Attack do portador é do tipo {t(typ,0)}.',f'Multi-Attack del portador es de tipo {t(typ,1)}.')
 elif m:=re.fullmatch(r"Holder's first successful (\w+)-type attack will have ([\d.]+×) power\. Single use\.",source):
  typ,n=m.groups();pair=(f'O primeiro ataque bem-sucedido do tipo {t(typ,0)} do portador terá {n} de poder. Uso único.',f'El primer ataque exitoso de tipo {t(typ,1)} del portador tendrá {n} de potencia. Un solo uso.')
 elif m:=re.fullmatch(r'Halves damage taken from a supereffective (\w+)-type attack\. Single use\.',source):
  typ=m[1];pair=(f'Reduz à metade o dano recebido de um ataque super efetivo do tipo {t(typ,0)}. Uso único.',f'Reduce a la mitad el daño recibido de un ataque supereficaz de tipo {t(typ,1)}. Un solo uso.')
 elif m:=re.fullmatch(r'If holder has a (\w+) move, this item allows it to use a (\w+) Z-Move\.',source):
  a,b=m.groups();pair=(f'Se o portador tiver um golpe do tipo {t(a,0)}, permite usar um Z-Move do tipo {t(b,0)}.',f'Si el portador tiene un movimiento de tipo {t(a,1)}, permite usar un Z-Move de tipo {t(b,1)}.')
 elif m:=re.fullmatch(r'If held by (?:a |an )?(.+) with (.+), it can use (.+)\.',source):
  a,b,c=m.groups();pair=(f'Se for segurado por {a} com o golpe {b}, permite usar {c}.',f'Si lo lleva {a} con el movimiento {b}, permite usar {c}.')
 if pair:out[source]={'pt':pair[0],'es':pair[1]}
(HERE/'reviewed-items.json').write_text(json.dumps(out,ensure_ascii=False,indent=2)+'\n')
remaining=[s for s in texts if s not in out]
(HERE/'items-mt-remaining.txt').write_text('\n'.join(remaining)+'\n')
print('Reviewed/templates:',len(out),'remainingMT:',len(remaining))
