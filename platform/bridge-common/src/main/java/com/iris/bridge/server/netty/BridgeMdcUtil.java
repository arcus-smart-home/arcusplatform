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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.session.Session;
import com.iris.messages.ClientMessage;
import com.iris.messages.Message;
import com.iris.messages.address.Address;
import com.iris.util.MdcContext;
import com.iris.util.MdcContext.MdcContextReference;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.AttributeKey;

public class BridgeMdcUtil {
   private static final AttributeKey<MdcContextReference> MDC_KEY =
         AttributeKey.valueOf(BridgeMdcUtil.class.getName() + "$MdcContextReference");
   
   private BridgeMdcUtil() {
   }

   public static void bindHttpContext(ClientFactory factory, Channel channel, FullHttpRequest request) {
      clearHttpContext(channel); // just in case
      
      String userAgent = request.headers().get(HttpHeaders.Names.USER_AGENT);

      String clientVersion = BridgeHeaders.getClientVersion(request);

      MdcContextReference context = MdcContext.captureMdcContext();
      channel.attr(MDC_KEY).set(context);
      
      if(!StringUtils.isEmpty(userAgent)) {
         MDC.put(MdcContext.MDC_USER_AGENT, userAgent);
      }
      
      if(!StringUtils.isEmpty(clientVersion)) {
         MDC.put(MdcContext.MDC_CLIENT_VERSION, clientVersion);
      }

      Client client = factory.get(channel);
      if(client != null) {
         MDC.put(MdcContext.MDC_TARGET, client.getPrincipalName());
      }
      
   }
   
   public static void clearHttpContext(Channel channel) {
      MdcContextReference old = channel.attr(MDC_KEY).getAndRemove();
      if(old != null) {
         old.close();
      }
   }
   
   public static MdcContextReference captureAndInitializeContext(Session session) {
      MdcContextReference ref = MdcContext.captureMdcContext();
      if(session == null) {
         return ref;
      }
      
      String place = session.getActivePlace();
      String principal = session.getClient() != null ? session.getClient().getPrincipalName() : null;
      String type = session.getClientType();
      String version = session.getClientVersion();
      
      if(!StringUtils.isEmpty(place)) {
         MDC.put(MdcContext.MDC_PLACE, place);
      }
      if(!StringUtils.isEmpty(principal)) {
         MDC.put(MdcContext.MDC_TARGET, principal);
      }
      if(StringUtils.isEmpty(version)) {
         version = "unknown";
      }
      MDC.put(MdcContext.MDC_CLIENT_VERSION, type + " " + version);
      return ref;
   }

   public static MdcContextReference captureAndInitializeContext(Session session, ClientMessage message) {
      String src = message.getSource();
      String dst = message.getDestination();
      String corr = message.getCorrelationId();
      String type = message.getType();

	   MdcContextReference context = BridgeMdcUtil.captureAndInitializeContext(session);

	   if (src != null) {
	      MDC.put(MdcContext.MDC_FROM, src);
	   }

      if (dst != null) {
	      MDC.put(MdcContext.MDC_TO, dst);
	   }

      if (corr != null) {
	      MDC.put(MdcContext.MDC_ID, corr);
	   }

      if (type != null) {
	      MDC.put(MdcContext.MDC_TYPE,  type);
	   }

	   return context;
   }

   public static MdcContextReference captureAndInitializeContext(Session session, Message message) {
      Address src = message != null ? message.getSource() : null;
      Address dst = message != null ? message.getDestination() : null;
      String corr = message != null ? message.getCorrelationId() : null;
      String type = message != null ? message.getMessageType() : null;
      Address by = message != null ? message.getActor() : null;

	   MdcContextReference context = BridgeMdcUtil.captureAndInitializeContext(session);

	   if (src != null) {
	      MDC.put(MdcContext.MDC_FROM, src.getRepresentation());
	   }

      if (dst != null) {
	      MDC.put(MdcContext.MDC_TO, dst.getRepresentation());
	   }

      if (corr != null) {
	      MDC.put(MdcContext.MDC_ID, corr);
	   }

      if (type != null) {
	      MDC.put(MdcContext.MDC_TYPE, type);
	   }
      
      if (by != null) {
         MDC.put(MdcContext.MDC_BY, by.getRepresentation());
      }

	   return context;
   }

}

