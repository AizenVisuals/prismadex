# Trainer and gym-badge import

These scripts implement the user's expanded asset request without changing the
visual design. They never publish, touch a device or write accounts/teams.

## Trainer sprites

Public source: https://play.pokemonshowdown.com/sprites/trainers/ . The captured
index has 1,500 exact PNG filenames, with artist credit attached to each figure.
The source explicitly requires the named artist to be credited and forbids
editing without permission. These files are therefore copied **byte-identical**:
no resize, recolor, optimization, format conversion, renaming or palette edit.
Underlying Pokémon IP and fan-art rights are not represented as open-source.
The user expressly accepted the IP risk for the expanded catalog.

```sh
python scripts/trainer-badge-import/import_trainers.py
# Only when intentionally regenerating the Kotlin key declaration:
python scripts/trainer-badge-import/import_trainers.py --write-keys
```

The importer uses four concurrent normal HTTP requests, no authentication,
anti-bot circumvention or browser impersonation. The default Python HTTP client
received 403 on this host; ordinary curl succeeded and was used as the supported
HTTP transport. Failed images would be recorded rather than replaced with a
fabricated or aliased image. The intentional full import completed with zero
failures: 1,500 files, 1,102,285 bytes, 13 credited artist names. Existing 37 files
were compared and remained byte-identical. Current app choices retain those 37
first, then add the remaining public keys alphabetically.

`trainer-manifest.json` captures exact URL, PNG SHA-256, size, dimensions and artist.
`assets/trainers/credit-manifest.json` ships the same metadata compactly for
`TrainerCreditsRepository.credit(key)` and in-app per-selection credit. Source
index and notice are retained under `cache/trainers-index.html`; this public
collection does not supply a Git commit pin for the actual sprite files.

## Gym badges

The preferred SteGriff source remains the existing first 40 recreations, CC BY 3.0,
Stephen Griffiths 2011, commit `89fde1733f2f819e7d3fe8036e68a8ade4c49579`:
https://github.com/SteGriff/pokemon-badges . Its repository actually contains only
Kanto through Unova; the title does not establish Kalos/Galar coverage.

The Bulbagarden badge file/category and league tables were checked for the
missing sets, designs and version-specific order. Direct protected downloads are
not bypassed. The same verified designs are available through the already
accepted PokeAPI/sprites source, pinned commit
`1dd55c0436d2a5b6e324c0b780e1b1b416fbfb64`, `sprites/badges/`.
These mirrored game images carry no blanket open-image license; the explicit
user-accepted IP risk is preserved in the manifest.

```sh
python scripts/trainer-badge-import/import_badges.py
python scripts/trainer-badge-import/validate_assets.py
```

40 new badge PNG files (391,258 bytes), total 88 local badge PNGs:

- Kalos X/Y: source IDs 43–50, correct eight gym badges.
- Sword: 51,52,53,54,56,57,59,60.
- Shield: 51,52,53,55,56,58,59,60.
- BW2: Basic/Toxic/Insect/Bolt/Quake/Jet/Legend/Wave, with six existing SteGriff
  renders reused and the two additional exact source badges 35/42.
- Diamond/Pearl and BDSP: correct their gym order by reusing existing SteGriff
  renders; the previous set follows Platinum's different order.

All new badge images fit/pad to 96×96, matching the existing asset convention.
Trainer no-edit rules do not apply to these independent badge sources; their
original/source/output hashes and transforms are recorded separately.

`badge-manifest.json` and bundled `badges/set-manifest.json` record all source IDs,
names for provenance, game mappings, exact URLs, licenses and output hashes.
The UI remains image-only with positional accessibility labels; names from this
manifest are not inserted into the badge grid.

Galar uses `UserState.galarVersion` and separate `galarBadgeSets` so switching
Sword/Shield does not share or overwrite earned progress. No fake game IDs are
introduced. Existing other-game badge maps remain untouched. Gold/Silver, Crystal and
HeartGold/SoulSilver additionally retain their eight Kanto postgame badges in
`kantoPostgameBadges`, independently from the first eight Johto badges. The app
reuses existing Kanto images; no new artwork is invented. Games without gyms keep
their honest fallback.

## Validation

The offline validator checks PNG signatures, every PNG chunk's CRC, successful
IDAT decompression, all source/output hashes, exact Kotlin key coverage and both
Galar orders. `validation-report.json` records the real results. Six unit tests in
`GalarBadgeSelectionTest.kt` cover variant independence, other-game preservation,
unsupported games, corrected version mappings and the independent Kanto postgame
registries for all three Johto game groups. Device/Gradle execution is
performed by the integrating agent, not claimed by these scripts.
