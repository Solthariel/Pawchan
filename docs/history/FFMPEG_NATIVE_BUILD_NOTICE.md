> Историческое уведомление об удалённом инструменте сборки. Сохранено для происхождения и атрибуции; описание наличия файлов в оригинальном тексте относится к прежнему состоянию проекта.

# Scope and provenance

This tool replaces Pawchan 0.1.37's `video-ffmpeg/scripts/shared-prepare.sh` and
`shared-build.sh`. Those historical scripts substantially matched the scripts in
Slooop/Dashchan_2 3.2.69, themselves based in part on Mishiranu/Dashchan-Webm.
This rewrite was developed with access to those references. It is not a claim of
clean-room development, erased history, exclusive ownership of the workflow, or
legal clearance. The project's existing GPL and third-party notices remain.

The Kotlin implementation separates source acquisition, archive validation,
process execution, per-ABI recipes and bundle publication. It does not generate
legacy channel-layout/avcodec compatibility wrappers or C symbol stubs. The
single known MOV return-value correction is intentionally retained, identified
in `MovReturnValueFix.kt`, and covered by behavior tests; it is not presented as
a new invention. External FFmpeg/dav1d source licenses continue to apply.

The checked-in `video-ffmpeg/native-prebuilt` bundle now contains an actual
ARM64 FFmpeg 9.0.2 / dav1d 1.5.4 build made by this Kotlin pipeline with NDK
29.0.14206865. Its manifest records the real source archives, input fingerprint
and file hashes; the former 8.1.3 manifest and shell-script hashes are not reused
as the provenance of new binaries. Complete corresponding upstream source
archives accompany the bundle in `video-ffmpeg/corresponding-source`.

Cross-compilation and orchestration tests are not Android playback verification.
Existing authorship, GPL and third-party notices still apply; the Kotlin rewrite
and the FFmpeg version change do not erase upstream or historical provenance.

Reference projects:
- https://github.com/andrewpozdnakov7-jpg/Dashchan_2
- https://github.com/Mishiranu/Dashchan-Webm
