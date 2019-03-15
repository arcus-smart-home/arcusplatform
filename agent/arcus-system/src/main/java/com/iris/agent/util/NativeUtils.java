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
package com.iris.agent.util;

public final class NativeUtils {
   private enum Os { UNKNOWN, LINUX, MAC, WINDOWS }
   private enum Arch { UNKNOWN, X86, X86_64, ARM }

   private static Os os;
   private static Arch arch;

   static {
      String osName = System.getProperty("os.name").toLowerCase();
      String osArch = System.getProperty("os.arch").toLowerCase();

      Os detectedOs = Os.UNKNOWN;
      if (osName.contains("lin")) {
         detectedOs = Os.LINUX;
      } else if (osName.contains("mac")) {
         detectedOs = Os.MAC;
      } else if (osName.contains("win")) {
         detectedOs = Os.WINDOWS;
      }

      Arch detectedArch = Arch.UNKNOWN;
      if (osArch.contains("x86_64") || osArch.contains("x86-64") || osArch.contains("amd64")) {
         detectedArch = Arch.X86_64;
      } else if (osArch.contains("x86") || osArch.contains("i386") || osArch.contains("i486") || osArch.contains("i586") || osArch.contains("i686")) {
         detectedArch = Arch.X86;
      } else if (osArch.contains("arm")) {
         detectedArch = Arch.ARM;
      }

      os = detectedOs;
      arch = detectedArch;
   }

   private NativeUtils() {
   }

   public static Os getOperatingSystem() {
      return os;
   }

   public static Arch getArchitecture() {
      return arch;
   }

   public static String getNativeLibraryPrefix() {
      switch (os) {
      case LINUX:
      case MAC:
         return "lib";

      case WINDOWS:
         return "";

      default:
         throw new IllegalStateException("unknown operation system");
      }
   }

   public static String getNativeLibrarySuffix() {
      switch (os) {
      case LINUX:
         return ".so";

      case MAC:
         return ".dylib";

      case WINDOWS:
         return ".dll";

      default:
         throw new IllegalStateException("unknown operation system");
      }
   }

   public static boolean isWindows() {
      return os == Os.WINDOWS;
   }

   public static boolean isLinux() {
      return os == Os.LINUX;
   }

   public static boolean isMac() {
      return os == Os.MAC;
   }

   public static boolean isX86() {
      return arch != Arch.ARM && arch != Arch.UNKNOWN;
   }

   public static boolean isArm() {
      return arch == Arch.ARM;
   }

   public static boolean is64bit() {
      return arch == Arch.X86_64;
   }
}

