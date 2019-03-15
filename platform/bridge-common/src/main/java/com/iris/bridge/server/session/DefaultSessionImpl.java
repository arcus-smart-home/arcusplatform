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
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.io.json.JSON;
import com.iris.messages.ClientMessage;
import com.iris.messages.address.Address;
import com.iris.messages.service.SessionService;

// TODO should this implement Client and delegate a bunch of methods to the parent?
public class DefaultSessionImpl implements Session {
   private static final Logger logger = LoggerFactory.getLogger(DefaultSessionImpl.class);
   private static final String ADDRESS_SESSION_SERVICE = Address.platformService(SessionService.NAMESPACE).getRepresentation();
   
   private final SessionRegistry parent;
   private final BridgeMetrics bridgeMetrics;

   private volatile ClientToken clientToken = null;
   private final Date sessionStartTime;
   private Channel channel;
   private volatile String activePlace = null;
   private volatile String clientType = TYPE_OTHER;
   private volatile String clientVersion;
   private AtomicBoolean disconnectFlag = new AtomicBoolean(false);

   public DefaultSessionImpl(SessionRegistry parent, Channel channel, BridgeMetrics bridgeMetrics) {
      this.parent = parent;
      this.bridgeMetrics = bridgeMetrics;
      this.channel = channel;

      this.sessionStartTime = new Date();
   }

   @Override
   public boolean isInitialized() {
      return clientToken != null;
   }

   @Override
   public ClientToken getClientToken() {
      return this.clientToken;
   }

   @Override
   public void setClientToken(ClientToken clientToken) {
      this.clientToken = clientToken;
   }

   @Override
   public Channel getChannel() {
	   Channel channel =  this.channel;
	   Preconditions.checkState(channel != null, "Client has been disconnected");
      return channel;
   }

   @Override
   public Date getSessionStartTime() {
      return this.sessionStartTime;
   }

   @Override
   public void setActivePlace(String placeId) {
      this.activePlace = placeId;
   }

   @Override
   public String getActivePlace() {
      return this.activePlace;
   }

   @Override
   public void destroy() {
      parent.destroySession(this);
   }

   void destroy0() {
      this.channel = null;
   }
   
   @Override
   public void disconnect(int status) {
	   logger.debug("bridge session.disconnect is called with status {}", status);
	   Channel channel = this.channel;
	   if(channel == null) {
	      logger.debug("Channel is already null when disconnect is called");
	   }
	   else {
			if( !disconnectFlag.getAndSet(true) && channel.isActive() ) {
				ClientMessage msg =
					ClientMessage
						.builder()
						.withSource(ADDRESS_SESSION_SERVICE)
						.withPayload(SessionService.SessionExpiredEvent.builder().build())
						.create();

				// technically this should be implicit from the close websocket status, but we'll be completely sure
				channel.write(new TextWebSocketFrame(JSON.toJson(msg)));
		      channel
		          .writeAndFlush(new CloseWebSocketFrame(status, ""))
		          // we want to close the socket after this, closing the socket should result in destroy being called, or we have a bug.
		          .addListener((e) -> channel.close());
		  }
	   }
	   
   }

   @Override
   public void sendMessage(byte[] msg) {
      ByteBuf byteBuf = Unpooled.wrappedBuffer(msg);
      sendMessage(byteBuf);
   }

   @Override
   public void sendMessage(ByteBuf msg) {
      bridgeMetrics.incFramesSentCounter();
      getChannel().writeAndFlush(new BinaryWebSocketFrame(msg));
   }

   @Override
   public void sendMessage(String msg) {
      bridgeMetrics.incFramesSentCounter();
      getChannel().writeAndFlush(new TextWebSocketFrame(msg));
   }

   @Override
   public BridgeMetrics metrics() {
      return bridgeMetrics;
   }
   
   @Override
   public String getClientType() {
      return clientType;
   }
   
   @Override
   public void setClientType(String clientType) {
      if(clientType == null) {
         this.clientType = TYPE_OTHER;
      }
      else {
         this.clientType = clientType;
      }
   }
   
   @Override
   public String getClientVersion() {
      return clientVersion;
   }
   
   @Override
   public void setClientVersion(String clientVersion) {
      this.clientVersion = clientVersion;
   }

   @Override
   public ClientFactory getClientFactory() {
      return parent.getClientFactory();
   }
   
   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "DefaultSessionImpl [clientToken=" + clientToken
            + ", sessionStartTime=" + sessionStartTime + ", activePlace="
            + activePlace + ", clientType=" + clientType + ", clientVersion="
            + clientVersion + "]";
   }

	

}

