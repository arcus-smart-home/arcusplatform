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
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.ssl.JdkSslClientContext;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import java.net.URI;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class HttpRequester {
   private final static Logger logger = LoggerFactory.getLogger(HttpRequester.class);
   private final TrustManagerFactory trustManagerFactory;
   private final ConcurrentLinkedQueue<NettyHttpRequest> sentRequestQueue = new ConcurrentLinkedQueue<>();
   private final ConcurrentLinkedQueue<NettyHttpRequest> pendingRequestQueue = new ConcurrentLinkedQueue<>();
   private final URI uri;
   private Channel channel = null;
   private final EventLoopGroup group;
   private final Client.LostHttpConnectionHandler lostHandler;
   private final int retryAttempts;
   private final int retryDelay;
   private final int maxResponseSize;
   private int retries = 0;
   private final int IDLE_TIMEOUT_SECONDS = 30;
   private volatile boolean isConnecting = false;
   
   HttpRequester(
         URI uri, 
         TrustManagerFactory trustManagerFactory, 
         Client.LostHttpConnectionHandler lostHandler, 
         int retryAttempts, 
         int retryDelay,
         int maxResponseSize
   ) throws IllegalArgumentException {
      if(uri == null) {
         throw new IllegalArgumentException("Cannot pass a null URI to HttpRequester");
      }

      this.uri = uri;
      this.trustManagerFactory = trustManagerFactory;
      this.lostHandler = lostHandler;
      this.retryAttempts = retryAttempts;
      this.retryDelay = retryDelay;
      this.maxResponseSize = maxResponseSize;
      group = new NioEventLoopGroup();
   }
   
   public void execute(NettyHttpRequest request) {
      if (isConnecting) {
         pendingRequestQueue.add(request);
      }
      else if (channel == null) {
         pendingRequestQueue.add(request);
         connect();
      }
      else {
         sentRequestQueue.add(request);
         channel.writeAndFlush(request.getHttpRequest());
      }
   }
   
   public void disconnect() {
      if (channel != null) {
         channel.close();
      }
      disconnected();
   }
   
   public void shutdown() {
      group.shutdownGracefully();
   }
   
   private void disconnected() {
      channel = null;
      isConnecting = false;
      while (sentRequestQueue.peek() != null) {
         sentRequestQueue.poll().getResponseHandler().onThrowable(new RuntimeException("HTTP Connection Closed Unexpectedly."));
      }
      if (pendingRequestQueue.peek() != null) {
         if (retries >= retryAttempts) {
            shutdown();
            lostHandler.connectionLost(uri);
         }
         else {
            group.schedule(new Runnable() {
               @Override
               public void run() {
                  retries++;
                  connect();
               }        
            },
            retryDelay, TimeUnit.SECONDS);
         }
      }
      else {
         shutdown();
         lostHandler.connectionLost(uri);
      }
   }

   private void connect() {
      group.execute(new Runnable() {
         public void run() {
            try {
               doConnect();
            }
            catch(Exception e) {
               sendErrorToPending(e);
            }
         }
      });
   }
   
   private void doConnect() {
      if(isConnecting || channel != null) {
         return;
      }
      
      isConnecting = true;
      final String scheme = uri.getScheme();
      final String host = uri.getHost();
      
      Preconditions.checkNotNull(scheme, "The URL scheme must not be null: " + uri);
      Preconditions.checkNotNull(host, "The URL host must not be null: " + uri);
      Preconditions.checkArgument("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme), "Only HTTP(S) is supported: " + uri);
      
      int port = uri.getPort();
      if (port == -1) {
         if ("http".equalsIgnoreCase(scheme)) {
            port = 80;
         }
         else if ("https".equalsIgnoreCase(scheme)) {
            port = 443;
         }
      }
      
      final boolean ssl = "https".equalsIgnoreCase(scheme);
      SslContext sslCtx; 
      if (ssl) {
         Preconditions.checkNotNull(trustManagerFactory, "Trust manager must be specified for SSL connection.");
         try {
            sslCtx = new JdkSslClientContext(trustManagerFactory);
         } catch (SSLException e) {
            logger.error("SSL Error building SSL context [{}]", e.getMessage());
            throw new RuntimeException("SSL Error", e);
         }
      }
      else {
         sslCtx = null;
      }
      
      Bootstrap bootstrap = new Bootstrap();
      bootstrap.group(group)
         .channel(NioSocketChannel.class)
         .handler(new HttpRequestInitializer(sslCtx, uri, port));
      
      ChannelFuture future = bootstrap.connect(host, port);
      future.addListener(new SslChannelFutureListener() {
         @Override
         protected void onConnected(Channel channel) {
            HttpRequester.this.onConnected(channel);
         }

         /* (non-Javadoc)
          * @see com.iris.client.impl.netty.SslChannelFutureListener#onConnectError(java.lang.Throwable)
          */
         @Override
         protected void onConnectError(Throwable cause) {
            HttpRequester.this.onConnectError(cause);
         }
      });
   }
   
   protected void onConnected(Channel channel) {
      try {
         Preconditions.checkNotNull(channel);
         
         this.channel = channel;
         NettyHttpRequest request = pendingRequestQueue.poll();
         while(request != null) {
             sentRequestQueue.add(request);
             channel.writeAndFlush(request.getHttpRequest());
             request = pendingRequestQueue.poll();
         }
         isConnecting = false;
         retries = 0;
     }
     catch(Exception e) {
        sendErrorToPending(e);
     }
   }
   
   protected void onConnectError(Throwable cause) {
      sendErrorToPending(cause);
   }
   
   protected void sendErrorToPending(Throwable cause) {
      NettyHttpRequest request = pendingRequestQueue.poll();
      while(request != null) {
          request.getResponseHandler().onThrowable(cause);
          request = pendingRequestQueue.poll();
      }
   }
   
   private class HttpRequestInitializer extends ChannelInitializer<SocketChannel> {
      private final SslContext sslCtx;
      private final URI uri;
      private final int port;

       HttpRequestInitializer(SslContext sslCtx, URI u, int port) {
         this.sslCtx = sslCtx;
         this.uri = u;
         this.port = port;
      }

      @Override
      protected void initChannel(SocketChannel ch) throws Exception {
         ChannelPipeline pipeline = ch.pipeline();
         if (sslCtx != null) {
             pipeline.addLast(sslCtx.newHandler(ch.alloc(), this.uri.getHost(), this.port));
         }
         pipeline.addLast(new IdleStateHandler(IDLE_TIMEOUT_SECONDS, IDLE_TIMEOUT_SECONDS, IDLE_TIMEOUT_SECONDS));
         pipeline.addLast(new HttpIdleStateHandler());
         pipeline.addLast(new HttpClientCodec());
         pipeline.addLast(new HttpObjectAggregator(maxResponseSize));
         pipeline.addLast(new HttpClientHandler());
      }
   }

   private class HttpIdleStateHandler extends ChannelDuplexHandler {
   	@Override
      public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
          if (evt instanceof IdleStateEvent) {
              IdleStateEvent e = (IdleStateEvent) evt;
              if (e.state() == IdleState.ALL_IDLE) {
            	  ctx.close();
            	  logger.debug("Closing channel. Idle for >= [{}] seconds", IDLE_TIMEOUT_SECONDS);
              }
          }
      }
   }
   
   private class HttpClientHandler extends SimpleChannelInboundHandler<HttpObject> {

      @Override
      protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
         NettyHttpRequest request = sentRequestQueue.poll();
         if (request != null) {
            if (msg instanceof FullHttpResponse) {
               request.getResponseHandler().onCompleted(new NettyHttpResponse((FullHttpResponse)msg));
            }
            else {
               request.getResponseHandler().onThrowable(new RuntimeException("Unexpected response from http server"));
            }
         }
      }

      @Override
      public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
         NettyHttpRequest request = sentRequestQueue.poll();
         if (request != null) {
            request.getResponseHandler().onThrowable(cause);
         }
      }

      @Override
      public void channelInactive(ChannelHandlerContext ctx) throws Exception {
         disconnected();
      }
      
   }
}

