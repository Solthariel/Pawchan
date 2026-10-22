package android.media;
/** TEST MODEL ONLY. Not an Android AudioTrack implementation. */
public final class AudioTrack {
 public static final int MODE_STREAM=1, WRITE_NON_BLOCKING=1;
 public static AudioTrack last; public static boolean rejectSpeed;
 public int buffer, threshold, params, plays, pauses, head; public boolean playing,released;
 public static int getMinBufferSize(int a,int b,int c){return 7680;}
 public void setVolume(float v){}
 public void setPlaybackParams(PlaybackParams p){
   if (p.speed>1f && (buffer<23056 || rejectSpeed)) throw new IllegalArgumentException("buffer size");
   params++; playing=true;
 }
 public int setStartThresholdInFrames(int v){threshold=v; return v;}
 public void play(){playing=true;plays++;} public void pause(){playing=false;pauses++;}
 public int write(byte[] d,int offset,int size,int mode){if(!playing)return 0; head+=size/4;return size;}
 public int getPlaybackHeadPosition(){return head;} public int getPlayState(){return playing?3:2;}
 public void flush(){head=0;} public void release(){released=true;}
 public static final class Builder {private int size;public Builder setAudioAttributes(AudioAttributes v){return this;}
 public Builder setAudioFormat(AudioFormat v){return this;} public Builder setBufferSizeInBytes(int v){size=v;return this;}
 public Builder setTransferMode(int v){return this;} public AudioTrack build(){last=new AudioTrack();last.buffer=size;return last;} }
}
