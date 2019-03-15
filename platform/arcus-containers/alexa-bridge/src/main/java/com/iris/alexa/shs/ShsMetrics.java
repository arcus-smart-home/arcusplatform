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
package com.iris.alexa.shs;

import com.codahale.metrics.Counter;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;

public class ShsMetrics {

   private ShsMetrics() {
   }


   private static final IrisMetricSet METRICS = IrisMetrics.metrics("alexa.shs");

   private static final Counter shsRequest = METRICS.counter("request");
   private static final Counter invalidDirective = METRICS.counter("invalid.directive");
   private static final Counter nonDirective = METRICS.counter("non.directive");
   private static final Counter uncaughtException = METRICS.counter("uncaught.exception");
   private static final Counter healthCheck = METRICS.counter("healthcheck");
   private static final Counter expiredToken = METRICS.counter("token.expired");
   private static final Counter noPlace = METRICS.counter("no.place");

   public static void incShsRequest () { shsRequest.inc(); }
   public static void incInvalidDirective() { invalidDirective.inc(); }
   public static void incNonDirective() { nonDirective.inc(); }
   public static void incUncaughtException() { uncaughtException.inc(); }
   public static void incHealthCheck() { healthCheck.inc(); }
   public static void incExpiredToken() { expiredToken.inc(); }
   public static void incNoPlace() { noPlace.inc(); }
}

