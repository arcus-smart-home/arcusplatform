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
package com.iris.bridge.server.http.impl.matcher;

import io.netty.handler.codec.http.HttpMethod;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WildcardMatcher extends RegexMatcher {
   private final static Pattern regex = Pattern.compile("[^*]+|(\\*)");
   
   private static String makeRegex(String wildcard) {
      Matcher m = regex.matcher(wildcard);
      StringBuffer b= new StringBuffer();
      while (m.find()) {
          if(m.group(1) != null) m.appendReplacement(b, ".*");
          else m.appendReplacement(b, "\\\\Q" + m.group(0) + "\\\\E");
      }
      m.appendTail(b);
      return b.toString();
   }
   
   public WildcardMatcher(String wildcard) {
      this(wildcard, HttpMethod.GET);
   }
   
   public WildcardMatcher(String wildcard, HttpMethod method) {
      super(makeRegex(wildcard), method);
   }
}

