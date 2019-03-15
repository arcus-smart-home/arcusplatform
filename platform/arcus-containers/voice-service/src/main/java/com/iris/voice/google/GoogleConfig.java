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
package com.iris.voice.google;

import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.voice.VoiceConfig;

@Singleton
public class GoogleConfig extends VoiceConfig {

   @Inject(optional = true)
   @Named("google.service.success.cheat.enabled")
   private boolean successCheatEnabled = true;

   @Inject(optional = true)
   @Named("google.service.whitelist.enabled")
   private boolean whitelistEnabled = false;

   @Inject(optional = true)
   @Named("google.service.whitelist")
   private String whitelist = "";

   @Inject(optional = true)
   @Named("googe.homegraph.api.url")
   private String homeGraphApiUrl = "https://homegraph.googleapis.com/v1/devices";

   @Inject
   @Named("google.homegraph.api.key")
   private String homeGraphApiKey;

   @Inject(optional = true)
   @Named("google.service.http.max.connections")
   private int maxConnections = 20;

   @Inject(optional = true)
   @Named("google.service.http.route.max.connections")
   private int routeMaxConnections = 10;

   @Inject(optional = true)
   @Named("google.service.http.inactivity.validate.ms")
   private int validateAfterInactivityMs = 2000;

   @Inject(optional = true)
   @Named("google.service.http.ttl.ms")
   private long timeToLiveMs = -1;

   @Inject(optional = true)
   @Named("google.service.http.connection.request.timeout.ms")
   private int connectionRequestTimeoutMs = 1000;

   @Inject(optional = true)
   @Named("google.service.http.connection.timeout.ms")
   private int connectionTimeoutMs = 5000;

   @Inject(optional = true)
   @Named("google.service.http.socket.timeout.ms")
   private int socketTimeoutMs = (int) TimeUnit.SECONDS.toMillis(30);
   
   @Inject(optional = true)
   @Named(value = "google.homegraph.report.state.enabled")
   private boolean reportStateEnabled = false;  // should we be sending Report State posts?

   @Inject(optional = true)
   @Named(value = "google.homegraph.grpc.key.file")
   private String gRpcKeyFile; // Example Value: classpath:/eyeristest-43d52-b00fe89f00d6.json; 

   @Inject(optional = true)
   @Named(value = "google.homegraph.resport.state.delay.sec")
   private int reportStateAfterSyncDelaySec = 10; // number of seconds after a SYNC response before sending a Report State

   public boolean isSuccessCheatEnabled() {
      return successCheatEnabled;
   }

   public void setSuccessCheatEnabled(boolean successCheatEnabled) {
      this.successCheatEnabled = successCheatEnabled;
   }

   public boolean isWhitelistEnabled() {
      return whitelistEnabled;
   }

   public void setWhitelistEnabled(boolean whitelistEnabled) {
      this.whitelistEnabled = whitelistEnabled;
   }

   public String getWhitelist() {
      return whitelist;
   }

   public void setWhitelist(String whitelist) {
      this.whitelist = whitelist;
   }

   public String getHomeGraphApiUrl() {
      return homeGraphApiUrl;
   }

   public void setHomeGraphApiUrl(String homeGraphApiUrl) {
      this.homeGraphApiUrl = homeGraphApiUrl;
   }

   public String getHomeGraphApiKey() {
      return homeGraphApiKey;
   }

   public void setHomeGraphApiKey(String homeGraphApiKey) {
      this.homeGraphApiKey = homeGraphApiKey;
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
   
   public boolean isReportStateEnabled() {
      return this.reportStateEnabled;
   }

   public void setReportStateEnabled(boolean reportStateEnabled) {
      this.reportStateEnabled = reportStateEnabled;
   }

   public String getgRpcKeyFile() {
      return this.gRpcKeyFile;
   }

   public void setgRpcKeyFile(String gRpcKeyFile) {
      this.gRpcKeyFile = gRpcKeyFile;
   }

   public int getReportStateDelaySec() {
      return this.reportStateAfterSyncDelaySec;
   }
   
   public void setReportStateDelaySec(int reportStateAfterSyncDelaySec) {
      this.reportStateAfterSyncDelaySec = reportStateAfterSyncDelaySec;
   }

}

