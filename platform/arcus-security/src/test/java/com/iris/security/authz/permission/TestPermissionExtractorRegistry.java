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
package com.iris.security.authz.permission;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.MessagesModule;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.security.authz.AuthzFixtures;
import com.iris.security.authz.AuthzModule;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

@Modules({MessagesModule.class, AuthzModule.class})
public class TestPermissionExtractorRegistry extends IrisTestCase {

   @Inject
   private PermissionExtractorRegistry registry;

   @Test
   public void testDeviceCommand() {
      PermissionExtractor extractor = registry.getPermissionExtractor(AuthzFixtures.createSwitchOn(UUID.randomUUID()));
      assertTrue(extractor instanceof DefaultPermissionExtractor);
   }

   @Test
   public void testGetAttributes() {
      PermissionExtractor extractor = registry.getPermissionExtractor(AuthzFixtures.createGetAttributes(UUID.randomUUID(), "dev:name"));
      assertTrue(extractor instanceof GetAttributesPermissionExtractor);
   }

   @Test
   public void testSetAttributes() {
      Map<String,Object> attributes = new HashMap<>();
      attributes.put("dev:name", "foobar");
      PermissionExtractor extractor = registry.getPermissionExtractor(AuthzFixtures.createSetAttributes(UUID.randomUUID(), attributes));
      assertTrue(extractor instanceof SetAttributesPermissionExtractor);
   }

   public void testUnknown() {
      MessageBody response = MessageBody.buildMessage(MessageConstants.MSG_ACCOUNT_LIST_DEVICES, ImmutableMap.of("devices", Collections.emptyList()));
      PlatformMessage msg = PlatformMessage.create(response, Address.platformService("devices"), Address.clientAddress("android", "1"), "foobar");
      PermissionExtractor extractor = registry.getPermissionExtractor(msg);
      assertTrue(extractor instanceof DefaultPermissionExtractor);
   }

}

