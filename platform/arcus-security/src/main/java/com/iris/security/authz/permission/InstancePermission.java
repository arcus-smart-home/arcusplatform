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

import java.util.Set;

import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.WildcardPermission;

public class InstancePermission extends WildcardPermission {

   private static final long serialVersionUID = 1594916464620804061L;

   static boolean isInstancePermission(String permission) {
      String parts[] = permission.split(":");
      return parts.length == 3 && !parts[2].equals("*");
   }

   final Set<String> capabilities;
   final Set<String> instanceIds;

   public InstancePermission(String wildcardString, boolean caseSensitive) {
      super(wildcardString, caseSensitive);
      instanceIds = getParts().get(2);
      capabilities = getParts().get(0);
   }

   public InstancePermission(String wildcardString) {
      super(wildcardString);
      instanceIds = getParts().get(2);
      capabilities = getParts().get(0);
   }

   public boolean shouldEvaluate(Permission p) {
      if(p instanceof InstancePermission) {
         InstancePermission ip = (InstancePermission) p;
         return instanceIds.containsAll(ip.instanceIds) && capabilities.containsAll(ip.capabilities);
      }
      return false;
   }

}

