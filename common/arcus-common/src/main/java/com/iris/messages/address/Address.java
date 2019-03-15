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
package com.iris.messages.address;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.iris.Utils;
import com.iris.messages.MessageConstants;
import com.iris.messages.services.PlatformConstants;
import com.iris.util.IrisInterner;
import com.iris.util.IrisInterners;
import com.iris.util.IrisUUID;

/**
 *
 */
public abstract class Address implements Serializable {
   private static final long serialVersionUID = 16307199551636381L;
   private static final Logger logger = LoggerFactory.getLogger(Address.class);
   private static final IrisInterner<String> INTERN = IrisInterners.strings();

   public static final int ADDRESS_LENGTH = 44;
   public static final int NAMESPACE_LENGTH = 4;
   public static final int GROUP_LENGTH = 20;
   public static final int ID_LENGTH = 20;
   public static final int CONTEXT_QUALIFIER_LENGTH = 4;

   static final int NAMESPACE_OFFSET = 0;
   static final int GROUP_OFFSET = NAMESPACE_OFFSET + NAMESPACE_LENGTH;
   static final int ID_OFFSET = GROUP_OFFSET + GROUP_LENGTH;
   static final int CONTEXT_QUALIFIER_OFFSET = ID_OFFSET + 16;

   private static final Pattern HUBID_PATTERN = Pattern.compile("[A-Z]{3}-\\d{4}");
   private static final Set<String> STRINGID_SERVICES = ImmutableSet.of(
         PlatformConstants.SERVICE_RULE_TMPL, 
         PlatformConstants.SERVICE_PRODUCT,
         PlatformConstants.SERVICE_SCENE_TMPL
   );

   private static final BroadcastAddress BROADCAST = new BroadcastAddress();

   public static final UUID ZERO_UUID = new UUID(0, 0);
   static final String PLATFORM_GROUP = "";
   public static final String PLATFORM_DRIVER_GROUP = "dev";


   /**
    * Returns the passed in address, or {@link #broadcastAddress()} if it
    * is {@code null}.
    * @param address
    * @return
    */
   public static Address get(Address address) {
      return address == null ? broadcastAddress() : address;
   }

   public static BroadcastAddress broadcastAddress() {
      return BROADCAST;
   }
   
   public static BridgeAddress bridgeAddress(String bridgeId) {
      return new BridgeAddress(intern(bridgeId));
   }

   public static ClientAddress clientAddress(String serverId, String sessionId) {
      return new ClientAddress(intern(serverId), sessionId);
   }

   public static DeviceDriverAddress deviceAddress(String group, UUID deviceId) {
      return new DeviceDriverAddress(intern(group), deviceId);
   }

   public static DeviceDriverAddress platformDriverAddress(UUID deviceId) {
      return new DeviceDriverAddress(PLATFORM_DRIVER_GROUP, deviceId);
   }

   public static DeviceDriverAddress hubDriverAddress(String hubId, UUID deviceId) {
      return new DeviceDriverAddress(intern(hubId), deviceId);
   }

   public static HubAddress hubAddress(String hubId) {
      return new HubAddress(intern(hubId));
   }

   public static DeviceProtocolAddress protocolAddress(String protocolName, String group, ProtocolDeviceId protocolDeviceId) {
      return new DeviceProtocolAddress(intern(protocolName), intern(group), protocolDeviceId);
   }

   public static DeviceProtocolAddress protocolAddress(String protocolName, ProtocolDeviceId protocolDeviceId) {
      return new DeviceProtocolAddress(intern(protocolName), PLATFORM_GROUP, protocolDeviceId);
   }

   public static DeviceProtocolAddress protocolAddress(String protocolName, byte [] protocolDeviceId) {
      return new DeviceProtocolAddress(intern(protocolName), PLATFORM_GROUP, ProtocolDeviceId.fromBytes(protocolDeviceId));
   }

   public static DeviceProtocolAddress protocolAddress(String protocolName, String protocolDeviceId) {
      return new DeviceProtocolAddress(intern(protocolName), PLATFORM_GROUP, ProtocolDeviceId.hashDeviceId(protocolDeviceId));
   }

   public static DeviceProtocolAddress hubProtocolAddress(String hubId, String protocolName, ProtocolDeviceId protocolDeviceId) {
      return new DeviceProtocolAddress(intern(protocolName), intern(hubId), protocolDeviceId);
   }

   public static PlatformServiceAddress platformService(String serviceName) {
      return new PlatformServiceAddress(ZERO_UUID, intern(serviceName), null);
   }

   public static PlatformServiceAddress platformService(Object contextId, String serviceName) {
      return new PlatformServiceAddress(contextId, intern(serviceName), null);
   }

   public static PlatformServiceAddress platformService(Object contextId, String serviceName, Integer contextQualifier) {
      return new PlatformServiceAddress(contextId, intern(serviceName), contextQualifier);
   }

   public static HubServiceAddress hubService(String hubId, String serviceName) {
      return new HubServiceAddress(intern(hubId), intern(serviceName));
   }

   public static Address fromString(String address) {
      if(StringUtils.isEmpty(address)) {
         return broadcastAddress();
      }
      String [] parts = StringUtils.splitPreserveAllTokens(address, ":", 3);
      if(parts.length < 3) {
         throw new IllegalArgumentException("Invalid address [" + address + "], must be of the form [namespace:group:id], [namespace::id] or [namespace:group:]");
      }
      String namespace = parts[0];
      // there won't be a group if it was condensed to namespace::id, now only used for HubAddress
      String group = StringUtils.trimToNull(parts[1]);
      // there won't be an id if it was condensed to namespace:group: to target a platorm service
      String id = StringUtils.trimToNull(parts[2]);

      if(group == null && id == null) {
         throw new IllegalArgumentException("Invalid address [" + address + "], must be of the form [namespace:group:id], [namespace::id] or [namespace:group:]");
      }

      switch(namespace) {
      case MessageConstants.CLIENT:
         return clientAddress(group, id);
      case MessageConstants.DRIVER:
    	  // driver address should have a group and an id by
    	  // contract with the constructor for DeviceDriverAddress
          Utils.assertNotNull(group, "group may not be null");
          Utils.assertNotNull(id, "id may not be null");
         return deviceAddress(group, parseUuid(id));
      case MessageConstants.HUB:
         return hubAddress(id);
      case MessageConstants.BRIDGE:
         return bridgeAddress(id);
      case MessageConstants.SERVICE:
         if(isHubId(group)) {
            return hubService(group, id);
         }
         if(isHubId(id)) {
            return platformService(id, group);
         }
         if(group == null) {
            logger.warn("Parsing legacy address, this style is deprecated");
            group = id;
            id = null;
         }
         if(id == null) {
            return platformService(group);
         }
         String[] idParts = id.split("\\.");
         boolean requiresStringId = platformRequiresStringId(group);
         if(requiresStringId) {
            return platformService(idParts[0], group);
         }
         try{
         	return platformService(parseUuid(idParts[0]), group, idParts.length == 2 ? Integer.valueOf(idParts[1]) : null);
         }catch(Exception e) {
         	return platformService(idParts[0], group, idParts.length == 2 ? Integer.valueOf(idParts[1]) : null);
         }
      case MessageConstants.PROTOCOL:
         if(StringUtils.isEmpty(group)) {
            throw new IllegalArgumentException("No protocol specified");
         }
         String [] protoAndHubId = StringUtils.split(group, "-", 2);
         return protocolAddress(
               protoAndHubId[0],
               protoAndHubId.length == 2 ? protoAndHubId[1] : PLATFORM_GROUP,
               ProtocolDeviceId.fromRepresentation(id)
         );
      default:
         throw new IllegalArgumentException("Unrecognized address namespace: " + namespace);
      }
   }

   public static Address fromBytes(byte[] address) {
      return fromBytes(address, 0);
   }

   public static Address fromBytes(byte[] address, int offset) {
      Utils.assertTrue(address.length >= (ADDRESS_LENGTH + offset), "byte array is too short to be an address");
      // TODO validate that the whole address is empty?
      if(isBroadcastAddress(address, offset)) {
         return broadcastAddress();
      }
      String namespace = readNamespace(address, offset);
      switch(namespace) {
      case MessageConstants.CLIENT:
         return clientAddress(readGroupString(address, offset), readIdString(address, offset));
      case MessageConstants.DRIVER:
         return deviceAddress(readGroupString(address, offset), readIdUuid(address, offset));
      case MessageConstants.HUB:
         return hubAddress(readIdString(address, offset));
      case MessageConstants.SERVICE:
         String groupString = readGroupString(address, offset);
         String id = readIdString(address, offset);
         if(isHubId(groupString)) {
            return hubService(groupString, id);
         }
         boolean platformRequiresStringId = platformRequiresStringId(groupString);
         if(platformRequiresStringId) {
            return platformService(readIdString(address, offset), groupString);
         }
         return platformService(readIdUuid(address, offset), groupString, readContextQulifier(address, offset));
      case MessageConstants.PROTOCOL:
         return protocolAddress(
               readProtocolString(address, offset),
               readGroupString(address, offset),
               ProtocolDeviceId.fromBytes(readIdBytes(address, offset))
         );
      default:
         throw new IllegalArgumentException("Unrecognized address namespace: " + namespace);
      }
   }

   private static boolean platformRequiresStringId(String group) {
      return STRINGID_SERVICES.contains(group);
   }

   public abstract boolean isBroadcast();

   public abstract boolean isHubAddress();

   public String getHubId() { return null; }

   public abstract String getNamespace();

   public abstract Object getGroup();

   public abstract Object getId();

   public abstract String getRepresentation();

   protected String createRepresentation() {
      String namespace = getNamespace();
      Object group = getGroup();
      Object id = getId();

      if(group == null || PLATFORM_GROUP.equals(group) || ZERO_UUID.equals(group)) {
         return namespace + "::" + id;
      } else if(id == null || PLATFORM_GROUP.equals(id) || ZERO_UUID.equals(id)) {
         return namespace + ":" + group + ":";
      }
      return namespace + ":" + group + ":" + id;
   }

   public abstract byte[] getBytes();

   @Override
   public String toString() {
      return getRepresentation();
   }

   static byte[] createBytes() {
      return new byte[ADDRESS_LENGTH];
   }

   static void writePrefix(String value, byte[] dest) {
      writeString(value, dest, NAMESPACE_OFFSET, NAMESPACE_LENGTH);
   }

   static void writeGroup(String value, byte[] dest) {
      writeString(value, dest, GROUP_OFFSET, GROUP_LENGTH);
   }

   static void writeGroup(UUID value, byte[] dest) {
      writeUUID(value, dest, GROUP_OFFSET);
   }

   static void writeId(String value, byte[] dest) {
      writeString(value, dest, ID_OFFSET, ID_LENGTH);
   }

   static void writeId(UUID value, byte[] dest) {
      writeUUID(value, dest, ID_OFFSET);
   }

   static void writeContextQualifier(Integer value, byte[] dest) {
      if(value != null) {
         writeInt(value.intValue(), dest, CONTEXT_QUALIFIER_OFFSET);
      }
   }

   private static void writeUUID(UUID source, byte[] dest, int offset) {
      long msb = source.getMostSignificantBits();
      long lsb = source.getLeastSignificantBits();
      dest[offset]    = (byte) ((msb >> 56) & 0xff);
      dest[offset+1]  = (byte) ((msb >> 48) & 0xff);
      dest[offset+2]  = (byte) ((msb >> 40) & 0xff);
      dest[offset+3]  = (byte) ((msb >> 32) & 0xff);
      dest[offset+4]  = (byte) ((msb >> 24) & 0xff);
      dest[offset+5]  = (byte) ((msb >> 16) & 0xff);
      dest[offset+6]  = (byte) ((msb >> 8) & 0xff);
      dest[offset+7]  = (byte) (msb & 0xff);
      dest[offset+8]  = (byte) ((lsb >> 56) & 0xff);
      dest[offset+9]  = (byte) ((lsb >> 48) & 0xff);
      dest[offset+10] = (byte) ((lsb >> 40) & 0xff);
      dest[offset+11] = (byte) ((lsb >> 32) & 0xff);
      dest[offset+12] = (byte) ((lsb >> 24) & 0xff);
      dest[offset+13] = (byte) ((lsb >> 16) & 0xff);
      dest[offset+14] = (byte) ((lsb >> 8) & 0xff);
      dest[offset+15] = (byte) (lsb & 0xff);
      // 4 additional reserved bytes
      dest[offset+16] = dest[offset+17] = dest[offset+18] = dest[offset+19] = 0;
   }

   private static void writeString(String value, byte[] dest, int offset, int length) {
      byte [] source = value.getBytes(Utils.UTF_8);
      if(source.length > length) {
         throw new IllegalArgumentException("[" + value + "] is too long to be a valid address");
      }
      System.arraycopy(source, 0, dest, offset, source.length);
      for(int i=source.length; i<length; i++) {
         dest[offset+i] = 0;
      }
   }

   private static void writeInt(int value, byte[] dest, int offset) {
      dest[offset] = (byte) (value >> 24);
      dest[offset + 1] = (byte) (value >> 16);
      dest[offset + 2] = (byte) (value >> 8);
      dest[offset + 3] = (byte) value;
   }

   private static String readNamespace(byte[] address, int offset) {
      return readString(address, offset + NAMESPACE_OFFSET, NAMESPACE_LENGTH);
   }

   private static String readGroupString(byte[] address, int offset) {
      return readString(address, offset + GROUP_OFFSET, GROUP_LENGTH);
   }

   private static String readIdString(byte[] address, int offset) {
      return readString(address, offset + ID_OFFSET, ID_LENGTH);
   }

   private static String readProtocolString(byte[] address, int offset) {
      return readString(address, offset + GROUP_OFFSET + 16, 4);
   }

   private static Integer readContextQulifier(byte[] address, int offset) {
      int read = readInt(address, offset + CONTEXT_QUALIFIER_OFFSET);
      return read == 0 ? null : read;
   }

   private static String readString(byte[] address, int offset, int length) {
      for(int i=0; i<length; i++) {
         if(address[i + offset] == 0) {
            length = i;
            break;
         }
      }
      if(length == 0) {
         return "";
      }
      return new String(address, offset, length, Utils.UTF_8);
   }

   private static UUID readGroupUuid(byte[] address, int offset) {
      return readUuid(address, offset + GROUP_OFFSET);
   }

   private static UUID readIdUuid(byte[] address, int offset) {
      return readUuid(address, offset + ID_OFFSET);
   }

   private static int readInt(byte[] address, int offset) {
      return
            (address[offset] << 24) |
            (address[offset+1] & 0xFF) << 16 |
            (address[offset+2] & 0xFF) << 8 |
            (address[offset+3] & 0xFF);
   }

   private static UUID readUuid(byte [] address, int offset) {
      long msb=0, lsb=0;
      msb |= (((long) address[offset]) & 0xff) << 56;
      msb |= (((long) address[offset+1]) & 0xff) << 48;
      msb |= (((long) address[offset+2]) & 0xff) << 40;
      msb |= (((long) address[offset+3]) & 0xff) << 32;
      msb |= (((long) address[offset+4]) & 0xff) << 24;
      msb |= (((long) address[offset+5]) & 0xff) << 16;
      msb |= (((long) address[offset+6]) & 0xff) << 8;
      msb |= (((long) address[offset+7]) & 0xff);
      lsb |= (((long) address[offset+8]) & 0xff) << 56;
      lsb |= (((long) address[offset+9]) & 0xff) << 48;
      lsb |= (((long) address[offset+10]) & 0xff) << 40;
      lsb |= (((long) address[offset+11]) & 0xff) << 32;
      lsb |= (((long) address[offset+12]) & 0xff) << 24;
      lsb |= (((long) address[offset+13]) & 0xff) << 16;
      lsb |= (((long) address[offset+14]) & 0xff) << 8;
      lsb |= (((long) address[offset+15]) & 0xff);
      return new UUID(msb, lsb);
   }

   public static boolean isHubId(String value) {
      if(StringUtils.isBlank(value)) {
         return false;
      }
      return HUBID_PATTERN.matcher(value).matches();
   }

   private static UUID parseUuid(String id) {
      return IrisUUID.fromString(id);
   }

   private static byte[] readGroupBytes(byte[] address, int offset) {
      byte [] id = new byte[GROUP_LENGTH];
      System.arraycopy(address, offset + GROUP_OFFSET, id, 0, GROUP_LENGTH);
      return id;
   }

   private static byte[] readIdBytes(byte[] address, int offset) {
      byte [] id = new byte[ID_LENGTH];
      System.arraycopy(address, offset + ID_OFFSET, id, 0, ID_LENGTH);
      return id;
   }

   private static boolean isBroadcastAddress(byte[] address, int offset) {
      if(address[offset] != 0) {
         return false;
      }
      for(int i=1; i<ADDRESS_LENGTH; i++) {
         if(address[offset+i] != 0) {
            return false;
         }
      }
      return true;
   }

   private static String intern(String value) {
      return INTERN.intern(value);
   }
}

