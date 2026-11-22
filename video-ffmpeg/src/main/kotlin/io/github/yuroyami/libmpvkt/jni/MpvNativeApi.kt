package io.github.yuroyami.libmpvkt.jni

/** Marks the unsafe JNI binding: session owners must manage handles and lifetimes. */
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "Raw mpv JNI binding: callers must manage native handles, threading and lifetimes.",
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
public annotation class MpvNativeApi
