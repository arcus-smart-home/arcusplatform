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
package com.iris.protocol.ipcd.definition.reader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.iris.protocol.ipcd.definition.context.DefinitionContext;

public class TypeParser {
   private static final Pattern genericPattern = Pattern.compile("^([\\w|\\.|\\,]+)<(.*)>$");
   private static final Pattern simplePattern = Pattern.compile("[\\w|\\.|\\,]+");
   
   private static final Map<String, String> standardTypes;
   static {
      Map<String, String> map = new HashMap<>();
      map.put("string", "String");
      map.put("number", "Double");
      map.put("boolean", "Boolean");
      map.put("list", "List");
      map.put("map", "Map");
      map.put("object", "Object");
      standardTypes = Collections.unmodifiableMap(map);
   }
   
   public static String parse(String input) {
      String s = input.trim();
      if (DefinitionContext.VIRTUAL_TYPE.equals(s)) {
         return s;
      }
      Matcher genericMatcher = genericPattern.matcher(s);
      if (genericMatcher.matches()) {
         String type = findType(genericMatcher.group(1));
         if (StringUtils.isEmpty(type)) {
            throw new IllegalArgumentException("Could not turn " + s + " into a type.");
         }
         return type + "<" + parse(genericMatcher.group(2)) + ">";
      }
      else {
         Matcher simpleMatcher = simplePattern.matcher(s);
         if (simpleMatcher.matches()) {
            return findType(s);
         }
         throw new IllegalArgumentException("Could not turn " + s + " into a type.");
      }
   }
   
   private static String findType(String s) {
      if (s.contains(",")) {
         List<String> tokens = tokenize(s);
         StringBuilder sb = new StringBuilder();
         for (String token : tokens) {
            if (sb.length() > 0) {
               sb.append(',');
            }
            sb.append(parse(token));
         }
         return sb.toString();
      }
      String type = standardTypes.get(s.toLowerCase());
      return type != null ? type : s;
   }
   
   private static List<String> tokenize(String s) {
      List<String> tokens = new ArrayList<>();
      StringBuilder sb = new StringBuilder();
      int genericDepth = 0;
      for (char c : s.toCharArray()) {
         switch(c) {
         case '<':
            genericDepth++;
            sb.append(c);
            break;
         case '>':
            genericDepth--;
            sb.append(c);
            break;
         case ',':
            if (genericDepth > 0) {
               sb.append(c);
            }
            else {
               tokens.add(sb.toString());
               sb = new StringBuilder();
            }
            break;
         default:
            sb.append(c);
            break;
         }
      }
      if (sb.length() > 0) {
         tokens.add(sb.toString());
      }
      return tokens;
   }
}

