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
package com.iris.agent.os.watchdog;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;

import com.sun.jna.Platform;

@SuppressWarnings("null")
public final class WatchdogNative {
   private static final Impl IMPL;

   public static enum Flag {
      OVERHEAT,
      FANFAULT,
      EXTERN1,
      EXTERN2,
      POWERUNDER,
      CARDRESET,
      POWEROVER,
      SETTIMEOUT,
      MAGICCLOSE,
      PRETIMEOUT,
      ALARMONLY,
      KEEPALIVEPING
   };

   static {
      Impl impl = null;

      if (Platform.isLinux()) {
         impl = WatchdogNativeLinux.INSTANCE;
      }

      IMPL = impl;
   }

   public static boolean isAvailable() {
      return IMPL != null;
   }

   public static double getWatchdogTemp(FileOutputStream os) {
      return IMPL.getWatchdogTemp(fd(os));
   }

   public static int getWatchdogTimeout(FileOutputStream os) {
      return IMPL.getWatchdogTimeout(fd(os));
   }

   public static void setWatchdogTimeout(FileOutputStream os, int timeout) {
      IMPL.setWatchdogTimeout(fd(os), timeout);
   }

   public static void setWatchdogEnable(FileOutputStream os) {
      IMPL.setWatchdogEnable(fd(os));
   }

   public static Object getWatchdogInfo(FileOutputStream os) {
      return IMPL.getWatchdogInfo(fd(os));
   }

   public static long getWatchdogInfoOptions(Object watchdogInfo) {
      return IMPL.getWatchdogInfoOptions(watchdogInfo);
   }

   public static long getWatchdogInfoFirmwareVersion(Object watchdogInfo) {
      return IMPL.getWatchdogInfoFirmwareVersion(watchdogInfo);
   }

   public static String getWatchdogInfoIdentity(Object watchdogInfo) {
      return IMPL.getWatchdogInfoIdentity(watchdogInfo);
   }

   public static Set<Flag> getWatchdogInfoFlags(Object watchdogInfo) {
      return IMPL.getWatchdogInfoFlags(watchdogInfo);
   }

   private static int fd(FileOutputStream os) {
      try {
         return sun.misc.SharedSecrets.getJavaIOFileDescriptorAccess().get(os.getFD());
      } catch (IOException ex) {
         throw new RuntimeException(ex);
      }
   }

   interface Impl {
      int getWatchdogTimeout(int fd);
      void setWatchdogTimeout(int fd, int timeout);
      void setWatchdogEnable(int fd);

      Object getWatchdogInfo(int fd);
      double getWatchdogTemp(int fd);
      long getWatchdogInfoOptions(Object watchdogInfo);
      long getWatchdogInfoFirmwareVersion(Object watchdogInfo);
      String getWatchdogInfoIdentity(Object watchdogInfo);
      Set<Flag> getWatchdogInfoFlags(Object watchdogInfo);
   }
}

