# Upload budget, progress and stable unlock (0.1.25)

Based on published 0.1.24 (`1982b6e`). This is a source overlay, not an APK.

## Contracts and ownership

- `max_files_size` (KiB) from Dvach board metadata is the **aggregate** attachment
  budget, not a per-file allowance. Verified against the public web client's
  `FormFiles.totalSizeBytes`/`hasAvailableSize` implementation on 2026-10-21.
  Result=2 account metadata `maxFilesSize` overrides that budget; missing/invalid
  entitlement does not grant extra files or bytes. Domain keeps separate local
  per-file (50 MiB), selection (64 MiB) and file-count (8) safety limits.
- `PostingRules.maxTotalFileBytes` and `AccountUploadLimits.maxTotalFileBytes`
  express this contract. Existing `maxFileBytes` is the independent per-file
  safety constraint. No speculative 20 MiB video cap is added.
- The existing status line reports size preparation/failure/over-budget, upload
  percentage, then server wait. The size label alone is red for a negative
  remainder. Eligibility uses exact bytes, not rounded display values; a one-byte
  excess is displayed as `−<0,1 МБ`, never an apparently permissible zero.
- `ComposerUploadPreparation` runs finite latest-selection jobs owned by the composer
  scope/ViewModel. Byte transforms run on the supplied background dispatcher.
  Only matching source identity + edit options can publish a cache result.
  Cache values are transient, bounded by the selection cap, not persisted in
  draft files; unchanged source bytes are borrowed, not copied. Send reuses the
  same prepared bytes (including a generated unique hash). Invalid names or
  failed transforms keep the selection editable and disable sending.
- Cancellation reaches the preparation job and is checked between transforms; Android
  bitmap codecs themselves are synchronous and cannot be interrupted mid-codec.
  No global worker, detached scope, notification, extra progress bar or library
  borrowed from another client is introduced.
- Posting methods accept an explicit optional `PostingProgressObserver`; all
  repository decorators forward it. `PostingUploadTracker` counts media bytes
  written through OkHttp/Okio with bounded buffering, not multipart headers or
  estimated elapsed time. Updates are limited to 100 ms except start/completion.
  HTTP replay resets the request counter. A coroutine-job guard rejects callbacks
  after cancellation/completion. A server response, not 100%, determines success.
  These are bytes handed to the transport, not proof of server receipt.

## Layout

- Plain bottom navigation uses an opaque rectangular surface behind the existing
  straight divider. Bubble mode retains its capsule. Item bounds, selection pill,
  height and scrollbar geometry are unchanged.
- Unlock uses a content-sized block centered inside safe bounds, with scrolling
  on small/landscape screens. Title, dots, keypad and fingerprint stay present;
  two reserved status text lines replace conditional error + checking rows.
  Checking takes precedence over an old error. Cancellation notice is shortened
  to «Введите PIN или используйте отпечаток». System biometric UI is unchanged.
  PIN length, attempt limits, cryptography, back handling and lock visibility
  protections are not weakened.

## Verification

- `sh scripts/local-check.sh`: **PASS**, 13m 48s. Debug and release assembly,
  `qualityCheck`, Android Lint, source policy, release metadata, KtLint and Detekt.
- Normal JVM suite: **1189 tests**, zero failures/errors/skips (298 XML suites).
- Native-graphics Robolectric SDK 36 host run: **16 tests PASS** — 7 bottom
  outline, 5 floating-workspace, 2 stable PIN, 2 remaining-size color scenarios.
- Final app + composer AndroidTest Kotlin compilation: **PASS**.
- Publishing Python suites: **42 + 12 PASS**.
- Device smoke and release Macrobenchmark: **NOT RUN**. No connected Android
  device or KVM emulator is available. The system biometric window, real-device
  IME/insets and upload performance are not certified by these host tests.

The external host harness does not ship in the patch and does not weaken the
project's checks. No Gradle configuration, suppression or baseline is changed
in the delivered overlay.

## Device follow-up (not certified by the host run)

1. Toggle bubbles while scrolling a catalog and a thread. The plain bar's upper
   corners must mask list pixels; selected tab and right fast-scroll rail must
   keep their positions and hit targets.
2. Open/cancel the system biometric prompt, enter a wrong PIN, then a correct
   PIN. Header, four dots, keys and fingerprint row must retain their bounds.
   Also test a small landscape screen and enlarged system fonts.
3. For a 40 MiB budget, select 20 + 21 MiB of unmodified media: `−1 МБ`, red size
   number, disabled Send, intact attachments. Remove one: immediate recovery.
   Check exactly-at-budget and a one-byte excess without relying on rounding.
4. Reencode an image or strip metadata; while preparing, Send is disabled. The
   resulting remainder must use the prepared payload, and Send must not generate
   another hash or encode a second copy.
5. On a slow connection, text-only posting has no percentage; media has gradual
   percentage updates. End of upload changes to server wait, never premature
   success. Interrupt/retry/close: no old percentage in the next attempt. A
   network/server/captcha failure keeps the draft and is not counted as success.
