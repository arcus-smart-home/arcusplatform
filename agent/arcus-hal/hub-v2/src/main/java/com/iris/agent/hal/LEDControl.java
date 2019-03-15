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
package com.iris.agent.hal;

import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.hal.LEDState;
import com.iris.agent.storage.FileContentListener;
import com.iris.agent.storage.FileContentMonitor;
import com.iris.agent.storage.StorageService;

public class LEDControl implements FileContentListener {
   private static final Logger log = LoggerFactory.getLogger(LEDControl.class);
   public static final String LED_MODE = "tmp:///ledMode";

   private FileContentMonitor monitor;
   private LEDState state = LEDState.UNKNOWN;

   @SuppressWarnings("null")
   private LEDControl() {
   }

   public static final LEDControl create() {
      LEDControl control = new LEDControl();
      try {
         control.monitor = StorageService.getFileMonitor(URI.create(LED_MODE));
         control.monitor.addListener(control);
      } catch (IOException ex) {
         log.warn("could not install led state monitor: {}", ex.getMessage(), ex);
      }

      return control;
   }

   public void shutdown() {
      monitor.cancel();
   }

   public synchronized void set(LEDState state) {
	   set(state,0);
   }

   public synchronized void set(LEDState state, int duration) {
	  if (state == null) return;
      if (this.state.equals(state)) return;
      
      log.trace("updating led state from {} to {}...", this.state, state);
      this.state = state;

      String mode = state.toString().toLowerCase().replace('_', '-');
      if (duration > 0) {
    	  mode += " " + duration;
      }
      try (Writer writer = StorageService.getWriter(LED_MODE,StandardCharsets.UTF_8)) {
         IOUtils.write(mode, writer);
      } catch (Exception ex) {
         log.warn("failed to set led state: {}", ex.getMessage(), ex);
      }
   }

   public synchronized LEDState get() {
      return state;
   }

   @Override
   public synchronized void fileContentsModified(FileContentMonitor monitor) {
      String content = monitor.getContents();
      if (content == null) {
         state = LEDState.UNKNOWN;
         return;
      }

      try {
         String vals[] = content.split(" "); // May have a trailing duration after the mode...
         state = LEDState.valueOf(vals[0].toUpperCase().replace('-', '_'));
      } catch (Exception ex) {
         log.debug("unknown led mode: {}", content);
         state = LEDState.UNKNOWN;
      }
   }
}

