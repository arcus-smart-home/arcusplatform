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
package com.iris.protocol.ipcd;

import java.util.List;
import java.util.Map;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.iris.messages.type.IpcdDeviceType;
import com.iris.protocol.ipcd.message.model.Device;

public enum IpcdDeviceTypeRegistry {
   INSTANCE;

   public static final String DEVICE_TYPE_GENIE_GDO = IpcdProtocol.V1_DEVICE_TYPE_GENIE_GDO_CONTROLLER;
   public static final String DEVICE_TYPE_AOSMITH_WATER_HEATER = IpcdProtocol.V1_DEVICE_TYPE_AOSMITH_WATER_HEATER;
   public static final String DEVICE_TYPE_ECOWATER_SOFTENER = IpcdProtocol.V1_DEVICE_TYPE_ECOWATER_SOFTENER;

   public static final String DEVICE_TYPE_SWANN_WIFI_PLUG = "SwannWifiPlug";
   public static final String DEVICE_TYPE_SWANN_WIFI_BATTERY_CAMERA = "SwannWifiBatteryCamera";
   public static final String DEVICE_TYPE_GREATSTAR_INDOOR_WIFI_PLUG = "GreatStarIndoorPlug";
   public static final String DEVICE_TYPE_GREATSTAR_OUTDOOR_WIFI_PLUG = "GreatStarOutdoorPlug";
   public static final String DEVICE_TYPE_GENERIC_SWITCH = "GenericSwitch";
   public static final String DEVICE_TYPE_GENERIC_DIMMER = "GenericDimmer";
   public static final String DEVICE_TYPE_GENERIC_CONTACT_SENSOR = "GenericContactSensor";
   public static final String DEVICE_TYPE_GENERIC_PRESENCE_SENSOR = "GenericPresenceSensor";

   public static final String DEVICE_TYPE_OTHER = "Other";

   public static final IpcdDeviceType GENIE = createDeviceType("Genie", "Aladdin");
   public static final IpcdDeviceType AOSMITH1 = createDeviceType("A.O. Smith", "B1.00");
   public static final IpcdDeviceType AOSMITH2 = createDeviceType("A.O. Smith", "B2.00");
   public static final IpcdDeviceType ECOWATER = createDeviceType("EcoWater", "WHESCS5");
   public static final IpcdDeviceType SWANN_WIFI_PLUG = createDeviceType("Swann", "IrisWifiPlug");
   public static final IpcdDeviceType SWANN_WIFI_BATTERY_CAMERA = createDeviceType("Swann", "SWWHD-INTCAM-US");
   public static final IpcdDeviceType GREATSTAR_INDOOR_PLUG = createDeviceType("GreatStar", "plug_indoor" );
   public static final IpcdDeviceType GREATSTAR_OUTDOOR_PLUG = createDeviceType("GreatStar", "plug_outdoor" );
   public static final IpcdDeviceType GENERIC_SWITCH = createDeviceType("Generic", "Switch");
   public static final IpcdDeviceType GENERIC_DIMMER = createDeviceType("Generic", "Dimmer");
   public static final IpcdDeviceType GENERIC_CONTACT_SENSOR = createDeviceType("Generic", "ContactSensor");
   public static final IpcdDeviceType GENERIC_PRESENCE_SENSOR = createDeviceType("Generic", "PresenceSensor");

   private static final Map<String, List<IpcdDeviceType>> V1_TYPES = ImmutableMap.<String, List<IpcdDeviceType>>builder()
      .put(DEVICE_TYPE_GENIE_GDO.toLowerCase(), ImmutableList.of(GENIE))
      .put(DEVICE_TYPE_AOSMITH_WATER_HEATER.toLowerCase(), ImmutableList.of(AOSMITH1, AOSMITH2))
      .put(DEVICE_TYPE_ECOWATER_SOFTENER.toLowerCase(), ImmutableList.of(ECOWATER))
      .put(DEVICE_TYPE_SWANN_WIFI_PLUG.toLowerCase(), ImmutableList.of(SWANN_WIFI_PLUG))
      .put(DEVICE_TYPE_OTHER.toLowerCase(), ImmutableList.of(SWANN_WIFI_PLUG))
      .put(DEVICE_TYPE_SWANN_WIFI_BATTERY_CAMERA.toLowerCase(), ImmutableList.of(SWANN_WIFI_BATTERY_CAMERA))
      .put(DEVICE_TYPE_GREATSTAR_INDOOR_WIFI_PLUG.toLowerCase(), ImmutableList.of(GREATSTAR_INDOOR_PLUG))
      .put(DEVICE_TYPE_GREATSTAR_OUTDOOR_WIFI_PLUG.toLowerCase(), ImmutableList.of(GREATSTAR_OUTDOOR_PLUG))
      .put(DEVICE_TYPE_GENERIC_CONTACT_SENSOR.toLowerCase(), ImmutableList.of(GENERIC_CONTACT_SENSOR))
      .put(DEVICE_TYPE_GENERIC_SWITCH.toLowerCase(), ImmutableList.of(GENERIC_SWITCH))
      .put(DEVICE_TYPE_GENERIC_DIMMER.toLowerCase(), ImmutableList.of(GENERIC_DIMMER))
      .put(DEVICE_TYPE_GENERIC_PRESENCE_SENSOR.toLowerCase(), ImmutableList.of(GENERIC_PRESENCE_SENSOR))
      .build();

   private static final List<IpcdDeviceType> types;
   private static final List<Map<String, Object>> typesAsMaps;

   static {
      ImmutableList.Builder<IpcdDeviceType> tmpTypes = ImmutableList.builder();
      ImmutableList.Builder<Map<String, Object>> tmpTypesAsMaps = ImmutableList.builder();
      for(List<IpcdDeviceType> dts : V1_TYPES.values()) {
         for(IpcdDeviceType dt : dts) {
            tmpTypes.add(dt);
            tmpTypesAsMaps.add(ImmutableMap.copyOf(dt.toMap()));
         }
      }
      types = tmpTypes.build();
      typesAsMaps = tmpTypesAsMaps.build();
   }

   private static IpcdDeviceType createDeviceType(String vendor, String model) {
      IpcdDeviceType type = new IpcdDeviceType();
      type.setVendor(vendor);
      type.setModel(model);
      return type;
   }

   public List<IpcdDeviceType> listTypes() {
      return types;
   }

   public List<Map<String,Object>> listTypesAsMaps() {
      return typesAsMaps;
   }

   public Device createDeviceFromType(IpcdDeviceType type, String sn) {
      Device d = new Device();
      d.setModel(type.getModel());
      d.setVendor(type.getVendor());

      if(Objects.equal(d.getVendor(), GENIE.getVendor())) {
         sn = formatGenieSerialNumber(sn);
      }

      d.setSn(sn);
      return d;
   }

   public List<Device> createDeviceForV1Type(String v1DevType, String sn) {
      if (null == v1DevType) v1DevType = "Other";

      v1DevType = v1DevType.toLowerCase();

      List<IpcdDeviceType> types = V1_TYPES.get(v1DevType);
      if(types == null) {
         types = ImmutableList.of();
      }

      ImmutableList.Builder<Device> builder = ImmutableList.builder();
      for(IpcdDeviceType dt : types) {
         builder.add(createDeviceFromType(dt, sn));
      }
      return builder.build();
   }

   private String formatGenieSerialNumber(String sn) {
      return sn.length() > 12 ? sn.substring(0, 12) : sn;
   }

}

