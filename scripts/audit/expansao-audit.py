#!/usr/bin/env python3
"""PrismaDex expansion audit: coverage matrix over bundled assets, as loaded by the app.

Read-only over bundled assets. Writes only research/expansao-audit-coverage.json
and research/expansao-audit-matrix.md. Stdlib only.
"""
import json
import os
import re
import sys
from collections import Counter, defaultdict

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(ROOT, "android", "app", "src", "main", "assets")
OUT_JSON = os.path.join(ROOT, "research", "expansao-audit-coverage.json")
OUT_MD = os.path.join(ROOT, "research", "expansao-audit-matrix.md")

KNOWN_TYPES = ["normal", "grass", "fire", "water", "electric", "ice", "flying",
               "bug", "poison", "ground", "rock", "fighting", "psychic", "ghost",
               "dragon", "dark", "steel", "fairy"]


def load(name):
    with open(os.path.join(ASSETS, name), encoding="utf-8") as f:
        return json.load(f)


def pct(n, d):
    return round(100.0 * n / d, 2) if d else 0.0


def main():
    rep = {"gaps": [], "warnings": []}

    # ---- 1. Catalog: data.json + additional forms (same validation as the repo) ----
    data = load("data.json")
    add_root = load("additional-form-profiles.json")
    add_records = add_root["records"]
    valid_add, rejected_add = [], []
    for r in add_records:
        ok = (r["id"] == 300000 + r["pokeapiId"] and len(r["stats"]) == 6
              and all(s > 0 for s in r["stats"]) and 1 <= len(r["types"]) <= 2)
        valid_add.append(r) if ok else rejected_add.append(r["id"])
    catalog = []
    for row in data:
        catalog.append({"id": row["id"], "baseId": row.get("baseId", row["id"]),
                        "name": row["name"], "types": row["types"], "stats": row["stats"],
                        "abilities": row["abilities"], "height": row.get("height"),
                        "weight": row.get("weight"), "champions": bool(row.get("champions")),
                        "isMega": bool(row.get("isMega")), "source": "data.json"})
    for r in valid_add:
        catalog.append({"id": r["id"], "baseId": r["species"], "name": r["name"],
                        "types": r["types"], "stats": r["stats"], "abilities": r["abilities"],
                        "height": r.get("height"), "weight": r.get("weight"),
                        "champions": False, "isMega": bool(r.get("isMega")),
                        "source": "additional-form-profiles.json"})
    ids = [c["id"] for c in catalog]
    dup_ids = [i for i, n in Counter(ids).items() if n > 1]
    species = {c["baseId"] for c in catalog}
    name_counts = Counter(c["name"] for c in catalog)
    dup_names = {n: k for n, k in name_counts.items() if k > 1}
    bad_stats = [c["id"] for c in catalog
                 if len(c["stats"]) != 6 or any((not isinstance(s, int)) or s <= 0 or s > 255 for s in c["stats"])]
    bad_types = [c["id"] for c in catalog
                 if not c["types"] or len(c["types"]) > 2 or len(set(c["types"])) != len(c["types"])
                 or any(t not in KNOWN_TYPES for t in c["types"])]
    empty_abilities = [c["id"] for c in catalog if not c["abilities"]]
    null_height = [c["id"] for c in catalog if c["height"] is None]
    null_weight = [c["id"] for c in catalog if c["weight"] is None]
    champions = [c for c in catalog if c["champions"]]
    megas = [c for c in catalog if c["isMega"]]
    rep["catalog"] = {
        "data_json_rows": len(data), "additional_records": len(add_records),
        "additional_valid": len(valid_add), "additional_rejected": rejected_add,
        "total_records": len(catalog), "distinct_species_baseId": len(species),
        "duplicate_ids": dup_ids, "duplicate_names": dup_names,
        "bad_stats_ids": bad_stats, "bad_types_ids": bad_types,
        "empty_abilities_ids": empty_abilities,
        "null_height": len(null_height), "null_height_ids": null_height,
        "null_weight": len(null_weight), "null_weight_ids": null_weight,
        "champions_records": len(champions), "mega_records": len(megas),
        "by_source": dict(Counter(c["source"] for c in catalog)),
        "generation_missing_note": "generation/games come from pokemon-metadata.json, checked below",
    }
    if dup_ids:
        rep["gaps"].append(f"duplicate catalog ids: {dup_ids}")

    # ---- 2. Metadata + identities ----
    metadata = load("pokemon-metadata.json")
    identities = load("pokemon-identities.json")
    meta_missing = [c["id"] for c in catalog if str(c["id"]) not in metadata]
    meta_no_games = [c["id"] for c in catalog
                     if str(c["id"]) in metadata and not metadata[str(c["id"])].get("games")]
    ident_missing = [c["id"] for c in catalog if str(c["id"]) not in identities]
    # identities.json is scoped to Champions forms by design (canonical/showdown
    # mapping for the 354 roster entries); absence elsewhere is expected, not a gap.
    # The meaningful checks are roster coverage and stray entries.
    roster_pre = load("champions-roster.json").get("forms", {})
    ident_roster_missing = [int(k) for k in roster_pre if k not in identities]
    ident_stray = [int(k) for k in identities if k not in roster_pre]
    gen_dist = Counter()
    for c in catalog:
        m = metadata.get(str(c["id"]))
        if m:
            gen_dist[m.get("generation")] += 1
    rep["metadata_identities"] = {
        "metadata_entries": len(metadata), "identities_entries": len(identities),
        "catalog_ids_missing_metadata": meta_missing,
        "catalog_ids_with_empty_games": len(meta_no_games),
        "identities_scope_note": "identities.json covers Champions roster forms only, by design",
        "roster_ids_missing_identity_entry": ident_roster_missing,
        "identity_entries_outside_roster": ident_stray,
        "identities_without_canonicalName": [int(k) for k, v in identities.items()
                                             if not v.get("canonicalName")],
        "generation_distribution": {str(k): v for k, v in sorted(gen_dist.items(), key=lambda x: (x[0] is None, x[0]))},
    }

    # ---- 3. Champions roster + learnsets ----
    roster = load("champions-roster.json")
    roster_forms = roster.get("forms", {})
    roster_ids = [int(k) for k in roster_forms]
    roster_sources = Counter(v.get("source", "") for v in roster_forms.values())
    roster_not_in_catalog = [i for i in roster_ids if i not in set(ids)]
    champ_flag_not_in_roster = [c["id"] for c in champions if str(c["id"]) not in roster_forms]
    roster_not_flagged = [i for i in roster_ids if i not in {c["id"] for c in champions}]
    champ_learn = load("champions-learnsets.json")
    champ_learn_ids = {int(k) for k in champ_learn}
    champ_empty_learn = [i for i in roster_ids if not champ_learn.get(str(i))]
    rep["champions"] = {
        "roster_entries": len(roster_forms), "roster_sources": dict(roster_sources),
        "roster_ids_not_in_catalog": roster_not_in_catalog,
        "champions_flagged_not_in_roster": champ_flag_not_in_roster,
        "roster_ids_without_champions_flag": roster_not_flagged,
        "learnset_ids": len(champ_learn_ids),
        "roster_ids_with_empty_learnset": champ_empty_learn,
        "learnset_ids_not_in_roster": sorted(champ_learn_ids - set(roster_ids)),
    }

    # ---- 4. Moves ----
    moves = load("moves.json")
    move_details = load("move-details.json")
    champ_moves = load("champions-move-data.json")
    move_ids = {int(k) for k in moves}
    detail_ids = {int(k) for k in move_details}
    champ_move_ids = {int(k) for k in champ_moves}
    move_field_fill = {}
    for field in ("type", "category", "power", "accuracy", "pp"):
        move_field_fill[field] = {"filled": sum(1 for v in moves.values() if v.get(field) is not None),
                                  "total": len(moves)}
    detail_fill = {}
    for field in ("description", "summary", "championsDescription", "championsSummary", "target"):
        detail_fill[field] = {"filled": sum(1 for v in move_details.values() if v.get(field)),
                              "total": len(move_details)}
    detail_lang = {}
    loc = load("localized-descriptions.json")
    distinct_texts = set()
    for v in move_details.values():
        for f in ("description", "summary", "championsDescription", "championsSummary"):
            if v.get(f):
                distinct_texts.add(v[f])
    detail_lang = {
        "distinct_texts": len(distinct_texts),
        "with_pt_and_es": sum(1 for t in distinct_texts
                              if t in loc and loc[t].get("pt") and loc[t].get("es")),
        "without_translation": sorted(t for t in distinct_texts if t not in loc),
    }
    champ_desc_missing = [i for i in champ_move_ids if not move_details.get(str(i), {}).get("championsDescription")]
    rep["moves"] = {
        "moves_entries": len(moves), "move_details_entries": len(move_details),
        "champions_move_entries": len(champ_moves),
        "moves_without_details": sorted(move_ids - detail_ids),
        "details_without_moves_entry": sorted(detail_ids - move_ids),
        "champions_moves_without_details_entry": sorted(champ_move_ids - detail_ids),
        "move_field_fill": move_field_fill, "detail_field_fill": detail_fill,
        "detail_description_translations": detail_lang,
        "champions_moves_missing_championsDescription": len(champ_desc_missing),
    }

    # ---- 5. Items ----
    items = load("items.json")
    general = load("general-items.json")
    general_items = general["items"]
    held_champ = [v for v in items.values() if v.get("champions") and v.get("held")]
    item_desc_fill = {"filled": sum(1 for v in items.values() if v.get("description")), "total": len(items)}
    general_desc = {}
    for lang in ("pt", "en", "es"):
        general_desc[lang] = sum(1 for v in general_items
                                 if isinstance(v.get("description"), dict) and v["description"].get(lang))
    general_no_desc = [v["id"] for v in general_items if not v.get("description")]
    rep["items"] = {
        "items_entries": len(items), "held_champions_items": len(held_champ),
        "items_with_description": item_desc_fill,
        "general_items_entries": len(general_items),
        "general_description_fill": general_desc,
        "general_items_without_description": general_no_desc,
        "general_duplicate_ids": [i for i, n in Counter(v["id"] for v in general_items).items() if n > 1],
    }

    # ---- 6. Species profiles + abilities ----
    profiles_root = load("species-profiles.json")
    profiles = profiles_root["profiles"]
    abilities_root = load("ability-profiles.json")
    abilities = abilities_root["abilities"]
    profile_ids = {int(k) for k in profiles}
    profile_missing = [c["id"] for c in catalog if c["id"] not in profile_ids]
    prof_fill = {}
    for field in ("category", "genderRateEighths", "growthRate", "captureRate",
                  "baseExperience", "hatchCounter", "baseHappiness"):
        prof_fill[field] = {"filled": sum(1 for v in profiles.values() if v.get(field) is not None),
                            "total": len(profiles)}
    egg_empty = [int(k) for k, v in profiles.items() if not v.get("eggGroups")]
    prof_abilities_empty = [int(k) for k, v in profiles.items() if not v.get("abilities")]
    form_unresolved = [int(k) for k, v in profiles.items() if not v.get("formResolved")]
    ability_desc_missing = [k for k, v in abilities.items() if not v.get("description")]
    ability_desc_lang = {}
    for lang in ("pt", "en", "es"):
        ability_desc_lang[lang] = sum(1 for v in abilities.values()
                                      if isinstance(v.get("description"), dict) and v["description"].get(lang))
    def akey(s):
        return re.sub(r"[^a-z0-9]", "", (s or "").lower())
    ability_keys = {akey(v["name"]) for v in abilities.values()} | {akey(v["slug"]) for v in abilities.values()}
    app_ability_names = set()
    for c in catalog:
        app_ability_names.update(c["abilities"])
    ability_id_to_name = {v.get("id"): v.get("name") for v in abilities.values()}
    for v in profiles.values():
        for a in v.get("abilities", []):
            nm = ability_id_to_name.get(a.get("id")) or a.get("name")
            if nm:
                app_ability_names.add(nm)
    app_abilities_without_profile = sorted(a for a in app_ability_names if akey(a) not in ability_keys)
    whitespace_abilities = sorted({a for c in catalog for a in c["abilities"] if a != a.strip()})
    rep["profiles_abilities"] = {
        "profile_entries": len(profiles), "ability_entries": len(abilities),
        "catalog_ids_without_profile": profile_missing,
        "profile_field_fill": prof_fill,
        "profiles_with_empty_eggGroups": egg_empty,
        "profiles_with_empty_abilities": prof_abilities_empty,
        "profiles_form_unresolved": form_unresolved,
        "abilities_without_description": len(ability_desc_missing),
        "ability_description_lang_fill": ability_desc_lang,
        "app_used_ability_names": len(app_ability_names),
        "app_used_abilities_without_profile_entry": app_abilities_without_profile,
        "ability_strings_with_leading_trailing_whitespace": whitespace_abilities,
    }

    # ---- 7. Evolution families ----
    evo = load("evolution-families.json")
    chains = evo["chains"]
    node_ids = set()
    chain_species = set()
    unknown_nodes = []
    for ch in chains:
        for n in ch["nodes"]:
            node_ids.add(n["pokemonId"])
            chain_species.add(n["speciesId"])
            if n["pokemonId"] not in set(ids):
                unknown_nodes.append(n["pokemonId"])
    singleton = sum(1 for ch in chains if not ch["edges"])
    rep["evolution"] = {
        "chains": len(chains), "nodes": len(node_ids),
        "distinct_species_in_chains": len(chain_species),
        "species_without_chain": sorted(species - chain_species),
        "node_ids_not_in_catalog": sorted(set(unknown_nodes)),
        "singleton_chains_no_edges": singleton,
    }

    # ---- 8. Learnsets (historical) ----
    learn_dir = os.path.join(ASSETS, "learnsets")
    learn_files = {int(f[:-5]) for f in os.listdir(learn_dir) if f.endswith(".json")}
    learn_missing = [c["id"] for c in catalog if c["id"] not in learn_files]
    game_coverage = Counter()
    empty_files = []
    unknown_learn_moves = set()
    for fid in learn_files:
        with open(os.path.join(learn_dir, f"{fid}.json"), encoding="utf-8") as f:
            rows = json.load(f)
        if not rows:
            empty_files.append(fid)
        for row in rows:
            if row["id"] not in move_ids and row["id"] not in champ_move_ids:
                unknown_learn_moves.add(row["id"])
            for v in row.get("versions", []):
                game_coverage[v.get("game")] += 1
    rep["learnsets"] = {
        "files": len(learn_files), "catalog_ids_without_file": learn_missing,
        "empty_files": empty_files,
        "games_covered": {str(k): v for k, v in sorted(game_coverage.items())},
        "learnset_move_ids_unknown": sorted(unknown_learn_moves),
    }

    # ---- 9. Art + item art ----
    art_dir = os.path.join(ASSETS, "art")
    art_ids = {int(f[:-5]) for f in os.listdir(art_dir) if f.endswith(".webp")}
    art_missing = [c["id"] for c in catalog if c["id"] not in art_ids]
    art_extra = sorted(art_ids - set(ids))
    item_art_dir = os.path.join(ASSETS, "item-art")
    item_art_ids = {int(f[:-4]) for f in os.listdir(item_art_dir) if f.endswith(".png")}
    held_ids = {v["id"] for v in held_champ}
    rep["art"] = {
        "art_files": len(art_ids), "catalog_ids_without_art": art_missing,
        "art_files_without_catalog_entry": art_extra,
        "item_art_files": len(item_art_ids),
        "held_item_ids_without_icon": sorted(held_ids - item_art_ids),
    }

    # ---- 10. Calc engine maps ----
    calc_map = load("calc-species-map.json")
    calc_default = load("calc-default-abilities.json")
    champ_no_calc = [c["id"] for c in champions if str(c["id"]) not in calc_map]
    rep["calc"] = {
        "calc_species_map_entries": len(calc_map),
        "champions_ids_without_calc_mapping": champ_no_calc,
        "calc_default_abilities_entries": len(calc_default),
    }

    # ---- 11. Type chart verification (parsed from TypeAnalysis.kt) ----
    kt_path = os.path.join(ROOT, "android", "app", "src", "main", "java",
                           "dev", "bloody", "prismadex", "data", "TypeAnalysis.kt")
    with open(kt_path, encoding="utf-8") as f:
        kt = f.read()
    chart = {}
    for m in re.finditer(r'"(\w+)" to listOf\(([^)]*)\)', kt):
        chart[m.group(1)] = [float(x) for x in m.group(2).split(",")]
    chart_errors = []
    if set(chart) != set(KNOWN_TYPES):
        chart_errors.append(f"chart keys != 18 types: {sorted(set(chart) ^ set(KNOWN_TYPES))}")
    for t, row in chart.items():
        if len(row) != 18:
            chart_errors.append(f"{t}: row length {len(row)}")
        if any(v not in (0.0, 0.5, 1.0, 2.0) for v in row):
            chart_errors.append(f"{t}: unexpected values {sorted(set(row))}")
    idx = {t: i for i, t in enumerate(KNOWN_TYPES)}

    def mult(atk, defs):
        v = 1.0
        for d in defs:
            v *= chart[atk][idx[d]]
        return v

    spot = [("electric", ["ground"], 0.0), ("electric", ["flying"], 2.0),
            ("electric", ["water", "flying"], 4.0), ("ground", ["flying"], 0.0),
            ("ground", ["electric", "flying"], 0.0), ("ghost", ["normal"], 0.0),
            ("ghost", ["normal", "psychic"], 0.0), ("normal", ["ghost"], 0.0),
            ("fighting", ["ghost"], 0.0), ("fighting", ["rock", "dark"], 4.0),
            ("dragon", ["fairy"], 0.0), ("dragon", ["steel"], 0.5),
            ("poison", ["steel"], 0.0), ("psychic", ["dark"], 0.0),
            ("fairy", ["dragon"], 2.0), ("fairy", ["fire"], 0.5),
            ("ice", ["dragon", "flying"], 4.0), ("fire", ["grass", "ice"], 4.0),
            ("bug", ["psychic"], 2.0), ("rock", ["fire", "flying"], 4.0),
            ("dark", ["psychic", "ghost"], 4.0), ("steel", ["ice", "fairy"], 4.0),
            ("water", ["fire", "ground"], 4.0), ("grass", ["water", "ground"], 4.0)]
    for atk, defs, want in spot:
        got = mult(atk, defs)
        if got != want:
            chart_errors.append(f"{atk} vs {defs}: got {got}, want {want}")
    rep["type_chart"] = {"rows": len(chart), "spot_checks": len(spot), "errors": chart_errors}
    if chart_errors:
        rep["gaps"].extend(chart_errors)

    # ---- 12. Misc: games, localized descriptions, historical overrides, announced ----
    games = load("games.json")
    loc = load("localized-descriptions.json")
    hist = load("historical-move-overrides.json")
    announced = load("announced-pokemon.json")
    rep["misc"] = {
        "games_entries": games if isinstance(games, list) else len(games),
        "localized_descriptions_keys": len(loc),
        "historical_override_games": sorted(hist.get("games", {}).keys()),
        "announced_pokemon": announced,
    }

    with open(OUT_JSON, "w", encoding="utf-8") as f:
        json.dump(rep, f, indent=1, ensure_ascii=False, sort_keys=True)
    return rep


def matrix(rep):
    c = rep["catalog"]
    lines = []
    lines.append("# Matriz de cobertura — auditoria da expansão (ativos empacotados, como o app carrega)")
    lines.append("")
    lines.append("Gerado por `scripts/audit/expansao-audit.py`. Detalhes em `research/expansao-audit-coverage.json`.")
    lines.append("")
    lines.append("## Catálogo")
    lines.append(f"- data.json: {c['data_json_rows']} registros; formas adicionais válidas: {c['additional_valid']} de {c['additional_records']} (rejeitadas: {c['additional_rejected']}).")
    lines.append(f"- Total combinado: {c['total_records']} registros, {c['distinct_species_baseId']} espécies distintas (baseId).")
    lines.append(f"- Champions: {c['champions_records']} registros; Mega: {c['mega_records']}.")
    lines.append(f"- IDs duplicados: {c['duplicate_ids'] or 'nenhum'}. Estatísticas inválidas: {c['bad_stats_ids'] or 'nenhuma'}. Tipos inválidos: {c['bad_types_ids'] or 'nenhum'}.")
    lines.append(f"- Habilidades vazias: {len(c['empty_abilities_ids'])} ids. Altura nula: {c['null_height']}; peso nulo: {c['null_weight']}.")
    lines.append(f"- Nomes duplicados (formas distintas com o mesmo nome): {c['duplicate_names'] or 'nenhum'}.")
    lines.append("")
    lines.append("## Metadados / identidades")
    m = rep["metadata_identities"]
    lines.append(f"- metadata: {m['metadata_entries']} entradas; sem metadados: {m['catalog_ids_missing_metadata'] or 'nenhum'} (formas adicionais carregam geração/jogos no próprio registro).")
    lines.append(f"- Sem lista de jogos: {m['catalog_ids_with_empty_games']} ids. Identidades: {m['identities_entries']} entradas, escopo Champions por desenho; roster sem entrada: {m['roster_ids_missing_identity_entry'] or 'nenhum'}; fora do roster: {m['identity_entries_outside_roster'] or 'nenhum'}.")
    lines.append(f"- Distribuição por geração: {m['generation_distribution']}.")
    lines.append("")
    lines.append("## Champions")
    ch = rep["champions"]
    lines.append(f"- Roster: {ch['roster_entries']} entradas (fontes: {ch['roster_sources']}).")
    lines.append(f"- Roster fora do catálogo: {ch['roster_ids_not_in_catalog'] or 'nenhum'}. Flag Champions fora do roster: {ch['champions_flagged_not_in_roster'] or 'nenhum'}.")
    lines.append(f"- Roster sem flag Champions: {ch['roster_ids_without_champions_flag'] or 'nenhum'}. Learnsets vazios no roster: {len(ch['roster_ids_with_empty_learnset'])}.")
    lines.append("")
    lines.append("## Golpes / itens")
    mv = rep["moves"]
    lines.append(f"- moves: {mv['moves_entries']}; detalhes: {mv['move_details_entries']}; Champions: {mv['champions_move_entries']}.")
    lines.append(f"- Golpes sem detalhe: {len(mv['moves_without_details'])}; detalhes sem golpe: {len(mv['details_without_moves_entry'])}.")
    lines.append(f"- Preenchimento de campos: {mv['move_field_fill']}; detalhes: {mv['detail_field_fill']}.")
    it = rep["items"]
    lines.append(f"- items: {it['items_entries']} ({it['held_champions_items']} seguráveis Champions); gerais: {it['general_items_entries']}, sem descrição: {len(it['general_items_without_description'])}.")
    lines.append("")
    lines.append("## Perfis / habilidades")
    pr = rep["profiles_abilities"]
    lines.append(f"- Perfis: {pr['profile_entries']}; sem perfil: {len(pr['catalog_ids_without_profile'])} ids.")
    lines.append(f"- Preenchimento: {pr['profile_field_fill']}.")
    lines.append(f"- Formas não resolvidas: {len(pr['profiles_form_unresolved'])}; habilidades sem descrição: {pr['abilities_without_description']}.")
    lines.append(f"- Habilidades usadas no app sem entrada de perfil (comparação normalizada): {pr['app_used_abilities_without_profile_entry'] or 'nenhuma'}. Strings com espaço nas bordas: {pr['ability_strings_with_leading_trailing_whitespace'] or 'nenhuma'}.")
    lines.append(f"- Traduções de textos de golpes: {mv['detail_description_translations']['with_pt_and_es']}/{mv['detail_description_translations']['distinct_texts']} textos distintos com PT+ES.")
    lines.append("")
    lines.append("## Evolução / learnsets / arte / motor")
    ev = rep["evolution"]
    lines.append(f"- Cadeias: {ev['chains']}; espécies sem cadeia: {len(ev['species_without_chain'])}; nós fora do catálogo: {ev['node_ids_not_in_catalog'] or 'nenhum'}.")
    ln = rep["learnsets"]
    lines.append(f"- Arquivos learnset: {ln['files']}; ids sem arquivo: {len(ln['catalog_ids_without_file'])}; jogos cobertos: {ln['games_covered']}.")
    ar = rep["art"]
    lines.append(f"- Artes: {ar['art_files']} arquivos; ids sem arte (placeholder): {len(ar['catalog_ids_without_art'])}; ícones de item sem arquivo: {len(ar['held_item_ids_without_icon'])}.")
    ca = rep["calc"]
    lines.append(f"- Motor: {ca['calc_species_map_entries']} mapeamentos; Champions sem mapeamento: {len(ca['champions_ids_without_calc_mapping'])}.")
    tc = rep["type_chart"]
    lines.append(f"- Tabela de tipos: {tc['rows']} linhas, {tc['spot_checks']} verificações pontuais, erros: {tc['errors'] or 'nenhum'}.")
    lines.append("")
    lines.append("## Lacunas registradas")
    for g in rep["gaps"]:
        lines.append(f"- {g}")
    if not rep["gaps"]:
        lines.append("- Nenhuma lacuna estrutural detectada por esta auditoria automatizada.")
    with open(OUT_MD, "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")


if __name__ == "__main__":
    rep = main()
    matrix(rep)
    print(json.dumps({k: rep[k] for k in ("catalog", "champions", "type_chart")}, indent=1, ensure_ascii=False)[:2000])
    print("GAPS:", rep["gaps"][:10])
    print("WROTE", OUT_JSON, "and", OUT_MD)
