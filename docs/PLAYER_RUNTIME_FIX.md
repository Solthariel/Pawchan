# Runtime corrections for the 0.1.25 player replacement candidate

This is a follow-up to the supplied engine replacement, not a new release.
It does not change version numbers, publish updates, replace FFmpeg binaries or
include the earlier Windows-only test corrections.

## Changes

- PCM capacity reserves at least 400 ms / four platform minimum buffers for up to
  2x playback. On API 31+, startup uses a separate 10 ms threshold. Creating the
  track explicitly applies the requested play/pause state. Volume and mute no
  longer reapply playback parameters.
- A rejected live speed is contained in the PCM adapter. Native timing and the UI
  retain the last accepted speed instead of crashing the main thread or claiming
  the rejected speed was applied.
- Audio presence comes from opened stream metadata. A silent video's mute button
  is disabled and the command does not modify the remembered mute preference.
- Audio focus and the noisy-output receiver belong to the session across
  viewer/PiP/viewer, not to two competing Activity adapters. Silent video does not
  request audio focus. Focus callbacks are dispatched on the main looper.
- A live, host-owned permission accompanies PiP. PIN protection no longer blocks
  all PiP unconditionally: an unlocked host can enter. The actual lock deadline
  revokes permission in the background; the external surface is detached, audio
  pauses, position is remembered and the PiP task closes. The session retains a
  permission observer even while parked between windows. Returning media is not
  opened over the PIN screen. Screen-off also closes PiP.
- `PawchanVideoStall` reports a stationary requested-playing position every three
  seconds with buffering state, PCM track state, queued frames and speed. It does
  not log media URLs, paths, cookies or post text.

With lock delay **Immediate**, moving the main Activity to the background locks
it immediately, so PiP also closes immediately. A nonzero configured delay allows
PiP until that deadline. This is deliberate; security is not disabled for PiP.

## Verified in this environment

- Production Java bridge: compilation with Android types, JDK `--release 17`,
  `-Xlint:all -Werror`.
- Focused Kotlin boundary compilation and 21 JUnit tests for recovery, mute,
  accepted/rejected speed, loading intent, privacy revocation and lock deadlines.
- 19 existing range publication / EOF / cancellation / real local HTTP tests.
- Production PCM adapter against an isolated strict Android policy model:
  217 assertions. The same regression rejects the original undersized buffer.
  This model is NOT a real AudioTrack or a latency/performance measurement.
- Existing C++ Timeline regression, warnings treated as errors.
- KtLint and Detekt for all 24 changed/new Kotlin files, no suppressed rules.
- Focused compilation of PiP, panel, lifecycle, real PIN implementation and the
  affected instrumentation tests. Neighboring UI/domain boundary stubs are used
  where needed: this is NOT a complete Android module build.

## Not verified here

The normal Gradle test command reached project configuration and stopped with
`SDK location not found`. No full LOCAL CHECK, APK build, Android Lint or device
instrumentation result is claimed for this patch. Native decode/JNI integration
was not rerun; C++ production sources are unchanged. A real phone is still needed
for actual AudioTrack, PiP, screen lock, vendor MediaCodec, audio focus and load.

The reported first-Play failure and repeated freeze near one second have NOT been
reproduced on their actual files here. Explicit PCM startup and the focus ownership
fix address plausible failure paths, but are not proof those reports are solved.
The Qualcomm MediaCodec BAD_VALUE from the supplied log is also not fixed here.
Please keep two original repro files/URLs if either symptom persists.

## Android checks to run

1. Build and install the same Debug candidate. Use the same two problematic clips.
2. First Play must start without pause/play. Let each clip run beyond 2 seconds.
3. On audible clips cycle 1x -> 1.5x -> 2x -> 0.5x, both playing and paused.
   Check for crashes, time continuity and audio/video sync.
4. Silent clip: mute disabled; opening an audible clip must retain the previous
   real mute preference. Background audio must not pause a silent clip.
5. PiP -> app -> PiP while playing and paused: same session/position, no extra
   audio-focus pause, no duplicate audio or lost surface.
6. Enable PIN, unlock, configure 30-second delay, enter PiP: it should work before
   the deadline, then disappear and stop sound on lock. A stale PiP control must
   not restart it. Returning to the app still requires PIN.
7. Check Immediate delay, screen-off, rotation and PiP close/expand separately.
   Check ordinary Home/background without PiP still pauses.
8. If playback stalls, capture full Logcat including `PawchanVideoStall`,
   `PawchanPcm`, `PawchanVideoHandoff`, `AudioTrack`, `MediaCodec`, `AndroidRuntime`.

The existing engine instrumentation fixture test now waits beyond 1.2 seconds
before touching playback (short one-second black fixtures retain their 200 ms
check) and tests real 1.5x on H.264/AAC. A mute-enable/disable UI test was added.
These device tests were compiled in the focused check, but not executed here.
