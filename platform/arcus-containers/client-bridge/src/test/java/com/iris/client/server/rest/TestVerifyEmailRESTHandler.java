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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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
import com.iris.core.platform.PlatformMessageBus;
import com.iris.io.json.JSON;
import com.iris.messages.ClientMessage;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PersonCapability;
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
	PersonPlaceAssocDAO.class,
	FullHttpRequest.class, 
	ChannelHandlerContext.class, 
	PlatformMessageBus.class, 
	ClientFactory.class, 
	Client.class, 
	Authenticator.class,	
	BridgeMetrics.class,
	PlacePopulationCacheManager.class
})
public class TestVerifyEmailRESTHandler extends IrisMockTestCase {

	@Inject protected PlacePopulationCacheManager mockPopulationCacheMgr;
   @Inject
   private PersonDAO personDao;
   @Inject
   private PersonPlaceAssocDAO personPlaceAssocDao;
   
   @Inject
   private ClientFactory clientFactory;
   private Channel channel = new LocalChannel();
   @Inject
   private Client client;
   
   @Inject
   private Authenticator authenticator;

   @Inject
   private VerifyEmailRESTHandler handler;

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
   public void testRequestEmailVerification_Success() throws Exception {
   	Person curPerson = createPerson();
   	String token = "t123545";
      curPerson.setEmailVerificationToken(token);
      
      mockSetup(curPerson.getId(), token);		
   	
   	EasyMock.expect(personDao.findById(curPerson.getId())).andReturn(curPerson);
   	Capture<Person> personUpdatedCapture = EasyMock.newCapture(CaptureType.LAST);
   	EasyMock.expect(personDao.update(EasyMock.capture(personUpdatedCapture))).andReturn(curPerson);
   	
   	Set<UUID> placeIds = ImmutableSet.<UUID>of(curPerson.getCurrPlace(), UUID.randomUUID());
   	EasyMock.expect(personPlaceAssocDao.findPlaceIdsByPerson(curPerson.getId())).andReturn(placeIds);
   	
      replay();      
      
      FullHttpResponse response = handler.respond(request, ctx);   
           
      //Should send a value change for each place this person belongs to
   	List<PlatformMessage> msgs = platformMsgCapture.getValues();
   	Iterator<UUID> placeIdsIt = placeIds.iterator();
   	msgs.forEach((cur) -> {
   		assertValueChangeSent(cur, placeIdsIt.next());
   	});
   	
   	Person personUpdated = personUpdatedCapture.getValue();
   	assertNotNull(personUpdated.getEmailVerificationToken());
   	assertNotNull(personUpdated.getEmailVerified());
      
      MessageBody mb = toClientRequest(response);
      assertEquals(MessageConstants.MSG_EMPTY_MESSAGE, mb.getMessageType());
      
   }
   
   @Test
   public void testRequestEmailVerification_PersonNotExist() throws Exception {
      Person curPerson = createPerson();
      String token = "t123545";
      curPerson.setEmailVerificationToken(token);
      
      mockSetup(curPerson.getId(), token);		
   	
   	EasyMock.expect(personDao.findById(curPerson.getId())).andReturn(null);
      replay();      
      
      FullHttpResponse response = handler.respond(request, ctx);   
      
      MessageBody mb = toClientRequest(response);
      assertEquals(ErrorEvent.MESSAGE_TYPE, mb.getMessageType());     
      
   }
   
   @Test
   public void testRequestEmailVerification_TokenNotMatch() throws Exception {
      Person curPerson = createPerson();
      String token = "t123545";
      String token2 = "z123545";
      curPerson.setEmailVerificationToken(token);
      
      mockSetup(curPerson.getId(), token2);		
   	
   	EasyMock.expect(personDao.findById(curPerson.getId())).andReturn(curPerson).anyTimes();
      replay();      
      
      FullHttpResponse response = handler.respond(request, ctx);   
      
      MessageBody mb = toClientRequest(response);
      assertEquals(ErrorEvent.MESSAGE_TYPE, mb.getMessageType());     
      
   }
   
   @Test
   public void testRequestEmailVerification_TokenEmpty() throws Exception {
      Person curPerson = createPerson();
      String token = "t123545";
      curPerson.setEmailVerificationToken(token);
      
      mockSetup(curPerson.getId(), null);		//null token in request
   	
   	EasyMock.expect(personDao.findById(curPerson.getId())).andReturn(curPerson).anyTimes();
      replay();      
      
      FullHttpResponse response = handler.respond(request, ctx);   
      
      MessageBody mb = toClientRequest(response);
      assertEquals(ErrorEvent.MESSAGE_TYPE, mb.getMessageType());     
      
   }
   
   @Test
   public void testRequestEmailVerification_EmailAlreadyVerified() throws Exception {
      Person curPerson = createPerson();
      String token = "t123545";
      curPerson.setEmailVerificationToken(token);
      curPerson.setEmailVerified(new Date()); //already verified.
      
      mockSetup(curPerson.getId(), token);		
   	
   	EasyMock.expect(personDao.findById(curPerson.getId())).andReturn(curPerson).anyTimes();
      replay();      
      
      FullHttpResponse response = handler.respond(request, ctx);   
      
      MessageBody mb = toClientRequest(response);
      assertEquals(MessageConstants.MSG_EMPTY_MESSAGE, mb.getMessageType());     
      
   }
   
   private void mockSetup(UUID personId, String token) {
   	EasyMock.expect(client.getPrincipalId()).andReturn(personId).anyTimes();
      EasyMock.expect(request.content()).andReturn(Unpooled.copiedBuffer(generateClientMessage(personId, generateRequest(token)).getBytes()));
      EasyMock.expect(ctx.channel()).andReturn(channel).anyTimes();

      EasyMock.expect(clientFactory.get(channel)).andReturn(client).anyTimes();
      platformMsgCapture = EasyMock.newCapture(CaptureType.ALL);     
      EasyMock.expect(platformBus.send(EasyMock.capture(platformMsgCapture))).andAnswer(
         () -> {
            return Futures.immediateFuture(null);
         }
      ).anyTimes();
      
      
      //logout always needs to be called
      client.logout();
   	EasyMock.expectLastCall();   	  	
   	EasyMock.expect(authenticator.expireCookie()).andReturn(new DefaultCookie("test", "test"));
   }
   
   private void assertValueChangeSent(PlatformMessage msg, UUID placeId) {
   	assertNotNull(msg.getValue());
   	assertEquals(Capability.EVENT_VALUE_CHANGE, msg.getMessageType());
   	assertEquals(placeId.toString(), msg.getPlaceId());
		
	}

	private MessageBody toClientRequest(FullHttpResponse response) {
      String json = response.content().toString(CharsetUtil.UTF_8);
      ClientMessage clientMessage = JSON.fromJson(json, ClientMessage.class);
      return clientMessage.getPayload();
   }

   private MessageBody generateRequest(String token) {
      return PersonCapability.VerifyEmailRequest.builder().withToken(token).build();
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
      return curPerson;
   }

}

