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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.iris.capability.definition.ProtocolDefinition;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.io.Deserializer;
import com.iris.io.Serializer;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.service.BridgeService;
import com.iris.messages.service.DeviceService;
import com.iris.protocol.Protocol;
import com.iris.protocol.Protocols;
import com.iris.protocol.RemoveProtocolRequest;
import com.iris.protocol.constants.IpcdConstants;
import com.iris.protocol.ipcd.message.IpcdMessage;
import com.iris.protocol.ipcd.message.model.Device;
import com.iris.protocol.ipcd.message.model.DeviceInfo;
import com.iris.protocol.ipcd.message.model.ParameterInfo;
import com.iris.protocol.ipcd.message.serialize.IpcdSerDe;
import com.iris.util.IrisCollections;
import com.iris.util.IrisUUID;

public enum IpcdProtocol implements Protocol<IpcdMessage>, IpcdConstants {
   INSTANCE;
   
   // onChange, onChangeBy, onEquals, onLessThan, onGreaterThan
   
   public final static String THRESHOLD_RULE_ON_CHANGE = "onChange";
   public final static String THRESHOLD_RULE_ON_CHANGE_BY = "onChangeBy";
   public final static String THRESHOLD_RULE_ON_EQUALS = "onEquals";
   public final static String THRESHOLD_RULE_ON_LESS_THAN = "onLessThan";
   public final static String THRESHOLD_RULE_ON_GREATER_THAN = "onGreaterThan";
   
   public final static String PARAM_TYPE_STRING = "string";
   public final static String PARAM_TYPE_ENUM = "enum";
   public final static String PARAM_TYPE_NUMBER = "number";
   public final static String PARAM_TYPE_BOOLEAN = "boolean";
   
   public final static String EVENT_ON_BOOT = "onBoot";
   public final static String EVENT_ON_CONNECT = "onConnect";
   public final static String EVENT_ON_DOWNLOAD_COMPLETE = "onDownloadComplete";
   public final static String EVENT_ON_DOWNLOAD_FAILED = "onDownloadFailed";
   public final static String EVENT_ON_UPDATE = "onUpdate";
   public final static String EVENT_ON_FACTORY_RESET = "onFactoryReset";
   public final static String EVENT_ON_VALUE_CHANGE = "onValueChange";
   
   public final static String STATUS_SUCCESS = "success";
   public final static String STATUS_WARN = "warn";
   public final static String STATUS_FAIL = "fail";
   public final static String STATUS_ERROR = "error";
   
   public final static String PARAM_MODE_READ = "r";
   public final static String PARAM_MODE_WRITE = "w";
   public final static String PARAM_MODE_READWRITE = "rw";
   
   public final static String V1_DEVICE_TYPE_AOSMITH_WATER_HEATER = "AosmithWaterHeater";
   public final static String V1_DEVICE_TYPE_GENIE_GDO_CONTROLLER = "GenieGDOController";
   public final static String V1_DEVICE_TYPE_ECOWATER_SOFTENER = "EcoWaterSoftener";
   public final static Set<String> V1_DEVICE_TYPES = IrisCollections.setOf(
                              V1_DEVICE_TYPE_AOSMITH_WATER_HEATER,
                              V1_DEVICE_TYPE_GENIE_GDO_CONTROLLER,
                              V1_DEVICE_TYPE_ECOWATER_SOFTENER);

   private final IpcdSerDe serDe = new IpcdSerDe();
	private final Serializer<IpcdMessage> serializer = new IpcdProtocolSerializer();
	private final Deserializer<IpcdMessage> deserializer = new IpcdProtocolDeserializer();
   
	@Override
	public String getName() {
		return NAME;
	}

	@Override
   public String getNamespace() {
      return NAMESPACE;
   }

   @Override
   public ProtocolDefinition getDefinition() {
      return DEFINITION;
   }

   @Override
	public Serializer<IpcdMessage> createSerializer() {
	   return serializer;
	}

	@Override
	public Deserializer<IpcdMessage> createDeserializer() {
	   return deserializer;
	}

   @Override
   public PlatformMessage remove(RemoveProtocolRequest req) {
      if(req.isBridgeChild()) {
         return Protocols.removeBridgeChild(req);
      }
      else {
         return Protocols.removeBridgeDevice(NAMESPACE, req);
      }
   }

   public class IpcdProtocolSerializer implements Serializer<IpcdMessage> {
   	@Override
   	public byte[] serialize(IpcdMessage value) throws IllegalArgumentException {
   		String json = serDe.toJson(value);
   		return json != null ? json.getBytes(StandardCharsets.UTF_8) : new byte[]{};
   	}

   	@Override
   	public void serialize(IpcdMessage value, OutputStream out) throws IOException, IllegalArgumentException {
   		byte[] serializedMessage = serialize(value);
   		out.write(serializedMessage);
   	}
   }

   public static class IpcdProtocolDeserializer implements Deserializer<IpcdMessage>{
   	private IpcdSerDe serDe = new IpcdSerDe();

   	@Override
   	public IpcdMessage deserialize(byte[] input) throws IllegalArgumentException {
   		String json = new String(input, StandardCharsets.UTF_8);
   		return serDe.parse(new StringReader(json));
   	}

   	@Override
   	public IpcdMessage deserialize(InputStream input) throws IOException, IllegalArgumentException {
   		return serDe.parse(new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8)));
   	}
   }
   
   // Utility methods
   public static boolean isV1DeviceType(String deviceType) {
      for (String v1DeviceType : V1_DEVICE_TYPES) {
         if (v1DeviceType.equalsIgnoreCase(deviceType)) {
            return true;
         }
      }
      return false;
   }
   
   public static boolean isWriteable(ParameterInfo paramInfo) {
      return paramInfo.getAttrib().equals(PARAM_MODE_WRITE) || paramInfo.getAttrib().equals(PARAM_MODE_READWRITE);
   }
   
   public static Address ipcdAddress(Device device) {
      if (device == null) {
         return null;
      }
      return Address.protocolAddress(IpcdProtocol.NAMESPACE, ipcdProtocolId(device));
   }
   
   public static ProtocolDeviceId ipcdProtocolId(Device device) {
      String rawId = device.getVendor() + '-' + device.getModel() + '-' + device.getSn();
      return ProtocolDeviceId.hashDeviceId(rawId);
   }
   
   public static AttributesBuilder protocolAttributesBuilder() {
      return new AttributesBuilder();
   }
   
   public static void mergeProtocolAttributesIntoIpcdDevice(IpcdDevice ipcdDevice, AttributeMap attrs) {
      if (attrs != null && !attrs.isEmpty()) {
         String vendor = attrs.get(strKey(IpcdProtocol.ATTR_VENDOR));
         String model = attrs.get(strKey(IpcdProtocol.ATTR_MODEL));
         String sn = attrs.get(strKey(IpcdProtocol.ATTR_SN));
         String ipcdVer = attrs.get(strKey(IpcdProtocol.ATTR_IPCDVER));
         String fwVer = attrs.get(strKey(IpcdProtocol.ATTR_FWVER));
         String connection = attrs.get(strKey(IpcdProtocol.ATTR_CONNECTION));
         Set<String> actions = attrs.get(setKey(IpcdProtocol.ATTR_ACTIONS));
         Set<String> commands = attrs.get(setKey(IpcdProtocol.ATTR_COMMANDS));
         if (!StringUtils.isEmpty(vendor)) ipcdDevice.setVendor(vendor);
         if (!StringUtils.isEmpty(model)) ipcdDevice.setModel(model);
         if (!StringUtils.isEmpty(sn)) ipcdDevice.setSn(sn);
         if (!StringUtils.isEmpty(ipcdVer)) ipcdDevice.setIpcdver(ipcdVer);
         if (!StringUtils.isEmpty(fwVer)) ipcdDevice.setFirmware(fwVer);
         if (!StringUtils.isEmpty(connection)) ipcdDevice.setConnection(connection);
         if (actions != null) ipcdDevice.setActions(actions);
         if (commands != null) ipcdDevice.setCommands(commands);
      }
   }
   
   public static class AttributesBuilder {
      private IpcdDevice ipcdDevice = new IpcdDevice();
      private String modelCode;
      private String serialCode;
      
      private AttributesBuilder() {}
      
      public AttributesBuilder withIpcdDevice(IpcdDevice ipcdDevice) {
         this.ipcdDevice = ipcdDevice.copy();
         return this;
      }
      
      public AttributesBuilder withDevice(Device device) {
         ipcdDevice.setVendor(device.getVendor());
         ipcdDevice.setModel(device.getModel());
         ipcdDevice.setSn(device.getSn());
         ipcdDevice.setIpcdver(device.getIpcdver());
         return this;
      }
      
      public AttributesBuilder withDeviceInfo(DeviceInfo deviceInfo) {
         ipcdDevice.updateWithDeviceInfo(deviceInfo);
         return this;
      }
      
      public AttributesBuilder withRegistrationAttributes(Map<String,String> attributes) {
         String modelAttribute = attributes.get(ATTR_MODELCODE);
         String serialAttribute = attributes.get(ATTR_SERIALCODE);
         modelCode = modelAttribute != null ? modelAttribute : "";
         serialCode = serialAttribute != null ? serialAttribute : "";
         return this;
      }
      
      public AttributeMap create() {
         AttributeMap attrMap = AttributeMap.newMap();
         attrMap.set(strKey(ATTR_VENDOR), ipcdDevice.getVendor());
         attrMap.set(strKey(ATTR_MODEL), ipcdDevice.getModel());
         attrMap.set(strKey(ATTR_SN), ipcdDevice.getSn());
         attrMap.set(strKey(ATTR_IPCDVER), ipcdDevice.getIpcdver());
         attrMap.set(strKey(ATTR_FWVER), ipcdDevice.getFirmware());
         attrMap.set(strKey(ATTR_CONNECTION), ipcdDevice.getConnection());
         attrMap.set(setKey(ATTR_ACTIONS), ipcdDevice.getActions());
         attrMap.set(setKey(ATTR_COMMANDS), ipcdDevice.getCommands());
         attrMap.set(strKey(ATTR_MODELCODE), modelCode);
         attrMap.set(strKey(ATTR_SERIALCODE), serialCode);
         return attrMap;
      }
   }
   
   private static AttributeKey<String> strKey(String name) {
      return AttributeKey.create(name, String.class);
   }
   
   private static AttributeKey<Set<String>> setKey(String name) {
      return AttributeKey.createSetOf(name, String.class);
   }

   @Override
   public boolean isTransientAddress() {
      return false;
   }
}

