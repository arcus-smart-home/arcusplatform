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
package com.iris.messages;

import java.util.Collections;
import java.util.HashMap;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Objects;
import com.iris.bootstrap.Bootstrap;
import com.iris.bootstrap.ServiceLocator;
import com.iris.bootstrap.guice.GuiceServiceLocator;
import com.iris.messages.address.Address;
import com.iris.messages.address.HubAddress;

public class TestPlatformMessageBuilder {
   private final Address fromAddress = HubAddress.hubAddress("ABC-1234");
   private final Address toAddress = Address.platformService("Devices");
   private final MessageBody deviceEvent = MessageBody.buildMessage("Event", new HashMap<String,Object>());

   @SuppressWarnings("unchecked")
   @Before
   public void setUp() throws Exception {
      Bootstrap bootstrap = Bootstrap.builder()
            .withModuleClasses(MessagesModule.class)
            .build();
      ServiceLocator.init(GuiceServiceLocator.create(bootstrap.bootstrap()));
   }

   @After
   public void tearDown() throws Exception {
      ServiceLocator.destroy();
   }

   @Test
   public void testBasicMessage() {
      PlatformMessage message = PlatformMessage.builder()
               .from(fromAddress)
               .to(toAddress)
               .withPayload(deviceEvent).create();
      Assert.assertTrue(message.isPlatform());
      Assert.assertFalse(message.isRequest());
      Assert.assertFalse(message.isError());
      Assert.assertNull(message.getCorrelationId());
      Assert.assertEquals("Event", message.getMessageType());
      Assert.assertEquals(fromAddress, message.getSource());
      Assert.assertEquals(toAddress, message.getDestination());
      Assert.assertEquals(-1, message.getTimeToLive());
      long now = System.currentTimeMillis();
      long sentTime = message.getTimestamp().getTime();
      Assert.assertTrue(((now - 500) <= sentTime) && (sentTime <= now));
   }

   @Test
   public void testCopyBuilder() {
      PlatformMessage message = 
            PlatformMessage
               .builder()
               .from(fromAddress)
               .to(toAddress)
               .withPayload(deviceEvent)
               .create();
      PlatformMessage.Builder builder = PlatformMessage.builder(message);
      Assert.assertEquals(message, builder.create());
      
      builder.withPlaceId("place");
      Assert.assertFalse(Objects.equal(message, builder.create()));
   }

   @Test
   public void testMessageWithCorrelationId() {
      PlatformMessage message = PlatformMessage.builder()
            .from(fromAddress)
            .to(toAddress)
            .withCorrelationId("id")
            .withPayload(deviceEvent).create();
      Assert.assertTrue(message.isPlatform());
      Assert.assertFalse(message.isRequest());
      Assert.assertFalse(message.isError());
      Assert.assertEquals("id", message.getCorrelationId());
      Assert.assertEquals("Event", message.getMessageType());
      Assert.assertEquals(fromAddress, message.getSource());
      Assert.assertEquals(toAddress, message.getDestination());
      Assert.assertEquals(-1, message.getTimeToLive());
      long now = System.currentTimeMillis();
      long sentTime = message.getTimestamp().getTime();
      Assert.assertTrue(((now - 500) <= sentTime) && (sentTime <= now));
   }

   @Test
   public void testErrorMessage() {
      PlatformMessage message = PlatformMessage.builder()
            .from(fromAddress)
            .to(toAddress)
            .withPayload(new ErrorEvent("code", "message")).create();
      Assert.assertTrue(message.isPlatform());
      Assert.assertFalse(message.isRequest());
      Assert.assertTrue(message.isError());
      Assert.assertNull(message.getCorrelationId());
      Assert.assertEquals(ErrorEvent.MESSAGE_TYPE, message.getMessageType());
      Assert.assertEquals(fromAddress, message.getSource());
      Assert.assertEquals(toAddress, message.getDestination());
      Assert.assertEquals(-1, message.getTimeToLive());
      long now = System.currentTimeMillis();
      long sentTime = message.getTimestamp().getTime();
      Assert.assertTrue(((now - 500) <= sentTime) && (sentTime <= now));
   }

   @Test
   public void testCreateBroadcastMessage() {
      PlatformMessage message = PlatformMessage.createBroadcast(deviceEvent, fromAddress);
      Assert.assertTrue(message.isPlatform());
      Assert.assertFalse(message.isRequest());
      Assert.assertFalse(message.isError());
      Assert.assertNull(message.getCorrelationId());
      Assert.assertEquals("Event", message.getMessageType());
      Assert.assertEquals(fromAddress, message.getSource());
      Assert.assertEquals(Address.broadcastAddress(), message.getDestination());
      Assert.assertEquals(-1, message.getTimeToLive());
      long now = System.currentTimeMillis();
      long sentTime = message.getTimestamp().getTime();
      Assert.assertTrue(((now - 500) <= sentTime) && (sentTime <= now));
   }

   @Test
   public void testRequest() {
      PlatformMessage message = PlatformMessage.buildRequest(MessageConstants.MSG_ACCOUNT_LIST_DEVICES, Collections.<String,Object>emptyMap(), fromAddress, toAddress).create();
      Assert.assertTrue(message.isPlatform());
      Assert.assertTrue(message.isRequest());
      Assert.assertFalse(message.isError());
      Assert.assertNull(message.getCorrelationId());
      Assert.assertEquals(MessageConstants.MSG_ACCOUNT_LIST_DEVICES, message.getMessageType());
      Assert.assertEquals(fromAddress, message.getSource());
      Assert.assertEquals(toAddress, message.getDestination());
      Assert.assertEquals(-1, message.getTimeToLive());
      long now = System.currentTimeMillis();
      long sentTime = message.getTimestamp().getTime();
      Assert.assertTrue(((now - 500) <= sentTime) && (sentTime <= now));
   }

   @Test
   public void testRequestWithCorrelation() {
      PlatformMessage message = PlatformMessage.buildRequest(MessageConstants.MSG_ACCOUNT_LIST_DEVICES, Collections.<String,Object>emptyMap(), fromAddress, toAddress)
            .withCorrelationId("id")
            .create();
      Assert.assertTrue(message.isPlatform());
      Assert.assertTrue(message.isRequest());
      Assert.assertFalse(message.isError());
      Assert.assertEquals("id", message.getCorrelationId());
      Assert.assertEquals(MessageConstants.MSG_ACCOUNT_LIST_DEVICES, message.getMessageType());
      Assert.assertEquals(fromAddress, message.getSource());
      Assert.assertEquals(toAddress, message.getDestination());
      Assert.assertEquals(-1, message.getTimeToLive());
      long now = System.currentTimeMillis();
      long sentTime = message.getTimestamp().getTime();
      Assert.assertTrue(((now - 500) <= sentTime) && (sentTime <= now));
   }

   @Test
   public void testRequestWithoutCorrelation() {
      PlatformMessage message = PlatformMessage.buildRequest(MessageConstants.MSG_ACCOUNT_LIST_DEVICES, Collections.<String,Object>emptyMap(), fromAddress, toAddress)
            .withoutCorrelationId()
            .create();
      Assert.assertTrue(message.isPlatform());
      Assert.assertTrue(message.isRequest());
      Assert.assertFalse(message.isError());
      Assert.assertNull(message.getCorrelationId());
      Assert.assertEquals(MessageConstants.MSG_ACCOUNT_LIST_DEVICES, message.getMessageType());
      Assert.assertEquals(fromAddress, message.getSource());
      Assert.assertEquals(toAddress, message.getDestination());
      Assert.assertEquals(-1, message.getTimeToLive());
      long now = System.currentTimeMillis();
      long sentTime = message.getTimestamp().getTime();
      Assert.assertTrue(((now - 500) <= sentTime) && (sentTime <= now));
   }

   @Test
   public void testBuildResponse() {
      PlatformMessage request = PlatformMessage.buildRequest(MessageConstants.MSG_ACCOUNT_LIST_DEVICES, Collections.<String,Object>emptyMap(), fromAddress, toAddress)
            .withCorrelationId("id")
            .create();
      PlatformMessage message = PlatformMessage.buildResponse(request, Collections.<String,Object>emptyMap()).create();
      Assert.assertTrue(message.isPlatform());
      Assert.assertFalse(message.isRequest());
      Assert.assertFalse(message.isError());
      Assert.assertEquals("id", message.getCorrelationId());
      Assert.assertEquals(MessageConstants.MSG_ACCOUNT_LIST_DEVICES_RESPONSE, message.getMessageType());
      Assert.assertEquals(toAddress, message.getSource());
      Assert.assertEquals(fromAddress, message.getDestination());
      Assert.assertEquals(-1, message.getTimeToLive());
      long now = System.currentTimeMillis();
      long sentTime = message.getTimestamp().getTime();
      Assert.assertTrue(((now - 500) <= sentTime) && (sentTime <= now));
   }

   @Test
   public void testTimeToLive() {
      PlatformMessage message = PlatformMessage.builder()
            .from(fromAddress)
            .to(toAddress)
            .withTimeToLive(500)
            .withPayload(deviceEvent).create();
      Assert.assertTrue(message.isPlatform());
      Assert.assertFalse(message.isRequest());
      Assert.assertFalse(message.isError());
      Assert.assertNull(message.getCorrelationId());
      Assert.assertEquals("Event", message.getMessageType());
      Assert.assertEquals(fromAddress, message.getSource());
      Assert.assertEquals(toAddress, message.getDestination());
      Assert.assertEquals(500, message.getTimeToLive());
      long now = System.currentTimeMillis();
      long sentTime = message.getTimestamp().getTime();
      Assert.assertTrue(((now - 500) <= sentTime) && (sentTime <= now));
   }

   @Test
   public void testTimestamp() {
      PlatformMessage message = PlatformMessage.builder()
            .from(fromAddress)
            .to(toAddress)
            .withTimestamp(100)
            .withPayload(deviceEvent).create();
      Assert.assertTrue(message.isPlatform());
      Assert.assertFalse(message.isRequest());
      Assert.assertFalse(message.isError());
      Assert.assertNull(message.getCorrelationId());
      Assert.assertEquals("Event", message.getMessageType());
      Assert.assertEquals(fromAddress, message.getSource());
      Assert.assertEquals(toAddress, message.getDestination());
      Assert.assertEquals(-1, message.getTimeToLive());
      Assert.assertEquals(100, message.getTimestamp().getTime());
   }

   @Test
   public void testSetRequestMessageTrueWithCorrelation() {
      PlatformMessage message = PlatformMessage.builder()
            .from(fromAddress)
            .to(toAddress)
            .withCorrelationId("id")
            .isRequestMessage(true)
            .withPayload("MyType")
            .create();
   Assert.assertTrue(message.isPlatform());
   Assert.assertTrue(message.isRequest());
   Assert.assertFalse(message.isError());
   Assert.assertEquals("id", message.getCorrelationId());
   Assert.assertEquals("MyType", message.getMessageType());
   Assert.assertEquals(fromAddress, message.getSource());
   Assert.assertEquals(toAddress, message.getDestination());
   Assert.assertEquals(-1, message.getTimeToLive());
   long now = System.currentTimeMillis();
   long sentTime = message.getTimestamp().getTime();
   Assert.assertTrue(((now - 500) <= sentTime) && (sentTime <= now));
   }

   @Test
   public void testSetRequestMessageTrueWithoutCorrelation() {
      PlatformMessage message = PlatformMessage.builder()
            .from(fromAddress)
            .to(toAddress)
            .withoutCorrelationId()
            .isRequestMessage(true)
            .withPayload("MyType")
            .create();
   Assert.assertTrue(message.isPlatform());
   Assert.assertTrue(message.isRequest());
   Assert.assertFalse(message.isError());
   Assert.assertNull(message.getCorrelationId());
   Assert.assertEquals("MyType", message.getMessageType());
   Assert.assertEquals(fromAddress, message.getSource());
   Assert.assertEquals(toAddress, message.getDestination());
   Assert.assertEquals(-1, message.getTimeToLive());
   long now = System.currentTimeMillis();
   long sentTime = message.getTimestamp().getTime();
   Assert.assertTrue(((now - 500) <= sentTime) && (sentTime <= now));
   }

}

