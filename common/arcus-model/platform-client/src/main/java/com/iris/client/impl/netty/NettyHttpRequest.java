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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.ClientCookieEncoder;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

public class NettyHttpRequest {
   private final URI uri;
   private final ResponseHandler handler;
   private final FullHttpRequest request;
   
   protected NettyHttpRequest(URI uri, 
         ResponseHandler handler,
         HttpMethod method,
         String json,
         Map<String, String> formParams,
         List<Map.Entry<String, Object>> headers, 
         Map<String, String> cookies) {
      this.uri = uri;
      this.handler = handler;
      
      String url = uri.getRawPath();
      if (method == HttpMethod.GET && formParams != null && formParams.size() > 0) {
         StringBuffer sb = new StringBuffer(url);
         char prefixChar = '?';
         for (String name : formParams.keySet()) {
            sb.append(prefixChar);
            if (prefixChar == '?') {
               prefixChar = '&';
            }
            try {
               sb.append(URLEncoder.encode(name, Charset.forName("UTF-8").name()))
                  .append('=')
                  .append(URLEncoder.encode(formParams.get(name), Charset.forName("UTF-8").name()));
            }
            catch (UnsupportedEncodingException e) {
               throw new RuntimeException("UTF-8 not supported for url encoding", e);
            }
         }
         url = sb.toString();
      }
      
      request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, url);
      request.setMethod(method);
      
      // Some Default Headers
      request.headers().set(HttpHeaders.Names.HOST, uri.getHost());
      request.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
      request.headers().set(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP);
      
      addHeaders(headers);
      
      addCookies(cookies);
      
      if (method == HttpMethod.POST) {
         if (json != null && !json.isEmpty()) {
            jsonPayload(json);
         }
         else {
            formPayload(formParams);
         }
      }
   }
   
   public HttpRequest getHttpRequest() {
      return request;
   }

   public ResponseHandler getResponseHandler() {
      return handler;
   }

   public URI getUri() {
      return uri;
   }
   
   private void formPayload(Map<String, String> formParams) {
      if (formParams != null && formParams.size() > 0) {
         StringBuffer sb = new StringBuffer();
         for (String name : formParams.keySet()) {
            if (sb.length() > 0) {
               sb.append('&');
            }
            try {
               sb.append(URLEncoder.encode(name, Charset.forName("UTF-8").name()));
               sb.append('=');
               sb.append(URLEncoder.encode(formParams.get(name), Charset.forName("UTF-8").name()));
            } catch (UnsupportedEncodingException e) {
               throw new RuntimeException("UTF-8 not supported for url encoding", e);
            }
         }
         request.headers().set(HttpHeaders.Names.CONTENT_TYPE, HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED);
         setContent(sb.toString());
      }
   }
   
   private void jsonPayload(String json) {
      request.headers().add(HttpHeaders.Names.CONTENT_TYPE, "application/json");
      setContent(json);
   }
   
   private void setContent(String content) {
      ByteBuf buf = Unpooled.copiedBuffer(content, Charset.forName("UTF-8"));
      request.headers().set(HttpHeaders.Names.CONTENT_LENGTH, buf.readableBytes());
      request.content().clear().writeBytes(buf);
   }
   
   private void addHeaders(List<Map.Entry<String, Object>> headers) {
      if (headers != null) {
         for (Map.Entry<String, Object> header : headers) {
            request.headers().add(header.getKey(), header.getValue());
         }
      }
   }
   
   private void addCookies(Map<String, String> cookies) {
      if (cookies != null && cookies.size() > 0) {
         List<Cookie> cookieList = new ArrayList<>();
         for (String name : cookies.keySet()) {
            cookieList.add(new DefaultCookie(name, cookies.get(name)));
         }
         request.headers().set(HttpHeaders.Names.COOKIE, ClientCookieEncoder.encode(cookieList));
      }
   }
   
   public static Builder builder() {
      return new Builder();
   }
   
   public static class Builder {
      private final static Logger logger = LoggerFactory.getLogger(NettyHttpRequest.Builder.class);
      private URI uri = null; 
      private ResponseHandler handler = null;
      private HttpMethod method = HttpMethod.GET;
      private String json = null;
      private Map<String, String> formParams = null;
      private List<Map.Entry<String, Object>> headers = null;
      private Map<String, String> cookies = null;
      
      protected Builder() {};
      
      public Builder get() {
         method = HttpMethod.GET;
         return this;
      }
      
      public Builder post() {
         method = HttpMethod.POST;
         return this;
      }
      
      public Builder uri(String uri) {
         try {
            this.uri = new URI(uri);
         } catch (URISyntaxException e) {
            logger.error("Attempting to make request to invalid URI [{}]", uri);
            throw new RuntimeException("Invalid URI for http request", e);
         }
         return this;
      }
      
      public Builder uri(URI uri) {
         this.uri = uri;
         return this;
      }
      
      public Builder setHandler(ResponseHandler handler) {
         this.handler = handler;
         return this;
      }
      
      public Builder setJson(String json) {
         this.json = json;
         return this;
      }
      
      public Builder addFormParam(String name, String value) {
         if (formParams == null) {
            formParams = new HashMap<>();
         }
         formParams.put(name, value);
         return this;
      }
      
      public Builder addHeader(String name, Object value) {
         if (headers == null) {
            headers = new ArrayList<>();
         }
         Map.Entry<String, Object> headerEntry = Maps.immutableEntry(name, value);
         headers.add(headerEntry);
         return this;
      }
      
      public Builder addCookie(String name, String value) {
         if (cookies == null) {
            cookies = new HashMap<>();
         }
         cookies.put(name,  value);
         return this;
      }
      
      public NettyHttpRequest build() {
         return new NettyHttpRequest(uri, handler, method, json, formParams, headers, cookies);
      }
   }
}

