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
package com.iris.modelmanager.version;

public class Version implements Comparable<Version> {

   private int major;
   private int minor;
   private int patch;

   public static Version valueOf(String version) {
      String parts[] = version.split("\\.");
      return new Version(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
   }

   private Version(int major, int minor, int patch) {
      this.major = major;
      this.minor = minor;
      this.patch = patch;
   }

   public int getMajor() {
      return major;
   }

   public int getMinor() {
      return minor;
   }

   public int getPatch() {
      return patch;
   }

   @Override
   public int compareTo(Version o) {
      int value = this.major - o.major;
      if(value != 0) {
         return value;
      }
      value = this.minor - o.minor;

      if(value != 0) {
         return value;
      }

      return this.patch - o.patch;
   }
}

