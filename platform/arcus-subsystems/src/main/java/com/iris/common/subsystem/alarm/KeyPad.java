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

import java.util.Date;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.alarm.security.SecurityAlarm;
import com.iris.common.subsystem.alarm.subs.AlarmSubsystemState;
import com.iris.common.subsystem.alarm.subs.AlarmSubsystemState.Name;
import com.iris.common.subsystem.alarm.subs.AlertState;
import com.iris.common.subsystem.util.AddressesVariableBinder;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.HubAlarmCapability;
import com.iris.messages.capability.KeyPadCapability;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.KeyPadModel;
import com.iris.messages.model.serv.AlarmModel;
import com.iris.messages.model.serv.HubAlarmModel;
import com.iris.messages.model.subs.AlarmSubsystemModel;
import com.iris.messages.model.subs.SecurityAlarmModeModel;
import com.iris.model.predicate.Predicates;

public class KeyPad {

	private static final Logger logger = LoggerFactory.getLogger(KeyPad.class);

	public static final Predicate<Model> isKeypad = Predicates.isA(KeyPadCapability.NAMESPACE);
	
   public static final Set<String> SOUNDS_OFF = ImmutableSet.of();
   public static final Set<String> SOUNDS_KEYPAD_ONLY = ImmutableSet.of(
         KeyPadCapability.ENABLEDSOUNDS_ARMED,
         KeyPadCapability.ENABLEDSOUNDS_ARMING,
         KeyPadCapability.ENABLEDSOUNDS_BUTTONS,
         KeyPadCapability.ENABLEDSOUNDS_DISARMED
   );
   public static final Set<String> SOUNDS_ALARM_ONLY = ImmutableSet.of(KeyPadCapability.ALARMSTATE_ALERTING);
   public static final Set<String> SOUNDS_ON = ImmutableSet.of(
         KeyPadCapability.ENABLEDSOUNDS_ARMED,
         KeyPadCapability.ENABLEDSOUNDS_ARMING,
         KeyPadCapability.ENABLEDSOUNDS_BUTTONS,
         KeyPadCapability.ENABLEDSOUNDS_DISARMED,
         KeyPadCapability.ENABLEDSOUNDS_SOAKING,
         KeyPadCapability.ENABLEDSOUNDS_ALERTING
   );

	public static KeyPad get(SubsystemContext<AlarmSubsystemModel> context, Address keypadAddress) {
		KeyPad keypad = context.getVariable("keypad:" + keypadAddress.getRepresentation()).as(KeyPad.class);
		if(keypad == null) {
			keypad = new KeyPad();
		}
		keypad.context = context;
		keypad.address = keypadAddress;
		return keypad;
	}

	public static KeyPad get(SubsystemContext<AlarmSubsystemModel> context, Model keypadModel) {
		KeyPad keypad = context.getVariable("keypad:" + keypadModel.getAddress().getRepresentation()).as(KeyPad.class);
		if(keypad == null) {
			keypad = new KeyPad();
		}
		keypad.context = context;
		keypad.address = keypadModel.getAddress();
		keypad.model = keypadModel;
		return keypad;
	}
	
	public static void bind(SubsystemContext<AlarmSubsystemModel> context) {
		// sync the known keypads, new keypads will get caught in Binder#afterAdded
		syncSounds(context);
		Binder.instance().bind(context);
	}
	
	public static void syncSounds(SubsystemContext<AlarmSubsystemModel> context) {
		Name state = getState(context);
		KeyPadAlarmMode mode = getMode(state, context);
		Set<String> sounds = getEnabledSounds(context, state, mode);
		Set<String> pairedKeyPads = Binder.instance().getAddresses(context);
		for(String keyPad: pairedKeyPads) {
			Model m = context.models().getModelByAddress(Address.fromString(keyPad));
			if(m == null) {
				// removed
				continue;
			}
			
			KeyPad keypad = get(context, m);
			keypad.syncSounds(sounds);
			keypad.syncState(state, mode);
		}
	}
	
	public static MessageBody createAlertingRequest(KeyPadAlertMode alertMode) {
		return KeyPadCapability.AlertingRequest.builder()
			.withAlarmMode(alertMode.name())
			.build();
	}
	
	/**
	 * Sync sounds with each keypad, and also send AlertingRequest
	 * @param context
	 * @param alertMode
	 */
	public static void sendAlert(SubsystemContext<AlarmSubsystemModel> context, KeyPadAlertMode alertMode) {
		KeyPadAlarmMode securityMode = getMode(AlertState.instance().getName(), context);
		Set<String> sounds = getEnabledSounds(context, AlertState.instance().getName(), securityMode);
		MessageBody alert = createAlertingRequest(alertMode);
		
		for(Model m: getKeypads(context)) {
			KeyPad keypad = get(context, m);
			keypad.syncSounds(sounds);
			keypad.send(alert);
		}
	}
	
	public static Iterable<Model> getKeypads(SubsystemContext<?> context) {
		return context.models().getModels(isKeypad);
	}

	private static Set<String> getEnabledSounds(SubsystemContext<AlarmSubsystemModel> context, Name state, KeyPadAlarmMode mode) {
		Model securitySubsystemModel = context.models().getModelByAddress(Address.platformService(context.getPlaceId(), SecuritySubsystemCapability.NAMESPACE));
		boolean isKeypadSoundsEnabled = SecurityAlarmModeModel.getSoundsEnabled(mode == KeyPadAlarmMode.PARTIAL ? KeyPadAlarmMode.PARTIAL.name() : KeyPadAlarmMode.ON.name(), securitySubsystemModel, true);
		boolean isAlarmSoundsEnabled = false;
		if(state == Name.ALERT) {
			for(String alert: context.model().getActiveAlerts()) {
				if(!AlarmModel.getSilent(alert, context.model(), false)) {
					isAlarmSoundsEnabled = true;
					break;
				}
			}
		}
		else {
			isAlarmSoundsEnabled = !AlarmModel.getSilent(SecurityAlarm.NAME, context.model(), false);
		}
		
		if(isKeypadSoundsEnabled) {
			if(isAlarmSoundsEnabled) {
				return SOUNDS_ON;
			}
			else {
				return SOUNDS_KEYPAD_ONLY;
			}
		}
		else {
			if(isAlarmSoundsEnabled) {
				return SOUNDS_ALARM_ONLY;
			}
			else {
				return SOUNDS_OFF;
			}
		}
	}

	private static Name getState(SubsystemContext<AlarmSubsystemModel> context) {
		if(AlarmSubsystemCapability.ALARMPROVIDER_HUB.equals(context.model().getAlarmProvider())) {
			return getStateHub(context);
		}
		return AlarmSubsystemState.get(context).getName();
	}

	private static Name getStateHub(SubsystemContext<AlarmSubsystemModel> context) {
		Model hub = AlarmUtil.getHubModel(context, false);
		if(hub == null) {
			logger.warn("hub local alarms enabled, but no hub is present getting the state to sync keypad sounds, falling back to disarmed");
			return Name.DISARMED;
		}
		Name name;
		String alarmState = HubAlarmModel.getAlarmState(hub, HubAlarmCapability.ALARMSTATE_INACTIVE);
		String secAlarmState = HubAlarmModel.getSecurityAlertState(hub, HubAlarmCapability.SECURITYALERTSTATE_INACTIVE);
		switch(alarmState) {
			case HubAlarmCapability.ALARMSTATE_INACTIVE:
			case HubAlarmCapability.ALARMSTATE_CLEARING:
				name = Name.DISARMED;
				break;

			case HubAlarmCapability.ALARMSTATE_READY:
				switch(secAlarmState) {
					case HubAlarmCapability.SECURITYALERTSTATE_INACTIVE:
					case HubAlarmCapability.SECURITYALERTSTATE_DISARMED:
					case HubAlarmCapability.SECURITYALERTSTATE_CLEARING:
					case HubAlarmCapability.SECURITYALERTSTATE_PENDING_CLEAR:
						name = Name.DISARMED;
						break;

					case HubAlarmCapability.SECURITYALERTSTATE_ARMING:
						name = Name.ARMING;
						break;

					case HubAlarmCapability.SECURITYALERTSTATE_READY:
						name = Name.ARMED;
						break;

					default:
						context.logger().warn("AlarmSubsystem state [{}] and security alarm state [{}] are inconsistent", alarmState, secAlarmState);
						name = Name.DISARMED;
				}
				break;

			case HubAlarmCapability.ALARMSTATE_PREALERT:
				name = Name.PREALERT;
				break;

			case HubAlarmCapability.ALARMSTATE_ALERTING:
				name = Name.ALERT;
				break;

			default:
				context.logger().warn("AlarmSubsystem state [{}] is not recognized", alarmState);
				name = Name.DISARMED;
		}
		return name;
	}

	private static KeyPadAlarmMode getMode(Name state, SubsystemContext<AlarmSubsystemModel> context) {
		if(state == Name.DISARMED) {
			return KeyPadAlarmMode.OFF;
		}
		return 
			AlarmSubsystemModel.isSecurityModePARTIAL(context.model()) ? 
				KeyPadAlarmMode.PARTIAL : 
				KeyPadAlarmMode.ON;
	}
	
	private static String getKeyPadState(Name state) {
		switch(state) {
		case DISARMED: return KeyPadCapability.ALARMSTATE_DISARMED;
		case ARMING:   return KeyPadCapability.ALARMSTATE_ARMING;
		case ALERT:    return KeyPadCapability.ALARMSTATE_ALERTING;
		case PREALERT: return KeyPadCapability.ALARMSTATE_SOAKING;
		case ARMED:    return KeyPadCapability.ALARMSTATE_ARMED;
		default: throw new IllegalArgumentException("Unrecognized state: " + state);
		}
	}

	private transient SubsystemContext<AlarmSubsystemModel> context;
	private transient Address address;
	private transient Model model;
	private Date bypassedUntil;
	
	private KeyPad() {
		this.bypassedUntil = null;
	}
	
	public Model getModel() {
		if(model == null) {
			model = context.models().getModelByAddress(address);
		}
		return model;
	}
	
	public boolean isBypassed() {
		return isBypassed(new Date());
	}
	
	public boolean isBypassed(Date at) {
		return bypassedUntil != null && bypassedUntil.after(at);
	}

	public void setBypassDelay(long bypassDelayMs) {
		this.bypassedUntil = new Date(System.currentTimeMillis() + bypassDelayMs);
		context.setVariable("keypad:" + address.getRepresentation(), this);
	}
	
	public void sendRetryBypassed(long bypassDelayMs) {
		setBypassDelay(bypassDelayMs);
		sendArmUnavailable();
	}
	
	public void sendArmUnavailable() {
		MessageBody unableToArm = 
				KeyPadCapability
					.ArmingUnavailableRequest
					.instance();
		context.request(address, unableToArm);
	}

	public void send(MessageBody message) {
		context.request(address, message);
	}
	
	public void onAdded() {
		Name state = getState(context);
		KeyPadAlarmMode mode = getMode(state, context);
		Set<String> sounds = getEnabledSounds(context, state, mode);
		syncSounds(sounds);
		syncState(state, mode);
	}
	
	public void onRemoved() {
		context.setVariable("keypad:" + address.getRepresentation(), null);
	}
	
   public void syncSounds(Set<String> sounds) {
   	Model m = getModel();
      if(!sounds.equals(KeyPadModel.getEnabledSounds(m))) {
         MessageBody setSounds = MessageBody.buildMessage(
               Capability.CMD_SET_ATTRIBUTES, 
               ImmutableMap.<String, Object>of(KeyPadCapability.ATTR_ENABLEDSOUNDS, sounds)
         );
         context.request(m.getAddress(), setSounds);    
      }
   }

   public void syncState(Name alarmState, KeyPadAlarmMode mode) {
   	String state = getKeyPadState(alarmState);
   	Model m = getModel();
      if(
      		KeyPadModel.getAlarmState(m, "").equals(state) && 
      		KeyPadModel.getAlarmMode(m, "").equals(mode.name())
		) {
         return;
      }
      
      MessageBody attributes = MessageBody.buildMessage(
            Capability.CMD_SET_ATTRIBUTES, 
            ImmutableMap.<String, Object>of(
                  KeyPadCapability.ATTR_ALARMSTATE, state,
                  KeyPadCapability.ATTR_ALARMMODE, mode.name()
            )
      );
      context.request(m.getAddress(), attributes);    
   }
   
   public enum KeyPadAlarmMode {
      OFF,
      ON,
      PARTIAL
   }
   
   public enum KeyPadAlertMode {
      ON,
      PARTIAL,
      PANIC
   }
   
   private static class Binder extends AddressesVariableBinder<AlarmSubsystemModel> {
   	private static Binder INSTANCE = new Binder();
   	
   	public static Binder instance() {
   		return INSTANCE;
   	}

   	public Binder() {
   		super(isKeypad, "keypads");
   	}

		@Override
		protected void afterAdded(SubsystemContext<AlarmSubsystemModel> context, Model model) {
			get(context, model).onAdded();
		}

		@Override
		protected void afterRemoved(SubsystemContext<AlarmSubsystemModel> context, Address address) {
			get(context, address).onRemoved();
		}

   }
   
}

