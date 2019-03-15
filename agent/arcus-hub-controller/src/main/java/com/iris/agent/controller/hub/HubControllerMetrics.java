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
package com.iris.agent.controller.hub;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.RatioGauge;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;

public final class HubControllerMetrics {
   private static final IrisMetricSet MS = IrisMetrics.metrics("hub.controller");
   public static final Histogram PING = MS.histogram("ping");

   public static final Meter PING_METER = MS.meter("ping-meter");
   public static final Meter PONG_METER = MS.meter("pong-meter");

   public static final RatioGauge PING_GAUGE = MS.ratio15(PING_METER, PONG_METER);

   private HubControllerMetrics() {
   }
}

