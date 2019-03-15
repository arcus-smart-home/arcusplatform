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
package com.iris.video.previewupload.server;

import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class VideoPreviewUploadServerConfig {
   @Inject(optional = true) @Named("ssl.handshake.timeout")
   private long sslHandshakeTimeout = TimeUnit.MILLISECONDS.convert(90, TimeUnit.SECONDS);

   @Inject(optional = true) @Named("ssl.close.notify.timeout")
   private long sslCloseNotifyTimeout = TimeUnit.MILLISECONDS.convert(90, TimeUnit.SECONDS);

   @Inject(optional = true) @Named("max.preview.bytes")
   private int maxPreviewSize = 5 * 1024 * 1024;

   public long getSslHandshakeTimeout() {
      return sslHandshakeTimeout;
   }

   public void setSslHandshakeTimeout(long sslHandshakeTimeout) {
      this.sslHandshakeTimeout = sslHandshakeTimeout;
   }

   public long getSslCloseNotifyTimeout() {
      return sslCloseNotifyTimeout;
   }

   public void setSslCloseNotifyTimeout(long sslCloseNotifyTimeout) {
      this.sslCloseNotifyTimeout = sslCloseNotifyTimeout;
   }

   public int getMaxPreviewSize() {
      return maxPreviewSize;
   }

   public void setMaxPreviewSize(int maxPreviewSize) {
      this.maxPreviewSize = maxPreviewSize;
   }
}

