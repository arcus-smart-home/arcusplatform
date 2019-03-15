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
package com.iris.bridge.server.session;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.util.Date;

import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.security.authz.AuthorizationContext;

public interface Session {
   public static final AttributeKey<Session> ATTR_SOCKET_SESSION = 
         AttributeKey.<Session>valueOf(Session.class.getName());
   
   public static final String TYPE_IOS = "ios";
   public static final String TYPE_ANDROID = "android";
   public static final String TYPE_OCULUS = "oculus";
   public static final String TYPE_BROWSER = "browser";
   public static final String TYPE_OTHER = "other";
   public static final String TYPE_HUB = "hub";
   public static final String TYPE_IPCD = "ipcd";

	public boolean isInitialized();

	public default Client getClient() {
	   return getClientFactory().get(getChannel());
	}

	public ClientToken getClientToken();

	public void setClientToken(ClientToken clientToken);

	public default AuthorizationContext getAuthorizationContext() {
	   Client c = getClient();
	   if(c == null) {
	      return null;
	   }
	   return c.getAuthorizationContext();
	}

   public default AuthorizationContext getAuthorizationContext(boolean reload) { 
      Client c = getClient();
      if(c == null) {
         return null;
      }
      return c.getAuthorizationContext(reload);
   }
   
	public Channel getChannel();

	public Date getSessionStartTime();

	public void destroy();

	public void sendMessage(byte[] msg);

	public void sendMessage(ByteBuf msg);

	public void sendMessage(String msg);

	public void setActivePlace(String placeId);

	public String getActivePlace();
	
	public String getClientType();
	
	public void setClientType(String clientType);
	
	public String getClientVersion();
	
	public void setClientVersion(String clientVersion);
	
	public void disconnect(int status);
	
	public BridgeMetrics metrics();

	public ClientFactory getClientFactory();
	
}

