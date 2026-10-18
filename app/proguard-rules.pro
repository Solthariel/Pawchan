# LibVLC JNI looks up Java classes, constructors, fields and callback methods by name.
# Keep only the native binding dependency; application code remains shrinkable.

# WorkManager persists this name; keep it stable across release updates.
-keepnames class com.solthariel.pawchan.ArchiveMediaWorker
