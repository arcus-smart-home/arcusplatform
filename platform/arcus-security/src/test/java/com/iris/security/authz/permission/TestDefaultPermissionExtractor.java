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

import java.util.List;
import java.util.UUID;

import org.apache.shiro.authz.Permission;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.iris.messages.MessagesModule;
import com.iris.security.authz.AuthzFixtures;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

@Modules(MessagesModule.class)
public class TestDefaultPermissionExtractor extends IrisTestCase {

   private PermissionExtractor extractor = new DefaultPermissionExtractor();

   @Test
   public void testExtractPermissionsDeviceCommand() {
      UUID devId = UUID.randomUUID();
      List<Permission> requiredPermissions = extractor.extractRequiredPermissions(AuthzFixtures.createSwitchOn(devId));
      assertEquals(1, requiredPermissions.size());
      assertTrue(requiredPermissions.get(0) instanceof InstancePermission);
      InstancePermission ip = (InstancePermission) requiredPermissions.get(0);
      assertEquals(ImmutableSet.of("swit"), ip.capabilities);
      assertEquals(ImmutableSet.of(devId.toString()), ip.instanceIds);
      assertEquals("swit:x:" + devId.toString(), ip.toString());
   }

}

