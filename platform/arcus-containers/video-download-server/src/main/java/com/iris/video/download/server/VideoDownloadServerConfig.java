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
package com.iris.video.download.server;

import java.util.Base64;

import javax.annotation.PostConstruct;
import javax.crypto.spec.SecretKeySpec;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.iris.video.VideoConfig;

public class VideoDownloadServerConfig extends VideoConfig {

   @Inject @Named("video.download.secret")
   protected String videoDownloadSecret;

   @Inject(optional = true) @Named("video.download.session.timeout")
   protected long downloadSessionTimeout = 300;

   @Inject(optional = true) @Named("video.download.ssl.handshake.timeout")
   protected long downloadSslHandshakeTimeout = 30;

   @Inject(optional = true) @Named("video.download.ssl.closenotify.timeout")
   protected long downloadSslCloseNotifyTimeout = 30;

   @Inject(optional = true) @Named("video.bind.address")
   protected String bindAddress = "0.0.0.0";

   @Inject(optional = true) @Named("video.port")
   protected int tcpPort = 8284;

   @Inject(optional = true) @Named("video.boss.thread.count")
   protected int bossThreadCount = -1;

   @Inject(optional = true) @Named("video.worker.thread.count")
   protected int workerThreadCount = -1;

   @Inject(optional = true) @Named("video.so.keepalive")
   protected boolean soKeepAlive = true;

   @Inject(optional = true) @Named("video.so.backlog")
   protected int soBacklog = 10;

   @Inject(optional = true) @Named("video.tls")
   protected boolean tls = true;

   @Inject(optional = true) @Named("video.download.chunksize")
   protected int chunkSize = 32 * 1024;

   @Inject(optional = true) @Named("video.download.concurrency")
   protected int concurrency = 100;

   @Inject(optional = true) @Named("video.download.max.write.block.time")
   protected long maxWriteBufferBlockTime = -1;

   @Inject(optional = true) @Named("video.download.write.highwater")
   protected int writeHighWater = 32*1024;

   @Inject(optional = true) @Named("video.download.write.lowwater")
   protected int writeLowWater = 8*1024;

   @Inject(optional = true) @Named("video.download.snd.buffer.size")
   protected int sndBufferSize = 8*1024;

   protected SecretKeySpec secret;

   @Inject(optional = true) @Named("video.download.tmp.dir")
   private String tmpDir = "/tmp/video-download";

   @Inject(optional = true) @Named("video.download.convert.timeout.secs")
   private long convertTimeoutSecs = 120;

   @PostConstruct
   public void initialize() {
      byte[] secretKey = Base64.getDecoder().decode(videoDownloadSecret);
      this.secret = new SecretKeySpec(secretKey, "HmacSHA256");
   }

   public long getDownloadSessionTimeout() {
      return downloadSessionTimeout;
   }

   public void setDownloadSessionTimeout(long downloadSessionTimeout) {
      this.downloadSessionTimeout = downloadSessionTimeout;
   }

   public long getDownloadSslHandshakeTimeout() {
      return downloadSslHandshakeTimeout;
   }

   public void setDownloadSslHandshakeTimeout(long downloadSslHandshakeTimeout) {
      this.downloadSslHandshakeTimeout = downloadSslHandshakeTimeout;
   }

   public long getDownloadSslCloseNotifyTimeout() {
      return downloadSslCloseNotifyTimeout;
   }

   public void setDownloadSslCloseNotifyTimeout(long downloadSslCloseNotifyTimeout) {
      this.downloadSslCloseNotifyTimeout = downloadSslCloseNotifyTimeout;
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

   public int getChunkSize() {
      return chunkSize;
   }

   public void setChunkSize(int chunkSize) {
      this.chunkSize = chunkSize;
   }

   public int getConcurrency() {
      return concurrency;
   }

   public void setConcurrency(int concurrency) {
      this.concurrency = concurrency;
   }

   public long getMaxWriteBufferBlockTime() {
      return maxWriteBufferBlockTime;
   }

   public void setMaxWriteBufferBlockTime(long maxWriteBufferBlockTime) {
      this.maxWriteBufferBlockTime = maxWriteBufferBlockTime;
   }

   public int getWriteHighWater() {
      return writeHighWater;
   }

   public void setWriteHighWater(int writeHighWater) {
      this.writeHighWater = writeHighWater;
   }

   public int getWriteLowWater() {
      return writeLowWater;
   }

   public void setWriteLowWater(int writeLowWater) {
      this.writeLowWater = writeLowWater;
   }

   public int getSndBufferSize() {
      return sndBufferSize;
   }

   public void setSndBufferSize(int sndBufferSize) {
      this.sndBufferSize = sndBufferSize;
   }

   public SecretKeySpec getDownloadSecretAsSpec() {
      return secret;
   }

   public String getTmpDir() {
      return tmpDir;
   }

   public long getConvertTimeoutSecs() {
      return convertTimeoutSecs;
   }
}

