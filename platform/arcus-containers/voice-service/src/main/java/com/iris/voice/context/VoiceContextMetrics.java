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
package com.iris.voice.context;

import com.codahale.metrics.Timer;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;

enum VoiceContextMetrics {
   ;

   private static final IrisMetricSet METRICS = IrisMetrics.metrics("voice.service");
   private static final Timer partitionLoadTimer = METRICS.timer("partitionload.time");
   private static final Timer contextLoadTimer = METRICS.timer("contextload.time");

   static Timer.Context startPartitionLoadTime() {
      return partitionLoadTimer.time();
   }

   static Timer.Context startContextLoadTime() {
      return contextLoadTimer.time();
   }
}

