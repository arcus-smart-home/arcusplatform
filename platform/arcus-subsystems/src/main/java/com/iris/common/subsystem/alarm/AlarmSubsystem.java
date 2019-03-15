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

import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.capability.definition.DefinitionRegistry;
import com.iris.common.alarm.AlertType;
import com.iris.common.subsystem.Subsystem;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.alarm.AlarmUtil.CheckSecurityModeOption;
import com.iris.common.subsystem.alarm.incident.AlarmIncidentService;
import com.iris.common.subsystem.event.SubsystemStartedEvent;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.HubAlarmCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.event.AddressableEvent;
import com.iris.messages.event.MessageReceivedEvent;
import com.iris.messages.event.ModelAddedEvent;
import com.iris.messages.event.ModelEvent;
import com.iris.messages.event.ModelRemovedEvent;
import com.iris.messages.event.ModelReportEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.hub.HubAdvancedModel;
import com.iris.messages.model.subs.AlarmSubsystemModel;
import com.iris.messages.model.subs.SubsystemModel;
import com.iris.model.Version;
import com.iris.model.query.expression.ExpressionCompiler;

@Singleton
public class AlarmSubsystem implements Subsystem<AlarmSubsystemModel> {

	public static final String QUERY_ALARM = "has alarm";
	public static final String QUERY_SECURITY_SUBSYSTEM = "base:caps contains 'subsecurity'";
	public static final List<String> ALARM_TYPES = ImmutableList.of(
			AlarmSubsystemCapability.ACTIVEALERTS_SMOKE,
			AlarmSubsystemCapability.ACTIVEALERTS_CO,
			AlarmSubsystemCapability.ACTIVEALERTS_SECURITY,
			AlarmSubsystemCapability.ACTIVEALERTS_PANIC,
			AlarmSubsystemCapability.ACTIVEALERTS_WATER
			//AlarmSubsystemCapability.ACTIVEALERTS_WEATHER,
			//AlarmSubsystemCapability.ACTIVEALERTS_CARE
	);
	

	public static final Predicate<Model> HAS_ALARM = ExpressionCompiler.compile(QUERY_ALARM);
	

	public static final Boolean FAN_SHUTOFF_SUPPORTED_DEFAULT = Boolean.FALSE;
	public static final Boolean FAN_SHUTOFF_ON_CO_DEFAULT = Boolean.TRUE;
	public static final Boolean FAN_SHUTOFF_ON_SMOKE_DEFAULT = Boolean.FALSE;
	
	@Inject(optional = true) @Named("subalarm.hubprovider.minversion")
	private final String minHubVersionForHubProviderStr = "2.1.0.060";	
	private Version minHubVersionForHubProvider;
	private static final String HUB_MIN_VERSION_ERROR_MSG = "Can not change to HUB provider because the hub OS version does not meet the minimum.";
	
	
	
	public static Predicate<Model> hasAlarm() {
		return HAS_ALARM;
	}
	
	public static Comparator<String> alarmPriorityComparator() {
		return AlertType.ALERT_PRIORITY_COMPARATOR;
	}

	private static final String VAR_TARGETPROVIDER = "targetProvider";

	private final PlatformAlarmSubsystem platformAlarmSubsystem;
	private final HubAlarmSubsystem hubAlarmSubsystem;
	
	// note the binders are extracted to here because its hard to add / remove these listeners
	// at runtime and they behavior is the same across V1 & V2
	// private final KeypadBinder keypadBinder = new KeypadBinder();

	@Inject
	public AlarmSubsystem(AlarmIncidentService alarmIncidentService) {
	   this.platformAlarmSubsystem = new PlatformAlarmSubsystem(alarmIncidentService);
	   this.hubAlarmSubsystem = new HubAlarmSubsystem(alarmIncidentService);
	}
	
	public AlarmSubsystem(AlarmIncidentService alarmIncidentService, boolean activeOnAdd) {
	   this.platformAlarmSubsystem = new PlatformAlarmSubsystem(alarmIncidentService, activeOnAdd);
	   this.hubAlarmSubsystem = new HubAlarmSubsystem(alarmIncidentService);
	}

	@Override
	public String getName() {
		return AlarmSubsystemCapability.NAME;
	}

	@Override
	public String getNamespace() {
		return AlarmSubsystemCapability.NAMESPACE;
	}

	@Override
	public Class<AlarmSubsystemModel> getType() {
		return AlarmSubsystemModel.class;
	}

	@Override
	public Version getVersion() {
		return Version.fromRepresentation("2");
	}

   @Inject
   public void setDefinitionRegistry(DefinitionRegistry registry) {
      this.platformAlarmSubsystem.setDefinitionRegistry(registry);
      this.hubAlarmSubsystem.setDefinitionRegistry(registry);
   }

	@Override
	public void onEvent(AddressableEvent event, SubsystemContext<AlarmSubsystemModel> context) {
		if(event instanceof ModelEvent && SubsystemModel.isStateSUSPENDED(context.model())) {
			// short-circuit most events when suspended
			return;
		}

	   if (event instanceof MessageReceivedEvent &&
	       AlarmSubsystemCapability.SetProviderRequest.NAME.equals(((MessageReceivedEvent)event).getMessage().getMessageType())) {
	      PlatformMessage msg = ((MessageReceivedEvent)event).getMessage();
	      try {
	      	onSetProvider(context, ((MessageReceivedEvent)event).getMessage());
		   } catch (ErrorEventException e) {
		   	context.model().setLastAlarmProviderError(e.getDescription());
		      context.sendResponse(msg, e.toErrorEvent());
		   }catch (Exception ex) {
		   	ErrorEvent e = Errors.fromException(ex);
		   	context.model().setLastAlarmProviderError(e.getMessage());
		      context.sendResponse(msg, e);
		   }
		} else {
			if (event instanceof SubsystemStartedEvent) {
		      String alarmSubsystemProvider = context.model().getAlarmProvider();
		      if (alarmSubsystemProvider == null) {
		         context.model().setAlarmProvider(AlarmSubsystemCapability.ALARMPROVIDER_PLATFORM);
		      }
			}

			if(event instanceof ModelReportEvent) {
				onReport(context, (ModelReportEvent) event);
			}

			if(event instanceof ModelRemovedEvent) {
				onModelRemoved(context, (ModelRemovedEvent) event);
			}
			
			if(event instanceof ModelAddedEvent) {
				onModelAdded(context, (ModelAddedEvent) event);
			}

			delegate(context).onEvent(event, context);
		}
	}

	
	private void onReport(SubsystemContext<AlarmSubsystemModel> context, ModelReportEvent event) {
		ModelReportEvent.ValueChange stateChange = event.getChanges().get(HubAlarmCapability.ATTR_STATE);

		// handle hub factory reset and repaired to the account
		if(stateChange != null && AlarmSubsystemCapability.ALARMPROVIDER_HUB.equals(context.model().getAlarmProvider()) &&
			HubAlarmCapability.STATE_SUSPENDED.equals(stateChange.getNewValue())) {

			setProviderPlatform(context);
		}

		String targetProvider = context.getVariable(VAR_TARGETPROVIDER).as(String.class);
		if(targetProvider == null) {
			return;
		}

		if(stateChange == null || !isSteadyState(context)) {
			return;
		}

		switch(targetProvider) {
			case AlarmSubsystemCapability.ALARMPROVIDER_HUB:
				if(HubAlarmCapability.STATE_ACTIVE.equals(stateChange.getNewValue())) {
					platformAlarmSubsystem.onStopped(context);
					hubAlarmSubsystem.onStarted(context);
					context.setVariable(VAR_TARGETPROVIDER, null);
					context.model().setAlarmProvider(AlarmSubsystemCapability.ALARMPROVIDER_HUB);
				}
				break;
			case AlarmSubsystemCapability.ALARMPROVIDER_PLATFORM:
				if(HubAlarmCapability.STATE_SUSPENDED.equals(stateChange.getNewValue())) {
					setProviderPlatform(context);
				}
				break;
			default:
				context.logger().warn("unknown alarm subsystem provider: {}", targetProvider);
				break;
		}
	}

	private void setProviderPlatform(SubsystemContext<AlarmSubsystemModel> context) {
		hubAlarmSubsystem.onStopped(context);
		platformAlarmSubsystem.onStarted(context);
		context.setVariable(VAR_TARGETPROVIDER, null);
		context.model().setAlarmProvider(AlarmSubsystemCapability.ALARMPROVIDER_PLATFORM);
	}

	private void onModelRemoved(SubsystemContext<AlarmSubsystemModel> context, ModelRemovedEvent event) {
		if(!context.model().isAlarmProviderHUB()) {
			return;
		}
		if(event.getModel().supports(HubCapability.NAMESPACE)) {
			hubAlarmSubsystem.onStopped(context);
			platformAlarmSubsystem.onStarted(context);
			context.model().setAlarmProvider(AlarmSubsystemCapability.ALARMPROVIDER_PLATFORM);
		}
	}
	
	private void onModelAdded(SubsystemContext<AlarmSubsystemModel> context, ModelAddedEvent event) {
		if(event.getAddress().isHubAddress()) {
			AlarmUtil.syncAlarmProviderIfNecessary(context, false, null, CheckSecurityModeOption.IGNORE);
		}

	}


	private void onSetProvider(SubsystemContext<AlarmSubsystemModel> context, PlatformMessage request) {
		if(context.model().isStateSUSPENDED() || !isSteadyState(context)) {
			throw new ErrorEventException(AlarmErrors.ILLEGAL_SETPROV_STATE);
		}
		
		MessageBody body = request.getValue();
		String provider = AlarmSubsystemCapability.SetProviderRequest.getProvider(body);
		String currentProvider = context.model().getAlarmProvider();
		if(currentProvider == null) {
			currentProvider = AlarmSubsystemCapability.ALARMPROVIDER_PLATFORM;
		}

		if(StringUtils.equals(provider, currentProvider)) {
			context.sendResponse(request, AlarmSubsystemCapability.SetProviderResponse.instance());
			return;
		}

		switch(provider) {
			case AlarmSubsystemCapability.SetProviderRequest.PROVIDER_HUB:
				if(!AlarmSubsystemCapability.REQUESTEDALARMPROVIDER_HUB.equals(context.model().getRequestedAlarmProvider())) {
					context.model().setRequestedAlarmProvider(AlarmSubsystemCapability.REQUESTEDALARMPROVIDER_HUB);
				}
				context.model().setLastAlarmProviderAttempt(new Date());
				useHubProvider(request, context);
				break;
			case AlarmSubsystemCapability.SetProviderRequest.PROVIDER_PLATFORM:
				if(!AlarmSubsystemCapability.REQUESTEDALARMPROVIDER_PLATFORM.equals(context.model().getRequestedAlarmProvider())) {
					context.model().setRequestedAlarmProvider(AlarmSubsystemCapability.REQUESTEDALARMPROVIDER_PLATFORM);
				}
				context.model().setLastAlarmProviderAttempt(new Date());
				usePlatformProvider(request, context);
				break;
			default:
				throw new ErrorEventException(Errors.fromCode(Errors.CODE_INVALID_REQUEST, "Unknown alarm subsystem provider: " + provider));
		}
	}

	private boolean isSteadyState(SubsystemContext<AlarmSubsystemModel> context) {
		if(context.model().isAlarmStateINACTIVE()) {
			return true;
		}
		return context.model().isAlarmStateREADY() && (context.model().isSecurityModeDISARMED() || context.model().isSecurityModeINACTIVE());
	}

	private void useHubProvider(final PlatformMessage request, final SubsystemContext<AlarmSubsystemModel> context) {
	   //Make sure hub exists
		Model hub = AlarmUtil.getHubModel(context);	
		//And meet min FW version
	   assertMinHubVersion(hub, context);
	   hubAlarmSubsystem.assertCanSwitchTo(context);
	   context.setVariable(VAR_TARGETPROVIDER, AlarmSubsystemCapability.ALARMPROVIDER_HUB);
		platformAlarmSubsystem.onStopped(context);
		AlarmUtil.sendHubRequest(context, request, HubAlarmCapability.ActivateRequest.instance(),
			new Function<PlatformMessage, MessageBody>() {
				@Override
				public MessageBody apply(PlatformMessage input) {
					hubAlarmSubsystem.onStarted(context);
					context.setVariable(VAR_TARGETPROVIDER, null);
					context.model().setAlarmProvider(AlarmSubsystemCapability.ALARMPROVIDER_HUB);
					context.model().setLastAlarmProviderError("");
					return AlarmSubsystemCapability.SetProviderResponse.instance();
				}
			},
			new Predicate<PlatformMessage>() {
				@Override
				public boolean apply(PlatformMessage input) {
					context.logger().warn("failed to activate alarms on the hub, restarting platform subsystem.");
					platformAlarmSubsystem.onStarted(context);					
					context.model().setLastAlarmProviderError(AlarmUtil.extractErrorMessage(input.getValue()));
					return false;
				}
			}
		);
	}

	private void usePlatformProvider(PlatformMessage request, final SubsystemContext<AlarmSubsystemModel> context) {
		context.setVariable(VAR_TARGETPROVIDER, AlarmSubsystemCapability.ALARMPROVIDER_PLATFORM);
		hubAlarmSubsystem.onStopped(context);
		AlarmUtil.sendHubRequest(context, request, HubAlarmCapability.SuspendRequest.instance(),
			new Function<PlatformMessage, MessageBody>() {
				@Override
				public MessageBody apply(PlatformMessage input) {
					platformAlarmSubsystem.onStarted(context);
					context.setVariable(VAR_TARGETPROVIDER, null);
					context.model().setAlarmProvider(AlarmSubsystemCapability.ALARMPROVIDER_PLATFORM);
					context.model().setLastAlarmProviderError("");
					return AlarmSubsystemCapability.SetProviderResponse.instance();
				}
			},
			new Predicate<PlatformMessage>() {
				@Override
				public boolean apply(PlatformMessage input) {
					context.logger().warn("failed to suspend alarms on the hub, restarting hub subsystem");
					hubAlarmSubsystem.onStarted(context);
					context.model().setLastAlarmProviderError(AlarmUtil.extractErrorMessage(input.getValue()));
					return false;
				}
			});
	}

	private Subsystem<AlarmSubsystemModel> delegate(SubsystemContext<AlarmSubsystemModel> context) {
		String alarmSubsystemProvider = context.model().getAlarmProvider();
		boolean isHub = AlarmSubsystemCapability.ALARMPROVIDER_HUB.equals(alarmSubsystemProvider);
		return isHub ? hubAlarmSubsystem : platformAlarmSubsystem;
	}
	
	private Version getMinHubVersionForHubProvider() {
		if(minHubVersionForHubProvider == null) {
			minHubVersionForHubProvider = Version.fromRepresentation(minHubVersionForHubProviderStr);
		}
		return minHubVersionForHubProvider;
	}
	
	/**
	 * Return true if the hub OS version meets the min hub firmware version criteria
	 * @param hub
	 * @param context 
	 * @return
	 */
	private void assertMinHubVersion(Model hub, SubsystemContext<AlarmSubsystemModel> context) {
		Version hubVersion;
		try{			
   		hubVersion = Version.fromRepresentation(HubAdvancedModel.getOsver(hub));   	   
		}catch(Exception e) {
			context.logger().warn("the hub firmware version is invalid for hub with id [{}]", hub.getId());
			throw new AlarmSubsystemCapability.HubBelowMinFwException(HUB_MIN_VERSION_ERROR_MSG, e); 
		}
		if(hubVersion != null && hubVersion.compareTo(getMinHubVersionForHubProvider()) > 0) {
	   	throw new AlarmSubsystemCapability.HubBelowMinFwException(HUB_MIN_VERSION_ERROR_MSG); 
	   }
	}
}

