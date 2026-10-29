function prismaDamage(json) {
  var q=JSON.parse(json),C=PrismaCalc,g=C.Generations.get(0);
  function make(s){
    if(!g.species.get(C.toID(s.name)))throw new Error('Forma sem correspondência no motor Champions: '+s.name);
    var opts={nature:s.nature,evs:{},boosts:s.boosts||{},status:s.status||'',abilityOn:!!s.abilityOn};
    ['hp','atk','def','spa','spd','spe'].forEach(function(k,i){opts.evs[k]=s.sps[i]||0;});
    if(s.ability){var ab=g.abilities.get(C.toID(s.ability));if(!ab)throw new Error('Habilidade não reconhecida: '+s.ability);opts.ability=ab.name;}
    if(s.item){var item=g.items.get(C.toID(s.item));if(!item)throw new Error('Item não reconhecido: '+s.item);opts.item=item.name;}
    var p=new C.Pokemon(g,s.name,opts);
    p.originalCurHP=Math.max(1,Math.floor(p.maxHP()*(s.hpPercent||100)/100));return p;
  }
  var a=make(q.attacker),d=make(q.defender),mo={ability:a.ability,item:a.item};
  if(q.critical)mo.isCrit=true;
  if(q.hits>0)mo.hits=q.hits;
  if(!g.moves.get(C.toID(q.move)))throw new Error('Golpe sem correspondência no motor Champions: '+q.move);
  var move=new C.Move(g,q.move,mo),field=new C.Field(q.field||{}),result=C.calculate(g,a,d,move,field),range=result.range();
  var ko=move.category==='Status'?{text:'Golpe de status: não causa dano direto.'}:result.kochance(false);
  var rolls=Array.isArray(result.damage)&&result.damage.every(function(v){return typeof v==='number';})?result.damage:[];
  return JSON.stringify({attackerName:a.name,defenderName:d.name,gameType:field.gameType,min:range[0],max:range[1],percentMin:Math.floor(range[0]*1000/d.maxHP())/10,percentMax:Math.floor(range[1]*1000/d.maxHP())/10,hp:d.maxHP(),currentHP:d.curHP(),rolls:rolls,ko:ko.text||'',koChance:ko.chance===undefined?null:ko.chance,koHits:ko.n||0,description:result.fullDesc('%',false),attackerStats:a.rawStats,defenderStats:d.rawStats,attackerAbility:a.ability||'',defenderAbility:d.ability||'',category:move.category});
}
