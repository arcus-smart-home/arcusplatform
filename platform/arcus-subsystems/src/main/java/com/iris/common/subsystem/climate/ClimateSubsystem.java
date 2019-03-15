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
package com.iris.common.subsystem.climate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.annotation.Version;
import com.iris.common.subsystem.BaseSubsystem;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.annotation.Subsystem;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.ClimateSubsystemCapability;
import com.iris.messages.capability.ClimateSubsystemCapability.DisableSchedulerResponse;
import com.iris.messages.capability.ClimateSubsystemCapability.EnableSchedulerResponse;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.FanCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.RelativeHumidityCapability;
import com.iris.messages.capability.SchedulableCapability;
import com.iris.messages.capability.ScheduleCapability;
import com.iris.messages.capability.SchedulerCapability;
import com.iris.messages.capability.SpaceHeaterCapability;
import com.iris.messages.capability.SwitchCapability;
import com.iris.messages.capability.TemperatureCapability;
import com.iris.messages.capability.ThermostatCapability;
import com.iris.messages.capability.VentCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.event.ModelAddedEvent;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.event.ModelRemovedEvent;
import com.iris.messages.listener.annotation.OnAdded;
import com.iris.messages.listener.annotation.OnMessage;
import com.iris.messages.listener.annotation.OnRemoved;
import com.iris.messages.listener.annotation.OnValueChanged;
import com.iris.messages.listener.annotation.Request;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.DeviceConnectionModel;
import com.iris.messages.model.dev.RelativeHumidityModel;
import com.iris.messages.model.dev.SchedulableModel;
import com.iris.messages.model.dev.TemperatureModel;
import com.iris.messages.model.dev.ThermostatModel;
import com.iris.messages.model.serv.PlaceModel;
import com.iris.messages.model.subs.ClimateSubsystemModel;
import com.iris.messages.service.SchedulerService;
import com.iris.messages.type.ThermostatScheduleStatus;
import com.iris.messages.type.TimeOfDayCommand;
import com.iris.type.LooselyTypedReference;
import com.iris.util.TypeMarker;

/**
 *
 */
@Singleton
@Subsystem(ClimateSubsystemModel.class)
@Version(1)
public class ClimateSubsystem extends BaseSubsystem<ClimateSubsystemModel> {
   // TODO try to choose a thermostat unless the user specifically chose something different
   private static final String VAR_USER_SELECTED_TEMPERATURE = "climate.thermostat.selected";
   private static final String VAR_USER_SELECTED_HUMIDITY = "climate.humidity.selected";
   private static final String VAR_SCHEDADRESS = "thermsched:";

   private static final Address SCHEDULER_ADDRESS = Address.platformService(SchedulerService.NAMESPACE);
   private static final String THERMOSTAT_SCHEDULE_GROUP = "temperature";

   private static final Set<String> DAYS =
         ImmutableSet.<String>of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN");

   private static final String MORNING_TIME = "6:00:00";
   private static final String DAY_TIME = "8:00:00";
   private static final String EVENING_TIME = "18:00:00";
   private static final String NIGHT_TIME = "22:00:00";

   private static final double MORNING_HEATPOINT = 21.1;
   private static final double DAY_HEATPOINT = 18.9;
   private static final double EVENING_HEATPOINT = 21.1;
   private static final double NIGHT_HEATPOINT = 18.9;

   private static final double MORNING_COOLPOINT = 25.6;
   private static final double DAY_COOLPOINT = 27.8;
   private static final double EVENING_COOLPOINT = 25.6;
   private static final double NIGHT_COOLPOINT = 26.7;

   private static final List<Map<String, Object>> DEFAULT_SCHEDULE =
         ImmutableList
            .<Map<String, Object>>builder()
            .add(autoSetPoint(MORNING_TIME, MORNING_HEATPOINT, MORNING_COOLPOINT))
            .add(autoSetPoint(DAY_TIME, DAY_HEATPOINT, DAY_COOLPOINT))
            .add(autoSetPoint(EVENING_TIME, EVENING_HEATPOINT, EVENING_COOLPOINT))
            .add(autoSetPoint(NIGHT_TIME, NIGHT_HEATPOINT, NIGHT_COOLPOINT))
            .add(heatSetPoint(MORNING_TIME, MORNING_HEATPOINT))
            .add(heatSetPoint(DAY_TIME, DAY_HEATPOINT))
            .add(heatSetPoint(EVENING_TIME, EVENING_HEATPOINT))
            .add(heatSetPoint(NIGHT_TIME, NIGHT_HEATPOINT))
            .add(coolSetPoint(MORNING_TIME, MORNING_COOLPOINT))
            .add(coolSetPoint(DAY_TIME, DAY_COOLPOINT))
            .add(coolSetPoint(EVENING_TIME, EVENING_COOLPOINT))
            .add(coolSetPoint(NIGHT_TIME, NIGHT_COOLPOINT))
            .build();

   private static final List<Map<String, Object>> EMPTY_SCHEDULE =
         ImmutableList
            .<Map<String, Object>>builder()
            .add(ImmutableMap.<String,Object>of(
                  TimeOfDayCommand.ATTR_SCHEDULEID, ThermostatCapability.HVACMODE_AUTO,
                  TimeOfDayCommand.ATTR_TIME, MORNING_TIME,
                  TimeOfDayCommand.ATTR_DAYS, DAYS))
             .add(ImmutableMap.<String,Object>of(
                  TimeOfDayCommand.ATTR_SCHEDULEID, ThermostatCapability.HVACMODE_COOL,
                  TimeOfDayCommand.ATTR_TIME, MORNING_TIME,
                  TimeOfDayCommand.ATTR_DAYS, DAYS))
             .add(ImmutableMap.<String,Object>of(
                  TimeOfDayCommand.ATTR_SCHEDULEID, ThermostatCapability.HVACMODE_HEAT,
                  TimeOfDayCommand.ATTR_TIME, MORNING_TIME,
                  TimeOfDayCommand.ATTR_DAYS, DAYS))
            .build();

   private static Map<String, Object> autoSetPoint(String timeOfDay, double heatpoint, double coolpoint) {
      return ImmutableMap
            .of(
                  TimeOfDayCommand.ATTR_SCHEDULEID, ThermostatCapability.HVACMODE_AUTO,
                  TimeOfDayCommand.ATTR_TIME, timeOfDay,
                  TimeOfDayCommand.ATTR_DAYS, DAYS,
                  TimeOfDayCommand.ATTR_ATTRIBUTES, ImmutableMap.of(
                        ThermostatCapability.ATTR_HEATSETPOINT, heatpoint,
                        ThermostatCapability.ATTR_COOLSETPOINT, coolpoint
                  )
            );
   }

   private static Map<String, Object> heatSetPoint(String timeOfDay, double heatpoint) {
      return ImmutableMap
            .of(
                  TimeOfDayCommand.ATTR_SCHEDULEID, ThermostatCapability.HVACMODE_HEAT,
                  TimeOfDayCommand.ATTR_TIME, timeOfDay,
                  TimeOfDayCommand.ATTR_DAYS, DAYS,
                  TimeOfDayCommand.ATTR_ATTRIBUTES, ImmutableMap.of(
                        ThermostatCapability.ATTR_HEATSETPOINT, heatpoint
                  )
            );
   }

   private static Map<String, Object> coolSetPoint(String timeOfDay, double coolpoint) {
      return ImmutableMap
            .of(
                  TimeOfDayCommand.ATTR_SCHEDULEID, ThermostatCapability.HVACMODE_COOL,
                  TimeOfDayCommand.ATTR_TIME, timeOfDay,
                  TimeOfDayCommand.ATTR_DAYS, DAYS,
                  TimeOfDayCommand.ATTR_ATTRIBUTES, ImmutableMap.of(
                        ThermostatCapability.ATTR_COOLSETPOINT, coolpoint
                  )
            );
   }

   /**
    *
    */
   public ClimateSubsystem() {
   }

   /* (non-Javadoc)
    * @see com.iris.common.subsystem.BaseSubsystem#onStarted(com.iris.common.subsystem.SubsystemContext)
    */
   @Override
   protected void onStarted(SubsystemContext<ClimateSubsystemModel> context) {
      initializeAttributes(context.model());
      syncDevices(context);
   }

   @OnAdded(query=ClimateSubsystemUtils.IS_CLIMATE_DEVICE)
   public void onDeviceAdded(ModelAddedEvent event, SubsystemContext<ClimateSubsystemModel> context) {
      context.logger().debug("Discovered new climate device: [{}]", event.getAddress());
      syncDevices(context);
   }

   @OnRemoved(query=ClimateSubsystemUtils.IS_CLIMATE_DEVICE)
   public void onDeviceRemoved(ModelRemovedEvent event, SubsystemContext<ClimateSubsystemModel> context) {
      context.logger().debug("Climate device was removed: [{}]", event.getAddress());
      syncDevices(context);
   }

   @OnValueChanged(attributes=Capability.ATTR_CAPS)
   public void onDeviceCapabilitiesChanged(ModelChangedEvent event, SubsystemContext<ClimateSubsystemModel> context) {
      // don't really know if this is or was a climate device, and we could check, but this should happen very rarely, so sync just in case
      syncDevices(context);
   }

   @OnValueChanged(attributes=VentCapability.ATTR_LEVEL)
   public void onVentOpenedOrClosed(ModelChangedEvent event, SubsystemContext<ClimateSubsystemModel> context) {
      Model m = context.models().getModelByAddress(event.getAddress());
      if(m == null) {
         context.logger().warn("Received vent level change for untracked model {}", event.getAddress());
         return; // weird
      }
      updateVent(m, context);
   }
   @OnValueChanged(attributes={FanCapability.ATTR_SPEED,SwitchCapability.ATTR_STATE})
   public void onFanSpeedChanged(ModelChangedEvent event, SubsystemContext<ClimateSubsystemModel> context) {
      Model m = context.models().getModelByAddress(event.getAddress());
      if(m == null) {
         context.logger().warn("Received vent level change for untracked model {}", event.getAddress());
         return; // weird
      }
      if(m.supports(FanCapability.NAMESPACE)){
         updateFan(m, context);
      }
   }
   
   @OnValueChanged(attributes={SpaceHeaterCapability.ATTR_HEATSTATE})
   public void onHeaterStateChanged(ModelChangedEvent event, SubsystemContext<ClimateSubsystemModel> context) {
      Model m = context.models().getModelByAddress(event.getAddress());
      if(m == null) {
         context.logger().warn("Received heat state change for untracked model {}", event.getAddress());
         return; // weird
      }
      if(m.supports(SpaceHeaterCapability.NAMESPACE)){
         updateHeater(m, context);
      }
   }

   @OnValueChanged(attributes=ThermostatCapability.ATTR_HVACMODE)
   public void onThermostatModeChanged(ModelChangedEvent event, SubsystemContext<ClimateSubsystemModel> context) {
      syncThermostatMode(event.getAddress(), context);
   }

   @OnValueChanged(attributes=SchedulableCapability.ATTR_SCHEDULEENABLED)
   public void onScheduleEnableChange(ModelChangedEvent event, SubsystemContext<ClimateSubsystemModel> context) {
      Map<String, Object> attributes = context.model().getThermostatSchedules().get(event.getAddress().getRepresentation());
      if(attributes == null) {
         throw new ErrorEventException("thermostat.notfound", "No thermostat with address [" + event.getAddress() + "]");
      }
      if(!isTimezoneSet(context)) {
         throw new ErrorEventException("timezone.notset", "Can't enable scheduling with no timezone set.");
      }
      ThermostatScheduleStatus status = new ThermostatScheduleStatus(attributes);
      status.setEnabled((Boolean) event.getAttributeValue());
      putInMap(
            context.model(),
            ClimateSubsystemCapability.ATTR_THERMOSTATSCHEDULES,
            event.getAddress().getRepresentation(),
            status.toMap()
      );
      syncThermostatMode(event.getAddress(), context);
   }
   
   @OnValueChanged(attributes=SchedulableCapability.ATTR_TYPE)
   public void onScheduleTypeChange(ModelChangedEvent event, SubsystemContext<ClimateSubsystemModel> context) {
      Map<String, Object> attributes = context.model().getThermostatSchedules().get(event.getAddress().getRepresentation());
      if(attributes == null) {
         throw new ErrorEventException("thermostat.notfound", "No thermostat with address [" + event.getAddress() + "]");
      }
      if(!isTimezoneSet(context)) {
         throw new ErrorEventException("timezone.notset", "Can't enable scheduling with no timezone set.");
      }
      ThermostatScheduleStatus status = new ThermostatScheduleStatus(attributes);
      String newTypeValue = (String) event.getAttributeValue();
      if(SchedulableCapability.TYPE_NOT_SUPPORTED.equals(newTypeValue) && StringUtils.isNotBlank(status.getScheduler())) {
      	//type updated to NOT_SUPPORTED, and there is an existing scheduler.  So need to remove the scheduler
      	String oldSchedulerAddress = status.getScheduler();
      	status.setScheduler(null);
      	MessageBody message = SchedulerCapability.DeleteRequest.instance();               
      	context.request(Address.fromString(oldSchedulerAddress), message);
      }else{
      	if(StringUtils.isBlank(status.getScheduler())) {
      		//type is updated to anything other than NOT_SUPPORTED, and there is no existing scheduler.  So need to create one
      		Model thermostatModel = context.models().getModelByAddress(Address.fromString(status.getThermostat()));
      		sendCreateScheduleRequest(context, status.getThermostat(), ClimateSubsystemUtils.isSchedulable(thermostatModel), newTypeValue);
      	}
      }
      
      putInMap(
            context.model(),
            ClimateSubsystemCapability.ATTR_THERMOSTATSCHEDULES,
            event.getAddress().getRepresentation(),
            status.toMap()
      );
      
   }

   @OnValueChanged(attributes={ DeviceConnectionCapability.ATTR_STATE, TemperatureCapability.ATTR_TEMPERATURE })
   public void onTemperatureChanged(ModelChangedEvent event, SubsystemContext<ClimateSubsystemModel> context) {
      if(event.getAddress().getRepresentation().equals(context.model().getPrimaryTemperatureDevice())) {
         syncTemperature(context);
      }
   }

   @OnValueChanged(attributes={ DeviceConnectionCapability.ATTR_STATE, RelativeHumidityCapability.ATTR_HUMIDITY })
   public void onHumidityChanged(ModelChangedEvent event, SubsystemContext<ClimateSubsystemModel> context) {
      if(event.getAddress().getRepresentation().equals(context.model().getPrimaryTemperatureDevice())) {
         syncHumidity(context);
      }
   }
   
   

   @OnMessage(types=SchedulerService.ScheduleCommandsResponse.NAME)
   public void onSchedulerResponse(SubsystemContext<ClimateSubsystemModel> context, PlatformMessage response) {
      MessageBody m = response.getValue();
      String schedulerAddress = SchedulerService.ScheduleWeeklyCommandResponse.getSchedulerAddress(m);
      String thermostatAddress = context.getVariable(response.getCorrelationId()).as(String.class);
      if(thermostatAddress == null) {
         return;
      }
      context.setVariable(response.getCorrelationId(), null);

      context.logger().info("Created schedule [{}] for thermostat [{}]", schedulerAddress, thermostatAddress);
      ThermostatScheduleStatus status = new ThermostatScheduleStatus(context.model().getThermostatSchedules().get(thermostatAddress));
      status.setScheduler(schedulerAddress);
      putInMap(context.model(), ClimateSubsystemCapability.ATTR_THERMOSTATSCHEDULES, thermostatAddress, status.toMap());

      syncThermostatMode(Address.fromString(thermostatAddress), context);
   }
   
   @OnAdded(query=ClimateSubsystemUtils.IS_SCHEDULER)
   public void onSchedulerAdded(ModelAddedEvent event, SubsystemContext<ClimateSubsystemModel> context) {
      context.logger().debug("Discovered new scheduler: [{}]", event.getAddress());
      syncThermostats(context, ImmutableSet.<String>of());
   }

   @OnRemoved(query=ClimateSubsystemUtils.IS_SCHEDULER)
   public void onSchedulerRemoved(ModelRemovedEvent event, SubsystemContext<ClimateSubsystemModel> context) {
      context.logger().debug("A scheduler was removed: [{}]", event.getAddress());
      syncThermostats(context, ImmutableSet.<String>of());
   }
   

   @Request(ClimateSubsystemCapability.EnableSchedulerRequest.NAME)
   public MessageBody enableSchedule(
         @Named(ClimateSubsystemCapability.EnableSchedulerRequest.ATTR_THERMOSTAT)
         String thermostat,
         SubsystemContext<ClimateSubsystemModel> context
   ) {
      Map<String, Object> attributes = context.model().getThermostatSchedules().get(thermostat);
      if(attributes == null) {
         throw new ErrorEventException("thermostat.notfound", "No thermostat with address [" + thermostat + "]");
      }
      if(!isTimezoneSet(context)) {
         throw new ErrorEventException("timezone.notset", "Can't enable scheduling with no timezone set.");
      }

      Address thermostatAddr = Address.fromString(thermostat);
      Model m  = context.models().getModelByAddress(thermostatAddr);
      if(ClimateSubsystemUtils.isSchedulable(m)) {
         context.request(thermostatAddr, SchedulableCapability.EnableScheduleRequest.instance());
      }

      ThermostatScheduleStatus status = new ThermostatScheduleStatus(attributes);
      status.setEnabled(true);
      putInMap(
            context.model(),
            ClimateSubsystemCapability.ATTR_THERMOSTATSCHEDULES,
            thermostat,
            status.toMap()
      );
      syncThermostatMode(thermostatAddr, context);

      return EnableSchedulerResponse.instance();
   }

   @Request(ClimateSubsystemCapability.DisableSchedulerRequest.NAME)
   public MessageBody disableSchedule(
         @Named(ClimateSubsystemCapability.DisableSchedulerRequest.ATTR_THERMOSTAT)
         String thermostat,
         SubsystemContext<ClimateSubsystemModel> context
   ) {
      Map<String, Object> attributes = context.model().getThermostatSchedules().get(thermostat);
      if(attributes == null) {
         throw new ErrorEventException("thermostat.notfound", "No thermostat with address [" + thermostat + "]");
      }

      Address thermostatAddr = Address.fromString(thermostat);
      Model m  = context.models().getModelByAddress(thermostatAddr);
      if(ClimateSubsystemUtils.isSchedulable(m)) {
         context.request(thermostatAddr, SchedulableCapability.DisableScheduleRequest.instance());
      }

      ThermostatScheduleStatus status = new ThermostatScheduleStatus(attributes);
      status.setEnabled(false);
      putInMap(
            context.model(),
            ClimateSubsystemCapability.ATTR_THERMOSTATSCHEDULES,
            thermostat,
            status.toMap()
      );
      syncThermostatMode(Address.fromString(thermostat), context);

      return DisableSchedulerResponse.instance();
   }

   protected void initializeAttributes(ClimateSubsystemModel model) {
      setIfNull(model, ClimateSubsystemCapability.ATTR_CONTROLDEVICES, ImmutableSet.<String>of());
      setIfNull(model, ClimateSubsystemCapability.ATTR_TEMPERATUREDEVICES, ImmutableSet.<String>of());
      setIfNull(model, ClimateSubsystemCapability.ATTR_HUMIDITYDEVICES, ImmutableSet.<String>of());
      setIfNull(model, ClimateSubsystemCapability.ATTR_THERMOSTATS, ImmutableSet.<String>of());
      setIfNull(model, ClimateSubsystemCapability.ATTR_ACTIVEFANS, ImmutableSet.<String>of());
      setIfNull(model, ClimateSubsystemCapability.ATTR_ACTIVEHEATERS, ImmutableSet.<String>of());
      setIfNull(model, ClimateSubsystemCapability.ATTR_CLOSEDVENTS, ImmutableSet.<String>of());
      setIfNull(model, ClimateSubsystemCapability.ATTR_THERMOSTATSCHEDULES, ImmutableMap.of());
      setIfNull(model, ClimateSubsystemCapability.ATTR_PRIMARYTHERMOSTAT, "");
      setIfNull(model, ClimateSubsystemCapability.ATTR_PRIMARYTEMPERATUREDEVICE, "");
      setIfNull(model, ClimateSubsystemCapability.ATTR_PRIMARYHUMIDITYDEVICE, "");
   }

   /**
    * Creates the initial schedule for a thermostat.  The response is handled by onSchedulerResponse.
    * @param context
    * @param thermostat
    */
   protected void initializeThermostat(SubsystemContext<ClimateSubsystemModel> context, String thermostatAddress) {
      context.logger().info("Initializing schedules for thermostat [{}]", thermostatAddress);

      Model m = context.models().getModelByAddress(Address.fromString(thermostatAddress));
      ThermostatScheduleStatus status = new ThermostatScheduleStatus();
      boolean enabled = false; // disabled by default, unless the thermostat is natively scheduled, and then see what it thinks
      boolean initialize = true;
      if(ClimateSubsystemUtils.isSchedulable(m) && SchedulableCapability.TYPE_NOT_SUPPORTED.equals(SchedulableModel.getType(m))) {
      	context.logger().debug("no schedule for thermostat whose schedulable type is {}", SchedulableCapability.TYPE_NOT_SUPPORTED);
      }else {
      	if(ClimateSubsystemUtils.isSchedulable(m)) {
            enabled = SchedulableModel.getScheduleEnabled(m);
            // if it is running from a native schedule, don't change the settings
            initialize = !enabled;
         }
         status.setEnabled(enabled);
         status.setThermostat(thermostatAddress);
         putInMap(context.model(), ClimateSubsystemCapability.ATTR_THERMOSTATSCHEDULES, thermostatAddress, status.toMap());

         if(initialize) {
            // TODO do this based on time of day
            MessageBody message =
                  MessageBody
                     .buildMessage(
                           Capability.CMD_SET_ATTRIBUTES,
                           ImmutableMap.<String, Object>of(
                                 ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_AUTO,
                                 ThermostatCapability.ATTR_COOLSETPOINT, DAY_COOLPOINT,
                                 ThermostatCapability.ATTR_HEATSETPOINT, DAY_HEATPOINT
                           )
                     );
            context.request(Address.fromString(thermostatAddress), message);
         }
         {
         	sendCreateScheduleRequest(context, thermostatAddress, ClimateSubsystemUtils.isSchedulable(m), SchedulableModel.getType(m));            
         }
      }
      
   }
   
   private void sendCreateScheduleRequest(SubsystemContext<ClimateSubsystemModel> context, String targetAddress, boolean isSchedulable, String scheduleType) {
   	List<Map<String,Object>> commands = DEFAULT_SCHEDULE;
      if(isSchedulable && SchedulableCapability.TYPE_DEVICE_ONLY.equals(scheduleType)) {
         context.logger().debug("applying empty schedule to thermostat that only supports scheduling on the device");
         commands = EMPTY_SCHEDULE;
      }
      MessageBody message =
         SchedulerService.ScheduleCommandsRequest.builder()
            .withTarget(targetAddress)
            .withGroup(THERMOSTAT_SCHEDULE_GROUP)
            .withCommands(commands)
            .build();
      String correlationId = context.request(SCHEDULER_ADDRESS, message);
      context.setVariable(correlationId, targetAddress);
   }

   protected void syncDevices(SubsystemContext<ClimateSubsystemModel> context) {
      Set<String> oldThermostats = new HashSet<>(context.model().getThermostats());
      Set<String> controlDevices = new HashSet<>();
      Set<String> temperatureDevices = new HashSet<>();
      Set<String> humidityDevices = new HashSet<>();
      Set<String> thermostats = new HashSet<>();
      Set<String> closedVents = new HashSet<>();
      Set<String> activeFans = new HashSet<>();
      Set<String> newlyAddedThermostats = new HashSet<>();      
      Set<String> activeHeaters = new HashSet<>();

      for(Model model: context.models().getModelsByType(DeviceCapability.NAMESPACE)) {
         addAddressIfMatches(ClimateSubsystemUtils.isControlDevice(), model, controlDevices);
         addAddressIfMatches(ClimateSubsystemUtils.isTemperatureDevice(), model, temperatureDevices);
         addAddressIfMatches(ClimateSubsystemUtils.isHumidityDevice(), model, humidityDevices);
         addAddressIfMatches(ClimateSubsystemUtils.isThermostat(), model, thermostats);
         addAddressIfMatches(ClimateSubsystemUtils.isClosedVent(), model, closedVents);
         addAddressIfMatches(ClimateSubsystemUtils.isActiveFan(), model, activeFans);
         addAddressIfMatches(ClimateSubsystemUtils.isActiveHeater(), model, activeHeaters);
         
         String thermostatAddress = model.getAddress().getRepresentation();
         if(ClimateSubsystemUtils.isThermostat(model) && !oldThermostats.remove(thermostatAddress)) {
            initializeThermostat(context, thermostatAddress);
            newlyAddedThermostats.add(thermostatAddress);
         }
      }
      context.model().setControlDevices(controlDevices);
      context.model().setTemperatureDevices(temperatureDevices);
      context.model().setHumidityDevices(humidityDevices);
      context.model().setThermostats(thermostats);
      context.model().setClosedVents(closedVents);
      context.model().setActiveFans(activeFans);
      context.model().setActiveHeaters(activeHeaters);
      
      for(String address: oldThermostats) {
         context.setVariable(VAR_SCHEDADRESS + address, null);
      }

      syncAvailable(context);
      syncThermostats(context, newlyAddedThermostats);
      syncPrimaryThermostat(context);
      syncPrimaryTemperatureDevice(context);
      syncPrimaryHumidityDevice(context);
      syncTemperature(context);
      syncHumidity(context);
   }

   protected void syncAvailable(SubsystemContext<ClimateSubsystemModel> context) {
      boolean hasTemperatureDevices = !context.model().getTemperatureDevices().isEmpty();
      boolean hasControlDevices = !context.model().getControlDevices().isEmpty();
      context.model().setAvailable(hasTemperatureDevices || hasControlDevices);
   }

   protected void syncThermostats(SubsystemContext<ClimateSubsystemModel> context, Set<String> newlyAddedThermostats) {
	  Map<String, Model> existingScheduleMap = new HashMap<>();
	  String curTarget = null;
      for(Model curSchedule : context.models().getModelsByType(SchedulerCapability.NAMESPACE)) {
    	  curTarget = curSchedule.getAttribute(TypeMarker.string(), SchedulerCapability.ATTR_TARGET, "");
    	  if(StringUtils.isNotBlank(curTarget)) {
    		  existingScheduleMap.put(curTarget, curSchedule);
    	  }    	  
      }
	      
      Map<String, Map<String, Object>> schedules = new HashMap<>(ClimateSubsystemModel.getThermostatSchedules(context.model(), ImmutableMap.<String, Map<String, Object>>of()));
      Set<String> addresses = ClimateSubsystemModel.getThermostats(context.model(), ImmutableSet.<String>of());
      schedules.keySet().retainAll(addresses);
      for(String address: context.model().getThermostats()) {
         Model m  = context.models().getModelByAddress(Address.fromString(address));
         Map<String, Object> attributes = schedules.get(address);
         ThermostatScheduleStatus curScheduleStatus = null;
         if(attributes == null) {
        	curScheduleStatus = new ThermostatScheduleStatus();            
            curScheduleStatus.setEnabled(ClimateSubsystemUtils.isSchedulable(m) ? SchedulableModel.getScheduleEnabled(m) : true);
            curScheduleStatus.setScheduler(context.getVariable(address).as(String.class));
            curScheduleStatus.setThermostat(address);
         } else {
        	curScheduleStatus = new ThermostatScheduleStatus(attributes);
        	if(ClimateSubsystemUtils.isSchedulable(m)) {
        		curScheduleStatus.setEnabled(SchedulableModel.getScheduleEnabled(m));
        	}            
         }
         boolean isAdd = true;
         if(!newlyAddedThermostats.contains(address)) {
        	 //Only sync with existing schedulers if this is not a newly added thermostat because it is possible the scheduler has not been created.
        	 Model existingSchedule = existingScheduleMap.get(address);
        	 if(existingSchedule != null) {
        		 String curSchedAddress = existingSchedule.getAddress().getRepresentation();
        		 if(StringUtils.isBlank(curScheduleStatus.getScheduler()) || !curScheduleStatus.getScheduler().equals(curSchedAddress)) {
        			 context.logger().warn("ThermostatScheduleStatus is out of sync for " + curScheduleStatus.getThermostat() +".  Updating scheduler to "+curSchedAddress);
        			 curScheduleStatus.setScheduler(curSchedAddress);
        		 }        		 
        	 }else{
        		 //Does not exist an existing schedule for the current ThermostatScheduleStatus, remove it.
        		 isAdd = false;
        		 context.logger().warn("ThermostatScheduleStatus is out of sync for " + curScheduleStatus.getThermostat() +".  removing ThermostatScheduleStatus.");
        	 }
         }
         if(isAdd) {
        	 schedules.put(address, curScheduleStatus.toMap());
         }         
      }
      context.model().setThermostatSchedules(schedules);
      for(String address: addresses) {
         syncThermostatMode(Address.fromString(address), context);
      }
   }

   protected void syncThermostatMode(Address thermostatAddress, SubsystemContext<ClimateSubsystemModel> context) {
      Map<String, Object> attributes =
            ClimateSubsystemModel
               .getThermostatSchedules(context.model(), ImmutableMap.<String, Map<String,Object>>of())
               .get(thermostatAddress.getRepresentation());
      if(attributes == null) {
         context.logger().warn("Can't change scheduler mode, thermostat hasn't been initialized yet");
         return;
      }

      ThermostatScheduleStatus status = new ThermostatScheduleStatus(attributes);
      if(StringUtils.isEmpty(status.getScheduler())) {
         context.logger().warn("Can't change scheduler mode, scheduler address hasn't been loaded yet");
         return;
      }

      Model thermostat = context.models().getModelByAddress(thermostatAddress);
      String hvacMode = ThermostatModel.getHvacmode(thermostat);
      Map<String, Object> schedulerAttributes;
      if(
            Boolean.TRUE.equals(status.getEnabled()) &&
            !ThermostatCapability.HVACMODE_OFF.equals(hvacMode)
      ) {
         context.logger().debug("Enabling schedule [{}] for [{}]", hvacMode, thermostatAddress);
         schedulerAttributes = ImmutableMap.<String, Object>of(ScheduleCapability.ATTR_ENABLED + ":" + hvacMode, true);
      }
      else {
         context.logger().debug("Disabling schedules for [{}]", thermostatAddress);
         schedulerAttributes = ImmutableMap.<String, Object>of(
               ScheduleCapability.ATTR_ENABLED + ":" + ThermostatCapability.HVACMODE_AUTO, false,
               ScheduleCapability.ATTR_ENABLED + ":" + ThermostatCapability.HVACMODE_COOL, false,
               ScheduleCapability.ATTR_ENABLED + ":" + ThermostatCapability.HVACMODE_HEAT, false
         );
      }

      context.request(
            Address.fromString(status.getScheduler()),
            MessageBody.buildMessage(Capability.CMD_SET_ATTRIBUTES, schedulerAttributes)
      );
   }

   protected void syncPrimaryThermostat(SubsystemContext<ClimateSubsystemModel> context) {
      String primaryThermostat = context.model().getPrimaryThermostat("");
      Set<String> thermostats = context.model().getThermostats();
      if(thermostats.contains(primaryThermostat)) {
         return;
      }

      String thermostat = Iterables.getFirst(thermostats, "");
      context.model().setPrimaryThermostat(thermostat);
      LooselyTypedReference ref = context.getVariable(VAR_USER_SELECTED_TEMPERATURE);
      if(ref.isNull() || !ref.as(Boolean.class)) {
         // if the user hasn't explicitly selected a primary temperature device, set it to be the thermostat
         context.model().setPrimaryTemperatureDevice(thermostat);
      }
   }

   protected void syncPrimaryTemperatureDevice(SubsystemContext<ClimateSubsystemModel> context) {
      String primaryTemperatureDevice = context.model().getPrimaryTemperatureDevice("");
      Set<String> temperatureDevices = context.model().getTemperatureDevices();
      if(temperatureDevices.contains(primaryTemperatureDevice)) {
         return;
      }

      context.setVariable(VAR_USER_SELECTED_TEMPERATURE, false);
      primaryTemperatureDevice = chooseAddressFrom(temperatureDevices, context);
      context.model().setPrimaryTemperatureDevice(primaryTemperatureDevice);
      syncTemperature(context);
   }

   protected void syncPrimaryHumidityDevice(SubsystemContext<ClimateSubsystemModel> context) {
      String primaryHumidityDevice = context.model().getPrimaryHumidityDevice("");
      Set<String> humidityDevices = context.model().getHumidityDevices();
      if(humidityDevices.contains(primaryHumidityDevice)) {
         return;
      }

      context.setVariable(VAR_USER_SELECTED_TEMPERATURE, false);
      primaryHumidityDevice = chooseAddressFrom(humidityDevices, context);
      context.model().setPrimaryHumidityDevice(primaryHumidityDevice);
      syncHumidity(context);
   }

   protected void syncTemperature(SubsystemContext<ClimateSubsystemModel> context) {
      String primaryTemperatureAddress = context.model().getPrimaryTemperatureDevice("");
      Model primaryTemperatureDevice = StringUtils.isEmpty(primaryTemperatureAddress) ? null : context.models().getModelByAddress(Address.fromString(primaryTemperatureAddress));
      if(primaryTemperatureDevice != null && DeviceConnectionModel.isStateONLINE(primaryTemperatureDevice)) {
         context.model().setTemperature(TemperatureModel.getTemperature(primaryTemperatureDevice, null));
      }
      else {
         context.model().setTemperature(null);
      }
   }

   protected void syncHumidity(SubsystemContext<ClimateSubsystemModel> context) {
      String primaryHumidityAddress = context.model().getPrimaryHumidityDevice("");
      Model primaryHumidityDevice = StringUtils.isEmpty(primaryHumidityAddress) ? null : context.models().getModelByAddress(Address.fromString(primaryHumidityAddress));
      if(primaryHumidityDevice != null && DeviceConnectionModel.isStateONLINE(primaryHumidityDevice)) {
         context.model().setHumidity(RelativeHumidityModel.getHumidity(primaryHumidityDevice, null));
      }
      else {
         context.model().setHumidity(null);
      }
   }

   protected void updateVent(Model model, SubsystemContext<ClimateSubsystemModel> context) {
      if(ClimateSubsystemUtils.isClosedVent(model) && DeviceConnectionModel.isStateONLINE(model)) {
         addAddressToSet(model.getAddress().getRepresentation(), ClimateSubsystemCapability.ATTR_CLOSEDVENTS, context.model());
      }
      else {
         removeAddressFromSet(model.getAddress().getRepresentation(), ClimateSubsystemCapability.ATTR_CLOSEDVENTS, context.model());
      }
   }

   protected void updateFan(Model model, SubsystemContext<ClimateSubsystemModel> context) {
      if(ClimateSubsystemUtils.isActiveFan(model) && DeviceConnectionModel.isStateONLINE(model)) {
         addAddressToSet(model.getAddress().getRepresentation(), ClimateSubsystemCapability.ATTR_ACTIVEFANS, context.model());
      }
      else {
         removeAddressFromSet(model.getAddress().getRepresentation(), ClimateSubsystemCapability.ATTR_ACTIVEFANS, context.model());
      }
   }
   
   protected void updateHeater(Model model, SubsystemContext<ClimateSubsystemModel> context) {
      if(ClimateSubsystemUtils.isActiveHeater(model) && DeviceConnectionModel.isStateONLINE(model)) {
         addAddressToSet(model.getAddress().getRepresentation(), ClimateSubsystemCapability.ATTR_ACTIVEHEATERS, context.model());
      }
      else {
         removeAddressFromSet(model.getAddress().getRepresentation(), ClimateSubsystemCapability.ATTR_ACTIVEHEATERS, context.model());
      }
   }

   /* (non-Javadoc)
    * @see com.iris.common.subsystem.BaseSubsystem#setAttribute(java.lang.String, java.lang.Object, com.iris.common.subsystem.SubsystemContext)
    */
   @Override
   protected void setAttribute(
         String name, Object value,
         SubsystemContext<ClimateSubsystemModel> context
   ) throws ErrorEventException {
      String address = (String) value;

      if(ClimateSubsystemCapability.ATTR_PRIMARYTHERMOSTAT.equals(name)) {
         if(!context.model().getThermostats().contains(value)) {
            throw new ErrorEventException(ClimateErrors.notThermostat(address));
         }
      }
      if(ClimateSubsystemCapability.ATTR_PRIMARYTEMPERATUREDEVICE.equals(name)) {
         if(!context.model().getTemperatureDevices().contains(value)) {
            throw new ErrorEventException(ClimateErrors.notTemperatureDevice(address));
         }
         context.setVariable(VAR_USER_SELECTED_TEMPERATURE, true);
      }
      if(ClimateSubsystemCapability.ATTR_PRIMARYHUMIDITYDEVICE.equals(name)) {
         if(!context.model().getTemperatureDevices().contains(value)) {
            throw new ErrorEventException(ClimateErrors.notHumidityDevice(address));
         }
         context.setVariable(VAR_USER_SELECTED_HUMIDITY, true);
      }
      try {
         super.setAttribute(name, value, context);
      }
      finally {
         if(ClimateSubsystemCapability.ATTR_PRIMARYTEMPERATUREDEVICE.equals(name)) {
            syncTemperature(context);
         }
         if(ClimateSubsystemCapability.ATTR_PRIMARYHUMIDITYDEVICE.equals(name)) {
            syncHumidity(context);
         }
      }
   }

   private boolean isTimezoneSet(SubsystemContext<ClimateSubsystemModel> context) {
      Address placeAddress = Address.platformService(context.getPlaceId(), PlaceCapability.NAMESPACE);
      String tz = PlaceModel.getTzName(context.models().getModelByAddress(placeAddress));
      return !StringUtils.isEmpty(tz);
   }

   /**
    * If there is a thermostat in devices, then it uses that address,
    * otherwise it chooses a random address from devices.
    * @param devices
    * @param context
    * @return
    */
   private String chooseAddressFrom(
         Set<String> devices,
         SubsystemContext<ClimateSubsystemModel> context
   ) {
      for(String thermostatAddress: context.model().getThermostats()) {
         if(devices.contains(thermostatAddress)) {
            return thermostatAddress;
         }
      }
      return Iterables.getFirst(devices, "");
   }

}

