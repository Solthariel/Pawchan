# Interface v4: corrections from the uploaded working project

Historical v4 report. Its separate Pages close-all slot was superseded by
[Interface v5](INTERFACE_V5.md); see that report for the current navigation contract.

Base: Pawchan-current-20261101-161345.zip, supplied by the owner after a successful
local Interface v3 build. No site, database, network or updater changes are made.

## Corrections and causes

- Settings kept language/filter_alt despite the requested replacements. Add two
  distinct kinds (PUBLIC/CLEANING), map the settings sections to official local
  public/cleaning_services vectors, preserve other uses of GLOBE/FILTER.
  All other section icons already resolve through the same Rounded registry;
  the Media section intentionally retains image.
- Gallery's filters had been moved to a second row that disappeared in selection
  mode. Restore a single 64dp-minimum header: compact title and 48dp filter targets
  in an internally scrollable strip, Back and selection outside that strip.
  Selection uses the same minimum height, so the grid does not jump at default font.
- Replace the stacked icon-plus-label bottom actions with a compact 56dp-minimum
  M3 surface and standard TextButton/IconButton controls. Labels go beside icons
  when two actions fit; narrow/large-font/many-action cases retain accessible icon
  buttons. No extra system-bar inset is added: existing gallery hosts own it.
- Pages header previously switched layout when close-all appeared. Disclosure is
  now always the last slot; Pages reserves a separate close-all slot in either
  state. Per-page close aligns with close-all. Pages badge counts open threads;
  Favorites/Replies retain unread totals. Navigation and close callbacks unchanged.
- Catalog identity inherited a 22sp app title and 4dp outer inset. Restore a compact
  18sp title; chooser surface aligns at 16dp with list content, text has its own
  12dp internal inset. Preserve two-line arrow centering and minimum 48dp hit area.
- The companion Windows build script is ASCII without BOM, avoiding the stray
  invisible character reported at the first line of the previous script.

## Regression coverage

JVM: settings symbol mapping, registry resources and section counter semantics.
Instrumented source tests: same-row gallery filters; horizontal overflow; stable
header height across selection; 56dp bottom bar; enabled/disabled actions and four
48dp targets at narrow width; Pages disclosure/badge stability and close-all not
triggering expansion; catalog surface inset, 18sp text and two-line centering.

Added instrumented tests are compiled against real Compose API in a host harness
where available, not executed. Harness resource IDs and unchanged external services
are compile-only boundaries, not APK/resource/runtime validation.

## Still required before publication

No Android SDK/NDK or device is available here. Full Gradle LOCAL CHECK, AAPT,
Android Lint, all device-smoke suites and release macrobenchmark are NOT run.
This is a source patch for local checking, not a publication-ready release.
See the external checks report for exact executed commands and counts.

On the target Samsung, check default and large font, 320/393dp and landscape:
1. Gallery filters scroll inside the toolbar; Back and selection stay in place.
2. Enter/exit selection: no grid jump; filters and download/link actions still work.
3. Bottom actions for zero/multiple selection, favourites and embedded gallery pane;
   TalkBack descriptions remain available when visible labels are hidden.
4. Pages expanded/collapsed/empty, close-one and close-all; arrow and badges do not
   swap columns; Favorites/Replies counters preserve existing behavior.
5. Catalog long name, delayed/missing speed, dropdown dim/keyboard/Back handling.
6. All settings icons: public, cleaning_services, unchanged Media image.
7. Previous palettes/live preview, 60/90 dim, Android system navigation dim, own-post
   accent, attachment stateful symbols and legacy token cleanup remain intact.

No push, publication, install or signing change is authorized/performed.
