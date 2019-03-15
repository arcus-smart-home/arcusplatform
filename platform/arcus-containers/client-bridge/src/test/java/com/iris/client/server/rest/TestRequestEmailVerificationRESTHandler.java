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

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.netty.Authenticator;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.notification.Notifications;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.io.json.JSON;
import com.iris.messages.ClientMessage;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.capability.NotificationCapability.NotifyRequest;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.PersonCapability.SendVerificationEmailRequest;
import com.iris.messages.model.Fixtures;
import com.iris.messages.model.Person;
import com.iris.messages.services.PlatformConstants;
import com.iris.messages.type.Population;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.local.LocalChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.util.CharsetUtil;


@Mocks({ PersonDAO.class, 
	FullHttpRequest.class, 
	ChannelHandlerContext.class, 
	PlatformMessageBus.class, 
	ClientFactory.class, 
	Client.class, 
	Authenticator.class,	
	BridgeMetrics.class,
	PersonPlaceAssocDAO.class,
	PlacePopulationCacheManager.class})
public class TestRequestEmailVerificationRESTHandler extends IrisMockTestCase {

   @Inject
   private PersonDAO personDao;
   @Inject
   private PersonPlaceAssocDAO mockPersonPlaceAssocDAO;
   
   @Inject protected PlacePopulationCacheManager mockPopulationCacheMgr;
   
   @Inject
   private ClientFactory clientFactory;
   private Channel channel = new LocalChannel();
   @Inject
   private Client client;
   
   @Inject
   private Authenticator authenticator;

   @Inject
   private RequestEmailVerificatonRESTHandler handler;

   @Inject
   private FullHttpRequest request;

   @Inject
   private ChannelHandlerContext ctx;
   
   @Inject
   private PlatformMessageBus platformBus;

   private Capture<PlatformMessage> platformMsgCapture;

   @Before
	public void setupMocks() {
   	EasyMock.expect(mockPopulationCacheMgr.getPopulationByPlaceId(EasyMock.anyObject(UUID.class))).andReturn(Population.NAME_GENERAL).anyTimes();
   	EasyMock.expect(mockPopulationCacheMgr.getPopulationByPlaceId(EasyMock.anyObject(String.class))).andReturn(Population.NAME_GENERAL).anyTimes();
   }	

   @Test
   public void testRequestEmailVerification_Success_Android() throws Exception {
   	doRequestEmailVerification_Success(SendVerificationEmailRequest.SOURCE_ANDROID);      
   }
   
   private void doRequestEmailVerification_Success(String source) throws Exception {
   	doRequestEmailVerification_Success(source, false);
   }
   
   private void doRequestEmailVerification_Success(String source, boolean valueChangeEventSent) throws Exception {
   	Person curPerson = createPerson();
   	if(valueChangeEventSent) {
   		curPerson.setEmailVerified(new Date());
   	}
      
   	Capture<Person> personUpdatedCapture = mockSetup(curPerson, true, source, 1);		
      
      replay();      
      
      FullHttpResponse response = handler.respond(request, ctx);   
           
   	//notification needs to be sent
   	PlatformMessage msg = platformMsgCapture.getValues().get(0);
   	assertNotificationSent(msg, source); 
   	if(valueChangeEventSent) {
   		msg = platformMsgCapture.getValues().get(1);
   		assertNotNull(msg.getValue());
      	assertEquals(Capability.EVENT_VALUE_CHANGE, msg.getMessageType());
      	Map<String, Object> changes = msg.getValue().getAttributes();      	
   		assertEquals(Boolean.FALSE, changes.get(PersonCapability.ATTR_EMAILVERIFIED));
   		
   	}
   	Person personUpdated = personUpdatedCapture.getValue();
   	assertNotNull(personUpdated.getEmailVerificationToken());
   	assertNull(personUpdated.getEmailVerified());
      
      MessageBody mb = toClientRequest(response);
      assertEquals(MessageConstants.MSG_EMPTY_MESSAGE, mb.getMessageType());
   }
   
   @Test
   public void testRequestEmailVerification_AlreadyVerified() throws Exception {
   	doRequestEmailVerification_Success(SendVerificationEmailRequest.SOURCE_ANDROID, true);
   }
   
   @Test
   public void testRequestEmailVerification_Resend() throws Exception {
   	String source = SendVerificationEmailRequest.SOURCE_IOS;
   	Person curPerson = createPerson();
      
   	Capture<Person> personUpdatedCapture = mockSetup(curPerson, true, source, 2);		
      
      replay();      
      
      FullHttpResponse response = handler.respond(request, ctx);   
           
   	//notification needs to be sent
   	PlatformMessage msg = platformMsgCapture.getValues().get(0);
   	assertNotificationSent(msg, source);      
   	Person personUpdated = personUpdatedCapture.getValues().get(0);
   	String firstToken = personUpdated.getEmailVerificationToken();
   	assertNotNull(firstToken);
   	assertNull(personUpdated.getEmailVerified());
      
      MessageBody mb = toClientRequest(response);
      assertEquals(MessageConstants.MSG_EMPTY_MESSAGE, mb.getMessageType());  
      
      response = handler.respond(request, ctx);  
      //notification needs to be sent
   	msg = platformMsgCapture.getValues().get(1);
   	assertNotificationSent(msg, source);      
   	personUpdated = personUpdatedCapture.getValues().get(1);
   	String secondToken = personUpdated.getEmailVerificationToken();
   	assertNotNull(secondToken);
   	assertNotEquals(firstToken, secondToken);
   	assertNull(personUpdated.getEmailVerified());
   }
   
   
   @Test
   public void testRequestEmailVerification_Success_iOS() throws Exception {
   	doRequestEmailVerification_Success(SendVerificationEmailRequest.SOURCE_IOS);      
   }
   
   @Test
   public void testRequestEmailVerification_PersonNotExist() throws Exception {
      Person curPerson = createPerson();
      
      mockSetup(curPerson, false, PersonCapability.SendVerificationEmailRequest.SOURCE_WEB, 1);		
   	
   	//EasyMock.expect(personDao.findById(curPerson.getId())).andReturn(null);
      replay();      
      
      FullHttpResponse response = handler.respond(request, ctx);   
      
      MessageBody mb = toClientRequest(response);
      assertEquals(ErrorEvent.MESSAGE_TYPE, mb.getMessageType());     
      
   }
   
   @Test
   public void testRequestEmailVerification_PrincipalNotMatch() throws Exception {
      Person curPerson = createPerson();
      
      mockSetup(curPerson, true, false, PersonCapability.SendVerificationEmailRequest.SOURCE_WEB, 1);		  	   	
   	
      replay();      
      
      FullHttpResponse response = handler.respond(request, ctx);   
      
      MessageBody mb = toClientRequest(response);
      //Due to I2-3123, we allow it for now
      //assertEquals(ErrorEvent.MESSAGE_TYPE, mb.getMessageType());     
      assertEquals(MessageConstants.MSG_EMPTY_MESSAGE, mb.getMessageType());
   }
   
   private Capture<Person> mockSetup(Person curPerson, boolean personExist, String source, int times) {
   	return mockSetup(curPerson, personExist, true, source, times);
   }
   
   private Capture<Person> mockSetup(Person curPerson, boolean personExist, boolean principalMatch, String source, int times) {
   	EasyMock.expect(client.getPrincipalId()).andReturn(curPerson.getId()).times(times);
      EasyMock.expect(request.content()).andReturn(Unpooled.copiedBuffer(generateClientMessage(curPerson.getId(), generateRequest(source)).getBytes())).times(times);
      EasyMock.expect(ctx.channel()).andReturn(channel).times(times);

      EasyMock.expect(clientFactory.get(channel)).andReturn(client).times(times);
      platformMsgCapture = EasyMock.newCapture(CaptureType.ALL);     
      EasyMock.expect(platformBus.send(EasyMock.capture(platformMsgCapture))).andAnswer(
         () -> {
            return Futures.immediateFuture(null);
         }
      ).anyTimes();
      
      
      //logout always needs to be called
      client.logout();
   	EasyMock.expectLastCall().times(times);   	  	
   	EasyMock.expect(authenticator.expireCookie()).andReturn(new DefaultCookie("test", "test")).times(times);
   	
   	if(principalMatch) {
   		EasyMock.expect(client.getPrincipalName()).andReturn(curPerson.getEmail()).times(times);   	
   	}else{
   		EasyMock.expect(client.getPrincipalName()).andReturn("wrongemail@gmail.com").times(times); 
   	}
   	if(personExist) {
   		EasyMock.expect(personDao.findById(curPerson.getId())).andReturn(curPerson).times(times);
   	}else{
   		EasyMock.expect(personDao.findById(curPerson.getId())).andReturn(null).times(times);
   	}
   	Capture<Person> personUpdatedCapture = EasyMock.newCapture(CaptureType.ALL);
   	EasyMock.expect(personDao.update(EasyMock.capture(personUpdatedCapture))).andReturn(curPerson).times(times);
   	
   	EasyMock.expect(mockPersonPlaceAssocDAO.findPlaceIdsByPerson(curPerson.getId())).andReturn(ImmutableSet.<UUID>of(curPerson.getCurrPlace())).anyTimes();
   	
   	return personUpdatedCapture;
   }
   
   private void assertNotificationSent(PlatformMessage msg, String expectedSource) {
   	assertNotNull(msg.getValue());
   	assertEquals(NotificationCapability.NAMESPACE, msg.getDestination().getGroup());
   	assertEquals(NotifyRequest.NAME, msg.getMessageType());
   	MessageBody msgBody = msg.getValue();
   	Map<String, String> params = NotifyRequest.getMsgParams(msgBody);
		String actualSource = params.get(Notifications.AccountEmailVerify.PARAM_PLATFORM);
		assertEquals(expectedSource.toLowerCase(), actualSource);
		assertNotNull(params.get(Notifications.AccountEmailVerify.PARAM_TOKEN));
	}

	private MessageBody toClientRequest(FullHttpResponse response) {
      String json = response.content().toString(CharsetUtil.UTF_8);
      ClientMessage clientMessage = JSON.fromJson(json, ClientMessage.class);
      return clientMessage.getPayload();
   }

   private MessageBody generateRequest(String source) {
      return PersonCapability.SendVerificationEmailRequest.builder()
      			.withSource(source).build();
   }

   private String generateClientMessage(UUID personId, MessageBody body) {
      ClientMessage.Builder messageBuilder = ClientMessage.builder()
      		.withCorrelationId("")
      		.withSource(Address.platformService(PlatformConstants.SERVICE_PEOPLE).getRepresentation())
      		.withDestination(Address.platformService(personId, PlatformConstants.SERVICE_PEOPLE).getRepresentation())
      		.withPayload(body);
      return JSON.toJson(messageBuilder.create());
   }
   
   private Person createPerson() {
   	Person curPerson = Fixtures.createPerson();
      curPerson.setId(UUID.randomUUID());
      curPerson.setEmailVerificationToken(null);
      curPerson.setEmailVerified(null);
      curPerson.setCurrPlace(UUID.randomUUID());
      return curPerson;
   }

}

