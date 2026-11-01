# Interface v3 — toolbar geometry, attachment symbols, preview border

Status: source patch for local validation, NOT fully verified for publication.
Baseline: rounded-icons-v2. No version, dependency or build-rule changes.

## Agreed changes

- Shared toolbar minimum height 64dp, actual clickable target 48dp, normal glyph 24dp,
  horizontal inset 4dp. Glyph outside edge is therefore 16dp from the viewport edge.
- One-line titles use titleLarge. Titles start at 16dp without a leading button,
  or 64dp after the leading button. Contextual section labels keep secondary typography.
- Catalog disclosure is centered on the entire title/subtitle column, not its first line.
  Catalog title text starts at the same 16dp content inset as the list.
- Catalog, boards, settings, library and thread search share a 48dp search frame;
  each caller still owns focus, IME action, query and cancellation.
- Thread bars below 360dp move bookmark/gallery into the existing overflow menu.
  Search result navigation uses a secondary row, preserving field width.
- Gallery filters occupy a scrollable secondary row; large counts do not squeeze the title.
  Selection keeps navigation/title/select-all positions aligned with the normal header.
- Composer keeps Back/title/attach in the main row, with posting controls in a secondary
  row below 480dp. Attach glyph is 24dp, not 26dp. Site-provided posting symbols keep
  their original aspect ratio and are not replaced with Material UI glyphs.
- Captcha reload is flush with its control row. A weighted timer cannot push it out.
  Status-row inset is 4dp, so its reload glyph aligns with the top-bar glyphs; form and
  captcha image content keep their 16dp inset. Deadline/reload/send logic is unchanged.
- Thread section close-all and individual close controls share the last 48dp column.
  Disclosure has a separate preceding column; counters reserve corresponding space.
- Archive and full-screen changelog follow toolbar dimensions; centered attachment
  dialog header remains a modal header, with its actions vertically centered.
- Remove the preview window border introduced by modal-layers-v1. The external rounded
  shape stays; inner post shape/border still follow the card preference.
- Attachment options use official local Rounded symbols: fingerprint/fingerprint_off,
  ink_eraser/ink_eraser_off, label/label_off, visibility/visibility_off, repeat,
  drive_file_rename_outline. An off glyph indicates the enabled removal/transformation
  option, not completion of preprocessing. Existing toggle/action behavior is unchanged.
- Existing attachment button outlines are not restyled. Network/autohide icons and
  Settings overflow destinations are not changed in this batch.

## Preserved

Palettes and live custom-color preview; 60%/90% modal levels; system-bar dim ownership;
quick-board keyboard/gesture ownership; post text sizes and own-post accent; updater
and legacy token cleanup; site/domain/storage/network behavior. No push or publication.

## Validation and limitations

External checks report contains exact commands/results on the final source archive.
KtLint/Detekt and selected Kotlin/JVM tests are separate from Android runtime checks.
Compose API compilation uses real Compose jars but compile-only resource identifiers
and explicit unchanged service/platform boundaries. It is not an APK/AAPT validation.

Added/updated regressions cover 48dp targets/search, two-line catalog arrow centering,
loading title stability, captcha right edge, narrow gallery/editor geometry, option
icon states and compact-thread layout boundary. Instrumented tests require a device.

No Android SDK/NDK/device is available in the agent environment. Full Gradle LOCAL CHECK,
AAPT/Android Lint, device-smoke and release macrobenchmark have NOT been run. Do not
call this patch ready for publication on the basis of the host checks.

## Required device validation

Run the full sequence in docs/DEVELOPMENT.md on these exact sources. Then inspect:

1. 320/360/393dp and landscape; default and large system font/display size.
2. Catalog header before/after speed loads; dropdown triangle; quick-board lookup,
   keyboard, outside-tap dismissal and 60% dim protected toolbar/input region.
3. Threads title, each section open/closed, close-all vs per-row close and counters.
4. Thread normal/search/selection; bookmark/gallery both direct and compact-menu access.
5. Gallery full screen and embedded pane, all filters, huge counts, selection and return.
6. New thread/reply on boards with/without site icons/sage and attachment limit reached.
7. Captcha loading, active/expired timer, long rejection, cooldown, submitting and sent;
   reload glyph right edge, disabled semantics and unchanged server-authoritative expiry.
8. Attachment toggles, rename/reencoding dialog cancel/apply, disabling rename when name
   removal is enabled; correct selected glyphs with both dark and light palettes.
9. Preview and reply preview in card/no-card modes; nested windows and both dim levels;
   preview swipe/return; no new outside border but inner card borders remain.
10. Settings/autohide/statistics, history/copies/archive, changelog and media viewer,
    Back/system Back, search/clear, system-bar/IME insets and modal layering.

Because preview/layout are involved, release performance checks and trace review remain
mandatory under docs/contracts/PERFORMANCE.md before publication.
