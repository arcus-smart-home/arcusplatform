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
package com.iris.bridge.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;

public class BridgeMetrics {
   private static final IrisMetricSet METRICS = IrisMetrics.metrics(BridgeMetrics.class);

   private final String bridgeName;

   private final Counter bridgePlatformMsgSentCounter;
   private final Counter bridgeProtocolMsgSentCounter;
   private final Counter bridgePlatformMsgReceivedCounter;
   private final Counter bridgeProtocolMsgReceivedCounter;
   private final Counter bridgePlatformMsgDiscardedCounter;
   private final Counter bridgePlatformDeviceNotFound;
   private final Counter bridgeProtocolMsgDiscardedCounter;
   private final Counter bridgeFramesSentCounter;
   private final Counter bridgeFramesReceivedCounter;
   private final Counter bridgeSocketCounter;
   private final Counter bridgeHttpReceivedCounter;
   private final Counter bridgeWebSocketReceivedCounter;
   private final Counter bridgeUnknownFrameReceivedCounter;
   private final Counter bridgeBadHttpRequestCounter;
   private final Counter bridgeForbiddenHttpRequestCounter;
   private final Counter bridgeNotFoundHttpRequestCounter;
   private final Counter bridgeNotModifiedHttpRequestCounter;
   private final Counter bridgeErrorHttpRequestCounter;
   private final Counter bridgeWsUpgradeCounter;
   private final Counter bridgeSessionCreatedCounter;
   private final Counter bridgeSessionDestroyedCounter;
   private final Counter bridgeAuthenticationTriedCounter;
   private final Counter bridgeAuthenticationSucceededCounter;
   private final Counter bridgeAuthenticationFailedCounter;
   private final Counter bridgeAuthorizationTriedCounter;
   private final Counter bridgeAuthorizationSucceededCounter;
   private final Counter bridgeAuthorizationFailedCounter;

   private final Timer   bridgeProcessHttpRequestTimer;
   private final Timer   bridgeProcessWsRequestTimer;
   private final Timer   bridgeAuthenticationTimer;
   private final Timer   bridgeAuthorizationTimer;

   private final String bridgeHTTPResponseCounterPrefix;

   public BridgeMetrics(String bridgeName) {
      this.bridgeName = bridgeName;

      bridgePlatformMsgSentCounter         = METRICS.counter("bridge." + bridgeName + ".platform.sent");
      bridgeProtocolMsgSentCounter         = METRICS.counter("bridge." + bridgeName + ".protocol.sent");
      bridgePlatformMsgReceivedCounter     = METRICS.counter("bridge." + bridgeName + ".platform.received");
      bridgeProtocolMsgReceivedCounter     = METRICS.counter("bridge." + bridgeName + ".protocol.received");
      bridgePlatformMsgDiscardedCounter    = METRICS.counter("bridge." + bridgeName + ".platform.discarded");
      bridgePlatformDeviceNotFound         = METRICS.counter("bridge." + bridgeName + ".platform.devicenotfound");
      bridgeProtocolMsgDiscardedCounter    = METRICS.counter("bridge." + bridgeName + ".protocol.discarded");
      bridgeFramesSentCounter              = METRICS.counter("bridge." + bridgeName + ".frames.sent");
      bridgeFramesReceivedCounter          = METRICS.counter("bridge." + bridgeName + ".frames.received");
      bridgeSocketCounter                  = METRICS.counter("bridge." + bridgeName + ".sockets");
      bridgeHttpReceivedCounter            = METRICS.counter("bridge." + bridgeName + ".http.received");
      bridgeWebSocketReceivedCounter       = METRICS.counter("bridge." + bridgeName + ".ws.received");
      bridgeUnknownFrameReceivedCounter    = METRICS.counter("bridge." + bridgeName + ".unknown.received");
      bridgeBadHttpRequestCounter          = METRICS.counter("bridge." + bridgeName + ".http.bad.request.sent");
      bridgeForbiddenHttpRequestCounter    = METRICS.counter("bridge." + bridgeName + ".http.forbidden.sent");
      bridgeNotFoundHttpRequestCounter     = METRICS.counter("bridge." + bridgeName + ".http.notfound.sent");
      bridgeNotModifiedHttpRequestCounter  = METRICS.counter("bridge." + bridgeName + ".http.notmodified.sent");
      bridgeErrorHttpRequestCounter        = METRICS.counter("bridge." + bridgeName + ".http.error.sent");
      bridgeWsUpgradeCounter               = METRICS.counter("bridge." + bridgeName + ".ws.upgrade.received");
      bridgeSessionCreatedCounter          = METRICS.counter("bridge." + bridgeName + ".session.created");
      bridgeSessionDestroyedCounter        = METRICS.counter("bridge." + bridgeName + ".session.destroyed");
      bridgeAuthenticationTriedCounter     = METRICS.counter("bridge." + bridgeName + ".authentication.tried");
      bridgeAuthenticationSucceededCounter = METRICS.counter("bridge." + bridgeName + ".authentication.succeeded");
      bridgeAuthenticationFailedCounter    = METRICS.counter("bridge." + bridgeName + ".authentication.failed");
      bridgeAuthorizationTriedCounter      = METRICS.counter("bridge." + bridgeName + ".authorization.tried");
      bridgeAuthorizationSucceededCounter  = METRICS.counter("bridge." + bridgeName + ".authorization.succeeded");
      bridgeAuthorizationFailedCounter     = METRICS.counter("bridge." + bridgeName + ".authorization.failed");

      bridgeProcessHttpRequestTimer        = METRICS.timer("bridge." + bridgeName + ".process.http.request");
      bridgeProcessWsRequestTimer          = METRICS.timer("bridge." + bridgeName + ".process.ws.request");
      bridgeAuthenticationTimer            = METRICS.timer("bridge." + bridgeName + ".authentication");
      bridgeAuthorizationTimer             = METRICS.timer("bridge." + bridgeName + ".authorization");
      bridgeHTTPResponseCounterPrefix      = "bridge." + bridgeName + ".http.request.";
   }

   public void incPlatformMsgSentCounter() {
      bridgePlatformMsgSentCounter.inc();
   }

   public void incProtocolMsgSentCounter() {
      bridgeProtocolMsgSentCounter.inc();
   }

   public void incPlatformMsgReceivedCounter() {
      bridgePlatformMsgReceivedCounter.inc();
   }

   public void incProtocolMsgReceivedCounter() {
      bridgeProtocolMsgReceivedCounter.inc();
   }

   public void incPlatformDeviceNotFoundCounter() {
      bridgePlatformDeviceNotFound.inc();
   }

   public void incPlatformMsgDiscardedCounter() {
      bridgePlatformMsgDiscardedCounter.inc();
   }

   public void incProtocolMsgDiscardedCounter() {
      bridgeProtocolMsgDiscardedCounter.inc();
   }

   public void incFramesSentCounter() {
      bridgeFramesSentCounter.inc();
   }

   public void incFramesReceivedCounter() {
      bridgeFramesReceivedCounter.inc();
   }

   public void incHTTPResponseCounter(String handlerClass, int responseCode) {
      METRICS.counter(bridgeHTTPResponseCounterPrefix + handlerClass.toLowerCase() + "." + responseCode).inc();
   }

   public void incSocketCounter() { bridgeSocketCounter.inc(); }

   public void incHttpReceivedCounter() { bridgeHttpReceivedCounter.inc(); }

   public void incWebSocketReceivedCounter() { bridgeWebSocketReceivedCounter.inc(); }

   public void incUnknownFrameReceivedCounter() { bridgeUnknownFrameReceivedCounter.inc(); }

   public void incBadHttpRequestCounter() { bridgeBadHttpRequestCounter.inc(); }

   public void incForbiddenHttpRequestCounter() { bridgeForbiddenHttpRequestCounter.inc(); }

   public void incNotFoundHttpRequestCounter() { bridgeNotFoundHttpRequestCounter.inc(); }

   public void incNotModifiedHttpRequestCounter() { bridgeNotModifiedHttpRequestCounter.inc(); }

   public void incErrorHttpRequestCounter() { bridgeErrorHttpRequestCounter.inc(); }

   public void incWsUpgradeCounter() { bridgeWsUpgradeCounter.inc(); }

   public void incSessionCreatedCounter() { bridgeSessionCreatedCounter.inc(); }

   public void incSessionDestroyedCounter() { bridgeSessionDestroyedCounter.inc(); }

   public void incAuthenticationTriedCounter() { bridgeAuthenticationTriedCounter.inc(); }

   public void incAuthenticationSucceededCounter() { bridgeAuthenticationSucceededCounter.inc(); }

   public void incAuthenticationFailedCounter() { bridgeAuthenticationFailedCounter.inc(); }

   public void incAuthorizationTriedCounter() { bridgeAuthorizationTriedCounter.inc(); }

   public void incAuthorizationSucceededCounter() { bridgeAuthorizationSucceededCounter.inc(); }

   public void incAuthorizationFailedCounter() { bridgeAuthorizationFailedCounter.inc(); }

   public Timer.Context startProcessHttpRequestTimer() { return bridgeProcessHttpRequestTimer.time(); }

   public Timer.Context startProcessWsRequestTimer() { return bridgeProcessWsRequestTimer.time(); }

   public Timer.Context startAuthenticationTimer() { return bridgeAuthenticationTimer.time(); }

   public Timer.Context startAuthorizationTimer() {
      return bridgeAuthorizationTimer.time();
   }

   public String getBridgeName() {
      return bridgeName;
   }
}



