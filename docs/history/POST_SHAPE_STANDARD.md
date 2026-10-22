# Post state geometry (0.1.24)

> Исторический снимок конкретного патча/проверки, не инструкция для текущего выпуска.
> Числа PASS/FAIL относятся только к указанной в отчёте базе и среде.
> Старые команды применения/публикации не выполнять; см. [карту действующих документов](../README.md).

## Ownership and behavior

`core/ui/PostContainerShape.kt` is the shared, immutable shape definition used by
thread cards, hidden cards, fresh-post overlays and reading-return markers.

| Display mode | Post surface | Fresh / selected | Interaction clipping |
| --- | --- | --- | --- |
| Plain | Rectangle | Rectangle | Rectangle |
| Bubbles | Rounded, 14 dp | Same rounded outline | Same rounded outline |

Selection continues to change the existing surface color. Fresh highlighting
continues to use the existing primary tint, 0.12 alpha and 900 ms fade. Neither
state changes the outline or layout. The inner combined-click target and Surface
use the same shape. Hidden cards use that shape too. The redundant `framed` option
was removed; the display preference was already authoritative.

Fresh overlays cache the outline by drawing size, density and layout direction.
Animated alpha is read inside drawing, not composition. The reading-return line
is draw-only and clipped to the post outline; no label row, padding or scrolling
offset is added. Only the marker is clipped, not the separator or child content.

The new-post notice pill, thumbnail corners and inline search/text highlights
keep their independent geometry. Timing, new-post tracking, reading anchors,
network requests and state colors are unchanged.

## Verification

- `PostContainerShapeTest`: plain bounds, four equal corners, density, RTL and
  shared immutable shape reuse.
- `PostShapeRuntimeTest`: actual thread and hidden cards, fresh and selected states,
  reading marker corners, display-mode switching and fresh fade completion.
- Full debug/release compilation and project quality checks are required before
  distributing the overlay patch.

Host-rendered UI tests are not a substitute for device smoke tests or a release
Macrobenchmark. An on-device check should refresh a thread in both modes, select
and long-press new posts, hide/reveal a post, then return to the saved reading
anchor and confirm the right-side position marker is unchanged.

## Local verification results

- Base: `5860e7a` (published 0.1.23); target: 0.1.24, Android versionCode 25.
- `scripts/local-check.sh`: PASS (debug, release, unit tests, Lint, static checks).
- JVM suite: 1174 tests, no failures, errors or skipped tests.
- Host-rendered Robolectric SDK 36 native-graphics run of the 10 post-shape
  AndroidTest scenarios: PASS. Temporary host dependencies are not in this patch.
- AndroidTest Kotlin compilation: PASS.
- Publishing Python suites: 42 + 12 tests, PASS.
- Device smoke and release Macrobenchmark: NOT RUN. No attached Android device
  or KVM-backed emulator is available. Performance is not certified.

Overlay this ZIP onto the current 0.1.23 source checkout. It contains only changed
files; no APK, SDK, build output, Gradle cache or local build settings.
