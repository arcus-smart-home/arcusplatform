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
package com.iris.bridge.server.traffic;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Gauge;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.metrics.IrisMetrics;

import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import io.netty.handler.traffic.TrafficCounter;
import io.netty.channel.ChannelHandler.Sharable;

@Sharable
public class DefaultTrafficHandler extends GlobalTrafficShapingHandler implements TrafficHandler {
   public static final long CHECK_INTERVAL = TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS);
   public static final long MAX_TIME = TimeUnit.MILLISECONDS.convert(300, TimeUnit.SECONDS);
   public static final long WRITE_GLOBAL_LIMIT = 0; // no limit
   public static final long READ_GLOBAL_LIMIT = 0; // no limit

   public DefaultTrafficHandler(BridgeMetrics metrics) {
      super(Executors.newScheduledThreadPool(1), WRITE_GLOBAL_LIMIT, READ_GLOBAL_LIMIT, CHECK_INTERVAL, MAX_TIME);

      IrisMetrics.metrics("bridge." + metrics.getBridgeName() + ".bytes").gauge("sent", (Gauge<Long>)() -> {
         TrafficCounter tc = trafficCounter();
         return tc != null ? tc.cumulativeWrittenBytes() : 0L;
      });

      IrisMetrics.metrics("bridge." + metrics.getBridgeName() + ".bytes").gauge("received", (Gauge<Long>)() -> {
         TrafficCounter tc = trafficCounter();
         return tc != null ? tc.cumulativeReadBytes() : 0L;
      });
   }
}

