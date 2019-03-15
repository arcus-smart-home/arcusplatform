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
package com.iris.video.streaming.server;

import java.util.Base64;

import javax.annotation.PostConstruct;
import javax.crypto.spec.SecretKeySpec;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.iris.video.VideoConfig;

public class VideoStreamingServerConfig extends VideoConfig {

   @Inject(optional = true) @Named("port")
   protected int tcpPort = 8282;

   @Inject @Named("video.stream.secret")
   protected String videoStreamSecret;

   @Inject(optional = true) @Named("hls.target.length")
   protected int hlsTargetLength = 10;

   @Inject(optional = true) @Named("hls.segment.length")
   protected double hlsSegmentLength = 1.8;

   @Inject(optional = true) @Named("hls.segments.required")
   protected double hlsSegmentsRequired= 3.0;

   protected SecretKeySpec secret;

   @PostConstruct
   public void initialize() {
      byte[] secretKey = Base64.getDecoder().decode(videoStreamSecret);
      this.secret = new SecretKeySpec(secretKey, "HmacSHA256");
   }

   public int getTcpPort() {
      return tcpPort;
   }

   public void setTcpPort(int tcpPort) {
      this.tcpPort = tcpPort;
   }

   public SecretKeySpec getStreamingSecretAsSpec() {
      return secret;
   }

   public int getHlsTargetLength() {
      return hlsTargetLength;
   }

   public void setHlsTargetLength(int hlsTargetLength) {
      this.hlsTargetLength = hlsTargetLength;
   }

   public double getHlsSegmentLength() {
      return hlsSegmentLength;
   }

   public void setHlsSegmentLength(double hlsSegmentLength) {
      this.hlsSegmentLength = hlsSegmentLength;
   }

   public double getHlsSegmentsRequired() {
      return hlsSegmentsRequired;
   }

   public void setHlsSegmentsRequired(double hlsSegmentsRequired) {
      this.hlsSegmentsRequired = hlsSegmentsRequired;
   }
}

