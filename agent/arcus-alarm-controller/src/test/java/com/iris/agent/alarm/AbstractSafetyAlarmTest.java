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
package com.iris.agent.alarm;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.google.common.collect.ImmutableSet;
import com.iris.messages.capability.HubAlarmCapability;

/**
 * Covers the decision behind {@link AbstractSafetyAlarm#reconcile()}. The
 * alarms themselves need the hub attribute service and HAL, so they are not
 * constructed here.
 */
@RunWith(JUnit4.class)
public class AbstractSafetyAlarmTest {
   private static final Set<String> NONE = ImmutableSet.of();
   private static final Set<String> LEAKING = ImmutableSet.of("PROT:ZIGB-ABC-1234:AAAAAAAAAAAAAAAAAAAAAAAAAAA=");

   @Test
   public void testReadyWithTriggeredDeviceIsMissedAlert() {
      // sensor LEAK on the hub, water alarm READY with no trigger recorded
      Assert.assertTrue(AbstractSafetyAlarm.isMissedAlert(HubAlarmCapability.COALERTSTATE_READY, LEAKING));
   }

   @Test
   public void testReadyWithNothingTriggeredIsFine() {
      Assert.assertFalse(AbstractSafetyAlarm.isMissedAlert(HubAlarmCapability.COALERTSTATE_READY, NONE));
   }

   @Test
   public void testClearingWithTriggeredDeviceIsNotRealerted() {
      // cancelled and waiting for the sensor to clear; re-alerting would make cancel useless
      Assert.assertFalse(AbstractSafetyAlarm.isMissedAlert(HubAlarmCapability.COALERTSTATE_CLEARING, LEAKING));
   }

   @Test
   public void testOtherStatesAreLeftAlone() {
      for (String state : new String[] {
            HubAlarmCapability.COALERTSTATE_INACTIVE,
            HubAlarmCapability.COALERTSTATE_ALERT,
            HubAlarmCapability.COALERTSTATE_PENDING_CLEAR
      }) {
         Assert.assertFalse(state, AbstractSafetyAlarm.isMissedAlert(state, LEAKING));
      }
   }
}
