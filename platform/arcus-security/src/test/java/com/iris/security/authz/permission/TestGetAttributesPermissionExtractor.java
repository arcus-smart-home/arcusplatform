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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.shiro.authz.Permission;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.iris.messages.MessagesModule;
import com.iris.messages.PlatformMessage;
import com.iris.security.authz.AuthzFixtures;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

@Modules(MessagesModule.class)
public class TestGetAttributesPermissionExtractor extends IrisTestCase {

   private GetAttributesPermissionExtractor extractor = new GetAttributesPermissionExtractor();

   @Test
   public void testExtractPermissionsGetAttributesSingleSpecificName() {
      UUID devId = UUID.randomUUID();
      PlatformMessage gaMsg = AuthzFixtures.createGetAttributes(devId, "dev:name");
      List<Permission> requiredPermissions = extractor.extractRequiredPermissions(gaMsg);
      assertEquals(1, requiredPermissions.size());
      assertTrue(requiredPermissions.get(0) instanceof InstancePermission);
      InstancePermission ip = (InstancePermission) requiredPermissions.get(0);
      assertEquals(ImmutableSet.of("dev"), ip.capabilities);
      assertEquals(ImmutableSet.of(devId.toString()), ip.instanceIds);
      assertEquals("dev:r:" + devId.toString(), ip.toString());
   }

   @Test
   public void testExtractPermissionGetAttributesSingleNamespace() {
      UUID devId = UUID.randomUUID();
      PlatformMessage gaMsg = AuthzFixtures.createGetAttributes(devId, "dev");
      List<Permission> requiredPermissions = extractor.extractRequiredPermissions(gaMsg);
      assertEquals(1, requiredPermissions.size());
      assertTrue(requiredPermissions.get(0) instanceof InstancePermission);
      InstancePermission ip = (InstancePermission) requiredPermissions.get(0);
      assertEquals(ImmutableSet.of("dev"), ip.capabilities);
      assertEquals(ImmutableSet.of(devId.toString()), ip.instanceIds);
      assertEquals("dev:r:" + devId.toString(), ip.toString());
   }

   @Test
   public void testExtractPermissionsSeveralSpecificNames() {
      UUID devId = UUID.randomUUID();
      PlatformMessage gaMsg = AuthzFixtures.createGetAttributes(devId, "dev:name", "cont:contact", "temp:temperature");
      List<Permission> requiredPermissions = extractor.extractRequiredPermissions(gaMsg);
      assertEquals(3, requiredPermissions.size());
      Set<String> permissionsAsStrings = new HashSet<>();
      for(Permission perm : requiredPermissions) { permissionsAsStrings.add(perm.toString()); }
      assertTrue(permissionsAsStrings.contains("dev:r:" + devId.toString()));
      assertTrue(permissionsAsStrings.contains("cont:r:" + devId.toString()));
      assertTrue(permissionsAsStrings.contains("temp:r:" + devId.toString()));
   }

   @Test
   public void testExtractPermissionsMixedNamespacesAndNames() {
      UUID devId = UUID.randomUUID();
      PlatformMessage gaMsg = AuthzFixtures.createGetAttributes(devId, "dev", "cont:contact", "temp:temperature");
      List<Permission> requiredPermissions = extractor.extractRequiredPermissions(gaMsg);
      assertEquals(3, requiredPermissions.size());
      Set<String> permissionsAsStrings = new HashSet<>();
      for(Permission perm : requiredPermissions) { permissionsAsStrings.add(perm.toString()); }
      assertTrue(permissionsAsStrings.contains("dev:r:" + devId.toString()));
      assertTrue(permissionsAsStrings.contains("cont:r:" + devId.toString()));
      assertTrue(permissionsAsStrings.contains("temp:r:" + devId.toString()));
   }

}

