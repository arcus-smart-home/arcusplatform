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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;

/**
 * Tests that the platform-client Netty pipelines correctly decompress
 * gzip-encoded HTTP responses. This verifies the fix for
 * https://github.com/arcus-smart-home/arcusplatform/issues/94
 */
public class TestGzipDecompression {

   private static final String TEST_BODY = "{\"type\":\"SessionCreatedEvent\",\"attributes\":{\"status\":\"ok\"}}";

   private NioEventLoopGroup serverGroup;
   private Channel serverChannel;
   private int serverPort;

   @Before
   public void setUp() throws Exception {
      serverGroup = new NioEventLoopGroup(1);
   }

   @After
   public void tearDown() throws Exception {
      if (serverChannel != null) {
         serverChannel.close().sync();
      }
      serverGroup.shutdownGracefully().sync();
   }

   @Test
   public void testHttpRequesterDecompressesGzipResponse() throws Exception {
      startServer(true);
      ResponseCapture capture = executeRequest(65536);

      assertNull("Unexpected error: " + capture.error, capture.error);
      assertEquals(200, capture.statusCode);
      assertEquals(TEST_BODY, capture.bodyText);
   }

   @Test
   public void testHttpRequesterHandlesUncompressedResponse() throws Exception {
      startServer(false);
      ResponseCapture capture = executeRequest(65536);

      assertNull("Unexpected error: " + capture.error, capture.error);
      assertEquals(200, capture.statusCode);
      assertEquals(TEST_BODY, capture.bodyText);
   }

   @Test
   public void testHttpRequesterDecompressesLargeGzipResponse() throws Exception {
      StringBuilder sb = new StringBuilder("{\"items\":[");
      for (int i = 0; i < 500; i++) {
         if (i > 0) sb.append(",");
         sb.append("{\"id\":").append(i)
           .append(",\"name\":\"device-").append(i)
           .append("\",\"type\":\"switch\",\"state\":\"OFF\"}");
      }
      sb.append("]}");
      String largeBody = sb.toString();

      startServer(true, largeBody);
      ResponseCapture capture = executeRequest(1048576);

      assertNull("Unexpected error: " + capture.error, capture.error);
      assertEquals(200, capture.statusCode);
      assertEquals(largeBody, capture.bodyText);
   }

   @Test
   public void testRequestAdvertisesGzipAcceptEncoding() throws Exception {
      NettyHttpRequest request = NettyHttpRequest.builder()
            .get()
            .uri("http://127.0.0.1:8080/test")
            .setHandler(new ResponseHandler() {
               @Override
               public void onCompleted(NettyHttpResponse response) {}
               @Override
               public void onThrowable(Throwable throwable) {}
            })
            .build();

      String acceptEncoding = ((FullHttpRequest) request.getHttpRequest())
            .headers().get(HttpHeaders.Names.ACCEPT_ENCODING);
      assertEquals(HttpHeaders.Values.GZIP, acceptEncoding);
   }

   /**
    * Captures response data eagerly inside the Netty handler callback,
    * before the ByteBuf is released by the pipeline.
    */
   private static class ResponseCapture {
      volatile String bodyText;
      volatile int statusCode;
      volatile Throwable error;
   }

   private ResponseCapture executeRequest(int maxResponseSize) throws Exception {
      CountDownLatch latch = new CountDownLatch(1);
      ResponseCapture capture = new ResponseCapture();

      HttpRequester requester = new HttpRequester(
            new URI("http://127.0.0.1:" + serverPort),
            null,
            new Client.LostHttpConnectionHandler() {
               @Override
               public void connectionLost(URI uri) {}
            },
            0,
            1,
            maxResponseSize
      );

      try {
         NettyHttpRequest request = NettyHttpRequest.builder()
               .get()
               .uri("http://127.0.0.1:" + serverPort + "/test")
               .setHandler(new ResponseHandler() {
                  @Override
                  public void onCompleted(NettyHttpResponse response) {
                     // Read eagerly before ByteBuf is released
                     capture.statusCode = response.getStatusCode();
                     capture.bodyText = response.getBodyAsText();
                     latch.countDown();
                  }

                  @Override
                  public void onThrowable(Throwable throwable) {
                     capture.error = throwable;
                     latch.countDown();
                  }
               })
               .build();

         requester.execute(request);
         assertTrue("Timed out waiting for response", latch.await(10, TimeUnit.SECONDS));
      } finally {
         requester.shutdown();
      }

      return capture;
   }

   private void startServer(boolean gzip) throws Exception {
      startServer(gzip, TEST_BODY);
   }

   private void startServer(boolean gzip, String body) throws Exception {
      ServerBootstrap bootstrap = new ServerBootstrap();
      bootstrap.group(serverGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<SocketChannel>() {
               @Override
               protected void initChannel(SocketChannel ch) {
                  ChannelPipeline p = ch.pipeline();
                  p.addLast(new HttpServerCodec());
                  p.addLast(new HttpObjectAggregator(1048576));
                  p.addLast(new GzipTestServerHandler(gzip, body));
               }
            });

      serverChannel = bootstrap.bind(0).sync().channel();
      serverPort = ((java.net.InetSocketAddress) serverChannel.localAddress()).getPort();
   }

   private static class GzipTestServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
      private final boolean gzip;
      private final String body;

      GzipTestServerHandler(boolean gzip, String body) {
         this.gzip = gzip;
         this.body = body;
      }

      @Override
      protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
         byte[] bodyBytes = body.getBytes("UTF-8");

         FullHttpResponse response;
         if (gzip) {
            byte[] compressed = gzipCompress(bodyBytes);
            response = new DefaultFullHttpResponse(
                  HttpVersion.HTTP_1_1,
                  HttpResponseStatus.OK,
                  Unpooled.wrappedBuffer(compressed));
            response.headers().set(HttpHeaders.Names.CONTENT_ENCODING, HttpHeaders.Values.GZIP);
            response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, compressed.length);
         } else {
            response = new DefaultFullHttpResponse(
                  HttpVersion.HTTP_1_1,
                  HttpResponseStatus.OK,
                  Unpooled.wrappedBuffer(bodyBytes));
            response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, bodyBytes.length);
         }

         response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "application/json");
         ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
      }

      @Override
      public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
         ctx.close();
      }
   }

   private static byte[] gzipCompress(byte[] data) throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
         gzip.write(data);
      }
      return baos.toByteArray();
   }
}
