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
package com.iris.media;

import java.util.Map;

public final class RtspPushHeaders {
   private final Map<String,String> headers;

   public RtspPushHeaders(Map<String,String> headers) {
      this.headers = headers;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Accessor methods
   /////////////////////////////////////////////////////////////////////////////

   public Map<String,String> getHeaders() {
      return headers;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Printing support
   /////////////////////////////////////////////////////////////////////////////

   @Override
   public String toString() {
      return "rtsp push headers [" +
         "hdrs=" + getHeaders() +
         "]";
   }
}

