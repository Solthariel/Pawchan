# Bottom navigation outline (0.1.24 implementation record)

> Исторический снимок конкретного патча/проверки, не инструкция для текущего выпуска.
> Числа PASS/FAIL относятся только к указанной в отчёте базе и среде.
> Старые команды применения/публикации не выполнять; см. [карту действующих документов](../README.md).

This is the historical 0.1.24 record. In 0.1.25 the plain-mode surface is
rectangular and opaque beneath the divider; the bubble surface is unchanged.
See [current upload/unlock and panel fixes](UPLOAD_AND_UNLOCK_STABILITY.md).
The description and verification numbers below apply to 0.1.24.

This addition changes only the navigation container decoration:

- Bubble mode retains the existing 22 dp capsule outline, 1 dp stroke and theme
  outlineVariant color.
- Plain mode removes that outline and draws a Material HorizontalDivider above
  the panel, with the same default thickness, 8 dp inner inset and outlineVariant
  color as PostSeparator. The divider is an overlay, not another layout row.
- The existing surface background and shape remain unchanged. The 36 dp bar,
  10 dp outer inset, button padding, icons, active-tab capsule, badge, click
  callbacks and system/keyboard/scrollbar handling remain unchanged.
- The existing post-display preference is used. There is no separate setting,
  persistence format or new navigation state owner.

The pending 0.1.24 post-shape patch is preserved in this combined overlay. No
0.1.25 is prepared while 0.1.24 is unpublished; release-sequence protection is
not bypassed. The combined ZIP replaces the previous 0.1.24 ZIP.

Checks cover top-only versus capsule pixels in light/dark themes, identical
button bounds, identical active-tab pixels and all four actions in both modes.
The older floating-workspace tests explicitly select bubble mode, matching the
scenario their geometry assertions were originally written for.

## Verification results

- Combined pending release: 0.1.24, versionCode 25, based on published `5860e7a`.
- `scripts/local-check.sh`: PASS (debug/release, qualityCheck, Lint and static checks).
- JVM tests: 1174, no failures/errors/skips.
- Native-graphics host run of the 7 new outline scenarios and 5 existing floating
  workspace scenarios: all 12 PASS (Robolectric SDK 36; not a device run).
- AndroidTest Kotlin compilation: PASS.
- Publishing Python suites: 42 + 12 PASS.
- Device smoke and release Macrobenchmark: NOT RUN; no attached device or KVM
  emulator. No performance certification is claimed.

The ZIP contains only changed/new source, tests, notes and release metadata. It
replaces the previous 0.1.24 ZIP and may be overlaid on either 0.1.23 or the pending
0.1.24 checkout. Do not use it to overwrite an already published newer release.
