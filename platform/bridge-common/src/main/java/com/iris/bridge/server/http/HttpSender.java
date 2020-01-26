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
package com.iris.bridge.server.http;

import static io.netty.handler.codec.http.HttpHeaderNames.CACHE_CONTROL;
import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderNames.DATE;
import static io.netty.handler.codec.http.HttpHeaderNames.EXPIRES;
import static io.netty.handler.codec.http.HttpHeaderNames.LAST_MODIFIED;
import static io.netty.handler.codec.http.HttpHeaderNames.LOCATION;
import static io.netty.handler.codec.http.HttpResponseStatus.FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.bridge.metrics.BridgeMetrics;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;
import io.netty.handler.stream.ChunkedStream;
import io.netty.util.CharsetUtil;

public class HttpSender {
   private static final Logger logger = LoggerFactory.getLogger(HttpSender.class);

   public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
   public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
   public static final int HTTP_CACHE_SECONDS = 60;

   public static final int STATUS_NOT_MODIFIED = HttpResponseStatus.NOT_MODIFIED.code();
   public static final int STATUS_FORBIDDEN = HttpResponseStatus.FORBIDDEN.code();
   public static final int STATUS_NOT_FOUND = HttpResponseStatus.NOT_FOUND.code();
   public static final int STATUS_SERVER_ERROR = HttpResponseStatus.INTERNAL_SERVER_ERROR.code();
   public static final int STATUS_BAD_REQUEST = HttpResponseStatus.BAD_REQUEST.code();
   public static final int STATUS_FOUND = HttpResponseStatus.FOUND.code();

   private final BridgeMetrics metrics;
   private final String className;

   public HttpSender(Class<?> clazz, BridgeMetrics metrics) {
      this.metrics = metrics;
      this.className = clazz.getName();
   }

   /**
    * Sets the Date and Cache headers for the HTTP Response
    *
    * @param response
    *            HTTP response
    * @param fileToCache
    *            file to extract content type
    */
   public void setDateAndCacheHeaders(HttpResponse response, File fileToCache) {
      SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
      dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

      // Date header
      Calendar time = new GregorianCalendar();
      response.headers().set(DATE, dateFormatter.format(time.getTime()));

      // Add cache headers
      time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
      response.headers().set(EXPIRES, dateFormatter.format(time.getTime()));
      response.headers().set(CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
      response.headers().set(
            LAST_MODIFIED, dateFormatter.format(new Date(fileToCache.lastModified())));
   }

   /**
    * Sets the Date header for the HTTP response
    *
    * @param response
    *            HTTP response
    */
   public void setDateHeader(FullHttpResponse response) {
      SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
      dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

      Calendar time = new GregorianCalendar();
      response.headers().set(DATE, dateFormatter.format(time.getTime()));
   }

   public void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
	   if (HttpUtil.isTransferEncodingChunked(res)){
		   sendChunkedHttpResponse(ctx,req,res);
	   } else {
		   sendHttpResponseFull(ctx,req,res);
	   }
   }

   public void sendHttpResponseFull(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
      HttpUtil.setContentLength(res, res.content().readableBytes());
      ChannelFuture f1 = ctx.channel().writeAndFlush(res);
      if (!HttpUtil.isKeepAlive(req) || res.status().code() != 200) {
         f1.addListener(ChannelFutureListener.CLOSE);
      }
      metrics.incHTTPResponseCounter(className, res.status().code());
   }

	public void sendChunkedHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
		
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        HttpUtil.setTransferEncodingChunked(response, true);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, res.headers().get(HttpHeaderNames.CONTENT_TYPE));

        if(HttpUtil.isKeepAlive(req)) {
           response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }

        ctx.write(response);
        ctx.write(new ChunkedStream(new ByteBufInputStream(res.content())));
        ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        if(!HttpUtil.isKeepAlive(req)) {
           future.addListener(ChannelFutureListener.CLOSE);
        }		
        metrics.incHTTPResponseCounter(className, res.status().code());
	}
	
   public void sendRedirect(ChannelHandlerContext ctx, String newUri, FullHttpRequest req) {
      FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, FOUND);
      response.headers().set(LOCATION, newUri);
      metrics.incHTTPResponseCounter(className, HttpSender.STATUS_FOUND);
      // Close the connection as soon as the redirect message is sent.
      ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
   }

   public void sendError(ChannelHandlerContext ctx, int code, FullHttpRequest req) {
      HttpResponseStatus status = HttpResponseStatus.valueOf(code);
      metrics.incHTTPResponseCounter(className, code);
      FullHttpResponse response = new DefaultFullHttpResponse(
            HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8));
      response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");

      // Close the connection as soon as the error message is sent.
      ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
   }

   public void sendNotModified(ChannelHandlerContext ctx, FullHttpRequest req) {
      FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, NOT_MODIFIED);
      setDateHeader(response);
      HttpUtil.setContentLength(response, 0);

      // if keepalive is set, don't close the connection. The zero content length header will tell the client
      // that we're done
      if (HttpUtil.isKeepAlive(req)) {
         ctx.writeAndFlush(response);
         return;
      }

      // If keepalive is not set, close the connection as soon as the message is sent.
      ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
   }

   public void sendFile(final File file, Map<String, String> headers, boolean keepAlive, ChannelHandlerContext ctx) throws Exception {
      RandomAccessFile raf;
      try {
         raf = new RandomAccessFile(file, "r");
      } catch (FileNotFoundException ignore) {
         throw new HttpException(HttpResponseStatus.NOT_FOUND.code());
      }

      long fileLength = raf.length();

      HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
      HttpUtil.setContentLength(response, fileLength);
      
      headers.keySet().stream().forEach((k) -> response.headers().set(k, headers.get(k)));
      setDateAndCacheHeaders(response, file);
      if (keepAlive) {
         response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
      }

      // Write the initial line and the header.
      ctx.write(response);

      // Write the content.
      ChannelFuture sendFileFuture;
      ChannelFuture lastContentFuture;
      if (ctx.pipeline().get(SslHandler.class) == null) {
         sendFileFuture = ctx.write(new DefaultFileRegion(raf.getChannel(), 0, fileLength), ctx.newProgressivePromise());
         // Write the end marker.
         lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
      } else {
         sendFileFuture = ctx.writeAndFlush(new HttpChunkedInput(new ChunkedFile(raf, 0, fileLength, 8192)),
                         ctx.newProgressivePromise());
         // HttpChunkedInput will write the end marker (LastHttpContent) for us.
         lastContentFuture = sendFileFuture;
      }

      sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
         @Override
         public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
            if (total < 0) { // total unknown
               logger.debug("{} - {} transfer progress: {}", future.channel(), file, progress);
            } else {
               logger.debug("{} - {} transfer progress: {} / {}", future.channel(), file, progress, total);
            }
         }

         @Override
         public void operationComplete(ChannelProgressiveFuture future) {
            logger.debug("{} - {} transfer complete.", future.channel(), file);
         }
      });

      // Decide whether to close the connection or not.
      if (!keepAlive) {
         // Close the connection when the whole content is written out.
         lastContentFuture.addListener(ChannelFutureListener.CLOSE);
      }
   }
}

