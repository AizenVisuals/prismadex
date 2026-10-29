# Reproducible national species profiles

These scripts import structured facts and community-authored ability summaries.
They never scrape PokémonDB, execute downloaded source, import Pokédex flavor
text, alter existing IDs or infer Champions legality. Runtime is entirely offline.

## Sources and licenses

- PokeAPI CSV data: commit `2bf639e5572812a67d2b94903fdcb9d095f5aeb2`,
  repository BSD-3-Clause license. Primary repository license was inspected before
  collection; its full notice is bundled in Android assets. Pokémon names and game
  facts remain associated with their rights holders. Category labels are brief
  structured classification fields; no lore/flavor paragraphs are imported.
- Pokémon Showdown community-written ability effect summaries: MIT commit
  `1242c1e2d9763ba5b85d33f13aecb89bae19ae3d`, `data/text/abilities.ts`.
  Existing bundled Showdown MIT notice applies. These are main-series reference
  effects, explicitly **not** certification of Champions-specific mechanics.
- Exact source URLs, SHA-256 digests and source IDs: `sources.json`,
  `ability-source.json` and `form-id-map.json`.
- `catalog-form-aliases.json` retains previously verified canonical-name mappings
  so the importer does not require private research artifacts.

The PokeAPI Fair Use Policy asks callers to cache downloaded resources. This
pipeline uses pinned GitHub CSV files, local cache, at most three concurrent
requests and no per-Pokémon API fan-out. A downloaded cache with a mismatched
locked digest fails validation. REA is reserved for packaged binaries; it is not
used to pretend that ordinary CSV/source parsing is reverse-engineering evidence.

## Run

Python 3 standard library is sufficient for rebuilding committed data:

```sh
python scripts/data-import/extract_ability_text.py
python scripts/data-import/import_species_profiles.py --fetch
python scripts/data-import/import_additional_forms.py
python scripts/data-import/import_species_profiles.py
python scripts/data-import/validate_profiles.py
```

`--fetch` only acquires missing pinned source files; existing caches are checked
against the lock manifest. On a machine with the committed cache, omit that option
for a network-free regeneration. No npm/Babel/JavaScript execution is required.
`extract_ability_text.py` accepts only literal top-level text properties.

To change the source revision intentionally, update the pinned commit, refresh
its source lock with human review, review exact-form mappings and compare the
coverage report. Never let a new source silently remap existing app IDs.

## New form IDs

The 51 additional PokeAPI records use `300000 + exact upstream pokemon ID`.
Existing native IDs remain untouched, including custom IDs in the tens of
millions. Every generated new ID is checked against the complete existing ID set.
These forms have complete source types and six base stats; a source missing either
would be reported and excluded rather than filled with guessed stats.

They enter the national catalog only: `champions=false`, `games=[]`. An empty
availability list means unverified game eligibility. It must not be widened by
sharing a species or sprite with an existing form. National forms absent from the
art cache must show a neutral placeholder until exact art is supplied.

Cosmetic Poltchageist/Sinistcha variants keep separate native IDs and explicit
PokeAPI form IDs, even though the source assigns the same stat-bearing pokemon ID.

## Text language

English ability summaries are preserved. Names of moves, items, Pokémon and
abilities and numerical values are protected during translation. PT/ES text was
produced locally using the already installed translation runtime; 32 manual
review overrides cover frequent abilities and category labels. The remainder
has **not** received a complete human semantic review. Machine output must not
be represented as an official translation. No model or translator is bundled
with the app; only text fields are shipped.

Regenerating translations is optional for an unchanged source:

```sh
python scripts/data-import/import_species_profiles.py --prepare-translations
# Existing local ctranslate2/sentencepiece runtime + models are required only here:
research/translation-tools/.venv/bin/python scripts/data-import/translate_profiles.py
python scripts/data-import/import_species_profiles.py
```

`translations.json` and `reviewed-translations.json` make ordinary imports
independent of that private development runtime.

## Coverage and honest unknowns

`coverage-report.json` separates species biology from exact-form facts. Species
category, egg groups, gender distribution, growth, catch rate, hatch cycles and
base happiness describe the **species**, including when displayed on a form.
`formGender` separately identifies explicit male/female form identities.

The pinned source documents 1,025 national species; no later species are invented.
For the resulting 1,357 records:

- Species biology: 1,357/1,357.
- Exact-form height/weight: 1,353/1,357 (99.71%).
- Base experience: 1,304/1,357 (96.09%).
- Normal/hidden national ability relations: 1,344/1,357 (99.04%).
- App-used ability effects: 313/313 unique catalog ability slugs (100%), in PT/EN/ES.
- Ability catalog: 376 entries, 317 effect summaries in PT/EN/ES. The 59 missing
  summaries belong to unused spin-off/Conquest entries, not normal hidden-ability
  classification.

Unknown exact profiles: Radiant Sun Solgaleo, Full Moon Lunala, Zygarde Core and
Zygarde Cell. They retain species biology but no guessed physical/ability facts.
Eight newly mapped Mega variants plus original-color Mega Magearna have no ability
relations in the pinned source; existing Champions abilities remain untouched.
The 53 absent base-experience values are explicitly null in the upstream data or
belong to an unmapped exact form. No database migration or account data write is
performed by these importers/repositories.

## Exact additional artwork

```sh
python scripts/data-import/import_additional_art.py
python scripts/data-import/validate_profiles.py
```

Uses pinned PokeAPI/sprites commit
`1dd55c0436d2a5b6e324c0b780e1b1b416fbfb64`, exact official-artwork ID first,
exact sprite ID second. Validates PNG signature and image decoding, converts to
WebP and records both source/output SHA-256. 47 of the 51 additional forms have
exact source art. Four travel forms have neither path and retain a neutral
placeholder: Koraidon limited/swimming and Miraidon low-power/aquatic.
