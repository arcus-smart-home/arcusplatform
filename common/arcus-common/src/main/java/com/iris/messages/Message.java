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
package com.iris.messages;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.MDC;

import com.iris.messages.address.Address;
import com.iris.util.MdcContext;
import com.iris.util.MdcContext.MdcContextReference;

/**
 *
 */
public abstract class Message implements Serializable {
   
   public static MdcContextReference captureAndInitializeContext(Message message) {
      MdcContextReference context = MdcContext.captureMdcContext();
      Message m = (Message) message;
      MDC.put(MdcContext.MDC_PLACE, m.getPlaceId());
      MDC.put(MdcContext.MDC_FROM,  m.getSource().getRepresentation());
      MDC.put(MdcContext.MDC_TO,    m.getDestination() == null || m.getDestination().isBroadcast() ? "[broadcast]" : m.getDestination().getRepresentation());
      MDC.put(MdcContext.MDC_BY,    m.getActor() == null ? "[unknown]" : m.getActor().getRepresentation());
      MDC.put(MdcContext.MDC_ID,    m.getCorrelationId());
      MDC.put(MdcContext.MDC_TYPE,  m.getMessageType());
      return context;
   }
   
   private final Address source;
   private final Address destination;
   private final boolean isPlatform;
   private final boolean isRequest;
   private final boolean isError;
   private final String typeName;
   private final Long clientTime;
   private final long timestamp;
   private final int timeToLive;
   private final String correlationId;
   private final String placeId;
   private final String population;
   private final Address actor;
   private final byte[] buffer;

   protected Message(Builder<? extends Builder<?>> builder, boolean isPlatform) {
      this.source = builder.source;
      this.destination = builder.destination;
      this.isPlatform = isPlatform;
      this.isRequest = builder.isRequestMessage;
      this.isError = builder.isError;
      this.typeName = builder.type;
      this.clientTime = builder.clientTime;
      this.timestamp = builder.timestamp;
      this.timeToLive = builder.timeToLiveMs;
      this.correlationId = builder.correlationId;
      this.placeId = builder.placeId;
      this.population = builder.population;
      this.actor = builder.actor;
      this.buffer = builder.buffer;
   }

   /**
    * The sender of the message, this often
    * implies security or context restrictions.
    * @return
    */
   public Address getSource() {
      return source;
   }

   /**
    * The destination of the message.
    * When this is set to broadcast it indicates
    * the message is a generic event, like
    * base:Added or base:ValueChange.
    * When this is set to a specific location
    * it indicates the message is a request
    * or a response.
    * @return
    */
   public Address getDestination() {
      return destination;
   }

   /**
    * Whether or not this is a platform message.
    * If its not a platform message, its a protocol message.
    * @return
    */
   public boolean isPlatform() {
      return isPlatform;
   }

   /**
    * Whether this is a request or not.  A request generally
    * expects a response.
    * If this is {@code true} the handler should notify the
    * caller about any errors or results of the request.
    * If this is {@code false} the handler should NEVER generate
    * an event in response, as it can lead to event loops.
    * @return
    */
   public boolean isRequest() {
      return isRequest;
   }

   /**
    * Indicates this is an error message.  Errors are
    * always a response to a request.
    * @return
    */
   public boolean isError() {
      return isError;
   }

   /**
    * The type of message, lets the handler know how to
    * interpret the payload.
    * @return
    */
   public String getMessageType() {
      return typeName;
   }

   public Date getClientTime() {
      return clientTime == null ? null : new Date(clientTime);
   }

   /**
    * The time this message first entered into the
    * platform.
    * @return
    */
   public Date getTimestamp() {
      return new Date(timestamp);
   }

   /**
    * The time (in milliseconds) this message
    * should be valid for processing.
    * @return
    */
   public int getTimeToLive() {
      return timeToLive;
   }

   /**
    * The correlation id.  When {@code isRequest() == true}
    * and a correlation id is set, then the handler should always
    * return an event with the same correlation id (a response).
    * Note that requests for which correlation id is not sent, may
    * or may not send a response.
    * @return
    */
   public String getCorrelationId() {
      return correlationId;
   }

   /**
    * All messages generated by clients, hubs, drivers, rules, and subsystems
    * should populate placeId.  Generally this will be done by the bridges and used
    * for authorization.
    * Some administrative messages may not have a placeId.
    * @return
    */
   public String getPlaceId() {
      return placeId;
   }

   public Address getActor() {
      return actor;
   }

   /**
    * The payload as a raw byte-buffer, this is generally used
    * when passing the message from one system to another without
    * interpreting the contents.
    * @return
    */
   public byte[] getBuffer() {
      if (buffer == null) {
         return computeBuffer();
      }
      else {
      	return buffer;
      }
   }

   public boolean isExpired() {
      return isExpired(0);
   }

   public boolean isExpired(long defaultTimeoutMs) {
      return isExpired(defaultTimeoutMs, System.currentTimeMillis(), timestamp, timeToLive);
   }

   public static boolean isExpired(long defaultTimeoutMs, long now, long timestamp, int timeToLive) {
      long timeout = timeToLive < 0 ? defaultTimeoutMs : timeToLive;
      if(timeout == 0) {
         return false;
      }
      return now > (timestamp + timeout);
   }
   
   public String getPopulation() {
		return population;
	}

   /**
    * Parses the buffer into a value.  The message type is generally
    * used to determine the final type.
    * @return
    */
   public abstract Object getValue();
   
   /**
    * Parses the value into a buffer.  The message type is generally
    * used to determine the final buffer.
    * @return
    */
   protected abstract byte[] computeBuffer();

   @Override
   public String toString() {
      StringBuilder bld = new StringBuilder();
      bld.append("Message [source=").append(source);

      if (destination != null && !destination.isBroadcast()) {
         bld.append(",destination=").append(destination);
      }
      
      bld.append(",type=").append(typeName);

      if (timeToLive >= 0) {
         bld.append(",timeToLive=").append(timeToLive);
      }

      if (correlationId != null) {
         bld.append(",correlationId=").append(correlationId);
      }

      if (typeName.equals("Error")) {
         bld.append(",error=").append(new String(getBuffer()));
      }
      
      bld.append("]");
      return bld.toString();
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((actor == null) ? 0 : actor.hashCode());
      result = prime * result + Arrays.hashCode(buffer);
      result = prime * result
            + ((clientTime == null) ? 0 : clientTime.hashCode());
      result = prime * result
            + ((correlationId == null) ? 0 : correlationId.hashCode());
      result = prime * result
            + ((destination == null) ? 0 : destination.hashCode());
      result = prime * result + (isError ? 1231 : 1237);
      result = prime * result + (isPlatform ? 1231 : 1237);
      result = prime * result + (isRequest ? 1231 : 1237);
      result = prime * result + ((placeId == null) ? 0 : placeId.hashCode());
      result = prime * result + ((population == null) ? 0 : population.hashCode());
      result = prime * result + ((source == null) ? 0 : source.hashCode());
      result = prime * result + timeToLive;
      result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
      result = prime * result + ((typeName == null) ? 0 : typeName.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      Message other = (Message) obj;
      if (actor == null) {
         if (other.actor != null)
            return false;
      } else if (!actor.equals(other.actor))
         return false;
      if (!Arrays.equals(buffer, other.buffer))
         return false;
      if (clientTime == null) {
         if (other.clientTime != null)
            return false;
      } else if (!clientTime.equals(other.clientTime))
         return false;
      if (correlationId == null) {
         if (other.correlationId != null)
            return false;
      } else if (!correlationId.equals(other.correlationId))
         return false;
      if (destination == null) {
         if (other.destination != null)
            return false;
      } else if (!destination.equals(other.destination))
         return false;
      if (isError != other.isError)
         return false;
      if (isPlatform != other.isPlatform)
         return false;
      if (isRequest != other.isRequest)
         return false;
      if (placeId == null) {
         if (other.placeId != null)
            return false;
      } else if (!placeId.equals(other.placeId))
         return false;
      if (population == null) {
         if (other.population != null)
            return false;
      } else if (!population.equals(other.population))
         return false;
      if (source == null) {
         if (other.source != null)
            return false;
      } else if (!source.equals(other.source))
         return false;
      if (timeToLive != other.timeToLive)
         return false;
      if (timestamp != other.timestamp)
         return false;
      if (typeName == null) {
         if (other.typeName != null)
            return false;
      } else if (!typeName.equals(other.typeName))
         return false;
      return true;
   }

   

	public static abstract class Builder<B extends Builder<B>> implements Cloneable {
      protected Address source;
      protected Address destination = Address.broadcastAddress();
      protected boolean isError = false;
      protected boolean isRequestMessage = false;
      protected String type;
      protected Long clientTime;
      protected long timestamp;
      protected int timeToLiveMs = -1;
      protected String correlationId;
      protected String placeId;
      protected String population;
      protected Address actor;
      protected byte[] buffer;

      protected Builder() { }

      @SuppressWarnings("unchecked")
      private B ths() {
         return (B) this;
      }

      protected B populate(Message message) {
         this.source = message.getSource();
         this.destination = message.getDestination();
         this.isError = message.isError();
         this.isRequestMessage = message.isRequest();
         this.type = message.getMessageType();
         this.clientTime = message.getClientTime() == null ? null : message.getClientTime().getTime();
         this.timestamp = message.getTimestamp().getTime();
         this.timeToLiveMs = message.getTimeToLive();
         this.correlationId = message.getCorrelationId();
         this.placeId = message.getPlaceId();
         this.population = message.getPopulation();
         this.actor = message.getActor();
         // TODO this should technically be a copy operation, but we shouldn't ever edit this array either...
         this.buffer = message.buffer;
         return ths();
      }

      @Override
      @SuppressWarnings("unchecked")
      protected B clone() {
         try {
            return (B) super.clone();
         }
         catch(CloneNotSupportedException e) {
            throw new RuntimeException("Unable to clone object", e);
         }
      }

      public B from(String source) {
         this.source = Address.fromString(source);
         return ths();
      }

      public B from(Address source) {
         this.source = source;
         return ths();
      }

      public B to(Address destination) {
         this.destination = destination;
         return ths();
      }

      public B broadcast() {
         this.destination = Address.broadcastAddress();
         return ths();
      }

      public B isRequestMessage(boolean isRequestMessage) {
         this.isRequestMessage = isRequestMessage;
         return ths();
      }

      public B isErrorMessage(boolean isError) {
         this.isError = isError;
         return ths();
      }

      public B withClientTime(Long timestamp) {
         this.clientTime = timestamp;
         return ths();
      }

      public B withClientTime(Date timestamp) {
         this.clientTime = timestamp == null ? null : timestamp.getTime();
         return ths();
      }

      public B withTimestamp(long timestamp) {
         this.timestamp = timestamp;
         return ths();
      }

      public B withTimestamp(Date timestamp) {
         this.timestamp = timestamp == null ? 0 : timestamp.getTime();
         return ths();
      }

      public B withTimeToLive(int timeToLiveMs) {
         this.timeToLiveMs = timeToLiveMs;
         return ths();
      }

      public B withoutTimeToLive() {
         this.timeToLiveMs = -1;
         return ths();
      }

      public B withCorrelationId(String correlationId) {
         this.correlationId = correlationId;
         return ths();
      }

      // TODO withGeneratedCorrelationId()

      public B withoutCorrelationId() {
         this.correlationId = null;
         return ths();
      }

      public B withPlaceId(@Nullable String placeId) {
         this.placeId = placeId;
         return ths();
      }
      
      public B withPlaceId(@Nullable UUID placeId) {
         if(placeId == null) {
            this.placeId = null;
         }
         else {
            this.placeId = placeId.toString();
         }
         return ths();
      }
      
      public B withPopulation(@Nullable String population) {
         this.population = population;
         return ths();
      }


      public B withActor(@Nullable Address actor) {
         this.actor = actor;
         return ths();
      }

      public B withPayload(String type, byte[] buffer) {
         byte[] copied = new byte[buffer.length];
         System.arraycopy(buffer, 0, copied, 0, buffer.length);
         return withPayloadUnsafe(type, copied);
      }

      protected B withPayloadUnsafe(String type, byte[] buffer) {
         this.type = type;
         this.buffer = buffer;
         return ths();
      }

   }
}

