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
package com.iris.video.service;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.crypto.spec.SecretKeySpec;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class VideoServiceConfig extends com.iris.video.VideoConfig {
   @Inject(optional = true) @Named("video.premium.quota")
   protected long videoPremiumQuota = 3L * 1024L * 1024L * 1024L;
   
   @Inject(optional = true) @Named("video.basic.quota")
   protected long videoBasicQuota = 0L;
   
   @Inject(optional = true) @Named("video.premium.max.favorite")
   protected long videoPremiumMaxFavorite = 150;  

	@Inject(optional = true) @Named("video.basic.max.favorite")
   protected long videoBasicMaxFavorite = 0;

	@Inject(optional = true) @Named("video.purge.delay")
   protected long purgeDelay = TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS);

   @Inject(optional = true) @Named("video.download.access.time")
   protected long videoDownloadAccessTime = TimeUnit.MILLISECONDS.convert(24, TimeUnit.HOURS);

   @Inject(optional = true) @Named("video.stream.access.time")
   protected long videoStreamAccessTime = TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS);

   @Inject(optional = true) @Named("video.stream.default.time")
   protected int videoStreamDefaultTime = (int) TimeUnit.SECONDS.convert(5, TimeUnit.MINUTES);
   
   @Inject(optional = true) @Named("video.stream.max.time")
   protected int videoStreamMaxTime = (int) TimeUnit.SECONDS.convert(20, TimeUnit.MINUTES);

   @Inject @Named("video.download.secret")
   protected String videoDownloadSecret;

   @Inject @Named("video.stream.secret")
   protected String videoStreamSecret;

   @Inject @Named("video.record.secret")
   protected String videoRecordSecret;

   @Inject(optional = true) @Named("video.quota.cache.expireTimeMs")
   protected long quotaCacheExpirationTime = TimeUnit.MILLISECONDS.convert(24, TimeUnit.HOURS);

   @Inject(optional = true) @Named("video.quota.cache.idleTimeMs")
   protected long quotaCacheAccessExpirationTime = TimeUnit.MILLISECONDS.convert(2, TimeUnit.HOURS);

   @Inject(optional = true) @Named("video.deleteall.max")
   protected long maxDeleteAll = 1L;

   protected SecretKeySpec recordSecret;
   protected SecretKeySpec streamSecret;
   protected SecretKeySpec downloadSecret;

   @PostConstruct
   public void initialize() {
      byte[] downloadSecretKey = Base64.getDecoder().decode(videoDownloadSecret);
      this.downloadSecret = new SecretKeySpec(downloadSecretKey, "HmacSHA256");

      byte[] streamSecretKey = Base64.getDecoder().decode(videoStreamSecret);
      this.streamSecret = new SecretKeySpec(streamSecretKey, "HmacSHA256");

      byte[] recordSecretKey = Base64.getDecoder().decode(videoRecordSecret);
      this.recordSecret = new SecretKeySpec(recordSecretKey, "HmacSHA256");
   }

   public SecretKeySpec getDownloadSecretAsSpec() {
      return downloadSecret;
   }

   public SecretKeySpec getStreamSecretAsSpec() {
      return streamSecret;
   }

   public SecretKeySpec getRecordSecretAsSpec() {
      return recordSecret;
   }

   public long getVideoPremiumQuota() {
      return videoPremiumQuota;
   }

   public void setVideoPremiumQuota(long videoPremiumQuota) {
      this.videoPremiumQuota = videoPremiumQuota;
   }

   public long getPurgeDelay() {
      return purgeDelay;
   }

   public void setPurgeDelay(long purgeDelay) {
      this.purgeDelay = purgeDelay;
   }

   public long getVideoDownloadAccessTime() {
      return videoDownloadAccessTime;
   }

   public void setVideoDownloadAccessTime(long videoDownloadAccessTime) {
      this.videoDownloadAccessTime = videoDownloadAccessTime;
   }

   public long getVideoStreamAccessTime() {
      return videoStreamAccessTime;
   }

   public void setVideoStreamAccessTime(long videoStreamAccessTime) {
      this.videoStreamAccessTime = videoStreamAccessTime;
   }

   public int getVideoStreamDefaultTime() {
      return videoStreamDefaultTime;
   }

   public void setVideoStreamDefaultTime(int videoStreamDefaultTime) {
      this.videoStreamDefaultTime = videoStreamDefaultTime;
   }
   
   public int getVideoStreamMaxTime() {
      return videoStreamMaxTime;
   }

   public void setVideoStreamMaxTime(int videoStreamMaxTime) {
      this.videoStreamMaxTime = videoStreamMaxTime;
   }

   public long getQuotaCacheExpirationTime() {
     return quotaCacheExpirationTime;
   }

   public void setQuotaCacheExpirationTime(long quotaCacheExpirationTime) {
     this.quotaCacheExpirationTime = quotaCacheExpirationTime;
   }

   public long getQuotaCacheAccessExpirationTime() {
      return quotaCacheAccessExpirationTime;
   }

   public void setQuotaCacheAccessExpirationTime(long quotaCacheAccessExpirationTime) {
      this.quotaCacheAccessExpirationTime = quotaCacheAccessExpirationTime;
   }

   public long getMaxDeleteAll() {
      return maxDeleteAll;
   }

   public void setMaxDeleteAll(long maxDeleteAll) {
      this.maxDeleteAll = maxDeleteAll;
   }
   
   public long getVideoBasicQuota() {
		return videoBasicQuota;
	}

	public void setVideoBasicQuota(long videoBasicQuota) {
		this.videoBasicQuota = videoBasicQuota;
	}
	
	public long getVideoPremiumMaxFavorite() {
		return videoPremiumMaxFavorite;
	}

	public void setVideoPremiumMaxFavorite(long videoPremiumMaxFavorite) {
		this.videoPremiumMaxFavorite = videoPremiumMaxFavorite;
	}
	
	public long getVideoBasicMaxFavorite() {
		return videoBasicMaxFavorite;
	}

	public void setVideoBasicMaxFavorite(long videoBasicMaxFavorite) {
		this.videoBasicMaxFavorite = videoBasicMaxFavorite;
	}
}

