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

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.shiro.codec.Base64;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.http.handlers.TemplatedHttpHandler.TemplatedResponse;
import com.iris.core.dao.AccountDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.core.template.TemplateService;
import com.iris.messages.PlatformMessage;
import com.iris.messages.model.Person;
import com.iris.messages.model.Place;
import com.iris.messages.type.Population;
import com.iris.platform.location.UspsDataServiceModule;
import com.iris.platform.notification.audit.AuditEventState;
import com.iris.platform.notification.audit.NotificationAuditor;
import com.iris.platform.notification.provider.ivr.TwilioHelper;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;

@Mocks({ PlatformMessageBus.class,BridgeMetrics.class, FullHttpRequest.class, ChannelHandlerContext.class, HttpHeaders.class,PersonDAO.class,PlaceDAO.class,AccountDAO.class,TemplateService.class,NotificationAuditor.class, PlacePopulationCacheManager.class})
@Modules({UspsDataServiceModule.class})
public class TestTwilioAckScriptHandler extends IrisMockTestCase  {

   @Inject
   private TwilioAckScriptHandler handler;

   @Inject
   private FullHttpRequest request;

   @Inject
   private ChannelHandlerContext ctx;
   
   @Inject
   private HttpHeaders httpHeaders;
   
   @Inject
   private PersonDAO personDAO;
   
   @Inject
   private PlaceDAO placeDAO;

   @Inject
   private NotificationAuditor auditor;

   @Inject
   private Person person;
   
   @Inject
   private Place place;
   
   @Inject private PlatformMessageBus bus;
   
   @Inject private PlacePopulationCacheManager populationCacheMgr;

   private UUID personID = UUID.randomUUID();
   private UUID placeId = UUID.randomUUID();
   private String sig;
   private URIBuilder builder;
   private long timestamp = new Date().getTime();
   
   @Override
   protected Set<String> configs() {
      Set<String> configs = super.configs();
      configs.add("src/test/resources/TestTwilioAckScriptHandler.properties");
      return configs;
   }
   
   @Before
   public void setUp() throws Exception {
      super.setUp();
      builder = buildParameters(null);
      FieldUtils.writeField(handler, "twilioAccountAuth", "AUTHKEY", true);

      sig = Base64.encodeToString(HmacUtils.hmacSha1 ("AUTHKEY", TwilioHelper.PROTOCOL_HTTPS + "somehost" + builder.toString()));
      EasyMock.expect(request.getUri()).andReturn(builder.toString()).anyTimes();
      EasyMock.expect(request.getMethod()).andReturn(HttpMethod.GET);
      EasyMock.expect(request.headers()).andReturn(httpHeaders).anyTimes();
      
      EasyMock.expect(httpHeaders.contains(TwilioHelper.SIGNATURE_HEADER_KEY)).andReturn(true).anyTimes();
      EasyMock.expect(httpHeaders.get(TwilioHelper.SIGNATURE_HEADER_KEY)).andReturn(sig).anyTimes();
      EasyMock.expect(httpHeaders.get(TwilioHelper.HOST_HEADER_KEY)).andReturn("somehost").anyTimes();
      EasyMock.expect(personDAO.findById(personID)).andReturn(person);
      EasyMock.expect(placeDAO.findById(placeId)).andReturn(place);
      EasyMock.expect(populationCacheMgr.getPopulationByPlaceId(EasyMock.anyObject(UUID.class))).andReturn(Population.NAME_GENERAL).anyTimes();
   }
   
   @Test
   public void handleVerifyFailed() throws Exception{
      replay();
      FieldUtils.writeField(handler, "twilioAccountAuth", "AUTHKEY2", true);
      TemplatedResponse response = handler.doHandle(request, ctx);
      assertEquals(HttpResponseStatus.NOT_FOUND,response.getResponseStatus().get());
   }

   @SuppressWarnings("unchecked")
   @Test
   public void handleAcknowledged() throws Exception{
      auditor.log("place:"+placeId,Instant.ofEpochMilli(timestamp), AuditEventState.DELIVERED,TwilioAckScriptHandler.ACKNOWLEDGED_MESSAGE);
      EasyMock.expectLastCall();
      EasyMock.expect(bus.send(EasyMock.anyObject(PlatformMessage.class))).andReturn(null);
      replay();
      TemplatedResponse response = handler.doHandle(request, ctx);
      Map<String,Object> context = (Map<String,Object>)response.getContext();
      assertEquals("test",response.getTemplateId().get());
      assertEquals(person,context.get("_person"));
      System.out.println("Callback URL: " + context.get("callbackURL"));
      assertEquals("/ivr/script/ack?script=test&notificationId=place%3A" + placeId.toString() + "&notificationTimestamp=" + timestamp + "&placeId=" + placeId.toString() + "&personId="+personID.toString() + "&retryCount=0", context.get("callbackURL"));

      assertEquals("acknowledged",response.getSectionId().get());
      verify();
   }
   
   @Test
   public void handleRetryTimeout() throws Exception{
      EasyMock.reset(request);
      EasyMock.expect(bus.send(EasyMock.anyObject(PlatformMessage.class))).andReturn(null);
      EasyMock.expect(request.headers()).andReturn(httpHeaders).anyTimes();
      EasyMock.expect(request.getMethod()).andReturn(HttpMethod.GET).anyTimes();
      EasyMock.expect(request.getUri()).andReturn(buildParameters(ImmutableMap.of(TwilioAckScriptHandler.RETRY_COUNT_PARAM, "3")).toString()).anyTimes();
      FieldUtils.writeField(handler, "twilioVerifyAuth", false, true);
      auditor.log("place:"+placeId,Instant.ofEpochMilli(timestamp), AuditEventState.FAILED,TwilioScriptHandler.TIMEOUT_MESSAGE);
      EasyMock.expectLastCall();
      replay();
      handler.doHandle(request, ctx);
      verify();
   }
   
   private URIBuilder buildParameters(Map<String,String>params) throws Exception{
      URIBuilder builder = new URIBuilder("/ivr/script");
      if(params!=null){
         for(Map.Entry<String, String>entry:params.entrySet()){
            builder.addParameter(entry.getKey(),entry.getValue());
         }
      }
      builder
      .addParameter(TwilioScriptHandler.SCRIPT_PARAM, "test")
      .addParameter(TwilioScriptHandler.STEP_PARAM, "submitAck")
      .addParameter(TwilioScriptHandler.TWILIO_DIGITS_PARAM, "#")
      .addParameter(TwilioHelper.PERSON_ID_PARAM_NAME, personID.toString())
      .addParameter(TwilioHelper.PLACE_ID_PARAM_NAME, placeId.toString())
      .addParameter(TwilioHelper.NOTIFICATION_ID_PARAM_NAME, "place:"+placeId)
      .addParameter(TwilioScriptHandler.CUSTOM_MESSAGE_PARAM, "test")
      .addParameter(TwilioHelper.NOTIFICATION_EVENT_TIME_PARAM_NAME, Long.toString(timestamp));
      

      return builder;
   }
   


}

