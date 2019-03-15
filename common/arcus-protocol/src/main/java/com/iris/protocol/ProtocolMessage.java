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
package com.iris.protocol;

import org.apache.commons.codec.binary.Base64;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.iris.Utils;
import com.iris.io.Serializer;
import com.iris.messages.ErrorEvent;
import com.iris.messages.Message;
import com.iris.messages.address.Address;
import com.iris.protocol.control.ControlProtocol;

/**
 * A message that wraps a raw protocol layer message.
 */
public class ProtocolMessage extends Message {
   public static Builder builder() {
      return new Builder();
   }

   public static Builder builder(ProtocolMessage message) {
      Preconditions.checkNotNull(message, "message may not be null");
      return builder().populate(message);
   }

   public static Builder builder(Builder builder) {
      Preconditions.checkNotNull(builder, "builder may not be null");
      return builder.clone();
   }

   public static <T> Builder buildProtocolMessage(
         Address source,
         Address destination,
         Protocol<T> protocol,
         T message
   ) {
      return builder()
               .from(source)
               .to(destination)
               .withPayload(protocol, message);
   }

   public static <T> Builder buildProtocolMessage(
         Address source,
         Address destination,
         String protocolName,
         byte [] payload
   ) {
      return builder()
               .from(source)
               .to(destination)
               .withPayload(protocolName, payload);
   }

	/**
	 * Creates a new protocol message to be broadcast,
	 * i.e. no specific destination.  Generally these are
	 * sent from the device when a real-world action happens.
	 * @param clientId
	 * 	A source unique id.  This does not need to be globally unique,
	 * 	but the combination of source and client id should be.
	 * @param source
	 * 	The source that generated the event, a message must always have
	 * 	a source.
	 * @param protocol
	 * 	The protocol the message uses.
	 * @param message
	 * 	The message to encode.
	 * @return
	 * 	A message that may be submitted to the message bus.
	 */
	public static <T> ProtocolMessage createProtocolMessage(
			Address source,
			Address destination,
			Protocol<T> protocol,
			T message
	) {
		return buildProtocolMessage(source, destination, protocol, message).create();
	}

	/**
	 * Creates a new protocol message to be broadcast,
	 * i.e. no specific destination.  Generally these are
	 * sent from the device when a real-world action happens.
	 * @param source
	 * @param protocolName
	 * @param payload
	 * @return
	 */
	public static ProtocolMessage createProtocolMessage(
			Address source,
			Address destination,
			String protocolName,
			byte[] payload
	) {
		return buildProtocolMessage(source, destination, protocolName, payload).create();
	}

	/**
	 * @deprecated Use {@link #builder()} instead.
	 * @param source
	 * @param destination
	 * @param isError
	 * @param type
	 * @param timestamp
	 * @param timeToLive
	 * @param correlationId
	 * @param buffer
	 */
   @Deprecated
   public ProtocolMessage(
         Address source, Address destination, boolean isError, String type, long timestamp,
         int timeToLive, String correlationId, byte[] buffer) {
      this(
            builder()
               .to(source)
               .from(destination)
               .isErrorMessage(isError)
               .withTimestamp(timestamp)
               .withTimeToLive(timeToLive)
               .withCorrelationId(correlationId)
               .withPayload(type, buffer)
      );
   }

   private Object value;
   private String encoded;
   private Integer reflexVersion;

   ProtocolMessage(Builder builder) {
      super(builder, false);
      this.encoded = builder.encoded;
      this.reflexVersion = builder.reflexVersion;
   }

   @Override
   public Object getValue() {
      if (value != null) {
         return value;
      }

      Object result = Protocols.getProtocolByName(getMessageType())
         .createDeserializer()
          .deserialize(getBuffer());

      this.value = result;
      return result;
   }

   public Integer getReflexVersion() {
      return reflexVersion;
   }

   @Override
   public byte[] computeBuffer() {
      if (encoded == null) {
         return null;
      }

      return Base64.decodeBase64(encoded);
   }

   public <M> M getValue(Protocol<M> protocol) {
      if (value != null) {
         return (M)value;
      }

      M result = protocol.createDeserializer()
               .deserialize(getBuffer());

      this.value = result;
      return result;
   }

   public String getEncodedPayloadIfExists() {
      return encoded;
   }

   public static class Builder extends Message.Builder<Builder> {
      private String encoded;
      private Integer reflexVersion;
      Builder() { }

      @Override
      public Builder withPayload(String type, byte[] buffer) {
         this.encoded = null;
         return super.withPayload(type,buffer);
      }

      public <T> Builder withPayload(Protocol<T> protocol, T payload) {
         Utils.assertNotNull(protocol, "protocol may not be null");
         Serializer<T> serializer = protocol.createSerializer();
         return withPayload(protocol.getNamespace(), serializer, payload);
      }

      public <T> Builder withPayload(String protocolName, Serializer<T> serializer, T payload) {
         this.encoded = null;
         // contract with serializer is that a new buffer is returned so unsafe is safe here
         return withPayloadUnsafe(protocolName, serializer.serialize(payload));
      }

      public Builder withBase64EncodedPayload(String type, String encoded) {
         this.type = type;
         this.encoded = encoded;
         return this;
      }

      public Builder withReflexVersion(@Nullable Integer version) {
         this.reflexVersion = version;
         return this;
      }

      public ProtocolMessage createExpired(String type) {
         this.type = type;

         Utils.assertNotNull(source, "Must specify a source");
         Utils.assertNotNull(type, "Must specify a type");

         return new ProtocolMessage(this);
      }

      public ProtocolMessage create() {
         Utils.assertNotNull(source, "Must specify a source");
         Utils.assertNotNull(type, "Must specify a type");
         Utils.assertTrue(buffer != null || encoded != null, "Must specify a payload");

         if(timestamp <= 0) {
            timestamp = System.currentTimeMillis();
         }

         return new ProtocolMessage(this);
      }

      // bring into scope so that this method may be accessed internally
      /* (non-Javadoc)
       * @see com.iris.messages.Message.Builder#populate(com.iris.messages.Message)
       */
      @Override
      protected Builder populate(Message message) {
         super.populate(message);
         if (ProtocolMessage.class == message.getClass()) {
            ProtocolMessage pmsg = (ProtocolMessage)message;
            this.encoded = pmsg.getEncodedPayloadIfExists();
            this.reflexVersion = pmsg.getReflexVersion();
         }

         return this;
      }

      /* (non-Javadoc)
       * @see java.lang.Object#clone()
       */
      @Override
      protected Builder clone() {
         return super.clone();
      }

   }
}

