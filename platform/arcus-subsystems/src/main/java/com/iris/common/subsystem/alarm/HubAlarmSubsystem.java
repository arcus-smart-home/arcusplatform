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

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.annotation.Version;
import com.iris.capability.key.NamespacedKey;
import com.iris.common.subsystem.BaseSubsystem;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.common.subsystem.alarm.AlarmUtil.CheckSecurityModeOption;
import com.iris.common.subsystem.alarm.co.CarbonMonoxideAlarm;
import com.iris.common.subsystem.alarm.generic.AlarmState;
import com.iris.common.subsystem.alarm.incident.AlarmIncidentService;
import com.iris.common.subsystem.alarm.panic.PanicAlarm;
import com.iris.common.subsystem.alarm.security.SecurityAlarm;
import com.iris.common.subsystem.alarm.security.SecurityErrors;
import com.iris.common.subsystem.alarm.smoke.SmokeAlarm;
import com.iris.common.subsystem.alarm.water.WaterAlarm;
import com.iris.common.subsystem.annotation.Subsystem;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.DeviceDriverAddress;
import com.iris.messages.address.DeviceProtocolAddress;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.AlarmIncidentCapability;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.AlarmSubsystemCapability.SecurityHubDisarmingException;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.HubAlarmCapability;
import com.iris.messages.capability.HubConnectionCapability;
import com.iris.messages.capability.KeyPadCapability;
import com.iris.messages.capability.PersonCapability;
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
import com.iris.messages.event.ModelReportEvent;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.listener.annotation.OnAdded;
import com.iris.messages.listener.annotation.OnMessage;
import com.iris.messages.listener.annotation.OnRemoved;
import com.iris.messages.listener.annotation.OnReport;
import com.iris.messages.listener.annotation.OnScheduledEvent;
import com.iris.messages.listener.annotation.OnValueChanged;
import com.iris.messages.listener.annotation.Request;
import com.iris.messages.model.Model;
import com.iris.messages.model.ServiceLevel;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.dev.DeviceAdvancedModel;
import com.iris.messages.model.hub.HubConnectionModel;
import com.iris.messages.model.serv.AlarmIncidentModel;
import com.iris.messages.model.serv.AlarmModel;
import com.iris.messages.model.serv.HubAlarmModel;
import com.iris.messages.model.serv.PlaceModel;
import com.iris.messages.model.subs.AlarmSubsystemModel;
import com.iris.messages.model.subs.SafetySubsystemModel;
import com.iris.messages.model.subs.SecurityAlarmModeModel;
import com.iris.messages.model.subs.SecuritySubsystemModel;
import com.iris.messages.service.AlarmService;
import com.iris.messages.type.IncidentTrigger;

@Singleton
@Subsystem(AlarmSubsystemModel.class)
@Version(2)
public class HubAlarmSubsystem extends BaseSubsystem<AlarmSubsystemModel> {

   // visible for testing
   static final String TO_PREALERTTIMER = "prealertTimer";
   static final String VAR_CANCELINCIDENT = "cancelIncident";
   static final String VAR_TRIGGERINFO = "triggerInfo";
   static final String VAR_PANIC_TRIGGER = "panicTrigger";

   private static final String SECURITY_PREFIX = "hubalarm:security";
   private static final String PANIC_PREFIX = "hubalarm:panic";
   private static final String CO_PREFIX = "hubalarm:co";
   private static final String SMOKE_PREFIX = "hubalarm:smoke";
   private static final String WATER_PREFIX = "hubalarm:water";

   @Nullable
   private static String hubToInstanceName(String name, String prefix, String suffix, int offset) {
      if (name == null || name.length() <= offset || !name.startsWith(prefix)) {
         return null;
      }
      return "alarm:" + Character.toLowerCase(name.charAt(offset)) + name.substring(offset+1) + suffix;
   }

   @Nullable
   private static String instanceToHubName(String name) {
      if(name == null) {
         return null;
      }
      String[] parts = name.split(":");
      if(parts.length != 3) {
         return null;
      }
      return "hubalarm:" + parts[2].toLowerCase() + StringUtils.capitalize(parts[1]);
   }

   @Nullable
   private static String hubNameToInstance(String name) {
      if(StringUtils.startsWith(name, SECURITY_PREFIX)) {
         return secNameTransformer.apply(name);
      }
      if(StringUtils.startsWith(name, PANIC_PREFIX)) {
         return panicNameTransformer.apply(name);
      }
      if(StringUtils.startsWith(name, CO_PREFIX)) {
         return coNameTransformer.apply(name);
      }
      if(StringUtils.startsWith(name, SMOKE_PREFIX)) {
         return smokeNameTransformer.apply(name);
      }
      if(StringUtils.startsWith(name, WATER_PREFIX)) {
         return waterNameTransformer.apply(name);
      }
      return null;
   }

   private static final Function<String, String> secNameTransformer = new Function<String, String>() {
      final String SUFFIX = ":SECURITY";
      final int OFFSET = SECURITY_PREFIX.length();

      @Override
      public String apply(String input){
         return hubToInstanceName(input, SECURITY_PREFIX, SUFFIX, OFFSET);
      }
   };

   private static final Function<String, String> panicNameTransformer = new Function<String, String>() {
      final String SUFFIX = ":PANIC";
      final int OFFSET = PANIC_PREFIX.length();

      @Override
      public String apply(String input) {
         return hubToInstanceName(input, PANIC_PREFIX, SUFFIX, OFFSET);
      }
   };

   private static final Function<String, String> coNameTransformer = new Function<String, String>() {
      final String SUFFIX = ":CO";
      final int OFFSET = CO_PREFIX.length();

      @Override
      public String apply(String input) {
         return hubToInstanceName(input, CO_PREFIX, SUFFIX, OFFSET);
      }
   };

   private static final Function<String, String> smokeNameTransformer = new Function<String, String>() {
      final String SUFFIX = ":SMOKE";
      final int OFFSET = SMOKE_PREFIX.length();

      @Override
      public String apply(String input) {
         return hubToInstanceName(input, SMOKE_PREFIX, SUFFIX, OFFSET);
      }
   };

   private static final Function<String, String> waterNameTransformer = new Function<String, String>() {
      final String SUFFIX = ":WATER";
      final int OFFSET = WATER_PREFIX.length();

      @Override
      public String apply(String input) {
         return hubToInstanceName(input, WATER_PREFIX, SUFFIX, OFFSET);
      }
   };

   private static final Function<String, String> alarmNameTransformer = new Function<String, String>() {
      @Override
      public String apply(String input) {
         NamespacedKey key = NamespacedKey.parse(input);
         return NamespacedKey.of(AlarmSubsystemCapability.NAMESPACE, key.getName()).getRepresentation();
      }
   };

   @Named("alarm.hub.offline.prealert.buffer.secs")
   @Inject(optional = true)
   private long hubOfflinePrealertBuffer = 60;

   private final AlarmIncidentService incidentService;
   private final CallTree callTree = new CallTree();

   @Inject
   public HubAlarmSubsystem(AlarmIncidentService incidentService) {
      this.incidentService = incidentService;
   }

   @Override
   protected void onStarted(SubsystemContext<AlarmSubsystemModel> context) {
      super.onStarted(context);
      context.model().setMonitoredAlerts(ImmutableSet.of(CarbonMonoxideAlarm.NAME, PanicAlarm.NAME, SecurityAlarm.NAME, SmokeAlarm.NAME));
      callTree.bind(context);
      syncHubState(context);
      SubsystemUtils.restoreTimeout(context, AlarmUtil.TO_CANCEL);
      SubsystemUtils.restoreTimeout(context, TO_PREALERTTIMER);
      AlarmUtil.syncFanShutoff(context);
      AlarmUtil.syncRecordOnSecurity(context);
      KeyPad.bind(context);
      syncMonitored(context);

   }

   @Override
   protected void onStopped(SubsystemContext<AlarmSubsystemModel> context) {
      super.onStopped(context);
   }

   @Request(Capability.CMD_SET_ATTRIBUTES)
   @Override
   public MessageBody setAttributes(final PlatformMessage message, SubsystemContext<AlarmSubsystemModel> context) {
      Model hub = AlarmUtil.getHubModel(context);
      final Map<String, Object> sendToHub = new HashMap<>();
      final Map<String, Object> handleLocally = new HashMap<>();

      final MessageBody request = message.getValue();
      for(Map.Entry<String, Object> entry : request.getAttributes().entrySet()) {
         if(entry.getKey().startsWith(AlarmCapability.ATTR_SILENT) && !entry.getKey().endsWith(SecurityAlarm.NAME)) {
            String hubAttrName = instanceToHubName(entry.getKey());
            if(hubAttrName == null) {
               context.logger().info("ignoring attempt to set {} to {}, it could not be transformed to a hub attribute", entry.getKey(), entry.getValue());
               continue;
            }
            sendToHub.put(hubAttrName, entry.getValue());
         } else {
            handleLocally.put(entry.getKey(), entry.getValue());
         }
      }

      if(sendToHub.isEmpty()) {
         return super.setAttributes(message, context);
      }

      MessageBody hrequest = MessageBody.buildMessage(Capability.CMD_SET_ATTRIBUTES, sendToHub);
      context.sendAndExpectResponse(hub.getAddress(), hrequest, 60, TimeUnit.SECONDS, new SubsystemContext.ResponseAction<AlarmSubsystemModel>() {
         @Override
         public void onResponse(SubsystemContext<AlarmSubsystemModel> context, PlatformMessage response) {
            List<Map<String, Object>> errors = new ArrayList<>();
            if(response.isError()) {
               errors.add(response.getValue().getAttributes());
            } else {
               for(Map.Entry<String, Object> entry : sendToHub.entrySet()) {
                  String instanceName = hubNameToInstance(entry.getKey());
                  if(instanceName == null) {
                     context.logger().warn("could not transform {} to an instance name, ignoring", entry.getKey());
                     continue;
                  }
                  setAttribute(instanceName, entry.getValue(), context);
               }
            }
            handleSetLocalAndSendResponse(message, handleLocally, errors, context);
         }

         @Override
         public void onError(SubsystemContext<AlarmSubsystemModel> context, Throwable cause) {
            List<Map<String, Object>> errors = new ArrayList<>();
            errors.add(Errors.fromException(cause).getAttributes());
            handleSetLocalAndSendResponse(message, handleLocally, errors, context);
         }

         @Override
         public void onTimeout(SubsystemContext<AlarmSubsystemModel> context) {
            List<Map<String, Object>> errors = new ArrayList<>();
            errors.add(Errors.hubOffline().getAttributes());
            handleSetLocalAndSendResponse(message, handleLocally, errors, context);
         }
      });

      return MessageBody.noResponse();
   }

   private void handleSetLocalAndSendResponse(PlatformMessage request, Map<String, Object> attributes, List<Map<String, Object>> errors, SubsystemContext<AlarmSubsystemModel> context) {
      for(Map.Entry<String, Object> entry: attributes.entrySet()) {
         try {
            setAttribute(entry.getKey(), entry.getValue(), context);
         }
         catch(ErrorEventException e) {
            context.logger().warn("Error setting attribute [{}]", entry.getKey(), e);
            errors.add(e.toErrorEvent().getAttributes());
         }
         catch(Exception e) {
            context.logger().warn("Error setting attribute [{}]", entry.getKey(), e);
            errors.add(Errors.invalidParam(entry.getKey()).getAttributes());
         }
      }

      if(errors.isEmpty()) {
         AlarmUtil.sendResponse(context, request, MessageBody.emptyMessage());
      } else {
         AlarmUtil.sendResponse(context, request, MessageBody.buildMessage(Capability.EVENT_SET_ATTRIBUTES_ERROR, ImmutableMap.<String, Object>of("errors", errors)));
      }
   }

   @OnValueChanged(
      query=AlarmSubsystem.QUERY_ALARM,
      attributes=AlarmCapability.ATTR_SILENT + ":" + AlarmSubsystemCapability.ACTIVEALERTS_SECURITY
   )
   public void onSilentChanged(SubsystemContext<AlarmSubsystemModel> context, Model model, ModelChangedEvent event) {
      AlarmUtil.syncKeyPadSounds(context);
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

   // timeout
   @OnScheduledEvent
   public void onEvent(ScheduledEvent event, SubsystemContext<AlarmSubsystemModel> context) {
      if(SubsystemUtils.isMatchingTimeout(event, context, AlarmUtil.TO_CANCEL)) {
         context.logger().debug("trying to cancel incident due to it's retry timeout");
         tryCancel(context);
      } else if(SubsystemUtils.isMatchingTimeout(event, context, TO_PREALERTTIMER)) {
         context.logger().debug("signalling alert because hub has not reported a state change");
         TriggerInfo ti = getTriggerInfo(context);
         List<Map<String, Object>> events = AlarmModel.getTriggers(SecurityAlarm.NAME, context.model(), ImmutableList.<Map<String,Object>>of());
         ti.setIndex(SecurityAlarm.NAME, events.size());
         setTriggerInfo(context, ti);
         incidentService.addAlert(context, SecurityAlarm.NAME, AlarmUtil.eventsToTriggers(events, 0));
         AlarmModel.setAlertState(SecurityAlarm.NAME, context.model(), AlarmCapability.ALERTSTATE_ALERT);
         context.model().setAlarmState(AlarmSubsystemCapability.ALARMSTATE_ALERTING);
      }
   }

   @OnValueChanged(attributes=HubConnectionCapability.ATTR_STATE)
   public void onHubConnectivityChanged(SubsystemContext<AlarmSubsystemModel> context, Model hub) {
      incidentService.onHubConnectivityChanged(context, hub);
      if(
            HubConnectionModel.isStateONLINE(hub) 
      ) {
      	if(context.model().isAlarmStateCLEARING() &&
            (HubAlarmModel.isAlarmStatePREALERT(hub) || HubAlarmModel.isAlarmStateALERTING(hub))) {
            // incident cleared while hub was offline,
            context.logger().debug("Detected hub reconnect with alarm cancelled platform side, attempting to disarm hub");
            sendDisarm(context, hub);
      	}else if(context.model().isSecurityModeDISARMED()) {
      		//hub comes online while the system is disarmed
      		syncAlarmProviderIfNecessary(context, false, hub, CheckSecurityModeOption.IGNORE);
      	}
      }
   }

   @Request(SubsystemCapability.ActivateRequest.NAME)
   public void activate(final SubsystemContext<AlarmSubsystemModel> context, PlatformMessage request) {
      if(AlarmUtil.isActive(context)) {
         return;
      }

      assertValidActivateState(context);
      AlarmUtil.copyCallTree(context, callTree);
      AlarmUtil.copySilent(context);

      AlarmUtil.sendHubRequest(context, request, HubAlarmCapability.ActivateRequest.instance(),
         new Function<PlatformMessage, MessageBody>() {
            @Override
            public MessageBody apply(PlatformMessage input) {
               syncHubState(context);
               context.model().setState(SubsystemCapability.STATE_ACTIVE);
               context.model().setAvailable(true);
               return SubsystemCapability.ActivateResponse.instance();
            }
         }
      );
   }

   private void assertValidActivateState(SubsystemContext<AlarmSubsystemModel> context) {
      Model securitySubsystem = context.models().getModelByAddress(Address.platformService(context.getPlaceId(), SecuritySubsystemCapability.NAMESPACE));
      Model safetySubsystem = context.models().getModelByAddress(Address.platformService(context.getPlaceId(), SafetySubsystemCapability.NAMESPACE));

      if(SafetySubsystemModel.isStateACTIVE(safetySubsystem) && !SafetySubsystemModel.isAlarmREADY(safetySubsystem)) {
         throw new ErrorEventException(Errors.invalidRequest("Can't upgrade during a safety alarm, please clear the safety alarm first"));
      }

      if(SecuritySubsystemModel.isStateACTIVE(securitySubsystem) && !SecuritySubsystemModel.isAlarmStateDISARMED(securitySubsystem)) {
         throw new ErrorEventException(Errors.invalidRequest("Can't upgrade while the security alarm is not disarmed, please disarm the security alarm first"));
      }

      // make sure that if any devices were added while downgraded are hublocal
      assertCanSwitchTo(context);
   }

   @Request(SubsystemCapability.SuspendRequest.NAME)
   public void suspend(final SubsystemContext<AlarmSubsystemModel> context, PlatformMessage request) {
      if(!AlarmUtil.isActive(context)) {
         return;
      }

      assertValidSuspendState(context);

      AlarmUtil.sendHubRequest(context, request, HubAlarmCapability.SuspendRequest.instance(),
         new Function<PlatformMessage, MessageBody>() {
            @Override
            public MessageBody apply(PlatformMessage input) {
               context.model().setState(SubsystemCapability.STATE_SUSPENDED);
               context.model().setAvailable(false);
               return SubsystemCapability.SuspendResponse.instance();
            }
         }
      );
   }

   private void assertValidSuspendState(SubsystemContext<AlarmSubsystemModel> context) {
      if(context.model().isAlarmStateALERTING()) {
         throw new ErrorEventException(Errors.invalidRequest("Can't downgrade during an alarm, please cancel the incident first"));
      }
      String securityAlarmMode = AlarmModel.getAlertState(SecurityAlarm.NAME, context.model());
      if(!AlarmCapability.ALERTSTATE_DISARMED.equals(securityAlarmMode)) {
         throw new ErrorEventException(Errors.invalidRequest("Can't downgrade while the security alarm is armed, please disarm first"));
      }
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
      incidentService.verify(context, message.getDestination(), message.getActor());
      Model hub = AlarmUtil.getHubModel(context);
      // TODO:  does this need to be a request/response?  I'm not sure what would happen in the error case other than
      // logging because any promonitoring signalling won't be undone.
      context.send(hub.getAddress(), HubAlarmCapability.VerifiedEvent.instance());
   }

   private static final Function<Pair<AlarmIncidentModel, SubsystemContext<AlarmSubsystemModel>>, MessageBody> CANCEL_RESPONSE_SUPPLIER = new Function<Pair<AlarmIncidentModel, SubsystemContext<AlarmSubsystemModel>>, MessageBody>() {
      @Override
      public MessageBody apply(Pair<AlarmIncidentModel, SubsystemContext<AlarmSubsystemModel>> input) {
         return MessageBody.noResponse();
      }
   };

   @Request(value= AlarmIncidentCapability.CancelRequest.NAME)
   public MessageBody cancelIncident(SubsystemContext<AlarmSubsystemModel> context, PlatformMessage message) {
      if(!message.getDestination().getRepresentation().equals(context.model().getCurrentIncident())) {
         throw new ErrorEventException(Errors.invalidRequest("Incident is not currently active or cancellation has already been requested"));
      }
      AlarmUtil.assertActive(context);
      AlarmIncidentModel incident = cancel(context, message, AlarmService.CancelAlertRequest.METHOD_APP);
      // FIXME we need a custom error handler, but this shouldn't happen until the hub responds or times out
      return AlarmUtil.buildIncidentCancelResponse(incident, context);
   }

   @OnMessage(types=Capability.EVENT_VALUE_CHANGE, from="SERV:incident:*")
   public void onIncidentChanged(SubsystemContext<AlarmSubsystemModel> context, PlatformMessage message) {
      if(
            context.model().isAlarmStateCLEARING() && 
            AlarmIncidentCapability.PLATFORMSTATE_COMPLETE.equals(message.getValue().getAttributes().get(AlarmIncidentCapability.ATTR_PLATFORMSTATE))
      ) {
         context.logger().debug("Platform side completed, attempting to clear hub, for incident: [{}]", message.getSource());
         tryCancel(context);
      }
   }
   
   @OnMessage(types=AlarmIncidentCapability.CompletedEvent.NAME)
   public void onIncidentCompleted(SubsystemContext<AlarmSubsystemModel> context, PlatformMessage message) {
      context.logger().debug("Completed incident [{}]", message.getSource());
      onCompleted(context);
   }

   @OnReport(query = "base:caps contains 'hubalarm'")
   public void onReport(SubsystemContext<AlarmSubsystemModel> context, Model model, ModelReportEvent event) {
      Map<String, ModelReportEvent.ValueChange> changes = event.getChanges();
      if(changes == null || changes.isEmpty()) {
         return;
      }

      Model before = new SimpleModel(context.model());

      boolean hubalarmChanges = false;
      for(Map.Entry<String, ModelReportEvent.ValueChange> change : changes.entrySet()) {
         if(change.getKey().startsWith(HubAlarmCapability.NAMESPACE)) {
            hubalarmChanges = true;
            setContext(context, change.getKey(), change.getValue().getNewValue());
         }
      }

      if(hubalarmChanges) {
      	Model hub = AlarmUtil.getHubModel(context, false);
         sync(context, before, hub);
      }
   }
   
   @OnValueChanged(attributes={AlarmSubsystemCapability.ATTR_REQUESTEDALARMPROVIDER})
	public void onRequestedAlarmProviderChanged(ModelChangedEvent event, SubsystemContext<AlarmSubsystemModel> context) {
		syncAlarmProviderIfNecessary(context, true, null, CheckSecurityModeOption.DISARMED_OR_INACTIVE);
	}

   private void syncHubState(SubsystemContext<AlarmSubsystemModel> context) {
      Model hub = AlarmUtil.getHubModel(context, false);
      if(hub == null) {
         context.logger().error("unable to sync hub alarm state, no hub found at place {}", context.getPlaceId());
         return;
      }

      Model before = new SimpleModel(context.model());
      Map<String,Object> attributes = hub.toMap();
      for(Map.Entry<String,Object> attrEntr : attributes.entrySet()) {
         if(attrEntr.getKey().startsWith(HubAlarmCapability.NAMESPACE)) {
            setContext(context, attrEntr.getKey(), attrEntr.getValue());
         }
      }

      sync(context, before, hub);
   }

   private void setContext(SubsystemContext<AlarmSubsystemModel> context, String attributeName, Object attributeValue) {
      switch(attributeName) {
         // global alarm subsystem
         case HubAlarmCapability.ATTR_ALARMSTATE:
         case HubAlarmCapability.ATTR_SECURITYMODE:
         case HubAlarmCapability.ATTR_SECURITYARMTIME:
         case HubAlarmCapability.ATTR_LASTARMEDTIME:
         case HubAlarmCapability.ATTR_LASTDISARMEDTIME:
         case HubAlarmCapability.ATTR_ACTIVEALERTS:
            processValueChange(context, alarmNameTransformer, attributeName, attributeValue);
            break;
         case HubAlarmCapability.ATTR_CURRENTINCIDENT:
            String addr = StringUtils.isEmpty((String) attributeValue) ? "" : "SERV:incident:" + attributeValue;
            processValueChange(context, alarmNameTransformer, attributeName, addr);
            break;
         case HubAlarmCapability.ATTR_LASTDISARMEDBY:
         case HubAlarmCapability.ATTR_LASTDISARMEDFROM:
         case HubAlarmCapability.ATTR_LASTARMEDBY:
         case HubAlarmCapability.ATTR_LASTARMEDFROM:
            processAddressChange(context, alarmNameTransformer, attributeName, attributeValue);
            break;

         // security specific
         case HubAlarmCapability.ATTR_SECURITYALERTSTATE:
         case HubAlarmCapability.ATTR_SECURITYSILENT:
            processValueChange(context, secNameTransformer, attributeName, attributeValue);
            break;
         case HubAlarmCapability.ATTR_SECURITYDEVICES:
         case HubAlarmCapability.ATTR_SECURITYEXCLUDEDDEVICES:
         case HubAlarmCapability.ATTR_SECURITYACTIVEDEVICES:
         case HubAlarmCapability.ATTR_SECURITYOFFLINEDEVICES:
         case HubAlarmCapability.ATTR_SECURITYTRIGGEREDDEVICES:
            processDeviceAddressSetChange(context, secNameTransformer, attributeName, attributeValue);
            syncAlarmProviderIfNecessary(context, true, null, CheckSecurityModeOption.DISARMED_OR_INACTIVE);
            break;
         case HubAlarmCapability.ATTR_SECURITYTRIGGERS:
            processTriggersChange(context, secNameTransformer, attributeName, attributeValue);
            break;

         // co specific
         case HubAlarmCapability.ATTR_COALERTSTATE:
         case HubAlarmCapability.ATTR_COSILENT:
            processValueChange(context, coNameTransformer, attributeName, attributeValue);
            break;
         case HubAlarmCapability.ATTR_COACTIVEDEVICES:
         case HubAlarmCapability.ATTR_COOFFLINEDEVICES:
         case HubAlarmCapability.ATTR_COTRIGGEREDDEVICES:
            processDeviceAddressSetChange(context, coNameTransformer, attributeName, attributeValue);
            break;
         case HubAlarmCapability.ATTR_COTRIGGERS:
            processTriggersChange(context, coNameTransformer, attributeName, attributeValue);
            break;

         // smoke specific
         case HubAlarmCapability.ATTR_SMOKEALERTSTATE:
         case HubAlarmCapability.ATTR_SMOKESILENT:
            processValueChange(context, smokeNameTransformer, attributeName, attributeValue);
            break;
         case HubAlarmCapability.ATTR_SMOKEACTIVEDEVICES:
         case HubAlarmCapability.ATTR_SMOKEOFFLINEDEVICES:
         case HubAlarmCapability.ATTR_SMOKETRIGGEREDDEVICES:
            processDeviceAddressSetChange(context, smokeNameTransformer, attributeName, attributeValue);
            break;
         case HubAlarmCapability.ATTR_SMOKETRIGGERS:
            processTriggersChange(context, smokeNameTransformer, attributeName, attributeValue);
            break;

         // water specific
         case HubAlarmCapability.ATTR_WATERALERTSTATE:
         case HubAlarmCapability.ATTR_WATERSILENT:
            processValueChange(context, waterNameTransformer, attributeName, attributeValue);
            break;
         case HubAlarmCapability.ATTR_WATERACTIVEDEVICES:
         case HubAlarmCapability.ATTR_WATEROFFLINEDEVICES:
         case HubAlarmCapability.ATTR_WATERTRIGGEREDDEVICES:
            processDeviceAddressSetChange(context, waterNameTransformer, attributeName, attributeValue);
            break;
         case HubAlarmCapability.ATTR_WATERTRIGGERS:
            processTriggersChange(context, waterNameTransformer, attributeName, attributeValue);
            break;

         // panic specific
         case HubAlarmCapability.ATTR_PANICALERTSTATE:
         case HubAlarmCapability.ATTR_PANICSILENT:
            processValueChange(context, panicNameTransformer, attributeName, attributeValue);
            break;
         case HubAlarmCapability.ATTR_PANICACTIVEDEVICES:
         case HubAlarmCapability.ATTR_PANICOFFLINEDEVICES:
         case HubAlarmCapability.ATTR_PANICTRIGGEREDDEVICES:
            processDeviceAddressSetChange(context, panicNameTransformer, attributeName, attributeValue);
            break;
         case HubAlarmCapability.ATTR_PANICTRIGGERS:
            processTriggersChange(context, panicNameTransformer, attributeName, attributeValue);
            break;
      }
   }

   private void processTriggersChange(SubsystemContext<AlarmSubsystemModel> context, Function<String,String> nameTransformer, String attributeName, Object attributeValue) {
      ImmutableList.Builder<Map<String,Object>> transformedTriggers = ImmutableList.builder();
      Collection<Map<String,Object>> triggers = (Collection<Map<String,Object>>) attributeValue;
      for(Map<String, Object> t : triggers) {
         IncidentTrigger incidentTrigger = new IncidentTrigger(t);

         Model dev = findDeviceByProtAddr(context, Address.fromString(incidentTrigger.getSource()));
         if(dev != null) {
            incidentTrigger.setSource(dev.getAddress().getRepresentation());
         }
         transformedTriggers.add(incidentTrigger.toMap());
      }
      processValueChange(context, nameTransformer, attributeName, transformedTriggers.build());
   }

   private void processDeviceAddressSetChange(SubsystemContext<AlarmSubsystemModel> context, Function<String,String> nameTransformer, String attributeName, Object attributeValue) {
      Collection<String> protAddrs = (Collection<String>) attributeValue;
      ImmutableSet.Builder<String> platAddrs = ImmutableSet.builder();
      for(String s : protAddrs) {
         Address addr = Address.fromString(s);
         Model dev = findDeviceByProtAddr(context, addr);
         if(dev == null) {
            context.logger().info("no device found for protocol address {}", s);
            continue;
         }
         platAddrs.add(dev.getAddress().getRepresentation());
      }
      processValueChange(context, nameTransformer, attributeName, platAddrs.build());
   }

   private void processAddressChange(SubsystemContext<AlarmSubsystemModel> context, Function<String,String> nameTransformer, String attributeName, Object attributeValue) {
      if(attributeValue != null) {
         String newValue = (String) attributeValue;
         Address addr = Address.fromString((String) attributeValue);
         Model m = findDeviceByProtAddr(context, addr);
         if(m != null) {
            newValue = m.getAddress().getRepresentation();
         }
         processValueChange(context, nameTransformer, attributeName, newValue);
      }
   }

   @Nullable
   private Model findDeviceByProtAddr(SubsystemContext<AlarmSubsystemModel> context, Address addr) {
      if(!DeviceProtocolAddress.class.equals(addr.getClass())) {
         context.logger().info("ignoring non protocol address {}", addr);
         return null;
      }

      DeviceProtocolAddress protAddr = (DeviceProtocolAddress) addr;
      String prot = protAddr.getProtocolName();
      ProtocolDeviceId devId = protAddr.getProtocolDeviceId();
      Iterable<Model> devs = context.models().getModelsByType(DeviceCapability.NAMESPACE);
      for(Model m : devs) {
         if(Objects.equal(prot, DeviceAdvancedModel.getProtocol(m)) && Objects.equal(DeviceAdvancedModel.getProtocolid(m), devId.getRepresentation())) {
            return m;
         }
      }
      return null;
   }

   private void processValueChange(SubsystemContext<AlarmSubsystemModel> context, Function<String,String> nameTransformer, String attributeName, Object attributeValue) {
      String newName = nameTransformer.apply(attributeName);
      if(newName == null) {
         context.logger().warn("ignoring value change of attribute {}, new name could not be determined", attributeName);
         return;
      }
      if(attributeName.endsWith("AlertState") && Objects.equal(HubAlarmCapability.SECURITYALERTSTATE_PENDING_CLEAR, attributeValue)) {
         attributeValue = AlarmCapability.ALERTSTATE_CLEARING;
      }
      context.model().setAttribute(newName, attributeValue);
   }

   private void sync(SubsystemContext<AlarmSubsystemModel> context, Model before, Model hub) {

      syncDevices(context);

      Map<String, String> prevStates = getAlertStates(before);
      Map<String, String> currentStates = getAlertStates(context.model());

      if(!Objects.equal(prevStates, currentStates)) {
         Set<String> availableAlerts = new HashSet<>();
         for(Map.Entry<String, String> currentState : currentStates.entrySet()) {
            if(!AlarmCapability.ALERTSTATE_INACTIVE.equals(currentState.getValue())) {
               availableAlerts.add(currentState.getKey());
            }
         }
         context.model().setAvailableAlerts(availableAlerts);
      }
      AlarmNotificationUtils.notifyPromonAlertAdded(context, prevStates, currentStates);

      if(hub == null) {
         context.logger().warn("Unable to load the hub while processing a hub sync", new RuntimeException("No hub"));
      }
      // the hub has been disarmed but is still waiting to be cleared by the platform
      else if(AlarmUtil.isPendingClear(hub)) {
         replayIfNecessary(context, before);

         String cancellingInc = context.getVariable(VAR_CANCELINCIDENT).as(String.class);
         if(!isEmpty(cancellingInc)) {
            // both sides have been (independently?) cancelled, we'll record the platform side as the "root" cause
            // FIXME this should be un-necessary
            prepClearing(context, context.getVariable(VAR_CANCELINCIDENT).as(String.class));
         } else {
             // hub was cancelled, let's catch up platform
            context.logger().debug("Alarm cancelled hub side, cancelling on platform, previous platform state [{}]", AlarmSubsystemModel.getAlarmState(before));

            String actor = HubAlarmModel.getLastDisarmedFrom(hub, "");
            prepDisarm(context, actor , HubAlarmModel.getLastDisarmedBy(hub, ""), context.model().getCurrentIncident(), AlarmService.CancelAlertRequest.METHOD_KEYPAD);
         }
         tryCancel(context);
      }
      // the hub has an active alarm
      else if(AlarmUtil.isHubAlerting(hub)) {
         String curInc = AlarmSubsystemModel.getCurrentIncident(before, "");
         if(isEmpty(curInc)) {
            curInc = context.getVariable(VAR_CANCELINCIDENT).as(String.class);
         }
         String hubCurInc = HubAlarmModel.getCurrentIncident(hub, "");
         if(!isEmpty(hubCurInc)) {
            hubCurInc = "SERV:incident:" + hubCurInc;
         }
         if(AlarmSubsystemModel.isAlarmStateCLEARING(before) && StringUtils.equals(curInc, hubCurInc)) {
            // cancelled while the hub was offline
            context.logger().debug("Alarm cancelled platform side, cancelling hub");
            replayIfNecessary(context, before);
            // FIXME this call to prep should be un-necessary
            prepClearing(context, context.getVariable(VAR_CANCELINCIDENT).as(String.class));
            sendDisarm(context, hub);
         }
         else {
            // make sure any inprogress cancellations are cleared so these participate in the current incident
            clearCancellation(context);
            String incident = HubAlarmModel.getCurrentIncident(hub, "");
            String incidentAddress = StringUtils.isEmpty(incident) ? "" : "SERV:incident:" + incident;
            context.model().setCurrentIncident(incidentAddress);
            context.model().setActiveAlerts(HubAlarmModel.getActiveAlerts(hub, ImmutableList.<String>of()));
            syncAlerts(context, before);
         }
      }
      // the hub has been disarmed and is waiting for the platform the clear the incident, but the hub itself doesn't
      // have a current incident.  One known cause for this is a race condition on the hub such that a trigger and
      // disarm request happen close enough in time that the hub doesn't transition from READY -> PREALERT.  Instead
      // it transitions from READY -> CLEARING.  In any case we need to tell the hub to clear otherwise it will be
      // stuck.
      else if(!AlarmUtil.isHubIncidentActive(hub) && hasQuasiClearing(hub)) {
         context.logger().warn("received report of hub waiting for a clear incident but it has no active incident.");
         context.send(hub.getAddress(), HubAlarmCapability.ClearIncidentRequest.instance());
      }
      // the hub is in a non-alarm state
      else {
         String cancellingInc = context.getVariable(VAR_CANCELINCIDENT).as(String.class);
         // the platform was trying to clear an incident but the hub lost it?  Cancel it I guess?
         if(!isEmpty(cancellingInc)) {
            context.logger().warn("Received report with no incident while platform still had incident [{}]", context.getVariable(VAR_CANCELINCIDENT).as(String.class));
            tryCancel(context);
         }
         // the platform had an active incident that the hub doesn't know about?  Cancel it I guess?
         else if(AlarmSubsystemModel.isAlarmStatePREALERT(before) || AlarmSubsystemModel.isAlarmStateALERTING(before)) {
            context.logger().warn("Received report with no incident while platform still had incident [{}] and was in prealert or alert.", AlarmSubsystemModel.getCurrentIncident(before));
            prepClearing(context, AlarmSubsystemModel.getCurrentIncident(before));
            tryCancel(context);
         }
         syncAlerts(context, before);
      } 
   }

   private boolean hasQuasiClearing(Model hub) {
      return HubAlarmModel.isSecurityAlertStatePENDING_CLEAR(hub) ||
             HubAlarmModel.isPanicAlertStatePENDING_CLEAR(hub) ||
             HubAlarmModel.isSmokeAlertStatePENDING_CLEAR(hub) ||
             HubAlarmModel.isCoAlertStatePENDING_CLEAR(hub) ||
             HubAlarmModel.isWaterAlertStatePENDING_CLEAR(hub);
   }

   private void syncDevices(SubsystemContext<AlarmSubsystemModel> context) {
      syncDevices(context, SmokeAlarm.NAME);
      syncDevices(context, CarbonMonoxideAlarm.NAME);
      syncDevices(context, WaterAlarm.NAME);
      syncDevices(context, PanicAlarm.NAME);
   }

   private void syncDevices(SubsystemContext<AlarmSubsystemModel> context, String alarm) {
      ImmutableSet.Builder<String> allDevicesBuilder = ImmutableSet.builder();
      allDevicesBuilder.addAll(AlarmModel.getActiveDevices(alarm, context.model(), ImmutableSet.<String>of()));
      allDevicesBuilder.addAll(AlarmModel.getOfflineDevices(alarm, context.model(), ImmutableSet.<String>of()));
      allDevicesBuilder.addAll(AlarmModel.getTriggeredDevices(alarm, context.model(), ImmutableSet.<String>of()));
      AlarmModel.setDevices(alarm, context.model(), allDevicesBuilder.build());
   }

   private void replayIfNecessary(SubsystemContext<AlarmSubsystemModel> context, Model before) {
      try {
         boolean sendNotifications = !Objects.equal(AlarmSubsystemModel.getCurrentIncident(before, ""), "");
         boolean preAlertAdded = addPreAlertIfNecessary(context, SecurityAlarm.NAME, sendNotifications);
         addPanicTriggerIfNecessary(context);
         for(String alarm : context.model().getActiveAlerts()) {
            Model hub = AlarmUtil.getHubModel(context);
            Date preAlertEnd = HubAlarmModel.getSecurityPreAlertEndTime(hub, new Date(0));
            Date disarmTime = HubAlarmModel.getLastDisarmedTime(hub, new Date());
            if(SecurityAlarm.NAME.equals(alarm) && preAlertEnd.after(disarmTime)) {
               // security never went into alerting
               continue;
            }
            
            TriggerInfo ti = getTriggerInfo(context);
            boolean newAlert = preAlertAdded;
            if(!newAlert) {
               newAlert = ti.getIndex(alarm) == 0;
            }
            List<Map<String, Object>> events = AlarmModel.getTriggers(alarm, context.model(), ImmutableList.<Map<String,Object>>of());
            if(newAlert) {
               incidentService.addAlert(context, alarm, AlarmUtil.eventsToTriggers(events,0), sendNotifications);
            }
            List<IncidentTrigger> triggers = AlarmUtil.eventsToTriggers(events, ti.getIndex(alarm));
            ti.setIndex(alarm, events.size());
            setTriggerInfo(context, ti);
            if(!triggers.isEmpty()) {
               incidentService.updateIncident(context, triggers, sendNotifications);
            }
         }
      }
      catch(Exception e) {
         context.logger().warn("Unable to replay incident", e);
      }
   }

   private Map<String, String> getAlertStates(Model model) {
      return ImmutableMap.of(
         SecurityAlarm.NAME, AlarmModel.getAlertState(SecurityAlarm.NAME, model, AlarmCapability.ALERTSTATE_INACTIVE),
         SmokeAlarm.NAME, AlarmModel.getAlertState(SmokeAlarm.NAME, model, AlarmCapability.ALERTSTATE_INACTIVE),
         CarbonMonoxideAlarm.NAME, AlarmModel.getAlertState(CarbonMonoxideAlarm.NAME, model, AlarmCapability.ALERTSTATE_INACTIVE),
         WaterAlarm.NAME, AlarmModel.getAlertState(WaterAlarm.NAME, model, AlarmCapability.ALERTSTATE_INACTIVE),
         PanicAlarm.NAME, AlarmModel.getAlertState(PanicAlarm.NAME, model, AlarmCapability.ALERTSTATE_INACTIVE)
      );
   }

   private void syncAlerts(SubsystemContext<AlarmSubsystemModel> context, Model before) {
      syncAlert(context, before, SecurityAlarm.NAME);
      syncAlert(context, before, SmokeAlarm.NAME);
      syncAlert(context, before, CarbonMonoxideAlarm.NAME);
      syncAlert(context, before, WaterAlarm.NAME);
      syncAlert(context, before, PanicAlarm.NAME);
   }

   private void syncAlert(SubsystemContext<AlarmSubsystemModel> context, Model before, String alertType) {
      String state = AlarmModel.getAlertState(alertType, context.model());
      if(state == null) {
         return;
      }

      String prevState = AlarmModel.getAlertState(alertType, before);
      switch(state) {
         case AlarmCapability.ALERTSTATE_PREALERT:
            if(!AlarmCapability.ALERTSTATE_PREALERT.equals(prevState)) {
               final Model securitySubsystem = context.models().getModelByAddress(Address.platformService(context.getPlaceId(), SecuritySubsystemCapability.NAMESPACE));
               final int configuredEntranceDelay = SecurityAlarmModeModel.getEntranceDelaySec(context.model().getSecurityMode(), securitySubsystem, 30);
               SubsystemUtils.setTimeout(TimeUnit.SECONDS.toMillis(configuredEntranceDelay + hubOfflinePrealertBuffer), context, TO_PREALERTTIMER);
            }
            addPreAlertIfNecessary(context, alertType, true);
            break;
         case AlarmCapability.ALERTSTATE_ALERT:
            SubsystemUtils.clearTimeout(context, TO_PREALERTTIMER);
            addPreAlertIfNecessary(context, alertType, true);

            if(!AlarmCapability.ALERTSTATE_ALERT.equals(prevState)) {
               addPanicTriggerIfNecessary(context);
               addAlert(context, alertType);
               handleValveShutoff(context, alertType);
               handleFanShutoff(context, alertType);
               handleRecording(context, alertType);
            }

            updateAlertIfNewTriggers(context, alertType);
            break;
         default:
            if(AlarmCapability.ALERTSTATE_PREALERT.equals(prevState)) {
               SubsystemUtils.clearTimeout(context, TO_PREALERTTIMER);
            }
            addPreAlertIfNecessary(context, alertType, true);
            break;
      }
   }

   private void addAlert(SubsystemContext<AlarmSubsystemModel> context, String alertType) {
      // replay all the trigger when transitioning to ALERT so they can be included
      // as initial triggers in notifications and sent to UCC
      // BUT don't touch TriggerInfo, updateIncident will get it and add history entries
      // if they haven't already been sent
      List<IncidentTrigger> triggers = AlarmUtil.eventsToTriggers(AlarmModel.getTriggers(alertType, context.model(), ImmutableList.<Map<String,Object>>of()), 0);
      incidentService.addAlert(context, alertType, triggers);
   }
   
   private void updateAlertIfNewTriggers(SubsystemContext<AlarmSubsystemModel> context, String alarmType) {
      TriggerInfo ti = getTriggerInfo(context);
      int idx = ti.getIndex(alarmType);
      List<Map<String, Object>> events = AlarmModel.getTriggers(alarmType, context.model(), ImmutableList.<Map<String,Object>>of());
      List<IncidentTrigger> triggers = AlarmUtil.eventsToTriggers(events, idx);
      if(!triggers.isEmpty()) {
         ti.setIndex(alarmType, events.size());
         setTriggerInfo(context, ti);
         incidentService.updateIncident(context, triggers);
      }
   }
   
   private void handleValveShutoff(SubsystemContext<AlarmSubsystemModel> context, String alertType) {
      switch(alertType) {
         case WaterAlarm.NAME:
            AlarmUtil.shutoffValvesIfNeeded(context);
            break;
         default:
            break;
      }
   }

   private void handleFanShutoff(SubsystemContext<AlarmSubsystemModel> context, String alertType) {
      switch(alertType) {
         case CarbonMonoxideAlarm.NAME:
         case SmokeAlarm.NAME:
            new FanShutoffAdapter(context).fanShutoffIfNecessary(alertType);
            break;
         default:
            break;
      }
   }

   private void handleRecording(SubsystemContext<AlarmSubsystemModel> context, String alertType) {
      switch(alertType) {
         case PanicAlarm.NAME:
         case SecurityAlarm.NAME:
            new RecordOnSecurityAdapter(context).sendRecordMessageIfNecessary();
            break;
         default:
            break;
      }
   }

   private boolean addPreAlertIfNecessary(SubsystemContext<AlarmSubsystemModel> context, String alertType, boolean sendNotifications) {
      if(!SecurityAlarm.NAME.equals(alertType)) {
         return false;
      }

      if(
         AlarmModel.isAlertStateDISARMED(alertType, context.model()) ||
         AlarmModel.isAlertStateARMING(alertType, context.model()) ||
         AlarmModel.isAlertStateREADY(alertType, context.model())
      ) {
         return false;
      }

      Model hub = AlarmUtil.getHubModel(context);
      Date preAlertEnd = HubAlarmModel.getSecurityPreAlertEndTime(hub);

      TriggerInfo ti = getTriggerInfo(context);

      int idx = ti.getIndex(SecurityAlarm.NAME);
      List<IncidentTrigger> triggers = AlarmUtil.eventsToTriggers(AlarmModel.getTriggers(alertType, context.model(), ImmutableList.<Map<String,Object>>of()), idx);
      List<IncidentTrigger> filtered = new ArrayList<>(triggers.size());

      for(IncidentTrigger trigger : triggers) {
         if(trigger.getTime().equals(preAlertEnd) || trigger.getTime().before(preAlertEnd)) {
            filtered.add(trigger);
         } else {
            break;
         }
      }

      // the hub version of this method will only add the prealert tracker if the incident doesn't already have one
      if(!filtered.isEmpty()) {
         ti.setIndex(SecurityAlarm.NAME, idx + filtered.size());
         setTriggerInfo(context, ti);
         incidentService.addPreAlert(context, alertType, preAlertEnd, filtered);
         incidentService.updateIncident(context, filtered, sendNotifications);
         return true;
      }
      return false;
   }

   private boolean addPanicTriggerIfNecessary(SubsystemContext<AlarmSubsystemModel> context) {
      if(!AlarmModel.isAlertStateALERT(PanicAlarm.NAME, context.model())) {
         return false;
      }

      if(!AlarmModel.getTriggers(PanicAlarm.NAME, context.model(), ImmutableList.<Map<String, Object>>of()).isEmpty()) {
         return false;
      }
      
      context.logger().debug("Missing panic trigger from hub, attempting to restore");
      IncidentTrigger trigger = getPanicTrigger(context);
      if(trigger == null) {
         context.logger().warn("Missing panic trigger from subsystem, attributing panic to account owner");
         // uh... we need something
         trigger = new IncidentTrigger();
         trigger.setAlarm(PanicAlarm.NAME);
         trigger.setEvent(IncidentTrigger.EVENT_VERIFIED_ALARM);
         trigger.setSource(SubsystemUtils.getAccountOwnerAddress(context).getRepresentation());
         trigger.setTime(new Date());
      }
      AlarmModel.setTriggers(PanicAlarm.NAME, context.model(), ImmutableList.of(trigger.toMap()));
      return true;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Security Event Handlers
   /////////////////////////////////////////////////////////////////////////////

   @Request(value=AlarmSubsystemCapability.ArmRequest.NAME)
   public MessageBody arm(final SubsystemContext<AlarmSubsystemModel> context, final PlatformMessage request) {
      return doArm(context, request, AlarmSubsystemCapability.ArmRequest.getMode(request.getValue()), false, request.getActor());
   }

   @Request(value=AlarmSubsystemCapability.ArmBypassedRequest.NAME)
   public MessageBody armBypassed(SubsystemContext<AlarmSubsystemModel> context, PlatformMessage request) {
      return doArm(context, request, AlarmSubsystemCapability.ArmBypassedRequest.getMode(request.getValue()), true, request.getActor());
   }

   private MessageBody doArm(final SubsystemContext<AlarmSubsystemModel> context, final PlatformMessage request, String mode, boolean bypassed, Address actor) {
      AlarmUtil.assertActive(context);
      Model hub = AlarmUtil.getHubModel(context);
      if(hub == null) {
         return SecurityErrors.NO_HUB;
      }

      String incidentAddress = AlarmSubsystemModel.getCurrentIncident(context.model());
      if (isEmpty(incidentAddress))
      {
         incidentAddress = context.getVariable(VAR_CANCELINCIDENT).as(String.class);
      }
      if (!isEmpty(incidentAddress))
      {
         AlarmIncidentModel currentIncident = incidentService.getIncident(context, Address.fromString(incidentAddress));
         if (currentIncident != null && !currentIncident.isPlatformStateCOMPLETE())
         {
            return SecurityErrors.ARM_INVALID_CURRENT_INCIDENT;
         }
      }

      final Model securitySubsystem = context.models().getModelByAddress(Address.platformService(context.getPlaceId(), SecuritySubsystemCapability.NAMESPACE));
      final int configuredExitDelay = SecurityAlarmModeModel.getExitDelaySec(mode, securitySubsystem, 30);

      String by = null;
      if(actor != null && PersonCapability.NAMESPACE.equals(actor.getGroup())) {
         by = actor.getRepresentation();
         actor = request.getSource();
      }

      final KeyPad keypad = isDeviceAddress(actor) ? KeyPad.get(context, actor) : null;
      if(keypad != null) {
         actor = platToProtAddr(context, hub, actor);
      }

      MessageBody hrequest = HubAlarmCapability.ArmRequest.builder()
         .withActiveDevices(platToProtAddr(context, hub, ImmutableSet.copyOf(SecurityAlarmModeModel.getDevices(mode, securitySubsystem, ImmutableSet.<String>of()))))
         .withExitDelaySecs(configuredExitDelay)
         .withAlarmSensitivityDeviceCount(SecurityAlarmModeModel.getAlarmSensitivityDeviceCount(mode, securitySubsystem, 1))
         .withEntranceDelaySecs(SecurityAlarmModeModel.getEntranceDelaySec(mode, securitySubsystem, 30))
         .withSilent(AlarmModel.getSilent(SecurityAlarm.NAME, context.model()))
         .withSoundsEnabled(SecurityAlarmModeModel.getSoundsEnabled(mode, securitySubsystem, true))
         .withMode(mode)
         .withBypassed(bypassed)
         .withArmedFrom(actor == null ? null : actor.getRepresentation())
         .withArmedBy(by)
         .build();

      AlarmUtil.sendHubRequest(context, request, hrequest,
         new Function<PlatformMessage, MessageBody>() {
            @Override
            public MessageBody apply(PlatformMessage input) {
               Date reportedArmTime = HubAlarmCapability.ArmResponse.getSecurityArmTime(input.getValue());
               return AlarmSubsystemCapability.ArmResponse.builder()
                  .withDelaySec(calculateRemainingDelay(reportedArmTime, configuredExitDelay))
                  .build();
            }
         },
         new Predicate<PlatformMessage>() {
            @Override
            public boolean apply(PlatformMessage input) {
               MessageBody body = input.getValue();
               String errorCode = (String) body.getAttributes().get(ErrorEvent.CODE_ATTR);
               String msg = (String) body.getAttributes().get(ErrorEvent.MESSAGE_ATTR);
               if(SecurityErrors.CODE_TRIGGERED_DEVICES.equals(errorCode)) {
                  if(keypad != null) {
                     keypad.setBypassDelay(AlarmUtil.keypadArmBypassedTimeoutMs);
                  } else {
                     if(msg == null) {
                        // default handling, the error message as is will pass through
                        return false;
                     }

                     String[] protAddrs = msg.split(",");
                     Set<String> driverAddrs = new HashSet<>();
                     for(String protAddr : protAddrs) {
                        Model m = findDeviceByProtAddr(context, Address.fromString(protAddr));
                        if(m != null) {
                           driverAddrs.add(m.getAddress().getRepresentation());
                        }
                     }
                     AlarmUtil.sendResponse(context, request, Errors.fromCode(SecurityErrors.CODE_TRIGGERED_DEVICES, StringUtils.join(driverAddrs, ",")));
                  }
                  return true;
               }
               else if(SecurityErrors.CODE_ARM_INVALID.equals(errorCode)) {
                  MessageBody error = Errors.fromCode(SecurityHubDisarmingException.CODE_SECURITY_HUBDISARMING, msg);
                  AlarmUtil.sendResponse(context, request, error);
                  return true;
               }
               return false;
            }
         }
      );

      return MessageBody.noResponse();
   }

   private boolean isDeviceAddress(Address address) {
      return address instanceof DeviceDriverAddress;
   }

   private int calculateRemainingDelay(Date reportedTime, int configuredTime) {
      Date now = new Date();
      int diff = Math.abs((int) TimeUnit.MILLISECONDS.toSeconds(reportedTime.getTime() - now.getTime()));
      if(diff > configuredTime) {
         return configuredTime;
      }
      return diff;
   }

   private Set<String> platToProtAddr(SubsystemContext<AlarmSubsystemModel> context, Model hub, Set<String> platAddrs) {
      ImmutableSet.Builder<String> protAddr = new ImmutableSet.Builder<>();
      for(String platAddr : platAddrs) {
         Address protAddress = platToProtAddr(context, hub, Address.fromString(platAddr));
         if(protAddress == null) {
            context.logger().warn("Missing security device [{}]", platAddr);
            continue;
         }
         protAddr.add(protAddress.getRepresentation());
      }
      return protAddr.build();
   }

   private Address platToProtAddr(SubsystemContext<AlarmSubsystemModel> context, Model hub, Address platAddr) {
      Model m = context.models().getModelByAddress(platAddr);
      if(m == null) {
         return null;
      }
      ProtocolDeviceId id = ProtocolDeviceId.fromRepresentation(DeviceAdvancedModel.getProtocolid(m));
      String prot = DeviceAdvancedModel.getProtocol(m);
      return Address.hubProtocolAddress(hub.getId(), prot, id);
   }

   @OnMessage(types={ KeyPadCapability.ArmPressedEvent.NAME })
   public void armKeypad(SubsystemContext<AlarmSubsystemModel> context, PlatformMessage message) {
      if(!AlarmUtil.isActive(context)) {
         return;
      }

      MessageBody request = message.getValue();
      String mode = KeyPadCapability.ArmPressedEvent.getMode(request);
      KeyPad keypad = KeyPad.get(context, message.getSource());
      if(keypad.isBypassed()) {
         doArm(context, message, mode, true, message.getSource());
      } else {
         doArm(context, message, mode, false, message.getSource());
      }
   }

   private static final Function<Pair<AlarmIncidentModel, SubsystemContext<AlarmSubsystemModel>>, MessageBody> DISARM_RESPONSE_SUPPLIER = new Function<Pair<AlarmIncidentModel, SubsystemContext<AlarmSubsystemModel>>, MessageBody>() {
      @Override
      public MessageBody apply(Pair<AlarmIncidentModel, SubsystemContext<AlarmSubsystemModel>> input) {
         return AlarmSubsystemCapability.DisarmResponse.instance();
      }
   };

   @Request(value=AlarmSubsystemCapability.DisarmRequest.NAME)
   public MessageBody disarm(final SubsystemContext<AlarmSubsystemModel> context, final PlatformMessage request) {
      AlarmUtil.assertActive(context);
      doDisarm(context, request, DISARM_RESPONSE_SUPPLIER, AlarmService.CancelAlertRequest.METHOD_APP);
      return MessageBody.noResponse();
   }

   private static final Function<Pair<AlarmIncidentModel, SubsystemContext<AlarmSubsystemModel>>, MessageBody> KEYPAD_DISARM_RESPONSE_SUPPLIER = new Function<Pair<AlarmIncidentModel, SubsystemContext<AlarmSubsystemModel>>, MessageBody>() {
      @Override
      public MessageBody apply(Pair<AlarmIncidentModel, SubsystemContext<AlarmSubsystemModel>> input) {
         return MessageBody.noResponse();
      }
   };

   @OnMessage(types={ KeyPadCapability.DisarmPressedEvent.NAME })
   public void disarmKeypad(SubsystemContext<AlarmSubsystemModel> context, PlatformMessage message) {
      if(!AlarmUtil.isActive(context)) {
         return;
      }
      doDisarm(context, message, KEYPAD_DISARM_RESPONSE_SUPPLIER, AlarmService.CancelAlertRequest.METHOD_KEYPAD);
   }

   private void doDisarm(
         final SubsystemContext<AlarmSubsystemModel> context, 
         final PlatformMessage request, 
         final Function<Pair<AlarmIncidentModel, SubsystemContext<AlarmSubsystemModel>>, MessageBody> responseSupplier, 
         final String method
   ) {
      prepDisarm(context, request, AlarmSubsystemModel.getCurrentIncident(context.model()), method);

      MessageBody hrequest = buildDisarm(context);
      AlarmUtil.sendHubRequest(context, request, hrequest,
         new Function<PlatformMessage, MessageBody>() {
            @Override
            public MessageBody apply(PlatformMessage input) {
               context.model().setCurrentIncident("");
               context.model().setActiveAlerts(ImmutableList.<String>of());
               SubsystemUtils.clearTimeout(context, TO_PREALERTTIMER);
               AlarmIncidentModel incident = tryCancel(context);
               return responseSupplier.apply(new ImmutablePair<>(incident, context));
            }
         }
      );
      if(!context.model().getCurrentIncident("").isEmpty()) {
         context.logger().debug("Attempting to cancel active alert since a disarm request was received");
         tryCancel(context, true);
      }
   }

   private void sendDisarm(SubsystemContext<AlarmSubsystemModel> context, Model hub) {
      MessageBody hrequest = buildDisarm(context);
      context.send(hub.getAddress(), hrequest);
   }

   private MessageBody buildDisarm(SubsystemContext<AlarmSubsystemModel> context) {
      String by = context.getVariable(AlarmUtil.VAR_CANCELLEDBY).as(String.class);
      String actor = context.getVariable(AlarmUtil.VAR_CANCELACTOR).as(String.class);
      return HubAlarmCapability.DisarmRequest.builder()
            .withDisarmedBy(by)
            .withDisarmedFrom(actor)
            .build();
   }

   @Request(value=AlarmSubsystemCapability.PanicRequest.NAME)
   public MessageBody panic(final SubsystemContext<AlarmSubsystemModel> context, final PlatformMessage trigger) {
      AlarmUtil.assertActive(context);

      AlarmState.TriggerEvent triggerEvent;
      String source;
      switch(String.valueOf(trigger.getSource().getGroup())) {
         case RuleCapability.NAMESPACE:
            triggerEvent = AlarmState.TriggerEvent.RULE;
            source = trigger.getSource().getRepresentation();
            break;
         default:
            if(trigger.getActor() == null) {
               throw new ErrorEventException("panic.unavailable", "Unable to determine source of panic");
            }
            else {
               // not quite right, but close enough?
               triggerEvent = AlarmState.TriggerEvent.VERIFIED_ALARM;
               source = trigger.getActor().getRepresentation();
            }
            break;
      }

      IncidentTrigger t = new IncidentTrigger();
      t.setAlarm(PanicAlarm.NAME);
      t.setEvent(triggerEvent.name());
      t.setSource(source);
      t.setTime(new Date());
      setPanicTrigger(context, t);

      MessageBody hrequest = HubAlarmCapability.PanicRequest.builder()
         .withEvent(triggerEvent.name())
         .withSource(source)
         .build();

      AlarmUtil.sendHubRequest(context, trigger, hrequest, new Function<PlatformMessage, MessageBody>() {
         @Override
         public MessageBody apply(PlatformMessage input) {
            return AlarmSubsystemCapability.PanicResponse.instance();
         }
      });

      return MessageBody.noResponse();
   }

   /////////////////////////////////////////////////////////////
   // Backwards compatibility safety subsystem request handlers
   /////////////////////////////////////////////////////////////

   @Request(SafetySubsystemCapability.TriggerRequest.NAME)
   public MessageBody triggerV1(SubsystemContext<AlarmSubsystemModel> context) {
      return Errors.invalidRequest("subsafety:Trigger is no longer supported");
   }

   private static final Function<Pair<AlarmIncidentModel, SubsystemContext<AlarmSubsystemModel>>, MessageBody> SAFETY_RESPONSE_SUPPLIER = new Function<Pair<AlarmIncidentModel, SubsystemContext<AlarmSubsystemModel>>, MessageBody>() {
      @Override
      public MessageBody apply(Pair<AlarmIncidentModel, SubsystemContext<AlarmSubsystemModel>> input) {
         return SafetySubsystemCapability.ClearResponse.instance();
      }
   };

   @Request(SafetySubsystemCapability.ClearRequest.NAME)
   public void clearV1(SubsystemContext<AlarmSubsystemModel> context, PlatformMessage message) {
      doDisarm(context, message, SAFETY_RESPONSE_SUPPLIER, AlarmService.CancelAlertRequest.METHOD_APP);
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
      arm(context, message);
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

   private void prepDisarm(final SubsystemContext<AlarmSubsystemModel> context, @Nullable PlatformMessage request, String incidentAddr, String method) {
      String by = null;
      Address actor = request != null ? request.getActor() : null;
      if(actor != null && PersonCapability.NAMESPACE.equals(actor.getGroup())) {
         by = actor.getRepresentation();
         actor = request.getSource();
      }

      final KeyPad keypad = isDeviceAddress(actor) ? KeyPad.get(context, actor) : null;
      if(keypad != null) {
         actor = platToProtAddr(context, AlarmUtil.getHubModel(context), actor);
      }
      
      prepDisarm(context, actor != null ? actor.getRepresentation() : null, by, incidentAddr, method);
   }
   
   private void prepDisarm(SubsystemContext<AlarmSubsystemModel> context, String actor, String by, String incidentAddr, String method) {
      
      // used for disarm as well as cancelling an incident
      context.setVariable(AlarmUtil.VAR_CANCELLEDBY, by);
      context.setVariable(AlarmUtil.VAR_CANCELACTOR, actor);
      context.setVariable(AlarmUtil.VAR_CANCELMETHOD, method);
      
      if(StringUtils.isEmpty(incidentAddr)) {
         // this is just a plain disarm, no incident to cancel
         return;
      }
      
      prepClearing(context, incidentAddr);
   }
   
   private void prepClearing(SubsystemContext<AlarmSubsystemModel> context, String incidentAddr) {
      context.setVariable(VAR_CANCELINCIDENT, incidentAddr);
      
      context.model().setCurrentIncident("");
      context.model().setActiveAlerts(ImmutableList.<String>of());
      context.model().setAlarmState(AlarmSubsystemCapability.ALARMSTATE_CLEARING);
      SubsystemUtils.clearTimeout(context, TO_PREALERTTIMER);
   }
   
   private AlarmIncidentModel cancel(
         final SubsystemContext<AlarmSubsystemModel> context, 
         final PlatformMessage request, 
         final String method
   ) {
      prepDisarm(context, request, AlarmSubsystemModel.getCurrentIncident(context.model()), method);
      return tryCancel(context);
   }

   private AlarmIncidentModel tryCancel(final SubsystemContext<AlarmSubsystemModel> context) {
      return tryCancel(context, false);
   }

   private AlarmIncidentModel tryCancel(final SubsystemContext<AlarmSubsystemModel> context, boolean suppressDisarm) {
      SubsystemUtils.setTimeout(35000, context, AlarmUtil.TO_CANCEL);
      Address cancelledBy = context.getVariable(AlarmUtil.VAR_CANCELLEDBY).as(Address.class);
      String method = context.getVariable(AlarmUtil.VAR_CANCELMETHOD).as(String.class);
      String incidentAddr = context.getVariable(VAR_CANCELINCIDENT).as(String.class);

      Model hub = AlarmUtil.getHubModel(context, false);
      if(hub == null) {
         context.logger().warn("Unable to disarm / clear hub incident because no hub model is present", new RuntimeException("Missing hub for hub alarm"));
      }
      else {
         String incidentId = HubAlarmModel.getCurrentIncident(hub, "");
         if(!incidentId.isEmpty()) {
            incidentAddr = Address.platformService(incidentId, AlarmIncidentCapability.NAMESPACE).getRepresentation();
         }
         if(AlarmUtil.isHubAlerting(hub) && !suppressDisarm) {
            context.logger().debug("Attempting to disarm hub because incident cancellation was requested");
            sendDisarm(context, hub);
         }
      }
      
      AlarmIncidentModel incident;
      if(StringUtils.isEmpty(incidentAddr)) {
         context.logger().warn("Unable to cancel incident because there is no incident -- will attempt to clear out any current incidents");
         // fallback to just cancelling the alarm platform side if there is an active one
         incident = incidentService.cancel(context, cancelledBy, method);
      }
      else {
         incident = incidentService.cancel(context, incidentAddr, cancelledBy, method);
      }
      if((incident == null || incident.isPlatformStateCOMPLETE()) && AlarmUtil.isPendingClear(hub)) {
         context.logger().debug("Attempting to clear hub incident because incident cancellation was requested:  hub state = {}, hub incident = {}", HubAlarmModel.getAlarmState(hub), HubAlarmModel.getCurrentIncident(hub));
         context.send(hub.getAddress(), HubAlarmCapability.ClearIncidentRequest.instance());
      }
      else if((incident == null || incident.isAlertStateCOMPLETE()) && !AlarmUtil.isHubIncidentActive(hub)) {
         context.logger().debug("Incident is complete and hub is disarmed or missing, completing incident");
         onCompleted(context);
      }
      return incident;
   }

   private void onCompleted(final SubsystemContext<AlarmSubsystemModel> context) {
      clearTriggerInfo(context);
      clearCancellation(context);
   }

   private void clearCancellation(SubsystemContext<AlarmSubsystemModel> context) {
      context.setVariable(AlarmUtil.VAR_CANCELLEDBY, null);
      context.setVariable(AlarmUtil.VAR_CANCELACTOR, null);
      context.setVariable(AlarmUtil.VAR_CANCELMETHOD, null);
      context.setVariable(VAR_CANCELINCIDENT, null);
      SubsystemUtils.clearTimeout(context, AlarmUtil.TO_CANCEL);
   }

   void assertCanSwitchTo(SubsystemContext<AlarmSubsystemModel> context) {
      final Model securitySubsystem = context.models().getModelByAddress(Address.platformService(context.getPlaceId(), SecuritySubsystemCapability.NAMESPACE));
      final Model safetySubsystem = context.models().getModelByAddress(Address.platformService(context.getPlaceId(), SafetySubsystemCapability.NAMESPACE));

      Iterable<Model> devices = context.models().getModelsByType(DeviceCapability.NAMESPACE);
      for(Model m : devices) {
         if(isParticipating(m, securitySubsystem, safetySubsystem) && !DeviceAdvancedModel.getHubLocal(m, false)) {
            throw new ErrorEventException(AlarmErrors.NONLOCALDRIVER);
         }
      }
   }

   private boolean isParticipating(Model dev, Model security, Model safety) {
      String addr = dev.getAddress().getRepresentation();
      return
         dev.supports(KeyPadCapability.NAMESPACE) ||
         containsIfNotNull(SafetySubsystemModel.getActiveDevices(safety), addr) ||
         containsIfNotNull(SecurityAlarmModeModel.getDevices(AlarmSubsystemCapability.SECURITYMODE_ON, security), addr) ||
         containsIfNotNull(SecurityAlarmModeModel.getDevices(AlarmSubsystemCapability.SECURITYMODE_PARTIAL, security), addr);
   }
   
   private boolean containsIfNotNull(Set<String> curSet, String target) {
   	if(curSet != null) {
   		return curSet.contains(target);
   	}else{
   		return false;
   	}
   }

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
      syncMonitored(context);
   }

   private void syncMonitored(SubsystemContext<AlarmSubsystemModel> context) {
      Model m = context.models().getModelByAddress(Address.platformService(context.getPlaceId(), PlaceCapability.NAMESPACE));
      ServiceLevel sl = ServiceLevel.fromString(PlaceModel.getServiceLevel(m));
      AlarmModel.setMonitored(SecurityAlarm.NAME, context.model(), sl.isPromon());
      AlarmModel.setMonitored(SmokeAlarm.NAME, context.model(), sl.isPromon());
      AlarmModel.setMonitored(CarbonMonoxideAlarm.NAME, context.model(), sl.isPromon());
      AlarmModel.setMonitored(PanicAlarm.NAME, context.model(), sl.isPromon());
   }

   TriggerInfo getTriggerInfo(SubsystemContext<AlarmSubsystemModel> context) {
      TriggerInfo info = context.getVariable(VAR_TRIGGERINFO).as(TriggerInfo.class);
      String incidentAddress = AlarmSubsystemModel.getCurrentIncident(context.model(), "");
      if(incidentAddress.isEmpty()) {
         incidentAddress = context.getVariable(VAR_CANCELINCIDENT).as(String.class);
      }
      if(info == null || !StringUtils.equals(incidentAddress, info.incidentAddress)) {
         info = new TriggerInfo();
         info.incidentAddress = incidentAddress;
      }
      return info;
   }

   void clearTriggerInfo(SubsystemContext<AlarmSubsystemModel> context) {
      setTriggerInfo(context, null);
      clearPanicTrigger(context);
   }

   void setTriggerInfo(SubsystemContext<AlarmSubsystemModel> context, @Nullable TriggerInfo info) {
      context.setVariable(VAR_TRIGGERINFO, info);
   }
   
   @SuppressWarnings("unchecked")
   IncidentTrigger getPanicTrigger(SubsystemContext<AlarmSubsystemModel> context) {
      Map<String, Object> trigger = context.getVariable(VAR_PANIC_TRIGGER).as(Map.class);
      return trigger != null ? new IncidentTrigger(trigger) : null;
   }
   
   void setPanicTrigger(SubsystemContext<AlarmSubsystemModel> context, IncidentTrigger trigger) {
      context.setVariable(VAR_PANIC_TRIGGER, trigger.toMap());
   }
   
   void clearPanicTrigger(SubsystemContext<AlarmSubsystemModel> context) {
      context.setVariable(VAR_PANIC_TRIGGER, null);
   }
   
   private void syncAlarmProviderIfNecessary(SubsystemContext<AlarmSubsystemModel> context, boolean checkHubOnline, Model hub, CheckSecurityModeOption checkSecurityMode) {   	   	
   	AlarmUtil.syncAlarmProviderIfNecessary(context, checkHubOnline, hub, checkSecurityMode);
	}


   static class TriggerInfo {
      private String incidentAddress;
      private int smoke = 0;
      private int co = 0;
      private int security = 0;
      private int panic = 0;
      private int water = 0;

      public boolean isSignalled(String alarmType) {
         return getIndex(alarmType) > 0;
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

            @Override
            public String toString() {
                  return "TriggerInfo [incidentAddress=" + incidentAddress + ", smoke=" + smoke + ", co=" + co + ", security="
                              + security + ", panic=" + panic + ", water=" + water + "]";
            }

   }
}

