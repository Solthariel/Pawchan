package com.solthariel.pawchan.ffmpeg;
import android.media.AudioTrack;
/** Exercises PRODUCTION PcmOutput against a strict platform model, NOT real Android. */
public final class PcmOutputPolicyTest {
 private static int tests;
 private static void check(boolean value,String message){tests++;if(!value)throw new AssertionError(message);}
 public static void main(String[] args){
  try(PcmOutput pcm=new PcmOutput()){
   pcm.play(true);pcm.configure();AudioTrack t=AudioTrack.last;
   check(t.buffer>=76800,"2x buffer reserve");check(t.threshold==480,"small startup threshold");
   check(t.plays==1,"explicit play after late configure");
   check(pcm.write(new byte[4096],0)==4096,"first play writes without pause/play");
   check(pcm.sound(100,false,1500)==1500,"1.5x accepted");
   check(pcm.sound(100,false,2000)==2000,"2x accepted");
   int before=t.params;pcm.sound(20,true,2000);pcm.sound(100,false,2000);
   check(t.params==before,"mute/volume do not reapply speed");
   pcm.play(false);pcm.sound(100,false,500);check(!t.playing,"speed preserves pause");
   check(pcm.write(new byte[4],0)==0,"paused write does not advance");
   pcm.flush();check(!t.playing,"seek preserves pause");pcm.play(true);pcm.flush();
   check(t.playing,"seek preserves play");
   AudioTrack.rejectSpeed=true;check(pcm.sound(100,false,1500)==500,"rejected speed keeps accepted speed");
   AudioTrack.rejectSpeed=false;pcm.cancel();check(pcm.write(new byte[4],0)==-1,"cancel wakes pump");
  }
  check(AudioTrack.last.released,"track released");
  try(PcmOutput pcm=new PcmOutput()){
   pcm.configure();check(!AudioTrack.last.playing,"preload stays paused");
   pcm.play(true);for(int i=0;i<200;i++)check(pcm.write(new byte[1024],0)==1024,"continues beyond first second");
  }
  android.os.Build.VERSION.SDK_INT=30;
  try(PcmOutput pcm=new PcmOutput()){
   pcm.play(true);pcm.configure();check(AudioTrack.last.threshold==0,"pre-31 avoids new API");
   check(AudioTrack.last.playing,"pre-31 explicitly starts");
  }
  System.out.println("PCM policy assertions: "+tests+" PASS (platform model, not device)");
 }
}
