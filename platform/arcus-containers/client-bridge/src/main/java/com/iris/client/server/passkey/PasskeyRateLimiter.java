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

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.net.InetAddresses;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.server.netty.IPTrackingUtil;
import com.iris.network.NetworkClock;
import com.iris.network.NetworkClocks;
import com.iris.network.RateLimiter;
import com.iris.network.RateLimiters;

import io.netty.channel.Channel;

/**
 * A token bucket per client address for the passkey endpoints anyone can call, since
 * each request writes to Cassandra.
 *
 * The address is the one the bridge already tracks: X-Forwarded-For when
 * {@code allow.forwardedfor} is set, otherwise the socket's remote address. Behind a
 * proxy without {@code allow.forwardedfor}, every client shares the proxy's bucket.
 */
@Singleton
public class PasskeyRateLimiter {
   private static final Logger logger = LoggerFactory.getLogger(PasskeyRateLimiter.class);

   // a flood from more addresses than this evicts buckets, which start over full
   private static final int MAX_ADDRESSES = 10000;

   private final boolean enabled;
   private final NetworkClock clock;
   private final LoadingCache<String, RateLimiter> buckets;

   @Inject
   public PasskeyRateLimiter(PasskeyConfig config) {
      this(config, NetworkClocks.system());
   }

   PasskeyRateLimiter(PasskeyConfig config, NetworkClock clock) {
      int capacity = config.getRateLimitCapacity();
      double ratePerSecond = config.getRateLimitRate();
      this.enabled = capacity > 0 && ratePerSecond > 0;
      this.clock = clock;
      // an idle bucket is full again by the time it's dropped, so dropping it changes nothing
      long refillSecs = enabled ? (long) Math.ceil(capacity / ratePerSecond) : 1;
      this.buckets = CacheBuilder.newBuilder()
            .maximumSize(MAX_ADDRESSES)
            .expireAfterAccess(refillSecs, TimeUnit.SECONDS)
            .build(CacheLoader.from(() -> RateLimiters.tokenBucket(capacity, ratePerSecond).build()));
   }

   /**
    * Takes a token for the client on this channel.
    *
    * @return 0 if the request can go ahead, otherwise about how many nanoseconds until it can
    */
   public long tryAcquire(Channel channel) {
      if (!enabled) {
         return 0;
      }
      String address = clientAddress(channel.attr(IPTrackingUtil.ip).get(), channel.remoteAddress());
      long waitNs = buckets.getUnchecked(address).tryAcquireOrGetWait(1.0, clock);
      if (waitNs > 0) {
         logger.debug("Rate limited passkey request from [{}]", address);
      }
      return waitNs;
   }

   /**
    * The address to count a request against. Of a list of X-Forwarded-For hops, the last
    * is the one added by the nearest proxy, which the client can't forge. IPv6 addresses
    * are grouped by /64, since a single client usually has a whole /64.
    */
   static String clientAddress(@Nullable String tracked, @Nullable SocketAddress remote) {
      String address = null;
      if (tracked != null) {
         address = tracked.substring(tracked.lastIndexOf(',') + 1).trim();
      }
      if (StringUtils.isEmpty(address) && remote instanceof InetSocketAddress && ((InetSocketAddress) remote).getAddress() != null) {
         address = ((InetSocketAddress) remote).getAddress().getHostAddress();
      }
      if (StringUtils.isEmpty(address)) {
         return "unknown";
      }
      if (!InetAddresses.isInetAddress(address)) {
         return address;
      }
      InetAddress inet = InetAddresses.forString(address);
      if (inet instanceof Inet6Address) {
         byte[] prefix = inet.getAddress();
         Arrays.fill(prefix, 8, 16, (byte) 0);
         try {
            return InetAddresses.toAddrString(InetAddress.getByAddress(prefix)) + "/64";
         }
         catch (UnknownHostException e) {
            // can't happen, the address is 16 bytes
            return address;
         }
      }
      return InetAddresses.toAddrString(inet);
   }
}
