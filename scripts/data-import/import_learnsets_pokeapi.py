#!/usr/bin/env python3
"""Merge-only learnset import from pinned PokeAPI CSVs (same source as the
existing import scripts in this directory).

Source: PokeAPI/pokeapi commit 2bf639e5572812a67d2b94903fdcb9d095f5aeb2
(BSD-3-Clause), CSV files under data/v2/csv/. SHA-256 hashes below are
verified on every run; a mismatch aborts before any file is touched.

Rules (additive only, never invent data):
- Only PokeAPI version groups mapped to app games 1..24. Version group 32
  (Champions) is EXCLUDED: the app's Champions learnsets come from
  Showdown data (champions-learnsets.json) and are kept separate.
  DLC groups with no rows at this commit (Isle of Armor, Crown Tundra,
  Teal Mask, Indigo Disk) and the Japan-only Gen 1 groups are not merged.
- Each PokeAPI pokemon id maps to exactly ONE canonical app record
  (the record whose id equals the PokeAPI id, or the unique form
  mapping). Displacement forms that share a base species' PokeAPI id
  (20011011/20011012) receive no copied rows: no distinct source rows
  exist for them. Catalog ids with no PokeAPI mapping at all
  (20077/20079/20201/20202) are left untouched and reported.
- Existing version entries are never removed or rewritten. Missing
  (game, method, level) entries are appended. Method representation
  follows the file convention per game: names for games 1..16 and 24,
  numeric strings for games 17..23 (the parser accepts both).
- Level-up rows at level 0 are PokeAPI "start move" rows. When the same
  triple already has a level-1 entry, the level-0 row is skipped: the
  UI renders both as separate labels and the level-1 row already
  covers the move. Skipped rows are counted and reported.
- Z-moves (622..657) and Shadow moves (10001..10018) have NO rows in
  pokemon_moves.csv at this commit, so no learnset data exists to fetch
  for them. They are reported, not added.
- Game-25 entries already present in learnset files are dead data for
  the UI and are left untouched.

Usage:
  python3 import_learnsets_pokeapi.py            # dry run, prints summary
  python3 import_learnsets_pokeapi.py --apply    # writes learnset files
"""
import argparse
import csv
import hashlib
import json
import os
import re
import sys
import unicodedata
import urllib.request
from collections import Counter, defaultdict

COMMIT = "2bf639e5572812a67d2b94903fdcb9d095f5aeb2"
BASE_URL = f"https://raw.githubusercontent.com/PokeAPI/pokeapi/{COMMIT}/data/v2/csv/"
FILES = {
    "pokemon_moves.csv": "22a807cef26891eeac0d0c900bd363e66baf421f7ee795bc7bc3e718f23b939e",
    "pokemon_move_methods.csv": "78b75acfcf6dca4c82da9d4851357cbbd65e61b08154eddfc2ace0efd00e408e",
    "pokemon.csv": "16c81c33188b0eac403aa2f759fcbe9e42c611f722d263f5b5a6a5bff9f8ce6b",
    "moves.csv": "8aafd37bf78f19471495c05b201545180f50f0a08a2a2a844d69f9837dd39ac9",
    "version_groups.csv": "28da8d89d8eb4966941f81a9e62b3990510ed4d76dd774158246551a8e7707a7",
}
CACHE_DIR = "/tmp/prismadex-learnset-import"

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(ROOT, "android", "app", "src", "main", "assets")
LEARNSET_DIR = os.path.join(ASSETS, "learnsets")

NUM_TO_NAME = {
    "1": "level-up", "2": "egg", "3": "tutor", "4": "machine",
    "5": "stadium-surfing-pikachu", "6": "light-ball-egg",
    "7": "colosseum-purification", "8": "xd-shadow", "9": "xd-purification",
    "10": "form-change", "11": "zygarde-cube", "12": "train",
}
NAME_TO_NUM = {v: k for k, v in NUM_TO_NAME.items()}
# Games whose learnset files use numeric method strings.
NUMERIC_GAMES = set(range(17, 24))


def sha256_file(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def fetch_all():
    os.makedirs(CACHE_DIR, exist_ok=True)
    paths = {}
    for name, digest in FILES.items():
        path = os.path.join(CACHE_DIR, name)
        if not os.path.exists(path) or sha256_file(path) != digest:
            urllib.request.urlretrieve(BASE_URL + name, path)
        actual = sha256_file(path)
        if actual != digest:
            sys.exit(f"SHA-256 MISMATCH for {name}: got {actual}, want {digest}")
        paths[name] = path
    return paths


def norm(s):
    return re.sub(r"[^a-z0-9]", "", unicodedata.normalize("NFKD", s).lower())


def load_json(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--apply", action="store_true")
    args = ap.parse_args()
    paths = fetch_all()

    games = load_json(os.path.join(ASSETS, "games.json"))
    vg = {}
    for r in csv.DictReader(open(paths["version_groups.csv"], encoding="utf-8")):
        vg[r["identifier"]] = int(r["id"])
    hand = {"lets-go-pikachu-eevee": "lets-go-pikachu-lets-go-eevee",
            "legends-z-a": "legends-za"}
    vg_to_app = {}
    for k, v in games.items():
        ident = hand.get(v, v)
        if ident in vg:
            vg_to_app[vg[ident]] = int(k)
    # Champions (VG 32) stays Showdown-sourced; never merge PokeAPI rows.
    vg_to_app.pop(32, None)

    moves_json = load_json(os.path.join(ASSETS, "moves.json"))
    by_name = defaultdict(list)
    for iid, rec in moves_json.items():
        by_name[norm(rec["name"])].append(int(iid))
    src2int = {}
    for r in csv.DictReader(open(paths["moves.csv"], encoding="utf-8")):
        cands = by_name.get(norm(r["identifier"]), [])
        if len(cands) == 1:
            src2int[int(r["id"])] = cands[0]

    # Canonical mapping: one app id per PokeAPI pokemon id. A record whose
    # app id equals the PokeAPI id wins; otherwise the unique mapping wins.
    # Forms sharing a base id (20011011/20011012) are NOT canonical.
    fmap = load_json(os.path.join(ROOT, "scripts", "data-import", "form-id-map.json"))
    addforms = load_json(os.path.join(ASSETS, "additional-form-profiles.json"))["records"]
    candidates = defaultdict(list)
    for appid_s, rec in fmap.items():
        candidates[int(rec["pokeapiId"])].append(int(appid_s))
    for rec in addforms:
        candidates[int(rec["pokeapiId"])].append(int(rec["id"]))
    poke2app = {}
    shared = {}
    for pid, appids in candidates.items():
        uniq = sorted(set(appids))
        if len(uniq) == 1:
            poke2app[pid] = uniq[0]
        elif pid in uniq:
            poke2app[pid] = pid
            shared[pid] = uniq
        else:
            shared[pid] = uniq  # no canonical record; nobody receives rows
    catalog = {p["id"] for p in load_json(os.path.join(ASSETS, "data.json"))}
    catalog |= {r["id"] for r in addforms}
    mapped_appids = {int(k) for k in fmap} | {int(r["id"]) for r in addforms}
    no_mapping = sorted(catalog - mapped_appids)
    # Note: catalog ids that only appear as non-canonical shared forms.
    non_canonical = sorted({a for appids in shared.values() for a in appids} - set(poke2app.values()))

    methods = {int(r["id"]): r["identifier"]
               for r in csv.DictReader(open(paths["pokemon_move_methods.csv"], encoding="utf-8"))}
    desired = defaultdict(list)  # appid -> [(game, method_name, level)]
    unmapped_pokemon_rows = 0
    unmapped_pokemon_ids = set()
    for r in csv.DictReader(open(paths["pokemon_moves.csv"], encoding="utf-8")):
        vg_id = int(r["version_group_id"])
        if vg_id not in vg_to_app:
            continue
        appgame = vg_to_app[vg_id]
        if appgame == 25:
            continue
        appid = poke2app.get(int(r["pokemon_id"]))
        if appid is None:
            unmapped_pokemon_rows += 1
            unmapped_pokemon_ids.add(int(r["pokemon_id"]))
            continue
        imid = src2int.get(int(r["move_id"]))
        if imid is None:
            continue  # Z/Shadow moves: no rows exist for them anyway (verified)
        meth = methods[int(r["pokemon_move_method_id"])]
        lvl = int(r["level"]) if r["level"] else 0
        desired[appid].append((appgame, meth, lvl, imid))
    # Re-key by (appid, move) -> set of (game, method, level)
    desired_by_move = defaultdict(lambda: defaultdict(set))
    for appid, rows in desired.items():
        for game, meth, lvl, imid in rows:
            desired_by_move[appid][imid].add((game, meth, lvl))

    added_per_game = Counter()
    skipped_level0 = Counter()
    new_files = []
    changed_files = []
    roundtrip_mismatch = []
    per_pokemon_added = {}
    for appid in sorted(set(desired_by_move) | catalog):
        path = os.path.join(LEARNSET_DIR, f"{appid}.json")
        existing_rows = []
        raw = None
        if os.path.exists(path):
            with open(path, "rb") as f:
                raw = f.read()
            existing_rows = json.loads(raw.decode("utf-8"))
            if json.dumps(existing_rows, separators=(",", ":"), ensure_ascii=False).encode("utf-8") != raw:
                roundtrip_mismatch.append(appid)
        # Existing (move -> set of normalized (game, method, level))
        existing = {}
        for row in existing_rows:
            s = set()
            for v in row["versions"]:
                s.add((int(v["game"]), NUM_TO_NAME.get(str(v["method"]), str(v["method"])), int(v.get("level", 0))))
            existing[int(row["id"])] = s
        additions = {}  # move -> list of new version dicts
        for imid in sorted(desired_by_move.get(appid, {})):
            have = existing.get(imid, set())
            for (game, meth, lvl) in sorted(desired_by_move[appid][imid]):
                if (game, meth, lvl) in have:
                    continue
                if meth == "level-up" and lvl == 0 and (game, meth, 1) in have:
                    skipped_level0[game] += 1
                    continue
                additions.setdefault(imid, []).append(
                    {"game": game,
                     "method": NAME_TO_NUM[meth] if game in NUMERIC_GAMES else meth,
                     "level": lvl})
                have.add((game, meth, lvl))
                added_per_game[game] += 1
        if not additions:
            continue
        total_added = sum(len(v) for v in additions.values())
        per_pokemon_added[appid] = total_added
        new_rows = list(existing_rows)
        row_by_id = {int(r["id"]): r for r in new_rows}
        for imid in sorted(additions):
            if imid in row_by_id:
                row_by_id[imid]["versions"].extend(additions[imid])
            else:
                new_rows.append({"id": imid, "versions": additions[imid]})
        out = json.dumps(new_rows, separators=(",", ":"), ensure_ascii=False).encode("utf-8")
        if args.apply:
            with open(path, "wb") as f:
                f.write(out)
        if not os.path.exists(path):
            new_files.append(appid)
        else:
            changed_files.append(appid)

    summary = {
        "commit": COMMIT,
        "source_hashes": FILES,
        "added_per_game": dict(sorted(added_per_game.items())),
        "added_total": sum(added_per_game.values()),
        "skipped_level0_dup": dict(sorted(skipped_level0.items())),
        "changed_files": len(changed_files),
        "new_files": sorted(new_files),
        "per_pokemon_added_top": dict(sorted(per_pokemon_added.items(), key=lambda kv: -kv[1])[:20]),
        "unmapped_pokemon_rows_skipped": unmapped_pokemon_rows,
        "unmapped_pokemon_ids": sorted(unmapped_pokemon_ids),
        "shared_pokeapi_ids_no_copy": {str(k): v for k, v in sorted(shared.items())},
        "non_canonical_forms_no_copy": non_canonical,
        "catalog_ids_no_pokeapi_mapping": no_mapping,
        "roundtrip_serialization_mismatch": roundtrip_mismatch,
        "applied": args.apply,
    }
    print(json.dumps(summary, indent=2, ensure_ascii=False))
    if args.apply:
        out_path = os.path.join(ROOT, "research", "LEARNSET-IMPORT-SUMMARY-2026-10-30.json")
        with open(out_path, "w", encoding="utf-8") as f:
            json.dump(summary, f, indent=2, ensure_ascii=False)
        print("summary written to", out_path)


if __name__ == "__main__":
    main()
