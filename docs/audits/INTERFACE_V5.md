# Interface v5: bottom navigation and thread-list geometry

Base: the complete Interface v4 project. All previous gallery, board-header,
settings icons, theme, modal-layer and updater changes are retained.
This is a source patch for local verification, NOT a publication-ready release.

## Contract

- Five bottom targets: Catalog, Threads, Boards, Settings, Write. Write uses the
  existing official Rounded edit_square vector, opens the current catalog's new
  thread flow or the current thread's reply composer. It never selects a last
  visited destination. Elsewhere it is disabled. Loading/error/closed threads
  expose no writing action. Existing post reply/quote menus remain unchanged.
- Selected navigation uses theme primary without a persistent oval. Other enabled
  actions use onSurfaceVariant; disabled Write uses alpha 0.55 like unavailable
  board rows. Standard click feedback is retained. Minimum target height is 48dp;
  total overlay is 56dp including the 8dp fade. The list's overlay inset follows
  this height. Surface alpha is 0.92, no divider, border or heavy shadow.
- The thread toolbar no longer duplicates Write; six remaining full-sized targets
  determine the compact-toolbar boundary (312dp). Favorites and Gallery remain
  reachable through the compact menu below that width.
- All three thread sections use a common count column and a final 48dp action
  column: header counters above row counters, disclosure above each row's cross.
  Count width includes Pages and section totals; zero uses the same cell style.
  Collapsing does not move the columns. Close All stays in the toolbar menu.
- Cross actions are intentionally different: close the open page, remove the
  favorite, dismiss the local replies entry. They do not delete remote content.
  Labels describe each action for accessibility. Long-click menus are retained.

## Ownership / API

ThreadFeatureScreen gains one optional navigationHost slot receiving only a
nullable writing callback and content. The feature retains private UiState and
owns reply availability; app owns bottom navigation and the composer. No mutable
feature controller, data/network dependency or new library is exposed. Catalog
uses its existing onNewThreadClick callback, including its draft lifecycle.

## Automated coverage

- ThreadWriteActionTest: open-thread request and unavailable loading/error/closed
  states. ThreadToolbarPolicyTest: six-target boundary.
- ThreadRowCloseActionTest: each cross calls only its section's existing handler
  with the correct row/address; labels differ; no close-all/navigation fallback.
- AppBottomNavigationRuntimeTest: order, 48dp targets, selection, current-context
  writing, disabling after context loss without invoking a stale writing action.
- ThreadsSectionGeometryRuntimeTest: all count/action columns, zero counters,
  expansion stability, no Close All header. Existing counter semantics tests stay.
- Thread toolbar UI tests now check the intentional removal of duplicate Write;
  RuntimeSmokeTest still expects a real enabled Write action through app navigation.
  Its unchanged local bitmap fixture is separated into SmokeMediaFixture because
  analyzing the edited test exposed an existing function-length violation.

Runtime tests are supplied, not claimed to have run. Their red-before/green-after
Android execution is still required; there is no device in this environment.
Compile-only API checks and extracted immutable state models are NOT Android
runtime or an APK build. Available validation results are provided separately.

## Local verification still required

Run the complete sequence in docs/DEVELOPMENT.md: LOCAL CHECK, all three Python
suites, full device-smoke for all eight Android-test modules, inspect fresh XML
reports. No skipped/missing/old result counts as PASS. Run release before/after
Macrobenchmark with matching data, device, cache/network and at least five repeats;
no performance PASS is claimed without it.

Manual checks: 320/393dp and large fonts, all themes/light/dark/custom, 3-button
and gesture navigation, disabled Write in Boards/Threads/Settings/history,
loading/error/closed/open thread, current catalog draft, reply composer keyboard
open/close, all three removal actions and their independent row/long-click actions,
collapse/expand/search, zero and long counters, Close All via menu, last list item
above the new overlay, preserved unread badge and unrelated gallery controls.
No GitHub push, release-request or publishing is authorized by this source ZIP.
