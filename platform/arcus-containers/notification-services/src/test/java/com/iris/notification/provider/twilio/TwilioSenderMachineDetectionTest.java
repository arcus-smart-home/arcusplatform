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
package com.iris.notification.provider.twilio;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;

/**
 * twilio.machineDetection replaced twilio.ifmachine, and the two do not take
 * the same values. These cover the rejection happening at injection time, so a
 * mechanically renamed setting fails at startup rather than on the first call.
 */
public class TwilioSenderMachineDetectionTest {

   private static class Config extends AbstractModule {
      private final String machineDetection;

      Config(String machineDetection) {
         this.machineDetection = machineDetection;
      }

      @Override
      protected void configure() {
         bindConstant().annotatedWith(Names.named("twilio.account.sid")).to("ACtest");
         bindConstant().annotatedWith(Names.named("twilio.account.auth")).to("authtest");
         bindConstant().annotatedWith(Names.named("twilio.account.from")).to("+15555550100");
         bindConstant().annotatedWith(Names.named("twilio.callback.serverurl")).to("https://callback.invalid");
         if(machineDetection != null) {
            bindConstant().annotatedWith(Names.named("twilio.machineDetection")).to(machineDetection);
         }
      }
   }

   private TwilioSender senderWith(String machineDetection) {
      Injector injector = Guice.createInjector(new Config(machineDetection));
      return injector.getInstance(TwilioSender.class);
   }

   @Test
   public void testUnsetIsAllowed() {
      assertNotNull(senderWith(null));
   }

   @Test
   public void testEnableIsAllowed() {
      assertNotNull(senderWith("Enable"));
   }

   @Test
   public void testDetectMessageEndIsAllowed() {
      assertNotNull(senderWith("DetectMessageEnd"));
   }

   @Test
   public void testLegacyIfMachineValuesAreRejected() {
      // the values the old twilio.ifmachine setting took
      for(String legacy : new String[] { "Continue", "Hangup" }) {
         try {
            senderWith(legacy);
            fail("expected [" + legacy + "] to be rejected at injection time");
         }
         catch(Exception e) {
            assertTrue("expected an IllegalArgumentException naming the setting, got: " + e,
                  causeChainMentions(e, "twilio.machineDetection"));
         }
      }
   }

   private boolean causeChainMentions(Throwable t, String text) {
      for(Throwable c = t; c != null; c = c.getCause()) {
         if(c instanceof IllegalArgumentException && c.getMessage() != null && c.getMessage().contains(text)) {
            return true;
         }
      }
      return false;
   }
}
