# PrismaDex Android third-party notices

## Native backdrop rendering

The 30 sources under `app/src/main/java/dev/bloody/prismadex/glass/backdrop/`
are adapted from kota-zo/myspoti's vendored backdrop package:
https://github.com/kota-zo/myspoti
Snapshot commit: `320ba2a8fef4019b6c9da85c7f618399c6e4064a`.

Each file identifies **Kyant0/backdrop v2.0.0**, Copyright 2025 Kyant0,
**Apache License 2.0**: https://github.com/Kyant0/backdrop .
Original provenance and copyright headers are preserved in every file.
They also credit BitChord's vendored tree at commit `edc1e6f` and Myspoti's
subsequent modifications, including effect-resolution scaling, export reuse,
source lifecycle guards and transition-aware recording.

Myspoti's repository-level license is **GNU GPL version 3**. The upstream
Apache header does not remove downstream authorship; retain both license
texts, corresponding source and notices when distributing modified code.
No claim is made that Myspoti's downstream modifications are separately
Apache-licensed. Full license texts are retained in the adjacent `glass/licenses/`
source directory: `backdrop-APACHE-2.0.txt`, `myspoti-GPL-3.0.txt`.

PrismaDex changes (October 2026): package relocation, replacement of Myspoti
logging with Android Log, prevention of recursive sampling when glass is
inside its recorded source. `Glass.kt` adds platform fallbacks, touch springs,
low-resolution effects and PrismaDex surface styling. Record the page as a
sibling of glass overlays, never record the root containing those overlays.

## Material icons

Thirteen additional rounded icon vectors are bundled as individual Kotlin
sources from AndroidX Compose Material Icons 1.7.8, published by Google at
https://dl.google.com/dl/android/maven2/androidx/compose/material/material-icons-extended-android/1.7.8/material-icons-extended-android-1.7.8-sources.jar .
Their Android Open Source Project copyright and Apache-2.0 headers are retained.
Only icons used by Prismadex are included; the complete extended icon font/library
is not shipped. The Compose Material core icons dependency retains its own notices.

## Champions damage calculator

`damage-engine.js` is bundled and transpiled from smogon/damage-calc,
commit `c30417b84b558b407ea50ebba4b4bcc603f6fbff` (MIT, Honko and contributors).
It uses generation 0, the upstream Champions mechanics, rather than Scarlet/Violet
formulas with substituted EVs. `damage-bridge.js` and the Android adapter are
PrismaDex code. The preferred upstream TypeScript source is retained under `vendor/smogon-calc` at the repository root.
Rhino 1.8.0 (Mozilla, MPL-2.0) interprets the local engine on a worker thread;
its host-class bridge is disabled and no page or WebView is used. Rhino is not
modified. Source: https://github.com/mozilla/rhino/tree/Rhino1_8_0_Release .
License copies are bundled under `app/src/main/assets/licenses/`.

## Type badges

18 SVG badges by James Watkins / partywhale, MIT:
https://github.com/partywhale/pokemon-type-icons , commit
`fcbe6978c61c359680bc07636c3f9bdc0f346b43`.
Converted to Android vector drawables, preserving the paths, colors and shapes.
They replace the initial approximate Canvas silhouettes. Pokémon trademarks and
underlying game symbols remain the property of their respective holders.

## Online meta reports

The Meta tab reads the public monthly Smogon Pokémon Showdown usage reports:
https://www.smogon.com/stats/ . Singles selects `championsbss` and Doubles
`championsvgc`; OU is explicitly excluded. Period, regulation, cutoff, battle
count and source URL are shown. This is simulator usage, not the official in-game
ladder. PokéChamp DB is linked separately as a community in-game snapshot resource;
its rankings are not merged with simulator percentages. No advertisements,
analytics scripts or HTML from either site are executed inside PrismaDex.

## Pokémon Showdown Champions data corrections

Species fields, learnsets, move and item definitions: smogon/pokemon-showdown, commit 1242c1e2d9763ba5b85d33f13aecb89bae19ae3d (27 October 2026), MIT. Full text bundled in assets/licenses/showdown-data-MIT.txt. Only literal data and documented PP initialization are incorporated; no battle callbacks are copied into the interface. Eligibility entries are separately attributed to the official Regulation M-C page; Mega and battle-form supplements are labeled as reference data. Corrected Pokémon artwork is retrieved by exact canonical identity from PokeAPI/sprites, with public source context in docs/DADOS.md and the bundled identity mappings; character art remains the property of its respective Pokémon rights holders.

## Held-item sprites and move reference text (0.5.0)

166 held-item PNGs are bundled under `app/src/main/assets/item-art/`, retrieved by exact normalized item identity from PokeAPI/sprites commit `1dd55c0436d2a5b6e324c0b780e1b1b416fbfb64`. `Leek` uses the upstream filename `stick.png`; saved item IDs are unchanged. Source: https://github.com/PokeAPI/sprites/tree/1dd55c0436d2a5b6e324c0b780e1b1b416fbfb64/sprites/items . Underlying Pokémon artwork remains the property of its rights holders.

`move-details.json` includes literal descriptions from Pokémon Showdown's `data/text/moves.ts` at the same pinned MIT commit `1242c1e2d9763ba5b85d33f13aecb89bae19ae3d`. Champions-specific descriptions override the general text (17 of the 515 bundled Champions move definitions). Priority, targeting and flags come from the already-merged Champions definitions. General reference text is identified when browsing historical games; it does not certify historical mechanics. Source: https://github.com/smogon/pokemon-showdown/blob/1242c1e2d9763ba5b85d33f13aecb89bae19ae3d/data/text/moves.ts . Original English descriptions are displayed with an explicit language label; they are not silently machine-translated.

## Offline evolution-family facts (local development)

`evolution-families.json` is a derived factual relationship dataset, with new
PT/EN/ES condition prose and native graph presentation. Its initial 575 family
records were inspected from the earlier reference package `com.talzz.datadex`
version 3.57, SHA-256
`414a6f00a78d4ddb02bbc18e2fc14c55b022d9f5ec6ef8629c5b2218fee07967`.
No application code, layout, encrypted database or branded UI was copied from
that package. Extraction evidence and explicit structural normalizations are
recorded in `research/evolution/REA-EVOLUTION.md`. This attribution records data
provenance and does not claim that the reference application's assets or code
are licensed for redistribution.

A bounded update uses the primary PokeAPI dataset at commit
`2bf639e5572812a67d2b94903fdcb9d095f5aeb2`:
https://github.com/PokeAPI/pokeapi/tree/2bf639e5572812a67d2b94903fdcb9d095f5aeb2/data/v2/csv .
`pokemon_evolution.csv` records 407, 408 and 460 and `pokemon_forms.csv` records
10310/10311 corroborate Eevee's Leaf/Ice Stone alternatives and Rockruff's
Own Tempo/dusk route. Exact URLs and file digests are preserved in the asset's
source metadata and `research/evolution/pokeapi-sources.json`. PokeAPI repository
code is BSD-3-Clause; underlying Pokémon names, characters and game facts remain
associated with their respective rights holders. National evolution reference
facts do not establish a playable evolution mechanic in Pokémon Champions.

## National species profiles and additional forms (local development)

`species-profiles.json` imports structured category/biology facts and exact-form
physical/ability relations from PokeAPI commit
`2bf639e5572812a67d2b94903fdcb9d095f5aeb2` (BSD-3-Clause repository notice is
bundled in `assets/licenses/pokeapi-BSD-3-Clause.txt`). No Pokédex flavor text or
PokémonDB paragraphs are imported. Existing native IDs and all Champions-specific
availability, attributes, learnsets and rules are preserved. Extra forms occupy
an explicit namespace and carry no inferred game availability.

`ability-profiles.json` uses community-written Pokémon Showdown main-series
ability summaries from MIT commit
`1242c1e2d9763ba5b85d33f13aecb89bae19ae3d`,
https://github.com/smogon/pokemon-showdown/blob/1242c1e2d9763ba5b85d33f13aecb89bae19ae3d/data/text/abilities.ts .
Local PT/ES translations protect names and quantities. Thirty-two reviewed
text/category overrides are retained; remaining translations have not received
complete human semantic review. These are explicitly general reference effects,
not certification of Champions mechanics.

47 additional exact-form WebP images are converted from pinned PokeAPI/sprites
commit `1dd55c0436d2a5b6e324c0b780e1b1b416fbfb64` official-artwork PNGs.
Source URLs, PNG/WebP SHA-256 and missing-image cases are retained in
`scripts/data-import/additional-art-manifest.json`. No base-species image is
substituted for an unavailable exact form. Underlying Pokémon artwork, names and
characters remain the property of their respective rights holders.

The reproducible import, source locks, structured coverage audit and validator
are in `scripts/data-import/`; downloaded translation models are not distributed.
