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
package com.iris.agent.controller.hub.lights;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import com.iris.agent.hal.LEDState;
import com.iris.agent.hal.Model;

/**
 * The LED and button tables lost their backup-connection column with the cellular backup. Every
 * V2 combination of battery, authorized, pairing and connected still has a state of its own, and
 * the states are the ones the hub showed on its primary connection before.
 */
public class LEDConfigTest {
   private static final String V2 = Model.IH200.toString();
   private static final String V3 = Model.IH300.toString();

   @Test
   public void testEveryV2CombinationHasAState() {
      boolean[] values = { false, true };
      for (boolean battery : values) {
         for (boolean authorized : values) {
            for (boolean pairing : values) {
               for (boolean connected : values) {
                  assertNotEquals("battery=" + battery + " authorized=" + authorized + " pairing=" + pairing + " connected=" + connected,
                        LEDState.ALL_OFF, LEDConfig.get(V2, battery, authorized, pairing, connected));
               }
            }
         }
      }
   }

   @Test
   public void testV2StatesOnThePrimaryConnection() {
      assertEquals(LEDState.ASSOC_CONN_PRI, LEDConfig.get(V2, false, true, false, true));
      assertEquals(LEDState.ASSOC_CONN_PRI_BATT, LEDConfig.get(V2, true, true, false, true));
      assertEquals(LEDState.PAIRING_CONN_PRI, LEDConfig.get(V2, false, true, true, true));
      assertEquals(LEDState.UNASSOC_CONN_PRI, LEDConfig.get(V2, false, false, false, true));
      assertEquals(LEDState.PAIRING_DISCONN_BATT, LEDConfig.get(V2, true, true, true, false));
      assertEquals(LEDState.UNASSOC_DISCONN, LEDConfig.get(V2, false, false, false, false));
   }

   @Test
   public void testV3PersistentStates() {
      assertEquals(LEDState.INTERNET_IN_PROGRESS, LEDConfig.get(V3, false, false, false, false));
      assertEquals(LEDState.CLOUD_IN_PROGRESS, LEDConfig.get(V3, false, false, false, true));
      assertEquals(LEDState.ALL_OFF, LEDConfig.get(V3, false, true, false, true));
      assertEquals(LEDState.PAIRING_CONN_PRI, LEDConfig.get(V3, false, true, true, true));
   }

   @Test
   public void testV3ButtonPresses() {
      assertEquals(LEDState.BUTTON_PRESS_NORMAL, LEDConfig.INSTANCE.getButtonPressed(false, true, true));
      assertEquals(LEDState.BUTTON_PRESS_BATTERY, LEDConfig.INSTANCE.getButtonPressed(true, true, true));
      assertEquals(LEDState.BUTTON_PRESS_OFFLINE, LEDConfig.INSTANCE.getButtonPressed(false, false, false));
      assertEquals(LEDState.BUTTON_PRESS_OFFLINE_BATT, LEDConfig.INSTANCE.getButtonPressed(true, false, false));
   }
}
