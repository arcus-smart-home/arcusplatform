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
package com.iris.util;

import java.util.Arrays;

/**
 * Created by wesleystueve on 6/28/17.
 * Adapted from https://stackoverflow.com/questions/1155107/is-there-a-cross-platform-java-method-to-remove-filename-special-chars
 * and https://stackoverflow.com/questions/893977/java-how-to-find-out-whether-a-file-name-is-valid
 */
public class IrisFileName {
   //valid names and characters taken from https://msdn.microsoft.com/en-us/library/windows/desktop/aa365247(v=vs.85).aspx
   private static final int[] INVALID_CHARS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 34, 42, 47, 58, 60, 62, 63, 92, 124};
   private static final String[] INVALID_RESOURCE_BASENAMES = new String[]{"aux", "com1", "com2", "com3", "com4",
         "com5", "com6", "com7", "com8", "com9", "con", "lpt1", "lpt2",
         "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9", "nul", "prn"};
   private static final String[] INVALID_RESOURCE_FULLNAMES = new String[]{"clock$"};
   public static final int MAX_FILENAME_LENGTH = 255;

   static {
      Arrays.sort(INVALID_CHARS);
      Arrays.sort(INVALID_RESOURCE_BASENAMES);
      Arrays.sort(INVALID_RESOURCE_FULLNAMES);
   }

   public static Boolean containsInvalidCharacter(String fileName) {
      int len = fileName.codePointCount(0, fileName.length());
      for (int i = 0; i < len; i++) {
         int c = fileName.codePointAt(i);
         if (Arrays.binarySearch(INVALID_CHARS, c) >= 0) {
            return true;
         }
      }
      return false;
   }

   public static String clean(String fileName, String invalidCharReplacement) {
      if (fileName == null) return null;

      fileName = fileName.trim();

      StringBuilder cleanName = new StringBuilder();
      int len = fileName.codePointCount(0, fileName.length());
      for (int i = 0; i < len; i++) {
         int c = fileName.codePointAt(i);
         if (Arrays.binarySearch(INVALID_CHARS, c) < 0) {
            cleanName.appendCodePoint(c);
         } else {
            cleanName.append(invalidCharReplacement);
         }
      }
      return cleanName.toString();
   }


   /**
    * Returns true if the given name is a valid resource name on supported operating systems
    * and false otherwise.
    */
   public static boolean isValid(String fileName) {
      if (fileName == null) return false;

      //. and .. have special meaning on all platforms
      if (fileName.equals(".") || fileName.equals("..")) return false;

      //empty names are not valid
      final int length = fileName.length();
      if (length == 0 || length > MAX_FILENAME_LENGTH) return false;

      final char lastChar = fileName.charAt(length - 1);
      // filenames ending in dot are not valid
      if (lastChar == '.') return false;

      //file names ending with whitespace are truncated
      if (Character.isWhitespace(lastChar)) return false;

      //if the file name contains an invalid character
      if (containsInvalidCharacter(fileName)) return false;

      int dot = fileName.indexOf('.');
      //on windows, filename suffixes are not relevant to name validity
      String basename = dot == -1 ? fileName : fileName.substring(0, dot);
      if (Arrays.binarySearch(INVALID_RESOURCE_BASENAMES, basename.toLowerCase()) >= 0) {
         return false;
      }

      return Arrays.binarySearch(INVALID_RESOURCE_FULLNAMES, fileName.toLowerCase()) < 0;
   }
}


