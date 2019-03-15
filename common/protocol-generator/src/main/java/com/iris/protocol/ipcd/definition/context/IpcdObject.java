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
package com.iris.protocol.ipcd.definition.context;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IpcdObject {
   private String name;
   private String description;
   private String type;
   private boolean closures;
   private String commandElement;
   private List<Property> virtualProperties;
   private List<Property> properties;
   private List<IpcdObject> commands;
   private Set<Signature> signatures = new HashSet<>();
   
   public boolean isHasProperties() {
      return (properties != null && properties.size() > 0);
   }
   
   public boolean isHasDevice() {
      return hasProperty("device", properties) || hasProperty("device", virtualProperties);
   }
   
   public boolean isHasCommands() {
      return commandElement != null;
   }
   
   public boolean isVirtual() {
      return virtualProperties != null && !virtualProperties.isEmpty();
   }
   
   public Set<Signature> getSignatures() {
      return signatures;
   }
   
   public String getName() {
      return name;
   }
   public void setName(String name) {
      this.name = name;
   }
   public String getDescription() {
      return description;
   }
   public void setDescription(String description) {
      this.description = description;
   }
   public String getType() {
      return type;
   }
   public void setType(String type) {
      this.type = type;
   }  
   public String getCommandElement() {
      return commandElement;
   }
   public void setCommandElement(String commandElement) {
      this.commandElement = commandElement;
   }
   
   public List<IpcdObject> getCommands() {
      return commands;
   }

   public void setCommands(List<IpcdObject> commands) {
      this.commands = commands;
   }

   public boolean isClosures() {
      return closures;
   }
   public void setClosures(boolean closure) {
      this.closures = closure;
   }
   
   public List<Property> getVirtualProperties() {
      return virtualProperties;
   }

   public void setVirtualProperties(List<Property> virtualProperties) {
      this.virtualProperties = virtualProperties;
   }

   public List<Property> getProperties() {
      return properties;
   }
   public void setProperties(List<Property> properties) {
      this.properties = properties;
      updateSignatures();
   }
   
   private void updateSignatures() {
      signatures.clear();
      if (properties != null) {
         blender(0, new Signature(name));
      }
   }
   
   private void blender(int pos, Signature curSig) {
      if (pos >= properties.size()) {
         signatures.add(curSig);
         return;
      }
      else {
         Property prop = properties.get(pos);
         pos++;
         if (prop.isOptional() || prop.isKey()) {
            blender(pos, new Signature(name));
         }
         if (!prop.isKey()) {
            curSig.add(prop);
            blender(pos, curSig);
         }
      }
   }
   
   private static boolean hasProperty(String name, List<Property> props) {
      if (props == null) {
         return false;
      }
      for (Property prop : props) {
         if (name.equalsIgnoreCase(prop.getName())) {
            return true;
         }
      }
      return false;
   }
}

