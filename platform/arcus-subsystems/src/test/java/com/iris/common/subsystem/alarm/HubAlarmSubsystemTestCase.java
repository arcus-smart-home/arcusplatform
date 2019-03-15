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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Before;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.iris.common.subsystem.alarm.HubAlarmSubsystem.TriggerInfo;
import com.iris.common.subsystem.alarm.generic.AlarmState.TriggerEvent;
import com.iris.common.subsystem.alarm.incident.AlarmIncidentService;
import com.iris.common.subsystem.alarm.security.SecurityAlarm;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmIncidentCapability;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.ContactCapability;
import com.iris.messages.capability.HubAlarmCapability;
import com.iris.messages.capability.MotionCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.event.MessageReceivedEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.DeviceAdvancedModel;
import com.iris.messages.model.serv.AlarmIncidentModel;
import com.iris.messages.model.serv.HubAlarmModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.messages.type.IncidentTrigger;

public class HubAlarmSubsystemTestCase extends BaseAlarmSubsystemTestCase<HubAlarmSubsystem> {
	//protected Model hub;
	
	/*@Before
	public void createHub() {
		hub = addModel( ModelFixtures.createHubAttributes() );
	}*/
	
	@Override
	protected HubAlarmSubsystem newAlarmSubsystem(AlarmIncidentService service) {
		return new HubAlarmSubsystem(service);
	}

	protected MessageBody.Builder buildReport() {
		return
				MessageBody
					.messageBuilder(Capability.EVENT_REPORT)
					.withAttribute(HubAlarmCapability.ATTR_ACTIVEALERTS, context.model().getActiveAlerts())
					.withAttribute(HubAlarmCapability.ATTR_ALARMSTATE, context.model().getAlarmState())
					.withAttribute(HubAlarmCapability.ATTR_AVAILABLEALERTS, context.model().getAvailableAlerts())
					;
	}

	@Override
	protected void stagePreAlert(String mode, Model... triggeringDevices) {
		stageSubystemPreAlert(mode, triggeringDevices);
		stageHubPreAlert(mode, triggeringDevices);
		TriggerInfo info = subsystem.getTriggerInfo(context);
		info.setIndex(AlarmSubsystemCapability.ACTIVEALERTS_SECURITY, info.getIndex(AlarmSubsystemCapability.ACTIVEALERTS_SECURITY) + triggeringDevices.length);
		subsystem.setTriggerInfo(context, info);
	}
	
	protected void stageSubystemPreAlert(String mode, Model... triggeringDevices) {
		super.stagePreAlert(mode, triggeringDevices);
	}
	
	protected void stageHubPreAlert(String mode, Model... triggeringDevices) {
		HubAlarmModel.setAlarmState(hub, HubAlarmCapability.ALARMSTATE_PREALERT);
		// give a slow build server lots of time to catch up
		HubAlarmModel.setSecurityAlertState(hub, HubAlarmCapability.SECURITYALERTSTATE_PREALERT);
		HubAlarmModel.setSecurityPreAlertEndTime(hub, new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10)));
		HubAlarmModel.setSecurityMode(hub, mode);
		HubAlarmModel.setCurrentIncident(hub, incidentAddress.getId().toString());
		List<Map<String, Object>> triggers = new ArrayList<>();
		for(Model device: triggeringDevices) {
			IncidentTrigger trigger = new IncidentTrigger();
			trigger.setAlarm(IncidentTrigger.ALARM_SECURITY);
			trigger.setEvent(getEvent(device).name());
			trigger.setSource(Address.protocolAddress(DeviceAdvancedModel.getProtocol(device), DeviceAdvancedModel.getProtocolid(device)).getRepresentation());
			trigger.setTime(new Date());
			triggers.add(trigger.toMap());
		}
		HubAlarmModel.setSecurityTriggers(hub, triggers);
	}
	
	protected TriggerEvent getEvent(Model device) {
		if(device.supports(ContactCapability.NAMESPACE)) {
			return TriggerEvent.CONTACT;
		}
		else if(device.supports(MotionCapability.NAMESPACE)) {
			return TriggerEvent.MOTION;
		}
		throw new UnsupportedOperationException("Triggers for device of type " + device.getCapabilities() + " aren't mapped yet, please update this method");
	}

	protected List<Map<String, Object>> triggersToMap(IncidentTrigger[] triggers) {
		List<Map<String, Object>> result = new ArrayList<>(triggers.length);
		for(IncidentTrigger trigger: triggers) {
			result.add(trigger.toMap());
		}
		return result;
	}

	@Override
	protected void stageAlerting(String... alerts) {
		stageSubsystemAlerting(alerts);
		stageHubAlerting(alerts);
	}
	
	protected void stageSubsystemAlerting(String... alerts) {
		super.stageAlerting(alerts);
	}
	
	protected void stageHubAlerting(String... alerts) {
		HubAlarmModel.setActiveAlerts(hub, Arrays.asList(alerts));
		HubAlarmModel.setAlarmState(hub, HubAlarmCapability.ALARMSTATE_ALERTING);
		HubAlarmModel.setCurrentIncident(hub, incidentAddress.getId().toString());
		for(String alert: alerts) {
			if(SecurityAlarm.NAME.equals(alert)) {
				HubAlarmModel.setSecurityMode(hub, HubAlarmCapability.SECURITYMODE_ON);
			}
			hub.setAttribute(HubAlarmCapability.NAMESPACE + ":" + alert.toLowerCase() + "AlertState", HubAlarmCapability.SECURITYALERTSTATE_ALERT);
		}
	}

	protected void stageClearing(Address cancelledBy, Address cancelledFrom) {
		// FIXME lastDisarmedTime on the two will not match
		stageHubClearing(cancelledBy, cancelledFrom);
		stageSubsystemClearing(cancelledBy, cancelledFrom);
	}
	
	protected void stageHubClearing(Address cancelledBy, Address cancelledFrom) {
		HubAlarmModel.setAlarmState(hub, HubAlarmCapability.ALARMSTATE_CLEARING);
		for(String alertState: new String[] { HubAlarmCapability.ATTR_SECURITYALERTSTATE, HubAlarmCapability.ATTR_SMOKEALERTSTATE, HubAlarmCapability.ATTR_COACTIVEDEVICES, HubAlarmCapability.ATTR_PANICALERTSTATE }) {
			if(HubAlarmCapability.SECURITYALERTSTATE_ALERT.equals(hub.getAttribute(alertState))) {
				hub.setAttribute(alertState, HubAlarmCapability.SECURITYALERTSTATE_CLEARING);
			}
		}
		HubAlarmModel.setLastDisarmedBy(hub, cancelledBy.getRepresentation());
		HubAlarmModel.setLastDisarmedFrom(hub, cancelledFrom.getRepresentation());
		HubAlarmModel.setLastDisarmedTime(hub, new Date());
	}
	
	protected void stageSubsystemClearing(Address cancelledBy, Address cancelledFrom) {
		stageAlarmSubsystem();
		context.model().setActiveAlerts(ImmutableList.<String>of());
		context.model().setAlarmState(AlarmSubsystemCapability.ALARMSTATE_CLEARING);
		context.model().setCurrentIncident("");
		context.setVariable(AlarmUtil.VAR_CANCELLEDBY, cancelledBy);
		context.setVariable(AlarmUtil.VAR_CANCELACTOR, cancelledFrom);
		context.setVariable(AlarmUtil.VAR_CANCELMETHOD, cancelledFrom != null && MessageConstants.DRIVER.equals(cancelledFrom.getNamespace()) ? "KEYPAD" : "APP");
		context.setVariable(HubAlarmSubsystem.VAR_CANCELINCIDENT, incidentAddress.getRepresentation());
	}

	protected AlarmIncidentModel stageAlertingAlarmIncident(String alert) {
		AlarmIncidentModel incident = stageAlarmIncident(alert);
		incident.setAlertState(AlarmIncidentCapability.ALERTSTATE_ALERT);
		incident.setHubState(AlarmIncidentCapability.PLATFORMSTATE_ALERT);
		incident.setPlatformState(AlarmIncidentCapability.PLATFORMSTATE_ALERT);
		return incident;
	}
	
	protected void reportPanicAlert(Address incident, IncidentTrigger... triggers) {
		MessageBody report =
				buildReport()
					.withAttribute(HubAlarmCapability.ATTR_ALARMSTATE, HubAlarmCapability.ALARMSTATE_ALERTING)
					.withAttribute(HubAlarmCapability.ATTR_PANICALERTSTATE, HubAlarmCapability.PANICALERTSTATE_ALERT)
					.withAttribute(HubAlarmCapability.ATTR_PANICTRIGGEREDDEVICES, ImmutableList.of())
					.withAttribute(HubAlarmCapability.ATTR_PANICTRIGGERS, triggersToMap(triggers))
					.create();
		PlatformMessage message = PlatformMessage.createBroadcast(report, hub.getAddress());
		context.models().update(message);
	}
	
	protected void reportDisarmed(Address disarmedBy, Address disarmedFrom) {
		MessageBody report =
				buildReport()
					.withAttribute(HubAlarmCapability.ATTR_ALARMSTATE, HubAlarmCapability.ALARMSTATE_CLEARING)
					.withAttribute(HubAlarmCapability.ATTR_SECURITYALERTSTATE, HubAlarmCapability.SECURITYALERTSTATE_CLEARING)
					.withAttribute(HubAlarmCapability.ATTR_LASTDISARMEDBY, disarmedBy != null ? disarmedBy.getRepresentation() : null)
					.withAttribute(HubAlarmCapability.ATTR_LASTDISARMEDFROM, disarmedFrom != null ? disarmedFrom.getRepresentation() : null)
					.withAttribute(HubAlarmCapability.ATTR_LASTDISARMEDTIME, new Date())
					.create();
		PlatformMessage message = PlatformMessage.createBroadcast(report, hub.getAddress());
		context.models().update(message);
	}
	
	protected void reportDisarmedFromKeyPad(Model keypad) {
		reportDisarmed(Address.platformService(UUID.randomUUID(), PersonCapability.NAMESPACE), keypad.getAddress());
	}
	
	protected void reportDisarmedFromApp() {
		reportDisarmed(Address.platformService(UUID.randomUUID(), PersonCapability.NAMESPACE), clientAddress);
	}
	
	protected void reportIncidentCompleted() {
		MessageBody report =
				buildReport()
					.withAttribute(HubAlarmCapability.ATTR_CURRENTINCIDENT, "")
					.create();
		PlatformMessage message = PlatformMessage.createBroadcast(report, hub.getAddress());
		context.models().update(message);
	}
	
	@Override
	protected void expectCancelIncidentAndReturn(
			final ListenableFuture<Void> response, 
			final AlarmIncidentModel expectedIncident
	) {
		EasyMock
			.expect(
					incidentService.cancel(
							EasyMock.eq(context), 
							EasyMock.eq(expectedIncident.getAddress().getRepresentation()), 
							EasyMock.isA(Address.class), 
							EasyMock.isA(String.class)
					)
			)
			.andAnswer(new IAnswer<AlarmIncidentModel>() {

				@Override
				public AlarmIncidentModel answer() throws Throwable {
					response.addListener(
							new Runnable() {
								@Override
								public void run() {
									PlatformMessage message =
											PlatformMessage
												.broadcast()
												.from(expectedIncident.getAddress())
												.withPlaceId(expectedIncident.getPlaceId())
												.withPayload(AlarmIncidentCapability.CompletedEvent.instance())
												.create();
									subsystem.onEvent(new MessageReceivedEvent(message), context);
								}
							},
							MoreExecutors.directExecutor()
					);
					return expectedIncident;
				}
			});
		EasyMock.expect(incidentService.getIncident(EasyMock.eq(context), EasyMock.isA(Address.class))).andReturn(expectedIncident);
		
	}
	
	protected void assertDisarmAndExpect() {
		SendAndExpect op = sendAndExpectOperations.remove(0);
		assertEquals(op.getRequestAddress(), hub.getAddress());
		assertEquals(op.getMessage().getMessageType(), HubAlarmCapability.DisarmRequest.NAME);
	}
	
	protected void assertDisarmSent() {
		MessageBody clear = sends.getValue();
		assertEquals(HubAlarmCapability.DisarmRequest.NAME, clear.getMessageType());
	}
	
	protected void assertClearIncidentSent() {
		MessageBody clear = sends.getValue();
		assertEquals(HubAlarmCapability.ClearIncidentRequest.NAME, clear.getMessageType());
	}
	
	
	
	protected void assertClearing() {
		assertEquals("", context.model().getCurrentIncident());
		assertEquals(AlarmSubsystemCapability.ALARMSTATE_CLEARING, context.model().getAlarmState());
		assertEquals(incidentAddress.getRepresentation(), context.getVariable(HubAlarmSubsystem.VAR_CANCELINCIDENT).as(String.class));
	}
	
	protected void assertClearingFromApp() {
		assertClearing();
		assertEquals(MessageConstants.CLIENT, context.getVariable(AlarmUtil.VAR_CANCELACTOR).as(Address.class).getNamespace());
		assertEquals(PersonCapability.NAMESPACE, context.getVariable(AlarmUtil.VAR_CANCELLEDBY).as(Address.class).getGroup());
		assertEquals("APP", context.getVariable(AlarmUtil.VAR_CANCELMETHOD).as(String.class));
	}

	protected void assertClearingFromKeypad() {
		assertClearing();
		assertEquals(MessageConstants.DRIVER, context.getVariable(AlarmUtil.VAR_CANCELACTOR).as(Address.class).getNamespace());
		assertEquals(PersonCapability.NAMESPACE, context.getVariable(AlarmUtil.VAR_CANCELLEDBY).as(Address.class).getGroup());
		assertEquals("KEYPAD", context.getVariable(AlarmUtil.VAR_CANCELMETHOD).as(String.class));
	}

}

