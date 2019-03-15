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
package com.iris.common.subsystem.lawnngarden;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class LawnNGardenConfig {

   @Inject(optional = true)
   @Named("lawnngarden.retry.count")
   private int retryCount = 10;

   @Inject(optional = true)
   @Named("lawnngarden.retry.timeout.secs")
   private int retryTimeoutSeconds = 600;

   @Inject(optional = true)
   @Named("lawnngarden.timeout.secs")
   private int timeoutSeconds = 120;

   @Inject(optional = true)
   @Named("lawnngarden.default.max.transitions")
   private int defaultMaxTransitions = 4;

   public int retryCount() {
      return retryCount;
   }

   public void setRetryCount(int retryCount) {
      this.retryCount = retryCount;
   }

   public int retryTimeoutSeconds() {
      return retryTimeoutSeconds;
   }

   public void setRetryTimeoutSeconds(int retryTimeoutSeconds) {
      this.retryTimeoutSeconds = retryTimeoutSeconds;
   }

   public int timeoutSeconds() {
      return timeoutSeconds;
   }

   public void setTimeoutSeconds(int timeoutSeconds) {
      this.timeoutSeconds = timeoutSeconds;
   }

   public int defaultMaxTransitions() {
      return defaultMaxTransitions;
   }

   public void setDefaultMaxTransitions(int defaultMaxTransitions) {
      this.defaultMaxTransitions = defaultMaxTransitions;
   }
}

