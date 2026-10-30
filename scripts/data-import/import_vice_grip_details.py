#!/usr/bin/env python3
"""Add the single missing move-details row: id 11, Vice Grip.

Sources (pinned, MIT / BSD-3-Clause — see provenance file written here):
- Pokemon Showdown data/moves.ts @ 1242c1e2d9763ba5b85d33f13aecb89bae19ae3d
  (entry `visegrip`: Showdown spells the move "Vise Grip")
- Pokemon Showdown data/text/moves.ts @ same commit (shortDesc only; no desc)
- PokeAPI moves.csv + move_effect_prose.csv @ 2bf639e5572812a67d2b94903fdcb9d095f5aeb2
  (row 11: vice-grip, effect_id 1 = "no additional effect")

Convention (same as Pound, id 1): description = desc ?? shortDesc,
summary = shortDesc; Champions fields duplicate the general text because this
is not a Champions move. The text "No additional effect." already has PT+ES
translations in localized-descriptions.json, so no new translation is needed.
"""
import hashlib
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
A = ROOT / "android/app/src/main/assets"
CACHE = Path(__file__).parent / "cache"
SHOWDOWN_COMMIT = "1242c1e2d9763ba5b85d33f13aecb89bae19ae3d"
POKEAPI_COMMIT = "2bf639e5572812a67d2b94903fdcb9d095f5aeb2"


def sha256(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


moves_ts = (CACHE / "showdown-moves.ts").read_text()
m = re.search(r"\n\tvisegrip: \{(.*?)\n\t\},", moves_ts, re.S)
assert m, "visegrip entry not found in showdown-moves.ts"
entry = m.group(1)

def field(name, text=entry):
    mm = re.search(rf"\n\t\t{name}: ([^,\n]+),?", text)
    assert mm, f"field {name} missing"
    return mm.group(1).strip()

assert field("num") == "11"
assert field("accuracy") == "100"
assert field("basePower") == "55"
assert field("pp") == "30"
assert field("priority") == "0"
assert field("category") == '"Physical"'
assert field("type") == '"Normal"'
assert field("target") == '"normal"'
flags = re.search(r"flags: \{([^}]*)\}", entry).group(1)
flag_names = sorted(f.strip().split(":")[0] for f in flags.split(",") if f.strip())
assert flag_names == ["contact", "metronome", "mirror", "protect"], flag_names

text_ts = (CACHE / "showdown-move-text.ts").read_text()
t = re.search(r"\n\tvisegrip: \{(.*?)\n\t\},", text_ts, re.S)
assert t, "visegrip text entry not found"
assert "desc:" not in t.group(1), "unexpected desc field"
short = re.search(r'shortDesc: "([^"]+)"', t.group(1)).group(1)
assert short == "No additional effect.", short

# PokeAPI cross-check: moves.csv row 11 + effect 1 prose (EN).
moves_csv = (CACHE / "moves.csv").read_text().splitlines()
row11 = [ln.split(",") for ln in moves_csv if ln.startswith("11,")][0]
assert row11[1] == "vice-grip" and row11[4] == "55" and row11[5] == "30"
assert row11[6] == "100" and row11[7] == "0" and row11[10] == "1", row11
prose = [ln for ln in (CACHE / "move_effect_prose.csv").read_text().splitlines()
         if ln.startswith("1,9,")][0]
assert "no additional effect" in prose.lower(), prose

# moves.json row 11 must agree.
moves = json.loads((A / "moves.json").read_text())
assert moves["11"] == {"id": 11, "name": "Vice Grip", "power": 55, "accuracy": 100,
                       "pp": 30, "type": "normal", "category": "physical"}

# Translation must already exist (no new strings introduced).
loc = json.loads((A / "localized-descriptions.json").read_text())
assert loc.get(short, {}).get("pt") and loc.get(short, {}).get("es"), short

details = json.loads((A / "move-details.json").read_text())
assert "11" not in details, "row 11 already present — refusing to overwrite"
details["11"] = {
    "description": short,
    "summary": short,
    "championsDescription": short,
    "championsSummary": short,
    "priority": 0,
    "target": "normal",
    "flags": {"contact": 1, "protect": 1, "mirror": 1, "metronome": 1},
    "championsTextOverride": False,
}
(A / "move-details.json").write_text(
    json.dumps(details, ensure_ascii=False, separators=(",", ":")) + "\n")

provenance = {
    "moveId": 11, "moveName": "Vice Grip",
    "showdownCommit": SHOWDOWN_COMMIT, "showdownLicense": "MIT",
    "showdownMovesTs": {"file": "cache/showdown-moves.ts",
                        "sha256": sha256(CACHE / "showdown-moves.ts")},
    "showdownMoveTextTs": {"file": "cache/showdown-move-text.ts",
                           "sha256": sha256(CACHE / "showdown-move-text.ts")},
    "pokeapiCommit": POKEAPI_COMMIT, "pokeapiLicense": "BSD-3-Clause",
    "pokeapiMovesCsv": {"file": "cache/moves.csv", "sha256": sha256(CACHE / "moves.csv")},
    "pokeapiMoveEffectProseCsv": {"file": "cache/move_effect_prose.csv",
                                  "sha256": sha256(CACHE / "move_effect_prose.csv")},
    "crossChecks": ["showdown num/accuracy/basePower/pp/priority/category/type/target == moves.json row 11",
                    "pokeapi moves.csv row 11 agrees on power/pp/accuracy/priority/effect_id 1",
                    "pokeapi effect 1 prose: no additional effect",
                    "showdown text has shortDesc only, no desc; Pound convention applied"],
    "translation": "No additional effect. already translated (pt/es) in localized-descriptions.json",
}
(Path(__file__).parent / "showdown-moves-provenance.json").write_text(
    json.dumps(provenance, indent=2, ensure_ascii=False) + "\n")
print("Vice Grip details row added; provenance recorded.")
