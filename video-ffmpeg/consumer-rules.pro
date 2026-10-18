# JNI entry points and callbacks are resolved by name by the imported native core.
-keep class com.solthariel.pawchan.ffmpeg.VideoPlayer$Holder { *; }
-keep class com.solthariel.pawchan.ffmpeg.VideoPlayer$NativeBridge { *; }
-keepclasseswithmembernames class * { native <methods>; }
