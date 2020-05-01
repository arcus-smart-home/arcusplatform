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
package com.iris.client.impl.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker13;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.JdkSslClientContext;
import io.netty.handler.ssl.SslContext;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.client.impl.netty.WebsocketStateHandler.CloseCause;

public class Client {
   private static final Logger logger = LoggerFactory.getLogger(Client.class);
   private static final int STATUS_EXPIRED_STATUS = 4001;
   
   private final TrustManagerFactory trustManagerFactory;
   private final EventLoopGroup eventLoopGroup;
   private final WebsocketStateHandler websocketStateHandler;
   private final Map<URI, HttpRequester> httpRequesterMap = new ConcurrentHashMap<>();
   private final int retryAttempts; 
   private final int retryDelay;
   private final int maxResponseSize;
   
   private final AtomicReference<Channel> channelRef = new AtomicReference<Channel>();
   private final AtomicInteger retries = new AtomicInteger();
   private final AtomicReference<State> stateRef = new AtomicReference<>(State.CLOSED);
   
   private NettyWebsocket websocket;
   
   public Client(TrustManagerFactory trustManagerFactory, WebsocketStateHandler websocketStateHandler, int retryAttempts, int retryDelay, int maxResponseSize) {
      this.trustManagerFactory = trustManagerFactory;
      this.eventLoopGroup = new NioEventLoopGroup();
      this.websocketStateHandler = websocketStateHandler;
      this.retryAttempts = retryAttempts;
      this.retryDelay = retryDelay;
      this.maxResponseSize = maxResponseSize;
   }
   
   public void executeAsyncHttpRequest(NettyHttpRequest request) {
      try {
         URI authority = request.getUri().parseServerAuthority();
         HttpRequester requester = httpRequesterMap.get(authority);
         if (requester == null) {
            requester = new HttpRequester(authority,
                  trustManagerFactory,
                  new LostHttpConnectionHandler() {
                     @Override
                     public void connectionLost(URI uri) {
                        httpRequesterMap.remove(uri);
                     }
                  },
                  retryAttempts,
                  retryDelay,
                  maxResponseSize);
            httpRequesterMap.put(authority, requester);
         }
         requester.execute(request);
      } catch (URISyntaxException e) {
         throw new RuntimeException("Invalid URI Syntax for Http Request: " + request.getUri(), e);
      }
   }
   
   public void disconnect() {
      disconnect(CloseCause.REQUESTED);
   }
   
   public void shutdown() {
      disconnect();
      eventLoopGroup.shutdownGracefully();
   }

   public void openWebSocket(NettyWebsocket websocket) {
      disconnect();
      this.websocket = websocket;
      this.stateRef.set(State.DISCONNECTED);
      connectWebsocket();
   }
   
   public void fire(final String message) {
      Channel channel = this.channelRef.get();
      if (channel != null) {
         channel.writeAndFlush(new TextWebSocketFrame(message));
      }
      else if(websocket != null) {
         // If the channel is null, then the client is probably in a state where it is 
         // connecting but the websocket isn't up yet.
      	// TODO: How many times do we reschedule before giving up?
         eventLoopGroup.schedule(new Runnable() {
            @Override
            public void run() {
               fire(message);
            }
         }, 1, TimeUnit.SECONDS);
      }
      else {
         throw new IllegalStateException("Client is closed, can't send message");
      }
   }
   
   private void connectWebsocket() {
      if(!this.stateRef.compareAndSet(State.DISCONNECTED, State.CONNECTING)) {
         // already in progress,
         logger.debug("Ignoring connectWebsockeRequest because state is [{}]", this.stateRef.get());
         return;
      }
      
      logger.debug("Netty Iris Client attempting to connect to {}", websocket.getUri());
      websocketStateHandler.onConnecting();
      
      String scheme = websocket.getUri().getScheme();
      
      if (!"ws".equalsIgnoreCase(scheme) 
            && !"wss".equalsIgnoreCase(scheme) 
            && !"http".equalsIgnoreCase(scheme) 
            && !"https".equalsIgnoreCase(scheme)) {
         logger.warn("hub gateway uri has invalid scheme: {}", websocket.getUri());
         throw new IllegalArgumentException("Unsupported protocol: " + scheme);
      }
      final boolean isSSL = "wss".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
      final String host = websocket.getUri().getHost();
      final int port = (websocket.getUri().getPort() < 0) ? (isSSL ? 443 : 80) : websocket.getUri().getPort();
      final HttpHeaders headers = new DefaultHttpHeaders();
      for (Entry<String, Object> header : websocket.getHeaders()) {
         headers.set(header.getKey(), header.getValue());
      }
      
      SslContext sslContext = null;
      if (isSSL) {
         if (trustManagerFactory == null) {
            logger.error("Attempted SSL connection with no trust manager factory defined.");
            throw new RuntimeException("Attempted SSL connection with no trust manager factory defined.");
         }
         try {
            sslContext = new JdkSslClientContext(trustManagerFactory);
         }
         catch (SSLException e) {
            logger.error("Error initializing ssl context", e);
            throw new RuntimeException("Error initializing ssl context", e);
         }
      }
      
      final SslContext sslCtx = sslContext;
      ChannelInitializer<SocketChannel> initializer = new ChannelInitializer<SocketChannel>() {

         @Override
         protected void initChannel(SocketChannel ch) throws Exception {
            if (ch == null) {
               throw new NullPointerException("ch");
            }

            if (sslCtx != null) {
               ch.pipeline().addLast("ssl", sslCtx.newHandler(ch.alloc(), host, port));
            }

            ch.pipeline()
              .addLast("http-codec", new HttpClientCodec())
              .addLast("aggregator", new HttpObjectAggregator(maxResponseSize))
              .addLast("ws-handler", new WebsocketClient(new WebsocketClientHandshaker(websocket.getUri(), headers, websocket.getMaxFrameSize())))
              .addLast("iris-text-handler", new TextChannelHandler(websocket.getTextHandler()));
         }    
      };
      
      final Bootstrap bootstrap = new Bootstrap();
      bootstrap.group(eventLoopGroup)
               .channel(NioSocketChannel.class)
               .handler(initializer)
               // .option(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT)
               // .option(ChannelOption.ALLOW_HALF_CLOSURE, true)
               // .option(ChannelOption.AUTO_CLOSE, true)
               // .option(ChannelOption.AUTO_READ, true)
               .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
               // .option(ChannelOption.IP_TOS, 1)
               .option(ChannelOption.MAX_MESSAGES_PER_READ, 16)
               // .option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT)
               // .option(ChannelOption.RCVBUF_ALLOCATOR, AdaptiveRecvByteBufAllocator.DEFAULT)
               .option(ChannelOption.SO_KEEPALIVE, true)
               .option(ChannelOption.SO_LINGER, 120)
               // .option(ChannelOption.SO_RCVBUF, 32 * 1024)
               // .option(ChannelOption.SO_SNDBUF, 32 * 1024)
               // .option(ChannelOption.SO_TIMEOUT, 120000)
               .option(ChannelOption.TCP_NODELAY, true)
               // .option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 64 * 1024)
               // .option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 32 * 1024)
               // .option(ChannelOption.WRITE_SPIN_COUNT, 16)
               ;
      eventLoopGroup.submit(new Runnable() {
         @Override
         public void run() {
            ChannelFuture future = bootstrap.connect(host, port);
            future.addListener(new SslChannelFutureListener() {
               @Override
               protected void onConnected(Channel channel) {
                  Client.this.onConnected(channel);
               }
               
               @Override
               protected void onConnectError(Throwable cause) {
                  Client.this.onConnectError(cause);
               }
            });
         }
      });
   }

   private void reconnect() {
      WebsocketStateHandler handler = this.websocketStateHandler;
      if(handler == null) {
         return;
      }
      
      if(websocket == null || this.stateRef.get() == State.CLOSED) {
         logger.debug("Ignoring reconnect because client is closed");
         return;
      }
      else if (retries.get() >= websocket.getRetryAttempts()) {
         logger.info("Closing socket because the maximum number of retries has been exceeded");
         disconnect(CloseCause.RETRIES_EXCEEDED);
      }
      else {
         logger.info("Connection lost, will re-connect");
         this.stateRef.set(State.DISCONNECTED);
         handler.onDisconnected();
         eventLoopGroup.schedule(new Runnable() {
   
            @Override
            public void run() {
               connectWebsocket();
               retries.incrementAndGet();
            }
            
         }, websocket.getRetryDelay(), TimeUnit.SECONDS);
      }
   }
   
   private void disconnect(CloseCause cause) {
      if(this.stateRef.getAndSet(State.CLOSED) != State.CLOSED) {
         logger.debug("Websocket closed, will not re-connect cause: [{}]", cause);
      }
      
      Channel channel = this.channelRef.getAndSet(null);
      if (channel != null) {
         websocketStateHandler.onClosed(cause);
         channel.writeAndFlush(new CloseWebSocketFrame());
         channel = null;
         websocket = null;
      }
   }
   
   protected void onConnected(Channel channel) {
      this.retries.set(0);
      this.stateRef.set(State.CONNECTED);
      this.channelRef.set(channel);
      this.websocketStateHandler.onConnected();
   }
   
   protected void onConnectError(Throwable cause) {
      logger.debug("connection failed. Attempting reconnect.", cause);
      reconnect();
   }
   
   interface LostHttpConnectionHandler {
      void connectionLost(URI uri);
   }
   
   private class WebsocketClientHandshaker extends WebSocketClientHandshaker13 {

      public WebsocketClientHandshaker(URI webSocketURL, HttpHeaders customHeaders, int maxFramePayloadLength) {
         super(webSocketURL, WebSocketVersion.V13, null, false, customHeaders, maxFramePayloadLength);
      }

      /* (non-Javadoc)
       * @see io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker13#verify(io.netty.handler.codec.http.FullHttpResponse)
       */
      @Override
      protected void verify(FullHttpResponse response) {
         if(
               response.getStatus().equals(HttpResponseStatus.UNAUTHORIZED) ||
               response.getStatus().equals(HttpResponseStatus.FORBIDDEN)
         ) {
            // don't retry unauth'd
            // TODO should we not retry any 4xx versions?
            disconnect(CloseCause.SESSION_EXPIRED);
         }
         // TODO follow redirects here
         super.verify(response);
      }
      
      
   }

   private class WebsocketClient extends WebSocketClientProtocolHandler {
      public WebsocketClient(WebsocketClientHandshaker handshaker) {
         super(handshaker, true);
      }

      @Override
      protected void decode(ChannelHandlerContext ctx, WebSocketFrame frame, List<Object> out) throws Exception {
         if (frame instanceof CloseWebSocketFrame) {
            CloseWebSocketFrame closeFrame = (CloseWebSocketFrame)frame;
            if(STATUS_EXPIRED_STATUS == closeFrame.statusCode()) {
               Client.this.disconnect(CloseCause.SESSION_EXPIRED);
            }
            else {
               reconnect();
            }
         }
         super.decode(ctx, frame, out);
      }

      @Override
      public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
          ctx.close();
          websocketStateHandler.onException(cause);
          logger.trace("Received Exception In Websocket Client.", cause);
      }

      @Override
      public void channelInactive(ChannelHandlerContext ctx) throws Exception {
         logger.info("Channel closed");
         // get rid of the stale channel reference
         Client.this.channelRef.set(null);
         reconnect();
      }
   }
   
   private enum State {
      CONNECTING,
      CONNECTED,
      DISCONNECTED,
      CLOSED
   }
}

