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

import java.io.IOException;
import java.net.SocketException;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;

import javax.net.ssl.SSLException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.google.common.base.Objects;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.config.BridgeServerConfig;
import com.iris.bridge.server.http.HttpException;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.RequestAuthorizer;
import com.iris.bridge.server.http.RequestHandler;
import com.iris.bridge.server.http.RequestMatcher;
import com.iris.bridge.server.http.Responder;
import com.iris.bridge.server.http.impl.RequestHandlerImpl;
import com.iris.bridge.server.session.ClientToken;
import com.iris.bridge.server.session.Session;
import com.iris.bridge.server.session.SessionFactory;
import com.iris.bridge.server.session.SessionListener;
import com.iris.bridge.server.session.SessionRegistry;
import com.iris.util.MdcContext.MdcContextReference;

import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;

public class BaseWebSocketServerHandler extends SimpleChannelInboundHandler<Object> {
   private static final Logger logger = LoggerFactory.getLogger(BaseWebSocketServerHandler.class);

   private static final AttributeKey<WebSocketServerHandshaker> ATTR_WEBSOCKET_HANDLER =
         AttributeKey.valueOf(BaseWebSocketServerHandler.class.getName() + "$WebSocketHandler");

   protected final ClientFactory clientFactory;
   protected final BridgeMetrics metrics;
   protected final BridgeServerConfig serverConfig;
   protected final SessionFactory sessionFactory;
   protected final SessionRegistry sessionRegistry;
   protected final Set<SessionListener> sessionListeners;
   protected final Set<RequestHandler> handlers;
   protected final HttpSender httpSender;
   protected final RequestHandler webSocketUpgradeHandler;

   public BaseWebSocketServerHandler(
      BridgeServerConfig serverConfig,
      BridgeMetrics metrics,
      SessionFactory sessionFactory,
      Set<SessionListener> sessionListeners,
      SessionRegistry sessionRegistry,
      Set<RequestHandler> handlers,
      RequestMatcher webSocketUpgradeMatcher,
      RequestAuthorizer sessionAuthorizer,
      ClientFactory clientFactory
      ) {
      this.serverConfig = serverConfig;
      this.metrics = metrics;
      this.sessionFactory = sessionFactory;
      this.sessionListeners = sessionListeners;
      this.sessionRegistry = sessionRegistry;
      this.handlers = handlers;
      this.httpSender = new HttpSender(BaseWebSocketServerHandler.class, metrics);
      this.clientFactory = clientFactory;

      this.webSocketUpgradeHandler = new RequestHandlerImpl(
         webSocketUpgradeMatcher,
         sessionAuthorizer,
         new WebSocketUpgradeResponder()
      );
   }

   protected WebSocketServerHandshaker createWebSocketHandshaker(ChannelHandlerContext ctx, FullHttpRequest request) {
      // TODO verify it isn't already open...
      // TODO inject the factory
      WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
            getWebSocketLocation(request),
            null,
            false,
            serverConfig.getMaxFrameSize()
      );
      WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(request);
      return handshaker;
   }

   protected WebSocketServerHandshaker getWebSocketHandshaker(ChannelHandlerContext ctx) {
      return ctx.attr(ATTR_WEBSOCKET_HANDLER).get();
   }

   protected void closeWebSocket(
         ChannelHandlerContext ctx,
         CloseWebSocketFrame frame
   ) {
      WebSocketServerHandshaker handshaker = ctx.attr(ATTR_WEBSOCKET_HANDLER).getAndRemove();
      if(handshaker != null) {
         handshaker.close(ctx.channel(), frame);
      }
      ctx.close();
   }

   @Override
   public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
         throws Exception {
      if (evt instanceof IdleStateEvent) {
         PingPong pingSession = PingPong.get(ctx.channel());

         IdleStateEvent event = (IdleStateEvent)evt;
         if (event.state() == IdleState.READER_IDLE) {
            logger.trace("Reader Idle");

            if(serverConfig.isCloseOnReadIdle()) {
               logger.debug("closing channel {}, configured with closeOnReadIdle attribute", ctx.channel());
               ctx.close();
               return;
            }

            if (pingSession != null) {
               Long timeOfOldestPing = pingSession.getTimeOfOldestPing();
               if (timeOfOldestPing == null) {
                  if (ctx != null) {
                     logger.trace("Pinging");
                     ctx.writeAndFlush(new PingWebSocketFrame());
                     // If there is no timeout on no pong response, don't record the pings.
                     if (serverConfig.getWebSocketPongTimeout() > 0) {
                        pingSession.recordPing();
                     }
                  }
               }
               else {
                  long timeSinceLastPing = System.currentTimeMillis() - timeOfOldestPing;
                  if (logger.isTraceEnabled()) {
                     logger.trace("Oldest Ping [{}]", timeSinceLastPing/1000L);
                  }

                  if (timeSinceLastPing > (serverConfig.getWebSocketPongTimeout() * 1000L)) {
                     ctx.close();
                  }
               }
            }
         }
         else if (event.state() == IdleState.WRITER_IDLE) {
            if (ctx != null && pingSession != null ) {
               // This can be triggered from multiple worker threads so check the last ping before
               // sending a new one.
               Long timeOfLastPing = pingSession.getTimeOfLastPing();
               long lastPingTime = timeOfLastPing != null ? timeOfLastPing : 0;
               long timeSinceLastPing = System.currentTimeMillis() - lastPingTime;
               if (timeSinceLastPing >= (serverConfig.getWebSocketPingRate() * 1000L)) {
                  ctx.writeAndFlush(new PingWebSocketFrame());
                  logger.trace("Send ping on idle connection - Ping! Handler[{}]", this);
                  if (pingSession != null) {
                  // If there is no timeout on no pong response, don't record the pings.
                     if (serverConfig.getWebSocketPongTimeout() > 0) {
                        pingSession.recordPing();
                     }
                  }
               }
            }
         }
      }
      super.userEventTriggered(ctx, evt);
   }

   @Override
   protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
      if (msg instanceof FullHttpRequest) {
         FullHttpRequest req = (FullHttpRequest)msg;
         if (serverConfig.isAllowForwardedFor()) {
            try {
               IPTrackingUtil.updateIp(ctx, req.headers());
            } catch (Exception ex) {
               logger.trace("could not update ip based on headers", ex);
            }
         }

         metrics.incHttpReceivedCounter();
         Timer.Context timerContext = metrics.startProcessHttpRequestTimer();
         try {
            handleHttpRequest(ctx, req);
         } finally {
            timerContext.stop();
         }
      } else if (msg instanceof WebSocketFrame) {
         metrics.incWebSocketReceivedCounter();
         Timer.Context timerContext = metrics.startProcessWsRequestTimer();
         try {
            handleWebSocketFrame(ctx, (WebSocketFrame)msg);
         } finally {
            timerContext.stop();
         }
      } else {
         metrics.incUnknownFrameReceivedCounter();
      }
   }

   @Override
   public void channelInactive(ChannelHandlerContext ctx) throws Exception {
      logger.trace("Closing Connection [{}]", ctx);
      PingPong.clear(ctx.channel());
      // Be sure to get rid of the socket session.
      terminateSocketSessionWithExtremePrejudice(ctx.channel());
      super.channelInactive(ctx);
   }

   @Override
   public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
      if (ctx != null) {
         if (logger.isDebugEnabled()) {
            if (!(cause instanceof IOException) &&
               !(cause instanceof SSLException) && 
               !(cause instanceof RejectedExecutionException) && 
               !(cause instanceof DecoderException) && 
               !(cause instanceof SocketException)) {
               logger.debug("closing connection abnormally due to exception [{}]:", ctx, cause);
            }
         }

         ctx.close();
      }
   }

   protected void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
      if (frame instanceof PingWebSocketFrame) {
         if (logger.isTraceEnabled()) {
            logger.trace("Ping with payload [{}]", ByteBufUtil.hexDump(frame.content()));
         }

         PongWebSocketFrame pong = new PongWebSocketFrame(frame.content().retain());
         ctx.writeAndFlush(pong);
      }
      else if (frame instanceof PongWebSocketFrame) {
         PingPong pingPongSession = PingPong.get(ctx.channel());
         if (pingPongSession != null) {
            pingPongSession.recordPong();
         }
      }
      else {
         throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass()
               .getName()));
      }
   }

   protected void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
      try {
         if (!req.getDecoderResult().isSuccess()) {
            logger.warn("Error handling request: [{} {}] - Bad Request", req.getMethod(), req.getUri());
            metrics.incBadHttpRequestCounter();
            metrics.incErrorHttpRequestCounter();
            httpSender.sendError(ctx, HttpSender.STATUS_BAD_REQUEST, req);
            return;
         }

         if (webSocketUpgradeHandler.matches(req)) {
            webSocketUpgradeHandler.handleRequest(req, ctx);
            return;
         }

         for (RequestHandler handler : handlers) {
            if (handler.matches(req)) {
               handler.handleRequest(req, ctx);
               return;
            }
         }

         logger.warn("Error handling request: [{} {}] - Not Found", req.getMethod(), req.getUri());
         metrics.incNotFoundHttpRequestCounter();
         metrics.incErrorHttpRequestCounter();
         httpSender.sendError(ctx, HttpSender.STATUS_NOT_FOUND, req);
      }
      catch (HttpException ex) {
         if (ex.getStatusCode() == HttpSender.STATUS_NOT_MODIFIED) {
            metrics.incNotModifiedHttpRequestCounter();
            httpSender.sendNotModified(ctx, req);
         }
         else {
            if (ex.getStatusCode() == HttpSender.STATUS_FORBIDDEN) {
               metrics.incForbiddenHttpRequestCounter();
            }
            else if (ex.getStatusCode() == HttpSender.STATUS_NOT_FOUND) {
               metrics.incNotFoundHttpRequestCounter();
            }
            else if (ex.getStatusCode() == HttpSender.STATUS_BAD_REQUEST) {
               metrics.incBadHttpRequestCounter();
            }
            logger.warn("Error handling request: [{} {}]", req.getMethod(), req.getUri(), ex);
            metrics.incErrorHttpRequestCounter();
            httpSender.sendError(ctx, ex.getStatusCode(), req);
         }
      }
      catch(Exception e) {
         logger.warn("Error handling request: [{} {}]", req.getMethod(), req.getUri(), e);
         httpSender.sendError(ctx, HttpSender.STATUS_SERVER_ERROR, req);
      }
   }

   protected Session createAndSetSocketSession(Client client, Channel ch, BridgeMetrics metrics) {
      // Clean out any existing session (Which shouldn't ever be the case).
      terminateSocketSessionWithExtremePrejudice(ch);
      // Create and set the new socketSession.
      Session socketSession = createSession(client, ch, metrics);
      ch.attr(Session.ATTR_SOCKET_SESSION).set(socketSession);

      if (logger.isDebugEnabled()) {
         Client cln = socketSession.getClient();
         ClientToken tok = socketSession.getClientToken();
         logger.debug("New websocket created for client: [{}] token: [{}]", (cln != null) ? cln.getPrincipalName() : null, (tok != null) ? tok.getRepresentation() : null);
      }

      return socketSession;
   }

   protected Session getSocketSession(Channel ch) {
      return ch.attr(Session.ATTR_SOCKET_SESSION).get();
   }

   protected void terminateSocketSessionWithExtremePrejudice(Channel ch) {
      Session socketSession = ch.attr(Session.ATTR_SOCKET_SESSION).get();
      if (socketSession != null) {
         ch.attr(Session.ATTR_SOCKET_SESSION).remove();
         ClientToken clientToken = socketSession.getClientToken();
         if (clientToken != null) {
            Session existing = sessionRegistry.getSession(clientToken);
            if(existing != null && Objects.equal(ch, existing.getChannel())) {
               socketSession.destroy();
            } else {
               logger.debug("Not removing existing session because channels are different");
            }
         }
      }
   }

   protected Session createSession(Client client, Channel ch, BridgeMetrics metrics) {
      return sessionFactory.createSession(client, ch, metrics);
   }

   private String getWebSocketLocation(FullHttpRequest req) {
      return (serverConfig.isUseSsl() ? "wss://" : "ws://") + 
         req.headers().get("Host") + 
         "/" + 
         this.serverConfig.getWebSocketPath();
   }

   private static void updateClientInfo(FullHttpRequest request, Session socketSession) {
      String clientType = BridgeHeaders.getClientType(request);
      socketSession.setClientType(clientType);
      
      String clientVersion = BridgeHeaders.getClientVersion(request);
      if(!StringUtils.isEmpty(clientVersion)) {
         socketSession.setClientVersion(clientVersion);
      }
   }
   private class WebSocketUpgradeResponder implements Responder {
      
      @Override
      public void sendResponse(FullHttpRequest req, ChannelHandlerContext ctx) throws Exception {
         Client client = clientFactory.get(ctx.channel());
         // Handshake
         WebSocketServerHandshaker handshaker = createWebSocketHandshaker(ctx, req);
         if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
         } else {
            handshaker.handshake(ctx.channel(), req);

            // The chunked write handler interferes with large websocket messages
            // so it needs to be removed from the pipeline since we are setting up
            // a websocket here.
            ctx.pipeline().remove(Bridge10ChannelInitializer.CHUNKED_WRITE_HANDLER);

            // Only create the session after the handshake.
            // at this point the session is not fully initialized
            // because we haven't gotten a message from the device that
            // can identify it. We only put the session in the registry when it
            // is initialized
            metrics.incSocketCounter();
            Session socketSession = createAndSetSocketSession(client, ctx.channel(), metrics);
            updateClientInfo(req, socketSession);
            try(MdcContextReference ref = BridgeMdcUtil.captureAndInitializeContext(socketSession)) {
               logger.trace("Getting ready to call session listeners [{}]", sessionListeners);
               sessionListeners.forEach((l) -> { l.onSessionCreated(socketSession); });
               if(socketSession.isInitialized()) {
                  sessionRegistry.putSession(socketSession);
               }
            }
         }
      }
   }

}

