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
package com.iris.protocol.zigbee;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.eclipse.jdt.annotation.Nullable;

import com.iris.capability.definition.ProtocolDefinition;
import com.iris.io.Deserializer;
import com.iris.io.Serializer;
import com.iris.messages.PlatformMessage;
import com.iris.protoc.runtime.ProtocMessage;
import com.iris.protoc.runtime.ProtocMessageSerDe;
import com.iris.protoc.runtime.ProtocSerDe;
import com.iris.protocol.Protocol;
import com.iris.protocol.Protocols;
import com.iris.protocol.RemoveProtocolRequest;
import com.iris.protocol.constants.ZigbeeConstants;
import com.iris.protocol.zigbee.msg.ZigbeeMessage;

public enum ZigbeeProtocol implements Protocol<ZigbeeMessage.Protocol>, ZigbeeConstants {
   INSTANCE;

   public static final String ATTR_HUB_EUI64 = "eui64";

   private static final ProtocMessageSerDe[] ZDP_SERDES = new ProtocMessageSerDe[] {
      com.iris.protocol.zigbee.zdp.Bind.serde(),
      com.iris.protocol.zigbee.zdp.Discovery.serde(),
      com.iris.protocol.zigbee.zdp.Mgmt.serde(),
   };

   private static final ProtocMessageSerDe[] ZCL_SERVER_SERDES = new ProtocMessageSerDe[] {
      com.iris.protocol.zigbee.zcl.Alarms.serverSerDe(),
      com.iris.protocol.zigbee.zcl.ApplianceAlerts.clientSerDe(),
      //com.iris.protocol.zigbee.zcl.Ballast.serverSerDe(),
      //com.iris.protocol.zigbee.zcl.Basic.serverSerDe(),
      //com.iris.protocol.zigbee.zcl.Color.serverSerDe(),
      //com.iris.protocol.zigbee.zcl.Dehumidification.serverSerDe(),
      //com.iris.protocol.zigbee.zcl.DeviceTemperature.serverSerDe(),
      com.iris.protocol.zigbee.zcl.DoorLock.serverSerDe(),
      //com.iris.protocol.zigbee.zcl.Fan.serverSerDe(),
      //com.iris.protocol.zigbee.zcl.FlowMeasurement.serverSerDe(),
      com.iris.protocol.zigbee.zcl.Groups.serverSerDe(),
      //com.iris.protocol.zigbee.zcl.HumidityMeasurement.serverSerDe(),
      com.iris.protocol.zigbee.zcl.IasAce.serverSerDe(),
      //com.iris.protocol.zigbee.zcl.IasWd.serverSerDe(),
      com.iris.protocol.zigbee.zcl.IasZone.serverSerDe(),
      com.iris.protocol.zigbee.zcl.Identify.serverSerDe(),
      //com.iris.protocol.zigbee.zcl.IlluminanceMeasurement.serverSerDe(),
      //com.iris.protocol.zigbee.zcl.IlluminanceSensing.serverSerDe(),
      //com.iris.protocol.zigbee.zcl.Level.serverSerDe(),
      //com.iris.protocol.zigbee.zcl.OccupancySensing.serverSerDe(),
      //com.iris.protocol.zigbee.zcl.OnOff.serverSerDe(),
      //com.iris.protocol.zigbee.zcl.OnOffSwitch.serverSerDe(),
      //com.iris.protocol.zigbee.zcl.Power.serverSerDe(),
      //com.iris.protocol.zigbee.zcl.PressureMeasurement.serverSerDe(),
      //com.iris.protocol.zigbee.zcl.Pump.serverSerDe(),
      com.iris.protocol.zigbee.zcl.Scenes.serverSerDe(),
      //com.iris.protocol.zigbee.zcl.Shade.serverSerDe(),
      //com.iris.protocol.zigbee.zcl.TemperatureMeasurement.serverSerDe(),
      //com.iris.protocol.zigbee.zcl.Thermostat.serverSerDe(),
      //com.iris.protocol.zigbee.zcl.ThermostatUI.serverSerDe(),
      //com.iris.protocol.zigbee.zcl.Time.serverSerDe(),
   };

   private static final ProtocMessageSerDe[] ZCL_CLIENT_SERDES = new ProtocMessageSerDe[] {
      com.iris.protocol.zigbee.zcl.Alarms.clientSerDe(),
      com.iris.protocol.zigbee.zcl.ApplianceAlerts.clientSerDe(),
      //com.iris.protocol.zigbee.zcl.Ballast.clientSerDe(),
      com.iris.protocol.zigbee.zcl.Basic.clientSerDe(),
      com.iris.protocol.zigbee.zcl.Color.clientSerDe(),
      //com.iris.protocol.zigbee.zcl.Dehumidification.clientSerDe(),
      //com.iris.protocol.zigbee.zcl.DeviceTemperature.clientSerDe(),
      com.iris.protocol.zigbee.zcl.DoorLock.clientSerDe(),
      //com.iris.protocol.zigbee.zcl.Fan.clientSerDe(),
      //com.iris.protocol.zigbee.zcl.FlowMeasurement.clientSerDe(),
      com.iris.protocol.zigbee.zcl.Groups.clientSerDe(),
      //com.iris.protocol.zigbee.zcl.HumidityMeasurement.clientSerDe(),
      com.iris.protocol.zigbee.zcl.IasAce.clientSerDe(),
      com.iris.protocol.zigbee.zcl.IasWd.clientSerDe(),
      com.iris.protocol.zigbee.zcl.IasZone.clientSerDe(),
      com.iris.protocol.zigbee.zcl.Identify.clientSerDe(),
      //com.iris.protocol.zigbee.zcl.IlluminanceMeasurement.clientSerDe(),
      //com.iris.protocol.zigbee.zcl.IlluminanceSensing.clientSerDe(),
      com.iris.protocol.zigbee.zcl.Level.clientSerDe(),
      //com.iris.protocol.zigbee.zcl.Metering.clientSerDe(),
      //com.iris.protocol.zigbee.zcl.OccupancySensing.clientSerDe(),
      com.iris.protocol.zigbee.zcl.OnOff.clientSerDe(),
      //com.iris.protocol.zigbee.zcl.OnOffSwitch.clientSerDe(),
      //com.iris.protocol.zigbee.zcl.Power.clientSerDe(),
      //com.iris.protocol.zigbee.zcl.PressureMeasurement.clientSerDe(),
      //com.iris.protocol.zigbee.zcl.Pump.clientSerDe(),
      com.iris.protocol.zigbee.zcl.Scenes.clientSerDe(),
      //com.iris.protocol.zigbee.zcl.Shade.clientSerDe(),
      //com.iris.protocol.zigbee.zcl.TemperatureMeasurement.clientSerDe(),
      com.iris.protocol.zigbee.zcl.Thermostat.clientSerDe(),
      //com.iris.protocol.zigbee.zcl.ThermostatUI.clientSerDe(),
      //com.iris.protocol.zigbee.zcl.Time.clientSerDe(),
   };

   /////////////////////////////////////////////////////////////////////////////
   // Protocol API
   /////////////////////////////////////////////////////////////////////////////

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
   public Serializer<ZigbeeMessage.Protocol> createSerializer() {
      return ZigbeeSerializer.INSTANCE;
   }

   @Override
   public Deserializer<ZigbeeMessage.Protocol> createDeserializer() {
      return ZigbeeDeserializer.INSTANCE;
   }

   @Override
   public PlatformMessage remove(RemoveProtocolRequest req) {
      return Protocols.removeHubDevice(NAMESPACE, req);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Helper methods for working with Zigbee protocol messages
   /////////////////////////////////////////////////////////////////////////////

   public static boolean isZdp(ZigbeeMessage.Protocol msg) {
      return msg.getType() == ZigbeeMessage.Zdp.ID;
   }

   public static boolean isZcl(ZigbeeMessage.Protocol msg) {
      return msg.getType() == ZigbeeMessage.Zcl.ID;
   }

   public static ZigbeeMessage.Zcl getZclMessage(ZigbeeMessage.Protocol msg) {
      if (msg.getType() != ZigbeeMessage.Zcl.ID) {
         throw new IllegalArgumentException("Zigbee protocol message does not contain a Zcl message");
      }
      try {
         return ZigbeeMessage.Zcl.serde().nioSerDe().decode(ByteBuffer.wrap(msg.getPayload()).order(ByteOrder.LITTLE_ENDIAN));
      } catch (IOException e) {
         throw new RuntimeException("IO Exception while attempting to deserialize Zcl message.", e);
      }
   }

   public static ZigbeeMessage.Zdp getZdpMessage(ZigbeeMessage.Protocol msg) {
      if (msg.getType() != ZigbeeMessage.Zdp.ID) {
         throw new IllegalArgumentException("Zigbee protocol message does not contain a Zdp message");
      }
      try {
         return ZigbeeMessage.Zdp.serde().nioSerDe().decode(ByteBuffer.wrap(msg.getPayload()).order(ByteOrder.LITTLE_ENDIAN));
      } catch (IOException e) {
         throw new RuntimeException("IO Exception while attempting to deserialize Zdp message.", e);
      }
   }

   public static ZigbeeMessage.SetOfflineTimeout getSetOfflineTimeoutMessage(ZigbeeMessage.Protocol msg) {
      if(msg.getType() != ZigbeeMessage.SetOfflineTimeout.ID) {
         throw new IllegalArgumentException("Zigbee protocol message does not contain a SetOfflineMessage");
      }
      try {
         return ZigbeeMessage.SetOfflineTimeout.serde().nioSerDe().decode(ByteBuffer.wrap(msg.getPayload()).order(ByteOrder.LITTLE_ENDIAN));
      } catch(IOException e) {
         throw new RuntimeException("IO Exception while attempting to deserialize SetOfflineTimeout message", e);
      }
   }

   public static ZigbeeMessage.Control getControlMessage(ZigbeeMessage.Protocol msg) {
      if(msg.getType() != ZigbeeMessage.Control.ID) {
         throw new IllegalArgumentException("Zigbee protocol message does not contain a Control message");
      }

      try {
         return ZigbeeMessage.Control.serde().nioSerDe().decode(ByteBuffer.wrap(msg.getPayload()).order(ByteOrder.LITTLE_ENDIAN));
      } catch(IOException e) {
         throw new RuntimeException("IO Exception while attempting to deserialize Control message", e);
      }
   }

   public static ZigbeeMessage.Protocol packageMessage(ProtocMessage message) {
      return packageMessage(message.getMessageId(), message);
   }

   public static ZigbeeMessage.Protocol packageMessage(ZigbeeMessage.Zcl zclMessage) {
      return packageMessage(ZigbeeMessage.Zcl.ID, zclMessage);
   }

   public static ZigbeeMessage.Protocol packageMessage(ZigbeeMessage.Zdp zdpMessage) {
      return packageMessage(ZigbeeMessage.Zdp.ID, zdpMessage);
   }

   public static ZigbeeMessage.Protocol packageMessage(ZigbeeMessage.SetOfflineTimeout setOfflineTimeoutMessage) {
      return packageMessage(ZigbeeMessage.SetOfflineTimeout.ID, setOfflineTimeoutMessage);
   }

   public static ZigbeeMessage.Protocol packageMessage(ZigbeeMessage.Control controlMessage) {
      return packageMessage(ZigbeeMessage.Control.ID, controlMessage);
   }

   private static ZigbeeMessage.Protocol packageMessage(int type, ProtocMessage message) {
      ZigbeeMessage.Protocol.Builder builder = ZigbeeMessage.Protocol.builder();
      try {
         return builder
            .setType(type)
            .setPayload(ByteOrder.LITTLE_ENDIAN, message)
            .create();
      } catch (IOException e) {
         throw new RuntimeException("IO Exception while trying to serialize a zigbee message", e);
      }
   }

   public static ProtocSerDe<? extends ProtocMessage> getZclGeneralSerDe(ZigbeeMessage.Zcl msg) {
      ProtocSerDe<? extends ProtocMessage> result = com.iris.protocol.zigbee.zcl.General.serde().serde(msg.getZclMessageId());
      if (result != null) {
         return result;
      }

      throw new RuntimeException("no serde exists for message: " + msg);
   }

   public static ProtocSerDe<? extends ProtocMessage> getZclServerSerDe(ZigbeeMessage.Zcl msg) {
      ProtocSerDe<? extends ProtocMessage> result = getSerDe(ZCL_SERVER_SERDES, msg.getZclMessageId());
      if (result != null) {
         return result;
      }

      throw new RuntimeException("no serde exists for message: " + msg);
   }

   public static ProtocSerDe<? extends ProtocMessage> getZclClientSerDe(ZigbeeMessage.Zcl msg) {
      ProtocSerDe<? extends ProtocMessage> result = getSerDe(ZCL_CLIENT_SERDES, msg.getZclMessageId());
      if (result != null) {
         return result;
      }

      throw new RuntimeException("no serde exists for message: " + msg);
   }

   public static ProtocSerDe<? extends ProtocMessage> zclSerDe(ZigbeeMessage.Zcl msg) {
      int flags = msg.getFlags();

      // If the cluster specific flag is not set then the message
      // has to be one of the ZCL general command frames.
      if ((flags & ZigbeeMessage.Zcl.CLUSTER_SPECIFIC) == 0) {
         return getZclGeneralSerDe(msg);
      }

      // If the from server flag is set then this message is from the server side
      // of a cluster, otherwise it is from the client side of the cluster.
      if ((flags & ZigbeeMessage.Zcl.FROM_SERVER) != 0) {
         return getZclServerSerDe(msg);
      } else {
         return getZclClientSerDe(msg);
      }
   }

   public static ProtocSerDe<? extends ProtocMessage> zdpSerDe(ZigbeeMessage.Zdp msg) {
      ProtocSerDe<? extends ProtocMessage> result = getSerDe(ZDP_SERDES, msg.getZdpMessageId());
      if (result != null) {
         return result;
      }

      throw new RuntimeException("no serde exists for message: " + msg);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Implementation details
   /////////////////////////////////////////////////////////////////////////////

   @Nullable
   private static ProtocSerDe<? extends ProtocMessage> getSerDe(ProtocMessageSerDe[] serdes, int id) {
      for (ProtocMessageSerDe serde : serdes) {
         ProtocSerDe<? extends ProtocMessage> result = serde.serde(id);
         if (result != null) {
            return result;
         }
      }

      return null;
   }

   private static enum ZigbeeSerializer implements Serializer<ZigbeeMessage.Protocol> {
      INSTANCE;

      @Override
      public byte[] serialize(ZigbeeMessage.Protocol value) {
         try {
            return value.toBytes(ByteOrder.LITTLE_ENDIAN);
         } catch (Exception e) {
            throw new RuntimeException("IO Exception while serializing zigbee message", e);
         }
      }

      @Override
      public void serialize(ZigbeeMessage.Protocol value, OutputStream out) throws IOException, IllegalArgumentException {
         ZigbeeMessage.Protocol.serde().ioSerDe().encode(new DataOutputStream(out), value);
      }
   }

   private static enum ZigbeeDeserializer implements Deserializer<ZigbeeMessage.Protocol> {
      INSTANCE;

      @Override
      public ZigbeeMessage.Protocol deserialize(byte[] input) {
         try {
            return ZigbeeMessage.Protocol.serde().nioSerDe().decode(ByteBuffer.wrap(input).order(ByteOrder.LITTLE_ENDIAN));
         } catch (Exception e) {
            throw new RuntimeException("IO Exception while deserializing zigbee message", e);
         }
      }

      @Override
      public ZigbeeMessage.Protocol deserialize(InputStream input) throws IOException, IllegalArgumentException {
         return ZigbeeMessage.Protocol.serde().ioSerDe().decode(new DataInputStream(input));
      }
   }

   @Override
   public boolean isTransientAddress() {
      return false;
   }
}

