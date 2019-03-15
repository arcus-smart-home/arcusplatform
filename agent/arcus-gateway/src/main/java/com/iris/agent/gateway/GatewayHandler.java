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
package com.iris.agent.gateway;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.iris.agent.attributes.HubAttributesService;
import com.iris.io.Deserializer;
import com.iris.io.Serializer;
import com.iris.io.json.JSON;
import com.iris.messages.HubMessage;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.protoc.runtime.ProtocUtil;
import com.iris.protocol.ProtocolMessage;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;

class GatewayHandler extends SimpleChannelInboundHandler<Object> {
   private static final Logger log = LoggerFactory.getLogger(GatewayHandler.class);

   public static final long IDLE_TIMEOUT = TimeUnit.NANOSECONDS.convert(17, TimeUnit.SECONDS);
   public static final long PING_FREQ = TimeUnit.NANOSECONDS.convert(5000, TimeUnit.MILLISECONDS);
   public static final long PING_FREQ_CUTOFF = TimeUnit.NANOSECONDS.convert(4500, TimeUnit.MILLISECONDS);
   
   private final Serializer<PlatformMessage> platformSerializer = JSON.createSerializer(PlatformMessage.class);
   private final Serializer<ProtocolMessage> protocolSerializer = JSON.createSerializer(ProtocolMessage.class);
   private final Serializer<HubMessage> hubSerializer = JSON.createSerializer(HubMessage.class);
   private final Deserializer<HubMessage> hubDeserializer = JSON.createDeserializer(HubMessage.class);
   private final Deserializer<PlatformMessage> platformDeserializer = JSON.createDeserializer(PlatformMessage.class);
   private final Deserializer<ProtocolMessage> protocolDeserializer = JSON.createDeserializer(ProtocolMessage.class);

   private final Subject<PlatformMessage,PlatformMessage> authorizedMessages;
   private final Subject<PlatformMessage,PlatformMessage> registeredMessages;
   private final Subject<PlatformMessage,PlatformMessage> platformMessages;
   private final Subject<ProtocolMessage,ProtocolMessage> protocolMessages;

   private final WebSocketClientHandshaker handshaker;
   private final ByteBuf websocketFrameBuf;

   private ChannelHandlerContext ctx;
   private ChannelPromise handshakeFuture;

   private final AtomicBoolean authorized = new AtomicBoolean(false);
   private boolean connected;
   
   private long lastPlatformMsg;
   private long lastHubMsg;
   
   @SuppressWarnings({ "unchecked", "rawtypes", "null" })
   GatewayHandler(WebSocketClientHandshaker handshaker) {
      this.authorizedMessages = new SerializedSubject(PublishSubject.create());
      this.registeredMessages = new SerializedSubject(PublishSubject.create());
      this.platformMessages = new SerializedSubject(PublishSubject.create());
      this.protocolMessages = new SerializedSubject(PublishSubject.create());

      this.handshaker = handshaker;
      this.websocketFrameBuf = Unpooled.unreleasableBuffer(Unpooled.buffer(GatewayConnection.WEBSOCKETS_MAX_FRAME_LENGTH));

      this.lastHubMsg = System.nanoTime();
      this.lastPlatformMsg = System.nanoTime();
   }

   public SocketAddress getOutboundInterface() {
      return ctx.channel().localAddress();
   }

   @SuppressWarnings("null")
   boolean isConnected() {
      ChannelHandlerContext hctx = ctx;
      return connected && hctx != null && hctx.channel() != null && hctx.channel().isOpen();
   }
   
   long timeSinceLastPlatformMessage(TimeUnit unit) {
      return unit.convert(System.nanoTime() - lastPlatformMsg, TimeUnit.NANOSECONDS);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Gateway connection events
   /////////////////////////////////////////////////////////////////////////////

   public void onChannelClosed(final rx.Observer<?> obs) {
      onChannelFuture(ctx.channel().closeFuture(), obs);
   }

   public void onHandshakeComplete(final rx.Observer<?> obs) {
      onChannelFuture(handshakeFuture, obs);
   }

   private void onChannelFuture(@Nullable ChannelFuture future, final rx.Observer<?> obs) {
      if (future == null) {
         throw new NullPointerException("future");
      }

      future.addListener(new GenericFutureListener<Future<Object>>() {
         @Override
         public void operationComplete(@Nullable Future<Object> result) throws Exception {
            if (result == null) {
               obs.onError(new NullPointerException("future is null"));
               return;
            }

            try {
               result.get();
               obs.onCompleted();
            } catch (Exception ex) {
               obs.onError(ex);
            }
         }
      });
   }

   /////////////////////////////////////////////////////////////////////////////
   // Observables for platform / protocol messages
   /////////////////////////////////////////////////////////////////////////////

   public Observable<PlatformMessage> registered() {
      return registeredMessages;
   }

   public Observable<PlatformMessage> authorized() {
      return authorizedMessages;
   }

   public Observable<PlatformMessage> platform() {
      return platformMessages;
   }

   public Observable<ProtocolMessage> protocol() {
      return protocolMessages;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Gateway connection outgoing to platform
   /////////////////////////////////////////////////////////////////////////////
   
   public boolean send(PlatformMessage msg) {
      return send(msg, true);
   }

   public boolean send(PlatformMessage msg, boolean checkAuth) {
      Address address = msg.getDestination();
      if (address.isHubAddress() && !address.isBroadcast()) {
         return true;
      }
   
      ChannelHandlerContext c = ctx;
      if (c == null || c.channel() == null || (checkAuth && !authorized.get())) {
         return false;
      }

      try {
         ByteBuf buffer = ctx.alloc().ioBuffer();
         byte[] payload = platformSerializer.serialize(msg);
         ByteBufOutputStream out = new ByteBufOutputStream(buffer);
         hubSerializer.serialize(HubMessage.createPlatform(payload), out);
         IOUtils.closeQuietly(out);
   
         BinaryWebSocketFrame frame = new BinaryWebSocketFrame(buffer);
         c.writeAndFlush(frame);
         lastHubMsg = System.nanoTime();

         return true;
      } catch (IOException ex) {
         log.warn("gateway serialization failed, dropping message: {}", msg);
         return true;
      }
   }

   public boolean send(ProtocolMessage msg) {
      Address address = msg.getDestination();
      if (address.isHubAddress() && !address.isBroadcast()) {
         return true;
      }
   
   
      ChannelHandlerContext c = ctx;
      if (c == null || c.channel() == null || !authorized.get()) {
         return false;
      }
   
      try {
         ByteBuf buffer = ctx.alloc().ioBuffer();
         byte[] payload = protocolSerializer.serialize(msg);
         ByteBufOutputStream out = new ByteBufOutputStream(buffer);
         hubSerializer.serialize(HubMessage.createProtocol(payload), out);
         IOUtils.closeQuietly(out);
   
         BinaryWebSocketFrame frame = new BinaryWebSocketFrame(buffer);
         c.writeAndFlush(frame);
         lastHubMsg = System.nanoTime();

         return true;
      } catch (IOException ex) {
         log.warn("gateway serialization failed, dropping message: {}", msg);
         return false;
      }
   }
   
   void sendLogs(BlockingQueue<JsonObject> logs) {
      ChannelHandlerContext c = ctx;
      if (c == null || logs.isEmpty() || !connected) {
         return;
      }
   
      JsonArray lgs = new JsonArray();
      for (int i = 0; i < 1024; ++i) {
         JsonObject next = logs.poll();
         if (next == null) {
            break;
         }
   
         lgs.add(next);
      }
   
      try {
         String spayload = JSON.toJson(lgs);
         byte[] payload = spayload.getBytes(StandardCharsets.UTF_8);
   
         ByteBuf buffer = c.alloc().ioBuffer();
         ByteBufOutputStream out = new ByteBufOutputStream(buffer);
         hubSerializer.serialize(HubMessage.createLog(payload), out);
         IOUtils.closeQuietly(out);
   
         BinaryWebSocketFrame frame = new BinaryWebSocketFrame(buffer);
         c.writeAndFlush(frame);
   
         lastHubMsg = System.nanoTime();
      } catch (IOException ex) {
         log.warn("log serialization failed, dropping message", ex);
      }
   }
   
   void sendMetrics(@Nullable JsonObject metrics) {
      ChannelHandlerContext c = ctx;
      if (metrics == null || !connected) {
         return;
      }
   
      try {
         String spayload = JSON.toJson(metrics);
         byte[] payload = spayload.getBytes(StandardCharsets.UTF_8);
   
         ByteBuf buffer = c.alloc().ioBuffer();
         OutputStream out = new ByteBufOutputStream(buffer);
         hubSerializer.serialize(HubMessage.createMetrics(payload), out);
         IOUtils.closeQuietly(out);

         BinaryWebSocketFrame frame = new BinaryWebSocketFrame(buffer);
         c.writeAndFlush(frame);
   
         lastHubMsg = System.nanoTime();
      } catch (IOException ex) {
         log.warn("metrics serialization failed, dropping message", ex);
      }
   }
   
   boolean sendPing() {
      final ChannelHandlerContext c = ctx;
      if (c == null) {
         log.trace("skipping ping request since context is null");
         return false;
      }
   
      long curTime = System.nanoTime();
      long timeSinceLastPlatform = curTime - lastPlatformMsg;
      if (timeSinceLastPlatform > IDLE_TIMEOUT) {
         long sc = TimeUnit.SECONDS.convert(curTime - lastPlatformMsg, TimeUnit.NANOSECONDS);
         log.warn("gateway hasn't received message from platform in {} s, attempting to reconnect", sc);
         c.channel().eventLoop().execute(new Runnable() {
            @Override
            public void run() {
               log.warn("gateway closing connection, attempting to reconnect");
               c.channel().close();
               log.warn("gateway connection closed");
            }
         });
   
         return false;
      } else if (timeSinceLastPlatform > PING_FREQ_CUTOFF) {
         c.channel().eventLoop().execute(new Runnable() {
            @Override
            public void run() {
               log.trace("sending websocket ping");
               c.writeAndFlush(new PingWebSocketFrame());
               lastHubMsg = System.nanoTime();
            }
         });
      
         return true;
      } else {
         if (log.isTraceEnabled()) {
            long ms = TimeUnit.MILLISECONDS.convert(curTime - lastPlatformMsg, TimeUnit.NANOSECONDS);
            log.trace("skipping ping request since last platform message was received {} ms ago", ms);
         }

         return true;
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Gateway connection incoming from platform
   /////////////////////////////////////////////////////////////////////////////

   @Override
   protected void channelRead0(@Nullable ChannelHandlerContext ctx, @Nullable Object msg) throws Exception {
      if (ctx == null || msg == null) {
         return;
      }
   
      lastPlatformMsg = System.nanoTime();
      Channel ch = ctx.channel();
      if (!handshaker.isHandshakeComplete()) {
         handshaker.finishHandshake(ch, (FullHttpResponse) msg);

         connected = true;
         handshakeFuture.setSuccess();
         return;
      }
   
      if (msg instanceof FullHttpResponse) {
         log.warn("unxpected full http response: {}", msg);
         ctx.close();
         return;
      }
   
      WebSocketFrame frame = (WebSocketFrame) msg;
      if (frame instanceof BinaryWebSocketFrame) {
         websocketFrameBuf.clear();
         websocketFrameBuf.writeBytes(frame.content());
      } else if (frame instanceof ContinuationWebSocketFrame){
         if (websocketFrameBuf.isReadable()) {
            websocketFrameBuf.writeBytes(frame.content());
         } else {
            log.warn("continuation frame received without initial frame.");
            ctx.close();
         }
      } else if (frame instanceof PingWebSocketFrame) {
         log.trace("received websocket ping request from platform");
         ctx.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
         lastHubMsg = System.nanoTime();
         return;
      } else if (frame instanceof PongWebSocketFrame) {
         log.trace("received websocket pong response from platform");
         return;
      } else if (frame instanceof CloseWebSocketFrame) {
         log.warn("received websocket close request");
         ctx.close();
         return;
      }
   
      if (frame.isFinalFragment()) {
         decodeHubFrame(ctx, websocketFrameBuf);
      }
   }
   
   private void decodeHubFrame(ChannelHandlerContext ctx, ByteBuf buffer) {
      try {
         HubMessage message = hubDeserializer.deserialize(new ByteBufInputStream(buffer));

         switch (message.getType()) {
         case PROTOCOL: {
            ProtocolMessage msg = protocolDeserializer.deserialize(message.getPayload());
            if (isValidHubFrame(msg)) {
               dispatch(msg);
            }
   
            break;
         }
   
         case PLATFORM: {
            PlatformMessage msg = platformDeserializer.deserialize(message.getPayload());
            if (isValidHubFrame(msg)) {
               dispatch(ctx, msg);
            }
   
            break;
         }
   
         default:
            log.warn("dropping unknown message: {}", message);
            break;
         }
      } catch (Exception ex) {
         log.warn("could not decode websocket frame, dropping: {}", ProtocUtil.toHexString(buffer), ex);
      }
   }
   
   private void dispatch(ChannelHandlerContext ctx, PlatformMessage msg) {
      switch (msg.getMessageType()) {
      case MessageConstants.MSG_HUB_REGISTERED_REQUEST:
         registeredMessages.onNext(msg);
         break;
   
      case MessageConstants.MSG_HUB_AUTHORIZED_EVENT:
         authorized.set(true);
         authorizedMessages.onNext(msg);
         break;
   
      default:
         platformMessages.onNext(msg);
         break;
      }
   }
   
   private void dispatch(ProtocolMessage msg) {
      protocolMessages.onNext(msg);
   }
   
   private boolean isValidHubFrame(@Nullable PlatformMessage msg) {
      if (msg == null) {
         return false;
      }
   
      Address dst = msg.getDestination();
      return isValidHubFrame(msg, dst);
   }
   
   private boolean isValidHubFrame(@Nullable ProtocolMessage msg) {
      if (msg == null) {
         return false;
      }
   
      Address dst = msg.getDestination();
      return isValidHubFrame(msg, dst);
   }
   
   private boolean isValidHubFrame(Object msg, Address dst) {
      if (dst.isBroadcast()) {
         return true;
      }
   
      if (!dst.isHubAddress()) {
         log.warn("message is not addressed to hub, dropping: {}", msg);
         return false;
      }
   
      String hubId = HubAttributesService.getHubId();
      if (!hubId.equals(dst.getHubId())) {
         log.warn("message is not addressed to this hub, dropping: {}", msg);
         return false;
      }
   
      return true;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Gateway connection lifecycle
   /////////////////////////////////////////////////////////////////////////////
   
   @Nullable
   @SuppressWarnings({ "unused", "null" })
   ChannelFuture close() {
      final ChannelHandlerContext c = ctx;
      if (c == null) {
         log.trace("skipping close request because context is null");
         return null;
      }
   
      this.ctx = null;
      return c.close();
   }
   
   @Override
   public void handlerAdded(@Nullable ChannelHandlerContext ctx) throws Exception {
      Preconditions.checkNotNull(ctx);

      this.ctx = ctx;
      this.handshakeFuture = ctx.newPromise();
   }

   @Override
   @SuppressWarnings("null")
   public void channelActive(@Nullable ChannelHandlerContext ctx) throws Exception {
      handshaker.handshake(ctx.channel());
      super.channelActive(ctx);
   }

   @Override
   public void channelInactive(@Nullable ChannelHandlerContext ctx) throws Exception {
      authorized.set(false);
      connected = false;
      super.channelInactive(ctx);
   }

   @Override
   @SuppressWarnings("null")
   public void exceptionCaught(@Nullable ChannelHandlerContext ctx, @Nullable Throwable cause) throws Exception {
      if (cause != null) {
         log.warn("hub gateway exception: {}", cause.getMessage(), cause);
      } else {
         log.warn("hub gateway exception: unknown cause");
      }

      ChannelPromise hsf = handshakeFuture;
      if (hsf != null && !hsf.isDone()) {
         hsf.setFailure(cause);
      }

      ctx.close();
   }
}

