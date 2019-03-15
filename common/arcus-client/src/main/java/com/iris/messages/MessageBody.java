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

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.iris.device.attributes.AttributeMap;
import com.iris.device.attributes.AttributeValue;

public class MessageBody implements Serializable {

   private static final String RESPONSE = "Response";
   private static final String REQUEST = "Request";

   private static final MessageBody EMPTY = new MessageBody(MessageConstants.MSG_EMPTY_MESSAGE, Collections.<String,Object>emptyMap());
   private static final MessageBody PING = new MessageBody(MessageConstants.MSG_PING_REQUEST, Collections.<String,Object>emptyMap());
   private static final MessageBody PONG = new MessageBody(MessageConstants.MSG_PONG_RESPONSE, Collections.<String,Object>emptyMap());
   private static final MessageBody NO_RESPONSE = new MessageBody("NoResponse", Collections.<String,Object>emptyMap());

   /**
    * A marker type that indicate no response / message should be put on the bus.
    * @return
    */
   public static MessageBody noResponse() {
   	return NO_RESPONSE;
   }
   
   public static MessageBody emptyMessage() {
      return EMPTY;
   }

   public static MessageBody ping() {
      return PING;
   }

   public static MessageBody pong() {
      return PONG;
   }

   public static Builder messageBuilder(String messageType) {
      return new Builder(messageType);
   }

   public static MessageBody buildMessage(String messageType) {
      return build(messageType, null);
   }

   public static MessageBody buildMessage(String messageType, Map<String,Object> requestAttributes) {
      return build(messageType, requestAttributes);
   }

   public static Builder responseBuilder(MessageBody message) {
      return messageBuilder( responseType(message.getMessageType()) );
   }

   public static MessageBody buildResponse(MessageBody message, Map<String,Object> responseAttributes) {
      return build( responseType(message.getMessageType()), responseAttributes );
   }

   private static String responseType(String requestType) {
   	if(requestType.endsWith(REQUEST)) {
   		return requestType.substring(0, requestType.length() - REQUEST.length()) + RESPONSE;
   	}
   	else {
   		return requestType + RESPONSE;
   	}
   }
   
   private static MessageBody build(String messageType, Map<String, Object> attributes) {
   	switch(messageType) {
   	case MessageConstants.MSG_EMPTY_MESSAGE:
   		return EMPTY;
   	
   	case MessageConstants.MSG_PING_REQUEST:
   		return PING;
   	
   	case MessageConstants.MSG_PONG_RESPONSE:
   		return PONG;
   		
   	case ErrorEvent.MESSAGE_TYPE:
   		String code = (String) attributes.get(ErrorEvent.CODE_ATTR);
   		String message = (String) attributes.get(ErrorEvent.MESSAGE_ATTR);
   		return new ErrorEvent(code, message);
   		
		default:
   		if(attributes == null || attributes.isEmpty()) {
   			return new MessageBody(messageType, ImmutableMap.<String, Object>of());
   		}
   		else if(attributes instanceof ImmutableMap) {
   			return new MessageBody(messageType, attributes);
   		}
   		else {
   			// can't generically use ImmutableMap.copyOf because it doesn't allow nulls
   			return new MessageBody(messageType, Collections.unmodifiableMap(attributes));
   		}
   	}
   }
   
   public static class Builder {

      private final String messageType;
      private final Map<String,Object> attributes = new HashMap<>();

      private Builder(String messageType) {
         this.messageType = messageType;
      }

      public Builder withAttributes(Map<String,Object> attributes) {
         if(attributes != null) {
            this.attributes.putAll(attributes);
         }
         return this;
      }

      public Builder withAttributes(AttributeMap attributes) {
         if(attributes != null) {
            this.attributes.putAll(attributes.toMap());
         }
         return this;
      }

      public Builder withAttribute(String attributeKey, Object attributeValue) {
         this.attributes.put(attributeKey, attributeValue);
         return this;
      }

      public Builder withAttribute(AttributeValue<?> attributeValue) {
         this.attributes.put(attributeValue.getKey().getName(), attributeValue.getValue());
         return this;
      }

      public MessageBody create() {
         return build(messageType, attributes);
      }
   }

   private static final long serialVersionUID = 2377840207286002601L;

   protected final String messageType;
   protected final Map<String,Object> attributes;

   protected MessageBody(String messageType, Map<String,Object> attributes) {
      this.messageType = messageType;
      this.attributes = attributes;
   }

   public final Map<String, Object> getAttributes() {
      return attributes;
   }

   public final String getMessageType() {
      return messageType;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
            + ((attributes == null) ? 0 : attributes.hashCode());
      result = prime * result
            + ((messageType == null) ? 0 : messageType.hashCode());
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
      MessageBody other = (MessageBody) obj;
      if (attributes == null) {
         if (other.attributes != null)
            return false;
      } else if (!attributes.equals(other.attributes))
         return false;
      if (messageType == null) {
         if (other.messageType != null)
            return false;
      } else if (!messageType.equals(other.messageType))
         return false;
      return true;
   }

   @Override
   public String toString() {
      return "MessageBody [messageType=" + messageType + ", attributes="
            + attributes + "]";
   }
}

