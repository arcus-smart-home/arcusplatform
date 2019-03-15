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
package com.iris.common.rule.action;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.util.IrisCollections;
import com.iris.util.IrisFunctions;

/**
 * 
 */
@SuppressWarnings("serial")
public class SendAction implements Action {
   public static final String VAR_TO = "to";
   public static final String VAR_ATTRIBUTES = "attributes";
   public static final String NAME = "send";
   
   private final String type;
   private final Function<? super ActionContext, Address> to;
   private final Map<String, Object> staticAttributes;
   private final Map<String, Function<ActionContext, Object>> dynamicAttributes;
   
   public SendAction(
         String type,
         @Nullable Function<? super ActionContext, Address> to,
         @Nullable Map<String, Object> staticAttributes
   )  {
      this(type, to, staticAttributes, null);
   }
   
   public SendAction(
         String type,
         @Nullable Function<? super ActionContext, Address> to,
         @Nullable Map<String, Object> staticAttributes,
         @Nullable Map<String, Function<ActionContext, Object>> dynamicAttributes
   ) {
      Preconditions.checkNotNull(type, "type may not be null");
      this.type = type;
      // TODO should just force this to be a function that grabs the variable from context.getVariable...
      this.to = to;
      this.staticAttributes = staticAttributes;
      this.dynamicAttributes = dynamicAttributes;
   }
   
   public String getType() {
      return type;
   }
   
   public Address getTo(ActionContext context) {
      if(this.to == null) {
         return context.getVariable(VAR_TO, Address.class);
      }
      return this.to.apply(context);
   }
   
   public Map<String, Object> mergeAttributes() {
      if (staticAttributes == null && dynamicAttributes == null) {
         return null;
      }
      return IrisCollections.<String,Object>merge(staticAttributes, dynamicAttributes);
   }
   
   public Map<String, Object> getAttributes(ActionContext context) {
      if(staticAttributes == null && dynamicAttributes == null) {
         return context.getVariable(VAR_ATTRIBUTES, Map.class);
      }
      else if (dynamicAttributes == null) {
         return staticAttributes;
      }
      else {
         Map<String, Object> resolvedAttributes = new HashMap<>(IrisCollections.size(staticAttributes, dynamicAttributes));
         if (staticAttributes != null) {
            resolvedAttributes.putAll(staticAttributes);
         }
         resolvedAttributes.putAll(IrisFunctions.apply(dynamicAttributes, context));
         return resolvedAttributes;
      }
   }
   
   @Override
   public String getName() {
      return NAME;
   }

   @Override
   public String getDescription() {
      StringBuilder sb = new StringBuilder(NAME).append(" ");
      sb.append(type);
      
      // null indicates templated parameters
      // empty map indicates no parameters
      Map<String, Object> attributes = mergeAttributes();
      if(attributes == null || !attributes.isEmpty()) {
         sb
            .append("(")
            .append(Actions.valueOrVar(VAR_ATTRIBUTES, attributes))
            .append(")")
            ;
      }
      sb.append(" to ");
      sb.append(Actions.valueOrVar(VAR_TO, to != null ? to : null));
      return sb.toString();
   }

   @Override
   public void execute(ActionContext context) {
      Address to = getTo(context);
      Map<String, Object> attributes = getAttributes(context);
      
      // TODO enable broadcast?
      if(to == null) {
         context.logger().warn("Can't execute action [{}] because no destination is specified", this);
         return;
      }
      
      MessageBody message = MessageBody.buildMessage(this.type, attributes);
      context.send(to, message);
   }
   
   @Override
   public String toString() {
      return getDescription();
   }

   public static class Builder {
      private String messageType;
      private Function<ActionContext, Address> destination = null;
      private Map<String, Object> attributes;
      
      public Builder(String messageType) {
         Preconditions.checkNotNull(messageType, "messageType may not be null");
         this.messageType = messageType;
      }
      
      private Map<String, Object> attributes() {
         if(attributes == null) {
            attributes = new HashMap<String, Object>();
         }
         return attributes;
      }
      
      public Builder withDestination(String destination) {
         return withDestination(Address.fromString(destination));
      }
      
      public Builder withDestination(final Address destination) {
         return withDestination(IrisFunctions.constant(ActionContext.class, destination, destination.getRepresentation()));
      };
      
      public Builder withDestination(Function<ActionContext, Address> destination) {
         this.destination = destination;
         return this;
      }
      
      public Builder withTemplatedDestination() {
         this.destination = null;
         return this;
      }
      
      public Builder withTemplatedAttributes() {
         this.attributes = null;
         return this;
      }
      
      public Builder withAttribute(String attributeName, Object attributeValue) {
         Preconditions.checkNotNull(attributeName, "attributeName may not be null");
         Preconditions.checkNotNull(attributeValue, "attributeValue may not be null");
         this.attributes().put(attributeName, attributeValue);
         return this;
      }
      
      public Builder withAttributes(Map<String, Object> attributes) {
         Preconditions.checkNotNull(attributes, "attributes may not be null");
         this.attributes().putAll(attributes);
         return this;
      }
      
      public SendAction build() {
         return new SendAction(messageType, destination, attributes);
      }
   }

}

