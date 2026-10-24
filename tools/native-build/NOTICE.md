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

The checked-in `video-ffmpeg/native-prebuilt` files are unchanged. Their manifest
and recorded hashes describe their HISTORICAL build, including the former shell
scripts; those values must not be rewritten to pretend that existing binaries
were built by this new tool. A new verified bundle has its own manifest. Replacing
bundled libraries requires an actual NDK build, application checks and runtime
verification; passing orchestration tests with simulated compilers is not enough.

Reference projects:
- https://github.com/andrewpozdnakov7-jpg/Dashchan_2
- https://github.com/Mishiranu/Dashchan-Webm
