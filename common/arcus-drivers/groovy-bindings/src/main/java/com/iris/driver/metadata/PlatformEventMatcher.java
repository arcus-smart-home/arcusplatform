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
package com.iris.driver.metadata;

import org.apache.commons.lang3.StringUtils;

import com.iris.capability.key.NamespacedKey;

/**
 *
 */
public class PlatformEventMatcher extends EventMatcher {
   private String capability;
   private String event;
   private String instance;
   
   public String getCapability() {
      return capability;
   }
   
   public void setCapability(String capability) {
      this.capability = capability;
   }
   
   public boolean matchesAnyCapability() {
      return StringUtils.isEmpty(capability);
   }
   
   public String getEvent() {
      return event;
   }
   
   public void setEvent(String event) {
      this.event = event;
   }
   
   public boolean matchesAnyEvent() {
      return StringUtils.isEmpty(event);
   }

   public String getInstance() {
      return instance;
   }
   
   public void setInstance(String instance) {
      this.instance = instance;
   }
   
   public boolean matchesAnyInstance() {
      return StringUtils.isEmpty(instance);
   }

   public NamespacedKey getMethodName() {
      return NamespacedKey.of(capability, event, instance);
   }

   @Override
   public String toString() {
      return "PlatformEventMatcher [capability=" + getStringOrWildcard(capability) + 
            ", event=" + getStringOrWildcard(event) +
            ", instance=" + getStringOrWildcard(instance) + "]";
   }

}

