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
package com.iris.messages.capability;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.reflect.TypeUtils;

import com.iris.capability.definition.AttributeType;
import com.iris.capability.definition.AttributeTypes;
import com.iris.capability.definition.CapabilityDefinition;
import com.iris.capability.definition.Definitions;
import com.iris.device.attributes.AttributeKey;

/**
 *
 */
public interface Capability {
   public static final String NAME                = "Base";
   public static final String NAMESPACE           = "base";
   
   public static final String ATTR_ID             = "base:id";
   public static final String ATTR_TYPE           = "base:type";
   public static final String ATTR_ADDRESS        = "base:address";
   public static final String ATTR_TAGS           = "base:tags";
   public static final String ATTR_IMAGES         = "base:images";
   public static final String ATTR_CAPS           = "base:caps";
   public static final String ATTR_INSTANCES      = "base:instances";
   
   public static final AttributeType TYPE_ID      = AttributeTypes.stringType();
   public static final AttributeKey<String> KEY_ID = AttributeKey.create(ATTR_ID, String.class);
   public static final AttributeType TYPE_TYPE    = AttributeTypes.stringType();
   public static final AttributeKey<String> KEY_TYPE = AttributeKey.create(ATTR_TYPE, String.class);
   public static final AttributeType TYPE_ADDRESS = AttributeTypes.stringType();
   public static final AttributeKey<String> KEY_ADDRESS = AttributeKey.create(ATTR_ADDRESS, String.class);
   public static final AttributeType TYPE_TAGS    = AttributeTypes.setOf(AttributeTypes.stringType());
   public static final AttributeKey<Set<String>> KEY_TAGS = AttributeKey.createSetOf(ATTR_TAGS, String.class);
   public static final AttributeType TYPE_IMAGES  = AttributeTypes.mapOf(AttributeTypes.stringType());
   public static final AttributeKey<Map<String, String>> KEY_IMAGES = AttributeKey.createMapOf(ATTR_IMAGES, String.class);
   public static final AttributeType TYPE_CAPS    = AttributeTypes.setOf(AttributeTypes.stringType());
   public static final AttributeKey<Set<String>> KEY_CAPS = AttributeKey.createSetOf(ATTR_CAPS, String.class);
   public static final AttributeType TYPE_INSTANCES = AttributeTypes.mapOf(AttributeTypes.mapOf(AttributeTypes.setOf(AttributeTypes.stringType())));
   public static final AttributeKey<Map<String, Set<String>>> KEY_INSTANCES = (AttributeKey<Map<String, Set<String>>>) AttributeKey.createType(ATTR_INSTANCES, TypeUtils.parameterize(Map.class, String.class, TypeUtils.parameterize(Set.class, String.class)));

   public static final String CMD_GET_ATTRIBUTES  = "base:GetAttributes";
   public static final String CMD_SET_ATTRIBUTES  = "base:SetAttributes";
   public static final String CMD_ADD_TAGS        = "base:AddTags";
   public static final String CMD_REMOVE_TAGS     = "base:RemoveTags";
   
   public static final String EVENT_ADDED         = "base:Added";
   public static final String EVENT_VALUE_CHANGE  = "base:ValueChange";
   public static final String EVENT_DELETED       = "base:Deleted";
   public static final String EVENT_REPORT        = "base:Report";

   public static final String EVENT_GET_ATTRIBUTES_RESPONSE = "base:GetAttributesResponse";
   public static final String EVENT_SET_ATTRIBUTES_ERROR    = "base:SetAttributesError";
   public static final String EVENT_ADD_TAGS_RESPONSE       = "base:AddTagsResponse";
   public static final String EVENT_REMOVE_TAGS_RESPONSE    = "base:RemoveTagsResponse";
   
public static class GetAttributesRequest {
   public static final String NAME = CMD_GET_ATTRIBUTES;
   
   public static final String ATTR_NAMES = "names";
   
   public static final com.iris.capability.definition.AttributeType TYPE_NAMES =
         com.iris.capability.definition.AttributeTypes.setOf(AttributeTypes.stringType());

   public static java.util.Set<java.lang.String> getNames(com.iris.messages.MessageBody message) {
      if(message == null) { 
         return null;
      }
      return (java.util.Set<java.lang.String>) TYPE_NAMES.coerce(message.getAttributes().get(ATTR_NAMES));
   }
   
      public static Builder builder() {
         return new Builder();
      }
      
      public static Builder builder(com.iris.messages.MessageBody body) {
         return new Builder(body);
      }
      
      public static class Builder {
         
         private java.util.Set<java.lang.String> _names;
         
         Builder() {
         }

         Builder(com.iris.messages.MessageBody body) {
             this(body != null ? body.getAttributes() : null);
         }

         Builder(java.util.Map<String, Object> attributes) {
            if(attributes != null) {
              this._names = (java.util.Set<java.lang.String>) TYPE_NAMES.coerce(attributes.get(ATTR_NAMES));
            }
         }

         
         public java.util.Set<java.lang.String> getNames() {
            return _names;
         }

         public Builder withNames(java.util.Set<java.lang.String> value) {
            this._names = value;
            return this;
         }
         
         public com.iris.messages.MessageBody build() {
            java.util.Map<String, Object> attributes =
               new java.util.HashMap<String, Object>(1);
            
            attributes.put(ATTR_NAMES, _names);
            
            return com.iris.messages.MessageBody.buildMessage(NAME, attributes);
         }
      }

}

   /** Adds the requested tags to base:tags */
public static class AddTagsRequest {
   public static final String NAME = CMD_ADD_TAGS;

   
   /** The tags to add */
   public static final String ATTR_TAGS = "tags";
   
   public static final com.iris.capability.definition.AttributeType TYPE_TAGS =
      com.iris.capability.definition.AttributeTypes.setOf(AttributeTypes.stringType());

   /** The tags to add */
   public static java.util.Set<java.lang.String> getTags(com.iris.messages.MessageBody message) {
      if(message == null) { 
         return null;
      }
      return (java.util.Set<java.lang.String>) TYPE_TAGS.coerce(message.getAttributes().get(ATTR_TAGS));
   }
   
      public static Builder builder() {
         return new Builder();
      }
      
      public static Builder builder(com.iris.messages.MessageBody body) {
         return new Builder(body);
      }
      
      public static class Builder {
         
         private java.util.Set<java.lang.String> _tags;
         
         Builder() {
         }

         Builder(com.iris.messages.MessageBody body) {
             this(body != null ? body.getAttributes() : null);
         }

         Builder(java.util.Map<String, Object> attributes) {
            if(attributes != null) {
              this._tags = (java.util.Set<java.lang.String>) TYPE_TAGS.coerce(attributes.get(ATTR_TAGS));
            }
         }

         
         public java.util.Set<java.lang.String> getTags() {
            return _tags;
         }

         public Builder withTags(java.util.Set<java.lang.String> value) {
            this._tags = value;
            return this;
         }
         
         public com.iris.messages.MessageBody build() {
            java.util.Map<String, Object> attributes =
               new java.util.HashMap<String, Object>(1);
            
            attributes.put(ATTR_TAGS, _tags);
            
            return com.iris.messages.MessageBody.buildMessage(NAME, attributes);
         }
      }

}
public static class AddTagsResponse {
   public static final String NAME = EVENT_ADD_TAGS_RESPONSE;

   public static com.iris.messages.MessageBody instance() {
      return com.iris.messages.MessageBody.emptyMessage();
   }

}
   
/** Removes the requested tags to base:tags */
public static class RemoveTagsRequest {
   public static final String NAME = CMD_REMOVE_TAGS;
   
   
   /** The tags to remove */
   public static final String ATTR_TAGS = "tags";
   
   public static final com.iris.capability.definition.AttributeType TYPE_TAGS =
      com.iris.capability.definition.AttributeTypes.setOf(AttributeTypes.stringType());
   
   /** The tags to remove */
   public static java.util.Set<java.lang.String> getTags(com.iris.messages.MessageBody message) {
      if(message == null) { 
         return null;
      }
      return (java.util.Set<java.lang.String>) TYPE_TAGS.coerce(message.getAttributes().get(ATTR_TAGS));
   }

   public static Builder builder() {
      return new Builder();
   }
   
   public static Builder builder(com.iris.messages.MessageBody body) {
      return new Builder(body);
   }
   
   public static class Builder {
      
      private java.util.Set<java.lang.String> _tags;
      
      Builder() {
      }

      Builder(com.iris.messages.MessageBody body) {
          this(body != null ? body.getAttributes() : null);
      }

      Builder(java.util.Map<String, Object> attributes) {
         if(attributes != null) {
           this._tags = (java.util.Set<java.lang.String>) TYPE_TAGS.coerce(attributes.get(ATTR_TAGS));
         }
      }


      public java.util.Set<java.lang.String> getTags() {
         return _tags;
      }

      public Builder withTags(java.util.Set<java.lang.String> value) {
         this._tags = value;
         return this;
      }
      
      public com.iris.messages.MessageBody build() {
         java.util.Map<String, Object> attributes =
            new java.util.HashMap<String, Object>(1);
         
         attributes.put(ATTR_TAGS, _tags);
         
         return com.iris.messages.MessageBody.buildMessage(NAME, attributes);
      }
   }

}
public static class RemoveTagsResponse {
   public static final String NAME = EVENT_REMOVE_TAGS_RESPONSE;
   
   public static com.iris.messages.MessageBody instance() {
      return com.iris.messages.MessageBody.emptyMessage();
   }

}

   public static final CapabilityDefinition DEFINITION = 
      Definitions
         .capabilityBuilder()
         .withName(NAME)
         .withNamespace(NAMESPACE)
         .addAttribute(
               Definitions
                  .attributeBuilder()
                  .withType("string")
                  .withName("id")
                  .withDescription("The unique identifier for this object")
                  .build()
         )
         .addAttribute(
               Definitions
                  .attributeBuilder()
                  .withType("string")
                  .withName("type")
                  .withDescription("The type of the attributes")
                  .build()
         )
         .addAttribute(
               Definitions
                  .attributeBuilder()
                  .withType("string")
                  .withName("address")
                  .withDescription("The address for this object")
                  .build()
         )
         .addAttribute(
               Definitions
                  .attributeBuilder()
                  .withType("set<string>")
                  .withName("tags")
                  .withDescription("The tags associated with this object")
                  .build()
         )
         .addAttribute(
               Definitions
                  .attributeBuilder()
                  .withType("map<string>")
                  .withName("images")
                  .writable()
                  .withDescription("Images associated with this object")
                  .build()
         )
         .addAttribute(
               Definitions
                  .attributeBuilder()
                  .withType("set<string>")
                  .withName("caps")
                  .withDescription("The capabilities associated with this object")
                  .build()
         )
         .addAttribute(
               Definitions
                  .attributeBuilder()
                  .withType("map<set<string>>")
                  .withName("instances")
                  .withDescription("A map of instance id to capabilities for the multi-instance objects associated with this device")
                  .build()
         )
         .addMethod(
               Definitions
                  .methodBuilder()
                  .withName("GetAttributes")
                  .addParameter(
                        Definitions
                           .parameterBuilder()
                           .withType("list<string>")
                           .withName("names")
                           .build()
                  )
                  .build()
         )
         .addMethod(
               Definitions
                  .methodBuilder()
                  .withName("SetAttributes")
                  .build()
         )
         .addMethod(
               Definitions
                  .methodBuilder()
                  .withName("AddTags")
                  .addParameter(
                        Definitions
                           .parameterBuilder()
                           .withType("set<string>")
                           .withName(AddTagsRequest.ATTR_TAGS)
                           .build()
                  )
                  .build()
         )
         .addMethod(
               Definitions
                  .methodBuilder()
                  .withName("RemoveTags")
                  .addParameter(
                        Definitions
                           .parameterBuilder()
                           .withType("set<string>")
                           .withName(RemoveTagsRequest.ATTR_TAGS)
                           .build()
                  )
                  .build()
         )
         .addEvent(
               Definitions
                  .eventBuilder()
                  .withName("Added")
                  .build()
         )
         .addEvent(
               Definitions
                  .eventBuilder()
                  .withName("ValueChange")
                  .build()
         )
         .addEvent(
               Definitions
                  .eventBuilder()
                  .withName("Deleted")
                  .build()
         )
         .addEvent(
               Definitions
                  .eventBuilder()
                  .withName("GetAttributesResponse")
                  .build()
         )
         .addEvent(
               Definitions
                  .eventBuilder()
                  .withName("SetAttributesError")
                  .build()
         )
         .addEvent(
            Definitions
               .eventBuilder()
               .withName("AttrReport")
               .build()
         )
         .build();

}

