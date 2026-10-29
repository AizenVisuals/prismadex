#!/usr/bin/env python3
import json,re
from pathlib import Path
HERE=Path(__file__).parent
out=json.loads((HERE/'reviewed-items.json').read_text());texts=json.loads((HERE/'translation-inputs-items.json').read_text())
types={'Normal':('Normal','Normal'),'Fire':('Fogo','Fuego'),'Water':('Água','Agua'),'Grass':('Planta','Planta'),'Electric':('Elétrico','Eléctrico'),'Ice':('Gelo','Hielo'),'Fighting':('Lutador','Lucha'),'Poison':('Veneno','Veneno'),'Ground':('Terra','Tierra'),'Flying':('Voador','Volador'),'Psychic':('Psíquico','Psíquico'),'Bug':('Inseto','Bicho'),'Rock':('Pedra','Roca'),'Ghost':('Fantasma','Fantasma'),'Dragon':('Dragão','Dragón'),'Dark':('Sombrio','Siniestro'),'Steel':('Aço','Acero'),'Fairy':('Fada','Hada')}
stats={'Attack':('Ataque','Ataque'),'Defense':('Defesa','Defensa'),'Speed':('Velocidade','Velocidad'),'Sp. Atk':('Atq. Esp.','At. Esp.'),'Special Attack':('Atq. Esp.','At. Esp.'),'Sp. Def':('Def. Esp.','Def. Esp.')}
triggers={'when at 1/4 max HP or less':('quando está com 1/4 do HP máximo ou menos','cuando tiene 1/4 de sus HP máximos o menos'),'after it is hit by a physical attack':('após receber um ataque físico','tras recibir un ataque físico'),'after it is hit by a special attack':('após receber um ataque especial','tras recibir un ataque especial'),'after it uses a sound move':('após usar um golpe sonoro','tras usar un movimiento de sonido'),'if it gets affected by Intimidate':('se for afetado por Intimidate','si recibe el efecto de Intimidate')}
for typ,(pt,es) in types.items():triggers[f'if hit by a {typ}-type attack']=(f'se receber um ataque do tipo {pt}',f'si recibe un ataque de tipo {es}');triggers[f'if hit by an {typ}-type attack']=(f'se receber um ataque do tipo {pt}',f'si recibe un ataque de tipo {es}')
for source in texts:
 if source in out:continue
 pair=None
 if m:=re.fullmatch(r'If held by (?:a|an) (.+), this item allows it to Mega Evolve into (.+?)(?: in battle)?\.',source):
  a,b=m.groups();pair=(f'Se for segurado por {a}, permite sua Mega Evolução em {b}.',f'Si lo lleva {a}, permite su megaevolución en {b}.')
 elif m:=re.fullmatch(r'If held by (?:a|an) (.+), this item triggers its Primal Reversion in battle\.',source):
  a=m[1];pair=(f'Se for segurado por {a}, ativa sua Reversão Primal em batalha.',f'Si lo lleva {a}, activa su Regresión Primigenia en combate.')
 elif m:=re.fullmatch(r'If held by (?:a|an) (.+), this item changes its forme to (.+)\.',source):
  a,b=m.groups();pair=(f'Se for segurado por {a}, muda sua forma para {b}.',f'Si lo lleva {a}, cambia su forma a {b}.')
 elif m:=re.fullmatch(r'If holder has an (\w+) move, this item allows it to use an (\w+) Z-Move\.',source):
  a,b=m.groups();pair=(f'Se o portador tiver um golpe do tipo {types[a][0]}, permite usar um Z-Move do tipo {types[b][0]}.',f'Si el portador tiene un movimiento de tipo {types[a][1]}, permite usar un Z-Move de tipo {types[b][1]}.')
 elif m:=re.fullmatch(r"Holder's Techno Blast is (\w+) type\.",source):
  a=m[1];pair=(f'Techno Blast do portador é do tipo {types[a][0]}.',f'Techno Blast del portador es de tipo {types[a][1]}.')
 elif m:=re.fullmatch(r"Holder's use of (.+) lasts (\d+) turns instead of (\d+)\.",source):
  a,b,c=m.groups();pair=(f'{a} usado pelo portador dura {b} turnos em vez de {c}.',f'{a} usado por el portador dura {b} turnos en lugar de {c}.')
 elif m:=re.fullmatch(r"Raises holder's (.+?) by (\d+)(?: stage)? (.+)\. Single use\.",source):
  a,b,c=m.groups()
  if a in stats and c in triggers:pair=(f'Aumenta {stats[a][0]} do portador em {b} estágio(s) {triggers[c][0]}. Uso único.',f'Aumenta {stats[a][1]} del portador en {b} nivel(es) {triggers[c][1]}. Un solo uso.')
 elif m:=re.fullmatch(r"If the terrain is (.+), raises holder's (.+) by (\d+) stage\. Single use\.",source):
  a,b,c=m.groups();pair=(f'Se o terreno for {a}, aumenta {stats[b][0]} do portador em {c} estágio. Uso único.',f'Si el terreno es {a}, aumenta {stats[b][1]} del portador en {c} nivel. Un solo uso.')
 elif m:=re.fullmatch(r'Restores (\d+) HP when at (\d+/\d+) max HP or less\. Single use\.',source):
  a,b=m.groups();pair=(f'Recupera {a} HP quando o portador está com {b} do HP máximo ou menos. Uso único.',f'Recupera {a} HP cuando el portador tiene {b} de sus HP máximos o menos. Un solo uso.')
 elif m:=re.fullmatch(r'Restores (\d+/\d+) max HP at (\d+/\d+) max HP or less; confuses if -(\w+) Nature\. Single use\.',source):
  a,b,c=m.groups();stat={'Atk':('Ataque','Ataque'),'Def':('Defesa','Defensa'),'SpA':('Atq. Esp.','At. Esp.'),'SpD':('Def. Esp.','Def. Esp.'),'Spe':('Velocidade','Velocidad')}[c]
  pair=(f'Recupera {a} do HP máximo com {b} do HP máximo ou menos; causa confusão se a natureza reduzir {stat[0]}. Uso único.',f'Recupera {a} de los HP máximos con {b} de los HP máximos o menos; causa confusión si la naturaleza reduce {stat[1]}. Un solo uso.')
 elif m:=re.fullmatch(r"If held by (?:a|an) (.+), its (\w+)- and (\w+)-type (?:attacks|moves) have ([\d.]+×) power\.",source):
  a,b,c,d=m.groups();pair=(f'Se for segurado por {a}, seus ataques dos tipos {types[b][0]} e {types[c][0]} têm {d} de poder.',f'Si lo lleva {a}, sus ataques de tipos {types[b][1]} y {types[c][1]} tienen {d} de potencia.')
 elif m:=re.fullmatch(r"If held by (?:a|an) (.+), its (.+) is doubled\.",source):
  a,b=m.groups()
  if b in stats:pair=(f'Se for segurado por {a}, dobra seu atributo {stats[b][0]}.',f'Si lo lleva {a}, duplica su característica {stats[b][1]}.')
 elif m:=re.fullmatch(r'Can revive into (.+) with (.+) or (.+) with (.+)\.',source):
  a,b,c,d=m.groups();pair=(f'Pode ser revivido como {a} com {b}, ou como {c} com {d}.',f'Puede revivirse como {a} con {b}, o como {c} con {d}.')
 if pair:out[source]={'pt':pair[0],'es':pair[1]}
(HERE/'reviewed-items.json').write_text(json.dumps(out,ensure_ascii=False,indent=2)+'\n')
remaining=[s for s in texts if s not in out];(HERE/'items-mt-remaining.txt').write_text('\n'.join(remaining)+'\n');print('Reviewed',len(out),'MTremaining',len(remaining))
