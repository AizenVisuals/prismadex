#!/usr/bin/env python3
"""Reviewed PT/ES overrides for common battle-item summaries; no effect/rule changes."""
import json,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];HERE=Path(__file__).parent
reviewed={
'Leftovers':('Ao final de cada turno, o portador recupera 1/16 de seu HP máximo.','Al final de cada turno, el portador recupera 1/16 de sus HP máximos.'),
'Life Orb':('Os ataques do portador causam 1.3× de dano, e ele perde 1/10 de seu HP máximo após atacar.','Los ataques del portador causan 1.3× de daño y pierde 1/10 de sus HP máximos tras atacar.'),
'Choice Band':('O Ataque do portador é multiplicado por 1.5×, mas ele só pode selecionar o primeiro golpe que executar.','El Ataque del portador se multiplica por 1.5×, pero solo puede elegir el primer movimiento que utilice.'),
'Choice Specs':('O Atq. Esp. do portador é multiplicado por 1.5×, mas ele só pode selecionar o primeiro golpe que executar.','El At. Esp. del portador se multiplica por 1.5×, pero solo puede elegir el primer movimiento que utilice.'),
'Choice Scarf':('A Velocidade do portador é multiplicada por 1.5×, mas ele só pode selecionar o primeiro golpe que executar.','La Velocidad del portador se multiplica por 1.5×, pero solo puede elegir el primer movimiento que utilice.'),
'Focus Sash':('Com HP cheio, o portador sobrevive com 1 HP a um ataque que o nocautearia. Uso único.','Con los HP al máximo, el portador sobrevive con 1 HP a un ataque que lo debilitaría. Un solo uso.'),
'Focus Band':('O portador tem 10% de chance de sobreviver com 1 HP a um ataque que o nocautearia.','El portador tiene un 10% de probabilidad de sobrevivir con 1 HP a un ataque que lo debilitaría.'),
'Sitrus Berry':('Recupera 1/4 do HP máximo quando o portador está com 1/2 do HP máximo ou menos. Uso único.','Recupera 1/4 de los HP máximos cuando el portador tiene 1/2 de sus HP máximos o menos. Un solo uso.'),
'Lum Berry':('Cura uma condição de status persistente ou confusão do portador. Uso único.','Cura un problema de estado persistente o la confusión del portador. Un solo uso.'),
'Assault Vest':('A Def. Esp. do portador é multiplicada por 1.5×, mas ele só pode selecionar golpes que causam dano.','La Def. Esp. del portador se multiplica por 1.5×, pero solo puede elegir movimientos que causan daño.'),
'Eviolite':('Se a espécie do portador puder evoluir, sua Defesa e Def. Esp. são multiplicadas por 1.5×.','Si la especie del portador puede evolucionar, su Defensa y Def. Esp. se multiplican por 1.5×.'),
'Expert Belt':('Os ataques super efetivos do portador causam 1.2× de dano ao alvo.','Los ataques supereficaces del portador causan 1.2× de daño al objetivo.'),
'Light Clay':('Aurora Veil, Light Screen e Reflect usados pelo portador duram 8 turnos em vez de 5.','Aurora Veil, Light Screen y Reflect usados por el portador duran 8 turnos en lugar de 5.'),
'Rocky Helmet':('Se o portador for atingido por um golpe de contato, o atacante perde 1/6 de seu HP máximo.','Si el portador recibe un movimiento de contacto, el atacante pierde 1/6 de sus HP máximos.'),
'Black Sludge':('A cada turno, recupera 1/16 do HP máximo se o portador for do tipo Veneno; caso contrário, perde 1/8.','Cada turno, recupera 1/16 de los HP máximos si el portador es de tipo Veneno; de lo contrario, pierde 1/8.'),
'Toxic Orb':('Ao final de cada turno, este item tenta envenenar gravemente o portador.','Al final de cada turno, este objeto intenta intoxicar gravemente al portador.'),
'Flame Orb':('Ao final de cada turno, este item tenta causar queimadura no portador.','Al final de cada turno, este objeto intenta quemar al portador.'),
'Air Balloon':('O portador é imune a ataques do tipo Terra. O balão estoura quando ele é atingido.','El portador es inmune a ataques de tipo Tierra. El globo estalla cuando recibe un golpe.'),
'Weakness Policy':('Se o portador receber um ataque super efetivo, aumenta seu Ataque e Atq. Esp. em 2 estágios. Uso único.','Si el portador recibe un ataque supereficaz, aumenta su Ataque y At. Esp. en 2 niveles. Un solo uso.'),
'Safety Goggles':('O portador é imune a golpes de pó e ao dano de Sandstorm ou Hail.','El portador es inmune a movimientos de polvo y al daño de Sandstorm o Hail.'),
'Heavy-Duty Boots':('Ao entrar em campo, o portador não é afetado por armadilhas em seu lado do campo.','Al entrar en combate, el portador no se ve afectado por trampas en su lado del campo.'),
'Protective Pads':('Os golpes do portador ficam protegidos de efeitos adversos de contato, exceto Pickpocket.','Los movimientos del portador están protegidos de efectos adversos de contacto, excepto Pickpocket.'),
'Booster Energy':('Ativa as habilidades Protosynthesis ou Quark Drive. Uso único.','Activa las habilidades Protosynthesis o Quark Drive. Un solo uso.'),
'Ability Shield':('A habilidade do portador não pode ser alterada, suprimida ou ignorada por nenhum efeito.','La habilidad del portador no puede ser cambiada, anulada ni ignorada por ningún efecto.'),
'Clear Amulet':('Impede que outros Pokémon reduzam os estágios de atributos do portador.','Impide que otros Pokémon reduzcan los niveles de características del portador.'),
'Mirror Herb':('Quando um Pokémon adversário aumenta seus atributos, o portador copia esses aumentos. Uso único.','Cuando un Pokémon rival aumenta sus características, el portador copia esos aumentos. Un solo uso.'),
'Covert Cloak':('O portador não é afetado pelos efeitos secundários dos ataques de outros Pokémon.','El portador no se ve afectado por los efectos secundarios de ataques de otros Pokémon.'),
'Loaded Dice':('Os golpes do portador que acertam 2-5 vezes acertam 4-5 vezes; Population Bomb acerta 4-10 vezes.','Los movimientos del portador que golpean 2-5 veces golpean 4-5 veces; Population Bomb golpea 4-10 veces.'),
}
asset=json.loads((ROOT/'android/app/src/main/assets/general-items.json').read_text());out={}
for item in asset['items']:
 if item['name'] in reviewed:
  original=item['descriptionEnglish'];pt,es=reviewed[item['name']]
  nums=lambda s:re.findall(r'\d+(?:[.,/]\d+)*(?:%|×)?',s)
  assert nums(original)==nums(pt)==nums(es),(item['name'],nums(original),nums(pt),nums(es))
  out[original]={'pt':pt,'es':es}
(HERE/'reviewed-items.json').write_text(json.dumps(out,ensure_ascii=False,indent=2)+'\n')
print('Reviewed common-item summaries:',len(out))
