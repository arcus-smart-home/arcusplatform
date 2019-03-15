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
package com.iris.video.recording.server;

import java.util.Base64;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.iris.video.VideoConfig;
import com.netflix.governator.configuration.ConfigurationProvider;

public class VideoRecordingServerConfig extends VideoConfig {

   @Inject @Named("video.record.secret")
   protected String videoRecordSecret;

   @Inject(optional = true) @Named("video.recording.session.timeout")
   protected long recordingSessionTimeout = 300;

   @Inject(optional = true) @Named("video.recording.ssl.handshake.timeout")
   protected long recordingSslHandshakeTimeout = 30;

   @Inject(optional = true) @Named("video.recording.ssl.closenotify.timeout")
   protected long recordingSslCloseNotifyTimeout = 30;

   @Inject(optional = true) @Named("video.recording.read.idle.timeout")
   protected long readIdleTimeout = 30;

   @Inject(optional = true) @Named("video.bind.address")
   protected String bindAddress = "0.0.0.0";

   @Inject(optional = true) @Named("video.port")
   protected int tcpPort = 8083;

   @Inject(optional = true) @Named("video.boss.thread.count")
   protected int bossThreadCount = -1;

   @Inject(optional = true) @Named("video.worker.thread.count")
   protected int workerThreadCount = -1;

   @Inject(optional = true) @Named("video.so.keepalive")
   protected boolean soKeepAlive = true;

   @Inject(optional = true) @Named("video.so.backlog")
   protected int soBacklog = 10000;

   @Inject(optional = true) @Named("video.tls")
   protected boolean tls = true;

   @Inject(optional = true) @Named("video.flush.frequency")
   protected double videoFlushFrequency = 1.0;

   @Inject
   protected ConfigurationProvider configProvider;

   public byte[] getRecordSecretAsBytes() {
      return Base64.getDecoder().decode(videoRecordSecret);
   }

   public long getRecordingSessionTimeout() {
      return recordingSessionTimeout;
   }

   public void setRecordingSessionTimeout(long recordingSessionTimeout) {
      this.recordingSessionTimeout = recordingSessionTimeout;
   }

   public long getRecordingSslHandshakeTimeout() {
      return recordingSslHandshakeTimeout;
   }

   public void setRecordingSslHandshakeTimeout(long recordingSslHandshakeTimeout) {
      this.recordingSslHandshakeTimeout = recordingSslHandshakeTimeout;
   }

   public long getRecordingSslCloseNotifyTimeout() {
      return recordingSslCloseNotifyTimeout;
   }

   public void setRecordingSslCloseNotifyTimeout(long recordingSslCloseNotifyTimeout) {
      this.recordingSslCloseNotifyTimeout = recordingSslCloseNotifyTimeout;
   }

   public long getReadIdleTimeout() {
      return readIdleTimeout;
   }

   public void setReadIdleTimeout(long readIdleTimeout) {
      this.readIdleTimeout = readIdleTimeout;
   }

   public String getBindAddress() {
      return bindAddress;
   }

   public void setBindAddress(String bindAddress) {
      this.bindAddress = bindAddress;
   }

   public int getTcpPort() {
      return tcpPort;
   }

   public void setTcpPort(int tcpPort) {
      this.tcpPort = tcpPort;
   }

   public int getBossThreadCount() {
      if (bossThreadCount <= 0) {
         return Runtime.getRuntime().availableProcessors();
      }

      return bossThreadCount;
   }

   public void setBossThreadCount(int bossThreadCount) {
      this.bossThreadCount = bossThreadCount;
   }

   public int getWorkerThreadCount() {
      if (workerThreadCount <= 0) {
         return Runtime.getRuntime().availableProcessors();
      }

      return workerThreadCount;
   }

   public void setWorkerThreadCount(int workerThreadCount) {
      this.workerThreadCount = workerThreadCount;
   }

   public boolean isSoKeepAlive() {
      return soKeepAlive;
   }

   public void setSoKeepAlive(boolean soKeepAlive) {
      this.soKeepAlive = soKeepAlive;
   }

   public int getSoBacklog() {
      return soBacklog;
   }

   public void setSoBacklog(int soBacklog) {
      this.soBacklog = soBacklog;
   }

   public boolean isTls() {
      return tls;
   }

   public void setTls(boolean tls) {
      this.tls = tls;
   }

   public double getVideoFlushFrequency() {
      return videoFlushFrequency;
   }

   public void setVideoFlushFrequency(double videoFlushFrequency) {
      this.videoFlushFrequency = videoFlushFrequency;
   }
}

