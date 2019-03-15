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
package com.iris.platform.services.mobiledevice;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.core.dao.MobileDeviceDAO;
import com.iris.core.platform.ContextualEventMessageHandler;
import com.iris.core.platform.ContextualPlatformMessageDispatcher;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.core.platform.PlatformService;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.model.MobileDevice;
import com.iris.messages.services.PlatformConstants;

@Singleton
public class MobileDeviceService extends ContextualPlatformMessageDispatcher<MobileDevice> implements PlatformService {
   public static final String PROP_THREADPOOL = "platform.service.mobiledevice.threadpool";

   private static final Address address = Address.platformService(PlatformConstants.SERVICE_MOBILEDEVICES);

   private final MobileDeviceDAO mobileDeviceDao;

   @Inject
   public MobileDeviceService(
         PlatformMessageBus platformBus,
         @Named(PROP_THREADPOOL) Executor executor,
         Set<ContextualRequestMessageHandler<MobileDevice>> handlers,
         MobileDeviceDAO mobileDeviceDao
   ) {
      super(platformBus, executor, handlers, Collections.<ContextualEventMessageHandler<MobileDevice>>emptySet());
      this.mobileDeviceDao = mobileDeviceDao;
   }

   @Override
   public Address getAddress() {
      return address;
   }

   @Override
   public void handleMessage(PlatformMessage message) {
      super.handleMessage(message);
   }

   @Override
   protected MobileDevice loadContext(Object contextId, Integer qualifier) {
      if(!(contextId instanceof UUID)) {
         throw new IllegalArgumentException("The context ID must be a UUID");
      }
      if(qualifier == null) {
         throw new IllegalArgumentException("The context for a mobile device must be qualified by a numerical identifier within the context");
      }
      return mobileDeviceDao.findOne((UUID) contextId, qualifier);
   }
}

