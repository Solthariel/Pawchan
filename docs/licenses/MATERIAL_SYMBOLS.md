# Google Material Symbols Rounded

The standard action icons in this patch are vendored from Google's
[material-design-icons](https://github.com/google/material-design-icons) repository,
revision `10ecc14790612ca8fd164e3f4ffe9c16fc50d5b7`.

- Family: Material Symbols Rounded.
- Default static exports: weight 400, grade 0, optical size 24, FILL 0.
- Rating, board-star and thread-bookmark selection use official FILL 1 exports.
- `material-symbols-manifest.json` records each destination, source URL and SHA-256.
- License: Apache License 2.0, copied verbatim to `MATERIAL_SYMBOLS_LICENSE.txt`
  and included in the APK assets as `licenses/material-symbols.txt` with attribution.

The SVG path commands are unchanged. SVGs using `viewBox="0 -960 960 960"`
are converted to Android VectorDrawable viewport 960 by a single group translation
of Y=960. The legacy 24-unit export retains viewport 24 and translation 0.
The vectors have a 24dp intrinsic size and tintable white paths. The navigation
back arrow is auto-mirrored; media previous/next keep their playback direction.
No runtime font, network access, icon-pack dependency or SVG parser is added.

The launcher/notification branding, site-provided icons and refresh-progress
geometry are not generic UI icons. OP/person now uses `person`, sage uses
`arrow_downward`, cyclic threads use static `autorenew`; attachment hash/no-name
use `tag` / `format_clear`. No custom Canvas marker renderer remains.
