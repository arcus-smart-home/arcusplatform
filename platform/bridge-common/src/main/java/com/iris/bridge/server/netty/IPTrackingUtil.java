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
package com.iris.bridge.server.netty;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.apache.shiro.session.UnknownSessionException;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.session.Session;
import com.iris.util.MdcContext;
import com.iris.util.MdcContext.MdcContextReference;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.AttributeKey;

public class IPTrackingUtil {
   private static final Logger log = LoggerFactory.getLogger(IPTrackingUtil.class);
   public static final AttributeKey<String> ip = AttributeKey.valueOf("ip");

   private IPTrackingUtil() {
   }

   public static MdcContextReference captureAndInitializeContext(ChannelHandlerContext ctx) {
	   MdcContextReference context = MdcContext.captureMdcContext();

      try {
         Session session = getSocketSession(ctx.channel());
         if (session != null) {
	         String plc = session.getActivePlace();
	         if (plc != null) {
	            MDC.put(MdcContext.MDC_PLACE, plc);
	         }

            Client cln = session.getClient();
            if (cln != null) {
               try {
                  String prin = cln.getPrincipalName();
                  if (prin != null) {
	                  MDC.put(MdcContext.MDC_BY, prin);
                  }
               } catch (UnknownSessionException ex) {
                  // ignore
               }
            }
         }
      } catch (Throwable th) {
         log.trace("could not capture session information:", th);
      }

      String chIp = ctx.attr(ip).get();
	   if (chIp != null) {
	      MDC.put(MdcContext.MDC_IP, chIp);
	   }

	   return context;
	}

   public static void updateIp(ChannelHandlerContext ctx, HttpHeaders headers) {
      String forwarded = headers.get("X-Forwarded-For");
      if (forwarded != null) {
         String trimmed = forwarded.trim();
         if (!trimmed.isEmpty()) {
            ctx.attr(ip).set(trimmed);
            MDC.put(MdcContext.MDC_IP, trimmed);
         }
      }
   }

   public static void trackIp(ChannelHandlerContext ctx) {
      String chIp = ctx.attr(ip).get();
      if (chIp != null) {
         return; // ip address already setup
      }

	   chIp = getIpFromContext(ctx);
	   if (chIp != null) {
	      ctx.attr(ip).setIfAbsent(chIp);
	   }
	}

   @Nullable
   protected static String getIpFromContext(ChannelHandlerContext ctx) {
      try {
	      Channel chan = ctx.channel();
	      SocketAddress addr = (chan != null) ? chan.remoteAddress() : null;
	      if (addr instanceof InetSocketAddress) {
	         InetSocketAddress inet = (InetSocketAddress)addr;
	         return inet.getAddress().getHostAddress();
	      }
	   } catch (Exception ex) {
	      // ignore
	   }

      return null;
   }

   @Nullable
   protected static Session getSocketSession(Channel ch) {
      return ch.attr(Session.ATTR_SOCKET_SESSION).get();
   }
}

