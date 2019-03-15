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
package com.iris.network;

import com.google.common.collect.ImmutableList;

public class TestUnlimited extends RateLimiterTestCase<UnlimitedRateLimiter> {
   @Override
   protected Iterable<UnlimitedRateLimiter> createRateLimiters() {
      return ImmutableList.of(
         RateLimiters.unlimited().build()
      );
   }

   @Override
   protected int getMinimumPermits() {
      return Integer.MAX_VALUE;
   }

   @Override
   protected int getCurrentPermits(UnlimitedRateLimiter rl) {
      return Integer.MAX_VALUE;
   }

   @Override
   protected int getCapacity(UnlimitedRateLimiter rl) {
      return Integer.MAX_VALUE;
   }

   @Override
   protected int getRate(UnlimitedRateLimiter rl) {
      return Integer.MAX_VALUE;
   }
}

