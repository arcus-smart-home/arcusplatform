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
package com.iris.client.server.rest.twilio;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.shiro.codec.Base64;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.core.template.TemplateService;
import com.iris.messages.PlatformMessage;
import com.iris.messages.type.Population;
import com.iris.platform.notification.audit.AuditEventState;
import com.iris.platform.notification.audit.NotificationAuditor;
import com.iris.platform.notification.provider.ivr.TwilioHelper;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;

@Mocks({ BridgeMetrics.class, FullHttpRequest.class, ChannelHandlerContext.class, NotificationAuditor.class, HttpHeaders.class,PlatformMessageBus.class,TemplateService.class, PlacePopulationCacheManager.class })
public class TestTwilioAckEventHandler extends IrisMockTestCase  {

   private static final String DEFAULT_HOST = "platform.arcus.com";
   private static final String ALTERNATE_HOST = "platform2.arcus.com";
   private static final long timestamp = new Date().getTime();

   @Inject
   private TwilioAckEventHandler handler;

   @Inject
   private FullHttpRequest request;

   @Inject
   private ChannelHandlerContext ctx;
   
   @Inject
   private HttpHeaders httpHeaders;

   @Inject
   private NotificationAuditor notificationAuditor;
   
   @Inject
   private PlatformMessageBus bus;
   
   @Inject
   private PlacePopulationCacheManager mockPopulationCacheMgr;
    
   @Override
   protected Set<String> configs() {
      Set<String> configs = super.configs();
      configs.add("src/test/resources/TestTwilioAckEventHandler.properties");
      return configs;
   }
   
   @Before
   public void setUp() throws Exception {
      super.setUp();
   }
   
   public void buildMocks() throws Exception{
      buildMocks("human",DEFAULT_HOST,DEFAULT_HOST,TwilioHelper.CALL_STATUS_COMPLETED);
   }
   public void buildMocksAnsweredByMachine() throws Exception{
      buildMocks("machine",DEFAULT_HOST,DEFAULT_HOST,TwilioHelper.CALL_STATUS_COMPLETED);
   }
   public void buildMocksdifferentHost() throws Exception{
      buildMocks("human",DEFAULT_HOST,ALTERNATE_HOST,TwilioHelper.CALL_STATUS_COMPLETED);
   }
   public void buildMocksStatus(String status) throws Exception{
      buildMocks("human",DEFAULT_HOST,DEFAULT_HOST,status);
   }
   
   public void buildMocks(String anweredBy,String host, String sigHost,String callStatus) throws Exception{
      URIBuilder builder = new URIBuilder("/ivr/event/ack")
         .addParameter(TwilioBaseHandler.SCRIPT_PARAM, "alarm.smoke.triggered")
         .addParameter(TwilioHelper.NOTIFICATION_ID_PARAM_NAME, "place:"+UUID.randomUUID())
         .addParameter(TwilioHelper.PERSON_ID_PARAM_NAME, "test")
         .addParameter(TwilioHelper.NOTIFICATION_EVENT_TIME_PARAM_NAME, "12345678910")
         .addParameter(TwilioHelper.CALL_STATUS_PARAM_KEY, callStatus)
         .addParameter(TwilioHelper.ANSWEREDBY_PARAM_KEY, anweredBy);
      
      String testURI=builder.build().toString();
      FieldUtils.writeField(handler, "twilioAccountAuth", "AUTHKEY", true);
      
      String protocol =TwilioHelper.PROTOCOL_HTTPS;
      
      String sig = Base64.encodeToString(HmacUtils.hmacSha1 ("AUTHKEY", protocol + sigHost + testURI));
      
      EasyMock.expect(request.getMethod()).andReturn(HttpMethod.GET).anyTimes();
      EasyMock.expect(request.getUri()).andReturn(testURI).anyTimes();
      EasyMock.expect(request.headers()).andReturn(httpHeaders).anyTimes();
      EasyMock.expect(httpHeaders.contains(TwilioHelper.SIGNATURE_HEADER_KEY)).andReturn(true).anyTimes();
      EasyMock.expect(httpHeaders.get(TwilioHelper.SIGNATURE_HEADER_KEY)).andReturn(sig).anyTimes();
      EasyMock.expect(httpHeaders.get(TwilioHelper.HOST_HEADER_KEY)).andReturn(host).anyTimes();
      EasyMock.expect(mockPopulationCacheMgr.getPopulationByPlaceId(EasyMock.anyObject(UUID.class))).andReturn(Population.NAME_GENERAL);
   }
   
   public void expectAuditLog(AuditEventState state){
      notificationAuditor.log(EasyMock.anyString(),EasyMock.anyObject(Instant.class),EasyMock.eq(state),EasyMock.anyString());
      EasyMock.expectLastCall();
   }
   
   @Test
   public void testRespondSuccess() throws Exception {
      buildMocks();
      expectAuditLog(AuditEventState.DELIVERED);
      replay();
      FullHttpResponse response = handler.respond(request, ctx);
      assertEquals(HttpResponseStatus.NO_CONTENT,response.getStatus());
   }
   
   @Test
   public void testRespondSignatureValidationFailed() throws Exception {
      buildMocks();
      replay();
      FieldUtils.writeField(handler, "twilioAccountAuth", "xyx", true);
      FullHttpResponse response = handler.respond(request, ctx);
      assertEquals(HttpResponseStatus.NOT_FOUND,response.getStatus());
   }
   
   @Test
   public void testRespondSignatureValidationFailedValidationOff() throws Exception {
      buildMocks();
      expectAuditLog(AuditEventState.DELIVERED);
      replay();
      FieldUtils.writeField(handler, "twilioAccountAuth", "xyx", true);
      FieldUtils.writeField(handler, "twilioVerifyAuth", false, true);
      FullHttpResponse response = handler.respond(request, ctx);
      assertEquals(HttpResponseStatus.NO_CONTENT,response.getStatus());
   }
   
   
   @Test
   public void testRespondAnswedByMachine() throws Exception {
      buildMocksAnsweredByMachine();
      expectAuditLog(AuditEventState.DELIVERED);
      replay();
      FullHttpResponse response = handler.respond(request, ctx);
      assertEquals(HttpResponseStatus.NO_CONTENT,response.getStatus());
   }
   
   @Test
   public void testRespondSpecifyHost() throws Exception {
      FieldUtils.writeField(handler, "callbackHost", ALTERNATE_HOST, true);
      expectAuditLog(AuditEventState.DELIVERED);
      buildMocksdifferentHost();
      replay();
      FullHttpResponse response = handler.respond(request, ctx);
      assertEquals(HttpResponseStatus.NO_CONTENT,response.getStatus());
   }

   @Test
   public void testRespondFailed() throws Exception {
      testFailure(TwilioHelper.CALL_STATUS_FAILED);
   }
   @Test
   public void testRespondBusy() throws Exception {
      testFailure(TwilioHelper.CALL_STATUS_BUSY);
   }
   @Test
   public void testRespondNoAnswer() throws Exception {
      testFailure(TwilioHelper.CALL_STATUS_NOANSWER);
   }
   
   private void testFailure(String callStatus) throws Exception{
      buildMocksStatus(callStatus);
      expectAuditLog(AuditEventState.FAILED);
      EasyMock.expect(bus.send(EasyMock.anyObject(PlatformMessage.class))).andReturn(null);
      replay();
      FullHttpResponse response = handler.respond(request, ctx);
      assertEquals(HttpResponseStatus.NO_CONTENT,response.getStatus());
   }
   

}

