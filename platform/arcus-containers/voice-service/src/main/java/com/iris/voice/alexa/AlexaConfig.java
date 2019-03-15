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
package com.iris.voice.alexa;

import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.voice.VoiceConfig;

@Singleton
public class AlexaConfig extends VoiceConfig {

   @Inject(optional = true)
   @Named("alexa.service.success.cheat.enabled")
   private boolean successCheatEnabled = true;

   @Inject(optional = true)
   @Named("alexa.service.colortemp.delta")
   private int colorTempDelta = 500;

   @Inject(optional = true)
   @Named("alexa.service.battery.threshold")
   private int batteryThreshold = 15;

   @Inject(optional = true)
   @Named("alexa.service.http.max.connections")
   private int maxConnections = 20;

   @Inject(optional = true)
   @Named("alexa.service.http.route.max.connections")
   private int routeMaxConnections = 10;

   @Inject(optional = true)
   @Named("alexa.service.http.inactivity.validate.ms")
   private int validateAfterInactivityMs = 2000;

   @Inject(optional = true)
   @Named("alexa.service.http.ttl.ms")
   private long timeToLiveMs = -1;

   @Inject(optional = true)
   @Named("alexa.service.http.connection.request.timeout.ms")
   private int connectionRequestTimeoutMs = 1000;

   @Inject(optional = true)
   @Named("alexa.service.http.connection.timeout.ms")
   private int connectionTimeoutMs = 5000;

   @Inject(optional = true)
   @Named("alexa.service.http.socket.timeout.ms")
   private int socketTimeoutMs = (int) TimeUnit.SECONDS.toMillis(30);

   @Inject(optional = true)
   @Named("alexa.service.oauth.endpoint")
   private String oauthEndpoint = "https://api.amazon.com/auth/o2/token";

   @Inject(optional = true)
   @Named("alexa.service.oauth.preempt.refresh.time.mins")
   private int preemptRefreshTimeMins = 5;

   @Inject
   @Named("alexa.service.oauth.client.id")
   private String oauthClientId;

   @Inject
   @Named("alexa.service.oauth.client.secret")
   private String oauthClientSecret;

   @Inject(optional = true)
   @Named("alexa.service.event.endpoint")
   private String eventEndpoint = "https://api.amazonalexa.com/v3/events";

   @Inject(optional = true)
   @Named("alexa.service.deferred.correlation.timeout.secs")
   private int correlationTimeoutSecs = 30;

   @Inject(optional = true)
   @Named("alexa.service.proactive.enabled")
   private boolean proactiveEnabled = false;

   @Inject(optional = true)
   @Named("alexa.service.lock.deferred.enabled")
   private boolean lockDeferredEnabled = true;

   @Inject(optional = true)
   @Named("alexa.service.report.state.offline.enabled")
   private boolean reportStateOffline = false;

   public boolean isSuccessCheatEnabled() {
      return successCheatEnabled;
   }

   public void setSuccessCheatEnabled(boolean successCheatEnabled) {
      this.successCheatEnabled = successCheatEnabled;
   }

   public int getColorTempDelta() {
      return colorTempDelta;
   }

   public void setColorTempDelta(int colorTempDelta) {
      this.colorTempDelta = colorTempDelta;
   }

   public int getBatteryThreshold() {
      return batteryThreshold;
   }

   public void setBatteryThreshold(int batteryThreshold) {
      this.batteryThreshold = batteryThreshold;
   }

   public int getMaxConnections() {
      return maxConnections;
   }

   public void setMaxConnections(int maxConnections) {
      this.maxConnections = maxConnections;
   }

   public int getRouteMaxConnections() {
      return routeMaxConnections;
   }

   public void setRouteMaxConnections(int routeMaxConnections) {
      this.routeMaxConnections = routeMaxConnections;
   }

   public int getValidateAfterInactivityMs() {
      return validateAfterInactivityMs;
   }

   public void setValidateAfterInactivityMs(int validateAfterInactivityMs) {
      this.validateAfterInactivityMs = validateAfterInactivityMs;
   }

   public long getTimeToLiveMs() {
      return timeToLiveMs;
   }

   public void setTimeToLiveMs(long timeToLiveMs) {
      this.timeToLiveMs = timeToLiveMs;
   }

   public int getConnectionRequestTimeoutMs() {
      return connectionRequestTimeoutMs;
   }

   public void setConnectionRequestTimeoutMs(int connectionRequestTimeoutMs) {
      this.connectionRequestTimeoutMs = connectionRequestTimeoutMs;
   }

   public int getConnectionTimeoutMs() {
      return connectionTimeoutMs;
   }

   public void setConnectionTimeoutMs(int connectionTimeoutMs) {
      this.connectionTimeoutMs = connectionTimeoutMs;
   }

   public int getSocketTimeoutMs() {
      return socketTimeoutMs;
   }

   public void setSocketTimeoutMs(int socketTimeoutMs) {
      this.socketTimeoutMs = socketTimeoutMs;
   }

   public String getOauthEndpoint() {
      return oauthEndpoint;
   }

   public void setOauthEndpoint(String oauthEndpoint) {
      this.oauthEndpoint = oauthEndpoint;
   }

   public int getPreemptRefreshTimeMins() {
      return preemptRefreshTimeMins;
   }

   public void setPreemptRefreshTimeMins(int preemptRefreshTimeMins) {
      this.preemptRefreshTimeMins = preemptRefreshTimeMins;
   }

   public String getOauthClientId() {
      return oauthClientId;
   }

   public void setOauthClientId(String oauthClientId) {
      this.oauthClientId = oauthClientId;
   }

   public String getOauthClientSecret() {
      return oauthClientSecret;
   }

   public void setOauthClientSecret(String oauthClientSecret) {
      this.oauthClientSecret = oauthClientSecret;
   }

   public String getEventEndpoint() {
      return eventEndpoint;
   }

   public void setEventEndpoint(String eventEndpoint) {
      this.eventEndpoint = eventEndpoint;
   }

   public int getCorrelationTimeoutSecs() {
      return correlationTimeoutSecs;
   }

   public void setCorrelationTimeoutSecs(int correlationTimeoutSecs) {
      this.correlationTimeoutSecs = correlationTimeoutSecs;
   }

   public boolean isProactiveEnabled() {
      return proactiveEnabled;
   }

   public void setProactiveEnabled(boolean proactiveEnabled) {
      this.proactiveEnabled = proactiveEnabled;
   }

   public boolean isLockDeferredEnabled() {
      return lockDeferredEnabled;
   }

   public void setLockDeferredEnabled(boolean lockDeferredEnabled) {
      this.lockDeferredEnabled = lockDeferredEnabled;
   }

   public boolean isReportStateOffline() {
      return reportStateOffline;
   }

   public void setReportStateOffline(boolean reportStateOffline) {
      this.reportStateOffline = reportStateOffline;
   }
}

