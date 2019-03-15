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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import com.google.inject.Inject;
import com.iris.messages.MessagesModule;
import com.iris.messages.PlatformMessage;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

@Modules({MessagesModule.class, AuthzModule.class})
public class TestPermissionsAuthorizer_Authorization extends IrisTestCase {

   @Inject
   private Authorizer authorizer;

   @Test
   public void testGARejectionNotAllowedByWildcard() {
      AuthorizationContext context = AuthzFixtures.createContext(AuthzFixtures.placeId, "dev:w,x:*");
      UUID devId = UUID.randomUUID();
      PlatformMessage gaMsg = AuthzFixtures.createGetAttributes(devId, "dev:name");
      assertFalse(authorizer.isAuthorized(context, AuthzFixtures.placeId.toString(), gaMsg));

      gaMsg = AuthzFixtures.createGetAttributes(devId, "dev");
      assertFalse(authorizer.isAuthorized(context, AuthzFixtures.placeId.toString(), gaMsg));
   }

   @Test
   public void testGARejectedOnSpecificAllowedOnOthers() {
      UUID devId = UUID.randomUUID();
      UUID devId2 = UUID.randomUUID();
      AuthorizationContext context = AuthzFixtures.createContext(AuthzFixtures.placeId, "dev:w,x:" + devId.toString(), "dev:*:*");

      // specific permission should reject this one
      PlatformMessage gaMsg = AuthzFixtures.createGetAttributes(devId, "dev:name");
      assertFalse(authorizer.isAuthorized(context, AuthzFixtures.placeId.toString(), gaMsg));

      gaMsg = AuthzFixtures.createGetAttributes(devId, "dev");
      assertFalse(authorizer.isAuthorized(context, AuthzFixtures.placeId.toString(), gaMsg));

      // general permission should allow this one
      gaMsg = AuthzFixtures.createGetAttributes(devId2, "dev:name");
      assertTrue(authorizer.isAuthorized(context, AuthzFixtures.placeId.toString(), gaMsg));

      gaMsg = AuthzFixtures.createGetAttributes(devId2, "dev");
      assertTrue(authorizer.isAuthorized(context, AuthzFixtures.placeId.toString(), gaMsg));
   }

   @Test
   public void testGAAllowedOnSpecificRejectedOnOthers() {
      UUID devId = UUID.randomUUID();
      UUID devId2 = UUID.randomUUID();
      AuthorizationContext context = AuthzFixtures.createContext(AuthzFixtures.placeId, "dev:*:" + devId.toString(), "dev:w,x:*");

      // specific permission should allow this one
      PlatformMessage gaMsg = AuthzFixtures.createGetAttributes(devId, "dev:name");
      assertTrue(authorizer.isAuthorized(context, AuthzFixtures.placeId.toString(), gaMsg));

      gaMsg = AuthzFixtures.createGetAttributes(devId, "dev");
      assertTrue(authorizer.isAuthorized(context, AuthzFixtures.placeId.toString(), gaMsg));

      // general permission should prevent this one
      gaMsg = AuthzFixtures.createGetAttributes(devId2, "dev:name");
      assertFalse(authorizer.isAuthorized(context, AuthzFixtures.placeId.toString(), gaMsg));

      gaMsg = AuthzFixtures.createGetAttributes(devId2, "dev");
      assertFalse(authorizer.isAuthorized(context, AuthzFixtures.placeId.toString(), gaMsg));
   }

   @Test
   public void testGARejectedOnMixedNamespace() {
      UUID devId = UUID.randomUUID();
      AuthorizationContext context = AuthzFixtures.createContext(AuthzFixtures.placeId, "dev:*:" + devId.toString(), "cont:w,x:*");
      PlatformMessage gaMsg = AuthzFixtures.createGetAttributes(devId, "dev:name", "cont:contact");
      assertFalse(authorizer.isAuthorized(context, AuthzFixtures.placeId.toString(), gaMsg));
   }

   @Test
   public void testGAAllowedOnMixedNamespace() {
      UUID devId = UUID.randomUUID();
      AuthorizationContext context = AuthzFixtures.createContext(AuthzFixtures.placeId, "dev:*:*", "cont:*:*");
      PlatformMessage gaMsg = AuthzFixtures.createGetAttributes(devId, "dev:name", "cont:contact");
      assertTrue(authorizer.isAuthorized(context, AuthzFixtures.placeId.toString(), gaMsg));
   }

   @Test
   public void testSARejectionNotAllowedByWildcard() {
      AuthorizationContext context = AuthzFixtures.createContext(AuthzFixtures.placeId, "dev:r,x:*");
      UUID devId = UUID.randomUUID();
      Map<String,Object> attributes = new HashMap<>();
      attributes.put("dev:name", "foobar");
      PlatformMessage saMsg = AuthzFixtures.createSetAttributes(devId, attributes);
      assertFalse(authorizer.isAuthorized(context, AuthzFixtures.placeId.toString(), saMsg));
   }

   @Test
   public void testSARejectedOnSpecificAllowedOnOthers() {
      UUID devId = UUID.randomUUID();
      UUID devId2 = UUID.randomUUID();

      AuthorizationContext context = AuthzFixtures.createContext(AuthzFixtures.placeId, "dev:r,x:" + devId.toString(), "dev:*:*");

      Map<String,Object> attributes = new HashMap<>();
      attributes.put("dev:name", "foobar");

      // specific permission should reject this one
      PlatformMessage saMsg = AuthzFixtures.createSetAttributes(devId, attributes);
      assertFalse(authorizer.isAuthorized(context, AuthzFixtures.placeId.toString(), saMsg));

      // general permission should allow this one
      saMsg = AuthzFixtures.createSetAttributes(devId2, attributes);
      assertTrue(authorizer.isAuthorized(context, AuthzFixtures.placeId.toString(), saMsg));
   }

   @Test
   public void testSAAllowedOnSpecificRejectedOnOthers() {
      UUID devId = UUID.randomUUID();
      UUID devId2 = UUID.randomUUID();

      Map<String,Object> attributes = new HashMap<>();
      attributes.put("dev:name", "foobar");

      AuthorizationContext context = AuthzFixtures.createContext(AuthzFixtures.placeId, "dev:*:" + devId.toString(), "dev:r,x:*");

      // specific permission should allow this one
      PlatformMessage saMsg = AuthzFixtures.createSetAttributes(devId, attributes);
      assertTrue(authorizer.isAuthorized(context, AuthzFixtures.placeId.toString(), saMsg));

      // general permission should prevent this one
      saMsg = AuthzFixtures.createSetAttributes(devId2, attributes);
      assertFalse(authorizer.isAuthorized(context, AuthzFixtures.placeId.toString(), saMsg));
   }

   @Test
   public void testSARejectedOnMixedNamespace() {
      UUID devId = UUID.randomUUID();

      AuthorizationContext context = AuthzFixtures.createContext(AuthzFixtures.placeId, "dev:*:" + devId.toString(), "swit:r:*");

      Map<String,Object> attributes = new HashMap<>();
      attributes.put("dev:name", "foobar");
      attributes.put("swit:state", "ON");

      PlatformMessage saMsg = AuthzFixtures.createSetAttributes(devId, attributes);
      assertFalse(authorizer.isAuthorized(context, AuthzFixtures.placeId.toString(), saMsg));
   }

   @Test
   public void testSAAllowedOnMixedNamespace() {
      UUID devId = UUID.randomUUID();

      AuthorizationContext context = AuthzFixtures.createContext(AuthzFixtures.placeId, "dev:*:*", "swit:*:*");

      Map<String,Object> attributes = new HashMap<>();
      attributes.put("dev:name", "foobar");
      attributes.put("swit:state", "ON");

      PlatformMessage saMsg = AuthzFixtures.createSetAttributes(devId, attributes);
      assertTrue(authorizer.isAuthorized(context, AuthzFixtures.placeId.toString(), saMsg));
   }

   @Test
   public void testCommandRejectionNotAllowedByWildcard() {
      AuthorizationContext context = AuthzFixtures.createContext(AuthzFixtures.placeId, "swit:r,w:*");
      UUID devId = UUID.randomUUID();
      PlatformMessage cmdMsg = AuthzFixtures.createSwitchOn(devId);
      assertFalse(authorizer.isAuthorized(context, AuthzFixtures.placeId.toString(), cmdMsg));
   }

   @Test
   public void testCommandRejectedOnSpecificAllowedOnOthers() {
      UUID devId = UUID.randomUUID();
      UUID devId2 = UUID.randomUUID();

      AuthorizationContext context = AuthzFixtures.createContext(AuthzFixtures.placeId, "swit:r,w:" + devId.toString(), "swit:*:*");

      // specific permission should reject this one
      PlatformMessage cmdMsg = AuthzFixtures.createSwitchOn(devId);
      assertFalse(authorizer.isAuthorized(context, AuthzFixtures.placeId.toString(), cmdMsg));

      // general permission should allow this one
      cmdMsg = AuthzFixtures.createSwitchOn(devId2);
      assertTrue(authorizer.isAuthorized(context, AuthzFixtures.placeId.toString(), cmdMsg));
   }

   @Test
   public void testCommandAllowedOnSpecificRejectedOnOthers() {
      UUID devId = UUID.randomUUID();
      UUID devId2 = UUID.randomUUID();

      AuthorizationContext context = AuthzFixtures.createContext(AuthzFixtures.placeId, "swit:*:" + devId.toString(), "swit:r,w:*");

      // specific permission should allow this one
      PlatformMessage cmdMsg = AuthzFixtures.createSwitchOn(devId);
      assertTrue(authorizer.isAuthorized(context, AuthzFixtures.placeId.toString(), cmdMsg));

      // general permission should prevent this one
      cmdMsg = AuthzFixtures.createSwitchOn(devId2);
      assertFalse(authorizer.isAuthorized(context, AuthzFixtures.placeId.toString(), cmdMsg));
   }

   @Test
   public void testMessageRejectedIfNoPlacePermissions() {
      UUID devId = UUID.randomUUID();
      AuthorizationContext context = AuthzFixtures.createContext(UUID.randomUUID(), "swit:*:*");
      PlatformMessage cmdMsg = AuthzFixtures.createSwitchOn(devId);
      assertFalse(authorizer.isAuthorized(context, AuthzFixtures.placeId.toString(), cmdMsg));
   }

   @Test
   public void testRejectedOnOneCapabilityAllowedOnAnother() {
      UUID devId = UUID.randomUUID();

      AuthorizationContext context = AuthzFixtures.createContext(AuthzFixtures.placeId, "swit:x:" + devId.toString(), "dev:*:*");

      Map<String,Object> attributes = new HashMap<>();
      attributes.put("dev:name", "foobar");
      attributes.put("swit:state", "ON");

      PlatformMessage saMsg = AuthzFixtures.createSetAttributes(devId, attributes);

      // reject because device specific rule prevents switch set
      assertFalse(authorizer.isAuthorized(context, AuthzFixtures.placeId.toString(), saMsg));

      attributes.remove("swit:state");

      saMsg = AuthzFixtures.createSetAttributes(devId, attributes);

      // allowed because despite specific rule that prevents switch, this should fall back to the
      // dev:*:* rule that allow write on device base
      assertTrue(authorizer.isAuthorized(context, AuthzFixtures.placeId.toString(), saMsg));
   }

   @Test
   public void testNoSessionPlaceRejects() {
      AuthorizationContext context = AuthzFixtures.createContext(AuthzFixtures.placeId, "*:*:*");

      PlatformMessage msg = AuthzFixtures.createGetAttributes(UUID.randomUUID(), "swit:switch");
      assertFalse(authorizer.isAuthorized(context, null, msg));
   }

   @Test
   public void testSessionMismatchRejected() {
      AuthorizationContext context = AuthzFixtures.createContext(UUID.randomUUID(), "*:*:*");

      PlatformMessage msg = AuthzFixtures.createGetAttributes(UUID.randomUUID(), "swit:switch");
      assertFalse(authorizer.isAuthorized(context, AuthzFixtures.placeId.toString(), msg));
   }
}

