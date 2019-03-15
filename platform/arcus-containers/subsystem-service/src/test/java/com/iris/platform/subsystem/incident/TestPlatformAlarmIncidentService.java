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
package com.iris.platform.subsystem.incident;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.iris.common.alarm.AlertType;
import com.iris.common.subsystem.alarm.co.CarbonMonoxideAlarm;
import com.iris.common.subsystem.alarm.security.SecurityAlarm;
import com.iris.common.subsystem.alarm.water.WaterAlarm;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.AlarmIncidentCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.serv.AlarmModel;
import com.iris.messages.service.AlarmService;
import com.iris.messages.type.IncidentTrigger;
import com.iris.messages.type.Population;
import com.iris.messages.type.TrackerEvent;
import com.iris.platform.alarm.incident.AlarmIncident;
import com.iris.platform.alarm.incident.AlarmIncident.AlertState;
import com.iris.platform.alarm.incident.AlarmIncident.MonitoringState;
import com.iris.platform.alarm.incident.AlarmIncident.TrackerState;

@RunWith(Parameterized.class)
public class TestPlatformAlarmIncidentService extends AlarmIncidentServiceTestCase {
   
   @Parameters(name="{0}")
   public static Iterable<Object []> serviceLevels() {
      return ImmutableList.of(
            new Object [] { PlaceCapability.SERVICELEVEL_BASIC },
            new Object [] { PlaceCapability.SERVICELEVEL_PREMIUM },
            new Object [] { PlaceCapability.SERVICELEVEL_PREMIUM_PROMON }
      );
   }
   
   @Inject PlatformAlarmIncidentService service;
   
   public TestPlatformAlarmIncidentService(String serviceLevel) {
      super(serviceLevel);
   }

   protected void updateIncident(AlarmIncident incident, MonitoringState state) {
   	updateIncident(incident, state, null);
   }
   
   protected void updateIncident(AlarmIncident incident, MonitoringState state, String customEventMessage) {
   	com.iris.messages.MessageBody.Builder bodyBuilder = MessageBody.messageBuilder(Capability.CMD_SET_ATTRIBUTES)
   		.withAttribute(AlarmIncidentCapability.ATTR_MONITORINGSTATE, state.name());
   	      
      if(StringUtils.isNotBlank(customEventMessage)) {
      	TrackerEvent e = new TrackerEvent();
	    	  e.setMessage(customEventMessage);
	    	 bodyBuilder.withAttribute(AlarmIncidentCapability.ATTR_TRACKER, ImmutableList.<Map<String, Object>>of(e.toMap()));
      }
      
      PlatformMessage message =
            PlatformMessage
               .builder()
               .from(Address.platformService(AlarmService.NAMESPACE))
               .to(incident.getAddress())
               .isRequestMessage(true)
               .withPlaceId(placeId)
               .withPayload(bodyBuilder.create())
               .create();
      
      service.setAttributes(message);
   }

   @Override
   protected void assertIncidentPreAlert(AlarmIncident incident, Date prealertEndTime) {
      super.assertIncidentPreAlert(incident, prealertEndTime);
      assertEquals(null, incident.getHubAlertState());
   }

   @Override
   protected void assertIncidentAlert(AlarmIncident incident, AlertType primary, AlertType... additional) {
      super.assertIncidentAlert(incident, primary, additional);
      assertEquals(null, incident.getHubAlertState());
   }

   @Test
   public void testAddPreAlertCreatesNewIncident() {
      expectCurrentAndReturnNull();
      List<IncidentTrigger> triggers = ImmutableList.of(
            IncidentFixtures.createIncidentTrigger(AlertType.SECURITY, IncidentTrigger.EVENT_MOTION),
            IncidentFixtures.createIncidentTrigger(AlertType.SECURITY, IncidentTrigger.EVENT_MOTION)
      );
      Capture<AlarmIncident> incidentCapture = expectUpdate();
      replay();
      
      Date prealert = new Date();
      service.addPreAlert(context, SecurityAlarm.NAME, prealert, triggers);
      
      AlarmIncident incident = incidentCapture.getValue();
      assertIncidentTrackers(incident, TrackerState.PREALERT);
      assertIncidentPreAlert(incident, prealert);
      
      assertBroadcastAdded(incident);
      assertNoMessages();
      
      verify();
   }
   
   @Test
   public void testAddPreAlertWhileAlerting() {
      AlarmIncident current = IncidentFixtures.createSmokeAlarm(serviceLevel);
      IncidentTrigger trigger = IncidentFixtures.createIncidentTrigger(AlertType.SECURITY);
      expectCurrentAndReturn(current);
      replay();
      
      Date prealert = new Date();
      service.addPreAlert(context, SecurityAlarm.NAME, prealert, ImmutableList.of(trigger));
      
      assertNoMessages();
      verify();
   }

   @Test
   public void testAddAlertCreatesNewIncident() {
      expectCurrentAndReturnNull();
      List<IncidentTrigger> triggers = ImmutableList.of(IncidentFixtures.createIncidentTrigger(AlertType.WATER, IncidentTrigger.EVENT_LEAK));
      Capture<AlarmIncident> incidentCapture = expectUpdate();
      replay();
      
      service.addAlert(context, WaterAlarm.NAME, triggers);
      
      AlarmIncident incident = incidentCapture.getValue();
      assertFalse(incident.isMonitored());   //water is not monitored
      assertIncidentTrackers(incident, TrackerState.ALERT);
      assertIncidentAlert(incident, AlertType.WATER);
      
      assertBroadcastAdded(incident);
      assertAddAlarm(incident, AlertType.WATER, ImmutableList.of(AlertType.WATER), triggers);
      assertBroadcastAlert(AlertType.WATER, triggers);
      assertNoMessages();
      
      verify();
   }
   
   /**
    * The initial CO trigger is sent to both addAlert & updateIncident, but only relayed by addAlert
    */
   @Test
   public void testAddAlertThenUpdateAlertDoesNotDuplicateTriggers() {
      expectCurrentAndReturnNull();
      List<IncidentTrigger> triggers = ImmutableList.of(IncidentFixtures.createIncidentTrigger(AlertType.CO, IncidentTrigger.EVENT_CO));
      Capture<AlarmIncident> incidentCapture = expectUpdate();
      replay();
      
      // initial alert
      AlarmModel.setAlertState(CarbonMonoxideAlarm.NAME, context.model(), AlarmCapability.ALERTSTATE_ALERT);
      service.addAlert(context, CarbonMonoxideAlarm.NAME, triggers);
      
      AlarmIncident incident = incidentCapture.getValue();
      assertIncidentMonitored(incident);
      assertIncidentTrackers(incident, TrackerState.ALERT);
      assertIncidentAlert(incident, AlertType.CO);
      
      assertBroadcastAdded(incident);
      assertAddAlarm(incident, AlertType.CO, ImmutableList.of(AlertType.CO), triggers);
      assertBroadcastAlert(AlertType.CO, triggers);
      assertNoMessages();
      
      reset();
      expectCurrentAndReturn(incident);
      expectTriggersAdded(incident, triggers);
      replay();
      
      // update incident, nothing new
      service.updateIncident(context, triggers);
      assertNoMessages();
      
      verify();
   }
   
   /**
    * The initial SECURITY alerts have already been passed along during PREALERT
    * Then SECURITY alerts
    * Then an additional trigger is sent
    */
   @Test
   public void testAddAlertThenUpdateAlertIncludesNewTriggers() {
      expectCurrentAndReturnNull();
      List<IncidentTrigger> initialTriggers = ImmutableList.of(IncidentFixtures.createIncidentTrigger(AlertType.SECURITY, IncidentTrigger.EVENT_MOTION), IncidentFixtures.createIncidentTrigger(AlertType.SECURITY, IncidentTrigger.EVENT_MOTION));
      Date preAlertEndTime = new Date(initialTriggers.get(0).getTime().getTime() + 1000);
      IncidentTrigger postAlertTrigger = IncidentFixtures.createIncidentTrigger(AlertType.SECURITY, IncidentTrigger.EVENT_MOTION);
      postAlertTrigger.setTime(new Date(preAlertEndTime.getTime() + 1000));
      List<IncidentTrigger> secondaryTriggers = ImmutableList.of(postAlertTrigger);
      Capture<AlarmIncident> incidentCapture = expectUpdate();
      replay();

      // pre-alert
      AlarmModel.setAlertState(SecurityAlarm.NAME, context.model(), AlarmCapability.ALERTSTATE_PREALERT);
      service.addPreAlert(context, SecurityAlarm.NAME, preAlertEndTime, initialTriggers);

      AlarmIncident incident = incidentCapture.getValue();
      assertIncidentMonitored(incident);
      assertIncidentTrackers(incident, TrackerState.PREALERT);
      assertIncidentPreAlert(incident, preAlertEndTime);
      
      assertBroadcastAdded(incident);
      assertNoMessages();
      
      reset();
      EasyMock.expect(mockPopulationCacheMgr.getPopulationByPlaceId(EasyMock.anyObject(UUID.class))).andReturn(Population.NAME_GENERAL).anyTimes();
      expectCurrentAndReturn(incident);
      incidentCapture = expectUpdate();
      replay();
      
      // entrance delay expires
      AlarmModel.setAlertState(SecurityAlarm.NAME, context.model(), AlarmCapability.ALERTSTATE_ALERT);
      service.addAlert(context, SecurityAlarm.NAME, initialTriggers);
      
      incident = incidentCapture.getValue();
      assertIncidentMonitored(incident);
      assertIncidentTrackers(incident, TrackerState.PREALERT, TrackerState.ALERT);
      assertIncidentAlert(incident, AlertType.SECURITY);
      
      assertBroadcastChanged(incident);
      assertAddAlarm(incident, AlertType.SECURITY, ImmutableList.of(AlertType.SECURITY), initialTriggers);
      assertBroadcastAlert(AlertType.SECURITY, initialTriggers);
      assertNoMessages();
      
      reset();
      EasyMock.expect(mockPopulationCacheMgr.getPopulationByPlaceId(EasyMock.anyObject(UUID.class))).andReturn(Population.NAME_GENERAL).anyTimes();
      expectCurrentAndReturn(incident);
      expectTriggersAdded(incident, secondaryTriggers);
      replay();
      
      // update incident, nothing new
      service.updateIncident(context, secondaryTriggers);
      assertAddAlarm(incident, AlertType.SECURITY, ImmutableList.of(AlertType.SECURITY), secondaryTriggers);
      assertNoMessages();
      
      verify();
   }
   
   @Test
   public void testAddWaterAlarmThenCOAlarm() {
      //make sure the monitored flag is set correctly after 2nd alert is added
      IncidentTrigger trigger = IncidentFixtures.createIncidentTrigger(AlertType.CO);
      AlarmIncident current = IncidentFixtures.createAlertIncident(AlertType.WATER, false);
      expectCurrentAndReturn(current);
      Capture<AlarmIncident> incidentCapture = expectUpdate();
      replay();
      
      service.addAlert(context, CarbonMonoxideAlarm.NAME, ImmutableList.of(trigger));
      
      AlarmIncident incident = incidentCapture.getValue();
      assertIncidentMonitored(incident);
      
      assertBroadcastChanged(incident);
   }
   
   @Test
   public void testAddSmokeAlarmThenCOAlarm() {
      //make sure the monitored flag is set correctly after 2nd alert is added
      IncidentTrigger trigger = IncidentFixtures.createIncidentTrigger(AlertType.CO);
      AlarmIncident current = IncidentFixtures.createAlertIncident(AlertType.SMOKE, true);
      expectCurrentAndReturn(current);
      Capture<AlarmIncident> incidentCapture = expectUpdate();
      replay();
      
      service.addAlert(context, CarbonMonoxideAlarm.NAME, ImmutableList.of(trigger));
      
      AlarmIncident incident = incidentCapture.getValue();
      assertIncidentMonitored(incident);      
      assertBroadcastChanged(incident);
      
      assertEquals(AlertType.CO, incident.getAlert());
      assertTrue(incident.getAdditionalAlerts().contains(AlertType.SMOKE));
   }
   
   @Test
   public void testEscalateAlertWhileAlerting() {
      IncidentTrigger trigger = IncidentFixtures.createIncidentTrigger(AlertType.CO);
      AlarmIncident current = IncidentFixtures.createSecurityAlarm(this.serviceLevel);
      expectCurrentAndReturn(current);
      Capture<AlarmIncident> incidentCapture = expectUpdate();
      replay();
      
      service.addAlert(context, CarbonMonoxideAlarm.NAME, ImmutableList.of(trigger));
      
      AlarmIncident incident = incidentCapture.getValue();
      assertIncidentMonitored(incident);
      assertIncidentTrackers(incident, TrackerState.PREALERT, TrackerState.ALERT);
      assertIncidentAlert(incident, AlertType.CO, AlertType.SECURITY);
      
      assertBroadcastChanged(incident);
      assertAddAlarm(incident, AlertType.CO, ImmutableList.of(AlertType.CO, AlertType.SECURITY), ImmutableList.of(trigger));
      assertBroadcastAlert(AlertType.CO, ImmutableList.of(trigger));
      assertNoMessages();
      
      verify();
   }
   
   @Test
   public void testSecondaryAlertWhileAlerting() {
      IncidentTrigger trigger = IncidentFixtures.createIncidentTrigger(AlertType.WATER);
      AlarmIncident current = IncidentFixtures.createSmokeAlarm(serviceLevel);
      expectCurrentAndReturn(current);  //first alert is smoke
      Capture<AlarmIncident> incidentCapture = expectUpdate();
      replay();
      
      service.addAlert(context, WaterAlarm.NAME, ImmutableList.of(trigger));  //second alert is water
      
      AlarmIncident incident = incidentCapture.getValue();
      assertIncidentMonitored(incident);
      assertIncidentTrackers(incident, TrackerState.ALERT);
      assertIncidentAlert(incident, AlertType.SMOKE, AlertType.WATER);
      
      assertBroadcastChanged(incident);
      assertAddAlarm(incident, AlertType.WATER, ImmutableList.of(AlertType.SMOKE, AlertType.WATER), ImmutableList.of(trigger));
      assertBroadcastAlert(AlertType.WATER, ImmutableList.of(trigger));
      assertNoMessages();
      
      verify();
   }
   
   @Test
   public void testPreAlertUpgradedToAlert() {
      IncidentTrigger trigger = IncidentFixtures.createIncidentTrigger(AlertType.SECURITY);
      AlarmIncident current = IncidentFixtures.createPreAlert(this.serviceLevel);
      expectCurrentAndReturn(current);
      Capture<AlarmIncident> incidentCapture = expectUpdate();
      replay();
      
      service.addAlert(context, SecurityAlarm.NAME, ImmutableList.of(trigger));
      
      AlarmIncident incident = incidentCapture.getValue();
      assertIncidentMonitored(incident);
      
      assertIncidentTrackers(incident, TrackerState.PREALERT, TrackerState.ALERT);
      assertIncidentAlert(incident, AlertType.SECURITY);
      
      assertBroadcastChanged(incident);
      assertAddAlarm(incident, AlertType.SECURITY, ImmutableList.of(AlertType.SECURITY), ImmutableList.of(trigger));
      assertBroadcastAlert(AlertType.SECURITY, ImmutableList.of(trigger));
      assertNoMessages();
      
      verify();
   }
   
   @Test
   public void testCancelSucceeds() {
      AlarmIncident current = IncidentFixtures.createSmokeAlarm(serviceLevel);
      
      expectCurrentAndReturn(current);
      expectFindByAndReturn(current); // second call is to close it out
      expectCancelling(current, cancelledBy, AlarmService.CancelAlertRequest.METHOD_APP);
      Capture<AlarmIncident> cancellingCapture = expectUpdate();
      Capture<AlarmIncident> cancelledCapture = expectUpdate();
      replay();
      
      service.cancel(context, cancelledBy, AlarmService.CancelAlertRequest.METHOD_APP);
      assertTrue(cancellingCapture.hasCaptured());
      assertFalse(cancelledCapture.hasCaptured());
      
      PlatformMessage vc = bus.poll();
      assertEquals(Capability.EVENT_VALUE_CHANGE, vc.getMessageType());
      assertEquals(
         ImmutableMap.of(
            AlarmIncidentCapability.ATTR_ALERTSTATE, AlarmIncidentCapability.ALERTSTATE_CANCELLING,
            AlarmIncidentCapability.ATTR_PLATFORMSTATE, AlarmIncidentCapability.PLATFORMSTATE_CANCELLING
         ), 
         vc.getValue().getAttributes()
      );
      
      assertEquals(AlertState.CANCELLING, cancellingCapture.getValue().getAlertState());
      assertEquals(null, cancellingCapture.getValue().getHubAlertState());
      assertEquals(AlertState.CANCELLING, cancellingCapture.getValue().getPlatformAlertState());
      
      PlatformMessage request = bus.poll();
      assertEquals(AlarmService.CancelAlertRequest.NAME, request.getMessageType());
      
      PlatformMessage response = 
            PlatformMessage
               .respondTo(request)
               .withPayload(AlarmService.CancelAlertResponse.instance())
               .create();
      
      service.onEvent(response);
      
      AlarmIncident incident = cancelledCapture.getValue();
      for(TrackerEvent cur : incident.getTracker()) {
         assertTrackerEvent(cur);
      }
      assertEquals(cancelledBy, incident.getCancelledBy());
      assertNotNull(incident.getEndTime());
      assertEquals(AlertState.COMPLETE, incident.getAlertState());
      
      verify();
   }

   @Test
   public void testCancelFails() {
      AlarmIncident current = IncidentFixtures.createSmokeAlarm(serviceLevel);
      expectCurrentAndReturn(current);
      expectCancelling(current, cancelledBy, AlarmService.CancelAlertRequest.METHOD_APP);
      Capture<AlarmIncident> cancellingCapture = expectUpdate();
      replay();
      
      service.cancel(context, cancelledBy, AlarmService.CancelAlertRequest.METHOD_APP);
      
      PlatformMessage vc = bus.poll();
      assertEquals(Capability.EVENT_VALUE_CHANGE, vc.getMessageType());
      assertEquals(
         ImmutableMap.of(
            AlarmIncidentCapability.ATTR_ALERTSTATE, AlarmIncidentCapability.ALERTSTATE_CANCELLING,
            AlarmIncidentCapability.ATTR_PLATFORMSTATE, AlarmIncidentCapability.PLATFORMSTATE_CANCELLING
         ), 
         vc.getValue().getAttributes()
      );
      
      assertEquals(AlertState.CANCELLING, cancellingCapture.getValue().getAlertState());
      
      PlatformMessage request = bus.poll();
      assertEquals(AlarmService.CancelAlertRequest.NAME, request.getMessageType());
      
      PlatformMessage response = 
            PlatformMessage
               .respondTo(request)
               .withPayload(Errors.requestCancelled())
               .create();
      
      service.onEvent(response);
      
      verify();
   }

   @Test
   public void testMonitoringState() {
      AlarmIncident current = IncidentFixtures.createSecurityAlarm(this.serviceLevel);
      
      expectFindByAndReturn(current);
      Capture<AlarmIncident> pendingCapture = expectUpdate();
      expectFindByAndReturn(current.getId(), pendingCapture);
      Capture<AlarmIncident> dispatchingCapture  = expectUpdate();
      expectFindByAndReturn(current.getId(), dispatchingCapture);
      Capture<AlarmIncident> dispatchedCapture = expectUpdate();
      replay();
      
      {
         updateIncident(current, MonitoringState.PENDING);
         
         AlarmIncident incident = pendingCapture.getValue();
         assertEquals(MonitoringState.PENDING, incident.getMonitoringState());
         assertFalse(incident.isConfirmed());
         assertBroadcastChanged(incident);
      }
      
      {
         updateIncident(current, MonitoringState.DISPATCHING);
         
         AlarmIncident incident = dispatchingCapture.getValue();
         for(TrackerEvent cur : incident.getTracker()) {
            assertTrackerEvent(cur);
         }
         assertEquals(MonitoringState.DISPATCHING, incident.getMonitoringState());
         assertFalse(incident.isConfirmed());
         assertBroadcastChanged(incident);
      }
      
      {
         updateIncident(current, MonitoringState.DISPATCHED);
         
         AlarmIncident incident = dispatchedCapture.getValue();
         assertEquals(MonitoringState.DISPATCHED, incident.getMonitoringState());
         assertBroadcastChanged(incident);
         for(TrackerEvent cur : incident.getTracker()) {
            assertTrackerEvent(cur);
         }
         assertEquals(MonitoringState.DISPATCHED, incident.getMonitoringState());   
         assertTrue(incident.isConfirmed());
      }
      
      verify();
   }
   
   @Test
   public void testMonitoringStateForPanic() {
      AlarmIncident current = IncidentFixtures.createPanicAlarm(this.serviceLevel);
      
      expectFindByAndReturn(current);
      Capture<AlarmIncident> pendingCapture = expectUpdate();
      expectFindByAndReturn(current.getId(), pendingCapture);
      Capture<AlarmIncident> dispatchingCapture  = expectUpdate();
      replay();
      
      {
         updateIncident(current, MonitoringState.PENDING);
         
         AlarmIncident incident = pendingCapture.getValue();
         assertEquals(MonitoringState.PENDING, incident.getMonitoringState());
         assertTrue(incident.isConfirmed());
         assertBroadcastChanged(incident);
      }
      
      {
         updateIncident(current, MonitoringState.DISPATCHING);
         
         AlarmIncident incident = dispatchingCapture.getValue();
         for(TrackerEvent cur : incident.getTracker()) {
            assertTrackerEvent(cur);
         }
         assertEquals(MonitoringState.DISPATCHING, incident.getMonitoringState());
         assertTrue(incident.isConfirmed());
         assertBroadcastChanged(incident);
      }            
      
      verify();
   }
   
   @Test
   public void testMonitoringStateWithCustomEventMessage() {
      AlarmIncident current = IncidentFixtures.createPanicAlarm(this.serviceLevel);
      
      expectFindByAndReturn(current);
      Capture<AlarmIncident> pendingCapture = expectUpdate();
      expectFindByAndReturn(current.getId(), pendingCapture);
      Capture<AlarmIncident> dispatchingCapture  = expectUpdate();
      replay();
      
      {
         updateIncident(current, MonitoringState.PENDING);
         
         AlarmIncident incident = pendingCapture.getValue();
         assertEquals(MonitoringState.PENDING, incident.getMonitoringState());
         assertTrue(incident.isConfirmed());
         assertBroadcastChanged(incident);
      }
      String customMessage = "Refused Due to Recent Alarm";
      {
         updateIncident(current, MonitoringState.FAILED, customMessage);
         
         AlarmIncident incident = dispatchingCapture.getValue();
         assertEquals(2, incident.getTracker().size());
         TrackerEvent event1 = incident.getTracker().get(0);
         assertTrackerEvent(event1);
         TrackerEvent event2 = incident.getTracker().get(1);
         assertTrackerEvent(event2);
         assertEquals(customMessage, event2.getMessage());  //custom message matches
         
         
         assertEquals(MonitoringState.FAILED, incident.getMonitoringState());
         assertTrue(incident.isConfirmed());
         assertBroadcastChanged(incident);
      }            
      
      verify();
   }
   
   
   @Test
   public void testVerifySecurityAlarm() {
      AlarmIncident current = IncidentFixtures.createSecurityAlarm(this.serviceLevel);
      
      expectFindByAndReturn(current);
      Capture<AlarmIncident> pendingCapture = expectUpdate();
      expectFindByAndReturn(current.getId(), pendingCapture);
      Capture<AlarmIncident> verifyCapture  = expectUpdate();
      replay();
      
      {
         updateIncident(current, MonitoringState.PENDING);
         
         AlarmIncident incident = pendingCapture.getValue();
         assertEquals(MonitoringState.PENDING, incident.getMonitoringState());
         assertFalse(incident.isConfirmed());
         assertBroadcastChanged(incident);
         assertNoMessages();
      }
      
      {
         service.verify(context, current.getAddress(), Address.platformService(UUID.randomUUID(), PersonCapability.NAMESPACE));
         AlarmIncident incident = verifyCapture.getValue();
         assertEquals(MonitoringState.PENDING, incident.getMonitoringState());
         assertTrue(incident.isConfirmed());
         assertBroadcastChanged(incident);
         assertAddAlarm(AlertType.SECURITY);
         assertNoMessages();
      }
      
      verify();
   }

}

