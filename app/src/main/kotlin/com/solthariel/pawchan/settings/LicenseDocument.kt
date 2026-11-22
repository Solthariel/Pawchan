package com.solthariel.pawchan.settings

internal data class LicenseDocument(
    val title: String,
    val summary: String,
    val path: String,
    val source: String? = null,
)

internal val LICENSE_DOCUMENTS =
    listOf(
        LicenseDocument(
            "Pawchan",
            "Licensed under the GNU General Public License version 3",
            "licenses/Pawchan-GPL-3.0.txt",
            "https://github.com/Solthariel/Pawchan",
        ),
        LicenseDocument(
            "FFmpeg",
            "Licensed under the GNU Lesser General Public License version 2.1",
            "licenses/FFmpeg-LGPL-2.1.txt",
            "https://ffmpeg.org/",
        ),
        LicenseDocument(
            "FFmpeg",
            "Licensed under the GNU Lesser General Public License version 3",
            "licenses/FFmpeg-LGPL-3.txt",
            "https://ffmpeg.org/",
        ),
        LicenseDocument(
            "mpv",
            "Лицензии и сведения об исходниках mpv необходимо проверить перед распространением",
            "licenses/Player-GPL-3.0.txt",
            "https://github.com/mpv-android/mpv-android",
        ),
        LicenseDocument(
            "libmpvKt",
            "Kotlin/JNI wrapper — Apache License 2.0",
            "licenses/libmpvkt-Apache-2.0.txt",
            "https://github.com/yuroyami/libmpv-kotlin-multiplatform",
        ),
        LicenseDocument(
            "libmpvKt",
            "Third-party copyright and license notices, including mpv-android",
            "licenses/libmpvkt-NOTICE.txt",
            "https://github.com/yuroyami/libmpv-kotlin-multiplatform",
        ),
        LicenseDocument(
            "Android NDK / libc++",
            "Android NDK / LLVM and third-party license notices",
            "licenses/android-ndk-sysroot-NOTICE.txt",
            "https://developer.android.com/ndk",
        ),
        LicenseDocument(
            "dav1d",
            "Copyright © 2018–2025, VideoLAN and dav1d authors\nLicensed under the BSD 2-Clause License",
            "licenses/dav1d-BSD.txt",
            "https://code.videolan.org/videolan/dav1d",
        ),
        LicenseDocument(
            "Roboto Flex",
            "Copyright 2017 The Roboto Flex Project Authors\nLicensed under the SIL Open Font License 1.1",
            "licenses/fonts/robotoflex-OFL.txt",
            "https://github.com/TypeNetwork/Roboto-Flex",
        ),
        LicenseDocument(
            "Roboto Mono",
            "Copyright 2015 The Roboto Mono Project Authors\nLicensed under the SIL Open Font License 1.1",
            "licenses/fonts/robotomono-OFL.txt",
            "https://github.com/googlefonts/robotomono",
        ),
        LicenseDocument(
            "Roboto Serif",
            "Copyright 2020 The Roboto Serif Project Authors\nLicensed under the SIL Open Font License 1.1",
            "licenses/fonts/robotoserif-OFL.txt",
            "https://github.com/googlefonts/RobotoSerif",
        ),
        LicenseDocument(
            "Material Symbols",
            "Google Material Symbols Rounded\nLicensed under the Apache License version 2.0",
            "licenses/material-symbols.txt",
            "https://github.com/google/material-design-icons",
        ),
        LicenseDocument(
            "Значки форумов",
            "Source and attribution details are included with the app",
            "licenses/forum-icons.txt",
            "https://github.com/Solthariel/Pawchan",
        ),
    )
