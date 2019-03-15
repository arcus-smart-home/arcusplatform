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

import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.junit.Test;

public class TestPermissionFactory {

   @Test
   public void testCreatesInstancePermission() {
      UUID instanceId = UUID.randomUUID();
      Permission permission = PermissionFactory.createPermission("swit:x:" + instanceId.toString());
      assertTrue(permission instanceof InstancePermission);
   }

   @Test
   public void testCreatesWildcardPermission() {
      Permission permission = PermissionFactory.createPermission("swit:*:*");
      assertTrue(permission instanceof WildcardPermission);
   }
}

