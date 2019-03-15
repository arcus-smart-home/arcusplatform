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
package com.iris.video.streaming.server.http;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.*;

import java.util.List;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.RequestAuthorizer;
import com.iris.bridge.server.http.impl.HttpResource;
import com.iris.video.VideoUtil;

public abstract class AbstractStreamingHandler extends HttpResource {
   private static final Logger logger = LoggerFactory.getLogger(AbstractStreamingHandler.class);
   private final SecretKeySpec secret;

   public AbstractStreamingHandler(RequestAuthorizer authorizer, HttpSender httpSender, SecretKeySpec secret) {
      super(authorizer, httpSender);
      this.secret = secret;
   }

   protected FullHttpResponse getNoCacheFullHttpResponse(ByteBuf response, String contentType)
   {
      FullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, response);
      httpResponse.headers().set(HttpHeaders.Names.CONTENT_TYPE, contentType);
      setNoCacheHeaders(httpResponse);
      return httpResponse;
   }

   protected FullHttpResponse getMpegFullHttpResponse(ByteBuf response) {
      return getNoCacheFullHttpResponse(response, "application/x-mpegurl");
   }

   protected boolean validateRequest(FullHttpRequest request, UUID recordingId, QueryStringDecoder decoder) throws Exception {
      List<String> sigs = decoder.parameters().get("sig");
      if (sigs == null || sigs.size() != 1) {
         logger.debug("failed validation because sigs is null or size is not 1: {}", recordingId);
         return false;
      }

      List<String> exps = decoder.parameters().get("exp");
      if (exps == null || exps.size() != 1) {
         logger.debug("failed validation because sigs is null or size is not 1: {}", recordingId);
         return false;
      }

      String sigRaw = sigs.get(0);
      if (sigRaw == null) {
         logger.debug("failed validation because sigRaw is null: {}", recordingId);
         return false;
      }

      String sig = sigRaw.trim();
      if (sig.isEmpty()) {
         logger.debug("failed validation because sigRaw is empty: {}", recordingId);
         return false;
      }

      String exp = exps.get(0);
      if (!VideoUtil.verifyAccessSignature(secret, recordingId, exp, sig)) {
         logger.debug("failed validation because VideoUtil.verifyAccessSignature failed: {}", recordingId);
         return false;
      }

      return true;
   }

   @Nullable
   protected UUID getRecordingId(QueryStringDecoder uri, int start, int end, @Nullable String finl) {
      try {
         String path = uri.path();
         String recording = path.substring(start, end);

         UUID uuid = UUID.fromString(recording);
         if (uuid.version() != 1) {
            logger.debug("failed to retreive recording id because uuid wasn't version 1: {}", uri);
            return null;
         }

         if (finl != null) {
            String fin = path.substring(end);
            if (!finl.equals(fin)) {
               logger.debug("failed to retreive recording id because url was wrong: {}", uri);
               return null;
            }
         }

         return uuid;
      } catch (Exception ex) {
         return null;
      }
   }
}

