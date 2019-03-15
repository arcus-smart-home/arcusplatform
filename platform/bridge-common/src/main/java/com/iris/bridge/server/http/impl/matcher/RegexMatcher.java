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

import java.util.Objects;
import java.util.regex.Pattern;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;

import com.google.common.base.Preconditions;
import com.iris.bridge.server.http.RequestMatcher;

public class RegexMatcher implements RequestMatcher {
   private final Pattern regex;
   private final HttpMethod method;
   
   public RegexMatcher(String regex, HttpMethod method) {
      Preconditions.checkNotNull(regex, "Must specify a regex");
      this.regex = Pattern.compile(regex);
      this.method = method;
   }

   @Override
   public boolean matches(HttpRequest req) {
      if(!Objects.equals(req.getMethod(), method)) {
         return false;
      }
      
      // this isn't the URI its the path...
      String uri = req.getUri();
      boolean matches = regex.matcher(uri).matches();
      if (matches) {
         return true;
      }
      // Remove double slashes and try again. Sometimes double slashes are sent.
      uri = uri.replace("//", "/");
      return regex.matcher(uri).matches();
   }

   @Override
   public String toString() {
      return "RegexMatcher [regex=" + regex + ", method=" + method + "]";
   }
}

