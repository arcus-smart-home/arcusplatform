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
package com.iris.client.server.passkey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;
import com.iris.bridge.server.netty.IPTrackingUtil;
import com.iris.client.server.rest.passkey.PasskeyLoginOptionsRESTHandler;
import com.iris.core.dao.PersonDAO;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.network.ControlledNetworkClock;
import com.iris.network.NetworkClocks;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

public class PasskeyRateLimiterTest {
   private PasskeyConfig config;
   private ControlledNetworkClock clock;

   @Before
   public void setUp() {
      config = new PasskeyConfig();
      config.setRpId("example.com");
      config.setRateLimitCapacity(2);
      config.setRateLimitRate(0.2);
      clock = NetworkClocks.controlled();
   }

   @Test
   public void testBurstThenRefill() {
      PasskeyRateLimiter limiter = new PasskeyRateLimiter(config, clock);
      EmbeddedChannel channel = channel("203.0.113.9");
      assertEquals(0, limiter.tryAcquire(channel));
      assertEquals(0, limiter.tryAcquire(channel));
      assertEquals(TimeUnit.SECONDS.toNanos(5), limiter.tryAcquire(channel), TimeUnit.MILLISECONDS.toNanos(1));

      clock.tick(5001, TimeUnit.MILLISECONDS);
      assertEquals(0, limiter.tryAcquire(channel));
      assertTrue(limiter.tryAcquire(channel) > 0);
   }

   @Test
   public void testAddressesAreLimitedSeparately() {
      PasskeyRateLimiter limiter = new PasskeyRateLimiter(config, clock);
      EmbeddedChannel first = channel("203.0.113.9");
      limiter.tryAcquire(first);
      limiter.tryAcquire(first);
      assertTrue(limiter.tryAcquire(first) > 0);
      assertEquals(0, limiter.tryAcquire(channel("203.0.113.10")));
   }

   @Test
   public void testZeroCapacityTurnsLimitingOff() {
      config.setRateLimitCapacity(0);
      PasskeyRateLimiter limiter = new PasskeyRateLimiter(config, clock);
      EmbeddedChannel channel = channel("203.0.113.9");
      for (int i = 0; i < 100; i++) {
         assertEquals(0, limiter.tryAcquire(channel));
      }
   }

   @Test
   public void testClientAddress() {
      // the last X-Forwarded-For hop is the one the nearest proxy added
      assertEquals("198.51.100.7", PasskeyRateLimiter.clientAddress("203.0.113.9, 198.51.100.7", null));
      assertEquals("198.51.100.7", PasskeyRateLimiter.clientAddress(" 198.51.100.7 ", null));
      // IPv6 clients are grouped by /64
      assertEquals("2001:db8:1:2::/64", PasskeyRateLimiter.clientAddress("2001:db8:1:2::1", null));
      assertEquals("2001:db8:1:2::/64", PasskeyRateLimiter.clientAddress("2001:db8:1:2:ffff:1:2:3", null));
      assertEquals("2001:db8:1:3::/64", PasskeyRateLimiter.clientAddress("2001:db8:1:3::1", null));
      assertEquals("192.0.2.1", PasskeyRateLimiter.clientAddress("::ffff:192.0.2.1", null));
      // without a tracked address, the socket's
      assertEquals("192.0.2.1", PasskeyRateLimiter.clientAddress(null, new InetSocketAddress("192.0.2.1", 443)));
      assertEquals("192.0.2.1", PasskeyRateLimiter.clientAddress(" ", new InetSocketAddress("192.0.2.1", 443)));
      assertEquals("unknown", PasskeyRateLimiter.clientAddress(null, null));
      assertEquals("unknown", PasskeyRateLimiter.clientAddress("unknown", null));
   }

   @Test
   public void testLoginOptionsAnswersTooManyRequests() throws Exception {
      PasskeyService service = new PasskeyService(config, new InMemoryPasskeyDAO(), EasyMock.createNiceMock(PersonDAO.class), new InMemoryPlatformMessageBus());
      PasskeyLoginOptionsRESTHandler handler = new PasskeyLoginOptionsRESTHandler(
            new AlwaysAllow(), EasyMock.createNiceMock(BridgeMetrics.class), service, new PasskeyRateLimiter(config, clock));
      ChannelHandlerContext ctx = EasyMock.createNiceMock(ChannelHandlerContext.class);
      EasyMock.expect(ctx.channel()).andReturn(channel("203.0.113.9")).anyTimes();
      EasyMock.replay(ctx);

      assertEquals(HttpResponseStatus.OK, loginOptions(handler, ctx).status());
      assertEquals(HttpResponseStatus.OK, loginOptions(handler, ctx).status());
      FullHttpResponse limited = loginOptions(handler, ctx);
      assertEquals(HttpResponseStatus.TOO_MANY_REQUESTS, limited.status());
      assertEquals("5", limited.headers().get(HttpHeaderNames.RETRY_AFTER));
   }

   private static FullHttpResponse loginOptions(PasskeyLoginOptionsRESTHandler handler, ChannelHandlerContext ctx) throws Exception {
      return handler.respond(new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/passkey/LoginOptions"), ctx);
   }

   private static EmbeddedChannel channel(String ip) {
      EmbeddedChannel channel = new EmbeddedChannel();
      channel.attr(IPTrackingUtil.ip).set(ip);
      return channel;
   }
}
