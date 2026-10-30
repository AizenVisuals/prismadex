#!/usr/bin/env python3
"""Fill null exact-form height/weight in data.json from verified species profiles.

Only fills nulls, only from profiles with formResolved=true (exact-form facts).
Never overwrites an existing value, never touches stats, types, abilities
membership, champions flags or any other field. Also strips leading/trailing
whitespace from ability strings (known typo: 'galvanize ').

Source: species-profiles.json, built from pinned PokeAPI CSVs (BSD-3-Clause,
commit 2bf639e5572812a67d2b94903fdcb9d095f5aeb2) — see
scripts/data-import/README.md. Unresolved forms (4) keep null honestly.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
A = ROOT / "android/app/src/main/assets"
HERE = Path(__file__).parent

rows = json.loads((A / "data.json").read_text())
profiles = json.loads((A / "species-profiles.json").read_text())["profiles"]

before = {r["id"]: dict(r) for r in rows}
changes = []
ability_fixes = []
for row in rows:
    prof = profiles.get(str(row["id"]))
    if prof and prof.get("formResolved"):
        for catalog_key, profile_key in (("height", "heightMeters"), ("weight", "weightKg")):
            value = prof.get(profile_key)
            if row.get(catalog_key) is None and value is not None:
                row[catalog_key] = value
                changes.append({"id": row["id"], "name": row["name"],
                                "field": catalog_key, "value": value,
                                "source": "species-profiles.json exact-form fact"})
    cleaned = [a.strip() for a in row["abilities"]]
    if cleaned != row["abilities"]:
        ability_fixes.append({"id": row["id"], "name": row["name"],
                              "before": row["abilities"], "after": cleaned})
        row["abilities"] = cleaned

# Safety: nothing else may differ.
for row in rows:
    old = before[row["id"]]
    assert set(row) == set(old), f"key set changed for {row['id']}"
    for key, value in row.items():
        if key in ("height", "weight"):
            assert value == old[key] or old[key] is None, f"overwrite {row['id']} {key}"
            if value != old[key]:
                assert any(c["id"] == row["id"] and c["field"] == key for c in changes)
        elif key == "abilities":
            assert value == [a.strip() for a in old[key]]
        else:
            assert value == old[key], f"unexpected change {row['id']} {key}"

(A / "data.json").write_text(json.dumps(rows, ensure_ascii=False, separators=(",", ":")) + "\n")
(HERE / "physical-sync-report.json").write_text(json.dumps(
    {"source": "species-profiles.json (pinned PokeAPI, formResolved only)",
     "heightWeightFills": changes, "abilityWhitespaceFixes": ability_fixes},
    indent=2, ensure_ascii=False) + "\n")
print(f"height/weight fills: {len(changes)}; ability whitespace fixes: {len(ability_fixes)}")
for c in changes[:5]:
    print(c)
