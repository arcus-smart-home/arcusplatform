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
package com.iris.util;

import com.codahale.metrics.Counter;
import com.iris.Utils;
import com.iris.detector.DetectorResult;
import com.iris.detector.ProfanityDetector;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;

public final class TokenUtil {

   private static final IrisMetricSet metrics = IrisMetrics.metrics("token.generation");
   private static final Counter total = metrics.counter("generated");
   private static final Counter profanity = metrics.counter("profanity.detected");

   private TokenUtil() {
   }

   public static String randomTokenString(int length) {
      total.inc();
      String token = Utils.randomTokenString(length);
      DetectorResult result = ProfanityDetector.INSTANCE.detect(token);
      while(result.isFound()) {
         profanity.inc();
         token = Utils.randomTokenString(length);
         result = ProfanityDetector.INSTANCE.detect(token);
      }
      return token;
   }
}

