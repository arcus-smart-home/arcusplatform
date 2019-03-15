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

import java.util.Date;
import java.util.Map;

import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import org.eclipse.jdt.annotation.Nullable;

import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.messages.model.DriverId;

/**
 * Tagging base class for events that are handled by a driver.
 */
// TODO should we add ReceivedPlatformMessage / ReceivedProtocolMessage, make them all DriverEvent
// TODO should context be packed in here? might be a bit cleaner than thread local...
public abstract class DriverEvent {
   // Most are singletons for the moment
   private static final DeviceDisassociatedEvent   DISASSOCIATED = new DeviceDisassociatedEvent();
   private static final DriverStartedEvent         STARTED = new DriverStartedEvent();
   private static final DriverStoppedEvent         STOPPED = new DriverStoppedEvent();

   public static enum ActionAfterHandled {
      COMMIT,
      INITIALIZE_BINDINGS,
      NONE
   }
   
   
   // Package scope to make sure new "types" of DriverEvents aren't created
   DriverEvent() { }
   
   public String toString() { return getClass().getSimpleName(); }
   
   public static DeviceAssociatedEvent createAssociated(AttributeMap attributes) {
      return new DeviceAssociatedEvent(attributes);
   }
   
   public static DeviceConnectedEvent createConnected(Integer reflexVersion) {
      return new DeviceConnectedEvent(reflexVersion);
   }
   
   public static DeviceDisconnectedEvent createDisconnected(Integer reflexVersion) {
      return new DeviceDisconnectedEvent(reflexVersion);
   }

   public static DeviceDisassociatedEvent createDisassociated() {
      return DISASSOCIATED;
   }

   public static DriverStartedEvent driverStarted() {
      return STARTED;
   }
   
   public static DriverUpgradedEvent driverUpgraded(DriverId oldDriver) {
      return new DriverUpgradedEvent(oldDriver);
   }
   
   public static DriverStoppedEvent driverStopped() {
      return STOPPED;
   }
   
   public static DeviceAttributesUpdatedEvent createAttributesUpdated(Map<AttributeKey<?>,Object> attributes, Integer reflexVersion, boolean isDeviceMessage) {
      return new DeviceAttributesUpdatedEvent(attributes, reflexVersion, isDeviceMessage);
   }
   
   public static ScheduledDriverEvent createScheduledEvent(
         String name, 
         @Nullable Object data,
         @Nullable Address actor,
         @Nullable Date runAt
   ) {
      return new ScheduledDriverEvent(name, data, actor, runAt == null ? System.currentTimeMillis() : runAt.getTime());
   }

   public ActionAfterHandled getActionAfterHandled() {
      return ActionAfterHandled.COMMIT;
   }
}

