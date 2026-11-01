# Rounded icons v1 — implementation prepared, not fully verified

## User-visible scope

Baseline: modal-layers-v2, retaining colors-v5, live custom-color preview,
AMOLED/dynamic palettes, token-cleanup/public updater and 60%/90% modal layers.

- Standard actions now use bundled official Material Symbols Rounded vectors.
- Gallery (`MEDIA_MULTIPLE`) is `photo_library`, not a focus/fullscreen corner frame.
- Shared actions cover toolbar/navigation, media playback, settings/categories,
  status pin/lock, files, links and ratings. Ratings retain their semantic colors;
  selected thumbs use FILL 1, unselected thumbs FILL 0.
- Existing XML resource names are retained for lower navigation, catalog counters,
  attachment/composer actions and thread selection. Quote now shows quotation marks.
- Gallery image/video/select controls and PIN-delete/fingerprint visuals use Rounded.
  Their input/selection/authentication owners, callbacks and accessibility labels
  remain at the existing callers. No authentication or lifecycle logic is moved.
- `Моё сообщение` retains bodyLarge and its 48dp minimum target but restores
  `colorScheme.primary` rather than onSurface. Checkbox semantics/OWN dispatch
  and font settings are unchanged. This fixes the v2 color regression.
- Disclosure and forward/back indicators retain sizing, test tags and state labels.
- No slider, animation, modal-scrim, palette, updater or release-version changes.

## Boundaries, source and licensing

The common registry/rendering remains in core/ui, a presentation-only leaf.
No dependencies are added. Features consume its existing UiIcon API; two kinds
(VIDEO and CHECK_BOX) have actual media-filter callers. No business/data logic moves.
The old unused Canvas icon renderers are removed instead of keeping a second set.
Immutable resource mappings do not create collections or parse SVG in draw callbacks.
Vector loading/caching is the standard Compose resource painter, not a custom cache.

71 VectorDrawable destinations from a fixed official revision, with Apache-2.0
license in source and APK assets. See `../licenses/MATERIAL_SYMBOLS.md` and its
source/destination/hash manifest. 960-unit paths keep their geometry via translation;
the one 24-unit export keeps its original grid. No extra stroke is applied to
already-filled path geometry. Caller tint and dimensions remain caller-owned.

Exceptions are deliberate: source OP/person/sage markers, attachment hash/no-name
operations, site icons, launcher/notification branding and progress-indicator drawing.
These are not silently replaced with unrelated actions. There is no user icon-style
selector and no runtime font/download dependency.

## Automated checks and limitations

The external check report records commands and exact outcomes on the final sources.
Host-side selected JVM tests cover resource mappings, gallery semantics, all four
rating states and prior modal/preferences/palette/updater behavior. Python guards
validate resource integrity, pinned provenance, viewport, paths and licensing.
The final source comparison guards previous palette/live-preview/modal/system-bar/
quick-board/token changes outside this patch's explicitly allowed icon/style files.

New Android tests:
- `RoundedIconsRuntimeTest`: all common kinds have visible geometry; gallery caller
  tint updates while bounds remain unchanged.
- `PostMenuUiTest`: same body font as adjacent actions, theme primary in light/dark,
  OWN command still dispatches. Old v2 onSurface would fail the new color assertions.

Android tests are supplied and API-compiled, NOT executed here. The API harness
uses real Kotlin/Compose APIs and compile-only generated resource ID placeholders;
it is not AAPT, a Gradle module build, resource inflation or device rendering.
Additional API compilation covers the actual gallery filter and app-lock glyphs;
only their resource IDs/unchanged enum boundary are compile-time harness inputs.
No host source/resource guard is presented as a behavioral Android regression PASS.

No SDK/NDK/device is available here. Full LOCAL CHECK, Android Lint/AAPT, all device
modules, native-window checks and release macrobenchmark remain REQUIRED before
publication. No rule/suppression/baseline is weakened and no release/push is done.

## Device acceptance

1. Build Release with the helper; confirm the gallery is a stack of photos.
2. Search/refresh/write/attach/delete/menu/navigation still dispatch the same action;
   active/disabled colors, existing targets and TalkBack labels are preserved.
   Measure effective touch targets separately: UiToolbarButton still has its pre-existing
   36dp layout; this icon-only patch does not claim a global 48dp accessibility audit.
3. Inspect 18dp gallery filters, small status marks and ratings on Samsung Android 15,
   in light/dark/AMOLED/dynamic/custom palettes and large system font/display scale.
4. Confirm own-message accent and body font, checked/unchecked behavior and OWN toggle.
5. Verify fingerprint/PIN-backspace visuals do not alter login/cancel/input behavior.
6. Retest previous 60%/90% nesting, quick boards, IME, Back and Android system-bar dim.
7. Run the complete documented LOCAL CHECK -> Python -> device-smoke chain and
   required release performance comparison for list/inline-icon changes. Review
   fresh reports before requesting separate publication authorization.
