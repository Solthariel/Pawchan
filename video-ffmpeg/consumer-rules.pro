# libmpvkt JNI and stream callbacks, upstream AAR v0.3.0 consumer rules
# JNI entry points are resolved by class and method name.
-keepclasseswithmembernames,includedescriptorclasses class io.github.yuroyami.libmpvkt.jni.MpvNative { native <methods>; }
-keepclasseswithmembernames,includedescriptorclasses class io.github.yuroyami.libmpvkt.jni.MpvSurfaceReferences { native <methods>; }
# stream_cb.cpp resolves these two interfaces with FindClass and calls their methods by signature.
-keep interface io.github.yuroyami.libmpvkt.stream.MpvStreamProvider { *; }
-keep interface io.github.yuroyami.libmpvkt.stream.MpvStream { *; }
-keep class * implements io.github.yuroyami.libmpvkt.stream.MpvStream { public <methods>; }
-keep class * implements io.github.yuroyami.libmpvkt.stream.MpvStreamProvider { public <methods>; }
