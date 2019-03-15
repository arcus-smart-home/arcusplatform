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
package com.iris.alexa.message;

import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;

public final class Endpoint {

   private @Nullable final Scope scope;
   private final String endpointId;
   private @Nullable final Map<String, String> cookie;

   public Endpoint(@Nullable Scope scope, String endpointId, @Nullable Map<String, String> cookie) {
      this.scope = scope;
      this.endpointId = endpointId;
      this.cookie = cookie;
   }

   @Nullable
   public Scope getScope() {
      return scope;
   }

   public String getEndpointId() {
      return endpointId;
   }

   @Nullable
   public Map<String, String> getCookie() {
      return cookie;
   }

   @Override
   public String toString() {
      return "Endpoint{" +
         "scope=" + scope +
         ", endpointId='" + endpointId + '\'' +
         ", cookie=" + cookie +
         '}';
   }
}

