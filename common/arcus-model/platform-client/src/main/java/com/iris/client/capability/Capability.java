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
package com.iris.client.capability;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.iris.capability.definition.CapabilityDefinition;
import com.iris.capability.definition.Definitions;
import com.iris.client.ErrorEvent;
import com.iris.client.annotation.GetAttribute;

/**
 *
 */
public interface Capability {
   public static final String NAMESPACE           = "base";
   
   public static final String ATTR_ID             = "base:id";
   public static final String ATTR_TYPE           = "base:type";
   public static final String ATTR_ADDRESS        = "base:address";
   public static final String ATTR_TAGS           = "base:tags";
   public static final String ATTR_IMAGES         = "base:images";
   public static final String ATTR_CAPS           = "base:caps";
   public static final String ATTR_INSTANCES      = "base:instances";
   
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
   
   @GetAttribute(ATTR_TYPE)
	public String getType();
	
   @GetAttribute(ATTR_ID)
	public String getId();
	
   @GetAttribute(ATTR_ADDRESS)
	public String getAddress();

   @GetAttribute(ATTR_TAGS)
   public Collection<String> getTags();
   
   @GetAttribute(ATTR_IMAGES)
   public Map<String, String> getImages();
   
   @GetAttribute(ATTR_CAPS)
   public Collection<String> getCaps();
   
   @GetAttribute(ATTR_INSTANCES)
   public Map<String, Collection<String>> getInstances();
   
   /** Adds the given tags to base:tags */
   @com.iris.client.annotation.Command(value=AddTagsRequest.NAME, parameters={ AddTagsRequest.ATTR_TAGS })
   public com.iris.client.event.ClientFuture<AddTagsResponse> addTags(java.util.Collection<java.lang.String> tags);
   
   /** Removes the given tags from base:tags */
   @com.iris.client.annotation.Command(value=RemoveTagsRequest.NAME, parameters={ RemoveTagsRequest.ATTR_TAGS })
   public com.iris.client.event.ClientFuture<RemoveTagsResponse> removeTags(java.util.Collection<java.lang.String> tags);
   
   /** Adds the given tags to base:tags */
   public static class AddTagsRequest extends com.iris.client.ClientRequest {
      public static final String NAME = CMD_ADD_TAGS;

      public AddTagsRequest() {
         setCommand(NAME);
      }

   
   /** The tags to add */
   public static final String ATTR_TAGS = "tags";


      
      public java.util.Collection<java.lang.String> getTags() {
         return (java.util.Collection<java.lang.String>) getAttribute(ATTR_TAGS);
      }

      public void setTags(java.util.Collection<java.lang.String> value) {
         setAttribute(ATTR_TAGS, value);
      }
      
   }

   public static class AddTagsResponse extends com.iris.client.ClientEvent {
      public static final String NAME = EVENT_ADD_TAGS_RESPONSE;

      public AddTagsResponse(String type, String sourceAddress) {
         super(type, sourceAddress);
      }
   
      public AddTagsResponse(String type, String sourceAddress, java.util.Map<String, Object> attributes) {
         super(type, sourceAddress, attributes);
      }

      public AddTagsResponse(com.iris.client.ClientEvent copy) {
         super(copy.getType(), copy.getSourceAddress(), new java.util.HashMap<String, Object>(copy.getAttributes()));
      }
   }
   
   /** Removes the given tags from base:tags */
   public static class RemoveTagsRequest extends com.iris.client.ClientRequest {
      public static final String NAME = CMD_REMOVE_TAGS;

      public RemoveTagsRequest() {
         setCommand(NAME);
      }

   
   /** The tags to remove */
   public static final String ATTR_TAGS = "tags";


      
      public java.util.Collection<java.lang.String> getTags() {
         return (java.util.Collection<java.lang.String>) getAttribute(ATTR_TAGS);
      }

      public void setTags(java.util.Collection<java.lang.String> value) {
         setAttribute(ATTR_TAGS, value);
      }
      
   }

   public static class RemoveTagsResponse extends com.iris.client.ClientEvent {
      public static final String NAME = EVENT_REMOVE_TAGS_RESPONSE;

      public RemoveTagsResponse(String type, String sourceAddress) {
         super(type, sourceAddress);
      }
   
      public RemoveTagsResponse(String type, String sourceAddress, java.util.Map<String, Object> attributes) {
         super(type, sourceAddress, attributes);
      }

      public RemoveTagsResponse(com.iris.client.ClientEvent copy) {
         super(copy.getType(), copy.getSourceAddress(), new java.util.HashMap<String, Object>(copy.getAttributes()));
      }
   }
   
   /** Sent when a new object is added to the system. */
   public static class AddedEvent extends com.iris.client.ClientEvent {
      public static final String NAME = EVENT_ADDED;
       

      public AddedEvent(String sourceAddress) {
         super(NAME, sourceAddress);
      }
   
      public AddedEvent(String sourceAddress, java.util.Map<String, Object> attributes) {
         super(NAME, sourceAddress, attributes);
      }

      public AddedEvent(com.iris.client.ClientEvent copy) {
         super(NAME, copy.getSourceAddress(), new java.util.HashMap<String, Object>(copy.getAttributes()));
      }

   }
   
   /** Sent when an observable attribute is changed. */
   public static class ValueChangeEvent extends com.iris.client.ClientEvent {
      public static final String NAME = EVENT_VALUE_CHANGE;
       

      public ValueChangeEvent(String sourceAddress) {
         super(NAME, sourceAddress);
      }
   
      public ValueChangeEvent(String sourceAddress, java.util.Map<String, Object> attributes) {
         super(NAME, sourceAddress, attributes);
      }

      public ValueChangeEvent(com.iris.client.ClientEvent copy) {
         super(NAME, copy.getSourceAddress(), new java.util.HashMap<String, Object>(copy.getAttributes()));
      }

   }
   
   public static class DeletedEvent extends com.iris.client.ClientEvent {
      public static final String NAME = EVENT_DELETED;
       

      public DeletedEvent(String sourceAddress) {
         super(NAME, sourceAddress);
      }
   
      public DeletedEvent(String sourceAddress, java.util.Map<String, Object> attributes) {
         super(NAME, sourceAddress, attributes);
      }

      public DeletedEvent(com.iris.client.ClientEvent copy) {
         super(NAME, copy.getSourceAddress(), new java.util.HashMap<String, Object>(copy.getAttributes()));
      }

      
   }
   
   public static class GetAttributesValuesResponseEvent extends com.iris.client.ClientEvent {
      public static final String NAME = EVENT_GET_ATTRIBUTES_RESPONSE;
       

      public GetAttributesValuesResponseEvent(String sourceAddress) {
         super(NAME, sourceAddress);
      }
   
      public GetAttributesValuesResponseEvent(String sourceAddress, java.util.Map<String, Object> attributes) {
         super(NAME, sourceAddress, attributes);
      }

      public GetAttributesValuesResponseEvent(com.iris.client.ClientEvent copy) {
         super(NAME, copy.getSourceAddress(), new java.util.HashMap<String, Object>(copy.getAttributes()));
      }

      
   }
   
   public static class SetAttributesErrorEvent extends com.iris.client.ClientEvent {
      public static final String NAME = EVENT_SET_ATTRIBUTES_ERROR;
      
      private Collection<ErrorEvent> errors;

      public SetAttributesErrorEvent(String sourceAddress) {
         super(NAME, sourceAddress);
         init();
      }
   
      public SetAttributesErrorEvent(String sourceAddress, java.util.Map<String, Object> attributes) {
         super(NAME, sourceAddress, attributes);
         init();
      }

      public SetAttributesErrorEvent(com.iris.client.ClientEvent copy) {
         super(NAME, copy.getSourceAddress(), new java.util.HashMap<String, Object>(copy.getAttributes()));
         init();
      }
      
      private void init() {
         Collection<Map<String, Object>> errors = (Collection<Map<String,Object>>) getAttribute("errors");
         if(errors == null || errors.isEmpty()) {
            this.errors = Collections.<ErrorEvent>emptyList();
            return;
         }
         List<ErrorEvent> translated = new ArrayList<ErrorEvent>();
         for(Map<String, Object> error: errors) {
            translated.add(new ErrorEvent(getSourceAddress(), error));
         }
         this.errors = translated;
      }

      public Collection<ErrorEvent> getErrors() {
         return errors;
      }
      
   }

   /** Sent when an observable attribute is changed. */
   public static class ReportEvent extends com.iris.client.ClientEvent {
      public static final String NAME = EVENT_REPORT;


      public ReportEvent(String sourceAddress) {
         super(NAME, sourceAddress);
      }

      public ReportEvent(String sourceAddress, java.util.Map<String, Object> attributes) {
         super(NAME, sourceAddress, attributes);
      }

      public ReportEvent(com.iris.client.ClientEvent copy) {
         super(NAME, copy.getSourceAddress(), new java.util.HashMap<String, Object>(copy.getAttributes()));
      }

   }

   public static final CapabilityDefinition DEFINITION = 
         Definitions
            .capabilityBuilder()
            .withName("Capability")
            .withNamespace(NAMESPACE)
            .addAttribute(
                  Definitions
                     .attributeBuilder()
                     .withType("string")
                     .withName(ATTR_ID)
                     .withDescription("The unique identifier for this object")
                     .build()
            )
            .addAttribute(
                  Definitions
                     .attributeBuilder()
                     .withType("string")
                     .withName(ATTR_TYPE)
                     .withDescription("The type of the attributes")
                     .build()
            )
            .addAttribute(
                  Definitions
                     .attributeBuilder()
                     .withType("string")
                     .withName(ATTR_ADDRESS)
                     .withDescription("The address for this object")
                     .build()
            )
            .addAttribute(
                  Definitions
                     .attributeBuilder()
                     .withType("set<string>")
                     .withName(ATTR_TAGS)
                     .withDescription("The tags associated with this object")
                     .build()
            )
            .addAttribute(
                  Definitions
                     .attributeBuilder()
                     .withType("map<string>")
                     .withName(ATTR_IMAGES)
                     .writable()
                     .withDescription("Images associated with this object")
                     .build()
            )
            .addAttribute(
                  Definitions
                     .attributeBuilder()
                     .withType("set<string>")
                     .withName(ATTR_CAPS)
                     .withDescription("The capabilities associated with this object")
                     .build()
            )
            .addAttribute(
                  Definitions
                     .attributeBuilder()
                     .withType("map<set<string>>")
                     .withName(ATTR_INSTANCES)
                     .withDescription("A map of instance id to capabilities for the multi-instance objects associated with this device")
                     .build()
            )
            .addMethod(
                  Definitions
                     .methodBuilder()
                     .withName(CMD_GET_ATTRIBUTES)
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
                     .withName(CMD_SET_ATTRIBUTES)
                     .build()
            )
            .addMethod(
                  Definitions
                     .methodBuilder()
                     .withName(AddTagsRequest.NAME)
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
                     .withName(RemoveTagsRequest.NAME)
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
                     .withName(EVENT_ADDED)
                     .build()
            )
            .addEvent(
                  Definitions
                     .eventBuilder()
                     .withName(EVENT_VALUE_CHANGE)
                     .build()
            )
            .addEvent(
                  Definitions
                     .eventBuilder()
                     .withName(EVENT_DELETED)
                     .build()
            )
            .addEvent(
                  Definitions
                     .eventBuilder()
                     .withName(EVENT_GET_ATTRIBUTES_RESPONSE)
                     .build()
            )
            .addEvent(
                  Definitions
                     .eventBuilder()
                     .withName(EVENT_SET_ATTRIBUTES_ERROR)
                     .build()
            )
            .addEvent(
               Definitions
                  .eventBuilder()
                  .withName(EVENT_REPORT)
                  .build()
            )
            .build();
}

