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
package com.iris.platform.alarm.service;

import java.util.concurrent.ExecutorService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.core.messaging.MessageListener;
import com.iris.core.platform.AbstractPlatformService;
import com.iris.core.platform.PlatformDispatcherFactory;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.AddressMatcher;
import com.iris.messages.address.AddressMatchers;
import com.iris.messages.capability.NotificationCapability;

@Singleton
public class AlarmService extends AbstractPlatformService {

   public static final String NAME_EXECUTOR_POOL = "threadpool.alarm";

   public static final Address SERVICE_ADDRESS = Address.platformService(com.iris.messages.service.AlarmService.NAMESPACE);
   private static final AddressMatcher NOTIFICATION_ADDRESS = AddressMatchers.fromString("SERV:" + NotificationCapability.NAMESPACE + ":*");

   private MessageListener<PlatformMessage> dispatcher;

   @Inject
   public AlarmService(
      PlatformMessageBus platformBus,
      @Named(NAME_EXECUTOR_POOL)ExecutorService executor,
      PlatformDispatcherFactory dispatcherFactory,
      AddAlarmHandler updateIncidentHandler,
      CancelAlertHandler cancelAlertHandler,
      IvrNotificationAcknowledgedHandler ivrNotificationAcknowledgedHandler
   ) {
      super(platformBus, SERVICE_ADDRESS, executor);
      this.dispatcher = dispatcherFactory
         .buildDispatcher()
         .addAnnotatedHandler(updateIncidentHandler)
         .addAnnotatedHandler(cancelAlertHandler)
         .addAnnotatedHandler(ivrNotificationAcknowledgedHandler)
         .build();
   }

   @Override
   protected void onStart() {
      super.onStart();
      addListeners(AddressMatchers.equals(SERVICE_ADDRESS));
      addBroadcastMessageListeners(NOTIFICATION_ADDRESS);
   }

   @Override
   protected void doHandleMessage(PlatformMessage message) {
      dispatcher.onMessage(message);
   }

}

