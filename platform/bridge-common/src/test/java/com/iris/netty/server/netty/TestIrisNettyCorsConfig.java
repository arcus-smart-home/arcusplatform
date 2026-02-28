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
package com.iris.netty.server.netty;

import static org.junit.Assert.*;

import org.junit.Test;

import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;

/**
 * Verifies that IrisNettyCorsConfig produces a CorsConfig whose header values
 * pass Netty's strict header validation (introduced in 4.1.128).
 */
public class TestIrisNettyCorsConfig {

   @Test
   public void testCorsHeadersDoNotThrowOnWrite() {
      // Build the config using defaults (comma-separated values that need trimming)
      IrisNettyCorsConfig irisConfig = new IrisNettyCorsConfig();
      irisConfig.init();
      CorsConfig corsConfig = irisConfig.build();

      // Set up a minimal pipeline with the CorsHandler
      EmbeddedChannel channel = new EmbeddedChannel(new CorsHandler(corsConfig));

      // Send an HTTP request with a matching Origin header through the pipeline
      DefaultFullHttpRequest request = new DefaultFullHttpRequest(
            HttpVersion.HTTP_1_1, HttpMethod.POST, "/login");
      request.headers().set(HttpHeaderNames.ORIGIN, "https://one.example.com");
      channel.writeInbound(request);

      // Write a response back through the pipeline — this is where untrimmed
      // header values would cause an IllegalArgumentException from Netty's
      // header validation, preventing any response from being sent (502).
      FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
      channel.writeOutbound(response);

      // Verify the response made it through with CORS headers attached
      FullHttpResponse out = channel.readOutbound();
      assertNotNull("Response should have been written", out);
      assertNotNull("Access-Control-Allow-Origin should be set",
            out.headers().get(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN));

      out.release();
      channel.finish();
   }

   @Test
   public void testCorsHeadersWithAnyOrigin() {
      IrisNettyCorsConfig irisConfig = new IrisNettyCorsConfig();
      // Use reflection to set allowAny since it's injected
      try {
         java.lang.reflect.Field field = IrisNettyCorsConfig.class.getDeclaredField("allowAny");
         field.setAccessible(true);
         field.set(irisConfig, true);
      } catch (Exception e) {
         fail("Could not set allowAny field: " + e.getMessage());
      }
      irisConfig.init();
      CorsConfig corsConfig = irisConfig.build();

      EmbeddedChannel channel = new EmbeddedChannel(new CorsHandler(corsConfig));

      DefaultFullHttpRequest request = new DefaultFullHttpRequest(
            HttpVersion.HTTP_1_1, HttpMethod.POST, "/login");
      request.headers().set(HttpHeaderNames.ORIGIN, "https://anything.example.com");
      channel.writeInbound(request);

      FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
      channel.writeOutbound(response);

      FullHttpResponse out = channel.readOutbound();
      assertNotNull("Response should have been written", out);
      assertEquals("https://anything.example.com",
            out.headers().get(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN));

      out.release();
      channel.finish();
   }
}
