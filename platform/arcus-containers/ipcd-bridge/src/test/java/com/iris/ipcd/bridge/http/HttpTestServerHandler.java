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
package com.iris.ipcd.bridge.http;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Charsets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import io.netty.util.CharsetUtil;
import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

public class HttpTestServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
   public static final String V1_DEVICE_ID = "AD-47-12-35-52-71-89-12";
   public static final String DEVICE_AUTHORIZED = "DEVICE_AUTHORIZED";
         
   public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
   public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
   public static final int HTTP_CACHE_SECONDS = 60;

   @Override
   public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
       if (!request.getDecoderResult().isSuccess()) {
           System.out.println("### REQUEST REJECTED");
           sendError(ctx, BAD_REQUEST);
           return;
       }
       
       dumpRequest(request);

       FullHttpResponse response;
       if (request.getMethod() == POST && request.getUri().endsWith("/v1/devices")) {
          String contentDump = formatContent(request);
          System.out.println("### REQUEST CONTENT");
          System.out.println(contentDump);
          Map<String,String> attribs = getAttribs(request);
          if (attribs != null
                && !StringUtils.isEmpty(attribs.get("deviceText"))
                && !StringUtils.isEmpty(attribs.get("url"))
                && !StringUtils.isEmpty(attribs.get("deviceType"))) {
             
             response = authorized(request, V1_DEVICE_ID);
          }
          else {
             sendError(ctx, BAD_REQUEST);
             return;
          }
       }
       else if (request.getMethod() == GET || request.getMethod() == POST) {   
           String contentDump = formatContent(request);
           System.out.println("### REQUEST CONTENT");
           System.out.println(contentDump);
           response = response(request, wrapRequest(request, contentDump), OK);
       }
       else {
          sendError(ctx, METHOD_NOT_ALLOWED);
          return;
       }

       // Write the initial line and the header.
       ctx.writeAndFlush(response);

       // Decide whether to close the connection or not.
       if (!HttpHeaders.isKeepAlive(request)) {
           ctx.close();
       }
   }
   
   private static String wrapRequest(FullHttpRequest request, String content) {
      StringBuffer bf = new StringBuffer("<html>\n<head>\n<title>Test Server</title>\n</head>\n");
      bf.append("<body>\n");
      bf.append("<pre>\n");
      bf.append(request.toString());
      bf.append("\n");
      bf.append(content);
      bf.append("\n");
      bf.append("</pre>\n");
      bf.append("</body>\n");
      bf.append("</html>\n");
      return bf.toString();
   }
   
   private static Map<String,String> getAttribs(FullHttpRequest request) {
      String header = HttpHeaders.getHeader(request, HttpHeaders.Names.CONTENT_TYPE);
      if (HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED.equalsIgnoreCase(header)) {
         Map<String,String> attribs = new HashMap<>();
         HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(new DefaultHttpDataFactory(false), request);
         List<InterfaceHttpData> data = decoder.getBodyHttpDatas();
         
         if (data != null) {
            for (InterfaceHttpData datum : data) {
               if (datum.getHttpDataType() == HttpDataType.Attribute) {
                  Attribute attribute = (Attribute)datum;                
                  try {
                     attribs.put(attribute.getName(), attribute.getString());
                  } catch (IOException e) {
                     e.printStackTrace();
                  }
               }
            }
            return attribs;
         }
      }
      return null;
   }
   
   private static String formatContent(FullHttpRequest request) {
      String header = HttpHeaders.getHeader(request, HttpHeaders.Names.CONTENT_TYPE);
      StringBuffer bf = new StringBuffer();
      
      if (HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED.equalsIgnoreCase(header)) {
         HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(new DefaultHttpDataFactory(false), request);
         List<InterfaceHttpData> data = decoder.getBodyHttpDatas();
         
         if (data != null) {
            for (InterfaceHttpData datum : data) {
               if (datum.getHttpDataType() == HttpDataType.Attribute) {
                  Attribute attribute = (Attribute)datum;
                
                  try {
                     bf.append(attribute.getName()).append(" -> ").append(attribute.getString()).append("\n");
                  } catch (IOException e) {
                     e.printStackTrace();
                  }
               }
            }
         }
         else {
            bf.append("[No Data]\n");
         }
      }
      else if ("application/json".equalsIgnoreCase(header)) {
         ByteBuf byteBuf = request.content();
         byte[] bytes = new byte[byteBuf.readableBytes()];
         byteBuf.readBytes(bytes);
         String s = new String(bytes, StandardCharsets.UTF_8);
         bf.append(s);
      }
      else {
         bf.append("[Unknown Data Type ").append(header).append("]");
      }
      
      return bf.toString();
   }
   
   private static void dumpRequest(FullHttpRequest request) {
      StringBuffer bf = new StringBuffer("#### REQUEST ####\n");
      bf.append(request.toString()).append("\n");
      System.out.println(bf.toString());
   }
   
   private static FullHttpResponse response(FullHttpRequest request, String data, HttpResponseStatus status) {
      return response(request, data, "text/html", status);
   }
   
   private static FullHttpResponse response(FullHttpRequest request, String data, String contentType, HttpResponseStatus status) {
      ByteBuf content = Unpooled.copiedBuffer(data, Charsets.US_ASCII);
      FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status, content);
      HttpHeaders.setContentLength(response, content.readableBytes());
      response.headers().set(CONTENT_TYPE, contentType);
      setDateHeader(response);
      if (HttpHeaders.isKeepAlive(request)) {
         response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
      }
      return response;
   }
   
   private static FullHttpResponse authorized(FullHttpRequest request, String v1DeviceId) {
      StringBuffer sb = new StringBuffer("{")
                              .append("{\"success\":")
                              .append("true,")
                              .append("\"status\":")
                              .append('"').append(DEVICE_AUTHORIZED).append("\",")
                              .append("\"deviceId\":")
                              .append('"').append(v1DeviceId).append('"')
                              .append('}');
      return response(request, sb.toString(), "application/json", CREATED);
   }

   @Override
   public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
       cause.printStackTrace();
       if (ctx.channel().isActive()) {
           sendError(ctx, INTERNAL_SERVER_ERROR);
       }
   }

   private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
       FullHttpResponse response = new DefaultFullHttpResponse(
               HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8));
       response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");

       // Close the connection as soon as the error message is sent.
       ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
   }

   /**
    * Sets the Date header for the HTTP response
    *
    * @param response
    *            HTTP response
    */
   private static void setDateHeader(FullHttpResponse response) {
       SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
       dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

       Calendar time = new GregorianCalendar();
       response.headers().set(DATE, dateFormatter.format(time.getTime()));
   }
}

