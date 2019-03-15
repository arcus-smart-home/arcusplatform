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

import static com.iris.common.subsystem.alarm.CancelAlarmMessageTable.getCancelMessage;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.common.subsystem.alarm.CancelAlarmMessageTable.CancelMessage;
import com.iris.common.subsystem.alarm.co.CarbonMonoxideAlarm;
import com.iris.common.subsystem.alarm.incident.AlarmIncidentService;
import com.iris.common.subsystem.alarm.panic.PanicAlarm;
import com.iris.common.subsystem.alarm.security.SecurityAlarm;
import com.iris.common.subsystem.alarm.security.SecurityErrors;
import com.iris.common.subsystem.alarm.smoke.SmokeAlarm;
import com.iris.common.subsystem.alarm.subs.AlarmSubsystemState;
import com.iris.common.subsystem.alarm.water.WaterAlarm;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.PlatformServiceAddress;
import com.iris.messages.capability.AlarmIncidentCapability;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.AlarmSubsystemCapability.SetProviderRequest;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.SafetySubsystemCapability;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Model;
import com.iris.messages.model.hub.HubConnectionModel;
import com.iris.messages.model.hub.HubModel;
import com.iris.messages.model.serv.AlarmIncidentModel;
import com.iris.messages.model.serv.AlarmModel;
import com.iris.messages.model.serv.HubAlarmModel;
import com.iris.messages.model.subs.AlarmSubsystemModel;
import com.iris.messages.model.subs.SafetySubsystemModel;
import com.iris.messages.model.subs.SecurityAlarmModeModel;
import com.iris.messages.model.subs.SecuritySubsystemModel;
import com.iris.messages.model.subs.SubsystemModel;
import com.iris.messages.type.IncidentTrigger;

public class AlarmUtil {
	
	public enum CheckSecurityModeOption{
		DISARMED_ONLY,
		DISARMED_OR_INACTIVE,
		IGNORE
	}

   static final int keypadArmBypassedTimeoutMs = 4000;
   // fail faster for requests issued directly by a client, give rules & scenes more time to process
   static final int ClientRequestTimeoutSec = 15;
   static final int ServiceRequestTimeoutSec = 60;
   static final String TO_CANCEL = "cancel";
   static final String VAR_CANCELLEDBY = "cancelledBy";
   static final String VAR_CANCELACTOR = "cancelActor";
   static final String VAR_CANCELMETHOD = "cancelMethod";
   
   private static final String VAR_SYNC_ALARM_PROVIDER_IN_PROGRESS = "syncAlarmProvider";

   public static Model getHubModel(SubsystemContext<AlarmSubsystemModel> context) {
      return getHubModel(context, true);
   }

   public static Model getHubModel(SubsystemContext<AlarmSubsystemModel> context, boolean strict) {
      Iterable<Model> hubs = context.models().getModelsByType(HubCapability.NAMESPACE);
      Iterator<Model> ihubs = (hubs != null) ? hubs.iterator() : null;
      Model mhub = (ihubs != null && ihubs.hasNext()) ? ihubs.next() : null;
      if (mhub == null && strict) {
         throw new ErrorEventException(AlarmErrors.NOHUB);
      }
      return mhub;
   }

   public static void copyCallTree(SubsystemContext<AlarmSubsystemModel> context, CallTree callTree) {
      List<Map<String, Object>> oldCallTree = SecuritySubsystemModel.getCallTree(getSubsystem(context, SecuritySubsystemCapability.NAMESPACE), ImmutableList.<Map<String, Object>>of());
      List<Map<String, Object>> newCallTree = callTree.normalize(context, oldCallTree);
      context.logger().debug("Upgrading call tree from {} to {}", oldCallTree, newCallTree);
      callTree.setCallTree(context, newCallTree);
   }

   public static Model getSubsystem(SubsystemContext<?> context, String subsystemNamespace) {
      return context.models().getModelByAddress(Address.platformService(context.getPlaceId(), subsystemNamespace));
   }

   public static boolean isActive(SubsystemContext<AlarmSubsystemModel> context) {
      return context.model().isStateACTIVE();
   }

   public static void assertActive(SubsystemContext<AlarmSubsystemModel> context) {
      if(!AlarmUtil.isActive(context)) {
         throw new ErrorEventException(Errors.invalidRequest("The alarm subsystem is currently suspended"));
      }
   }

   public static void copySilent(SubsystemContext<AlarmSubsystemModel> context) {
      boolean safetySilent = SafetySubsystemModel.getSilentAlarm(getSubsystem(context, SafetySubsystemCapability.NAMESPACE), false);
      AlarmModel.setSilent(CarbonMonoxideAlarm.NAME, context.model(), safetySilent);
      AlarmModel.setSilent(SmokeAlarm.NAME, context.model(), safetySilent);
      AlarmModel.setSilent(WaterAlarm.NAME, context.model(), safetySilent);

      Model securitySubsystem = getSubsystem(context, SecuritySubsystemCapability.NAMESPACE);
      boolean securitySilent =
         SecurityAlarmModeModel.getSilent(SecuritySubsystemCapability.ALARMMODE_ON, securitySubsystem, false) &&
            SecurityAlarmModeModel.getSilent(SecuritySubsystemCapability.ALARMMODE_PARTIAL, securitySubsystem, false);
      AlarmModel.setSilent(PanicAlarm.NAME, context.model(), securitySilent);
      AlarmModel.setSilent(SecurityAlarm.NAME, context.model(), securitySilent);
   }

   public static List<IncidentTrigger> eventsToTriggers(List<Map<String, Object>> events, int offset) {
      if(events == null || events.size() <= offset) {
         return ImmutableList.of();
      }

      List<IncidentTrigger> triggers = new ArrayList<>(events.size() - offset);
      for(Map<String,Object> event: events.subList(offset, events.size())) {
         triggers.add(new IncidentTrigger(event));
      }
      return triggers;
   }

   public static MessageBody listIncidents(SubsystemContext<AlarmSubsystemModel> context, AlarmIncidentService incidentService) {
      List<AlarmIncidentModel> incidents = incidentService.listIncidents(context);
      List<Map<String, Object>> result = new ArrayList<>(incidents.size());
      for(AlarmIncidentModel incident: incidents) {
         result.add(incident.toMap());
      }
      return
         AlarmSubsystemCapability.ListIncidentsResponse
            .builder()
            .withIncidents(result)
            .build();
   }

   public static String getMonitoringStateForCancel(String monitoringState) {
      if(StringUtils.isEmpty(monitoringState)) {
         monitoringState = AlarmIncidentCapability.MONITORINGSTATE_NONE;
      }
      switch(monitoringState) {
         case AlarmIncidentCapability.MONITORINGSTATE_DISPATCHED: return AlarmIncidentCapability.CancelResponse.MONITORINGSTATE_DISPATCHED;
         case AlarmIncidentCapability.MONITORINGSTATE_DISPATCHING: return AlarmIncidentCapability.CancelResponse.MONITORINGSTATE_DISPATCHING;
         default:
            return AlarmIncidentCapability.CancelResponse.MONITORINGSTATE_CANCELLED;
      }

   }

   public static MessageBody buildIncidentCancelResponse(@Nullable AlarmIncidentModel incidentModel, @NonNull SubsystemContext<AlarmSubsystemModel> context) {
      Model subsystemModel = context.model();
      AlarmIncidentCapability.CancelResponse.Builder builder = AlarmIncidentCapability.CancelResponse.builder();
      if(incidentModel == null) {
         // got out of sync, just clear it out and keep on trucking
         builder
            .withAlarmState(AlarmIncidentCapability.CancelResponse.ALARMSTATE_CANCELLED)
            .withMonitoringState(AlarmIncidentCapability.CancelResponse.MONITORINGSTATE_CANCELLED)
            .withCleared(true);
      }
      else {
         boolean isCleared = true;
         //check to see the SMOKE or CO alarms is not still CLEARING, only looking at the main alert
         boolean isSmokeCOClearing = AlarmModel.isAlertStateCLEARING(SmokeAlarm.NAME, subsystemModel) || AlarmModel.isAlertStateCLEARING(CarbonMonoxideAlarm.NAME, subsystemModel);
         String monitoringState = incidentModel.getMonitoringState();
         if((AlarmIncidentCapability.MONITORINGSTATE_CANCELLED.equals(monitoringState) || AlarmIncidentCapability.MONITORINGSTATE_NONE.equals(monitoringState)) && !isSmokeCOClearing) {
            isCleared = true;
         }
         else{
            isCleared = false;
         }
         builder
            .withAlarmState(AlarmIncidentCapability.ALERTSTATE_COMPLETE.equals(incidentModel.getAlertState())? AlarmIncidentCapability.CancelResponse.ALARMSTATE_CANCELLED: AlarmIncidentCapability.CancelResponse.ALARMSTATE_CLEARING)
            .withMonitoringState(AlarmUtil.getMonitoringStateForCancel(incidentModel.getMonitoringState()))
            .withCleared(isCleared);

         Model hubModel = getHubModel(context, false);
         boolean hubOnlineOrMissing = hubModel == null || !HubModel.isStateDOWN(hubModel);
         CancelMessage cancelMessage = getCancelMessage(hubOnlineOrMissing, incidentModel.getAlert(), monitoringState, isSmokeCOClearing);
         if(cancelMessage != null) {
            builder
               .withWarningTitle(cancelMessage.getTitle())
               .withWarningMessage(cancelMessage.getText());
         }
      }

      return builder.build();
   }
   
   public static boolean isHubAlerting(@Nullable Model hub) {
      return hub != null && (HubAlarmModel.isAlarmStatePREALERT(hub) || HubAlarmModel.isAlarmStateALERTING(hub)); 
   }
   
   public static boolean isPendingClear(@Nullable Model hub) {
      return 
            hub != null &&
            !isHubAlerting(hub) &&
            isHubIncidentActive(hub);
   }
   
   public static boolean isHubIncidentActive(@Nullable Model hub) {
      return hub != null && !HubAlarmModel.getCurrentIncident(hub, "").isEmpty();
   }

   public static void sendHubRequest(
      @NonNull final SubsystemContext<AlarmSubsystemModel> context,
      @NonNull final PlatformMessage clientRequest,
      @NonNull final MessageBody request,
      @NonNull final Function<PlatformMessage, MessageBody> successHandler
   ) {
      sendHubRequest(context, clientRequest, request, successHandler, Predicates.<PlatformMessage>alwaysFalse());
   }

   public static void sendHubRequest(
      @NonNull final SubsystemContext<AlarmSubsystemModel> context,
      @NonNull final PlatformMessage clientRequest,
      @NonNull final MessageBody request,
      @NonNull final Function<PlatformMessage, MessageBody> successHandler,
      @NonNull final Predicate<PlatformMessage> errorHandler
   ) {
      Model hub = AlarmUtil.getHubModel(context);
      if(hub == null) {
         context.logger().error("invalid state, hub alarm subsystem active but no hub model present.");
         sendResponse(context, clientRequest, SecurityErrors.NO_HUB);
         return;
      }
      int timeoutSec = MessageConstants.CLIENT.equals(clientRequest.getSource().getNamespace()) ? ClientRequestTimeoutSec : ServiceRequestTimeoutSec;
      context.sendAndExpectResponse(hub.getAddress(), request, timeoutSec, TimeUnit.SECONDS, new SubsystemContext.ResponseAction<AlarmSubsystemModel>() {
         @Override
         public void onResponse(SubsystemContext<AlarmSubsystemModel> context, PlatformMessage response) {
            if(response.isError()) {
               if(!errorHandler.apply(response)) {
                  sendResponse(context, clientRequest, response.getValue());
               }
               return;
            }
            MessageBody body = successHandler.apply(response);
            if(body != null) {
               sendResponse(context, clientRequest, body);
            }
         }

         @Override
         public void onError(SubsystemContext<AlarmSubsystemModel> context, Throwable cause) {
            context.logger().warn("error waiting for hub response to {}", request.getMessageType(), cause);
            sendResponse(context, clientRequest, Errors.fromException(cause));
         }

         @Override
         public void onTimeout(SubsystemContext<AlarmSubsystemModel> context) {
            context.logger().warn("timed out waiting for hub response to {}", request.getMessageType());
            sendResponse(context, clientRequest, Errors.hubOffline());
         }
      });
   }

   public static void sendResponse(final SubsystemContext<AlarmSubsystemModel> context, final PlatformMessage msg, final MessageBody body) {
      if(!Address.broadcastAddress().equals(msg.getDestination())) {
         context.sendResponse(msg, body);
      } else {
         context.logger().trace("ignoring send response to {}", msg);
      }
   }

   public static void syncFanShutoff(@NonNull SubsystemContext<AlarmSubsystemModel> context) {
      SubsystemUtils.setIfNull(context.model(), AlarmSubsystemCapability.ATTR_FANSHUTOFFSUPPORTED, AlarmSubsystem.FAN_SHUTOFF_SUPPORTED_DEFAULT);
      SubsystemUtils.setIfNull(context.model(), AlarmSubsystemCapability.ATTR_FANSHUTOFFONCO, AlarmSubsystem.FAN_SHUTOFF_ON_CO_DEFAULT);
      SubsystemUtils.setIfNull(context.model(), AlarmSubsystemCapability.ATTR_FANSHUTOFFONSMOKE, AlarmSubsystem.FAN_SHUTOFF_ON_SMOKE_DEFAULT);
      syncFanShutoffSupported(context);
   }

   public static void syncFanShutoffSupported(SubsystemContext<AlarmSubsystemModel> context) {
      Iterable<Model> it = context.models().getModels(FanShutoffAdapter.IS_FAN_SHUTOFF_CAPABLE_DEVICE);
      if(it == null || !it.iterator().hasNext()) {
         context.model().setFanShutoffSupported(false);
      }else{
         context.model().setFanShutoffSupported(true);
      }
   }

   public static void syncRecordOnSecurity(@NonNull SubsystemContext<AlarmSubsystemModel> context) {
      SubsystemUtils.setIfNull(context.model(), AlarmSubsystemCapability.ATTR_RECORDINGSUPPORTED, Boolean.FALSE);
      SubsystemUtils.setIfNull(context.model(), AlarmSubsystemCapability.ATTR_RECORDINGDURATIONSEC, RecordOnSecurityAdapter.RECORDING_DURATION_DEFAULT);
      SubsystemUtils.setIfNull(context.model(), AlarmSubsystemCapability.ATTR_RECORDONSECURITY, RecordOnSecurityAdapter.RECORD_ON_SECURITY_DEFAULT);
      syncRecordOnSecuritySupported(context);
   }

   public static void syncRecordOnSecuritySupported(SubsystemContext<AlarmSubsystemModel> context) {
      RecordOnSecurityAdapter adapter = new RecordOnSecurityAdapter(context);
      if(adapter.isRecordingSupported()) {
         context.model().setRecordingSupported(true);
      }else{
         context.model().setRecordingSupported(false);
      }
   }

   public static void shutoffValvesIfNeeded(SubsystemContext<? extends SubsystemModel> context) {
      Model safetySubsystem = context.models().getModelByAddress(Address.platformService(context.getPlaceId(), SafetySubsystemCapability.NAMESPACE));
      if(safetySubsystem == null) {
         context.logger().warn("Unable to load safety subsystem, won't shut off any leaks");
      }
      else if(SafetySubsystemModel.getWaterShutOff(safetySubsystem, false)) {
         context.logger().info("Shutting of water valves due to leak");
         SubsystemUtils.sendTo(context, WaterAlarm.isWaterValve(), WaterAlarm.closeValve());
      }
      else {
         context.logger().info("Water valve shutoff is disabled, not shutting off");
      }
   }

   public static void syncKeyPadSounds(SubsystemContext<AlarmSubsystemModel> context) {
      AlarmSubsystemState.Name state = AlarmSubsystemState.get(context).getName();
      // only change keypads sound mask in "stable" states
      if(AlarmSubsystemState.Name.DISARMED == state || AlarmSubsystemState.Name.ARMED == state) {
         KeyPad.syncSounds(context);
      }
   }
   
   public static boolean syncAlarmProviderIfNecessary(SubsystemContext<AlarmSubsystemModel> context, boolean checkHubOnline, Model hub, CheckSecurityModeOption checkSecurityMode) {
   	if(alarmProviderMatchesWithRequested(context)) {
   		return false;
   	}
   	if(checkHubOnline) {
   		if(hub == null) {
   			hub = AlarmUtil.getHubModel(context, false);
   		}
   		if(hub == null ) {
   			recordFailedAttemptForSyncAlarmProvider(context, "Can not change alarm provider because hub does not exist");
   			return false;
   		}else if(!HubConnectionModel.isStateONLINE(hub)) {
   			recordFailedAttemptForSyncAlarmProvider(context, "Can not change alarm provider because hub is offline");
   			return false;
   		}
   	}
   	switch(checkSecurityMode) {
   		case DISARMED_ONLY:
   			if(!context.model().isSecurityModeDISARMED()) {
   				recordFailedAttemptForSyncAlarmProvider(context, "Can not change alarm provider because security mode is not disarmed");
      			return false;
      		}
   			break;
   		case DISARMED_OR_INACTIVE:
   			if(!context.model().isSecurityModeDISARMED() && !context.model().isSecurityModeINACTIVE()) {
   				recordFailedAttemptForSyncAlarmProvider(context, "Can not change alarm provider because security mode is not disarmed or inactive");
      			return false;
      		}
   			break;
   		default:
   			
   	}
   	doSyncAlarmProvider(context);
   	return true;
   }
   
   private static void recordFailedAttemptForSyncAlarmProvider(SubsystemContext<AlarmSubsystemModel> context, String errorMsg) {
   	context.model().setLastAlarmProviderAttempt(new Date());
   	context.model().setLastAlarmProviderError(errorMsg);
   }
   
   private static boolean alarmProviderMatchesWithRequested(SubsystemContext<AlarmSubsystemModel> context) {
   	String requestedProvider = context.model().getRequestedAlarmProvider();
   	return requestedProvider == null || requestedProvider.equals(context.model().getAlarmProvider());
   }
   
   private static void doSyncAlarmProvider(SubsystemContext<AlarmSubsystemModel> context) {
   	Boolean alreadyInProgress = context.getVariable(VAR_SYNC_ALARM_PROVIDER_IN_PROGRESS).as(Boolean.class);
   	if(alreadyInProgress == null) {
   		context.setVariable(VAR_SYNC_ALARM_PROVIDER_IN_PROGRESS, Boolean.TRUE);
      	//context.logger().debug("********* doSyncAlarmProvider is called for place {}", context.getPlaceId());
      	MessageBody message = SetProviderRequest.builder().withProvider(context.model().getRequestedAlarmProvider()).build();
   		context.model().setLastAlarmProviderAttempt(new Date());
   		
   		context.sendAndExpectResponse(PlatformServiceAddress.platformService(context.getPlaceId(), AlarmSubsystemCapability.NAMESPACE), 
   			message, 
   			AlarmUtil.ServiceRequestTimeoutSec, 
   			TimeUnit.SECONDS, 
   			new SubsystemContext.ResponseAction<AlarmSubsystemModel>(){
   				@Override
   				public void onResponse(SubsystemContext<AlarmSubsystemModel> context, PlatformMessage response) {
   					context.model().setLastAlarmProviderError(extractErrorMessage(response.getValue()));
   					context.setVariable(VAR_SYNC_ALARM_PROVIDER_IN_PROGRESS, null);
   				}
   
   				@Override
   				public void onError(SubsystemContext<AlarmSubsystemModel> context, Throwable cause) {
   					context.model().setLastAlarmProviderError(extractErrorMessage(Errors.fromException(cause)));
   					context.setVariable(VAR_SYNC_ALARM_PROVIDER_IN_PROGRESS, null);
   				}
   
   				@Override
   				public void onTimeout(SubsystemContext<AlarmSubsystemModel> context) {
   					context.model().setLastAlarmProviderError(extractErrorMessage(Errors.hubOffline()));		
   					context.setVariable(VAR_SYNC_ALARM_PROVIDER_IN_PROGRESS, null);
   				}				
   		});
   	}

   }
	
	public static String extractErrorMessage(MessageBody msg) {
		if(msg != null && msg instanceof ErrorEvent) {
			return ((ErrorEvent)msg).getMessage();
		}else {
			return "";
		}
	}

}

