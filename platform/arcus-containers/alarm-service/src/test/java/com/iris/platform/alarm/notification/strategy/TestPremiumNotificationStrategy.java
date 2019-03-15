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
import com.iris.common.alarm.AlertType;
import com.iris.messages.capability.NotificationCapability;
import com.iris.platform.alarm.incident.AlarmIncident;
import com.iris.platform.alarm.incident.Trigger;
import com.iris.platform.alarm.notification.calltree.CallTreeContext;

public class TestPremiumNotificationStrategy extends NotificationStrategyTestCase {

   
   private PremiumNotificationStrategy strategy;
   private NotificationStrategyConfig config;
   
   @Override
   public void setUp() throws Exception {
      super.setUp();
      config = new NotificationStrategyConfig();
      strategy = new PremiumNotificationStrategy(config, callTreeExecutor, callTreeDao, modelLoader);
   }

   @Test
   public void testCOOnly() {
      testSingleAlert(AlertType.CO, NotificationConstants.CO_KEY, false);
   }

   @Test
   public void testSmokeOnly() {
      testSingleAlert(AlertType.SMOKE, NotificationConstants.SMOKE_KEY, false);
   }

   @Test
   public void testSecurityOnly() {
      testSingleAlert(AlertType.SECURITY, NotificationConstants.SECURITY_KEY, false);
   }

   @Test
   public void testPanicOnly() {
      testSingleAlert(AlertType.PANIC, NotificationConstants.PANIC_KEY, false);
   }
   
   @Test
   public void testPanicByRuleOnly() {
      testSingleAlert(AlertType.PANIC, String.format(NotificationConstants.KEY_TEMPLATE_FOR_TRIGGER_RULE, "panic"), false, true);
   }

   @Test
   public void testAcknowledgeSecurity() {
      testSingleAlert(AlertType.SECURITY, NotificationConstants.SECURITY_KEY, true);
   }

   @Test
   public void testAcknowledgePanic() {
      testSingleAlert(AlertType.PANIC, NotificationConstants.PANIC_KEY, true);
   }

   @Test
   public void testAcknowledgeCO() {
      testSingleAlert(AlertType.CO, NotificationConstants.CO_KEY, true);
   }

   @Test
   public void testAcknowledgeSmoke() {
      testSingleAlert(AlertType.SMOKE, NotificationConstants.SMOKE_KEY, true);
   }

   @Test
   public void testCancel() {

   }

   @Test
   public void testMultipleAlerts() {
      Capture<CallTreeContext> contextCapture1 = EasyMock.newCapture();
      Capture<CallTreeContext> contextCapture2 = EasyMock.newCapture();
      callTreeExecutor.notifyParallel(EasyMock.capture(contextCapture1));
      EasyMock.expectLastCall();
      callTreeExecutor.startSequential(EasyMock.capture(contextCapture2));
      EasyMock.expectLastCall();

      setupCallTree(1, callTreeEntry(UUID.randomUUID(), true));
      Trigger t = setupTrigger(UUID.randomUUID(), AlertType.CO, "Some Device", "Some Dev Type Hint", 1);
      Trigger t2 = setupTrigger(UUID.randomUUID(), AlertType.SECURITY, "Some Device", "Some Dev Type Hint", 1);

      replay();

      AlarmIncident incident = incidentBuilder()
            .withAlert(AlertType.CO)
            .addAdditionalAlert(AlertType.SECURITY)
            .build();

      strategy.execute(incident.getAddress(), incident.getPlaceId(), ImmutableList.of(t, t2));
      assertCallTreeContext(contextCapture1.getValue(), NotificationConstants.CO_KEY, NotificationCapability.NotifyRequest.PRIORITY_CRITICAL);
      assertCallTreeContext(contextCapture2.getValue(), NotificationConstants.SECURITY_KEY, NotificationCapability.NotifyRequest.PRIORITY_CRITICAL);
      verify();
   }

   @Test
   public void testMultipleAlertsSecondLater() {
      Capture<CallTreeContext> contextCapture1 = EasyMock.newCapture();
      Capture<CallTreeContext> contextCapture2 = EasyMock.newCapture();
      callTreeExecutor.notifyParallel(EasyMock.capture(contextCapture1));
      EasyMock.expectLastCall();
      callTreeExecutor.startSequential(EasyMock.capture(contextCapture2));
      EasyMock.expectLastCall();

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

      assertCallTreeContext(contextCapture1.getValue(), NotificationConstants.CO_KEY, NotificationCapability.NotifyRequest.PRIORITY_CRITICAL);
      assertCallTreeContext(contextCapture2.getValue(), NotificationConstants.SECURITY_KEY, NotificationCapability.NotifyRequest.PRIORITY_CRITICAL);
      verify();
   }

   @Test
   public void testMultipleTriggersOfSameTypeOnlyIssueOneNotification() {
      Capture<CallTreeContext> contextCapture = EasyMock.newCapture(CaptureType.ALL);
      callTreeExecutor.notifyParallel(EasyMock.capture(contextCapture));
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

   private void testSingleAlert(AlertType type, String msgKey, boolean ack) {
	   testSingleAlert(type, msgKey, ack, false);
   }
   
   private void testSingleAlert(AlertType type, String msgKey, boolean ack, boolean isRuleTriggered) {
      Capture<CallTreeContext> contextCapture = EasyMock.newCapture();
      switch(type) {
         case CO:
         case SMOKE:
            callTreeExecutor.notifyParallel(EasyMock.capture(contextCapture));
            break;
         case SECURITY:
         case PANIC:
            callTreeExecutor.startSequential(EasyMock.capture(contextCapture));
            if(ack) {
               callTreeExecutor.stopSequential(incidentAddress, msgKey);
               EasyMock.expectLastCall();
            }
            break;
      }
      EasyMock.expectLastCall();

      setupCallTree(2, callTreeEntry(UUID.randomUUID(), true));
      Trigger t = null;
      if(isRuleTriggered) {
    	  t = setupRuleTrigger(placeId, 1, type, 1);
      }else{
    	  t = setupTrigger(UUID.randomUUID(), type, "Some Device", "Some Dev Type Hint", 1);
      }

      // setup cancel
      Capture<CallTreeContext> contextCaptureForCancel = EasyMock.newCapture();
      Capture<CallTreeContext> contextCaptureForCancel2 = EasyMock.newCapture();
      
      AlarmIncident incident = stagingCancelAlert(type, contextCaptureForCancel, contextCaptureForCancel2);
      replay();

     

      strategy.execute(incident.getAddress(), incident.getPlaceId(), ImmutableList.of(t));
      if(ack) {
         strategy.acknowledge(incidentAddress, type);
      }
      strategy.cancel(incidentAddress, incident.getPlaceId(), incident.getCancelledBy(), ImmutableList.<String>of(incident.getAlert().name()));
      assertCallTreeContext(contextCapture.getValue(), msgKey, NotificationCapability.NotifyRequest.PRIORITY_CRITICAL);
      assertCallTreeContext(contextCaptureForCancel.getValue(), String.format(NotificationConstants.KEY_TEMPLATE_FOR_CANCEL, type.name().toLowerCase()), NotificationCapability.NotifyRequest.PRIORITY_MEDIUM);
      assertCallTreeContext(contextCaptureForCancel2.getValue(), String.format(NotificationConstants.KEY_TEMPLATE_FOR_CANCEL, type.name().toLowerCase()), NotificationCapability.NotifyRequest.PRIORITY_LOW);
      verify();
   }
   
  
}

