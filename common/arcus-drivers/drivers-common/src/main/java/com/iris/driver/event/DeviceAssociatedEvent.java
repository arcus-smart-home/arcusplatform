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
package com.iris.driver.event;

import java.util.HashMap;
import java.util.Map;

import com.iris.device.attributes.AttributeMap;

/**
 * Fired when a device is newly associated with a driver,
 * see {@link DriverEvent#createAssociated()}.
 * Generally this happens shortly after the device is discovered.
 */
public final class DeviceAssociatedEvent extends DriverEvent {
   private final Map<String, Object> attributes;
   
   DeviceAssociatedEvent(AttributeMap attributes) { 
      this.attributes = attributes.toMap();
   }
   
   public Map<String, Object> getAttributes() {
      return new HashMap<String, Object>(attributes);
   }

   @Override
   public ActionAfterHandled getActionAfterHandled() {
      return ActionAfterHandled.INITIALIZE_BINDINGS;
   }
}

