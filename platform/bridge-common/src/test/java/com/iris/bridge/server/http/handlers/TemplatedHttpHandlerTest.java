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

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.HashMap;
import java.util.Map;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;
import com.iris.core.template.TemplateService;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;

@Mocks({ BridgeMetrics.class, FullHttpRequest.class, ChannelHandlerContext.class,TemplateService.class })
public class TemplatedHttpHandlerTest extends IrisMockTestCase {
  
   private TemplatedHttpHandler handler;
   
   @Inject private TemplateService templateService;
   @Inject private BridgeMetrics bridgeMetrics;
   @Inject private FullHttpRequest fullHttpRequest;
   @Inject private ChannelHandlerContext ctx;
   
   private HttpSender httpSender;
   
   
   @Before
   public void setUp() throws Exception {
      super.setUp();
      httpSender = new HttpSender(TemplatedHttpHandlerTest.class, bridgeMetrics);
      handler = new TemplatedHttpHandler(new AlwaysAllow(),new HttpSender(TemplatedHttpHandlerTest.class, bridgeMetrics),templateService) {
         public String getContentType() {
            return "application/xml";
         }
         public TemplatedResponse doHandle(FullHttpRequest request, ChannelHandlerContext ctx) {
            return this.createTemplateResponse("test.template", "Context");
         }
      };
   }

   @Test
   public void shouldRespondSuccessSingleTemplate() throws Exception {
      EasyMock.expect(templateService.render("test.template","Context")).andReturn("");
      replay();
      FullHttpResponse response = handler.respond(fullHttpRequest,ctx);
      verify();
      assertEquals(HttpResponseStatus.OK, response.getStatus());
      assertEquals("application/xml", response.headers().get(HttpHeaders.Names.CONTENT_TYPE));
   }
   @Test

   public void shouldRespondSuccessMultipartTemplate() throws Exception {
      Map<String,String> multi = new HashMap<String,String>();
      handler = new TemplatedHttpHandler(new AlwaysAllow(),httpSender,templateService) {
         public String getContentType() {
            return "application/xml";
         }
         public TemplatedResponse doHandle(FullHttpRequest request, ChannelHandlerContext ctx) {
            return this.createTemplateResponse("test.template", "Context","Section");
         }
      }; 
      EasyMock.expect(templateService.renderMultipart("test.template","Context")).andReturn(multi);
      
      replay();
      FullHttpResponse response = handler.respond(fullHttpRequest,ctx);
      verify();
      assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, response.getStatus());
   }   
   
   @Test
   public void shouldRespondReturnError() throws Exception {
      EasyMock.expect(templateService.render("test.template","Context")).andThrow(new RuntimeException("trouble"));
      replay();
      FullHttpResponse response = handler.respond(fullHttpRequest,ctx);
      verify();
      assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, response.getStatus());
   }
   
   @Test
   public void shouldRespondWithResponseCode() throws Exception {
      
      handler = new TemplatedHttpHandler(new AlwaysAllow(),httpSender,templateService) {
         public String getContentType() {
            return "application/xml";
         }
         public TemplatedResponse doHandle(FullHttpRequest request, ChannelHandlerContext ctx) {
            return new TemplatedResponse().withResponseStatus(HttpResponseStatus.NOT_FOUND);
         }
      }; 
      replay();
      FullHttpResponse response = handler.respond(fullHttpRequest,ctx);
      verify();
      assertEquals(HttpResponseStatus.NOT_FOUND, response.getStatus());
   }      
   

}

