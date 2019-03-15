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
package com.iris.common.subsystem.alarm.security;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;
import org.junit.Before;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.iris.common.subsystem.alarm.AlarmSubsystemFixture;
import com.iris.common.subsystem.alarm.PlatformAlarmSubsystemTestCase;
import com.iris.common.subsystem.alarm.ArmingInfo;
import com.iris.common.subsystem.alarm.co.CarbonMonoxideAlarm;
import com.iris.common.subsystem.alarm.panic.PanicAlarm;
import com.iris.common.subsystem.alarm.smoke.SmokeAlarm;
import com.iris.common.subsystem.alarm.water.WaterAlarm;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.DeviceDriverAddress;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.AlarmSubsystemCapability.ArmRequest;
import com.iris.messages.capability.AlarmSubsystemCapability.DisarmRequest;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.ContactCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.GlassCapability;
import com.iris.messages.capability.MotionCapability;
import com.iris.messages.capability.MotorizedDoorCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.RuleCapability;
import com.iris.messages.capability.SceneCapability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.AlarmModel;
import com.iris.messages.model.subs.AlarmSubsystemModel;
import com.iris.messages.model.subs.SecuritySubsystemModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.messages.model.test.ModelFixtures.DeviceBuilder;

public class SecurityAlarmTestCase extends PlatformAlarmSubsystemTestCase {
	protected SecurityAlarm alarm = new SecurityAlarm();
	protected SecuritySubsystemModel securityModel;
	protected Address currentActor;

	@Before
	public void initSecuritySubsystem() {
		securityModel = AlarmSubsystemFixture.createSecurityModel(placeId, store);
	}
	
	@Override
	protected AlarmSubsystemModel createSubsystemModel() {
      Map<String, Object> attributes =
            ModelFixtures
               .buildSubsystemAttributes(placeId, AlarmSubsystemModel.NAMESPACE)
               .put(
               		Capability.ATTR_INSTANCES,
               		ImmutableMap.of(
            					CarbonMonoxideAlarm.NAME, ImmutableSet.of(AlarmCapability.NAMESPACE),
            					PanicAlarm.NAME, ImmutableSet.of(AlarmCapability.NAMESPACE),
            					SecurityAlarm.NAME, ImmutableSet.of(AlarmCapability.NAMESPACE),
            					SmokeAlarm.NAME, ImmutableSet.of(AlarmCapability.NAMESPACE),
            					WaterAlarm.NAME, ImmutableSet.of(AlarmCapability.NAMESPACE)
         				)
      			)
               .create();
		return new AlarmSubsystemModel(addModel(attributes));
	}

	protected DeviceBuilder buildDevice(String namespace, boolean online) {
		return
				ModelFixtures
					.buildDeviceAttributes(namespace)
					.put(DeviceConnectionCapability.ATTR_STATE, online ? DeviceConnectionCapability.STATE_ONLINE : DeviceConnectionCapability.STATE_OFFLINE)
					.put(DeviceConnectionCapability.ATTR_LASTCHANGE, new Date());
	}
	
	protected Model addContactSensor(boolean online, boolean triggered) {
		return addModel(
				buildDevice(ContactCapability.NAMESPACE, online)
					.put(ContactCapability.ATTR_CONTACT, triggered ? ContactCapability.CONTACT_OPENED : ContactCapability.CONTACT_CLOSED)
					.put(ContactCapability.ATTR_CONTACTCHANGED, new Date())
					.create()
		);
	}
	
	protected Model addMotionSensor(boolean online, boolean triggered) {
		return addModel(
				buildDevice(MotionCapability.NAMESPACE, online)
					.put(MotionCapability.ATTR_MOTION, triggered ? MotionCapability.MOTION_DETECTED : MotionCapability.MOTION_NONE)
					.put(MotionCapability.ATTR_MOTIONCHANGED, new Date())
					.create()
		);
	}
	
	protected Model addGlassSensor(boolean online, boolean triggered) {
		return addModel(
				buildDevice(GlassCapability.NAMESPACE, online)
					.put(GlassCapability.ATTR_BREAK, triggered ? GlassCapability.BREAK_DETECTED : GlassCapability.BREAK_SAFE)
					.put(GlassCapability.ATTR_BREAKCHANGED, new Date())
					.create()
		);
	}
	
	protected Model addGarageDoor(boolean online, boolean triggered) {
		return addModel(
				buildDevice(MotorizedDoorCapability.NAMESPACE, online)
					.put(MotorizedDoorCapability.ATTR_DOORSTATE, triggered ? MotorizedDoorCapability.DOORSTATE_OPEN : MotorizedDoorCapability.DOORSTATE_CLOSED)
					.put(MotorizedDoorCapability.ATTR_DOORSTATECHANGED, new Date())
					.create()
		);
	}
	
	protected void stageInactive() {
		context.model().setState(SubsystemCapability.STATE_ACTIVE);
		context.model().setAttribute(SecurityAlarm.ATTR_ARMED_DEVICES, ImmutableSet.<String>of());
		context.model().setSecurityMode(AlarmSubsystemCapability.SECURITYMODE_INACTIVE);
		
		AlarmModel.setAlertState(SecurityAlarm.NAME, model, AlarmCapability.ALERTSTATE_INACTIVE);
		AlarmModel.setDevices(SecurityAlarm.NAME, context.model(), ImmutableSet.<String>of());
		AlarmModel.setExcludedDevices(SecurityAlarm.NAME, context.model(), ImmutableSet.<String>of());
		AlarmModel.setActiveDevices(SecurityAlarm.NAME, context.model(), ImmutableSet.<String>of());
		AlarmModel.setTriggeredDevices(SecurityAlarm.NAME, context.model(), ImmutableSet.<String>of());
	}
	
	protected void stageDisarmed(Set<String> securityAddresses) {
		context.model().setState(SubsystemCapability.STATE_ACTIVE);
		context.model().setAttribute(SecurityAlarm.ATTR_ARMED_DEVICES, ImmutableSet.of());
		context.model().setSecurityMode(AlarmSubsystemCapability.SECURITYMODE_DISARMED);

		AlarmModel.setAlertState(SecurityAlarm.NAME, model, AlarmCapability.ALERTSTATE_DISARMED);
		AlarmModel.setDevices(SecurityAlarm.NAME, context.model(), securityAddresses);
		AlarmModel.setExcludedDevices(SecurityAlarm.NAME, context.model(), ImmutableSet.<String>of());
		AlarmModel.setActiveDevices(SecurityAlarm.NAME, context.model(), ImmutableSet.<String>of());
		AlarmModel.setTriggeredDevices(SecurityAlarm.NAME, context.model(), ImmutableSet.<String>of());
	}

	protected void stageArming(String mode, Set<String> securityAddresses) {
		context.model().setState(SubsystemCapability.STATE_ACTIVE);
		context.model().setSecurityMode(mode);
		context.model().setAttribute(SecurityAlarm.ATTR_ARMED_DEVICES, securityAddresses);
		context.setVariable(SecurityAlarm.NAME + ":" + AlarmCapability.ALERTSTATE_ARMING, System.currentTimeMillis() + 30000L);
		ArmingInfo.save(context, armRequest(mode).getMessage());
		
		AlarmModel.setAlertState(SecurityAlarm.NAME, model, AlarmCapability.ALERTSTATE_ARMING);
		AlarmModel.setDevices(SecurityAlarm.NAME, context.model(), securityAddresses);
		AlarmModel.setExcludedDevices(SecurityAlarm.NAME, context.model(), ImmutableSet.<String>of());
		AlarmModel.setActiveDevices(SecurityAlarm.NAME, context.model(), securityAddresses);
		AlarmModel.setTriggeredDevices(SecurityAlarm.NAME, context.model(), ImmutableSet.<String>of());
	}

	protected void stageArmingBypassed(String mode, Set<String> armedDevices, Set<String> bypassedDevices) {
		context.model().setState(SubsystemCapability.STATE_ACTIVE);
		context.model().setSecurityMode(mode);
		context.model().setAttribute(SecurityAlarm.ATTR_ARMED_DEVICES, Sets.union(armedDevices, bypassedDevices));
		context.setVariable(SecurityAlarm.NAME + ":" + AlarmCapability.ALERTSTATE_ARMING, System.currentTimeMillis() + 30000L);
		ArmingInfo.save(context, armRequest(mode).getMessage());
		
		AlarmModel.setAlertState(SecurityAlarm.NAME, model, AlarmCapability.ALERTSTATE_ARMING);
		AlarmModel.setDevices(SecurityAlarm.NAME, context.model(), Sets.union(armedDevices, bypassedDevices));
		AlarmModel.setExcludedDevices(SecurityAlarm.NAME, context.model(), bypassedDevices);
		AlarmModel.setActiveDevices(SecurityAlarm.NAME, context.model(), armedDevices);
		AlarmModel.setTriggeredDevices(SecurityAlarm.NAME, context.model(), ImmutableSet.<String>of());
   }

	protected void stageArmed(String mode, Set<String> securityAddresses) {
		context.model().setState(SubsystemCapability.STATE_ACTIVE);
		context.model().setSecurityMode(mode);
		context.model().setAttribute(SecurityAlarm.ATTR_ARMED_DEVICES, securityAddresses);
		
		AlarmModel.setAlertState(SecurityAlarm.NAME, context.model(), AlarmCapability.ALERTSTATE_READY);
		AlarmModel.setDevices(SecurityAlarm.NAME, context.model(), securityAddresses);
		AlarmModel.setExcludedDevices(SecurityAlarm.NAME, context.model(), ImmutableSet.<String>of());
		AlarmModel.setActiveDevices(SecurityAlarm.NAME, context.model(), securityAddresses);
		AlarmModel.setTriggeredDevices(SecurityAlarm.NAME, context.model(), ImmutableSet.<String>of());
	}

	protected void stageArmedBypassed(String mode, Set<String> clearDevices, Set<String> bypassedDevices) {
		context.model().setState(SubsystemCapability.STATE_ACTIVE);
		context.model().setSecurityMode(mode);
		context.model().setAttribute(SecurityAlarm.ATTR_ARMED_DEVICES, Sets.union(clearDevices, bypassedDevices));
		
		AlarmModel.setAlertState(SecurityAlarm.NAME, context.model(), AlarmCapability.ALERTSTATE_READY);
		AlarmModel.setDevices(SecurityAlarm.NAME, context.model(), Sets.union(clearDevices, bypassedDevices));
		AlarmModel.setExcludedDevices(SecurityAlarm.NAME, context.model(), bypassedDevices);
		AlarmModel.setActiveDevices(SecurityAlarm.NAME, context.model(), clearDevices);
		AlarmModel.setOfflineDevices(SecurityAlarm.NAME, context.model(), bypassedDevices);
		AlarmModel.setTriggeredDevices(SecurityAlarm.NAME, context.model(), ImmutableSet.<String>of());
	}

	protected void stagePreAlert(String mode, Set<String> readyDevices, Set<String> triggeredDevices) {
		context.model().setState(SubsystemCapability.STATE_ACTIVE);
		context.model().setSecurityMode(mode);
		context.model().setAttribute(SecurityAlarm.ATTR_ARMED_DEVICES, Sets.union(readyDevices, triggeredDevices));
		context.setVariable(SecurityAlarm.NAME + ":" + AlarmCapability.ALERTSTATE_PREALERT, System.currentTimeMillis() + 30000L);
		
		AlarmModel.setAlertState(SecurityAlarm.NAME, model, AlarmCapability.ALERTSTATE_PREALERT);
		AlarmModel.setDevices(SecurityAlarm.NAME, context.model(), Sets.union(readyDevices, triggeredDevices));
		AlarmModel.setExcludedDevices(SecurityAlarm.NAME, context.model(), ImmutableSet.<String>of());
		AlarmModel.setActiveDevices(SecurityAlarm.NAME, context.model(), readyDevices);
		AlarmModel.setTriggeredDevices(SecurityAlarm.NAME, context.model(), triggeredDevices);
   }

	protected void stageAlert(String mode, Set<String> readyDevices, Set<String> triggeredDevices) {
		context.model().setState(SubsystemCapability.STATE_ACTIVE);
		context.model().setSecurityMode(mode);
		context.model().setAttribute(SecurityAlarm.ATTR_ARMED_DEVICES, Sets.union(readyDevices, triggeredDevices));
		
		AlarmModel.setAlertState(SecurityAlarm.NAME, model, AlarmCapability.ALERTSTATE_ALERT);
		AlarmModel.setDevices(SecurityAlarm.NAME, context.model(), Sets.union(readyDevices, triggeredDevices));
		AlarmModel.setExcludedDevices(SecurityAlarm.NAME, context.model(), ImmutableSet.<String>of());
		AlarmModel.setActiveDevices(SecurityAlarm.NAME, context.model(), readyDevices);
		AlarmModel.setTriggeredDevices(SecurityAlarm.NAME, context.model(), triggeredDevices);
   }

   protected void armViaApp(String mode) {
   	arm(mode, clientAddress, Address.platformService(UUID.randomUUID(), PersonCapability.NAMESPACE));
   }
   
   protected void arm(String mode, Address source, @Nullable Address actor) {
   	PlatformMessage message = 
      		PlatformMessage
      			.builder()
      			.from(source)
      			.to(model.getAddress())
      			.withActor(actor)
      			.withPayload(ArmRequest.builder().withMode(mode).build())
      			.create();
   	alarm.arm(context, message, mode);
   }
   
   protected void armBypassedViaApp(String mode) {
   	armBypassed(mode, clientAddress, Address.platformService(UUID.randomUUID(), PersonCapability.NAMESPACE));
   }
   
   protected void armBypassed(String mode, Address source, @Nullable Address actor) {
   	PlatformMessage message = 
      		PlatformMessage
      			.builder()
      			.from(source)
      			.to(model.getAddress())
      			.withActor(actor)
      			.withPayload(ArmRequest.builder().withMode(mode).build())
      			.create();
   	alarm.armBypassed(context, message, mode);
   }
   
   protected String disarmViaApp() {
      Address actor = createActor();
   	disarm(clientAddress, actor);
   	return actor.getRepresentation();
   }
   
   protected String disarmViaKeyPad() {
      return disarmViaDevice(Address.deviceAddress(DeviceCapability.NAMESPACE, UUID.randomUUID()));
   }
   
   protected String disarmViaDevice(Address device) {
      Address actor = createActor();
      disarm(
            device,
            actor
      );
      return actor.getRepresentation();
   }
   
   protected Address createActor() {
      return Address.platformService(UUID.randomUUID(), PersonCapability.NAMESPACE);
   }
   
   protected String disarmViaRule() {
      DeviceDriverAddress ruleAddress = Address.deviceAddress(RuleCapability.NAMESPACE, UUID.randomUUID());
   	disarm(ruleAddress,
   			null
		);
   	return ruleAddress.getRepresentation();
   }
   
   protected void disarmViaScene() {
   	disarm(
   			Address.deviceAddress(SceneCapability.NAMESPACE, UUID.randomUUID()),
   			null
		);
   }
   
   protected void disarm(Address source, @Nullable Address actor) {
   	PlatformMessage message = 
   		PlatformMessage
   			.builder()
   			.from(source)
   			.to(model.getAddress())
   			.withActor(actor)
   			.withPayload(DisarmRequest.instance())
   			.create();
   	alarm.disarm(context, message);
   }
   
	protected MessageBody cancel() {
		// just invoke cancel directly on this alarm because the full alarm subsystem is not staged
		PlatformMessage msg = cancelRequest(incidentAddress).getMessage();
		alarm.cancel(context, msg);
		return msg.getValue();
	}
			
   protected void assertInactive() {
		assertEquals(AlarmCapability.ALERTSTATE_INACTIVE, AlarmModel.getAlertState(SecurityAlarm.NAME, context.model()));
		assertEquals(addressesOf(), context.model().getAttribute(SecurityAlarm.ATTR_ARMED_DEVICES));
		assertEquals(addressesOf(), AlarmModel.getDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getActiveDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getExcludedDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getOfflineDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getTriggeredDevices(SecurityAlarm.NAME, context.model()));
	}
	
   protected void assertDisarmed(Set<String> devices) {
		assertEquals(AlarmCapability.ALERTSTATE_DISARMED, AlarmModel.getAlertState(SecurityAlarm.NAME, context.model()));
		assertEquals(devices, AlarmModel.getDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(addressesOf(), context.model().getAttribute(SecurityAlarm.ATTR_ARMED_DEVICES));
		assertEquals(addressesOf(), AlarmModel.getActiveDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getExcludedDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getOfflineDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getTriggeredDevices(SecurityAlarm.NAME, context.model()));
   }

   protected void assertArming(Set<String> allDevicesArmed) {
   	assertArming(allDevicesArmed, allDevicesArmed);
   }
   
   protected void assertArming(Set<String> allDevices, Set<String> armedDevices) {
		assertEquals(AlarmCapability.ALERTSTATE_ARMING, AlarmModel.getAlertState(SecurityAlarm.NAME, context.model()));
		assertEquals(allDevices, AlarmModel.getDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(armedDevices, context.model().getAttribute(SecurityAlarm.ATTR_ARMED_DEVICES));
		assertEquals(armedDevices, AlarmModel.getActiveDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getExcludedDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getOfflineDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getTriggeredDevices(SecurityAlarm.NAME, context.model()));
		assertTimeoutSet(SecurityAlarm.NAME + ":" + AlarmCapability.ALERTSTATE_ARMING);
   }

   protected void assertArmingBypassed(Set<String> activeDevices, Set<String> bypassedDevices) {
		assertEquals(AlarmCapability.ALERTSTATE_ARMING, AlarmModel.getAlertState(SecurityAlarm.NAME, context.model()));
		assertEquals(Sets.union(activeDevices, bypassedDevices), AlarmModel.getDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(Sets.union(activeDevices, bypassedDevices), context.model().getAttribute(SecurityAlarm.ATTR_ARMED_DEVICES));
		assertEquals(activeDevices, AlarmModel.getActiveDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(bypassedDevices, AlarmModel.getExcludedDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getOfflineDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(bypassedDevices, AlarmModel.getTriggeredDevices(SecurityAlarm.NAME, context.model()));
		assertTimeoutSet(SecurityAlarm.NAME + ":" + AlarmCapability.ALERTSTATE_ARMING);
   }

   protected void assertArmed(Set<String> devices) {
		assertEquals(AlarmCapability.ALERTSTATE_READY, AlarmModel.getAlertState(SecurityAlarm.NAME, context.model()));
		assertEquals(devices, AlarmModel.getDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(devices, AlarmModel.getActiveDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getExcludedDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getOfflineDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getTriggeredDevices(SecurityAlarm.NAME, context.model()));
   }

   protected void assertArmedWithTriggers(Set<String> armed, Set<String> triggered) {
		assertEquals(AlarmCapability.ALERTSTATE_READY, AlarmModel.getAlertState(SecurityAlarm.NAME, context.model()));
		assertEquals(Sets.union(armed, triggered), AlarmModel.getDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(armed, AlarmModel.getActiveDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getExcludedDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getOfflineDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(triggered, AlarmModel.getTriggeredDevices(SecurityAlarm.NAME, context.model()));
   }
   
   protected void assertArmedWithOffline(Set<String> armed, Set<String> offline) {
		assertEquals(AlarmCapability.ALERTSTATE_READY, AlarmModel.getAlertState(SecurityAlarm.NAME, context.model()));
		assertEquals(Sets.union(armed, offline), AlarmModel.getDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(armed, AlarmModel.getActiveDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getExcludedDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(offline, AlarmModel.getOfflineDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getTriggeredDevices(SecurityAlarm.NAME, context.model()));
   }
   
   protected void assertArmedWithBypassed(Set<String> armed, Set<String> triggered, Set<String> offline, Set<String> bypassed) {
		assertEquals(AlarmCapability.ALERTSTATE_READY, AlarmModel.getAlertState(SecurityAlarm.NAME, context.model()));
		assertEquals(Sets.union(armed, bypassed), AlarmModel.getDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(armed, AlarmModel.getActiveDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(bypassed, AlarmModel.getExcludedDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(offline, AlarmModel.getOfflineDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(triggered, AlarmModel.getTriggeredDevices(SecurityAlarm.NAME, context.model()));
   }
   
   protected void assertPreAlert(Set<String> ready, Set<String> triggered) {
   	assertPreAlert(ready, triggered, ImmutableSet.<String>of());
   	
   }
   
   protected void assertPreAlert(Set<String> ready, Set<String> triggered, Set<String> bypassed) {
		assertEquals(AlarmCapability.ALERTSTATE_PREALERT, AlarmModel.getAlertState(SecurityAlarm.NAME, context.model()));
		assertEquals(Sets.union(Sets.union(ready, triggered), bypassed), AlarmModel.getDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(ready, AlarmModel.getActiveDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(bypassed, AlarmModel.getExcludedDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getOfflineDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(triggered, AlarmModel.getTriggeredDevices(SecurityAlarm.NAME, context.model()));
		assertTimeoutSet(SecurityAlarm.NAME + ":" + AlarmCapability.ALERTSTATE_PREALERT);
   }

   protected void assertAlerting(Set<String> ready, Set<String> triggered) {
		assertEquals(AlarmCapability.ALERTSTATE_ALERT, AlarmModel.getAlertState(SecurityAlarm.NAME, context.model()));
		assertEquals(Sets.union(ready, triggered), AlarmModel.getDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(ready, AlarmModel.getActiveDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getExcludedDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getOfflineDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(triggered, AlarmModel.getTriggeredDevices(SecurityAlarm.NAME, context.model()));
   }

   protected void assertClearing(Set<String> ready, Set<String> triggered) {
		assertEquals(AlarmCapability.ALERTSTATE_CLEARING, AlarmModel.getAlertState(SecurityAlarm.NAME, context.model()));
		assertEquals(Sets.union(ready, triggered), AlarmModel.getDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(ready, AlarmModel.getActiveDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getExcludedDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(addressesOf(), AlarmModel.getOfflineDevices(SecurityAlarm.NAME, context.model()));
		assertEquals(triggered, AlarmModel.getTriggeredDevices(SecurityAlarm.NAME, context.model()));
	}
	
}

