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
package com.iris.bridge.server.http.handlers;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.impl.HttpResource;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;
import com.iris.core.template.TemplateService;

public abstract class TemplatedHttpHandler extends HttpResource {

   private static final Logger LOGGER = LoggerFactory.getLogger(TemplatedHttpHandler.class);

   protected TemplateService templateService;

   public TemplatedHttpHandler(AlwaysAllow alwaysAllow, HttpSender httpSender, TemplateService templateService) {
      super(alwaysAllow, httpSender);
      this.templateService = templateService;
   }

   @Override
   public FullHttpResponse respond(FullHttpRequest req, ChannelHandlerContext ctx) throws Exception {
      HttpResponseStatus responseStatus = HttpResponseStatus.OK;
      Optional<String> response = Optional.empty();

      try {
         TemplatedResponse handlerResponse = doHandle(req, ctx);
         if (handlerResponse.sectionId.isPresent()) {
            Map<String, String> multiResponse = templateService.renderMultipart(handlerResponse.templateId.get(), handlerResponse.context);
            response = Optional.of(multiResponse.get(handlerResponse.sectionId.get()));
         } else if (handlerResponse.templateId.isPresent()){
            response = Optional.of(templateService.render(handlerResponse.templateId.get(), handlerResponse.context));
         }

         if(!response.isPresent()){
            responseStatus=HttpResponseStatus.NO_CONTENT;
         }

         if(handlerResponse.responseStatus.isPresent()){
            responseStatus=handlerResponse.responseStatus.get();
         }
         

      } catch (Throwable th) {
         LOGGER.error("Error responding to TemplatedRequestHandler", th);
         responseStatus = HttpResponseStatus.INTERNAL_SERVER_ERROR;
      }
      
      FullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, responseStatus, Unpooled.copiedBuffer(response.orElse("").getBytes()));
      httpResponse.headers().set(HttpHeaders.Names.CONTENT_TYPE, getContentType());
      return httpResponse;
   }

   public abstract TemplatedResponse doHandle(FullHttpRequest request, ChannelHandlerContext ctx) throws Exception;

   public abstract String getContentType();
 
   protected TemplatedResponse createTemplateResponse(String templateId, Object context, String sectionId) {
      TemplatedResponse response = new TemplatedResponse().withTemplateId(templateId).withSectionId(sectionId).withContext(context);
      return response;
   }

   protected TemplatedResponse createTemplateResponse(String templateId, Object context) {
      return createTemplateResponse(templateId, context, null);
   }
   
   protected TemplatedResponse createTemplateResponse(HttpResponseStatus responseStatus) {
      return new TemplatedResponse().withResponseStatus(responseStatus);
   }

   public class TemplatedResponse {
      private Optional<String> templateId=Optional.empty();
      private Optional<String> sectionId=Optional.empty();
      private Object context;
      private Optional<HttpResponseStatus> responseStatus = Optional.empty();

      public TemplatedResponse withTemplateId(String templateId) {
         this.templateId = Optional.ofNullable(templateId);
         return this;
      }

      public TemplatedResponse withSectionId(String sectionId) {
         this.sectionId = Optional.ofNullable(sectionId);
         return this;
      }

      public Optional<String> getTemplateId() {
         return templateId;
      }

      public Optional<String> getSectionId() {
         return sectionId;
      }

      public Object getContext() {
         return context;
      }

      public Optional<HttpResponseStatus> getResponseStatus() {
         return responseStatus;
      }

      public TemplatedResponse withContext(Object context) {
         this.context = context;
         return this;
      }
      
      public TemplatedResponse withResponseStatus(HttpResponseStatus responseStatus ) {
         this.responseStatus = Optional.ofNullable(responseStatus);
         return this;
      }

   }
}

