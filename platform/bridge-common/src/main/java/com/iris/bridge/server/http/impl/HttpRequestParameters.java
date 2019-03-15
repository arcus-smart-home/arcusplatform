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
package com.iris.bridge.server.http.impl;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.bridge.server.http.HttpException;

public class HttpRequestParameters {
   private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequestParameters.class);

   private FullHttpRequest request;
   private Map<String, List<String>> parameters;

   public HttpRequestParameters(FullHttpRequest request) {
      this.request = request;
      parameters = parseParameters();
   }

   public Map<String, List<String>> getParameters() {
      if (parameters == null) {
         parameters = parseParameters();
      }
      return parameters;
   }

   private Map<String, List<String>> parseParameters() {
      if (request.getMethod().equals(HttpMethod.GET)) {
         return new QueryStringDecoder(request.getUri()).parameters();
      }
      else if (request.getMethod().equals(HttpMethod.POST)) {
         return parsePostFormParameters(request);
      }
      return new HashMap<String, List<String>>();
   }

   private Map<String, List<String>> parsePostFormParameters(FullHttpRequest request) {
      HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(new DefaultHttpDataFactory(false), request);
      Map<String, List<String>> attributes = new HashMap<String, List<String>>();
      List<InterfaceHttpData> datas = decoder.getBodyHttpDatas();
      for (InterfaceHttpData data : datas) {
         if (data.getHttpDataType() == HttpDataType.Attribute) {
            try {
               String name = data.getName();
               String value = ((Attribute) data).getString();
               attributes.putIfAbsent(name, new ArrayList<String>());
               attributes.get(name).add(value);
            } catch (IOException e) {
               LOGGER.error("Error getting HTTP attribute from POST request");
            }
         }
      }
      decoder.destroy();
      return attributes;
   }

   public String getParameter(String name) {
      return parameters.getOrDefault(name, Collections.emptyList())
            .stream()
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(String.format("Required parameter with name %s not found in request", name)));
   }
   
   public String getParameter(HttpResponseStatus statusOnError, String name) throws HttpException {
      return parameters.getOrDefault(name, Collections.emptyList())
            .stream()
            .findFirst()
            .orElseThrow(() ->  new HttpException(statusOnError));
   }
   
   public Optional<String> getOptionalParameter(String name) {
      return Optional.ofNullable(getParameter(name,null)); 
   }
   
   public long getParameterLong(String name) {
      String param=getParameter(name);
      return Long.parseLong(param);
   }   

   public String getParameter(String name, String defaultValue) {
      return parameters.getOrDefault(name, Collections.emptyList())
            .stream()
            .findFirst()
            .orElse(defaultValue);
   }

}

