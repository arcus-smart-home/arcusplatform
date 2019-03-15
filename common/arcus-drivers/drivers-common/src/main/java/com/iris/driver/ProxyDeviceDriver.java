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
package com.iris.driver;

import java.util.Map;

import com.google.common.base.Preconditions;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.driver.event.DriverEvent;
import com.iris.driver.service.registry.DriverRegistry;
import com.iris.messages.ErrorEvent;
import com.iris.messages.PlatformMessage;
import com.iris.messages.model.DriverId;
import com.iris.protocol.ProtocolMessage;

/**
 * Driver which reloads the actual driver for each request.
 */
public class ProxyDeviceDriver implements DeviceDriver {
   private final DriverRegistry registry;
   private final DriverId driverId;

   /**
    * 
    */
   public ProxyDeviceDriver(DriverRegistry registry, DriverId driverId) {
      Preconditions.checkNotNull(registry, "registry may not be null");
      Preconditions.checkNotNull(driverId, "driverId may not be null");
      this.registry = registry;
      this.driverId = driverId;
   }

   protected DeviceDriver delegate() {
      DeviceDriver driver = registry.loadDriverById(driverId);
      if(driver == null) {
         throw new DriverNotFoundException(driverId);
      }
      return driver;
   }

   protected DeviceDriver delegate(DeviceDriverContext context) {
      DeviceDriver driver = delegate();
      context.setAttributeValue(DeviceDriverImpl.DRIVER_HASH, driver.getDefinition().getHash());
      context.setAttributeValue(DeviceDriverImpl.DRIVER_COMMIT, driver.getDefinition().getCommit());
      return driver;
   }

   /* (non-Javadoc)
    * @see com.iris.driver.DeviceDriver#getDriverId()
    */
   @Override
   public DriverId getDriverId() {
      return driverId;
   }

   /* (non-Javadoc)
    * @see com.iris.driver.DeviceDriver#supports(com.iris.device.attributes.AttributeMap)
    */
   @Override
   public boolean supports(AttributeMap attributes) {
      return delegate().supports(attributes);
   }

   /* (non-Javadoc)
    * @see com.iris.driver.DeviceDriver#getDefinition()
    */
   @Override
   public DeviceDriverDefinition getDefinition() {
      return delegate().getDefinition();
   }

   /* (non-Javadoc)
    * @see com.iris.driver.DeviceDriver#getBaseAttributes()
    */
   @Override
   public AttributeMap getBaseAttributes() {
      return delegate().getBaseAttributes();
   }

   /* (non-Javadoc)
    * @see com.iris.driver.DeviceDriver#onRestored(com.iris.driver.DeviceDriverContext)
    */
   @Override
   public void onRestored(DeviceDriverContext context) {
      delegate(context).onRestored(context);
   }

   /* (non-Javadoc)
    * @see com.iris.driver.DeviceDriver#onUpgraded(com.iris.messages.model.DriverId, com.iris.driver.DeviceDriverContext)
    */
   @Override
   public void onUpgraded(DriverEvent event, DriverId previous, DeviceDriverContext context) throws Exception {
      delegate(context).onUpgraded(event, previous, context);
   }

   /* (non-Javadoc)
    * @see com.iris.driver.DeviceDriver#onSuspended(com.iris.driver.DeviceDriverContext)
    */
   @Override
   public void onSuspended(DeviceDriverContext context) {
      delegate(context).onSuspended(context);
   }

   /* (non-Javadoc)
    * @see com.iris.driver.DeviceDriver#onAttributesUpdated(com.iris.driver.DeviceDriverContext, java.util.Map)
    */
   @Override
	public void onAttributesUpdated(DeviceDriverContext context, Map<AttributeKey<?>,Object> attributes, Integer reflexVersion, boolean isDeviceMessage) {
      delegate(context).onAttributesUpdated(context, attributes, reflexVersion, isDeviceMessage);
	}

   /* (non-Javadoc)
    * @see com.iris.driver.DeviceDriver#handleDriverEvent(com.iris.driver.event.DriverEvent, com.iris.driver.DeviceDriverContext)
    */
   @Override
   public void handleDriverEvent(DriverEvent event, DeviceDriverContext context) throws Exception {
      delegate(context).handleDriverEvent(event, context);
   }

   /* (non-Javadoc)
    * @see com.iris.driver.DeviceDriver#handleProtocolMessage(com.iris.protocol.ProtocolMessage, com.iris.driver.DeviceDriverContext)
    */
   @Override
   public void handleProtocolMessage(ProtocolMessage message, DeviceDriverContext context) {
      delegate(context).handleProtocolMessage(message, context);
   }

   /* (non-Javadoc)
    * @see com.iris.driver.DeviceDriver#handlePlatformMessage(com.iris.messages.PlatformMessage, com.iris.driver.DeviceDriverContext)
    */
   @Override
   public void handlePlatformMessage(PlatformMessage message, DeviceDriverContext context) {
      delegate(context).handlePlatformMessage(message, context);
   }

   /* (non-Javadoc)
    * @see com.iris.driver.DeviceDriver#handleError(com.iris.messages.ErrorEvent, com.iris.driver.DeviceDriverContext)
    */
   @Override
   public void handleError(ErrorEvent error, DeviceDriverContext context) {
      delegate(context).handleError(error, context);
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "ProxyDeviceDriver [driverId=" + driverId + "]";
   }

   public static class DriverNotFoundException extends RuntimeException  {

      public DriverNotFoundException(DriverId driverId) {
         super("Unable to load driver [" + driverId + "]");
      }
   }
}

