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
package com.iris.common.subsystem.alarm;

import static com.iris.common.subsystem.alarm.CancelAlarmMessageTable.messageTexts;
import static com.iris.common.subsystem.alarm.CancelAlarmMessageTable.messageTitles;
import static com.iris.messages.capability.AlarmIncidentCapability.ALERTSTATE_COMPLETE;
import static com.iris.messages.capability.AlarmIncidentCapability.MONITORINGSTATE_DISPATCHED;
import static com.iris.messages.capability.AlarmIncidentCapability.MONITORINGSTATE_DISPATCHING;
import static com.iris.messages.capability.AlarmIncidentCapability.MONITORINGSTATE_FAILED;
import static com.iris.messages.capability.AlarmIncidentCapability.MONITORINGSTATE_NONE;
import static com.iris.messages.capability.AlarmIncidentCapability.MONITORINGSTATE_REFUSED;
import static com.iris.messages.capability.AlarmIncidentCapability.CancelResponse.ALARMSTATE_CANCELLED;
import static com.iris.messages.capability.AlarmIncidentCapability.CancelResponse.MONITORINGSTATE_CANCELLED;
import static com.iris.messages.capability.CarbonMonoxideCapability.CO_SAFE;
import static com.iris.messages.capability.SmokeCapability.SMOKE_SAFE;
import static java.lang.String.format;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import com.google.common.util.concurrent.SettableFuture;
import com.iris.common.subsystem.alarm.co.CarbonMonoxideAlarm;
import com.iris.common.subsystem.alarm.panic.PanicAlarm;
import com.iris.common.subsystem.alarm.smoke.SmokeAlarm;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.AlarmIncidentCapability.CancelResponse;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.hub.HubModel;
import com.iris.messages.model.serv.AlarmIncidentModel;
import com.iris.messages.model.test.ModelFixtures;

public class TestAlarmSubsystem_CancelAlarm extends PlatformAlarmSubsystemTestCase
{
   private Model smoke;
   private Model co;
   private Model hub;

   @Before
   public void createSiren()
   {
      // enable the alarms
      addContactDevice();
      smoke = addSmokeDevice(SMOKE_SAFE);
      co = addCODevice(CO_SAFE);
      addModel(ModelFixtures.createAlertAttributes());
      hub = addModel(ModelFixtures.createHubAttributes());
   }

   /*
   protected void assertAlertSent()
   {
      assertContainsRequestMessageWithAttrs(Capability.CMD_SET_ATTRIBUTES,
         ImmutableMap.<String, Object>of(AlertCapability.ATTR_STATE, AlertCapability.STATE_ALERTING));
   }

   protected void assertQuietSent()
   {
      assertContainsRequestMessageWithAttrs(Capability.CMD_SET_ATTRIBUTES,
         ImmutableMap.<String, Object>of(AlertCapability.ATTR_STATE, AlertCapability.STATE_QUIET));
   }
   */

   @Test
   public void testCancelPanicAlarm1() throws Exception
   {
      doTestAlarm(PanicAlarm.NAME, MONITORINGSTATE_NONE,
         ALARMSTATE_CANCELLED, true, MONITORINGSTATE_CANCELLED);
   }

   @Test
   public void testCancelPanicAlarm2() throws Exception
   {
      doTestAlarm(PanicAlarm.NAME, MONITORINGSTATE_DISPATCHED,
         ALARMSTATE_CANCELLED, false, MONITORINGSTATE_DISPATCHED, messageTitles[1], messageTexts[14]);
   }

   @Test
   public void testCancelPanicAlarm3() throws Exception
   {
      doTestAlarm(PanicAlarm.NAME, MONITORINGSTATE_DISPATCHING,
         ALARMSTATE_CANCELLED, false, MONITORINGSTATE_DISPATCHING, messageTitles[1], messageTexts[13]);
   }

   @Test
   public void testCancelPanicAlarm4() throws Exception
   {
      doTestAlarm(PanicAlarm.NAME, MONITORINGSTATE_REFUSED,
         ALARMSTATE_CANCELLED, false, MONITORINGSTATE_CANCELLED, messageTitles[1], messageTexts[15]);
   }

   @Test
   public void testCancelPanicAlarm5() throws Exception
   {
      doTestAlarm(PanicAlarm.NAME, MONITORINGSTATE_FAILED,
         ALARMSTATE_CANCELLED, false, MONITORINGSTATE_CANCELLED, messageTitles[1], messageTexts[15]);
   }

   @Test
   public void testCancelPanicAlarm_noHub() throws Exception
   {
      removeModel(hub);

      doTestAlarm(PanicAlarm.NAME, MONITORINGSTATE_FAILED,
         ALARMSTATE_CANCELLED, false, MONITORINGSTATE_CANCELLED, messageTitles[1], messageTexts[15]);
   }

   @Test
   public void testCancelPanicAlarm_hubOffline() throws Exception
   {
      HubModel.setState(hub, HubCapability.STATE_DOWN);

      doTestAlarm(PanicAlarm.NAME, MONITORINGSTATE_FAILED,
         ALARMSTATE_CANCELLED, false, MONITORINGSTATE_CANCELLED, messageTitles[2], messageTexts[26]);
   }

   @Test
   public void testCancelCOAlarm1() throws Exception
   {
      doTestAlarm(CarbonMonoxideAlarm.NAME, MONITORINGSTATE_NONE,
         ALARMSTATE_CANCELLED, true, MONITORINGSTATE_CANCELLED);
   }

   @Test
   public void testCancelCOAlarm2() throws Exception
   {
      doTestAlarm(CarbonMonoxideAlarm.NAME, MONITORINGSTATE_NONE,
         ALARMSTATE_CANCELLED, true, MONITORINGSTATE_CANCELLED);
   }

   @Test
   public void testCancelCOAlarm3() throws Exception
   {
      trigger(co);

      doTestAlarm(CarbonMonoxideAlarm.NAME, MONITORINGSTATE_NONE,
         ALARMSTATE_CANCELLED, false, MONITORINGSTATE_CANCELLED, messageTitles[0], messageTexts[7]);
   }

   @Test
   public void testCancelCOAlarm4() throws Exception
   {
      doTestAlarm(CarbonMonoxideAlarm.NAME, MONITORINGSTATE_DISPATCHING,
         ALARMSTATE_CANCELLED, false, MONITORINGSTATE_DISPATCHING, messageTitles[1], messageTexts[8]);
   }

   @Test
   public void testCancelCOAlarm5() throws Exception
   {
      trigger(co);

      doTestAlarm(CarbonMonoxideAlarm.NAME, MONITORINGSTATE_DISPATCHING,
         ALARMSTATE_CANCELLED, false, MONITORINGSTATE_DISPATCHING, messageTitles[0], messageTexts[9]);
   }

   @Test
   public void testCancelSmokeAlarm1() throws Exception
   {
      doTestAlarm(SmokeAlarm.NAME, MONITORINGSTATE_DISPATCHING,
         ALARMSTATE_CANCELLED, false, MONITORINGSTATE_DISPATCHING, messageTitles[1], messageTexts[1]);
   }

   @Test
   public void testCancelSmokeAlarm2() throws Exception
   {
      trigger(smoke);

      doTestAlarm(SmokeAlarm.NAME, MONITORINGSTATE_DISPATCHING,
         ALARMSTATE_CANCELLED, false, MONITORINGSTATE_DISPATCHING, messageTitles[0], messageTexts[2]);
   }

   private void doTestAlarm(String alarm, String currentMonitoringState, String expectedAlarmState,
      boolean expectedCleared, String expectedMonitoringState) throws Exception
   {
      doTestAlarm(alarm, currentMonitoringState, expectedAlarmState, expectedCleared, expectedMonitoringState, null, null);
   }

   private void doTestAlarm(String alarm, String currentMonitoringState, String expectedAlarmState,
      boolean expectedCleared, String expectedMonitoringState, String expectedWarningTitle, String expectedWarningMessage)
         throws Exception
   {
      stageAlerting(alarm);

      AlarmIncidentModel incident = stageAlarmIncident(alarm);
      incident.setMonitoringState(currentMonitoringState);
      incident.setAlertState(ALERTSTATE_COMPLETE);
      expectCancelIncidentAndReturn(SettableFuture.<Void>create(), incident);

      start();

      MessageBody response = cancel();

      assertCancelResponse(alarm, response,
         expectedAlarmState, expectedCleared, expectedMonitoringState, expectedWarningTitle, expectedWarningMessage);
   }

   /*
   private void assertCancelResponse(MessageBody response, String alarmstate, boolean isCleared,
      String monitoringstateCancelled, String title, String msg)
   {
      assertEquals(alarmstate, CancelResponse.getAlarmState(response));
      assertEquals(isCleared, CancelResponse.getCleared(response));
      assertEquals(title, CancelResponse.getWarningTitle(response));
      assertEquals(msg, CancelResponse.getWarningMessage(response));
   }
   */

   private void assertCancelResponse(String alarm, MessageBody response,
      String expectedAlarmState, boolean expectedCleared, String expectedMonitoringstate,
      String expectedWarningTitle, String expectedWarningMessage)
   {
      System.out.println(format("alarm=%s, alarmState=%s, isCleared=%s, monitoringState=%s, title=%s, msg=%s",
         alarm,
         CancelResponse.getAlarmState(response),
         CancelResponse.getCleared(response),
         CancelResponse.getMonitoringState(response),
         CancelResponse.getWarningTitle(response),
         CancelResponse.getWarningMessage(response)));

      assertEquals(expectedAlarmState, CancelResponse.getAlarmState(response));
      assertEquals(expectedCleared, CancelResponse.getCleared(response));
      assertEquals(expectedMonitoringstate, CancelResponse.getMonitoringState(response));

      if (expectedWarningTitle != null)
      {
         assertEquals(expectedWarningTitle, CancelResponse.getWarningTitle(response));
         assertEquals(expectedWarningMessage, CancelResponse.getWarningMessage(response));
      }
      else
      {
         assertTrue(StringUtils.isBlank(CancelResponse.getWarningTitle(response)));
         assertTrue(StringUtils.isBlank(CancelResponse.getWarningMessage(response)));
      }
   }
}

