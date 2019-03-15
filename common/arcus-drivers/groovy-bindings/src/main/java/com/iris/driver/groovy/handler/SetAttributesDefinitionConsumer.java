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
package com.iris.driver.groovy.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.groovy.context.SetAttributesHandlerDefinition;
import com.iris.driver.handler.SetAttributesConsumer;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.Capability;
import com.iris.protocol.reflex.ReflexProtocol;

public class SetAttributesDefinitionConsumer implements SetAttributesConsumer {
   private static final Logger log = LoggerFactory.getLogger(SetAttributesDefinitionConsumer.class);

   private final Exception declSite;
   private final Map<String,List<SetAttributesHandlerDefinition>> actions;

   public SetAttributesDefinitionConsumer(List<SetAttributesHandlerDefinition> definitions) {
      this.declSite = new Exception();

      this.actions = new HashMap<>();
      for (SetAttributesHandlerDefinition def : definitions) {
         for (SetAttributesHandlerDefinition.MatchAttr match : def.getMatches()) {
            List<SetAttributesHandlerDefinition> acts = actions.get(match.getAttributeName());
            if (acts == null) {
               acts = new ArrayList<>();
               actions.put(match.getAttributeName(), acts);
            }

            acts.add(def);
         }
      }

      for (List<?> list : actions.values()) {
         ((ArrayList<?>)list).trimToSize();
      }
   }

   @Override
   public Exception getDeclarationSite() {
      return declSite;
   }

   @Override
   public String getNamespace() {
      return null;
   }

   @Override
   public Set<String> setAttributes(DeviceDriverContext context, Map<String, Object> attributes) {
      Set<String> handledAttrbutes = new HashSet<>();

      boolean forwarded = false;
      for (Map.Entry<String,Object> entry : attributes.entrySet()) {
         List<SetAttributesHandlerDefinition> acts = actions.get(entry.getKey());
         if (acts == null || acts.isEmpty()) {
            continue;
         }

         boolean handled = false;
         for (SetAttributesHandlerDefinition def : acts) {
            boolean matched = false;
            for (SetAttributesHandlerDefinition.MatchAttr match : def.getMatches()) {
               if (match.matches(entry.getValue())) {
                  matched = true;
                  break;
               }
            }

            if (matched) {
               forwarded |= def.isForwarded();

               handled = true;
               for (SetAttributesHandlerDefinition.Action action : def.getActions()) {
                  try {
                     action.run(context, entry.getValue());
                  } catch (Exception ex) {
                     log.warn("exception while running set attributes definition action:", ex);
                  }
               }
            }
         }

         if (handled) {
            handledAttrbutes.add(entry.getKey());
         }
      }

      if (forwarded) {
         context.sendToDevice(ReflexProtocol.INSTANCE, MessageBody.buildMessage(Capability.CMD_SET_ATTRIBUTES, attributes), -1);
      }

      return handledAttrbutes;
   }
}

