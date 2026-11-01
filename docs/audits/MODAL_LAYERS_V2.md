# Modal layers v2 — 60% / 90%, menu type and Android navigation background

This is a local-preview patch above modal-layers-v1/colors-v5, not a published
release. Version 0.1.50, package, signing and update-feed configuration remain
unchanged. Palettes, live color preview and saved legacy-token cleanup remain.

## Two explicit strengths

- STANDARD: black alpha 0.60 for action menus, confirmations, settings,
  attachment editor/reencoding and inline post previews.
- MEDIA: black alpha 0.90 for the catalog attachment-selection grid (the user's
  strong-backdrop screenshot). It is one computed overlay, not two system dims.
- Opening a nested window does not multiply the base-page overlay. Visible lower
  surfaces receive the immediate child's strength. Closing it restores the plane.
- Fullscreen media/composer/gallery/changelog retain their own backgrounds;
  child modals dim the foreground plane, not another root background.

### Historical source

The old app Theme.Pawchan inherits android:style/Theme.Material.NoActionBar.
Android 15 AOSP Theme.Material sets backgroundDimAmount=0.6. The actual Compose
AAR's DialogWindowTheme/FloatingDialogWindowTheme does not override the amount.
CatalogAttachmentPicker also had PICKER_SCRIM_ALPHA=0.72: combined theoretical
opacity 1-(1-0.6)*(1-0.72)=0.888. The user selected 0.60 / 0.90 explicitly.
Vendor overrides on the user's Samsung have not been measured.

Reference:
https://github.com/aosp-mirror/platform_frameworks_base/blob/android-15.0.0_r1/core/res/res/values/themes_material.xml

## Coverage

| Surface | Rule |
| --- | --- |
| Catalog/thread/post/boards/settings/history/filter/statistics action menus | Normal 60%, existing anchoring preserved |
| Catalog image/attachment-selection grid | Strong 90% |
| Composer attachment editor, rename, reencoding/quality/confirmations | Normal 60%; nested lower surface only |
| Inline post preview stack / standalone preview | Normal 60%, previous stack/swipe ownership retained |
| Fullscreen media/editor/gallery/changelog | Own background, child surface dim only |
| Quick board switch opened from /b/ title | Normal 60% around the panel, panel itself not dimmed |
| Search suggestions outside this quick board panel, composer icon field | No additional modal scrim |
| App lock | Existing secure full-screen host retained |
| Media badges/captions | Local readability backgrounds, not modal scrims |

The quick-board panel remains in its original Compose layout, not a new native
window. The host excludes its measured rounded rectangle from its overlay.
Bounds and corner outline are UI-owned; drawing is cached for fixed geometry.
The existing input field, IME, Back handler, board selection, outside-tap and
outside-swipe observer remain. The editable toolbar is visually dimmed but keeps
its input/semantics; the inline panel remains a navigation aid rather than a
focus-trapping native dialog. Check TalkBack and old gesture tests on device.

## Font correction

The own-message action used TextButton's default label typography while adjacent
ActionMenuItem rows explicitly use bodyLarge. It now uses the same bodyLarge,
onSurface foreground and 48dp minimum touch target. Checkbox state and OWN
command dispatch are unchanged. Post text/body size settings are not changed.

## Android system navigation background

The root ModalLayerHost now wraps PawchanTheme, not just its page content. The
last-drawn theme background behind Android navigation buttons is consequently
below the scrim rather than overpainting it. AppBottomBar is not the system bar.

System-button contrast uses the effective darkened background. Native managed
windows use edge-to-edge, transparent bars and no extra contrast-enforcement
backplate; platform dim remains disabled. Window attributes owned by the adapter
are restored on disposal. Media visibility/immersive lifecycle remains owned by
MediaBarState, not this registry. core:ui explicitly depends on AndroidX core-ktx
for WindowCompat; it remains a presentation-only leaf with no project dependency.

## Verification, not a device PASS

Host tests exercise both strengths, registration/nesting/removal, no multiplied
base dim, system-bar strength and measured inline-panel geometry. A regression
check runs the updated normal-strength test against old v1: it must fail because
0.32 does not match 0.60. Android tests are compiled against real APIs but need
an actual device to execute:

- core:ui ModalLayerRuntimeTest: 60/90 page pixels, unchanged base under child,
  restoration, native dim disabled, secure policy and navigation contrast.
- app ModalNavigationBackgroundRuntimeTest: the actual PawchanTheme last-drawn
  bottom system-navigation background is dimmed and restored at both levels.
- app BoardLookupDimmingRuntimeTest: background 60%, panel stays bright,
  board-choice click and removal restore the page.
- feature:thread PostMenuUiTest: own-message size/weight/spacing/foreground match
  the adjacent row; OWN still dispatches.

Before any publication, perform all AGENTS.md gates: full Android LOCALCHECK,
all Python suites, full device-smoke, fresh report review and required release
performance/macrobenchmark checks. Unavailable/skipped is NOT PASS.

### Required visual/device acceptance

1. Compare both supplied screenshots in light/dark/AMOLED at 60/90.
2. Nested reencoding/quality dialog: base stays constant, editor position/scroll
   and input focus do not shift, Back/outside closes only the top window.
3. Samsung Android 15 with THREE-BUTTON navigation: bottom system background
   and top background darken consistently; icons remain readable, no extra
   opaque navigation stripe. Repeat with gesture navigation and in landscape.
4. Dismiss/rotate/show keyboard/split-screen and return from media: no stale
   bar color, dim, contrast mode, hidden bar or wrong secure-window behavior.
5. Quick-board panel: input/IME remain usable; board selection, outside tap,
   blank panel cell and outside swipe preserve original behavior. Test under
   an existing offset/insets, rotation and 200% text scale; cutout follows bounds.
6. Fullscreen and inline preview/menu combinations: no repeated base overlay,
   unwanted shape clipping, popup exit-animation flash or background clickthrough.
7. TalkBack: top native modal owns focus; quick-board input stays accessible;
   focus returns correctly. Measure frame time for the cached root cutout path.

No Android build, device-rendering, visual or performance PASS is asserted here.
