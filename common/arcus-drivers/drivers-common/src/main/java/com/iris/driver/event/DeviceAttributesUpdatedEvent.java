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
package com.iris.driver.event;

import java.util.Map;

import com.iris.device.attributes.AttributeKey;

/**
 * Fired when a device has its attributes updated in an out
 * of band way. This means that no driver code should be
 * invoked while processing these updated attributes, though
 * normal value changes will occur if needed.
 * 
 * see {@link DriverEvent#createAttributesUpdated()}.
 */
public final class DeviceAttributesUpdatedEvent extends DriverEvent {
   private final Map<AttributeKey<?>,Object> attributes;
   private final Integer reflexVersion;
   private final boolean isDeviceMessage;

   DeviceAttributesUpdatedEvent(Map<AttributeKey<?>,Object> attributes, Integer reflexVersion, boolean isDeviceMessage) { 
      this.attributes = attributes;
      this.reflexVersion = reflexVersion;
      this.isDeviceMessage = isDeviceMessage;
   }

   public Integer getReflexVersion() {
      return reflexVersion;
   }

   public boolean isDeviceMessage() {
      return isDeviceMessage;
   }

   public Map<AttributeKey<?>,Object> getAttributes() {
      return attributes;
   }
}

