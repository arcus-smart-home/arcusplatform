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
package com.iris.platform.alarm.notification.strategy;

import java.util.List;
import java.util.UUID;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.iris.common.alarm.AlertType;
import com.iris.messages.capability.NotificationCapability;
import com.iris.platform.alarm.incident.AlarmIncident;
import com.iris.platform.alarm.incident.Trigger;
import com.iris.platform.alarm.notification.calltree.CallTreeContext;

public class TestBasicNotificationStrategy extends NotificationStrategyTestCase {

   @Inject
   private BasicNotificationStrategy strategy;

   @Test
   public void testCOOnly() {
      testSingleAlert(AlertType.CO, NotificationConstants.CO_KEY);
   }

   @Test
   public void testSmokeOnly() {
      testSingleAlert(AlertType.SMOKE, NotificationConstants.SMOKE_KEY);
   }

   @Test
   public void testSecurityOnly() {
      testSingleAlert(AlertType.SECURITY, NotificationConstants.SECURITY_KEY);
   }

   @Test
   public void testPanicOnly() {
      testSingleAlert(AlertType.PANIC, NotificationConstants.PANIC_KEY);
   }

   @Test
   public void testMultipleAlerts() {
      Capture<CallTreeContext> contextCapture = EasyMock.newCapture(CaptureType.ALL);
      callTreeExecutor.notifyOwner(EasyMock.capture(contextCapture));
      EasyMock.expectLastCall().times(2);

      setupCallTree(1, callTreeEntry(UUID.randomUUID(), true));
      Trigger t = setupTrigger(UUID.randomUUID(), AlertType.CO, "Some Device", "Some Dev Type Hint", 1);
      Trigger t2 = setupTrigger(UUID.randomUUID(), AlertType.SECURITY, "Some Device", "Some Dev Type Hint", 1);

      replay();

      AlarmIncident incident = incidentBuilder()
            .withAlert(AlertType.CO)
            .addAdditionalAlert(AlertType.SECURITY)
            .build();

      strategy.execute(incident.getAddress(), incident.getPlaceId(), ImmutableList.of(t, t2));
      List<CallTreeContext> contexts = contextCapture.getValues();
      assertEquals(2, contexts.size());
      assertCallTreeContext(contexts.get(0), NotificationConstants.CO_KEY, NotificationCapability.NotifyRequest.PRIORITY_CRITICAL);
      assertCallTreeContext(contexts.get(1), NotificationConstants.SECURITY_KEY, NotificationCapability.NotifyRequest.PRIORITY_CRITICAL);
      verify();
   }

   @Test
   public void testMultipleAlertsSecondLater() {
      Capture<CallTreeContext> contextCapture = EasyMock.newCapture(CaptureType.ALL);
      callTreeExecutor.notifyOwner(EasyMock.capture(contextCapture));
      EasyMock.expectLastCall().times(2);

      setupCallTree(2, callTreeEntry(UUID.randomUUID(), true));
      Trigger t = setupTrigger(UUID.randomUUID(), AlertType.CO, "Some Device", "Some Dev Type Hint", 1);
      Trigger t2 = setupTrigger(UUID.randomUUID(), AlertType.SECURITY, "Some Device", "Some Dev Type Hint", 1);

      replay();

      AlarmIncident incident = incidentBuilder()
            .withAlert(AlertType.CO)
            .build();

      strategy.execute(incident.getAddress(), incident.getPlaceId(), ImmutableList.of(t));

      // simulate a second alert occurring after the first one has already been notified
      incident = AlarmIncident.builder(incident)
            .addAdditionalAlert(AlertType.SECURITY)
            .build();

      strategy.execute(incident.getAddress(), incident.getPlaceId(), ImmutableList.of(t, t2));

      List<CallTreeContext> contexts = contextCapture.getValues();
      assertEquals(2, contexts.size());
      assertCallTreeContext(contexts.get(0), NotificationConstants.CO_KEY, NotificationCapability.NotifyRequest.PRIORITY_CRITICAL);
      assertCallTreeContext(contexts.get(1), NotificationConstants.SECURITY_KEY, NotificationCapability.NotifyRequest.PRIORITY_CRITICAL);
      verify();
   }

   @Test
   public void testMultipleTriggersOfSameTypeOnlyIssueOneNotification() {
      Capture<CallTreeContext> contextCapture = EasyMock.newCapture(CaptureType.ALL);
      callTreeExecutor.notifyOwner(EasyMock.capture(contextCapture));
      EasyMock.expectLastCall();

      setupCallTree(1, callTreeEntry(UUID.randomUUID(), true));
      Trigger t = setupTrigger(UUID.randomUUID(), AlertType.CO, "Some Device", "Some Dev Type Hint", 1);
      Trigger t2 = setupTrigger(UUID.randomUUID(), AlertType.CO, "Some Device", "Some Dev Type Hint", 0);

      replay();

      AlarmIncident incident = incidentBuilder()
            .withAlert(AlertType.CO)
            .build();

      strategy.execute(incident.getAddress(), incident.getPlaceId(), ImmutableList.of(t, t2));
      List<CallTreeContext> contexts = contextCapture.getValues();
      assertEquals(1, contexts.size());
      assertCallTreeContext(contexts.get(0), NotificationConstants.CO_KEY, NotificationCapability.NotifyRequest.PRIORITY_CRITICAL);
      verify();
   }

   private void testSingleAlert(AlertType type, String msgKey) {
      Capture<CallTreeContext> contextCapture = EasyMock.newCapture();
      callTreeExecutor.notifyOwner(EasyMock.capture(contextCapture));
      EasyMock.expectLastCall();

      setupCallTree(1, callTreeEntry(UUID.randomUUID(), true));
      Trigger t = setupTrigger(UUID.randomUUID(), type, "Some Device", "Some Dev Type Hint", 1);

      replay();

      AlarmIncident incident = incidentBuilder()
            .withAlert(type)
            .build();

      strategy.execute(incident.getAddress(), incident.getPlaceId(), ImmutableList.of(t));
      assertCallTreeContext(contextCapture.getValue(), msgKey, NotificationCapability.NotifyRequest.PRIORITY_CRITICAL);
      verify();
   }
}

