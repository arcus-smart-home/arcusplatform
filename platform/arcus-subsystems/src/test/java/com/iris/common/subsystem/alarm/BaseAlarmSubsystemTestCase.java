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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.easymock.IExpectationSetters;
import org.junit.Before;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.iris.capability.key.NamespacedKey;
import com.iris.common.subsystem.BaseSubsystem;
import com.iris.common.subsystem.SubsystemTestCase;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.common.subsystem.alarm.co.CarbonMonoxideAlarm;
import com.iris.common.subsystem.alarm.incident.AlarmIncidentService;
import com.iris.common.subsystem.alarm.panic.PanicAlarm;
import com.iris.common.subsystem.alarm.security.SecurityAlarm;
import com.iris.common.subsystem.alarm.security.SecurityArmingState;
import com.iris.common.subsystem.alarm.smoke.SmokeAlarm;
import com.iris.common.subsystem.alarm.subs.AlertState;
import com.iris.common.subsystem.alarm.water.WaterAlarm;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.AlarmIncidentCapability;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.CarbonMonoxideCapability;
import com.iris.messages.capability.ContactCapability;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.GlassCapability;
import com.iris.messages.capability.KeyPadCapability;
import com.iris.messages.capability.LeakH2OCapability;
import com.iris.messages.capability.MotionCapability;
import com.iris.messages.capability.MotorizedDoorCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.RuleCapability;
import com.iris.messages.capability.SecurityAlarmModeCapability;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.capability.SmokeCapability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.capability.ValveCapability;
import com.iris.messages.event.MessageReceivedEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.serv.AlarmIncidentModel;
import com.iris.messages.model.serv.AlarmModel;
import com.iris.messages.model.subs.AlarmSubsystemModel;
import com.iris.messages.model.subs.SecurityAlarmModeModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.messages.type.IncidentTrigger;
import com.iris.util.IrisCollections;
import com.iris.util.IrisUUID;

import junit.framework.AssertionFailedError;

public abstract class BaseAlarmSubsystemTestCase<S extends BaseSubsystem<AlarmSubsystemModel>> extends SubsystemTestCase<AlarmSubsystemModel> {
	protected Address incidentAddress = Address.platformService(UUID.randomUUID(), AlarmIncidentCapability.NAMESPACE);
	protected AlarmIncidentService incidentService;	
	protected S subsystem;
	protected Model securitySubsystem;
	protected Model safetySubsystem;
	protected Model place;
	protected Address actorAddress = Address.platformService(UUID.randomUUID(), PersonCapability.NAMESPACE);
   protected Model hub;
	protected abstract S newAlarmSubsystem(AlarmIncidentService service);
	
	@Before
	public void createSubsystem() {
		this.incidentService = EasyMock.createMock(AlarmIncidentService.class);
		this.subsystem = newAlarmSubsystem(incidentService);
		hub = addModel(ModelFixtures.createHubAttributes());
	}
	
	@Override
	protected AlarmSubsystemModel createSubsystemModel() {
		Map<String, Object> attributes = 
				ModelFixtures
					.buildSubsystemAttributes(placeId, AlarmSubsystemCapability.NAMESPACE)
					.put(SubsystemCapability.ATTR_STATE, SubsystemCapability.STATE_ACTIVE)
					.create();
		Map<String, Object> additionalAttributes = getAdditionalAttributesForModel();
		if(additionalAttributes != null && !additionalAttributes.isEmpty()) {
			attributes.putAll(additionalAttributes);
		}
		return new AlarmSubsystemModel(addModel(attributes));
	}
	
	/**
	 * Overwrite this method if you want additional attributes added when the subsystem model is created
	 * @return
	 */
	protected Map<String, Object> getAdditionalAttributesForModel() {
      return ImmutableMap.<String, Object>of();
   }
	
	@Before
	public void createSecuritySubsystem() {
		securitySubsystem = AlarmSubsystemFixture.createSecurityModel(placeId, store);
	}
	
	@Before
	public void createSafetySubsystem() {
		safetySubsystem = AlarmSubsystemFixture.createSafetyModel(placeId, store);
	}
	
	@Before
	public void createPlace() {
		place = addModel( ModelFixtures.buildPlaceAttributes(context.getPlaceId()).create() );
	}
	
	@Before
	public void addAccount() {
		accountModel.setAttribute(AccountCapability.ATTR_OWNER, IrisUUID.randomUUID());
		accountModel = addModel(accountModel.toMap());
	}
	
	protected AlarmIncidentModel stageAlarmIncident(String alert) {
		SimpleModel incident = new SimpleModel();
		incident.setAttribute(AlarmIncidentCapability.ATTR_ALERT, alert);
		incident.setAttribute(Capability.ATTR_ADDRESS, incidentAddress);
		incident.setAttribute(Capability.ATTR_ID, incidentAddress.getId());
		return new AlarmIncidentModel(incident);
	}
	
	protected void stageAlarmSubsystem() {
		context.model().setVersion("1.0");
		context.model().setState(SubsystemCapability.STATE_ACTIVE);
		context.model().setAttribute(
				Capability.ATTR_INSTANCES, 
				ImmutableMap.of(
						CarbonMonoxideAlarm.NAME, ImmutableSet.of(AlarmCapability.NAMESPACE),
						PanicAlarm.NAME, ImmutableSet.of(AlarmCapability.NAMESPACE),
						SecurityAlarm.NAME, ImmutableSet.of(AlarmCapability.NAMESPACE),
						SmokeAlarm.NAME, ImmutableSet.of(AlarmCapability.NAMESPACE),
						WaterAlarm.NAME, ImmutableSet.of(AlarmCapability.NAMESPACE)
				));
	}
	
	protected void stageDisarmed(Set<String> sensors) {
		stageAlarmSubsystem();
		context.model().setAlarmState(AlarmSubsystemCapability.ALARMSTATE_READY);
		context.model().setSecurityMode(AlarmSubsystemCapability.SECURITYMODE_DISARMED);
		updateModel(
				Address.platformService(placeId, SecuritySubsystemCapability.NAMESPACE), 
				ImmutableMap.<String, Object>of(
						NamespacedKey.representation(SecurityAlarmModeCapability.ATTR_DEVICES, AlarmSubsystemCapability.SECURITYMODE_ON), 
						sensors
				)
		);
	}
	
	protected void stageArming(String mode) {
		Date armingTimeout = SubsystemUtils.setTimeout(30, context, SecurityAlarm.NAME + ":" + SecurityArmingState.instance().getName());
		stageAlarmSubsystem();
		context.model().setAlarmState(AlarmSubsystemCapability.ALARMSTATE_READY);
		context.model().setSecurityMode(mode);
		context.model().setSecurityArmTime(armingTimeout);
		
		AlarmModel.setAlertState(SecurityAlarm.NAME, context.model(), AlarmCapability.ALERTSTATE_ARMING);
	}
	
	protected void stageArmed(String mode) {
		stageAlarmSubsystem();
		context.model().setAlarmState(AlarmSubsystemCapability.ALARMSTATE_READY);
		context.model().setSecurityMode(mode);
		context.model().setSecurityArmTime(new Date(System.currentTimeMillis() - 30000));
		
		AlarmModel.setAlertState(SecurityAlarm.NAME, context.model(), AlarmCapability.ALERTSTATE_READY);
		context.model().setAttribute(SecurityAlarm.ATTR_ARMED_DEVICES, SecurityAlarmModeModel.getDevices(mode, securitySubsystem));
	}
	
	protected void stagePreAlert(String mode, Model... triggeringDevices) {
		stageAlarmSubsystem();
		context.model().setAlarmState(AlarmSubsystemCapability.ALARMSTATE_PREALERT);
		context.model().setSecurityMode(mode);
		context.model().setSecurityArmTime(new Date(System.currentTimeMillis() - 30000));
		context.model().setCurrentIncident(incidentAddress.getRepresentation());
		
		AlarmModel.setAlertState(SecurityAlarm.NAME, context.model(), AlarmCapability.ALERTSTATE_PREALERT);
		stageTriggers(SecurityAlarm.NAME, triggeringDevices);
	}
	
	protected void stageAlerting(String... alerts) {
		SubsystemUtils.setTimeout(30, context, AlertState.class.getName());
		
		for(String alert: alerts) {
			AlarmModel.setAlertState(alert, model, AlarmCapability.ALERTSTATE_ALERT);
		}
		
		stageAlarmSubsystem();
		context.model().setActiveAlerts(Arrays.asList(alerts));
		context.model().setAlarmState(AlarmSubsystemCapability.ALARMSTATE_ALERTING);
		context.model().setCurrentIncident(incidentAddress.getRepresentation());
	}
	
	protected void stageTriggers(String alarm, Model... devices) {
		List<Map<String, Object>> triggers = new ArrayList<>();
		for(Model device: devices) {
			IncidentTrigger trigger = new IncidentTrigger();
			trigger.setAlarm(alarm);
			trigger.setEvent("???");// FIXME set proper event
			trigger.setSource(device.getAddress().getRepresentation());
			trigger.setTime(new Date());
			triggers.add(trigger.toMap());
		}
		AlarmModel.setTriggers(alarm, context.model(), triggers);
	}
	
	protected void start() throws Exception {
		replay();
		init(subsystem);
	}

	protected void reset() {
		EasyMock.reset(incidentService);
		clearRequests();
	}
	
	protected void replay() {
		EasyMock.replay(incidentService);
	}
	
	protected void verify() {
		EasyMock.verify(incidentService);
	}
	
   protected MessageBody sendRequest(MessageReceivedEvent request) {
		subsystem.onEvent(request, context);
		MessageBody response = null;
		if(responses.getValues() != null && !responses.getValues().isEmpty()) {
		   response = responses.getValues().get(responses.getValues().size()-1);  //get the last request
		}
		commit();
		return response;
   }
   
   // add in actor, should probably push down
   @Override
   protected MessageReceivedEvent request(MessageBody body, Address source, String correlationId) {
      PlatformMessage.Builder builder = PlatformMessage.buildRequest(body, source, model.getAddress()).withActor(actorAddress);
      if(correlationId != null) {
         builder.withCorrelationId(correlationId);
      }

      return new MessageReceivedEvent(builder.create());
   }

	protected Model addKeyPad() {
		return addModel(
				ModelFixtures
					.buildDeviceAttributes(KeyPadCapability.NAMESPACE)
					.put(KeyPadCapability.ATTR_ALARMMODE, KeyPadCapability.ALARMMODE_OFF)
					.put(KeyPadCapability.ATTR_ALARMMODE, KeyPadCapability.ALARMMODE_OFF)
					.put(KeyPadCapability.ATTR_ENABLEDSOUNDS, KeyPad.SOUNDS_ON)
					.create()
		);
	}
	
	protected Model addRule(String template) {
		return addModel(
				ModelFixtures
					.buildServiceAttributes(RuleCapability.NAMESPACE)
					.put(RuleCapability.ATTR_CREATED, new Date())
					.put(RuleCapability.ATTR_CONTEXT, new HashMap<String, Object>())
					.put(RuleCapability.ATTR_MODIFIED, new Date())
					.put(RuleCapability.ATTR_NAME, template)
					.put(RuleCapability.ATTR_TEMPLATE, template)
					.put(RuleCapability.ATTR_STATE, RuleCapability.STATE_ENABLED)
					.create()
		);
	}

	protected Model addSmokeDevice(String state) {
		return addSmokeDevice(true, state);
	}
	
	protected Model addOfflineSmokeDevice(String state) {
		return addSmokeDevice(false, state);
	}
	
	protected Model addSmokeDevice(boolean online, String state) {
		return addModel(
				ModelFixtures
					.buildDeviceAttributes(SmokeCapability.NAMESPACE)
					.put(DeviceConnectionCapability.ATTR_STATE, online ? DeviceConnectionCapability.STATE_ONLINE : DeviceConnectionCapability.STATE_OFFLINE)
					.put(DeviceConnectionCapability.ATTR_LASTCHANGE, new Date())
					.put(SmokeCapability.ATTR_SMOKE, state)
					.put(SmokeCapability.ATTR_SMOKECHANGED, new Date())
					.create()
			);
	}

	protected Model addCODevice(String state) {
		return addCODevice(true, state);
	}
	
	protected Model addOfflineCODevice(String state) {
		return addCODevice(false, state);
	}
	
	protected Model addCODevice(boolean online, String state) {
		return addModel(
				ModelFixtures
					.buildDeviceAttributes(CarbonMonoxideCapability.NAMESPACE)
					.put(DeviceConnectionCapability.ATTR_STATE, online ? DeviceConnectionCapability.STATE_ONLINE : DeviceConnectionCapability.STATE_OFFLINE)
					.put(DeviceConnectionCapability.ATTR_LASTCHANGE, new Date())
					.put(CarbonMonoxideCapability.ATTR_CO, state)
					.put(CarbonMonoxideCapability.ATTR_COCHANGED, new Date())
					.create()
			);
	}

	protected Model addContactDevice() {
		return addModel(
				ModelFixtures
					.buildDeviceAttributes(ContactCapability.NAMESPACE)
					.put(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_ONLINE)
					.put(DeviceConnectionCapability.ATTR_LASTCHANGE, new Date())
					.put(DeviceAdvancedCapability.ATTR_PROTOCOL, "TEST")
					.put(DeviceAdvancedCapability.ATTR_PROTOCOLID, UUID.randomUUID().toString())
					.put(ContactCapability.ATTR_CONTACT, ContactCapability.CONTACT_CLOSED)
					.put(ContactCapability.ATTR_CONTACTCHANGED, new Date())
					.create()
			);
	}

	protected Model addMotionSensor() {
		return addModel(
				ModelFixtures
					.buildDeviceAttributes(MotionCapability.NAMESPACE)
					.put(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_ONLINE)
					.put(DeviceConnectionCapability.ATTR_LASTCHANGE, new Date())
					.put(MotionCapability.ATTR_MOTION, MotionCapability.MOTION_NONE)
					.put(MotionCapability.ATTR_MOTIONCHANGED, new Date())
					.create()
			);
	}

	protected Model addLeakDetector(String state) {
		return addLeakDetector(true, state);
	}
	
	protected Model addOfflineLeakDetector(String state) {
		return addLeakDetector(false, state);
	}
	
	protected Model addLeakDetector(boolean online, String state) {
		return addModel(
				ModelFixtures
					.buildDeviceAttributes(LeakH2OCapability.NAMESPACE)
					.put(DeviceConnectionCapability.ATTR_STATE, online ? DeviceConnectionCapability.STATE_ONLINE : DeviceConnectionCapability.STATE_OFFLINE)
					.put(DeviceConnectionCapability.ATTR_LASTCHANGE, new Date())
					.put(LeakH2OCapability.ATTR_STATE, state)
					.put(LeakH2OCapability.ATTR_STATECHANGED, new Date())
					.create()
			);
	}
	
	protected Model addShutoffValve() {
		return addModel(
			ModelFixtures
				.buildDeviceAttributes(ValveCapability.NAMESPACE)
				.put(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_ONLINE)
				.put(DeviceConnectionCapability.ATTR_LASTCHANGE, new Date())
				.put(ValveCapability.ATTR_VALVESTATE, ValveCapability.VALVESTATE_OPEN)
				.put(ValveCapability.ATTR_VALVESTATECHANGED, new Date())
				.create()
		);
	}

   protected void trigger(Model model) {
   	if(model.supports(CarbonMonoxideCapability.NAMESPACE)) {
			updateModel(
					model.getAddress(), 
					ImmutableMap.<String, Object>of(
							CarbonMonoxideCapability.ATTR_CO, CarbonMonoxideCapability.CO_DETECTED,
							CarbonMonoxideCapability.ATTR_COCHANGED, new Date()
					)
			);
   	}
   	if(model.supports(ContactCapability.NAMESPACE)) {
			updateModel(
					model.getAddress(), 
					ImmutableMap.<String, Object>of(
							ContactCapability.ATTR_CONTACT, ContactCapability.CONTACT_OPENED,
							ContactCapability.ATTR_CONTACTCHANGED, new Date()
					)
			);
   	}
   	if(model.supports(GlassCapability.NAMESPACE)) {
			updateModel(
					model.getAddress(), 
					ImmutableMap.<String, Object>of(
							GlassCapability.ATTR_BREAK, GlassCapability.BREAK_DETECTED,
							GlassCapability.ATTR_BREAKCHANGED, new Date()
					)
			);
   	}
   	if(model.supports(LeakH2OCapability.NAMESPACE)) {
			updateModel(
					model.getAddress(), 
					ImmutableMap.<String, Object>of(
							LeakH2OCapability.ATTR_STATE, LeakH2OCapability.STATE_LEAK,
							LeakH2OCapability.ATTR_STATECHANGED, new Date()
					)
			);
   	}
   	if(model.supports(MotionCapability.NAMESPACE)) {
			updateModel(
					model.getAddress(), 
					ImmutableMap.<String, Object>of(
							MotionCapability.ATTR_MOTION, MotionCapability.MOTION_DETECTED,
							MotionCapability.ATTR_MOTIONCHANGED, new Date()
					)
			);
   	}
   	if(model.supports(MotorizedDoorCapability.NAMESPACE)) {
   		// FIXME motdoor goes through a much uglier transition than this mocks
			updateModel(
					model.getAddress(), 
					ImmutableMap.<String, Object>of(
							MotorizedDoorCapability.ATTR_DOORSTATE, MotorizedDoorCapability.DOORSTATE_OPEN,
							MotorizedDoorCapability.ATTR_DOORSTATECHANGED, new Date()
					)
			);
   	}
   	if(model.supports(SmokeCapability.NAMESPACE)) {
			updateModel(
					model.getAddress(), 
					ImmutableMap.<String, Object>of(
							SmokeCapability.ATTR_SMOKE, SmokeCapability.SMOKE_DETECTED,
							SmokeCapability.ATTR_SMOKECHANGED, new Date()
					)
			);
   	}
   	commit();
   }

   protected void clear(Model model) {
   	if(model.supports(ContactCapability.NAMESPACE)) {
			updateModel(
					model.getAddress(), 
					ImmutableMap.<String, Object>of(
							ContactCapability.ATTR_CONTACT, ContactCapability.CONTACT_CLOSED,
							ContactCapability.ATTR_CONTACTCHANGED, new Date()
					)
			);
   	}
   	if(model.supports(CarbonMonoxideCapability.NAMESPACE)) {
			updateModel(
					model.getAddress(), 
					ImmutableMap.<String, Object>of(
							CarbonMonoxideCapability.ATTR_CO, CarbonMonoxideCapability.CO_SAFE,
							CarbonMonoxideCapability.ATTR_COCHANGED, new Date()
					)
			);
   	}
   	if(model.supports(GlassCapability.NAMESPACE)) {
			updateModel(
					model.getAddress(), 
					ImmutableMap.<String, Object>of(
							GlassCapability.ATTR_BREAK, GlassCapability.BREAK_SAFE,
							GlassCapability.ATTR_BREAKCHANGED, new Date()
					)
			);
   	}
   	if(model.supports(LeakH2OCapability.NAMESPACE)) {
			updateModel(
					model.getAddress(), 
					ImmutableMap.<String, Object>of(
							LeakH2OCapability.ATTR_STATE, LeakH2OCapability.STATE_SAFE,
							LeakH2OCapability.ATTR_STATECHANGED, new Date()
					)
			);
   	}
   	if(model.supports(MotionCapability.NAMESPACE)) {
			updateModel(
					model.getAddress(), 
					ImmutableMap.<String, Object>of(
							MotionCapability.ATTR_MOTION, MotionCapability.MOTION_NONE,
							MotionCapability.ATTR_MOTIONCHANGED, new Date()
					)
			);
   	}
   	if(model.supports(MotorizedDoorCapability.NAMESPACE)) {
   		// FIXME motdoor goes through a much uglier transition than this mocks
			updateModel(
					model.getAddress(), 
					ImmutableMap.<String, Object>of(
							MotorizedDoorCapability.ATTR_DOORSTATE, MotorizedDoorCapability.DOORSTATE_CLOSED,
							MotorizedDoorCapability.ATTR_DOORSTATECHANGED, new Date()
					)
			);
   	}
   	if(model.supports(SmokeCapability.NAMESPACE)) {
			updateModel(
					model.getAddress(), 
					ImmutableMap.<String, Object>of(
							SmokeCapability.ATTR_SMOKE, SmokeCapability.SMOKE_SAFE,
							SmokeCapability.ATTR_SMOKECHANGED, new Date()
					)
			);
   	}
   	commit();
   }
   
	protected MessageReceivedEvent armRequest(String mode) {
		MessageBody payload =
				AlarmSubsystemCapability.ArmRequest
					.builder()
					.withMode(mode)
					.build();
		
		return request(payload);
	}

	protected MessageReceivedEvent armBypassedRequest(String mode) {
		MessageBody payload =
				AlarmSubsystemCapability.ArmBypassedRequest
					.builder()
					.withMode(mode)
					.build();
		return request(payload);
	}
	
	protected MessageReceivedEvent disarmRequest() {
		return request( AlarmSubsystemCapability.DisarmRequest.instance() );
	}
	
	protected MessageReceivedEvent panicRequest() {
		return request( AlarmSubsystemCapability.PanicRequest.instance() );
	}
	
	protected MessageReceivedEvent cancelRequest() {
      String incident = model.getCurrentIncident();
      if(StringUtils.isEmpty(incident)) {
      	throw new AssertionFailedError("There is no current incident to cancel");
      }
      
      return cancelRequest(Address.fromString(incident));
	}
	
	protected MessageReceivedEvent cancelRequest(Address incidentAddress) {
      PlatformMessage message =
      		PlatformMessage
      			.buildRequest(AlarmIncidentCapability.CancelRequest.instance(), clientAddress, incidentAddress)
      			.withActor(Address.platformService(UUID.randomUUID(), PersonCapability.NAMESPACE))
      			.create();
      
      return new MessageReceivedEvent(message);
	}
	
	protected MessageBody arm(String mode) {
		return sendRequest(armRequest(mode));
	}
	
	protected void disarm() {
		sendRequest(disarmRequest());
	}
	
	protected void panic() {
		sendRequest(panicRequest());
	}
	
	protected MessageBody cancel() {
		return sendRequest(cancelRequest());
	}
	
	protected IExpectationSetters<AlarmIncidentModel> expectCurrentIncident() {
		return EasyMock.expect(incidentService.getCurrentIncident(context));
	}
	
	protected Capture<List<IncidentTrigger>> expectAddPreAlert() {
		Capture<List<IncidentTrigger>> triggerCapture = EasyMock.newCapture();
		EasyMock
			.expect(
					incidentService.addPreAlert(EasyMock.eq(context), EasyMock.eq(SecurityAlarm.NAME), EasyMock.isA(Date.class), EasyMock.capture(triggerCapture))
			)
			.andReturn(Address.platformService(UUID.randomUUID(), AlarmIncidentCapability.NAMESPACE));
		return triggerCapture;
	}
	
	protected Capture<List<IncidentTrigger>> expectAddAlert(String alarm) {
		Capture<List<IncidentTrigger>> triggers = expectAddAlert(alarm, Address.platformService(UUID.randomUUID(), AlarmIncidentCapability.NAMESPACE));
		return triggers;
	}
	
	protected Capture<List<IncidentTrigger>> expectAddAlert(String alarm, Address incident) {
		Capture<List<IncidentTrigger>> triggerCapture = EasyMock.newCapture();
		EasyMock
			.expect(
					incidentService.addAlert(EasyMock.eq(context), EasyMock.eq(alarm), EasyMock.capture(triggerCapture))
			)
			.andReturn(incident);
		return triggerCapture;
	}
	
	protected Capture<List<IncidentTrigger>> expectUpdateIncident() {
		Capture<List<IncidentTrigger>> triggerCapture = EasyMock.newCapture();
		incidentService.updateIncident(EasyMock.eq(context), EasyMock.capture(triggerCapture));
		EasyMock.expectLastCall();
		return triggerCapture;
	}

	protected Capture<List<IncidentTrigger>> expectUpdateIncidentHistory() {
		Capture<List<IncidentTrigger>> triggerCapture = EasyMock.newCapture();
		incidentService.updateIncidentHistory(EasyMock.eq(context), EasyMock.capture(triggerCapture));
		EasyMock.expectLastCall();
		return triggerCapture;
	}
	
	protected void expectCancelIncidentAndReturnError(Throwable cause, String alert) {
		expectCancelIncidentAndReturn(Futures.<Void>immediateFailedFuture(cause), stageAlarmIncident(alert));
	}
	
	protected void expectCancelIncidentAndReturnCancelled(String alert) {
		expectCancelIncidentAndReturn(Futures.<Void>immediateFuture(null), stageAlarmIncident(alert));
	}
	
	protected void expectCancelIncidentAndReturn(
			final ListenableFuture<Void> response, 
			final AlarmIncidentModel expectedIncident
	) {
		EasyMock
			.expect(
					incidentService.cancel(EasyMock.eq(context), EasyMock.isA(Address.class), EasyMock.isA(String.class))
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
	
	protected void assertInactive() {
		assertEquals(AlarmSubsystemCapability.ALARMSTATE_INACTIVE, context.model().getAlarmState());
		assertEquals(AlarmSubsystemCapability.SECURITYMODE_INACTIVE, context.model().getSecurityMode());
	}

	protected void assertReady(String... alerts) {
		Set<String> available = IrisCollections.setOf(alerts);
		assertEquals(AlarmSubsystemCapability.ALARMSTATE_READY, context.model().getAlarmState());
		assertEquals(available.contains(AlarmSubsystemCapability.ACTIVEALERTS_SECURITY) ? AlarmSubsystemCapability.SECURITYMODE_DISARMED : AlarmSubsystemCapability.SECURITYMODE_INACTIVE, context.model().getSecurityMode());
		assertEquals(available, context.model().getAvailableAlerts());
	}

	protected void assertAlerting(String... alerts) {
		assertEquals(AlarmSubsystemCapability.ALARMSTATE_ALERTING, context.model().getAlarmState());
		assertEquals(AlarmSubsystemCapability.SECURITYMODE_INACTIVE, context.model().getSecurityMode());
		assertEquals(Arrays.asList(alerts), context.model().getActiveAlerts());
	}

}

