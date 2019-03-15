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
package com.iris.common.subsystem;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.annotation.Version;
import com.iris.common.subsystem.annotation.Subsystem;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.Capability.AddTagsRequest;
import com.iris.messages.capability.Capability.AddTagsResponse;
import com.iris.messages.capability.Capability.RemoveTagsRequest;
import com.iris.messages.capability.Capability.RemoveTagsResponse;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.event.MessageReceivedEvent;
import com.iris.messages.model.subs.SubsystemModel;

/**
 * 
 */
public class TestBaseSubsystem extends SubsystemTestCase<SubsystemModel> {
   TestSubsystem subsystem = new TestSubsystem();
   
   @Before
   public void loadSubsystem() throws Exception {
      init(subsystem);
   }
   
   @Test
   public void testGetAttributes() {
      MessageReceivedEvent event = request(Capability.CMD_GET_ATTRIBUTES);
      
      subsystem.onEvent(event, context);
      
      assertEquals(1, responses.getValues().size());
      {
         MessageBody response = responses.getValue();
         assertAllBaseAttributes(response.getAttributes());
         assertAllSubsystemAttributes(response.getAttributes());
      }
   }
   
   @Test
   public void testGetAttributesByNamespace() {
      MessageReceivedEvent event = request(
            Capability.CMD_GET_ATTRIBUTES, 
            ImmutableMap.<String, Object>of("names", ImmutableSet.of(SubsystemCapability.NAMESPACE))
      );
      
      subsystem.onEvent(event, context);
      
      assertEquals(1, responses.getValues().size());
      {
         MessageBody response = responses.getValue();
         assertAllSubsystemAttributes(response.getAttributes());
         for(String key: response.getAttributes().keySet()) {
            assertTrue("Unexpected key " + key, key.startsWith(SubsystemCapability.NAMESPACE + ":"));
         }
      }
   }
   
   @Test
   public void testGetAttributesByNames() {
      Set<String> names = ImmutableSet.of(
            Capability.ATTR_ADDRESS,
            Capability.ATTR_TAGS,
            SubsystemCapability.ATTR_NAME
      );
      MessageReceivedEvent event = request(
            Capability.CMD_GET_ATTRIBUTES, 
            ImmutableMap.<String, Object>of("names", names)
      );
      
      subsystem.onEvent(event, context);
      
      assertEquals(1, responses.getValues().size());
      {
         MessageBody response = responses.getValue();
         Map<String, Object> attributes = response.getAttributes();
         assertEquals(names, attributes.keySet());
         assertEquals(context.model().getAddress().getRepresentation(), attributes.get(Capability.ATTR_ADDRESS));
         assertEquals(ImmutableSet.of(), attributes.get(Capability.ATTR_TAGS));
         assertEquals("Subsystem", attributes.get(SubsystemCapability.ATTR_NAME));
      }
   }
   
   @Test
   public void testSetAttributes() {
      Map<String, String> images = ImmutableMap.of("test", "image");
      // TODO add in another capability, the only settable one here is images
      MessageReceivedEvent event = request(
            Capability.CMD_SET_ATTRIBUTES, 
            ImmutableMap.<String, Object>of(Capability.ATTR_IMAGES, images)
      );
      
      subsystem.onEvent(event, context);
      
      assertEquals(1, responses.getValues().size());
      {
         MessageBody response = responses.getValue();
         assertEquals(MessageConstants.MSG_EMPTY_MESSAGE, response.getMessageType());
         assertEquals(ImmutableMap.of(), response.getAttributes());
      }
      {
         assertEquals(images, model.getAttribute(Capability.ATTR_IMAGES));
      }
   }
   
   @Test
   public void testSetReadOnlyAndNonExistentAttributes() {
      Map<String, String> images = ImmutableMap.of("test", "image");
      MessageReceivedEvent event = request(
            Capability.CMD_SET_ATTRIBUTES, 
            ImmutableMap.<String, Object>of(
                  SubsystemCapability.ATTR_NAME, "A new name",
                  "test:Attribute", "test",
                  Capability.ATTR_IMAGES, images
            )
      );
      
      subsystem.onEvent(event, context);
      
      assertEquals(1, responses.getValues().size());
      {
         MessageBody response = responses.getValue();
         assertEquals(Capability.EVENT_SET_ATTRIBUTES_ERROR, response.getMessageType());
         List<Map<String, Object>> errors = (List<Map<String, Object>>) response.getAttributes().get("errors");
         assertEquals(2, errors.size());
         for(Map<String, Object> error: errors) {
            assertEquals(Errors.CODE_UNSUPPORTED_ATTRIBUTE, error.get(ErrorEvent.CODE_ATTR));
         }
      }
      {
         // should have updated
         assertEquals(images, model.getAttribute(Capability.ATTR_IMAGES));
         
         // should not have changed
         assertEquals("Subsystem", model.getAttribute(SubsystemCapability.ATTR_NAME));
         assertEquals(null, model.getAttribute("test:Attribute"));
      }
   }
   
   @Test
   public void testAddTags() {
      Set<String> tags = ImmutableSet.of("tag1", "tag2", "tag3");
      
      MessageReceivedEvent event = request(
            AddTagsRequest
               .builder()
               .withTags(tags)
               .build()
      );
      
      subsystem.onEvent(event, context);
      
      assertEquals(1, responses.getValues().size());
      {
         MessageBody response = responses.getValue();
         assertEquals(AddTagsResponse.instance(), response);
         // should have updated
         assertEquals(tags, model.getAttribute(Capability.ATTR_TAGS));
      }
      
   }
   
   @Test
   public void testAddTagsWithExisting() {
      model.setAttribute(Capability.ATTR_TAGS, ImmutableSet.of("tag1", "tag2", "tag3"));
      Set<String> toAdd = ImmutableSet.of("tag3", "tag4", "tag5");
      
      MessageReceivedEvent event = request(
            AddTagsRequest
               .builder()
               .withTags(toAdd)
               .build()
      );
      
      subsystem.onEvent(event, context);
      
      assertEquals(1, responses.getValues().size());
      {
         MessageBody response = responses.getValue();
         assertEquals(AddTagsResponse.instance(), response);
         // should have updated
         assertEquals(
               ImmutableSet.of("tag1", "tag2", "tag3", "tag4", "tag5"),
               model.getAttribute(Capability.ATTR_TAGS));
      }
      
   }
   
   @Test
   public void testRemoveAllTags() {
      Set<String> tags = ImmutableSet.of("tag1", "tag2", "tag3");
      model.setAttribute(Capability.ATTR_TAGS, tags);
      
      MessageReceivedEvent event = request(
            RemoveTagsRequest
               .builder()
               .withTags(tags)
               .build()
      );
      
      subsystem.onEvent(event, context);
      
      assertEquals(1, responses.getValues().size());
      {
         MessageBody response = responses.getValue();
         assertEquals(RemoveTagsResponse.instance(), response);
         // should have updated
         assertEquals(ImmutableSet.of(), model.getAttribute(Capability.ATTR_TAGS));
      }
      
   }
   
   @Test
   public void testRemoveSomeTags() {
      model.setAttribute(Capability.ATTR_TAGS, ImmutableSet.of("tag1", "tag2", "tag3"));
      Set<String> toRemove = ImmutableSet.of("tag3", "tag4", "tag5");
      
      MessageReceivedEvent event = request(
            RemoveTagsRequest
               .builder()
               .withTags(toRemove)
               .build()
      );
      
      subsystem.onEvent(event, context);
      
      assertEquals(1, responses.getValues().size());
      {
         MessageBody response = responses.getValue();
         assertEquals(RemoveTagsResponse.instance(), response);
         // should have updated
         assertEquals(
               ImmutableSet.of("tag1", "tag2"),
               model.getAttribute(Capability.ATTR_TAGS));
      }
      
   }
   
   protected void assertAllBaseAttributes(Map<String, Object> attributes) {
      assertEquals(context.model().getId(), attributes.get(Capability.ATTR_ID));
      assertEquals(context.model().getAddress().getRepresentation(), attributes.get(Capability.ATTR_ADDRESS));
      assertEquals(SubsystemCapability.NAMESPACE, attributes.get(Capability.ATTR_TYPE));
      assertEquals(ImmutableSet.of(Capability.NAMESPACE, SubsystemCapability.NAMESPACE), attributes.get(Capability.ATTR_CAPS));
      assertEquals(ImmutableSet.of(), attributes.get(Capability.ATTR_TAGS));
      assertEquals(ImmutableMap.of(), attributes.get(Capability.ATTR_IMAGES));
      assertEquals(ImmutableMap.of(), attributes.get(Capability.ATTR_INSTANCES));
   }
   
   protected void assertAllSubsystemAttributes(Map<String, Object> attributes) {
      assertEquals(accountId.toString(), attributes.get(SubsystemCapability.ATTR_ACCOUNT));
      assertEquals(placeId.toString(), attributes.get(SubsystemCapability.ATTR_PLACE));
      assertEquals("Subsystem", attributes.get(SubsystemCapability.ATTR_NAME));
      assertEquals("1.0", attributes.get(SubsystemCapability.ATTR_VERSION));
      assertEquals(SubsystemCapability.STATE_ACTIVE, attributes.get(SubsystemCapability.ATTR_STATE));
      // TODO hash
   }

   @Subsystem(SubsystemModel.class)
   @Version(1)
   public static class TestSubsystem extends BaseSubsystem<SubsystemModel> {
      
   }
}

