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
