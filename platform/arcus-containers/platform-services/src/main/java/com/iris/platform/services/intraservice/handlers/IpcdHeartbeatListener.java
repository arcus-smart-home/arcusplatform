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
package com.iris.platform.services.intraservice.handlers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.platform.AbstractPlatformMessageListener;
import com.iris.core.platform.IntraServiceMessageBus;
import com.iris.messages.PlatformMessage;
import com.iris.messages.service.IpcdService;
import com.iris.platform.services.ipcd.registry.IpcdRegistry;

@Singleton
public class IpcdHeartbeatListener extends AbstractPlatformMessageListener {

   private final IpcdRegistry registry;

   @Inject
   public IpcdHeartbeatListener(IntraServiceMessageBus bus, IpcdRegistry registry) {
      super(bus);
      this.registry = registry;
   }

   @Override
   protected void onStart() {
      super.onStart();
      addListeners(IpcdService.ADDRESS);
   }

   @Override
   public void handleMessage(PlatformMessage message) {
      super.handleMessage(message);
   }

   @Override
   protected void handleEvent(PlatformMessage message) {
      if(IpcdService.DeviceHeartBeatEvent.NAME.equals(message.getMessageType())) {
         registry.onHeartBeat(message);
      }
   }
}

