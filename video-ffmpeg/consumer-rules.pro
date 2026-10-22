# Narrow JNI boundary and callbacks invoked from FFmpeg's custom IO/PCM adapters.
-keep class com.solthariel.pawchan.ffmpeg.PawNative { *; }
-keepclassmembers class com.solthariel.pawchan.ffmpeg.PublishedInput {
    int read(long, byte[], int);
    long length();
    boolean complete();
}
-keepclassmembers class com.solthariel.pawchan.ffmpeg.PcmOutput {
    void configure();
    int write(byte[], int);
    void flush();
    long remaining();
}
