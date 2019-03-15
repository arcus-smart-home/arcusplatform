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
package com.iris.bridge.server.netty;

import javax.net.ssl.SSLEngine;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;

public class SslMetrics {
   private static final SslMetrics INSTANCE = new SslMetrics();
   
   public static SslMetrics instance() {
      return INSTANCE;
   }

   public static SSLEngine instrument(SSLEngine engine) {
      return new InstrumentedSSLEngine(engine, instance());
   }
   
   private final Counter accepted;
   private final Counter refused;
   private final Histogram handshakeSuccesses;
   private final Histogram handshakeFailures;
   private final Counter handshakeTime;
   private final Histogram encodeRequests;
   private final Counter encodeTime;
   private final Histogram decodeRequests;
   private final Counter decodeTime;
   
   SslMetrics() {
      IrisMetricSet metrics = IrisMetrics.metrics("bridge.ssl");
      this.accepted = metrics.counter("connect.accepted");
      this.refused = metrics.counter("connect.refused");
      this.handshakeSuccesses = metrics.histogram("handshake.successes");
      this.handshakeFailures = metrics.histogram("handshake.failures");
      this.handshakeTime = metrics.counter("handshake.time");
      this.encodeRequests = metrics.histogram("encode.requests");
      this.encodeTime = metrics.counter("encode.time");
      this.decodeRequests = metrics.histogram("decode.requests");
      this.decodeTime = metrics.counter("decode.time");
   }
   
   long startTime() {
      return System.nanoTime();
   }
   
   void onAccepted() {
      accepted.inc();
   }
   
   void onRefused() {
      refused.inc();
   }
   
   void onHandshakeSuccess(long startTimeNs) {
      long delta = System.nanoTime() - startTimeNs;
      handshakeSuccesses.update(delta / 1000);
      handshakeTime.inc(delta);
   }

   void onHandshakeFailure(long startTimeNs) {
      long delta = System.nanoTime() - startTimeNs;
      handshakeFailures.update(delta / 1000);
      handshakeTime.inc(delta);
   }

   void onEncodeComplete(long startTimeNs) {
      long delta = System.nanoTime() - startTimeNs;
      encodeRequests.update(delta / 1000);
      encodeTime.inc(delta);
   }

   void onDecodeComplete(long startTimeNs) {
      long delta = System.nanoTime() - startTimeNs;
      decodeRequests.update(delta / 1000);
      decodeTime.inc(delta);
   }

}

