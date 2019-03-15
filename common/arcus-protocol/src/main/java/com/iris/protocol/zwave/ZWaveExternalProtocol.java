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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.iris.capability.definition.ProtocolDefinition;
import com.iris.io.Deserializer;
import com.iris.io.Serializer;
import com.iris.messages.PlatformMessage;
import com.iris.protocol.Protocols;
import com.iris.protocol.RemoveProtocolRequest;
import com.iris.protocol.constants.ZwaveConstants;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import com.iris.protocol.zwave.Protocol;

public enum ZWaveExternalProtocol implements com.iris.protocol.Protocol<Protocol.Message>, ZwaveConstants {
   INSTANCE;

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
	public Serializer<Protocol.Message> createSerializer() {
		return ZWaveSerializer.INSTANCE;
	}

	@Override
	public Deserializer<Protocol.Message> createDeserializer() {
		return ZWaveDeserializer.INSTANCE;
	}

   private static enum ZWaveSerializer implements Serializer<Protocol.Message> {
      INSTANCE;

      @Override
      public byte[] serialize(@Nullable Protocol.Message msg) {
         Preconditions.checkNotNull(msg);

         try {
            ByteBuf buf = Unpooled.buffer(64).order(ByteOrder.BIG_ENDIAN);
            Protocol.Message.serde().nettySerDe().encode(buf,msg);

            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);

            return data;
         } catch (IOException ex) {
            throw new RuntimeException(ex);
         }
      }

      @Override
      public void serialize(@Nullable Protocol.Message msg, @Nullable OutputStream out) throws IOException, IllegalArgumentException {
         Protocol.Message.serde().ioSerDe().encode(new DataOutputStream(out), msg);
      }
   }

   private static enum ZWaveDeserializer implements Deserializer<Protocol.Message> {
      INSTANCE;

      @Override
      public Protocol.Message deserialize(@Nullable byte[] input) {
         return Protocol.Message.serde().fromBytes(ByteOrder.BIG_ENDIAN, input);
      }

      @Override
      public Protocol.Message deserialize(@Nullable InputStream input) throws IOException, IllegalArgumentException {
         return Protocol.Message.serde().ioSerDe().decode(new DataInputStream(input));
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

