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
package com.iris.agent.logging;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.jdt.annotation.Nullable;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;

public class IrisAgentAppender extends OutputStreamAppender<ILoggingEvent> {
   private final ByteRingBuffer os = new ByteRingBuffer(512*1024);
   private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

   private boolean notifyListeners;
   private Level listenerLogLevel = Level.DEBUG;

   public IrisAgentAppender() {
   }

   public void addListener(Listener listener) {
      listeners.add(listener);
   }

   public void removeListener(Listener listener) {
      listeners.remove(listener);
   }

   public void setNotifyListeners(boolean notify) {
      notifyListeners = notify;
   }

   public void setListenerLogLevel(Level level) {
      this.listenerLogLevel = level;
   }

   @Override
   public void start() {
      setOutputStream(os);
      super.start();
   }

   @Override
   @SuppressWarnings("null")
   public void append(ILoggingEvent event) {
      if (notifyListeners && event.getLevel().isGreaterOrEqual(listenerLogLevel)) {
         for (Listener listener : listeners) {
            listener.appendLogEntry(event);
         }
      }

      synchronized (os) {
         super.append(event);
      }
   }

   public String getLogs() {
      byte[] clone = new byte[os.buffer.length];
      synchronized (os) {
         int first = os.buffer.length - os.idx;
         System.arraycopy(os.buffer, os.idx, clone, 0, first);
         System.arraycopy(os.buffer, 0, clone, first, os.idx);
      }

      String raw = new String(clone, StandardCharsets.UTF_8);
      String lines = raw;

      int idx = raw.indexOf(IrisAgentSotConverter.SOT);
      if (idx >= 0) {
         lines = raw.substring(idx+IrisAgentSotConverter.SOT_LENGTH);
      }

      return lines.replace(IrisAgentSotConverter.SOT_CHAR, '\n');
   }

   private static final class ByteRingBuffer extends OutputStream {
      private final byte[] buffer;
      private int idx = 0;

      private ByteRingBuffer(int size) {
         this.buffer = new byte[size];
      }

      @Override
      public void write(int b) throws IOException {
         buffer[idx] = (byte)b;
         idx = (idx + 1) % buffer.length;
      }

      @Override
      public void write(@Nullable byte[] b) throws IOException {
         if (b == null) throw new NullPointerException();
         write(b, 0, b.length);
      }

      @Override
      public void write(@Nullable byte[] b, int off, int len) throws IOException {
         if (b == null) throw new NullPointerException();

         int o = off;
         int l = len;
         while (l > 0) {
            int s = l;
            if (s > buffer.length) {
               s = buffer.length;
            }

            append(b, o, s);
            o += s;
            l -= s;
         }
      }

      private void append(byte[] b, int off, int len) throws IOException {
         int l = len;
         int o = off;
         int rem = buffer.length - idx;

         if (len >= rem) {
            System.arraycopy(b, o, buffer, idx, rem);
            l -= rem;
            o += rem;
            idx = 0;
         }

         if (len > 0) {
            System.arraycopy(b, o, buffer, idx, l);
            idx = idx + l;
         }
      }
   }

   public static interface Listener {
      void appendLogEntry(ILoggingEvent event);
   }
}

