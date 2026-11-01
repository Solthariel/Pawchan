# Rounded icons v2 — local preview source patch

## Scope
- Official bundled Material Symbols Rounded, pinned revision/provenance in
  ../licenses/material-symbols-manifest.json. No icon font or runtime network dependency.
- OP/person and sage use person / arrow_downward; OP reference decoration uses the same
  painter without modifying source text, copying, searching or export. DT stays text.
  Annotation selection and tint are remembered outside draw callbacks.
- Cyclic threads use static autorenew; pinned/closed remain keep / lock.
- Add-media uses add_photo_alternate; picker MIME/capability rules are unchanged.
- Favourite thread: bookmark_add / filled bookmark. Favourite board: outline/filled star,
  selected semantics and labelled action. Callbacks and persistence remain unchanged.
- Bottom bar: home / forum / list_alt / settings. Destinations and labels are unchanged.
- Settings: tune / palette / forum / image / send / filter_alt / language / storage / info.
  Media deliberately keeps its previous image icon.
- Disclosure: trailing filled down/up triangles. Submenu: filled right triangle.
  Destination: chevron. Back: arrow_back. Main action menus gain decorative leading icons;
  choice/radio menus are not uniformly decorated. Children do not own click actions.
- Catalog board selector has a subtle themed rounded background and trailing triangle.
  Opening still swaps the toolbar to the existing focused quick-board search input;
  this patch does not create a second expanded-state owner or alter its 60% modal layer.
- My-message menu row retains bodyLarge and primary accent from the preceding patch.

## Preserved
Palettes, real-time custom-colour preview, slider appearance, 60/90 dim strengths,
Android system-navigation-bar dimming, updater without token UI and legacy local
credential cleanup are not changed. No credentials are added. No publishing action.

## Verification boundaries
Local checks cover KtLint, Detekt, selected real Kotlin/Compose API compilation with
-Werror, selected JVM suites, Python suites, vector path/viewport/hash fidelity and
ZIP/source integrity. Detailed results are provided with the patch.
Host API harnesses use real Android/Compose artifacts and compile-only resource IDs;
unchanged Coil/state/service boundaries in the extra component harness are substitutes,
not Android runtime validation. The harness itself is outside the shipped source.
Android render/interaction tests were added/updated, including submenu dispatch,
disabled rows, disclosure state/placement, OP visibility/text integrity and all icons.

NOT release-qualified: no full Gradle build, AAPT/Android Lint, connected device suite,
macrobenchmark, allocation/frame measurements or visual Samsung validation ran here.
No Android SDK/NDK/device is available in this workspace. The supplied PowerShell
helper builds a Release APK on the user's working SDK for local visual inspection.
Before publishing, run the repository's complete LOCAL CHECK, device-smoke and required
performance matrix. A successful local preview build is not those checks.
