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
package com.iris.client.server.rest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import com.iris.bridge.server.http.HttpException;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;

public class HttpUtil {
	public static final String DELIMITER_FOR_PARAM = ":";	
	
	
	public static String extractReqParam(QueryStringDecoder decoder, String name) throws HttpException {
      List<String> values = decoder.parameters().get(name);
      if(values == null || values.size() != 1) {
         throw new HttpException(HttpResponseStatus.BAD_REQUEST.code());
      }
      String val = values.get(0);
      if(StringUtils.isBlank(val)) {
         throw new HttpException(HttpResponseStatus.BAD_REQUEST.code());
      }
      return val;
   }
	
	public static String extractOptionalParam(QueryStringDecoder decoder, String name) throws HttpException {
   	try{
   		return extractReqParam(decoder, name);
   	}catch(HttpException e) {
   		return null;
   	}      
   }	

   @Nullable
   public static String extractFormParam(HttpPostRequestDecoder decoder, String name, boolean required) throws HttpException {
      InterfaceHttpData data = decoder.getBodyHttpData(name);
      try {
         String value = null;
         if(data != null && data.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
            Attribute attribute = (Attribute) data;
            value = attribute.getValue();
         }
         if(value == null && required) {
            throw new HttpException(HttpResponseStatus.BAD_REQUEST.code());
         }
         return value;
      } catch(IOException ioe) {
         throw new HttpException(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
      }
   }
   
   public static FullHttpResponse redirectResponse(String redirectUrl) {
   	FullHttpResponse response = createFullHttpResponse(HttpResponseStatus.TEMPORARY_REDIRECT);
      response.headers().add(HttpHeaders.Names.LOCATION, redirectUrl);
      return response;
   }
   
   public static void sendRedirectResponse(ChannelHandlerContext ctx, String redirectUrl) {
   	FullHttpResponse response = redirectResponse(redirectUrl);
      ctx.channel().writeAndFlush(response);
   }
   
   public static FullHttpResponse createFullHttpResponse(HttpResponseStatus status) {
   	return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
   }
   
   public static FullHttpResponse createSuccessFullHttpResponse() {
   	return createFullHttpResponse(HttpResponseStatus.OK);
   }
   
   public static FullHttpResponse htmlResponse(String htmlContent, Map<String, Object> headers) {
   	FullHttpResponse response = createSuccessFullHttpResponse();   	
   	if(headers != null) {
   		headers.forEach((k,v) -> response.headers().set(k, v));
   	}
   	response.content().writeBytes(htmlContent.getBytes(StandardCharsets.UTF_8));
   	return response;
   }
   
   public static void sendHtmlResponse(ChannelHandlerContext ctx, String htmlContent, Map<String, Object> headers) {
   	FullHttpResponse response = htmlResponse(htmlContent, headers);
   	sendHttpResponseFull(ctx, response);  	
   }
   
   public static void sendHtmlResponse(ChannelHandlerContext ctx, String htmlContent) {
   	sendHtmlResponse(ctx, htmlContent, null); 	
   }
   
   public static void sendHttpResponseFull(ChannelHandlerContext ctx, FullHttpResponse response) {
   	HttpHeaders.setContentLength(response, response.content().readableBytes());
      ChannelFuture f1 = ctx.channel().writeAndFlush(response);
      if ( !HttpResponseStatus.OK.equals(response.getStatus())) {
         f1.addListener(ChannelFutureListener.CLOSE);
      }
   }
   
   public static void sendErrorResponse(ChannelHandlerContext ctx) {
      FullHttpResponse res = createFullHttpResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR);
      res.headers().add(HttpHeaders.Names.CONTENT_LENGTH, 0);
      ChannelFuture f = ctx.writeAndFlush(res);
      f.addListener(ChannelFutureListener.CLOSE);
   }
}

