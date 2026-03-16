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
package com.iris.driver;

import java.util.Map;

import com.google.common.base.Predicate;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.driver.event.DriverEvent;
import com.iris.messages.ErrorEvent;
import com.iris.messages.PlatformMessage;
import com.iris.messages.model.DriverId;
import com.iris.protocol.ProtocolMessage;

/**
 * A lightweight DeviceDriver that holds only definition, matcher, and base
 * attributes. Used in lazy-loading mode to keep discovery matching functional
 * without retaining full Groovy handler closures in memory.
 *
 * Handler methods throw UnsupportedOperationException. Callers requiring
 * message handling should obtain a full driver via loadDriverById().
 */
public class LightweightDeviceDriver implements DeviceDriver {
   private final DeviceDriverDefinition definition;
   private final Predicate<AttributeMap> matcher;
   private final AttributeMap baseAttributes;

   public LightweightDeviceDriver(
         DeviceDriverDefinition definition,
         Predicate<AttributeMap> matcher,
         AttributeMap baseAttributes
   ) {
      this.definition = definition;
      this.matcher = matcher;
      this.baseAttributes = baseAttributes;
   }

   @Override
   public DriverId getDriverId() {
      return definition.getId();
   }

   @Override
   public boolean supports(AttributeMap attributes) {
      return matcher.apply(attributes);
   }

   @Override
   public DeviceDriverDefinition getDefinition() {
      return definition;
   }

   @Override
   public AttributeMap getBaseAttributes() {
      return baseAttributes;
   }

   @Override
   public void onRestored(DeviceDriverContext context) {
      throw new UnsupportedOperationException("LightweightDeviceDriver does not support message handling");
   }

   @Override
   public void onUpgraded(DriverEvent event, DriverId previous, DeviceDriverContext context) throws Exception {
      throw new UnsupportedOperationException("LightweightDeviceDriver does not support message handling");
   }

   @Override
   public void onSuspended(DeviceDriverContext context) {
      throw new UnsupportedOperationException("LightweightDeviceDriver does not support message handling");
   }

   @Override
   public void onAttributesUpdated(DeviceDriverContext context, Map<AttributeKey<?>, Object> attributes, Integer reflexVersion, boolean isDeviceMessage) {
      throw new UnsupportedOperationException("LightweightDeviceDriver does not support message handling");
   }

   @Override
   public void handleDriverEvent(DriverEvent event, DeviceDriverContext context) throws Exception {
      throw new UnsupportedOperationException("LightweightDeviceDriver does not support message handling");
   }

   @Override
   public void handleProtocolMessage(ProtocolMessage message, DeviceDriverContext context) {
      throw new UnsupportedOperationException("LightweightDeviceDriver does not support message handling");
   }

   @Override
   public void handlePlatformMessage(PlatformMessage message, DeviceDriverContext context) {
      throw new UnsupportedOperationException("LightweightDeviceDriver does not support message handling");
   }

   @Override
   public void handleError(ErrorEvent error, DeviceDriverContext context) {
      throw new UnsupportedOperationException("LightweightDeviceDriver does not support message handling");
   }

   @Override
   public String toString() {
      return "LightweightDeviceDriver [id=" + definition.getId() + "]";
   }
}
