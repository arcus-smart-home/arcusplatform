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
package com.iris.protocol.zwave;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.iris.capability.definition.ProtocolDefinition;
import com.iris.io.Deserializer;
import com.iris.io.Serializer;
import com.iris.messages.PlatformMessage;
import com.iris.protocol.Protocols;
import com.iris.protocol.RemoveProtocolRequest;
import com.iris.protocol.constants.ZwaveConstants;
import com.iris.protocol.zwave.message.ZWaveCommandMessage;
import com.iris.protocol.zwave.message.ZWaveDelayedCommandsMessage;
import com.iris.protocol.zwave.message.ZWaveMessage;
import com.iris.protocol.zwave.message.ZWaveNodeInfoMessage;
import com.iris.protocol.zwave.message.ZWaveOrderedCommandsMessage;
import com.iris.protocol.zwave.message.controller.ZWaveScheduleMessage;
import com.iris.protocol.zwave.message.controller.ZWaveSetOfflineTimeoutMessage;
import com.iris.protocol.zwave.model.ZWaveAllCommandClasses;
import com.iris.protocol.zwave.model.ZWaveCommand;
import com.iris.protocol.zwave.model.ZWaveCommandClass;
import com.iris.protocol.zwave.model.ZWaveNode;

import io.netty.buffer.Unpooled;

public enum ZWaveProtocol implements com.iris.protocol.Protocol<ZWaveMessage>, ZwaveConstants {
   INSTANCE;

   private static final Logger log = LoggerFactory.getLogger(ZWaveProtocol.class);

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
	public Serializer<ZWaveMessage> createSerializer() {
		return ZWaveSerializer.INSTANCE;
	}

	@Override
	public Deserializer<ZWaveMessage> createDeserializer() {
		return ZWaveDeserializer.INSTANCE;
	}

	private static Protocol.Message[] serializeProtocolMessages(List<ZWaveMessage> messages) throws IOException {
	   int i = 0;
	   Protocol.Message[] results = new Protocol.Message[messages.size()];
	   for (ZWaveMessage msg : messages) {
	      results[i++] = ZWaveSerializer.INSTANCE.mux(msg);
	   }

	   return results;
	}

	private static List<ZWaveMessage> deserializeProtocolMessages(Protocol.Message[] messages) throws IOException {
	   List<ZWaveMessage> results = new ArrayList<>(messages.length);
	   for (Protocol.Message msg : messages) {
	      results.add(ZWaveDeserializer.INSTANCE.demux(msg));
	   }

	   return results;
	}

	private static Protocol.Message serializeCommand(ZWaveCommandMessage cmd) throws IOException {
	   ZWaveNode node = cmd.getDevice();
	   ZWaveCommand zcmd = cmd.getCommand();

	   Protocol.Command cm = Protocol.Command.builder()
	      .setNodeId(node.getNumber())
	      .setCommandClassId(zcmd.commandClass)
	      .setCommandId(zcmd.commandNumber)
	      .setPayload(zcmd.payload())
	      .create();

      return Protocol.Message.builder()
         .setType(Protocol.Command.ID)
         .setPayload(ByteOrder.BIG_ENDIAN, cm)
         .create();
	}

	private static ZWaveCommandMessage deserializeCommand(Protocol.Message msg) throws IOException {
      Protocol.Command pcmd = Protocol.Command.serde().nettySerDe()
	      .decode(Unpooled.wrappedBuffer(msg.getPayload()));

	   ZWaveCommandClass cc = ZWaveAllCommandClasses.allClasses.get(pcmd.rawCommandClassId());
	   if (cc == null) {
	      throw new IOException("unknown command class: " + pcmd.getCommandClassId());
	   }

	   ZWaveCommand command = cc.get(pcmd.rawCommandId());
	   if (command == null) {
	      throw new IOException("unknown command " + pcmd.getCommandId() + " in command class " + pcmd.getCommandClassId());
	   }

	   if (!command.parsePayload(pcmd.getPayload())) {
	      throw new IOException("could not parse command " + pcmd.getCommandId() + " in command class " + pcmd.getCommandClassId());
	   }

	   ZWaveNode node = new ZWaveNode(pcmd.rawNodeId());
      return new ZWaveCommandMessage(node, command);
   }

	private static Protocol.Message serializeOrderedCommands(ZWaveOrderedCommandsMessage cmd) throws IOException {
	   List<ZWaveMessage> zcmds = cmd.getCommands();

	   Protocol.Message[] ocmds = serializeProtocolMessages(zcmds);
	   Protocol.OrderedCommands cm = Protocol.OrderedCommands.builder()
	      .setPayload(ocmds)
	      .create();

      return Protocol.Message.builder()
         .setType(Protocol.OrderedCommands.ID)
         .setPayload(ByteOrder.BIG_ENDIAN, cm)
         .create();
	}

	private static Protocol.Message serializeDelayedCommands(ZWaveDelayedCommandsMessage cmd) throws IOException {
	   List<ZWaveMessage> zcmds = cmd.getCommands();

	   Protocol.Message[] ocmds = serializeProtocolMessages(zcmds);
	   Protocol.DelayedCommands cm = Protocol.DelayedCommands.builder()
	      .setDelay(cmd.getDelay())
	      .setPayload(ocmds)
	      .create();

      return Protocol.Message.builder()
         .setType(Protocol.DelayedCommands.ID)
         .setPayload(ByteOrder.BIG_ENDIAN, cm)
         .create();
	}

	private static ZWaveOrderedCommandsMessage deserializeOrderedCommands(Protocol.Message msg) throws IOException {
      Protocol.OrderedCommands pcmd = Protocol.OrderedCommands.serde().nettySerDe()
	      .decode(Unpooled.wrappedBuffer(msg.getPayload()));

      List<ZWaveMessage> commands = deserializeProtocolMessages(pcmd.getPayload());

	   ZWaveNode node = new ZWaveNode((byte)0);
      return new ZWaveOrderedCommandsMessage(node, commands);
   }

	private static ZWaveDelayedCommandsMessage deserializeDelayedCommands(Protocol.Message msg) throws IOException {
      Protocol.DelayedCommands pcmd = Protocol.DelayedCommands.serde().nettySerDe()
	      .decode(Unpooled.wrappedBuffer(msg.getPayload()));

      List<ZWaveMessage> commands = deserializeProtocolMessages(pcmd.getPayload());

	   ZWaveNode node = new ZWaveNode((byte)0);
      return new ZWaveDelayedCommandsMessage(node, pcmd.getDelay(), commands);
   }

	private static Protocol.Message serializeNodeInfo(ZWaveNodeInfoMessage nodeInfo) throws IOException {
	   Protocol.NodeInfo ni = Protocol.NodeInfo.builder()
	      .setNodeId(nodeInfo.getNodeId())
	      .setStatus(nodeInfo.getStatus())
	      .setBasic(nodeInfo.getBasic())
	      .setGeneric(nodeInfo.getGeneric())
	      .setSpecific(nodeInfo.getSpecific())
	      .create();

      return Protocol.Message.builder()
         .setType(Protocol.NodeInfo.ID)
         .setPayload(ByteOrder.BIG_ENDIAN, ni)
         .create();
	}

	private static ZWaveNodeInfoMessage deserializeNodeInfo(Protocol.Message msg) throws IOException {
	   Protocol.NodeInfo ni = Protocol.NodeInfo.serde().nettySerDe()
	      .decode(Unpooled.wrappedBuffer(msg.getPayload()));

      return new ZWaveNodeInfoMessage(ni.rawNodeId(), ni.rawStatus(), ni.rawBasic(), ni.rawGeneric(), ni.rawSpecific());
   }

	private static Protocol.Message serializeOfflineTimeout(ZWaveSetOfflineTimeoutMessage offline) throws IOException {
	   Protocol.SetOfflineTimeout ot = Protocol.SetOfflineTimeout.builder()
	      .setNodeId(offline.getNode().getNumber())
	      .setSeconds(offline.getSeconds())
	      .create();

      return Protocol.Message.builder()
         .setType(Protocol.SetOfflineTimeout.ID)
         .setPayload(ByteOrder.BIG_ENDIAN, ot)
         .create();
	}

	private static ZWaveSetOfflineTimeoutMessage deserializeOfflineTimeout(Protocol.Message msg) throws IOException {
	   Protocol.SetOfflineTimeout ot = Protocol.SetOfflineTimeout.serde().nettySerDe()
	      .decode(Unpooled.wrappedBuffer(msg.getPayload()));

	   ZWaveNode node = new ZWaveNode(ot.rawNodeId());
      return new ZWaveSetOfflineTimeoutMessage(node, ot.getSeconds());
   }

	private static Protocol.Message serializeSchedule(ZWaveScheduleMessage sch) throws IOException {
	   ZWaveCommand[] zcmds = sch.getCommands();

      Protocol.Schedule[] schedule = new Protocol.Schedule[zcmds.length];
      for (int i = 0; i < zcmds.length; ++i) {
         schedule[i] = Protocol.Schedule.builder()
            .setPayload(zcmds[i].toBytes())
            .create();
	   }

	   Protocol.SetSchedule ss = Protocol.SetSchedule.builder()
	      .setNodeId(sch.getNode().getNumber())
	      .setSeconds(sch.getSeconds())
	      .setSchedule(schedule)
	      .create();

      return Protocol.Message.builder()
         .setType(Protocol.SetSchedule.ID)
         .setPayload(ByteOrder.BIG_ENDIAN, ss)
         .create();
	}

	private static ZWaveScheduleMessage deserializeSchedule(Protocol.Message msg) throws IOException {
	   throw new IOException("schedule message should not be sent from hub");
   }

   private static enum ZWaveSerializer implements Serializer<ZWaveMessage> {
      INSTANCE;

      private final Serializer<Protocol.Message> external = ZWaveExternalProtocol.INSTANCE.createSerializer();

      @Override
      public byte[] serialize(@Nullable ZWaveMessage value) {
         Preconditions.checkNotNull(value);

         try {
            return external.serialize(mux(value));
         } catch (IOException ex) {
            throw new RuntimeException(ex);
         }
      }

      @Override
      public void serialize(@Nullable ZWaveMessage value, @Nullable OutputStream out) throws IOException, IllegalArgumentException {
         Preconditions.checkNotNull(value);
         external.serialize(mux(value), out);
      }

      private Protocol.Message mux(ZWaveMessage msg) throws IOException {
         if (msg instanceof ZWaveCommandMessage) {
            return serializeCommand((ZWaveCommandMessage)msg);
         } else if (msg instanceof ZWaveOrderedCommandsMessage) {
            return serializeOrderedCommands((ZWaveOrderedCommandsMessage)msg);
         } else if (msg instanceof ZWaveDelayedCommandsMessage) {
            return serializeDelayedCommands((ZWaveDelayedCommandsMessage)msg);
         } else if (msg instanceof ZWaveNodeInfoMessage) {
            return serializeNodeInfo((ZWaveNodeInfoMessage)msg);
         } else if (msg instanceof ZWaveScheduleMessage) {
            return serializeSchedule((ZWaveScheduleMessage)msg);
         } else if (msg instanceof ZWaveSetOfflineTimeoutMessage) {
            return serializeOfflineTimeout((ZWaveSetOfflineTimeoutMessage)msg);
         }

         throw new RuntimeException("cannot serialize zwave message: " + msg);
      }
   }

   private static enum ZWaveDeserializer implements Deserializer<ZWaveMessage> {
      INSTANCE;

      private final Deserializer<Protocol.Message> external = ZWaveExternalProtocol.INSTANCE.createDeserializer();

      @Override
      public ZWaveMessage deserialize(@Nullable byte[] input) {
         try {
            return demux(external.deserialize(input));
         } catch (IOException ex) {
            throw new RuntimeException(ex);
         }
      }

      @Override
      public ZWaveMessage deserialize(@Nullable InputStream input) throws IOException, IllegalArgumentException {
         return demux(external.deserialize(input));
      }

      private ZWaveMessage demux(Protocol.Message msg) throws IOException {
         switch (msg.getType()) {
         case Protocol.Command.ID:
            return deserializeCommand(msg);

         case Protocol.OrderedCommands.ID:
            return deserializeOrderedCommands(msg);

         case Protocol.DelayedCommands.ID:
            return deserializeDelayedCommands(msg);

         case Protocol.NodeInfo.ID:
            return deserializeNodeInfo(msg);

         case Protocol.SetSchedule.ID:
            return deserializeSchedule(msg);

         case Protocol.SetOfflineTimeout.ID:
            return deserializeOfflineTimeout(msg);

         default:
            throw new RuntimeException("cannot deserialize zwave message: " + msg);
         }
      }
   }

   @Override
   public boolean isTransientAddress() {
      return true;
   }

   @Override
   public PlatformMessage remove(RemoveProtocolRequest req) {
      return Protocols.removeHubDevice(NAMESPACE, req);
   }

}

