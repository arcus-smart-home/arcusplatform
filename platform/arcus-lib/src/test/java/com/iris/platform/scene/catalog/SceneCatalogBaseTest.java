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
package com.iris.platform.scene.catalog;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.slf4j.LoggerFactory;

import com.iris.common.rule.simple.SimpleContext;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.test.IrisTestCase;
import com.iris.util.IrisCollections;

public class SceneCatalogBaseTest extends IrisTestCase {

   protected SimpleContext context;
   protected Model account;

   @Before
   public void setUp() throws Exception {
      super.setUp();
      context = new SimpleContext(UUID.randomUUID(), Address.platformService(UUID.randomUUID(), "rule"), LoggerFactory.getLogger("rules." + getClass().getSimpleName()));
      account = addAccount();
   }

   /**
    * Adds a device model to the rule and action contexts.  The primaryType will
    * be used for devTypeHint and added to the capabilities.
    * @param primaryType
    * @return
    */
   protected SimpleModel addDevice(String... primaryType) {
      SimpleModel model = creteBasicDeviceModel(primaryType);
      context.putModel(model);
      
      return model;
   }
   
   protected SimpleModel addDevice(Map<String, Object> attributes, String... primaryType) {
      SimpleModel model = creteBasicDeviceModel(primaryType);
      model.update(attributes);
      context.putModel(model);
      
      return model;
   }
   
   private SimpleModel creteBasicDeviceModel(String... primaryType) {
      UUID id = UUID.randomUUID();
      SimpleModel model = new SimpleModel();
      model.setAttribute(Capability.ATTR_ID, id.toString());
      model.setAttribute(Capability.ATTR_TYPE, DeviceCapability.NAMESPACE);
      model.setAttribute(Capability.ATTR_ADDRESS, Address.platformDriverAddress(id).getRepresentation());
      Set<String> capabilities = IrisCollections.setOf(Capability.NAMESPACE, DeviceCapability.NAMESPACE, DeviceAdvancedCapability.NAMESPACE);
      capabilities.addAll(Arrays.asList(primaryType));
      model.setAttribute(Capability.ATTR_CAPS, capabilities);
      model.setAttribute(Capability.ATTR_TAGS, new HashSet<String>());
      model.setAttribute(DeviceCapability.ATTR_DEVTYPEHINT, primaryType);
      return model;
   }
   
   protected SimpleModel addAccount() {
      UUID id = UUID.randomUUID();
      SimpleModel model = new SimpleModel();
      model.setAttribute(Capability.ATTR_ID, id.toString());
      model.setAttribute(Capability.ATTR_TYPE, AccountCapability.NAMESPACE);
      model.setAttribute(Capability.ATTR_ADDRESS, Address.platformService(id,AccountCapability.NAMESPACE).getRepresentation());
      model.setAttribute(Capability.ATTR_CAPS, IrisCollections.setOf(Capability.NAMESPACE, AccountCapability.NAMESPACE));
      context.putModel(model);
      return model;
   }
}

