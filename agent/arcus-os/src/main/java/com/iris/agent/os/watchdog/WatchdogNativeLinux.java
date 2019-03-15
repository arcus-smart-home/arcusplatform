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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;

public enum WatchdogNativeLinux implements WatchdogNative.Impl {
   INSTANCE;

   public static final int WDIOF_UNKNOWN = -1; // Unknown flag error
   public static final int WDIOS_UNKNOWN = -1; // Unknown status error

   public static final int WDIOF_OVERHEAT = 0x0001; // Reset due to CPU overheat
   public static final int WDIOF_FANFAULT = 0x0002; // Fan failed
   public static final int WDIOF_EXTERN1 = 0x0004; // External relay 1
   public static final int WDIOF_EXTERN2 = 0x0008; // External relay 2
   public static final int WDIOF_POWERUNDER = 0x0010; // Power bad/power fault
   public static final int WDIOF_CARDRESET = 0x0020; // Card previously reset the CPU
   public static final int WDIOF_POWEROVER = 0x0040; // Power over voltage
   public static final int WDIOF_SETTIMEOUT = 0x0080; // Set timeout (in seconds)
   public static final int WDIOF_MAGICCLOSE = 0x0100; // Supports magic close char
   public static final int WDIOF_PRETIMEOUT = 0x0200; // Pretimeout (in seconds), get/set
   public static final int WDIOF_ALARMONLY = 0x0400; // Watchdog triggers a management or other external alarm not a reboot
   public static final int WDIOF_KEEPALIVEPING = 0x8000; // Keep alive ping reply

   public static final int WDIOS_DISABLECARD = 0x0001; // Turn off the watchdog timer
   public static final int WDIOS_ENABLECARD = 0x0002; // Turn on the watchdog timer
   public static final int WDIOS_TEMPPANIC = 0x0004; // Kernel panic on temperature trip

   public static final NativeLong WDIOC_GETSUPPORT = new NativeLong(2150127360L);
   public static final NativeLong WDIOC_GETSTATUS = new NativeLong(2147768065L);
   public static final NativeLong WDIOC_GETBOOTSTATUS = new NativeLong(2147768066L);
   public static final NativeLong WDIOC_GETTEMP = new NativeLong(2147768067L);
   public static final NativeLong WDIOC_SETOPTIONS = new NativeLong(2147768068L);
   public static final NativeLong WDIOC_KEEPALIVE = new NativeLong(2147768069L);
   public static final NativeLong WDIOC_SETTIMEOUT = new NativeLong(3221509894L);
   public static final NativeLong WDIOC_GETTIMEOUT = new NativeLong(2147768071L);
   public static final NativeLong WDIOC_SETPRETIMEOUT = new NativeLong(3221509896L);
   public static final NativeLong WDIOC_GETPRETIMEOUT = new NativeLong(2147768073L);
   public static final NativeLong WDIOC_GETTIMELEFT = new NativeLong(2147768074L);

   @Override
   public double getWatchdogTemp(int fd) {
      IntByReference ibr = new IntByReference();
      ioctl(fd, WDIOC_GETTEMP, ibr);
      return (double)ibr.getValue();
   }

   @Override
   public int getWatchdogTimeout(int fd) {
      IntByReference ibr = new IntByReference();
      ioctl(fd, WDIOC_GETTIMEOUT, ibr);
      return ibr.getValue();
   }

   @Override
   public void setWatchdogTimeout(int fd, int timeout) {
      IntByReference ibr = new IntByReference(timeout);
      ioctl(fd, WDIOC_SETTIMEOUT, ibr);
   }

   @Override
   public void setWatchdogEnable(int fd) {
      IntByReference ibr = new IntByReference(WDIOS_ENABLECARD);
      ioctl(fd, WDIOC_SETOPTIONS, ibr);
   }

   @Override
   public Object getWatchdogInfo(int fd) {
      Object info = new LINCLibrary.WatchdogInfo();
      ioctl(fd, WDIOC_GETSUPPORT, info);
      return info;
   }

   @Override
   public Set<WatchdogNative.Flag> getWatchdogInfoFlags(Object watchdogInfo) {
      int options = (int)getWatchdogInfoOptions(watchdogInfo);

      Set<WatchdogNative.Flag> results = EnumSet.noneOf(WatchdogNative.Flag.class);
      if ((options & WDIOF_OVERHEAT) != 0) results.add(WatchdogNative.Flag.OVERHEAT);
      if ((options & WDIOF_FANFAULT) != 0) results.add(WatchdogNative.Flag.FANFAULT);
      if ((options & WDIOF_EXTERN1) != 0) results.add(WatchdogNative.Flag.EXTERN1);
      if ((options & WDIOF_EXTERN2) != 0) results.add(WatchdogNative.Flag.EXTERN2);
      if ((options & WDIOF_POWERUNDER) != 0) results.add(WatchdogNative.Flag.POWERUNDER);
      if ((options & WDIOF_CARDRESET) != 0) results.add(WatchdogNative.Flag.CARDRESET);
      if ((options & WDIOF_POWEROVER) != 0) results.add(WatchdogNative.Flag.POWEROVER);
      if ((options & WDIOF_SETTIMEOUT) != 0) results.add(WatchdogNative.Flag.SETTIMEOUT);
      if ((options & WDIOF_MAGICCLOSE) != 0) results.add(WatchdogNative.Flag.MAGICCLOSE);
      if ((options & WDIOF_PRETIMEOUT) != 0) results.add(WatchdogNative.Flag.PRETIMEOUT);
      if ((options & WDIOF_ALARMONLY) != 0) results.add(WatchdogNative.Flag.ALARMONLY);
      if ((options & WDIOF_KEEPALIVEPING) != 0) results.add(WatchdogNative.Flag.KEEPALIVEPING);

      return results;
   }

   @Override
   public long getWatchdogInfoOptions(Object watchdogInfo) {
      return ((LINCLibrary.WatchdogInfo)watchdogInfo).options & 0xFFFFFFFFL;
   }

   @Override
   public long getWatchdogInfoFirmwareVersion(Object watchdogInfo) {
      return ((LINCLibrary.WatchdogInfo)watchdogInfo).firmware_version & 0xFFFFFFFFL;
   }

   @Override
   public String getWatchdogInfoIdentity(Object watchdogInfo) {
      byte[] id = ((LINCLibrary.WatchdogInfo)watchdogInfo).identity;

      int length = 0;
      while (length < id.length && id[length] != 0) length++;

      return new String(id, 0, length, StandardCharsets.US_ASCII);
   }

   private int ioctl(int fd, NativeLong request, Object... args) {
      return LINCLibrary.INSTANCE.ioctl(fd, request, args);
   }

   public interface LINCLibrary extends Library {
      LINCLibrary INSTANCE = (LINCLibrary)Native.loadLibrary("c", LINCLibrary.class);

      public int fcntl(int fd, int cmd, Object... args);
      public int ioctl(int fd, NativeLong reqeust, Object... args);

      public static class WatchdogInfo extends Structure {
         public static final int IDENTITY_SIZE = 32;
         public int options;
         public int firmware_version;
         public byte[] identity;

         public WatchdogInfo() {
            this.identity = new byte[IDENTITY_SIZE];
         }

         @Override
         protected List getFieldOrder() {
            return Arrays.asList("options", "firmware_version", "identity");
         }

         @Override
         public String toString() {
            return "WatchdogInfo [" +
                "options=" + Long.toHexString((long)options & 0xFFFFFFFF) +
               ",firmware_version=" + Long.toHexString((long)firmware_version & 0xFFFFFFFF) +
               ",identity=" + Arrays.toString(identity) +
               "]";
         }
      }
   }
}

