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

public final class Scope {

   public static Scope fromMap(Map<Object, Object> scope) {
      return new Scope(
         (String) scope.get("type"),
         (String) scope.get("token")
      );
   }

   private @Nullable final String type;
   private @Nullable final String token;

   public Scope(@Nullable String type, @Nullable String token) {
      this.type = type;
      this.token = token;
   }

   public @Nullable String getType() {
      return type;
   }

   public @Nullable
   String getToken() {
      return token;
   }

   @Override
   public String toString() {
      return "Scope{" +
         "type='" + type + '\'' +
         ", token='" + token + '\'' +
         '}';
   }
}


