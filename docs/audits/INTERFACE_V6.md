# Interface v6: composer hint, bottom press feedback and transparency setting

Source patch for local verification, not a publication-ready release.
Base: complete Interface v5, preserving its existing changes.

- Remove the new-thread attachment hint, its renderer and unused string resource.
  NewThreadUiState.canSubmit and posting validation are untouched: this is only
  removal of explanatory UI, not permission to post a thread without media.
- All five bottom buttons use the same standard Material indication clipped to
  a centered 48dp circle. The complete original cell remains clickable/selectable
  and retains its accessibility role and label. There is no permanent background.
  The replies badge is outside the clip and therefore is not cut off.
- Shared interaction source connects the full-cell input to the central circular
  visual feedback. Disabled Write, contextual routing, colors and count unchanged.
- Add Interface → Bottom panel → Transparency, 0–100%, default 20% (surface alpha
  0.80). Zero is opaque, 100% is clear. This fades only the background, never
  icons/badge. The top 8dp fade follows the selected surface alpha.
  NavigationPreferences owns the value; DataStore clamps/persists it under a new
  key, absent key defaults to 20%. Changing other list limits preserves it.
  PawchanApp provides the loaded value through an app-owned CompositionLocal;
  no I/O or new repository subscription is performed in the bar.
  Existing Scale control commits on release, then the shared background updates.

Android regressions supplied:
- ComposerAttachmentHintRuntimeTest renders the actual new-thread screen, checks
  absence of the hint and that missing media still disables Create Thread.
- AppBottomRippleRuntimeTest holds each real bottom button, checks pixels outside
  the central circle stay unchanged and pixels inside show feedback. Existing
  navigation, selection, disabled-state and hit-target tests remain unchanged.
- AppBottomTransparencyRuntimeTest changes 20/0/100 on the real bar, samples
  surface pixels and confirms opaque icon pixels remain.
- BottomBarTransparencyStoreTest covers missing key, clamping, unrelated limits
  and a real DataStore reopen. BottomNavigationSettingsTest checks the scale and
  latest-state update. BottomBarAppearanceTest covers percent-to-alpha mapping.

No Android runtime or red-before/green-after execution is claimed in this
SDK/device-less environment. API compilation is not Android runtime. Full
LOCAL CHECK, all three Python suites and full device-smoke of all eight modules
must be rerun and fresh reports reviewed on the owner's computer before sending.
No performance PASS, GitHub push or release authorization is implied.

Manual: hold/tap all five buttons in light/dark/custom themes, test taps near
cell edges, disabled Write, unchanged selected-tab color, unread badge 1/99/99+,
new-thread form with and without attachments and original submit validation.

Scope: these confirmed changes plus the requested transparency setting; composer header/formatting geometry
from the preceding screenshot discussion is not changed in this patch.
