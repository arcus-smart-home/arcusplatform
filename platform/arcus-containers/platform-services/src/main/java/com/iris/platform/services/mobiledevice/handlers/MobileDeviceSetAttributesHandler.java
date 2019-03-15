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
package com.iris.platform.services.mobiledevice.handlers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.core.dao.MobileDeviceDAO;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.model.MobileDevice;
import com.iris.platform.services.AbstractSetAttributesPlatformMessageHandler;
import com.iris.population.PlacePopulationCacheManager;

@Singleton
public class MobileDeviceSetAttributesHandler extends AbstractSetAttributesPlatformMessageHandler<MobileDevice> {

   private final MobileDeviceDAO mobileDeviceDao;

   @Inject
   public MobileDeviceSetAttributesHandler(
         CapabilityRegistry capabilityRegistry,
         BeanAttributesTransformer<MobileDevice> mobileDeviceTransformer,
         PlatformMessageBus platformBus,
         MobileDeviceDAO mobileDeviceDao,
         PlacePopulationCacheManager populationCacheMgr) {

      super(capabilityRegistry, mobileDeviceTransformer, platformBus, populationCacheMgr);
      this.mobileDeviceDao = mobileDeviceDao;
   }

   @Override
   protected void save(MobileDevice bean) {
      mobileDeviceDao.save(bean);
   }
}

