# Inline board lookup preview

Promoted from the experimental APK into the 0.1.17 release patch. Signed publication is a separate GitHub workflow step.

- Catalog owns toolbar input, focus, Back and the panel anchored below the 54 dp toolbar.
  The existing thread-search action remains separate. Thread-list geometry is unchanged.
- Host connects catalog's narrow lookup bindings to board presentation; catalog does not
  import the boards ViewModel. The route ViewModel owns directory requests and cancellation.
- Favorites and recent boards are deduplicated by full BoardAddress. Search transforms
  directory entries on Default; empty-query rows carry codes only, search rows code/title.
- AppStorageViewModel owns one retained RecentBoardsRepository. Its preferences implementation serializes IO visits,
  stores at most 100 distinct full addresses and restores them independently of route lifetime.
  Existing portable settings, drafts, release feeds and media/PiP behavior are unchanged.
- The panel uses five fixed columns and independently caps each group at two 48 dp rows. Spare rows are not transferred between groups.
  Each quick group scrolls independently. Search results use one scrollable list.
- Unit tests cover ranking, site isolation, deduplication, row budgets, history limits,
  lazy directory loading, retries and favorites updates. Android tests cover row geometry,
  selection, clear/outside dismissal, independent scroll and preferences recreation/errors.
- Full local-check plus isolated Robolectric execution are required before handoff.
  Physical-device IME, gesture Back and release frame-time measurements are not supplied
  by JVM tests and must not be represented as measured device results.

- An ancestor pointer observer shares the list hit path: it never consumes outside drags,
  dismisses the panel/IME on movement past touch slop and consumes only the first outside
  tap-up to prevent accidental navigation. Panel gestures stay scoped to the panel.
- Header code/title and speed always occupy the same slots. Font-glyph arrows are replaced
  with a shared 20 dp canvas: filled disclosure before labels, stroked forward after labels.
  Search-result navigation, clearing, empty rows and site isolation preserve earlier contracts.
