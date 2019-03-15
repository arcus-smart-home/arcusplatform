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

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.netty.Authenticator;
import com.iris.core.dao.MobileDeviceDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.io.json.JSON;
import com.iris.messages.ClientMessage;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.capability.NotificationCapability.NotifyRequest;
import com.iris.messages.model.Fixtures;
import com.iris.messages.model.MobileDevice;
import com.iris.messages.model.Person;
import com.iris.messages.service.SessionService;
import com.iris.messages.service.SessionService.LockDeviceRequest;
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

@RunWith(value = Parameterized.class)
@Mocks({ PersonDAO.class, 
	MobileDeviceDAO.class, 
	FullHttpRequest.class, 
	ChannelHandlerContext.class, 
	PlatformMessageBus.class, 
	ClientFactory.class, 
	Client.class, 
	Authenticator.class,	
	BridgeMetrics.class,
	PlacePopulationCacheManager.class})
public class TestLockDeviceRESTHandler extends IrisMockTestCase {

	@Inject protected PlacePopulationCacheManager mockPopulationCacheMgr;
	
   @Inject
   private PersonDAO personDao;
   @Inject
   private MobileDeviceDAO mobileDeviceDao;
   
   @Inject
   private ClientFactory clientFactory;
   private Channel channel = new LocalChannel();
   @Inject
   private Client client;
   
   @Inject
   private Authenticator authenticator;

   @Inject
   private LockDeviceRESTHandler handler;

   @Inject
   private FullHttpRequest request;

   @Inject
   private ChannelHandlerContext ctx;
   
   @Inject
   private PlatformMessageBus platformBus;
   
   private Person curLoggedPerson = null;
   //private UUID curLoggedInPerson = UUID.randomUUID();

   @Parameters(name="deviceIdentifier[{0}],reason[{1}],success[{2}],personMatch[{3}]")
   public static List<Object []> files() {
      return Arrays.<Object[]>asList(
            new Object [] { "12345", LockDeviceRequest.REASON_TOUCH_FAILED, true, true},
            new Object [] { "12345", LockDeviceRequest.REASON_USER_REQUESTED, true, true},
            new Object [] { "12345", "BadReason", false, true}, //reason not valid
            new Object [] { "12345", LockDeviceRequest.REASON_TOUCH_FAILED, false, false} //person id not match
      );
   }
   
   private final String deviceIdentifier;
   private final String reason;
   private final boolean isSuccess;
   private final boolean personMatch;
   
   @Before
	public void setupMocks() {
   	EasyMock.expect(mockPopulationCacheMgr.getPopulationByPlaceId(EasyMock.anyObject(UUID.class))).andReturn(Population.NAME_GENERAL).anyTimes();
   }	
   
   public TestLockDeviceRESTHandler(String deviceIdentifier, String reason, boolean isSuccess, boolean personMatch) {
      this.deviceIdentifier = deviceIdentifier;
      this.reason = reason;
      this.isSuccess = isSuccess;
      this.personMatch = personMatch;
      curLoggedPerson = Fixtures.createPerson();
      curLoggedPerson.setId(UUID.randomUUID());
   }

   @Test
   public void testLockDeviceRequest() throws Exception {
      

      FullHttpResponse response = callLockDeviceRequest(deviceIdentifier, reason, isSuccess, personMatch);
      MessageBody mb = toClientRequest(response);
      if(isSuccess) {
      	assertEquals(MessageConstants.MSG_EMPTY_MESSAGE, mb.getMessageType());
      }else{
      	assertEquals(ErrorEvent.MESSAGE_TYPE, mb.getMessageType());
      }
      
      
      
   }
   

   private FullHttpResponse callLockDeviceRequest(String curDeviceId, String curReason, boolean curSuccess, boolean curPersonMatch) throws Exception {
      EasyMock.expect(client.getPrincipalId()).andReturn(curLoggedPerson.getId()).anyTimes();
      EasyMock.expect(request.content()).andReturn(Unpooled.copiedBuffer(generateClientMessage(generateRequest(curDeviceId, curReason)).getBytes()));
      EasyMock.expect(ctx.channel()).andReturn(channel).anyTimes();

      EasyMock.expect(clientFactory.get(channel)).andReturn(client).anyTimes();
      Capture<PlatformMessage> platformMsgCapture = EasyMock.newCapture(CaptureType.ALL);
     
      EasyMock.expect(platformBus.send(EasyMock.capture(platformMsgCapture))).andAnswer(
         () -> {
            return Futures.immediateFuture(null);
         }
      ).anyTimes();
      
      MobileDevice mobileDevice = new MobileDevice();
      mobileDevice.setDeviceIdentifier(curDeviceId);
      if(curPersonMatch) {
      	mobileDevice.setPersonId(curLoggedPerson.getId());
      	mobileDeviceDao.delete(mobileDevice);
      	EasyMock.expectLastCall();
      	EasyMock.expect(personDao.findById(curLoggedPerson.getId())).andReturn(curLoggedPerson);
      }else{
      	mobileDevice.setPersonId(UUID.randomUUID());
      }
      EasyMock.expect(mobileDeviceDao.findWithToken(curDeviceId)).andReturn(mobileDevice);
      
      
      //logout always needs to be called
      client.logout();
   	EasyMock.expectLastCall();   	  	
   	EasyMock.expect(authenticator.expireCookie()).andReturn(new DefaultCookie("test", "test"));		
   	
      replay();      
      
      FullHttpResponse response = handler.respond(request, ctx);   
      
      if(curSuccess) {
      	//notification needs to be sent
      	PlatformMessage msg = platformMsgCapture.getValue();
      	assertNotificationSent(msg);         	      	
      }
      return response;
      
   }
   
   private void assertNotificationSent(PlatformMessage msg) {
   	assertNotNull(msg.getValue());
   	assertEquals(NotificationCapability.NAMESPACE, msg.getDestination().getGroup());
   	assertEquals(NotifyRequest.NAME, msg.getMessageType());
		
	}

	private MessageBody toClientRequest(FullHttpResponse response) {
      String json = response.content().toString(CharsetUtil.UTF_8);
      ClientMessage clientMessage = JSON.fromJson(json, ClientMessage.class);
      return clientMessage.getPayload();
   }

   private MessageBody generateRequest(String deviceIdentifier, String reason) {
      return SessionService.LockDeviceRequest.builder().withDeviceIdentifier(deviceIdentifier).withReason(reason).build();
   }

   private String generateClientMessage(MessageBody body) {
      ClientMessage.Builder messageBuilder = ClientMessage.builder().withCorrelationId("").withSource(Address.platformService(PlatformConstants.SERVICE_PEOPLE).getRepresentation()).withPayload(body);
      return JSON.toJson(messageBuilder.create());
   }

}

