#!/usr/bin/env python3
"""Reproducible species biology + exact-form profile import, entirely offline at runtime.
Uses pinned PokeAPI structured facts and MIT Showdown community effect summaries.
No Pokedex flavor text, lore paragraphs, site scraping or availability inference.
"""
from pathlib import Path
import argparse, csv, hashlib, json, re, urllib.request, concurrent.futures, unicodedata
ROOT=Path(__file__).resolve().parents[2]; HERE=Path(__file__).parent; CACHE=HERE/'cache'; ASSETS=ROOT/'android/app/src/main/assets'
POKEAPI='2bf639e5572812a67d2b94903fdcb9d095f5aeb2'; SHOWDOWN='1242c1e2d9763ba5b85d33f13aecb89bae19ae3d'
FILES=['pokemon_species.csv','pokemon_species_names.csv','pokemon.csv','pokemon_forms.csv','pokemon_abilities.csv','abilities.csv','ability_names.csv','egg_groups.csv','egg_group_prose.csv','pokemon_egg_groups.csv','growth_rates.csv','growth_rate_prose.csv','languages.csv','pokemon_types.csv','pokemon_stats.csv']
def download():
 CACHE.mkdir(parents=True,exist_ok=True)
 prior=json.loads((HERE/'sources.json').read_text()) if (HERE/'sources.json').exists() else {}
 expected={r['name']:r['sha256'] for r in prior.get('sources',[])} if prior.get('commit')==POKEAPI else {}
 def one(f):
  url=f'https://raw.githubusercontent.com/PokeAPI/pokeapi/{POKEAPI}/data/v2/csv/{f}';p=CACHE/f
  if not p.exists():p.write_bytes(urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'PrismaDex-data-import'}),timeout=30).read())
  if f in expected:assert hashlib.sha256(p.read_bytes()).hexdigest()==expected[f], 'Changed locked source cache: '+f
  return {'name':f,'url':url,'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'bytes':p.stat().st_size}
 with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:sources=list(pool.map(one,FILES))
 (HERE/'sources.json').write_text(json.dumps({'commit':POKEAPI,'retrievedAt':'2026-10-29','license':'BSD-3-Clause; repository LICENSE.md; flavor text excluded','sources':sources},indent=2)+'\n')
def rows(name):return list(csv.DictReader((CACHE/name).open()))
def integer(x):return int(x) if x not in ['',None] else None
def key(s):return re.sub('[^a-z0-9]','',unicodedata.normalize('NFKD',s).lower().replace('é','e'))
def tokens(s):return sorted(re.findall('[a-z0-9]+',unicodedata.normalize('NFKD',s).lower().replace('’','').replace("'",'')))
def text(pt,en,es):return {'pt':pt,'en':en,'es':es}
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--fetch',action='store_true');parser.add_argument('--prepare-translations',action='store_true');args=parser.parse_args()
 if args.fetch:download()
 lock=json.loads((HERE/'sources.json').read_text())
 assert lock['commit']==POKEAPI
 for source in lock['sources']:
  assert hashlib.sha256((CACHE/source['name']).read_bytes()).hexdigest()==source['sha256'], 'Changed source cache: '+source['name']
 catalog=json.loads((ASSETS/'data.json').read_text())
 additional_path=ASSETS/'additional-form-profiles.json'
 if additional_path.exists():catalog+=json.loads(additional_path.read_text())['records']
 species={int(x['id']):x for x in rows('pokemon_species.csv')};pokemon=rows('pokemon.csv');byname={x['identifier']:x for x in pokemon};byid={int(x['id']):x for x in pokemon}
 defaults={int(x['species_id']):x for x in pokemon if x['is_default']=='1'};identities=json.loads((ASSETS/'pokemon-identities.json').read_text())
 genus={(int(x['pokemon_species_id']),int(x['local_language_id'])):x['genus'] for x in rows('pokemon_species_names.csv') if x['genus']}
 ability_defs={int(x['id']):x for x in rows('abilities.csv')};ability_names={int(x['ability_id']):x['name'] for x in rows('ability_names.csv') if x['local_language_id']=='9'}
 ability_rows={}
 for r in rows('pokemon_abilities.csv'):ability_rows.setdefault(int(r['pokemon_id']),[]).append(r)
 eggs={}
 for r in rows('pokemon_egg_groups.csv'):eggs.setdefault(int(r['species_id']),[]).append(int(r['egg_group_id']))
 egg_names={(int(r['egg_group_id']),int(r['local_language_id'])):r['name'] for r in rows('egg_group_prose.csv')}
 egg_pt={1:'Monstro',2:'Água 1',3:'Inseto',4:'Voador',5:'Terrestre',6:'Fada',7:'Planta',8:'Humanoide',9:'Água 3',10:'Mineral',11:'Amorfo',12:'Água 2',13:'Ditto',14:'Dragão',15:'Não descoberto'}
 growth={1:text('Lento','Slow','Lento'),2:text('Médio','Medium','Medio'),3:text('Rápido','Fast','Rápido'),4:text('Médio lento','Medium slow','Medio lento'),5:text('Errático','Erratic','Errático'),6:text('Flutuante','Fluctuating','Fluctuante')}
 art_path=HERE/'catalog-form-aliases.json'
 artmap={int(i):name for i,name in json.loads(art_path.read_text()).items()}
 # Explicit linguistic equivalences retain form semantics; never merge by species alone.
 aliases={'partnerpikachu':'pikachu-starter','partnereevee':'eevee-starter','eternalfloette':'floette-eternal','coreminior':'minior-red','complet ezygarde':'zygarde-complete','primalgroudon':'groudon-primal','primalkyogre':'kyogre-primal','femalemeowstic':'meowstic-female','meowsticfemale':'meowstic-female','mega male meowstic':'meowstic-male-mega','megamale meowstic':'meowstic-male-mega','terastalterapagos':'terapagos-terastal','cosplaypikachu':'pikachu-cosplay','duskmanenecrozma':'necrozma-dusk','dawnwingsnecrozma':'necrozma-dawn','ice rider calyrex':'calyrex-ice','shadow rider calyrex':'calyrex-shadow','nopicefaceeiscue':'eiscue-noice','gigantamaxtoxtricity':'toxtricity-amped-gmax','gigantamaxflapple':'flapple-gmax','gigantamaxappletun':'appletun-gmax'}
 aliases={key(k):v for k,v in aliases.items()}
 bykey={key(x['identifier']):x for x in pokemon}
 bytokens={}
 for r in pokemon:bytokens.setdefault(tuple(tokens(r['identifier'])),[]).append(r)
 mapping={};mapped_forms={};unknown=[]
 forms=rows('pokemon_forms.csv')
 forms_by_tokens={}
 for f in forms:forms_by_tokens.setdefault(tuple(tokens(f['identifier'])),[]).append(f)
 for p in catalog:
  sid=p['species'];candidates=[]
  if p.get('pokeapiId'):candidates.append(byid.get(p['pokeapiId']))
  if p['id']==sid:candidates.append(defaults.get(sid))
  for name in [artmap.get(p['id']),identities.get(str(p['id']),{}).get('canonicalName'),p['name']]:
   if not name:continue
   n=name.lower().replace('’','').replace("'",'').replace('gigantamax','gmax')
   candidates += [byname.get(name),bykey.get(key(name)),bykey.get(key(n)),byname.get(aliases.get(key(name),'') )]
   for prefix,suffix in [('alolan','alola'),('galarian','galar'),('hisuian','hisui'),('paldean','paldea')]:
    if n.startswith(prefix+' '):candidates.append(bykey.get(key(n[len(prefix)+1:]+'-'+suffix)))
   candidates += bytokens.get(tuple(tokens(n)),[])
  found=next((x for x in candidates if x and int(x['species_id'])==sid),None)
  if found:mapping[p['id']]=found
  else:
   cosmetic=forms_by_tokens.get(tuple(tokens(p['name'])),[])
   candidate=next((f for f in cosmetic if int(byid[int(f['pokemon_id'])]['species_id'])==sid),None)
   if candidate:
    mapping[p['id']]=byid[int(candidate['pokemon_id'])];mapped_forms[p['id']]=int(candidate['id']);continue
   unknown.append({'id':p['id'],'speciesId':sid,'name':p['name'],'reason':'Exact form has no unambiguous canonical mapping; species biology only'})
 showdown=json.loads((CACHE/'showdown-ability-text.json').read_text())
 ability_output={};translation_inputs=set()
 for aid,definition in ability_defs.items():
  name=ability_names.get(aid,definition['identifier'].replace('-',' ').title());effect=showdown.get(key(definition['identifier'])) or showdown.get(key(name),{})
  desc=effect.get('shortDesc') or effect.get('desc')
  # Several keys only refer to another entry; resolve static references, never execute callbacks.
  if desc and desc.startswith('#'):desc=None
  if desc:translation_inputs.add(desc)
  ability_output[str(aid)]={'id':aid,'name':name,'slug':definition['identifier'],'descriptionEnglish':desc,'effectSource':'showdown-mainline-shortDesc' if desc else None,'championsSpecific':False}
 for p in catalog:
  if genus.get((p['species'],9)):translation_inputs.add(genus[p['species'],9])
 for name in {a for p in catalog for a in p['abilities']}:
  if any(a['slug']==name or key(a['name'])==key(name) for a in ability_output.values()):continue
  effect=showdown.get({'asoneshadowrider':'asonespectrier','asoneicerider':'asoneglastrier'}.get(key(name),key(name)),{});desc=effect.get('shortDesc') or effect.get('desc')
  if desc:translation_inputs.add(desc)
  ability_output['local:'+name]={'id':None,'name':effect.get('name',name.replace('-',' ').title()),'slug':name,'descriptionEnglish':desc,'effectSource':'showdown-mainline-shortDesc' if desc else None,'championsSpecific':False}
 (HERE/'translation-inputs.json').write_text(json.dumps(sorted(translation_inputs),ensure_ascii=False,indent=2)+'\n')
 if args.prepare_translations:print('Prepared',len(translation_inputs),'translation texts');return
 translations=json.loads((HERE/'translations.json').read_text()) if (HERE/'translations.json').exists() else {}
 reviewed=json.loads((HERE/'reviewed-translations.json').read_text()) if (HERE/'reviewed-translations.json').exists() else {}
 for en,langs in reviewed.items():translations.setdefault(en,{}).update(langs)
 missing_translations=[]
 def localized(en,es=None):
  if not en:return None
  trans=translations.get(en,{})
  if not trans.get('pt') or (not es and not trans.get('es')):missing_translations.append(en)
  return text(trans.get('pt') or en,en,es or trans.get('es') or en)
 for a in ability_output.values():
  if a['slug']=='embody-aspect':
   a['descriptionEnglish']="The effect depends on Ogerpon's mask: Teal raises Speed, Wellspring Special Defense, Hearthflame Attack, and Cornerstone Defense on switch-in."
  a['description']=localized(a.pop('descriptionEnglish'));a['descriptionTranslation']='local-machine-translation-with-reviewed-overrides' if a['description'] else None
 profiles={}
 for p in catalog:
  s=species[p['species']];exact=mapping.get(p['id']);pid=int(exact['id']) if exact else None
  profiles[str(p['id'])]={'pokemonId':p['id'],'speciesId':p['species'],'pokeapiPokemonId':pid,'formResolved':exact is not None,'formGender':('female' if 'female' in exact['identifier'].split('-') else 'male' if 'male' in exact['identifier'].split('-') else None) if exact else None,'pokeapiFormId':mapped_forms.get(p['id']),'heightMeters':integer(exact['height'])/10 if exact and exact['height'] else None,'weightKg':integer(exact['weight'])/10 if exact and exact['weight'] else None,'category':localized(genus.get((p['species'],9)),genus.get((p['species'],7))), 'eggGroups':[text(egg_pt[g],egg_names.get((g,9),str(g)),egg_names.get((g,7),str(g))) for g in eggs.get(p['species'],[])],'genderRateEighths':integer(s['gender_rate']),'growthRate':growth.get(integer(s['growth_rate_id'])),'captureRate':integer(s['capture_rate']),'baseExperience':integer(exact['base_experience']) if exact else None,'hatchCounter':integer(s['hatch_counter']),'baseHappiness':integer(s['base_happiness']),'isBaby':s['is_baby']=='1','isLegendary':s['is_legendary']=='1','isMythical':s['is_mythical']=='1','hasGenderDifferences':s['has_gender_differences']=='1','abilities':[{'id':int(a['ability_id']),'slot':int(a['slot']),'isHidden':a['is_hidden']=='1'} for a in ability_rows.get(pid,[])]}
 # Reorder translated genus labels naturally in Portuguese without changing category meaning.
 for profile in profiles.values():
  if profile['category']:
   pt=profile['category']['pt'].replace('Pokémon','').strip();profile['category']['pt']='Pokémon '+pt
 meta={'schemaVersion':1,'scope':'National species facts and exact-form reference; no changes to Champions eligibility or builds','pokeapiCommit':POKEAPI,'abilityEffectsCommit':SHOWDOWN,'date':'2026-10-29','flavorTextImported':False}
 (ASSETS/'species-profiles.json').write_text(json.dumps({**meta,'profiles':profiles},ensure_ascii=False,separators=(',',':'))+'\n')
 (ASSETS/'ability-profiles.json').write_text(json.dumps({**meta,'abilities':ability_output},ensure_ascii=False,separators=(',',':'))+'\n')
 fields=['heightMeters','weightKg','category','eggGroups','genderRateEighths','growthRate','captureRate','baseExperience','hatchCounter','baseHappiness','abilities']
 coverage={f:{'filled':sum(p[f] is not None and p[f]!=[] for p in profiles.values()),'total':len(profiles),'percent':round(100*sum(p[f] is not None and p[f]!=[] for p in profiles.values())/len(profiles),2)} for f in fields}
 ability_lookup={}
 for ability in ability_output.values():
  for name in [ability['slug'],ability['name']]:ability_lookup[key(name)]=ability
 used_abilities={name for p in catalog for name in p['abilities']}
 missing_used=[name for name in used_abilities if not ability_lookup.get(key(name),{}).get('description')]
 report={'catalogRecords':len(catalog),'nationalSpecies':len({p['species'] for p in catalog}),'sourceNationalSpecies':len(species),'sourcePokemonRecords':len(pokemon),'exactFormMappings':len(mapping),'speciesOnlyForms':unknown,'coverage':coverage,'appUsedAbilitySlugs':len(used_abilities),'appUsedAbilitiesWithEffect':len(used_abilities)-len(missing_used),'missingAppUsedAbilityEffects':sorted(missing_used),'abilityEntries':len(ability_output),'abilityDescriptions':sum(bool(a['description']) for a in ability_output.values()),'missingTranslationTexts':sorted(set(missing_translations)),'missingAbilityEffects':[a for a in ability_output.values() if not a['description']],'flavorTextImported':False}
 (HERE/'coverage-report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');(HERE/'form-id-map.json').write_text(json.dumps({str(i):{'pokeapiId':int(p['id']),'canonical':p['identifier'],'pokeapiFormId':mapped_forms.get(i),'speciesId':int(p['species_id'])} for i,p in mapping.items()},ensure_ascii=False,indent=2)+'\n')
 print(json.dumps({k:v for k,v in report.items() if k not in ['speciesOnlyForms','missingAbilityEffects','missingTranslationTexts']},indent=2));print('Unknown forms',len(unknown),unknown[:15]);print('Missing translations',len(set(missing_translations)))
if __name__=='__main__':main()
