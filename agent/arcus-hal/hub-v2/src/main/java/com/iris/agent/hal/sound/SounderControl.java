/*
 * Copyright 2019 Arcus Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.iris.agent.hal.sound;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteResultHandler;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.attributes.HubAttributesService;
import com.iris.agent.hal.IrisHal;
import com.iris.agent.hal.Model;
import com.iris.agent.hal.SounderMode;
import com.iris.agent.http.HttpService;
import com.iris.agent.util.ThreadUtils;
import com.iris.messages.capability.HubSoundsCapability;
import com.iris.messages.capability.HubVolumeCapability;

public class SounderControl implements ExecuteResultHandler {
   private static final Logger log = LoggerFactory.getLogger(SounderControl.class);

   private static final String PLAYTONES = "/usr/bin/play_tones";
   private static final String PLAYER = "/usr/bin/play";
   private static final String AMIXER = "/usr/bin/amixer";
   private static final long PLAYTONES_TIMEOUT_IN_MS = TimeUnit.MILLISECONDS.convert(3, TimeUnit.MINUTES);
   private static final long AMIX_TIMEOUT_IN_MS = TimeUnit.MILLISECONDS.convert(5, TimeUnit.SECONDS);

  // private final Map<SounderMode,String> tones;
   private final SoundConfig sounds;
   private final Map<String,String> volumesV2;          // Volumes are controlled through the player.
   private final Map<String,String> volumesV3;          // Volumes are controlled through a mixer.
   private final AtomicReference<PlayState> state;

   private HubAttributesService.Attribute<String> source;
   private HubAttributesService.Attribute<String> level;
   private HubAttributesService.Attribute<Boolean> playing;
   
   private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
   private Queue<CommandLine> commands = new LinkedList<CommandLine>();
   private SoundFile currentSound = null;

   public static final SounderControl create() {
      return new SounderControl();
   }

   private SounderControl() {
	   
	   
      this.state = new AtomicReference<>();

      this.source = HubAttributesService.ephemeral(String.class, HubSoundsCapability.ATTR_SOURCE, "");

      this.level = HubAttributesService.persisted(String.class, HubVolumeCapability.ATTR_VOLUME, HubVolumeCapability.VOLUME_HIGH);
      this.playing = HubAttributesService.ephemeral(Boolean.class, HubSoundsCapability.ATTR_PLAYING, false);

      this.source.setReportedOnConnect(false);
      this.source.setReportedOnValueChange(false);

      this.level.setReportedOnConnect(false);
      this.level.setReportedOnValueChange(true);

      this.playing.setReportedOnConnect(false);
      this.playing.setReportedOnValueChange(false);

      // Tones are 0-100
      this.volumesV2 = new HashMap<>();
      this.volumesV2.put(HubVolumeCapability.VOLUME_OFF, "0");
      this.volumesV2.put(HubVolumeCapability.VOLUME_LOW, "10");
      this.volumesV2.put(HubVolumeCapability.VOLUME_MID, "40");
      this.volumesV2.put(HubVolumeCapability.VOLUME_HIGH, "100");

      // A mixer is 0-63
      this.volumesV3 = new HashMap<>();
      this.volumesV3.put(HubVolumeCapability.VOLUME_OFF, "0");
      this.volumesV3.put(HubVolumeCapability.VOLUME_LOW, "10");
      this.volumesV3.put(HubVolumeCapability.VOLUME_MID, "25");
      this.volumesV3.put(HubVolumeCapability.VOLUME_HIGH, "63");

      
      this.sounds = SoundConfig.getInstance();
      
   }

   public void play(SounderMode mode, int duration) {
      if (SounderMode.NO_SOUND.equals(mode)) {
         log.trace("stopping sounds...", mode, duration);
         stop();
         // Give the last sound a moment to clear or player may return an error if still in use...
         ThreadUtils.sleep(500, TimeUnit.MILLISECONDS);
         return;
      }
      
      String model = IrisHal.getModel();
      SoundFile sound;
      if (Model.isV3(model)) {
          sound = sounds.get(Model.IH300,mode);
      } else {
          sound = sounds.get(Model.IH200,mode);
      }
      if (sound == null) {
          log.warn("No sounds found for {}, {}", model, mode);
          stop();
          return;
      }

      if (!state.compareAndSet(null, new PlayState(sound.getInfo(),TimeUnit.NANOSECONDS.convert(duration,TimeUnit.SECONDS),duration == 0 ? 0 : Long.MAX_VALUE))) {
         log.debug("another sound is currently playing, discarding new sound: {}", sound.getInfo());
         return;
      }

      log.trace("playing sound: tone={}, duration={}s", mode, duration);
      currentSound = sound;
      play(sound);
   }

   // Should this go away?
   // Or at least completely reworked
   public void play(String url, int repeat) {
      if (!state.compareAndSet(null, new PlayState(download(url),-1,repeat))) {
         log.trace("another sound is currently playing, discarding new sound");
         return;
      }

      log.trace("playing sound: url={}, repeats={}", url, repeat);
      //play(url);
   }

   private void play(@Nullable SoundFile sound) {
      PlayState currentState = state.get();
      if (currentState == null) {
         return;
      }

      long currentTimestamp = System.nanoTime();
      if (currentState.endTimestamp > Long.MIN_VALUE && currentTimestamp >= currentState.endTimestamp) {
         log.trace("play duration expired, stopping sounder");

         stop();
         return;
      }

      if (currentState.repeats < 0) {
         log.trace("play repeats exceeded, stopping sounder");

         stop();
         return;
      }

      if (sound == null) {
          stop();
          return;
      }

      DefaultExecutor executor = new DefaultExecutor();
      executor.setExitValue(0);

      ExecuteWatchdog watchdog = new ExecuteWatchdog(PLAYTONES_TIMEOUT_IN_MS);
      executor.setWatchdog(watchdog);
      
      Queue<CommandLine> queue;
            
      // Figure out the version we need to play
      switch (sound.getVersion()) {
          case IH200:
              queue = playerV2(sound);
              ByteArrayInputStream is = new ByteArrayInputStream(currentState.content.getBytes(StandardCharsets.US_ASCII));
              executor.setStreamHandler(new PumpStreamHandler(null,null,is));
              break;
          case IH300:
          case IH304:
              queue = playerV3(sound);
              break;
          default:
              log.error("No Player Found");
              stop();
              return;
      }
      try {
          currentState.repeats--;
          playing.set(true);
          if (sound != null) {
             source.set(sound.getMode().toString());
          }

          this.commands.clear();
          this.commands.addAll(queue);
          log.warn("Debug: queue {}, commands {}", queue, commands);
          log.warn("Debug: playing {}", commands.peek());
          executor.execute(commands.poll(), this);
       } catch (IOException ex) {
          stop();
       }      
   }

   private Queue<CommandLine> playerV2(SoundFile file) {
       String volume = volumesV2.get(level.get());
       volume = (volume != null) ? volume : "100";
       
       CommandLine cmd = new CommandLine(PLAYTONES);
       cmd.addArgument("-v");
       cmd.addArgument(volume);

       cmd.addArgument("-f");
       cmd.addArgument("/dev/stdin");

       Queue<CommandLine> q = new LinkedList<CommandLine>();
       q.add(cmd);
       return q;
   }

   private Queue<CommandLine> playerV3(SoundFile file) {
	    log.trace("Debug: SoundFile = {}", file);
	   
       String volume = volumesV3.get(level.get());
       volume = (volume != null) ? volume : "63";

       CommandLine amix  = new CommandLine(AMIXER);
       amix.addArgument("sset");
       amix.addArgument("Speaker");
       amix.addArgument(volume);

       // This should be instant.
       DefaultExecutor executor = new DefaultExecutor();
       executor.setExitValue(0);
       ExecuteWatchdog watchdog = new ExecuteWatchdog(AMIX_TIMEOUT_IN_MS);
       executor.setWatchdog(watchdog);
       try {
           executor.execute(amix);
       } catch (IOException e) {
           log.error("Could not adjust volume", e);
       }
           
       Queue<CommandLine> q = new LinkedList<CommandLine>();

       CommandLine player = new CommandLine(PLAYER);
       player.addArgument(file.getInfo());
       q.add(player);
       
       if (file.getInfo2() != null) {
	       player = new CommandLine(PLAYER);
	       player.addArgument(file.getInfo2());
	       q.add(player);
       }

       return q;
   }

   
   private void stop() {
      playing.set(false);
      source.set("");
      currentSound = null;
      state.set(null);
   }

   @Override
   public void onProcessComplete(int exitValue) {
	   log.trace("Playing complete with exit {}", exitValue);

	   // For v2, we handle repeats
	   if ((currentSound != null) && (currentSound.getVersion() == Model.IH200)) {
		   play(currentSound);
		   return;
	   }
	   DefaultExecutor executor = new DefaultExecutor();
	   executor.setExitValue(0);

	   log.trace("Executor Ready {}", exitValue);
	   log.trace("Debug Next: playing {}", commands.peek());
	   CommandLine next;
	   int delay;

	   // See if the file should be played continuously - if so, it will have "continuous" in it's name
	   //  (which is part of the command here).  In the future, we may want have a better way to
	   //  signal this, perhaps an endless repeat, but that would be too involved at this point
	   //  since we are close to a release...
	   if (commands.peek().toString().contains("continuous")) {
          // If playing has been stopped, remove from queue
          if (!playing.get()) {
             next = commands.poll();
          } else {
             next = commands.peek();
          }
	      delay = 10; // Just a short delay when playing a sound continuously
	   } else {
	      next = commands.poll();
	      delay = 500; // Normal half second delay
	   }
	   // Play unless next sound is null
	   if (next == null)  {
		   play(null);
	   } else {
		   scheduler.schedule(() -> { 
			   try {
				   executor.execute(next, this);
			   } catch (Exception e) {
		           log.error("Could not play next file", e);
			   }
		   },
		   delay,
		   TimeUnit.MILLISECONDS);
	   }
   }

   @Override
   public void onProcessFailed(@Nullable ExecuteException e) {
      play(null);

   }

   private String download(String url) {
      try {
         return HttpService.contentAsString(url);
      } catch (Throwable th) {
         log.error("failed to load sound file: {}", url);
         return "";
      }
   }
   
   private static final class PlayState {
      final String content;
      final long endTimestamp;
      long repeats;

      public PlayState(String content, long duration, long repeats) {
         this.content = content;
         this.endTimestamp = (duration > 0) ? (System.nanoTime() + duration) : Long.MIN_VALUE;
         this.repeats = repeats;
      }
   }
}

