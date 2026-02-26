package com.iris.bootstrap.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parses configuration key strings, extracting variable references.
 *
 * Drop-in replacement for com.netflix.governator.configuration.KeyParser.
 */
public class KeyParser {

   public static List<String> parse(String key) {
      if (key == null || key.isEmpty()) {
         return Collections.singletonList("");
      }

      // If no variable references, return as single-element list
      if (!key.contains("${")) {
         return Collections.singletonList(key);
      }

      List<String> parts = new ArrayList<>();
      int pos = 0;
      while (pos < key.length()) {
         int varStart = key.indexOf("${", pos);
         if (varStart < 0) {
            parts.add(key.substring(pos));
            break;
         }
         if (varStart > pos) {
            parts.add(key.substring(pos, varStart));
         }
         int varEnd = key.indexOf("}", varStart);
         if (varEnd < 0) {
            parts.add(key.substring(varStart));
            break;
         }
         parts.add(key.substring(varStart, varEnd + 1));
         pos = varEnd + 1;
      }
      return parts;
   }
}
