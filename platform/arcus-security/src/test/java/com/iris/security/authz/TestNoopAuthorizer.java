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
package com.iris.security.authz;

import java.util.UUID;

import org.junit.Test;

import com.google.inject.Inject;
import com.iris.messages.MessagesModule;
import com.iris.messages.PlatformMessage;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

@Modules({MessagesModule.class, AuthzModule.class})
public class TestNoopAuthorizer extends IrisTestCase {

   @Inject
   private Authorizer authorizer;

   @Override
   public void setUp() throws Exception {
      System.setProperty(AuthzModule.AUTHZ_ALGORITHM_PROP, AuthzModule.AUTHZ_ALGORITHM_NONE);
      super.setUp();
   }

   @Override
   public void tearDown() throws Exception {
      System.setProperty(AuthzModule.AUTHZ_ALGORITHM_PROP, AuthzModule.AUTHZ_ALGORITHM_PERMISSIONS);
      super.tearDown();
   }

   @Test
   public void testIsNoopAuthorizer() {
      assertTrue(authorizer instanceof NoopAuthorizer);
   }

   @Test
   public void testPermitsMessageEvenWithPrivsThatWouldDeny() {
      AuthorizationContext context = AuthzFixtures.createContext(AuthzFixtures.placeId, "dev:w,x:*");

      UUID devId = UUID.randomUUID();
      PlatformMessage gaMsg = AuthzFixtures.createGetAttributes(devId, "dev:name");

      // under the permissions authorizer this would be denied because the user doesn't have r privs
      assertTrue(authorizer.isAuthorized(context, AuthzFixtures.placeId.toString(), gaMsg));
   }
}

