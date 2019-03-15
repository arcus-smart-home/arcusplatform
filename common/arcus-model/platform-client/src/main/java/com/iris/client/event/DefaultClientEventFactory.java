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
package com.iris.client.event;

import java.util.Map;

import com.iris.client.ClientEvent;
import com.iris.client.EmptyEvent;
import com.iris.client.ErrorEvent;
import com.iris.client.capability.Capability;

public class DefaultClientEventFactory implements ClientEventFactory {
   
   public static ClientEventFactory getInstance() {
      return Reference.INSTANCE;
   }
   
   @Override
   public ClientEvent create(String type, String sourceAddress, Map<String, Object> attributes) {
      if(EmptyEvent.NAME.equals(type)) {
         return new EmptyEvent(sourceAddress);
      }
      else if(ErrorEvent.NAME.equals(type)) {
         return new ErrorEvent(sourceAddress, attributes);
      }
      else if(Capability.EVENT_ADDED.equals(type)) {
         return new Capability.AddedEvent(sourceAddress, attributes);
      }
      else if(Capability.EVENT_VALUE_CHANGE.equals(type)) {
         return new Capability.ValueChangeEvent(sourceAddress, attributes);
      }
      else if(Capability.EVENT_REPORT.equals(type)) {
         return new Capability.ReportEvent(sourceAddress, attributes);
      }
      else if(Capability.EVENT_DELETED.equals(type)) {
         return new Capability.DeletedEvent(sourceAddress, attributes);
      }
      else if(Capability.EVENT_GET_ATTRIBUTES_RESPONSE.equals(type)) {
         return new Capability.GetAttributesValuesResponseEvent(sourceAddress, attributes);
      }
      return new ClientEvent(type, sourceAddress, attributes);
   }

   private static class Reference {
      private static final DefaultClientEventFactory INSTANCE = new DefaultClientEventFactory();
   }
}

