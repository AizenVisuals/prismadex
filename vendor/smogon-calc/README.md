# Smogon Damage Calculator — source reference

Upstream: https://github.com/smogon/damage-calc
Commit: c30417b84b558b407ea50ebba4b4bcc603f6fbff
License: MIT, retained in LICENSE.

`src/` is the original preferred TypeScript source from upstream `calc/src/`.
`upstream-package.json` records the upstream package metadata. Tests and source data are retained. This directory is a source reference, not an independently configured npm package.

PrismaDex bundles a minified/transpiled engine as `android/app/src/main/assets/damage-engine.js` and its own adapter as `damage-bridge.js`. Android builds consume those checked-in assets; they do not require Node or download an engine at runtime. To rebuild an engine, use the pinned upstream project's build environment and transpile for the Rhino version used by the Android app, then run the Android damage regression tests before replacing the asset. Do not assume a newly generated bundle is byte-identical or compatible without verification.
