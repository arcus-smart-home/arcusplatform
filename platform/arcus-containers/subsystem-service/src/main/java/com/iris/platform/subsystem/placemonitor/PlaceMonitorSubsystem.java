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
/**
 *
 */
package com.iris.platform.subsystem.placemonitor;

import static com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts.CONTEXT_ATTR_DEVICENAME;
import static com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts.CONTEXT_ATTR_KEY;
import static com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts.CONTEXT_ATTR_SEVERITY;
import static com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts.CONTEXT_ATTR_TIME;
import static com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlerts.TEMPLATE_NAME;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.annotation.Version;
import com.iris.common.subsystem.BaseSubsystem;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.common.subsystem.annotation.AnnotatedSubsystemFactory;
import com.iris.common.subsystem.annotation.Subsystem;
import com.iris.common.subsystem.event.SubsystemEventAndContext;
import com.iris.core.template.TemplateService;
import com.iris.io.json.JSON;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.DeviceOtaCapability;
import com.iris.messages.capability.DevicePowerCapability;
import com.iris.messages.capability.HubConnectionCapability;
import com.iris.messages.capability.HubPowerCapability;
import com.iris.messages.capability.HubReflexCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.PlaceMonitorSubsystemCapability;
import com.iris.messages.capability.RuleTemplateCapability;
import com.iris.messages.event.ListenerList;
import com.iris.messages.event.MessageReceivedEvent;
import com.iris.messages.event.ModelChangedEvent;
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
import com.iris.messages.model.dev.DeviceModel;
import com.iris.messages.model.hub.HubModel;
import com.iris.messages.model.hub.HubPowerModel;
import com.iris.messages.model.serv.PlaceModel;
import com.iris.messages.model.subs.PlaceMonitorSubsystemModel;
import com.iris.messages.type.SmartHomeAlert;
import com.iris.util.TypeMarker;

/**
 *
 */
@Singleton
@Subsystem(PlaceMonitorSubsystemModel.class)
@Version(1)
public class PlaceMonitorSubsystem extends BaseSubsystem<PlaceMonitorSubsystemModel> {

   Map<String, PlaceMonitorHandler> handlers;
   PlaceMonitorNotifications notifier;
   Set<ListenerList<SubsystemEventAndContext>> valueChangeListeners;
   Set<ListenerList<SubsystemEventAndContext>> messageListeners;
   private final TemplateService templateService;

   @Inject
   public PlaceMonitorSubsystem(
      Map<String, PlaceMonitorHandler> handlers,
      PlaceMonitorNotifications notifier,
      TemplateService templateService
   ) {
      super();
      this.handlers = handlers;
      this.notifier = notifier;
      createListeners();
      this.templateService = templateService;
   }
   
   private void createListeners() {
      this.valueChangeListeners=new HashSet<ListenerList<SubsystemEventAndContext>>();
      this.messageListeners=new HashSet<ListenerList<SubsystemEventAndContext>>();
      if (this.handlers != null && this.handlers.size() > 0) {
         for (PlaceMonitorHandler handle : this.handlers.values()) {
            addValueChangeListeners(handle);
            addMessageListeners(handle);
         }
      }
   }
   
   /**
    * Search the handler for anything annotated with onValueChange.  If the handler contains an annotated method, add it to 
    * the list of handlers that will be notified when a value change event occurs.
    * 
    * @param handle - an PlaceMonitorHandler that handles tasks
    */
   private void addValueChangeListeners(PlaceMonitorHandler handle) {
      ListenerList<SubsystemEventAndContext> listeners = AnnotatedSubsystemFactory.createValueChangedListeners(handle);
      if (listeners.hasListeners()) {
         this.valueChangeListeners.add(listeners);
      }
   }
   
   /**
    * Search the handler for anything annotated with onMessage.  If the handler contains an annotated method, add it to 
    * the list of handlers that will be notified when a message is received.
    * 
    * @param handle - an PlaceMonitorHandler that handles tasks
    */
   private void addMessageListeners(PlaceMonitorHandler handle) {
      ListenerList<SubsystemEventAndContext> listeners = AnnotatedSubsystemFactory.createMessageReceivedListeners(handle);
      if (listeners.hasListeners()) {
         this.messageListeners.add(listeners);
      }
   }

   @Override
   protected void onAdded(SubsystemContext<PlaceMonitorSubsystemModel> context) {
      super.onAdded(context);
      forAllHandlers(context, handler -> handler.onAdded(context));
   }

   @Override
   protected void onStarted(SubsystemContext<PlaceMonitorSubsystemModel> context) {
      super.onStarted(context);
      forAllHandlers(context, handler -> handler.onStarted(context));
   }

   @OnAdded(query = "base:caps contains '" + DeviceCapability.NAMESPACE + "'")
   public void onDeviceAdded(Model model, SubsystemContext<PlaceMonitorSubsystemModel> context) {
      forAllHandlers(context, handler -> handler.onDeviceAdded(model, context));
   }

   @OnRemoved(query = "base:caps contains '" + DeviceCapability.NAMESPACE + "'")
   public void onDeviceRemoved(Model model, SubsystemContext<PlaceMonitorSubsystemModel> context) {
      forAllHandlers(context, handler -> handler.onDeviceRemoved(model, context));
   }

   @OnScheduledEvent
   public void onScheduledEvent(ScheduledEvent event, SubsystemContext<PlaceMonitorSubsystemModel> context) {
      context.logger().debug("Place Monitor received an onScheduledEvent for event [{}]", event);
      forAllHandlers(context, handler -> handler.onScheduledEvent(event, context));
   }

   @OnRemoved(query = "base:caps contains '" + PersonCapability.NAMESPACE + "'")
   public void onPersonRemoved(Model model, SubsystemContext<PlaceMonitorSubsystemModel> context) {
      context.logger().debug("onPersonRemoved for [{}]", model.getId());
      sendPinChangedSync(context);
   }

   @OnMessage(types = PersonCapability.PinChangedEventEvent.NAME)
   public void onPinChangedEventEvent(SubsystemContext<PlaceMonitorSubsystemModel> context, PlatformMessage response) {
      context.logger().debug("onPinChangedEventEvent for [{}]", response.getSource());
      sendPinChangedSync(context);
   }

   protected void sendPinChangedSync(SubsystemContext<PlaceMonitorSubsystemModel> context) {
      MessageBody messageBody = HubReflexCapability.SyncNeededEvent.instance();
      SubsystemUtils.sendToHub(context, messageBody);
   }

   //Not using yet.
   @OnMessage(types = DeviceOtaCapability.FirmwareUpdateResponse.NAME)
   public void onFirmwareUpdateResponse(SubsystemContext<PlaceMonitorSubsystemModel> context, PlatformMessage response) {
      String status = DeviceOtaCapability.FirmwareUpdateResponse.getStatus(response.getValue());
      String message = DeviceOtaCapability.FirmwareUpdateResponse.getMessage(response.getValue());
      context.logger().debug("onFirmwareUpdateResponse [{} {} {} ]", response.getSource(), status, message);
   }

   // Not getting the response for some reason
   @OnMessage(types = RuleTemplateCapability.CreateRuleResponse.NAME)
   public void onCrateRuleResponse(SubsystemContext<PlaceMonitorSubsystemModel> context, PlatformMessage response) {
      context.logger().debug("onCreateRuleResponse [{} {} {} ]", response.getSource());
   }

   @OnValueChanged(attributes = { DeviceConnectionCapability.ATTR_STATE })
   public void onConnectivityChange(Model model, SubsystemContext<PlaceMonitorSubsystemModel> context) {
      context.logger().debug("onConnectivityChange [{}]", model);
      forAllHandlers(context, handler -> handler.onConnectivityChange(model, context));
   }

   @OnValueChanged(attributes = { HubPowerCapability.ATTR_SOURCE })
   public void onHubPowerChange(Model model, SubsystemContext<PlaceMonitorSubsystemModel> context) {
      Model place = Iterables.getFirst(context.models().getModelsByType(PlaceCapability.NAMESPACE),null);
      switch(HubPowerModel.getSource(model)){
         case HubPowerCapability.SOURCE_BATTERY:
            notifier.sendHubWentToBatteryBackup(HubModel.getName(model), PlaceModel.getName(place), context);
            break;
         case HubPowerCapability.SOURCE_MAINS:
            notifier.sendHubWentToMainPower(HubModel.getName(model), PlaceModel.getName(place), context);
            break;
      }
      context.logger().debug("hub power change [{}]", model);
   }

   @OnValueChanged(attributes = { DevicePowerCapability.ATTR_BATTERY })
   public void onDevicePowerChange(Model device, SubsystemContext<PlaceMonitorSubsystemModel> context) {
      context.logger().debug("device power change [{}]", device);
      forAllHandlers(context, handler -> handler.onDeviceBatteryChange(device, context));
   }

   @OnValueChanged(attributes = { HubConnectionCapability.ATTR_STATE })
   public void onHubConnectivity(Model hub, SubsystemContext<PlaceMonitorSubsystemModel> context) {
      context.logger().debug("hub connection change [{}]", hub);
      forAllHandlers(context, handler -> handler.onHubConnectivityChange(hub, context));
   }

   @OnReport(query = "base:caps contains 'hubalarm'")
   public void onHubReport(ModelReportEvent event, Model hub, SubsystemContext<PlaceMonitorSubsystemModel> context) {
      context.logger().debug("hub report event [{}] for hub [{}]", event, hub);
      forAllHandlers(context, handler -> handler.onHubReportEvent(event, hub, context));
   }

   @OnValueChanged
   public void onValueChange(Model model, ModelChangedEvent event, SubsystemContext<PlaceMonitorSubsystemModel> context) {
      context.logger().trace("all value changes {}", model);
      valueChangeListeners.forEach(listenerList -> listenerList.fireEvent(new SubsystemEventAndContext(context, event)));
   }
   
   @OnMessage
   public void onMessageReceived(MessageReceivedEvent event, SubsystemContext<PlaceMonitorSubsystemModel> context) {
      context.logger().trace("all messages recieved {}", event.getMessage());
      this.messageListeners.forEach(listenerList -> listenerList.fireEvent(new SubsystemEventAndContext(context, event)));
   }

   private void forAllHandlers(SubsystemContext<PlaceMonitorSubsystemModel> context, Consumer<PlaceMonitorHandler> consumer) {
      for (PlaceMonitorHandler handler : handlers.values()) {
         try {
            consumer.accept(handler);
         } catch (Exception e) {
            context.logger().error("Exception in PlaceMonitorHandler execution " + handler.getClass().getName(), e);
         }
      }
   }

   // smart home alert integration
   @Request(value = PlaceMonitorSubsystemCapability.RenderAlertsRequest.NAME)
   public MessageBody renderAlerts(SubsystemContext<PlaceMonitorSubsystemModel> context) {
      List<Map<String, Object>> alerts = context.model().getSmartHomeAlerts();
      if(alerts == null) {
         alerts = ImmutableList.of();
      }
      List<Map<String, Object>> rendered = alerts.stream()
         .map(SmartHomeAlert::new).map(alert -> {
            Map<String, Object> tmplContext = createTemplateContext(context, alert);
            String json = templateService.render(TEMPLATE_NAME, tmplContext);  // alert.hbs is the container template.  It loads a partial based on the alert type.
            return JSON.fromJson(json, new TypeMarker<Map<String, Object>>() {});
         })
         .collect(Collectors.toList());

      return PlaceMonitorSubsystemCapability.RenderAlertsResponse.builder().withAlerts(rendered).build();
   }

   private Map<String, Object> createTemplateContext(SubsystemContext<PlaceMonitorSubsystemModel> context, SmartHomeAlert alert) {
      Map<String, Object> attrs = new HashMap<>(alert.getAttributes());
      if(alert.getSubjectaddr().startsWith("DRIV:dev")) {
         Model m = context.models().getModelByAddress(Address.fromString(alert.getSubjectaddr()));
         if(m != null) {
            // if the model has no name fallback to the name at the time the alert was generated
            attrs.put(CONTEXT_ATTR_DEVICENAME, DeviceModel.getName(m, (String) attrs.getOrDefault(CONTEXT_ATTR_DEVICENAME, "")));
         }
         // fall back to the name at the time the alert was created
      }
      return ImmutableMap.<String, Object>builder()
         .putAll(attrs)
         .put(CONTEXT_ATTR_KEY, alert.getAlerttype())
         .put(CONTEXT_ATTR_TIME, alert.getCreated())
         .put(CONTEXT_ATTR_SEVERITY, alert.getSeverity())
         .build();
   }
}

