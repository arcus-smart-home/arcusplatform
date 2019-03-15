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
package com.iris.messages.model.test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableSet;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.capability.AlertCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.ContactCapability;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.DevicePowerCapability;
import com.iris.messages.capability.EcowaterWaterSoftenerCapability;
import com.iris.messages.capability.HubAdvancedCapability;
import com.iris.messages.capability.HubAlarmCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.HubChimeCapability;
import com.iris.messages.capability.HubConnectionCapability;
import com.iris.messages.capability.KeyPadCapability;
import com.iris.messages.capability.MobileDeviceCapability;
import com.iris.messages.capability.MotionCapability;
import com.iris.messages.capability.MotorizedDoorCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.PresenceCapability;
import com.iris.messages.capability.RecordingCapability;
import com.iris.messages.capability.RelativeHumidityCapability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.capability.SwitchCapability;
import com.iris.messages.capability.TemperatureCapability;
import com.iris.messages.capability.ValveCapability;
import com.iris.messages.capability.WaterHeaterCapability;
import com.iris.messages.capability.WaterSoftenerCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.util.IrisCollections;
import com.iris.util.IrisCollections.MapBuilder;
import com.iris.util.IrisUUID;

public class ModelFixtures {

   public static final String HUB_ID = "ABC-1234";
   
   public static Map<String, Object> createMobileDeviceAttributes() {
      return buildDeviceAttributes(MobileDeviceCapability.NAMESPACE)
            .put(MobileDeviceCapability.ATTR_PERSONID, UUID.randomUUID())
            .put(MobileDeviceCapability.ATTR_DEVICEINDEX, 1)
            .put(MobileDeviceCapability.ATTR_ASSOCIATED, new Date())
            .put(MobileDeviceCapability.ATTR_OSTYPE, "ios")
            .put(MobileDeviceCapability.ATTR_OSVERSION, "Version 9.1 (Build 13B143)")
            .put(MobileDeviceCapability.ATTR_FORMFACTOR, "tablet")
            .put(MobileDeviceCapability.ATTR_PHONENUMBER, "555-555-5555")
            .put(MobileDeviceCapability.ATTR_DEVICEIDENTIFIER, "deviceidentifier")
            .put(MobileDeviceCapability.ATTR_DEVICEMODEL, "deviceModel")
            .put(MobileDeviceCapability.ATTR_DEVICEVENDOR, "deviceVendor")
            .put(MobileDeviceCapability.ATTR_RESOLUTION, "1024 by 768")
            .put(MobileDeviceCapability.ATTR_NOTIFICATIONTOKEN, "token")
            .put(MobileDeviceCapability.ATTR_LASTLATITUDE, 38.97)
            .put(MobileDeviceCapability.ATTR_LASTLONGITUDE, 95.23)
            .put(MobileDeviceCapability.ATTR_LASTLOCATIONTIME, new Date())
            .put(MobileDeviceCapability.ATTR_NAME, "iPad")
            .put(MobileDeviceCapability.ATTR_APPVERSION, "UNKNOWN")
            .create();
   }
   
   public static Map<String, Object> createPersonWith(String firstName, String lastName, String email, String mobilePhone, String pin, String placeId) {
	   return 
            buildServiceAttributes(PersonCapability.NAMESPACE)
               .put(PersonCapability.ATTR_FIRSTNAME, firstName)
               .put(PersonCapability.ATTR_LASTNAME, lastName)
               .put(PersonCapability.ATTR_EMAIL, email)
               .put(PersonCapability.ATTR_MOBILENUMBER, mobilePhone)
               .put(PersonCapability.ATTR_PLACESWITHPIN, StringUtils.isNotBlank(placeId) && StringUtils.isNotBlank(pin)? ImmutableSet.<String>of(placeId.toString()):ImmutableSet.<String>of())
               .put(PersonCapability.ATTR_HASPIN, StringUtils.isNotBlank(pin)? Boolean.TRUE:Boolean.FALSE)
               .create();
   }
   
   public static Map<String, Object> createPersonAttributes() {
      return createPersonWith("John", "Doe", "john@doe.com", "555-111-2222", "1234", null);            
   }
   
	public static Map<String, Object> createAlertAttributes() {
		return buildAlertAttributes().create();
	}

   public static Map<String, Object> createSwitchAttributes() {
      return
            buildDeviceAttributes(SwitchCapability.NAMESPACE)
               .put(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_OFF)
               .put(SwitchCapability.ATTR_INVERTED, false)
               .put(SwitchCapability.ATTR_STATECHANGED, new Date())
               .create();
   }
   
   public static Map<String, Object> createMotionAttributes() {
      return
            buildDeviceAttributes(MotionCapability.NAMESPACE)
               .put(MotionCapability.ATTR_MOTION, MotionCapability.MOTION_NONE)
               .put(MotionCapability.ATTR_MOTIONCHANGED, new Date())
               .create();
   }
   public static Map<String, Object> createContactAttributes() {
      return
            buildDeviceAttributes(ContactCapability.NAMESPACE)
               .put(ContactCapability.ATTR_CONTACT, ContactCapability.CONTACT_CLOSED)
               .put(ContactCapability.ATTR_CONTACTCHANGED, new Date())
               .create();
   }
   
   public static Map<String, Object> createWaterHeaterAttributes() {
      return
            buildDeviceAttributes(WaterHeaterCapability.NAMESPACE)
               .put(WaterHeaterCapability.ATTR_HEATINGSTATE, true)
               .put(WaterHeaterCapability.ATTR_SETPOINT, 45.0)
               .put(WaterHeaterCapability.ATTR_MAXSETPOINT, 50.0)
               .put(WaterHeaterCapability.ATTR_HOTWATERLEVEL, WaterHeaterCapability.HOTWATERLEVEL_HIGH)
               .create();
   }
   
   public static Map<String, Object> createWaterSoftenerAttributes() {
      return
            buildDeviceAttributes(WaterSoftenerCapability.NAMESPACE)
               .put(WaterSoftenerCapability.ATTR_CURRENTSALTLEVEL, 32)
               .put(WaterSoftenerCapability.ATTR_RECHARGESTARTTIME , 22)
               .put(WaterSoftenerCapability.ATTR_MAXSALTLEVEL, 60)
               .put(WaterSoftenerCapability.ATTR_CURRENTSALTLEVEL, 32)
               .put(WaterSoftenerCapability.ATTR_RECHARGETIMEREMAINING, 12)
               .put(WaterSoftenerCapability.ATTR_DAYSPOWEREDUP, 53)
               .put(WaterSoftenerCapability.ATTR_SALTLEVELENABLED, Boolean.TRUE)
               .create();
   }
   
   public static Map<String, Object> createEcoWaterWaterSoftenerAttributes() {
      return
            buildDeviceAttributes(EcowaterWaterSoftenerCapability.NAMESPACE)
               .put(EcowaterWaterSoftenerCapability.ATTR_ALERTONCONTINUOUSUSE, false)
               .put(EcowaterWaterSoftenerCapability.ATTR_ALERTONEXCESSIVEUSE , false)
               .put(EcowaterWaterSoftenerCapability.ATTR_CONTINUOUSDURATION, 60)
               .put(EcowaterWaterSoftenerCapability.ATTR_CONTINUOUSRATE, 1.2)
               .put(EcowaterWaterSoftenerCapability.ATTR_EXCESSIVEUSE, false)
               .put(EcowaterWaterSoftenerCapability.ATTR_CONTINUOUSUSE, false)
               .create();
   }
   
   public static Map<String, Object> createWaterValveAttributes() {
	      return
	            buildDeviceAttributes(ValveCapability.NAMESPACE)
	               .put(ValveCapability.ATTR_VALVESTATE, ValveCapability.VALVESTATE_OPEN)
	               .put(ValveCapability.ATTR_VALVESTATECHANGED , new Date())
	               .create();
	   }

   public static Map<String, Object> createPresenceAttributes() {
      return
            buildDeviceAttributes(PresenceCapability.NAMESPACE)
               .put(PresenceCapability.ATTR_PRESENCE, PresenceCapability.PRESENCE_ABSENT)
               .create();
   }
   public static Map<String, Object> createTemperatureAttributes() {
      return
            buildDeviceAttributes(TemperatureCapability.NAMESPACE)
               .put(TemperatureCapability.ATTR_TEMPERATURE, 72)
               .create();
   }
   
   
   public static Map<String, Object> createMotorizedDoorFixture() {
   	return buildMotorizedDoorAttributes().create();
	}

   public static Map<String, Object> createPlaceAttributes() {
      return buildPlaceAttributes().create();
   }
   
   public static MapBuilder<String, Object> buildPlaceAttributes() {
      return buildPlaceAttributes(UUID.randomUUID());
   }
   
   public static MapBuilder<String, Object> buildPlaceAttributes(UUID id) {
      return
            buildServiceAttributes(id, PlaceCapability.NAMESPACE)
               .put(PlaceCapability.ATTR_ACCOUNT, UUID.randomUUID().toString())
               .put(PlaceCapability.ATTR_NAME, "My Place")
               .put(PlaceCapability.ATTR_SERVICELEVEL, PlaceCapability.SERVICELEVEL_PREMIUM)
               ;
   }
   
   public static Map<String, Object> createAccountAttributes() {
      return 
         buildServiceAttributes(AccountCapability.NAMESPACE)
            .put(AccountCapability.ATTR_OWNER, UUID.randomUUID().toString())
            .create();
   }
   
   public static Map<String, Object> createServiceAttributes(String baseNamespace, String... namespaces) {
      return buildServiceAttributes(baseNamespace, namespaces).create();
   }

   public static RecordingBuilder buildRecordingAttributes() {
      return buildRecordingAttributes(IrisUUID.timeUUID());
   }

   public static RecordingBuilder buildRecordingAttributes(UUID recordingId) {
      return new RecordingBuilder(recordingId);
   }

   public static MapBuilder<String, Object> buildServiceAttributes(String baseNamespace, String... namespaces) {
      return buildServiceAttributes(UUID.randomUUID(), baseNamespace, namespaces);
   }

   public static MapBuilder<String, Object> buildServiceAttributes(UUID id, String baseNamespace, String... namespaces) {
      return buildServiceAttrs(id.toString(), baseNamespace, namespaces);
   }

   public static MapBuilder<String, Object> buildServiceAttrs(String id, String baseNamespace, String... namespaces) {
      Set<String> caps = new HashSet<>(namespaces.length + 2);
      caps.add(Capability.NAMESPACE);
      caps.add(baseNamespace);
      for(String namespace: namespaces) {
         caps.add(namespace);
      }
      return
            IrisCollections
                  .<String, Object>map()
                  .put(Capability.ATTR_ID, id)
                  .put(Capability.ATTR_ADDRESS, Address.platformService(id, baseNamespace).getRepresentation())
                  .put(Capability.ATTR_TYPE, baseNamespace)
                  .put(Capability.ATTR_CAPS, caps);
   }

   public static MapBuilder<String, Object> buildSubsystemAttributes(UUID placeId, String subsystemNamespace) {
      return 
            IrisCollections
               .<String, Object>map()
               .put(Capability.ATTR_ID, placeId.toString())
               .put(Capability.ATTR_ADDRESS, Address.platformService(placeId, subsystemNamespace).getRepresentation())
               .put(Capability.ATTR_TYPE, SubsystemCapability.NAMESPACE)
               .put(Capability.ATTR_CAPS, ImmutableSet.of(Capability.NAMESPACE, SubsystemCapability.NAMESPACE, subsystemNamespace))
               .put(SubsystemCapability.ATTR_NAME, subsystemNamespace)
               .put(SubsystemCapability.ATTR_PLACE, placeId.toString())
               ;
   }
   
   public static DeviceBuilder buildDeviceAttributes(String... namespaces) {
      return buildDeviceAttributes(UUID.randomUUID(), namespaces);
   }

   public static DeviceBuilder buildDeviceAttributes(UUID id, String... namespaces) {
      MapBuilder<String, Object> delegate =
            buildServiceAttributes(id, DeviceCapability.NAMESPACE)
                  .put(Capability.ATTR_ADDRESS, Address.platformDriverAddress(id).getRepresentation())
                  .put(DeviceCapability.ATTR_VENDOR, "Testitron")
                  .put(DeviceCapability.ATTR_MODEL, "test")
                  .put(DeviceCapability.ATTR_DEVTYPEHINT, namespaces[0])
                  ;
      return new DeviceBuilder(delegate, Arrays.asList(namespaces));
   }
   
   public static DeviceBuilder buildAlertAttributes() {
   	return
   			buildDeviceAttributes(AlertCapability.NAMESPACE)
   				.put(AlertCapability.ATTR_DEFAULTMAXALERTSECS, 600000)
   				.put(AlertCapability.ATTR_MAXALERTSECS, 600000)
   				.put(AlertCapability.ATTR_STATE, AlertCapability.STATE_QUIET)
   				.put(AlertCapability.ATTR_LASTALERTTIME, new Date())
   				;
   	
   }
   
   public static DeviceBuilder buildContactAttributes() {
      return 
            buildDeviceAttributes(ContactCapability.NAMESPACE)
               .put(ContactCapability.ATTR_CONTACT, ContactCapability.CONTACT_CLOSED)
               .put(ContactCapability.ATTR_CONTACTCHANGED, new Date())
               .put(ContactCapability.ATTR_USEHINT, ContactCapability.USEHINT_DOOR)
               ;
   }
   
   public static DeviceBuilder buildMotionAttributes() {
      return 
            buildDeviceAttributes(MotionCapability.NAMESPACE)
               .put(MotionCapability.ATTR_MOTION, MotionCapability.MOTION_NONE)
               .put(MotionCapability.ATTR_MOTIONCHANGED, new Date())
               ;
   }
   
   public static DeviceBuilder buildKeyPadAttributes() {
   	return
			buildDeviceAttributes(KeyPadCapability.NAMESPACE, AlertCapability.NAMESPACE, MotionCapability.NAMESPACE)
				.online()
				.put(MotionCapability.ATTR_MOTION, MotionCapability.MOTION_NONE)
				.put(MotionCapability.ATTR_MOTIONCHANGED, new Date())
				.put(KeyPadCapability.ATTR_ALARMMODE, KeyPadCapability.ALARMMODE_OFF)
				.put(KeyPadCapability.ATTR_ALARMSOUNDER, KeyPadCapability.ALARMSOUNDER_OFF)
				.put(KeyPadCapability.ATTR_ENABLEDSOUNDS, ImmutableSet.of())
				.put(KeyPadCapability.ATTR_ALARMSTATE, KeyPadCapability.ALARMSTATE_DISARMED)
				.put(AlertCapability.ATTR_STATE, AlertCapability.STATE_QUIET);
   }
   
   public static DeviceBuilder buildMotorizedDoorAttributes() {
		return 
				buildDeviceAttributes(MotorizedDoorCapability.NAMESPACE)
					.online()
	            .put(MotorizedDoorCapability.ATTR_DOORSTATE, MotorizedDoorCapability.DOORSTATE_CLOSED)
	            .put(MotorizedDoorCapability.ATTR_DOORLEVEL, 100)
	            ;
   }
   
   public static Map<String,Object> createHubAttributes() {
      return buildHubAttributes().create();
   }
   
   public static MapBuilder<String, Object> buildHubAttributes() {
      return 
         buildServiceAttrs(
               HUB_ID, 
               HubCapability.NAMESPACE, 
               HubAdvancedCapability.NAMESPACE, 
               HubConnectionCapability.NAMESPACE, 
               HubChimeCapability.NAMESPACE,
               HubAlarmCapability.NAMESPACE
         )
            .put(Capability.ATTR_TYPE, "hub")
            .put(HubCapability.ATTR_NAME, "My Hub")
            .put(HubCapability.ATTR_STATE, HubCapability.STATE_NORMAL)
            .put(HubCapability.ATTR_REGISTRATIONSTATE, HubCapability.REGISTRATIONSTATE_REGISTERED)
            .put(HubCapability.ATTR_ID, HUB_ID)
            .put(Capability.ATTR_ADDRESS, Address.hubService(HUB_ID, HubCapability.NAMESPACE))
            ;
   }

   public static class DeviceBuilder implements MapBuilder<String, Object> {
      private MapBuilder<String, Object> delegate;
      private Set<String> capabilities = new LinkedHashSet<>();
      
      private DeviceBuilder(MapBuilder<String, Object> delegate, Collection<String> namespaces) {
         this.delegate = delegate;
         this.capabilities.add(Capability.NAMESPACE);
         this.capabilities.add(DeviceCapability.NAMESPACE);
         this.capabilities.add(DeviceAdvancedCapability.NAMESPACE);
         this.capabilities.add(DeviceConnectionCapability.NAMESPACE);
         this.capabilities.add(DevicePowerCapability.NAMESPACE);
         this.capabilities.addAll(namespaces);
      }
      
      public DeviceBuilder addCapability(String namespace) {
         this.capabilities.add(namespace);
         return this;
      }
      
      public DeviceBuilder online() {
			put(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_ONLINE);
			put(DeviceConnectionCapability.ATTR_LASTCHANGE, new Date());
			return this;
      }
      
      public DeviceBuilder offline() {
			put(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_OFFLINE);
			put(DeviceConnectionCapability.ATTR_LASTCHANGE, new Date());
			return this;
      }
      
      public DeviceBuilder addTemperature() {
         return
               addCapability(TemperatureCapability.NAMESPACE)
                  .put(TemperatureCapability.ATTR_TEMPERATURE, 20.5)
                  ;
      }
      
      public DeviceBuilder addHumidity() {
         return
               addCapability(RelativeHumidityCapability.NAMESPACE)
                  .put(RelativeHumidityCapability.ATTR_HUMIDITY, 1.0)
                  ;
      }

      @Override
      public DeviceBuilder put(String key, Object value) {
         delegate.put(key, value);
         return this;
      }

      @Override
      public DeviceBuilder putAll(Map<String, Object> map) {
         delegate.putAll(map);
         return this;
      }

      @Override
      public DeviceBuilder remove(String key) {
         delegate.remove(key);
         return this;
      }

      @Override
      public Map<String, Object> create() {
         return this.delegate.put(Capability.ATTR_CAPS, capabilities).create();
      }
      
      public Model toModel() {
         return new SimpleModel(create());
      }
   }

   public static class RecordingBuilder implements MapBuilder<String, Object> {
      private MapBuilder<String, Object> delegate;
      
      private RecordingBuilder(UUID recordingId) {
         this.delegate =
            buildServiceAttributes(recordingId, RecordingCapability.NAMESPACE)
               .put(RecordingCapability.ATTR_ACCOUNTID, UUID.randomUUID())
               .put(RecordingCapability.ATTR_BANDWIDTH, 12800)
               .put(RecordingCapability.ATTR_CAMERAID, UUID.randomUUID())
               .put(RecordingCapability.ATTR_FRAMERATE, 5.0)
               .put(RecordingCapability.ATTR_HEIGHT, 640)
               .put(RecordingCapability.ATTR_NAME, new Date())
               .put(RecordingCapability.ATTR_PLACEID, UUID.randomUUID())
               .put(RecordingCapability.ATTR_PRECAPTURE, 0.0)
               .put(RecordingCapability.ATTR_TIMESTAMP, new Date(IrisUUID.timeof(recordingId)))
               .put(RecordingCapability.ATTR_TYPE, RecordingCapability.TYPE_RECORDING)
               .put(RecordingCapability.ATTR_WIDTH, 480)
               ;
      }
      
      public RecordingBuilder pendingDelete() {
         this.delegate.put(RecordingCapability.ATTR_DELETED, true);
         this.delegate.put(RecordingCapability.ATTR_DELETETIME, new Date());
         return this;
      }
      
      public RecordingBuilder recording() {
         this.delegate.put(RecordingCapability.ATTR_TYPE, RecordingCapability.TYPE_RECORDING);
         return this;
      }
      
      public RecordingBuilder stream() {
         this.delegate.put(RecordingCapability.ATTR_TYPE, RecordingCapability.TYPE_STREAM);
         return this;
      }
      
      public RecordingBuilder completed() {
         return completed(138.771, 1699708L);
      }
      
      public RecordingBuilder completed(double duration, long size) {
         this.delegate
            .put(RecordingCapability.ATTR_DURATION, duration)
            .put(RecordingCapability.ATTR_SIZE, size);
         return this;
      }
      

      @Override
      public RecordingBuilder put(String key, Object value) {
         delegate.put(key, value);
         return this;
      }

      @Override
      public RecordingBuilder putAll(Map<String, Object> map) {
         delegate.putAll(map);
         return this;
      }

      @Override
      public RecordingBuilder remove(String key) {
         delegate.remove(key);
         return this;
      }

      @Override
      public Map<String, Object> create() {
         return this.delegate.create();
      }
      
   }

}

