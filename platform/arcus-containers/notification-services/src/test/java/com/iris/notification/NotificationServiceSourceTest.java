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
package com.iris.notification;

import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.util.concurrent.MoreExecutors;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.services.PlatformConstants;
import com.iris.notification.dispatch.Dispatcher;
import com.iris.platform.notification.Notification;

/**
 * RoleAuthorizer refuses note:* from clients, but hub-bridge runs no authorizer, so a hub could
 * have the platform email, text or call anyone.
 */
public class NotificationServiceSourceTest {
   private Dispatcher dispatcher;
   private NotificationService service;

   @Before
   public void setUp() {
      dispatcher = EasyMock.createStrictMock(Dispatcher.class);
      service = new NotificationService(EasyMock.createNiceMock(PlatformMessageBus.class), dispatcher, new NotificationServiceConfig(), MoreExecutors.newDirectExecutorService());
   }

   private static PlatformMessage notify(Address source) {
      MessageBody body = NotificationCapability.NotifyRequest.builder()
            .withPersonId(UUID.randomUUID().toString())
            .withPlaceId(UUID.randomUUID().toString())
            .withMsgKey("test.key")
            .withPriority(NotificationCapability.NotifyRequest.PRIORITY_LOW)
            .build();
      return PlatformMessage.buildRequest(body, source, Address.platformService(NotificationCapability.NAMESPACE)).create();
   }

   @Test
   public void testHubNotificationIsDropped() {
      EasyMock.replay(dispatcher);

      service.doHandleMessage(notify(Address.hubService("ABC-1234", "hub")));

      EasyMock.verify(dispatcher); // strict mock: nothing was dispatched
   }

   @Test
   public void testClientNotificationIsDropped() {
      EasyMock.replay(dispatcher);

      service.doHandleMessage(notify(Address.clientAddress("android", "1")));

      EasyMock.verify(dispatcher);
   }

   @Test
   public void testServiceNotificationIsDispatched() {
      dispatcher.dispatch(EasyMock.anyObject(Notification.class));
      EasyMock.expectLastCall();
      EasyMock.replay(dispatcher);

      service.doHandleMessage(notify(Address.platformService(PlatformConstants.SERVICE_PEOPLE)));

      EasyMock.verify(dispatcher);
   }

   @Test
   public void testDriverNotificationIsDispatched() {
      dispatcher.dispatch(EasyMock.anyObject(Notification.class));
      EasyMock.expectLastCall();
      EasyMock.replay(dispatcher);

      service.doHandleMessage(notify(Address.platformDriverAddress(UUID.randomUUID())));

      EasyMock.verify(dispatcher);
   }
}
