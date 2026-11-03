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

import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * authz.algorithm=none can't enforce the risky operations switch, so configuring
 * both has to stop the bridge rather than silently lift the switch.
 */
public class AuthzModuleNoneTest {

   @Test
   public void testNoDisabledOperationsIsAllowed() {
      AuthzModule.assertNoDisabledRiskyOperations(null);
      AuthzModule.assertNoDisabledRiskyOperations("");
      AuthzModule.assertNoDisabledRiskyOperations("   ");
   }

   @Test
   public void testDisabledOperationRefusesToStart() {
      assertRefused("hub.delete");
      assertRefused("email.change,hub.delete");
      assertRefused("all");
   }

   private void assertRefused(String disabled) {
      try {
         AuthzModule.assertNoDisabledRiskyOperations(disabled);
         fail("expected " + disabled + " to be refused with authz.algorithm=none");
      }
      catch(IllegalStateException e) {
         // expected
      }
   }
}
