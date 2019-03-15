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
package com.iris.alexa.message.v2.error;

public class RateLimitExceededError implements ErrorPayload {

   private String rateLimit;
   private String timeUnit;

   public String getRateLimit() {
      return rateLimit;
   }

   public void setRateLimit(String rateLimit) {
      this.rateLimit = rateLimit;
   }

   public String getTimeUnit() {
      return timeUnit;
   }

   public void setTimeUnit(String timeUnit) {
      this.timeUnit = timeUnit;
   }

   @Override
   public String toString() {
      return "RateLimitExceededError [rateLimit=" + rateLimit + ", timeUnit="
            + timeUnit + ']';
   }

}

