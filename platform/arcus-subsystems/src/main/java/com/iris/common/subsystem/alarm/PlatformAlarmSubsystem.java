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

import static com.iris.common.subsystem.alarm.subs.AlarmSubsystemState.transition;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.annotation.Version;
import com.iris.capability.key.NamespacedKey;
import com.iris.common.subsystem.BaseSubsystem;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.common.subsystem.alarm.AlarmUtil.CheckSecurityModeOption;
import com.iris.common.subsystem.alarm.co.CarbonMonoxideAlarm;
import com.iris.common.subsystem.alarm.generic.AlarmState.TriggerEvent;
import com.iris.common.subsystem.alarm.incident.AlarmIncidentService;
import com.iris.common.subsystem.alarm.panic.PanicAlarm;
import com.iris.common.subsystem.alarm.security.SecurityAlarm;
import com.iris.common.subsystem.alarm.security.SecurityErrors;
import com.iris.common.subsystem.alarm.smoke.SmokeAlarm;
import com.iris.common.subsystem.alarm.subs.AlarmSubsystemState;
import com.iris.common.subsystem.alarm.subs.AlarmSubsystemState.Name;
import com.iris.common.subsystem.alarm.water.WaterAlarm;
import com.iris.common.subsystem.annotation.Subsystem;
import com.iris.common.subsystem.security.SecuritySubsystemUtil;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.AlarmIncidentCapability;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.HubConnectionCapability;
import com.iris.messages.capability.KeyPadCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.RuleCapability;
import com.iris.messages.capability.SafetySubsystemCapability;
import com.iris.messages.capability.SecurityAlarmModeCapability;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.event.ModelAddedEvent;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.event.ModelRemovedEvent;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.listener.annotation.OnAdded;
import com.iris.messages.listener.annotation.OnMessage;
import com.iris.messages.listener.annotation.OnRemoved;
import com.iris.messages.listener.annotation.OnScheduledEvent;
import com.iris.messages.listener.annotation.OnValueChanged;
import com.iris.messages.listener.annotation.Request;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.AlarmIncidentModel;
import com.iris.messages.model.serv.AlarmModel;
import com.iris.messages.model.subs.AlarmSubsystemModel;
import com.iris.messages.service.AlarmService;
import com.iris.messages.type.IncidentTrigger;
import com.iris.util.TypeMarker;

@Singleton
@Subsystem(AlarmSubsystemModel.class)
@Version(1)
public class PlatformAlarmSubsystem extends BaseSubsystem<AlarmSubsystemModel> {
	public static final String VAR_TRIGGERINFO = "triggerInfo";
	
	private final AlarmIncidentService incidentService;
	private final SecurityAlarm security = new SecurityAlarm();
	private final PanicAlarm panic = new PanicAlarm();
	private final CarbonMonoxideAlarm co = new CarbonMonoxideAlarm();
	private final SmokeAlarm smoke = new SmokeAlarm();
	private final WaterAlarm water = new WaterAlarm();
	
	private final CallTree callTree = new CallTree();
	
	private boolean activeOnAdd = false;

	@Inject
	public PlatformAlarmSubsystem(AlarmIncidentService incidentService) {
		this(incidentService, true);
	}
	
	public PlatformAlarmSubsystem(AlarmIncidentService incidentService, boolean activeOnAdd) {
		this.incidentService = incidentService;
		this.activeOnAdd = activeOnAdd;
	}

	@Override
	protected void onAdded(SubsystemContext<AlarmSubsystemModel> context) {
		super.onAdded(context);
		
		if(activeOnAdd) {
			context.model().setState(SubsystemCapability.STATE_ACTIVE);
			context.model().setAvailable(true);
		}
		else {
			context.model().setState(SubsystemCapability.STATE_SUSPENDED);
			context.model().setAvailable(false);
		}
		
		context.model().setActiveAlerts(ImmutableList.<String>of());
		context.model().setAlarmState(AlarmSubsystemCapability.ALARMSTATE_INACTIVE);
		context.model().setAvailableAlerts(ImmutableSet.<String>of());
		context.model().setCurrentIncident("");
		context.model().setMonitoredAlerts(ImmutableSet.<String>of(CarbonMonoxideAlarm.NAME, PanicAlarm.NAME, SecurityAlarm.NAME, SmokeAlarm.NAME));
		context.model().setTestModeEnabled(false);
		context.model().setFanShutoffSupported(AlarmSubsystem.FAN_SHUTOFF_SUPPORTED_DEFAULT);
		context.model().setFanShutoffOnCO(AlarmSubsystem.FAN_SHUTOFF_ON_CO_DEFAULT);  
		context.model().setFanShutoffOnSmoke(AlarmSubsystem.FAN_SHUTOFF_ON_SMOKE_DEFAULT);  
		context.model().setRecordingSupported(false);
		context.model().setRecordingDurationSec(RecordOnSecurityAdapter.RECORDING_DURATION_DEFAULT); 
		context.model().setRecordOnSecurity(RecordOnSecurityAdapter.RECORD_ON_SECURITY_DEFAULT);
	}

	@Override
	protected void onStarted(SubsystemContext<AlarmSubsystemModel> context) {
		super.onStarted(context);
		
		context.model().setMonitoredAlerts(ImmutableSet.<String>of(CarbonMonoxideAlarm.NAME, PanicAlarm.NAME, SecurityAlarm.NAME, SmokeAlarm.NAME));
		
		security.bind(context);
		panic.bind(context);
		co.bind(context);
		smoke.bind(context);
		water.bind(context);
		callTree.bind(context);
		
		syncAlerts(context);
		AlarmSubsystemState.start(context);
		SubsystemUtils.restoreTimeout(context, "cancel");
		AlarmUtil.syncFanShutoff(context);
		AlarmUtil.syncRecordOnSecurity(context);
		SubsystemUtils.setIfNull(context.model(), AlarmSubsystemCapability.ATTR_REQUESTEDALARMPROVIDER, AlarmSubsystemCapability.REQUESTEDALARMPROVIDER_HUB);
	}   

	@Override
   protected void onStopped(SubsystemContext<AlarmSubsystemModel> context) {
   	super.onStopped(context);
   	context.unbind();
   }

	@Override
	protected void setAttribute(String name, Object value, SubsystemContext<AlarmSubsystemModel> context) throws ErrorEventException {
		switch(name) {
		case AlarmSubsystemCapability.ATTR_CALLTREE:
			callTree.assertValid(context, (List<Map<String, Object>>) AlarmSubsystemCapability.TYPE_CALLTREE.coerce(value));
			break;
		case AlarmSubsystemCapability.ATTR_MONITOREDALERTS:
			throw new ErrorEventException(Errors.invalidRequest("Changing the monitored alerts is not currently supported"));
		}
		
		super.setAttribute(name, value, context);
	}

	@Request(SubsystemCapability.ActivateRequest.NAME)
	public void activate(SubsystemContext<AlarmSubsystemModel> context) {
		if(AlarmUtil.isActive(context)) {
			return;
		}
		
		assertValidActivateState(context);
		AlarmUtil.copyCallTree(context, callTree);
		AlarmUtil.copySilent(context);
		syncAlerts(context);
		// security needs extra handling to enable upgrading while armed
		security.activate(context);
		
		context.model().setState(SubsystemCapability.STATE_ACTIVE);
		context.model().setAvailable(true);
	}
	
	@Request(SubsystemCapability.SuspendRequest.NAME)
	public void suspend(SubsystemContext<AlarmSubsystemModel> context) {
		if(!AlarmUtil.isActive(context)) {
			return;
		}
		
		assertValidSuspendState(context);
		context.model().setState(SubsystemCapability.STATE_SUSPENDED);
		context.model().setAvailable(false);
	}
	
	/////////////////////////////////////////////////////////////
	// Alarm state management
	/////////////////////////////////////////////////////////////
	
	@OnValueChanged(
			query=AlarmSubsystem.QUERY_ALARM,
			attributes={
					AlarmCapability.ATTR_ALERTSTATE + ":" + AlarmSubsystemCapability.ACTIVEALERTS_CO,
					AlarmCapability.ATTR_ALERTSTATE + ":" + AlarmSubsystemCapability.ACTIVEALERTS_SMOKE,
					AlarmCapability.ATTR_ALERTSTATE + ":" + AlarmSubsystemCapability.ACTIVEALERTS_PANIC,
					AlarmCapability.ATTR_ALERTSTATE + ":" + AlarmSubsystemCapability.ACTIVEALERTS_SECURITY,
					AlarmCapability.ATTR_ALERTSTATE + ":" + AlarmSubsystemCapability.ACTIVEALERTS_WATER
			}
	)
	public void onAlarmStateChanged(SubsystemContext<AlarmSubsystemModel> context, Model model, ModelChangedEvent event) {
		syncAlerts(context);
		if(event.getAttributeName().equals(AlarmCapability.ATTR_ALERTSTATE+":"+AlarmSubsystemCapability.ACTIVEALERTS_SECURITY)) {
			if(context.model().isSecurityModeDISARMED() || context.model().isSecurityModeINACTIVE()) {
				AlarmUtil.syncAlarmProviderIfNecessary(context, true, null, CheckSecurityModeOption.DISARMED_OR_INACTIVE);
			}
		}
	}
	
	@OnValueChanged(
			query=AlarmSubsystem.QUERY_ALARM,
			attributes={
					AlarmCapability.ATTR_TRIGGERS + ":" + AlarmSubsystemCapability.ACTIVEALERTS_CO,
					AlarmCapability.ATTR_TRIGGERS + ":" + AlarmSubsystemCapability.ACTIVEALERTS_SMOKE,
					AlarmCapability.ATTR_TRIGGERS + ":" + AlarmSubsystemCapability.ACTIVEALERTS_PANIC,
					AlarmCapability.ATTR_TRIGGERS + ":" + AlarmSubsystemCapability.ACTIVEALERTS_SECURITY,
					AlarmCapability.ATTR_TRIGGERS + ":" + AlarmSubsystemCapability.ACTIVEALERTS_WATER
			}
	)
	public void onAlarmTriggersChanged(SubsystemContext<AlarmSubsystemModel> context, Model model, ModelChangedEvent event) {
		List<Map<String, Object>> newValue = (List<Map<String, Object>>) event.getAttributeValue();
		if(newValue == null || newValue.isEmpty()) {
			// clearing triggers, drop out
			return;
		}
		
		String alarm = NamespacedKey.parse(event.getAttributeName()).getInstance();
		TriggerInfo info = getTriggerInfo(context);
		if(info.isSignalled(alarm)) {
			List<IncidentTrigger> triggers = AlarmUtil.eventsToTriggers(newValue, info.getIndex(alarm));
			// empty triggers generally means that it was already handled by syncAlerts
			if(!triggers.isEmpty()) {
				incidentService.updateIncident(context, triggers);
				info.setIndex(alarm, newValue.size());
				setTriggerInfo(context, info);
			}
		} else if(info.isPreAlert(alarm)) {
			List<IncidentTrigger> triggers = AlarmUtil.eventsToTriggers(newValue, info.getPreAlertIndex(alarm));
			if(!triggers.isEmpty()) {
				incidentService.updateIncidentHistory(context, triggers);
				info.setPreAlertIndex(alarm, newValue.size());
				setTriggerInfo(context, info);
			}
		}
	}	
	
	@OnValueChanged(
			query=AlarmSubsystem.QUERY_SECURITY_SUBSYSTEM,
			attributes={
					SecurityAlarmModeCapability.ATTR_SOUNDSENABLED + ":" + AlarmSubsystemCapability.SECURITYMODE_ON,
					SecurityAlarmModeCapability.ATTR_SOUNDSENABLED + ":" + AlarmSubsystemCapability.SECURITYMODE_PARTIAL
			}
	)
	public void onKeyPadSoundsChanges(SubsystemContext<AlarmSubsystemModel> context) {
		AlarmUtil.syncKeyPadSounds(context);
	}
	
	
	@OnValueChanged(
		query=AlarmSubsystem.QUERY_ALARM,
		attributes=AlarmCapability.ATTR_SILENT + ":" + AlarmSubsystemCapability.ACTIVEALERTS_SECURITY
   )
   public void onSilentChanged(SubsystemContext<AlarmSubsystemModel> context, Model model, ModelChangedEvent event) {
   	AlarmUtil.syncKeyPadSounds(context);
   }

	// timeout
//	@OnScheduledEvent(name = "cancel") // TODO implement this
	@OnScheduledEvent
	public void onEvent(ScheduledEvent event, SubsystemContext<AlarmSubsystemModel> context) {
		if(SubsystemUtils.isMatchingTimeout(event, context, AlarmUtil.TO_CANCEL)) {
			tryCancel(context);
		}
		security.onTimeout(context, event);
		AlarmSubsystemState.timeout(context, event);
	}

	// Incident Handlers
	
	@Request(value=AlarmSubsystemCapability.ListIncidentsRequest.NAME)
	public MessageBody listIncidents(SubsystemContext<AlarmSubsystemModel> context) {
		return AlarmUtil.listIncidents(context, incidentService);
	}
	
	@Request(value=AlarmIncidentCapability.VerifyRequest.NAME)
	public void verify(SubsystemContext<AlarmSubsystemModel> context, PlatformMessage message) {
		if(!message.getDestination().getRepresentation().equals(context.model().getCurrentIncident())) {
			throw new ErrorEventException(SecurityErrors.CODE_INCIDENT_INACTIVE, "Incident [" + message.getDestination().getRepresentation() + "] is not currently active");
		}
		Date verifiedTime = incidentService.verify(context, message.getDestination(), message.getActor());
		security.onVerified(context, message.getActor(), verifiedTime);
		smoke.onVerified(context, message.getActor(), verifiedTime);
	}
	
	@Request(value=AlarmIncidentCapability.CancelRequest.NAME)
	public MessageBody cancelIncident(SubsystemContext<AlarmSubsystemModel> context, PlatformMessage message) {
		if(!message.getDestination().getRepresentation().equals(context.model().getCurrentIncident())) {
			throw new ErrorEventException(Errors.invalidRequest("Incident is not currently active or cancellation has already been requested"));
		}

		AlarmIncidentModel curIncident = tryCancel(context, message, AlarmService.CancelAlertRequest.METHOD_APP);
		return AlarmUtil.buildIncidentCancelResponse(curIncident, context);
	}
	
	@OnMessage(types=AlarmIncidentCapability.CompletedEvent.NAME)
	public void onIncidentStateChanged(SubsystemContext<AlarmSubsystemModel> context, PlatformMessage message) {
		String currentIncident = context.model().getCurrentIncident();
		if(
				StringUtils.isEmpty(currentIncident) ||
				currentIncident.equals(message.getSource().getRepresentation())
		) {
			onCompleted(context);
		}
		else {
			context.logger().debug("Received incident completed for incident [{}] while current incident is [{}]", message.getSource(), currentIncident);
		}
	}
	
	// Security Event Handlers

	@Request(value=AlarmSubsystemCapability.ArmRequest.NAME)
	public MessageBody arm(SubsystemContext<AlarmSubsystemModel> context, PlatformMessage message) {
		AlarmUtil.assertActive(context);
		
		String mode = AlarmSubsystemCapability.ArmRequest.getMode(message.getValue());
		security.arm(context, message, mode);
		int delaySec = getExitDelay(context);
		return
				AlarmSubsystemCapability.ArmResponse
					.builder()
					.withDelaySec(delaySec)
					.build();
	}
	
	@Request(value=AlarmSubsystemCapability.ArmBypassedRequest.NAME)
	public MessageBody armBypassed(SubsystemContext<AlarmSubsystemModel> context, PlatformMessage message) {
		AlarmUtil.assertActive(context);
		
		String mode = AlarmSubsystemCapability.ArmRequest.getMode(message.getValue());
		security.armBypassed(context, message, mode);
		int delaySec = getExitDelay(context);
		return
				AlarmSubsystemCapability.ArmResponse
					.builder()
					.withDelaySec(delaySec)
					.build();
	}
	
	@OnMessage(types={ KeyPadCapability.ArmPressedEvent.NAME })
	public void armKeypad(SubsystemContext<AlarmSubsystemModel> context, PlatformMessage message) {
		if(!AlarmUtil.isActive(context)) {
			// ignore
			return;
		}
		
		MessageBody request = message.getValue();
		String mode = KeyPadCapability.ArmPressedEvent.getMode(request);
		KeyPad keypad = KeyPad.get(context, message.getSource());
		try {
			if(keypad.isBypassed()) {
				security.armBypassed(context, message, mode);
			}
			else {
				security.arm(context, message, mode);
			}
		}
		catch(Exception e) {
			if(e instanceof ErrorEventException && SecurityErrors.CODE_TRIGGERED_DEVICES.equals(((ErrorEventException) e).getCode())) {
				context.logger().debug("Unable to arm [{}] due to triggered/offline devices, setting keypad bypassed delay for [{}] ms", mode, AlarmUtil.keypadArmBypassedTimeoutMs);
				keypad.sendRetryBypassed(AlarmUtil.keypadArmBypassedTimeoutMs);
			}
			else {
				context.logger().warn("Unable to arm [{}] from keypad [{}]", message.getSource(), e);
				keypad.sendArmUnavailable();
			}
		}
	}
	
	@Request(value=AlarmSubsystemCapability.DisarmRequest.NAME)
	public void disarm(SubsystemContext<AlarmSubsystemModel> context, PlatformMessage message) {
		AlarmUtil.assertActive(context);
		doDisarm(context, message, AlarmService.CancelAlertRequest.METHOD_APP);
	}
	
	@OnMessage(types={ KeyPadCapability.DisarmPressedEvent.NAME })
	public void disarmKeypad(SubsystemContext<AlarmSubsystemModel> context, PlatformMessage message) {
		if(!AlarmUtil.isActive(context)) {
			// ignore
			return;
		}
		doDisarm(context, message, AlarmService.CancelAlertRequest.METHOD_KEYPAD);
	}
	
	protected void doDisarm(SubsystemContext<AlarmSubsystemModel> context, PlatformMessage message, String method) {
		security.disarm(context, message);
		if(!StringUtils.isEmpty(context.model().getCurrentIncident())) {
			tryCancel(context, message, method);
		}
	}
	
	@Request(value=AlarmSubsystemCapability.PanicRequest.NAME)
	@OnMessage(types={ KeyPadCapability.PanicPressedEvent.NAME })
	public void panic(SubsystemContext<AlarmSubsystemModel> context, PlatformMessage trigger) {
		AlarmUtil.assertActive(context);
		
		switch(String.valueOf(trigger.getSource().getGroup())) {
		case RuleCapability.NAMESPACE:
			panic.onTriggered(context, trigger.getSource(), TriggerEvent.RULE);
			break;
			
		case DeviceCapability.NAMESPACE:
			panic.onTriggered(context, trigger.getSource(), TriggerEvent.KEYPAD);
			break;
			
		default:
			if(trigger.getActor() == null) {
				throw new ErrorEventException("panic.unavailable", "Unable to determine source of panic");
			}
			else {
				// not quite right, but close enough?
				panic.onTriggered(context, trigger.getActor(), TriggerEvent.VERIFIED_ALARM);
			}
		}
	}
	
	/////////////////////////////////////////////////////////////
	// Backwards compatibility safety subsystem request handlers
	/////////////////////////////////////////////////////////////
	
	@Request(SafetySubsystemCapability.TriggerRequest.NAME) 
	public MessageBody triggerV1(SubsystemContext<AlarmSubsystemModel> context) {
		return Errors.invalidRequest("subsafety:Trigger is no longer supported");
	}
	
	@Request(SafetySubsystemCapability.ClearRequest.NAME) 
	public void clearV1(SubsystemContext<AlarmSubsystemModel> context, PlatformMessage message) {
		tryCancel(context, message, AlarmService.CancelAlertRequest.METHOD_APP);
	}
	
	/////////////////////////////////////////////////////////////
	// Backwards compatibility security subsystem request handlers
	/////////////////////////////////////////////////////////////
	
	@Request(SecuritySubsystemCapability.PanicRequest.NAME) 
	public void panicV1(SubsystemContext<AlarmSubsystemModel> context, PlatformMessage trigger) {
		panic(context, trigger);
	}
	
	@Request(SecuritySubsystemCapability.ArmRequest.NAME) 
	public void armV1(SubsystemContext<AlarmSubsystemModel> context, PlatformMessage message) {
		try{
			arm(context, message);
		}catch(ErrorEventException e) {
			if(SecurityErrors.CODE_TRIGGERED_DEVICES.equalsIgnoreCase(e.getCode())) {
				//make it backward compatible with 1.16 app
				throw new ErrorEventException(SecuritySubsystemUtil.CODE_TRIGGERED_DEVICES, "Some devices are preventing the alarm from being armed");
			}else{
				throw e;
			}
		}
	}
	
	@Request(SecuritySubsystemCapability.ArmBypassedRequest.NAME) 
	public void armBypassedV1(SubsystemContext<AlarmSubsystemModel> context, PlatformMessage message) {
		armBypassed(context, message);
	}
	
	@Request(SecuritySubsystemCapability.AcknowledgeRequest.NAME) 
	public void acknowledgeV1(SubsystemContext<AlarmSubsystemModel> context, MessageBody request) {
		context.logger().debug("Dropping deprecated request to acknowledge");
	}
	
	@Request(SecuritySubsystemCapability.DisarmRequest.NAME) 
	public void disarmV1(SubsystemContext<AlarmSubsystemModel> context, PlatformMessage message) {
		disarm(context, message);
	}
	
	/////////////////////////////////////////////////////////////
	// Special events on alarm
	/////////////////////////////////////////////////////////////
	
	@OnAdded(query = FanShutoffAdapter.QUERY_FAN_SHUTOFF_CAPABLE_DEVICE)
   public void onFanShutoffCapableDeviceAdded(ModelAddedEvent event, SubsystemContext<AlarmSubsystemModel> context) {
      context.model().setFanShutoffSupported(true);
   }

	@OnRemoved(query = FanShutoffAdapter.QUERY_FAN_SHUTOFF_CAPABLE_DEVICE)
   public void onFanShutoffCapableDeviceRemoved(ModelRemovedEvent event, SubsystemContext<AlarmSubsystemModel> context) {
		AlarmUtil.syncFanShutoffSupported(context);
   }

	@OnAdded(query = RecordOnSecurityAdapter.QUERY_CAMERA_DEVICE)
   public void onCameraDeviceAdded(ModelAddedEvent event, SubsystemContext<AlarmSubsystemModel> context) {
		AlarmUtil.syncRecordOnSecuritySupported(context);
   }

   @OnRemoved(query = RecordOnSecurityAdapter.QUERY_CAMERA_DEVICE)
   public void onCameraDeviceRemoved(ModelRemovedEvent event, SubsystemContext<AlarmSubsystemModel> context) {
		AlarmUtil.syncRecordOnSecuritySupported(context);
   }
   
   @OnValueChanged(attributes = { PlaceCapability.ATTR_SERVICELEVEL })
   public void onSubscriptionLevelChange(ModelChangedEvent event, SubsystemContext<AlarmSubsystemModel> context) {
      context.logger().info("Detected a subscription level change {}", event);
      AlarmUtil.syncRecordOnSecuritySupported(context);
   }
   
   @OnValueChanged(attributes={AlarmSubsystemCapability.ATTR_REQUESTEDALARMPROVIDER})
	public void onRequestedAlarmProviderChanged(ModelChangedEvent event, SubsystemContext<AlarmSubsystemModel> context) {
		syncAlarmProviderIfNecessary(context, true, null, CheckSecurityModeOption.DISARMED_OR_INACTIVE);
	}
   
   @OnValueChanged(attributes=HubConnectionCapability.ATTR_STATE)
   public void onHubConnectivityChanged(SubsystemContext<AlarmSubsystemModel> context, Model hub) {          	
		//hub comes online while the system is disarmed
		syncAlarmProviderIfNecessary(context, true, hub, CheckSecurityModeOption.DISARMED_OR_INACTIVE);
   }
      
	private void assertValidActivateState(SubsystemContext<AlarmSubsystemModel> context) {
		String safetyAlarmState = (String) context.models().getAttributeValue(Address.platformService(context.getPlaceId(), SafetySubsystemCapability.NAMESPACE), SafetySubsystemCapability.ATTR_ALARM);
		if(SafetySubsystemCapability.ALARM_ALERT.equals(safetyAlarmState)) {
			throw new ErrorEventException(Errors.invalidRequest("Can't upgrade during a safety alarm, please clear the safety alarm first"));
		}
		
		String securityAlarmState = (String) context.models().getAttributeValue(Address.platformService(context.getPlaceId(), SecuritySubsystemCapability.NAMESPACE), SecuritySubsystemCapability.ATTR_ALARMSTATE);
		if(
				SecuritySubsystemCapability.ALARMSTATE_ARMING.equals(securityAlarmState) ||
				SecuritySubsystemCapability.ALARMSTATE_SOAKING.equals(securityAlarmState) ||
				SecuritySubsystemCapability.ALARMSTATE_ALERT.equals(securityAlarmState)
		) {
			throw new ErrorEventException(Errors.invalidRequest("Can't upgrade while the security alarm is arming or alerting, please disarm first"));
		}
	}
	
	private void assertValidSuspendState(SubsystemContext<AlarmSubsystemModel> context) {
		if(context.model().isAlarmStateALERTING()) {
			throw new ErrorEventException(Errors.invalidRequest("Can't downgrade during an alarm, please cancel the incident first"));
		}
		String securityAlarmMode = AlarmModel.getAlertState(SecurityAlarm.NAME, context.model());
		if(!(
				AlarmCapability.ALERTSTATE_INACTIVE.equals(securityAlarmMode) ||
				AlarmCapability.ALERTSTATE_DISARMED.endsWith(securityAlarmMode) ||
				AlarmCapability.ALERTSTATE_CLEARING.equals(securityAlarmMode)
		)) {
			throw new ErrorEventException(Errors.invalidRequest("Can't downgrade while the security alarm is armed, please disarm first"));
		}
	}

	private int getExitDelay(SubsystemContext<AlarmSubsystemModel> context) {
		Date now = new Date();
		Date armTime = context.model().getSecurityArmTime(new Date(0));
		long delaySec = Math.round( (armTime.getTime() - now.getTime()) / 1000.0 );
		return delaySec < 1 ? 0 : (int) delaySec;
	}

	private boolean syncAlerts(SubsystemContext<AlarmSubsystemModel> context) {
		Map<String, String> oldAlerts = getAlerts(context);
		Map<String, String> newAlerts = getNewAlerts(context);
		if(oldAlerts.equals(newAlerts)) {
			return false;
		}
		else {
			setAlerts(context, newAlerts);
			onAlertsUpdated(context, oldAlerts, newAlerts);
			return true;
		}
	}
	
	private void onAlertsUpdated(SubsystemContext<AlarmSubsystemModel> context, Map<String, String> oldAlerts, Map<String, String> newAlerts) {
		Set<String> availableAlarms = new HashSet<>();
		Map<String, Set<String>> deltasByEvent = new HashMap<>();
		for(Map.Entry<String, String> newAlert: newAlerts.entrySet()) {
			String alarm = newAlert.getKey();
			String value = newAlert.getValue();
			String oldValue = oldAlerts.get(alarm);
			
			if(!AlarmCapability.ALERTSTATE_INACTIVE.equals(value)) {
				availableAlarms.add(alarm);
			}
			if(Objects.equal(value, oldValue)) {
				continue;
			}
			
			Set<String> deltas = deltasByEvent.get(value);
			if(deltas == null) {
				deltas = new HashSet<>();
				deltasByEvent.put(value, deltas);
			}
			deltas.add(alarm);
		}
		context.model().setAvailableAlerts(availableAlarms);
		
		if(deltasByEvent.isEmpty()) {
			// break out
			return;
		}
		
		AlarmSubsystemState state = AlarmSubsystemState.get(context);
		for(Map.Entry<String, Set<String>> e: deltasByEvent.entrySet()) {
			String alertState = e.getKey();
			Set<String> alarms = e.getValue();
			for(String alarm: alarms) {
				context.logger().debug("Updating alarm [{}] to state [{}]", alarm, alertState);
				Name nextState = updateState(context, state, alertState, alarm);
				state = transition(context, state, nextState);
			}
		}
		
		AlarmNotificationUtils.notifyPromonAlertAdded(context, oldAlerts, newAlerts);
	}
	
	private Name updateState(SubsystemContext<AlarmSubsystemModel> context, AlarmSubsystemState state, String alertState, String alert) {
		// update available alerts
		Set<String> availableAlerts = context.model().getAvailableAlerts();
		if(AlarmCapability.ALERTSTATE_INACTIVE.equals(alertState)) {
			if(availableAlerts != null && availableAlerts.contains(alert)) {
				availableAlerts = new HashSet<>(availableAlerts);
				availableAlerts.remove(alert);
			}
		}
		else {
			if(availableAlerts == null || !availableAlerts.contains(alert)) {
				availableAlerts = new HashSet<>(availableAlerts == null ? ImmutableSet.<String>of() : availableAlerts);
				availableAlerts.add(alert);
			}
		}
		context.model().setAvailableAlerts(availableAlerts);

		// update the active incident
		Address incident = null;
		if(AlarmCapability.ALERTSTATE_PREALERT.equals(alertState) && SecurityAlarm.NAME.equals(alert)) {
			// currently only security supports prealert
			Date entranceDelay = context.getVariable(SecurityAlarm.NAME + ":" + AlarmCapability.ALERTSTATE_PREALERT).as(Date.class);
			if(entranceDelay != null) {
				List<Map<String, Object>> triggers = AlarmModel.getTriggers(alert, context.model());
				incident = incidentService.addPreAlert(context, alert, entranceDelay, AlarmUtil.eventsToTriggers(triggers, 0));
				if(incident != null) {
					context.model().setCurrentIncident(incident.getRepresentation());
					TriggerInfo info = getTriggerInfo(context);
					incidentService.updateIncidentHistory(context, AlarmUtil.eventsToTriggers(triggers, info.getPreAlertIndex(alert)));
					info.setPreAlertIndex(alert, triggers.size());
					setTriggerInfo(context, info);
				}
			}
		}
		else if(AlarmCapability.ALERTSTATE_ALERT.equals(alertState)) {
			List<Map<String, Object>> triggers = AlarmModel.getTriggers(alert, context.model());
			incident = incidentService.addAlert(context, alert, AlarmUtil.eventsToTriggers(triggers, 0));
			if(incident != null) {
				context.model().setCurrentIncident(incident.getRepresentation());
			}
			TriggerInfo info = getTriggerInfo(context);
			int idx = Math.max(info.getPreAlertIndex(alert), info.getIndex(alert));
			incidentService.updateIncident(context, AlarmUtil.eventsToTriggers(triggers, idx));
			info.setIndex(alert, triggers.size());
			setTriggerInfo(context, info);
		}
		
		// update the state machine
		switch(alertState) {
		case AlarmCapability.ALERTSTATE_INACTIVE:
			return state.onAlertInactive(context, alert);

		case AlarmCapability.ALERTSTATE_READY:
			if(SecurityAlarm.NAME.equals(alert)) {
				return state.onArmed(context);
			}
			else {
				return state.onAlertReady(context, alert);
			}
			
		case AlarmCapability.ALERTSTATE_ALERT:
			Name retValue = state.onAlert(context, alert);
			new FanShutoffAdapter(context).fanShutoffIfNecessary(alert);
			SubsystemUtils.clearTimeout(context, AlarmUtil.TO_CANCEL);
			return retValue;
		
		case AlarmCapability.ALERTSTATE_CLEARING:
			return state.onAlertClearing(context, alert);
		
		case AlarmCapability.ALERTSTATE_ARMING:
			return state.onArming(context);
			
		case AlarmCapability.ALERTSTATE_DISARMED:
			return state.onDisarmed(context);
			
		case AlarmCapability.ALERTSTATE_PREALERT:
			SubsystemUtils.clearTimeout(context, AlarmUtil.TO_CANCEL);
			return state.onPreAlert(context);
			
		default:
			throw new UnsupportedOperationException("Unsupported alarm state: " + alertState);
		}
	}

	private Map<String, String> getAlerts(SubsystemContext<AlarmSubsystemModel> context) {
		return context.getVariable("alerts").as(new TypeMarker<Map<String, String>>() {}, ImmutableMap.<String, String>of());
	}
	
	// package scope for testing
	void setAlerts(SubsystemContext<AlarmSubsystemModel> context, Map<String, String> alerts) {
		context.setVariable("alerts", alerts);
	}
	
	private Map<String, String> getNewAlerts(SubsystemContext<AlarmSubsystemModel> context) {
		Map<String, String> alarms = new HashMap<>(2 * AlarmSubsystem.ALARM_TYPES.size());
		for(Model model: context.models().getModels(AlarmSubsystem.hasAlarm())) {
			for(String alarmType: AlarmSubsystem.ALARM_TYPES) {
				String alarmState = AlarmModel.getAlertState(alarmType, model);
				if(StringUtils.isEmpty(alarmState)) {
					continue;
				}
				
				alarms.put(alarmType, alarmState);
			}
		}
		return alarms;
	}

	private AlarmIncidentModel tryCancel(final SubsystemContext<AlarmSubsystemModel> context, PlatformMessage message, String method) {
		security.cancel(context, message);
		panic.cancel(context, message);
		co.cancel(context, message);
		smoke.cancel(context, message);
		water.cancel(context, message);
		
		context.model().setCurrentIncident("");
		context.setVariable(AlarmUtil.VAR_CANCELLEDBY, message.getActor());
		context.setVariable(AlarmUtil.VAR_CANCELMETHOD, method);
		return tryCancel(context);
	}
	
	private AlarmIncidentModel tryCancel(final SubsystemContext<AlarmSubsystemModel> context) {
		SubsystemUtils.setTimeout(35000, context, AlarmUtil.TO_CANCEL);
		Address cancelledBy = context.getVariable(AlarmUtil.VAR_CANCELLEDBY).as(Address.class);
		String method = context.getVariable(AlarmUtil.VAR_CANCELMETHOD).as(String.class);
		AlarmIncidentModel incident = incidentService.cancel(context, cancelledBy, method);
		if(incident == null || incident.isAlertStateCOMPLETE()) {
			onCompleted(context);
		}
		return incident;
	}
	
	private void onCompleted(SubsystemContext<AlarmSubsystemModel> context) {
		context.setVariable(AlarmUtil.VAR_CANCELLEDBY, null);
		context.setVariable(AlarmUtil.VAR_CANCELMETHOD, null);
		clearTriggerInfo(context);
		SubsystemUtils.clearTimeout(context, AlarmUtil.TO_CANCEL);
		context.model().setCurrentIncident("");
		
		security.onCancelled(context);
		panic.onCancelled(context);
		co.onCancelled(context);
		smoke.onCancelled(context);
		water.onCancelled(context);
	}

	private TriggerInfo getTriggerInfo(SubsystemContext<AlarmSubsystemModel> context) {
		TriggerInfo info = context.getVariable(VAR_TRIGGERINFO).as(TriggerInfo.class);
		if(info == null || !StringUtils.equals(context.model().getCurrentIncident(), info.incidentAddress)) {
			info = new TriggerInfo();
			info.incidentAddress = context.model().getCurrentIncident();
		}
		return info;
	}
	
	private void clearTriggerInfo(SubsystemContext<AlarmSubsystemModel> context) {
		setTriggerInfo(context, null);
	}
	
	private void setTriggerInfo(SubsystemContext<AlarmSubsystemModel> context, @Nullable TriggerInfo info) {
		context.setVariable(VAR_TRIGGERINFO, info);
	}
	
	private void syncAlarmProviderIfNecessary(SubsystemContext<AlarmSubsystemModel> context, boolean checkHubOnline, Model hub, CheckSecurityModeOption checkSecurityMode) {
		AlarmUtil.syncAlarmProviderIfNecessary(context, checkHubOnline, hub, checkSecurityMode);
	}

	private static class TriggerInfo {
		private String incidentAddress;
		private int smoke = 0;
		private int co = 0;
		private int security = 0;
		private int panic = 0;
		private int water = 0;
		private int securityPreAlert = 0;
		
		public boolean isSignalled(String alarmType) {
			return getIndex(alarmType) > 0;
		}

		public boolean isPreAlert(String alarmType) {
			return getPreAlertIndex(alarmType) > 0;
		}

		public int getPreAlertIndex(String alarmType) {
			switch(alarmType) {
				case AlarmSubsystemCapability.ACTIVEALERTS_SECURITY: return securityPreAlert;
				default: return 0;
			}
		}

		public int getIndex(String alarmType) {
			switch(alarmType) {
			case AlarmSubsystemCapability.ACTIVEALERTS_CO:       return co;
			case AlarmSubsystemCapability.ACTIVEALERTS_PANIC:    return panic;
			case AlarmSubsystemCapability.ACTIVEALERTS_SECURITY: return security;
			case AlarmSubsystemCapability.ACTIVEALERTS_SMOKE:    return smoke;
			case AlarmSubsystemCapability.ACTIVEALERTS_WATER:    return water;
			default:
				throw new IllegalArgumentException("Unrecognized alarm type: " + alarmType);
			}
		}

		public void setPreAlertIndex(String alarmType, int index) {
			switch(alarmType) {
			case AlarmSubsystemCapability.ACTIVEALERTS_SECURITY:
				securityPreAlert = index;
				break;
			default:
				break;
			}
		}
		
		public void setIndex(String alarmType, int index) {
			switch(alarmType) {
			case AlarmSubsystemCapability.ACTIVEALERTS_CO:
				co = index;
				break;
			case AlarmSubsystemCapability.ACTIVEALERTS_PANIC:
				panic = index;
				break;
			case AlarmSubsystemCapability.ACTIVEALERTS_SECURITY:
				security = index;
				break;
			case AlarmSubsystemCapability.ACTIVEALERTS_SMOKE:
				smoke = index;
				break;
			case AlarmSubsystemCapability.ACTIVEALERTS_WATER:
				water = index;
				break;
			default:
				throw new IllegalArgumentException("Unrecognized alarm type: " + alarmType);
			}
		}
		
	}
}

