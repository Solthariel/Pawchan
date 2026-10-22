# PCM host regression model

Compiles the production `PcmOutput.java` with isolated Android model classes.
The models are outside Android sourceSets and must NEVER be packaged in the APK.
They deliberately require explicit play and reject an undersized speed buffer.
This verifies application policy only, not Android playback, latency or driver behavior.

Run from project root (JDK 17+):
```sh
mkdir -p /tmp/paw-pcm-test
javac --release 17 -d /tmp/paw-pcm-test $(find video-ffmpeg/tests/pcm-host -name '*.java') video-ffmpeg/src/main/java/com/solthariel/pawchan/ffmpeg/PcmOutput.java
java -cp /tmp/paw-pcm-test com.solthariel.pawchan.ffmpeg.PcmOutputPolicyTest
```
