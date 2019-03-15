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
package com.iris.metrics;

import java.util.concurrent.TimeUnit;

public final class IrisHubMetrics {
   public static final long HUB_METRIC_FAST_PERIOD_NS = TimeUnit.MINUTES.toNanos(15);
   public static final long HUB_METRIC_MEDIUM_PERIOD_NS = TimeUnit.HOURS.toNanos(1);
   public static final long HUB_METRIC_SLOW_PERIOD_NS = TimeUnit.DAYS.toNanos(1);
}

