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
package com.iris.security;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class SessionConfig {

   /*
    *  (seconds into the future) 1m  * 1h  * 1d  * 14d = 1209600 seconds
    */
   @Inject(optional = true) @Named("auth.timeout")
   private long defaultSessionTimeoutInSecs = 1209600L;

   /*
    * 30 minutes as default for public session length
    */
   @Inject(optional = true) @Named("public.auth.timeout")
   private long publicSessionTimeoutInSecs = 1800l;

   public SessionConfig() {
   }

   public long getDefaultSessionTimeoutInSecs() {
      return defaultSessionTimeoutInSecs;
   }
   
   public void setDefaultSessionTimeoutInSecs(long defaultSessionTimeoutInSecs) {
      this.defaultSessionTimeoutInSecs = defaultSessionTimeoutInSecs;
   }
   
   public long getPublicSessionTimeoutInSecs() {
      return publicSessionTimeoutInSecs;
   }
   
   public void setPublicSessionTimeoutInSecs(long publicSessionTimeoutInSecs) {
      this.publicSessionTimeoutInSecs = publicSessionTimeoutInSecs;
   }
   
}

