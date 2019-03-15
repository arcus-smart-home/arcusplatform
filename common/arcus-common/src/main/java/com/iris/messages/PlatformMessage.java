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
package com.iris.messages;

import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.iris.Utils;
import com.iris.io.Deserializer;
import com.iris.io.Serializer;
import com.iris.io.json.JSON;
import com.iris.messages.address.Address;

public class PlatformMessage extends Message {

   public static PlatformMessage.Builder builder() {
      return new PlatformMessage.Builder();
   }

   public static PlatformMessage.Builder builder(PlatformMessage message) {
      Preconditions.checkNotNull(message, "message may not be null");
      return new Builder(message);
   }

   public static PlatformMessage.Builder builder(PlatformMessage.Builder builder) {
      Preconditions.checkNotNull(builder, "builder may not be null");
      return builder.clone();
   }

   public static PlatformMessage.Builder broadcast() {
      return builder().broadcast();
   }

   /**
    * Creates a broadcast with the 'context' as the root cause, will
    * copy the appropriate headers like actor.
    * @param context
    * @return
    */
   public static PlatformMessage.Builder broadcast(PlatformMessage context) {
      return 
         builder()
            .broadcast()
            .withPlaceId(context.getPlaceId())
            .withPopulation(context.getPopulation())
            .withActor(context.getActor())
            ;
   }

   public static PlatformMessage.Builder request(Address destination) {
      return builder().isRequestMessage(true).to(destination);
   }
   
   public static PlatformMessage.Builder respondTo(PlatformMessage request) {
      return builder()
         .from(request.getDestination())
         .to(request.getSource())
         .withCorrelationId(request.getCorrelationId())
         .withPlaceId(request.getPlaceId())
         .withPopulation(request.getPopulation())
         .withActor(request.getActor())
         ;
   }

   public static PlatformMessage.Builder buildRequest(String messageType, Map<String,Object> requestAttributes, Address source, Address destination) {
      return buildMessage(MessageBody.buildMessage(messageType, requestAttributes), source, destination).isRequestMessage(true);
   }

   public static PlatformMessage.Builder buildEvent(String messageType, Map<String,Object> requestAttributes, Address source) {
      return buildBroadcast(MessageBody.buildMessage(messageType, requestAttributes), source).isRequestMessage(false);
   }

   public static PlatformMessage.Builder buildEvent(MessageBody event, Address source) {
      return buildBroadcast(event, source).isRequestMessage(false);
   }
   
   public static PlatformMessage.Builder buildEvent(PlatformMessage context, MessageBody event) {
      return buildBroadcast(context, event).isRequestMessage(false);
   }
   
   public static PlatformMessage.Builder buildEvent(PlatformMessage context, String event) {
	   return buildBroadcast(context, MessageBody.messageBuilder(event).create()).isRequestMessage(false);
   }
   
   public static PlatformMessage.Builder buildEvent(PlatformMessage context, String event, Map<String,Object> attributes) {
	   return buildBroadcast(context, MessageBody.buildMessage(event, attributes)).isRequestMessage(false);	   
   }

   public static PlatformMessage.Builder buildResponse(PlatformMessage request, Map<String,Object> responseAttributes) {
      return buildResponse(request, MessageBody.buildResponse(request.getValue(), responseAttributes));
   }

   public static PlatformMessage.Builder buildResponse(PlatformMessage request, Map<String,Object> responseAttributes, Address source) {
      return buildResponse(request, MessageBody.buildResponse(request.getValue(), responseAttributes), source);
   }

   public static PlatformMessage.Builder buildRequest(MessageBody payload, Address source, Address destination) {
      return buildMessage(payload, source, destination).isRequestMessage(true);
   }

   public static PlatformMessage.Builder buildMessage(MessageBody payload, Address source, Address destination) {
      return
            builder()
               .from(source)
               .to(destination)
               .withPayload(payload);
   }

   public static PlatformMessage.Builder buildBroadcast(MessageBody payload, Address source) {
      return
            builder()
               .from(source)
               .broadcast()
               .withPayload(payload);
   }
   
   public static PlatformMessage.Builder buildBroadcast(PlatformMessage request, MessageBody payload) {
      return
    		  respondTo(request)
               .broadcast()
               .withPayload(payload);
   }

   public static PlatformMessage.Builder buildResponse(PlatformMessage request, MessageBody response) {
      Utils.assertNotNull(request, "request may not be null");
      return respondTo(request).withPayload(response);
   }

   public static PlatformMessage.Builder buildResponse(PlatformMessage request, MessageBody response, Address source) {
      Utils.assertNotNull(request, "request may not be null");
      return buildResponse(request, response).from(source);
   }

   public static PlatformMessage create(
         MessageBody payload,
         Address source, Address destination, String correlationId
   ) {
      return
            builder()
               .from(source)
               .to(destination)
               .withCorrelationId(correlationId)
               .withPayload(payload)
               .create();
   }

   public static PlatformMessage createMessage(MessageBody payload, Address source, Address destination) {
      return buildMessage(payload, source, destination).create();
   }

   public static PlatformMessage createBroadcast(MessageBody payload, Address source) {
      return buildBroadcast(payload, source).create();
   }

   public static PlatformMessage createResponse(PlatformMessage request, MessageBody response) {
      return buildResponse(request, response).create();
   }

   public static PlatformMessage createResponse(PlatformMessage request, MessageBody response, Address source) {
      return buildResponse(request, response, source).create();
   }

   public static Serializer<MessageBody> getSerializer(String type) {
      return LazyInitializer.SERIALIZER;
   }

   public static Deserializer<MessageBody> getDeserializer(String type) {
      return LazyInitializer.DESERIALIZER;
   }

   private final MessageBody value;

   PlatformMessage(Builder builder) {
      super(builder, true);
      this.value = builder.value;
   }

   @Override
   public MessageBody getValue() {
      return value;
   }

   @Override
   public byte[] computeBuffer() {
      if (value == null) {
         return Utils.EMPTY_BYTE_ARRAY;
      }

      return PlatformMessage.getSerializer(getMessageType()).serialize(value);
   }

   /**
    * Determines if a message requires a response. This method will return
    * true iff all of the following conditions are met:
    *    * The message is a request.
    *    * The message has a correlation id
    *
    * @return True iff the message requires a response.
    */
   public boolean isResponseRequired() {
      return this.isRequest() && !this.isError() && this.getCorrelationId() != null;
   }

   public static class Builder extends Message.Builder<Builder> {
      private MessageBody value;

      Builder() { }
      
      Builder(PlatformMessage message) {
      	super.populate(message);
      	value = message.getValue();
      }

      @Override
      protected Builder populate(Message message) {
         super.populate(message);
         if (PlatformMessage.class == message.getClass()) {
            value = ((PlatformMessage) message).getValue();
         }

         return this;
      }
      
      @Override
      public Builder withPayload(String type, byte[] buffer) {
         this.withPayload(PlatformMessage.getDeserializer(type).deserialize(buffer));
         return super.withPayload(type,buffer);
      }

      public Builder withPayload(MessageBody payload) {
         this.type = payload.getMessageType();
         this.value = payload;
         isErrorMessage(ErrorEvent.MESSAGE_TYPE.equals(this.type));
         return this;
      }
      
      public Builder withPayload(String eventName) {
         MessageBody mb = MessageBody.buildMessage(eventName);
         return withPayload(mb);
      }

      public Builder withPayload(String eventName, Map<String, Object> attributes) {
         MessageBody mb = MessageBody.buildMessage(eventName, attributes);
         return withPayload(mb);
      }

      @Deprecated
      public <T> Builder withPayload(String type, Serializer<T> serializer, T payload) {
         this.value = null;
         isErrorMessage(ErrorEvent.MESSAGE_TYPE.equals(type));
         // contract with serializer is that a new buffer is returned so unsafe is safe here
         withPayloadUnsafe(type, serializer.serialize(payload));
         return this;
      }

      public PlatformMessage createExpired(String type) {
         this.type = type;
         Utils.assertNotNull(source, "Must specify a source");
         Utils.assertFalse(source.isBroadcast(), "Source address may not be the broadcast address");
         Utils.assertNotNull(type, "Must specify a type");
         return new PlatformMessage(this);
      }

      public PlatformMessage create() {
         Utils.assertNotNull(source, "Must specify a source");
         Utils.assertFalse(source.isBroadcast(), "Source address may not be the broadcast address");
         Utils.assertNotNull(type, "Must specify a type");
         Utils.assertTrue(value != null, "Must specify a payload");

         if(timestamp <= 0) {
            timestamp = System.currentTimeMillis();
         }

         return new PlatformMessage(this);
      }

   }
   
   private static class LazyInitializer {
      private static final Serializer<MessageBody> SERIALIZER = JSON.createSerializer(MessageBody.class);
      private static final Deserializer<MessageBody> DESERIALIZER = JSON.createDeserializer(MessageBody.class);
   }

   @Override
   public String toString() {
      return "PlatformMessage [value=" + value + ", [" + super.toString() + "]]";
   }

   
}

