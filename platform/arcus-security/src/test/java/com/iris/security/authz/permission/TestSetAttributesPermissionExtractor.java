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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.shiro.authz.Permission;
import org.junit.Test;

import com.iris.messages.MessagesModule;
import com.iris.messages.PlatformMessage;
import com.iris.security.authz.AuthzFixtures;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

@Modules(MessagesModule.class)
public class TestSetAttributesPermissionExtractor extends IrisTestCase {

   private SetAttributesPermissionExtractor extractor = new SetAttributesPermissionExtractor();

   @Test
   public void testExtractPermissionsSetAttributesSingleAttribute() {
      UUID devId = UUID.randomUUID();
      Map<String,Object> attributes = new HashMap<>();
      attributes.put("dev:name", "foobar");
      PlatformMessage saMsg = AuthzFixtures.createSetAttributes(devId, attributes);
      List<Permission> requiredPermissions = extractor.extractRequiredPermissions(saMsg);
      assertEquals(1, requiredPermissions.size());
      assertEquals("dev:w:" + devId.toString(), requiredPermissions.get(0).toString());
   }

   @Test
   public void testExtractPermissionsSetAttributesMixedAttributes() {
      UUID devId = UUID.randomUUID();
      Map<String,Object> attributes = new HashMap<>();
      attributes.put("dev:name", "foobar");
      attributes.put("swit:state", "ON");
      PlatformMessage saMsg = AuthzFixtures.createSetAttributes(devId, attributes);
      List<Permission> requiredPermissions = extractor.extractRequiredPermissions(saMsg);
      assertEquals(2, requiredPermissions.size());
      Set<String> permissionsAsStrings = new HashSet<>();
      for(Permission perm : requiredPermissions) { permissionsAsStrings.add(perm.toString()); }
      assertTrue(permissionsAsStrings.contains("dev:w:" + devId.toString()));
      assertTrue(permissionsAsStrings.contains("swit:w:" + devId.toString()));
   }

}

