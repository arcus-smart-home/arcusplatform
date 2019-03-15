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

import java.util.Set;

import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.config.BridgeServerConfig;
import com.iris.bridge.server.http.RequestAuthorizer;
import com.iris.bridge.server.http.RequestHandler;
import com.iris.bridge.server.http.RequestMatcher;
import com.iris.bridge.server.message.DeviceMessageHandler;
import com.iris.bridge.server.session.Session;
import com.iris.bridge.server.session.SessionFactory;
import com.iris.bridge.server.session.SessionListener;
import com.iris.bridge.server.session.SessionRegistry;
import com.iris.util.MdcContext.MdcContextReference;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class just composes an entire frame before passing it on to its descendant.
 */
public class Text10WebSocketServerHandler extends BaseWebSocketServerHandler {
   private static final Logger logger = LoggerFactory.getLogger(Text10WebSocketServerHandler.class);
   private static final Logger comlog = LoggerFactory.getLogger("COMLOG");

   private StringBuilder jsonBuffer = null;
   protected final DeviceMessageHandler<String> deviceMessageHandler;

   public Text10WebSocketServerHandler(
      BridgeServerConfig serverConfig,
      BridgeMetrics metrics,
      SessionFactory sessionFactory,
      Set<SessionListener> sessionListeners,
      SessionRegistry sessionRegistry,
      Set<RequestHandler> handlers,
      RequestMatcher webSocketUpgradeMatcher,
      RequestAuthorizer sessionAuthorizer,
      ClientFactory clientFactory,
      DeviceMessageHandler<String> deviceMessageHandler
      ) {
      super(serverConfig, metrics, sessionFactory, sessionListeners, sessionRegistry,
         handlers, webSocketUpgradeMatcher, sessionAuthorizer, clientFactory);
      this.deviceMessageHandler = deviceMessageHandler;
   }

   @Override
   protected void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
      if (logger.isTraceEnabled()) {
         logger.trace("Received incoming frame [{}]", frame.getClass().getName());
      }

   // Check for closing frame
      if (frame instanceof CloseWebSocketFrame) {
         if (jsonBuffer != null) {
             handleMessageCompleted(ctx, jsonBuffer.toString());
         }
         terminateSocketSessionWithExtremePrejudice(ctx.channel());
         closeWebSocket(ctx, (CloseWebSocketFrame) frame.retain());
         metrics.incSessionDestroyedCounter();
         return;
      }

      if (frame instanceof TextWebSocketFrame) {
         metrics.incFramesReceivedCounter();
         jsonBuffer = new StringBuilder();
         jsonBuffer.append(((TextWebSocketFrame)frame).text());
      } else if (frame instanceof ContinuationWebSocketFrame) {
         metrics.incFramesReceivedCounter();
         if (jsonBuffer != null) {
            jsonBuffer.append(((ContinuationWebSocketFrame)frame).text());
         } else {
            comlog.warn("Continuation frame received without initial frame.");
         }
      } else {
         super.handleWebSocketFrame(ctx, frame);
         return;
      }

      // Check if Text or Continuation Frame is final fragment and handle if needed.
      if (frame.isFinalFragment()) {
       handleMessageCompleted(ctx, jsonBuffer.toString());
       jsonBuffer = null;
      }
   }

   protected void handleMessageCompleted(ChannelHandlerContext ctx, String json) {
      Session socketSession = getSocketSession(ctx.channel());
      if (socketSession == null) {
         logger.warn("Received a message from the Web Socket but couldn't find a session socket. That should never happen.");
         socketSession = createAndSetSocketSession(clientFactory.get(ctx.channel()), ctx.channel(), metrics);
      }

      try(MdcContextReference ref = BridgeMdcUtil.captureAndInitializeContext(socketSession)) {
         String response = deviceMessageHandler.handleMessage(socketSession, json);
         if (response != null) {
            ctx.writeAndFlush(new TextWebSocketFrame(response));
         }
      }
   }
}

