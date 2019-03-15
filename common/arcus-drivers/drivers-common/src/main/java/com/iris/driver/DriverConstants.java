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
package com.iris.driver;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.reflect.TypeUtils;

import com.iris.device.attributes.AttributeKey;
import com.iris.device.model.AttributeDefinition;
import com.iris.device.model.AttributeFlag;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.model.type.MapType;
import com.iris.model.type.SetType;
import com.iris.model.type.StringType;

// FIXME: This maintains a list of constant AttributeKey's and definitions needed by the driver
// infrastructure until we move it to purely using the new code generated infrastructure
@Deprecated
public interface DriverConstants {

   public static final AttributeKey<Set<String>> BASE_ATTR_CAPS = AttributeKey.createSetOf(Capability.ATTR_CAPS, String.class);
   public static final AttributeDefinition BASE_ATTR_DEF_CAPS = new AttributeDefinition(
         BASE_ATTR_CAPS,
         EnumSet.of(AttributeFlag.READABLE),
         "Capabilities provided by this object",
         null,
         new SetType(StringType.INSTANCE));

   public static final AttributeKey<Set<String>> BASE_ATTR_INSTANCES = 
         (AttributeKey) AttributeKey.createType(Capability.ATTR_INSTANCES, TypeUtils.parameterize(Map.class, String.class, TypeUtils.parameterize(Set.class, String.class)));
   public static final AttributeDefinition BASE_ATTR_DEF_INSTANCES = new AttributeDefinition(
         BASE_ATTR_CAPS,
         EnumSet.of(AttributeFlag.READABLE),
         "Multi-instance capabilities provided by this object",
         null,
         new MapType(new SetType(StringType.INSTANCE)));

   public static final AttributeKey<String> DEV_ATTR_VENDOR = AttributeKey.create(DeviceCapability.ATTR_VENDOR, String.class);
   public static final AttributeDefinition DEV_ATTR_DEF_VENDOR = new AttributeDefinition(
         DEV_ATTR_VENDOR,
         EnumSet.of(AttributeFlag.OPTIONAL, AttributeFlag.READABLE),
         "Vendor name",
         null,
         StringType.INSTANCE);

   public static final AttributeKey<String> DEV_ATTR_MODEL = AttributeKey.create(DeviceCapability.ATTR_MODEL, String.class);
   public static final AttributeDefinition DEV_ATTR_DEF_MODEL = new AttributeDefinition(
         DEV_ATTR_MODEL,
         EnumSet.of(AttributeFlag.OPTIONAL, AttributeFlag.READABLE),
         "Model name",
         null,
         StringType.INSTANCE);
   
   public static final AttributeKey<String> DEV_ATTR_PRODUCTID = AttributeKey.create(DeviceCapability.ATTR_PRODUCTID, String.class);
   public static final AttributeDefinition DEV_ATTR_DEF_PRODUCTID = new AttributeDefinition(
         DEV_ATTR_PRODUCTID,
         EnumSet.of(AttributeFlag.OPTIONAL, AttributeFlag.READABLE),
         "ID of the product catalog entry that describes this device",
         null,
         StringType.INSTANCE);


   public static final AttributeKey<String> DEV_ATTR_DEVTYPEHINT = AttributeKey.create(DeviceCapability.ATTR_DEVTYPEHINT, String.class);
   public static final AttributeDefinition DEV_ATTR_DEF_DEVTYPEHINT = new AttributeDefinition(
         DEV_ATTR_DEVTYPEHINT,
         EnumSet.of(AttributeFlag.OPTIONAL, AttributeFlag.READABLE),
         "The single capability that best represents the primary role of the device. For example, a contact sensor that implements contact, temperature, and battery would have contact for the devtypehint",
         null,
         StringType.INSTANCE);

   public static final AttributeKey<String> DEVADV_ATTR_DRIVERNAME = AttributeKey.create(DeviceAdvancedCapability.ATTR_DRIVERNAME, String.class);
   public static final AttributeDefinition DEVADV_ATTR_DEF_DRIVERNAME = new AttributeDefinition(
         DEVADV_ATTR_DRIVERNAME,
         EnumSet.of(AttributeFlag.READABLE),
         "The name of the driver handling the device.",
         null,
         StringType.INSTANCE);

   public static final AttributeKey<String> DEVADV_ATTR_DRIVERVERSION = AttributeKey.create(DeviceAdvancedCapability.ATTR_DRIVERVERSION, String.class);
   public static final AttributeDefinition DEVADV_ATTR_DEF_DRIVERVERSION = new AttributeDefinition(
         DEVADV_ATTR_DRIVERVERSION,
         EnumSet.of(AttributeFlag.READABLE),
         "The current verison of the driver handling the device.",
         null,
         StringType.INSTANCE);

   public static final AttributeKey<String> DEVADV_ATTR_PROTOCOL = AttributeKey.create(DeviceAdvancedCapability.ATTR_PROTOCOL, String.class);
   public static final AttributeDefinition DEVADV_ATTR_DEF_PROTOCOL = new AttributeDefinition(
         DEVADV_ATTR_PROTOCOL,
         EnumSet.of(AttributeFlag.READABLE),
         "Protocol supported by the device; should initially be one of (zwave, zigbee, alljoyn, ipcd)",
         null,
         StringType.INSTANCE);

   public static final AttributeKey<String> DEVADV_ATTR_SUBPROTOCOL = AttributeKey.create(DeviceAdvancedCapability.ATTR_SUBPROTOCOL, String.class);
   public static final AttributeDefinition DEVADV_ATTR_DEF_SUBPROTOCOL = new AttributeDefinition(
         DEVADV_ATTR_SUBPROTOCOL,
         EnumSet.of(AttributeFlag.OPTIONAL, AttributeFlag.READABLE),
         "Sub-protocol supported by the device. For zigbee devices, this may be ha1.1, ha1.2, etc.",
         null,
         StringType.INSTANCE);

   public static final AttributeKey<String> DEVADV_ATTR_PROTOCOLID = AttributeKey.create(DeviceAdvancedCapability.ATTR_PROTOCOLID, String.class);
   public static final AttributeDefinition DEVADV_ATTR_DEF_PROTOCOLID = new AttributeDefinition(
         DEVADV_ATTR_PROTOCOLID,
         EnumSet.of(AttributeFlag.READABLE),
         "Protocol specific identifier for this device. This should be globally unique. For zigbee devices this will be the mac address. For zwave devices, this should be homeid.deviceid.",
         null,
         StringType.INSTANCE);

}

