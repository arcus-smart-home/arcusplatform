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

/**
 * Fired when a device is "disconnected", the exact definition
 * of connected is controlled by the driver, but generally
 * it means the device is unavailable to receive commands.
 * This is often caused by a timeout.
 * See {@link DriverEvent#createDisconnected()}.
 */
public final class DeviceDisconnectedEvent extends DriverEvent {
   private final Integer reflexVersion;

   public DeviceDisconnectedEvent(Integer reflexVersion) {
      this.reflexVersion = reflexVersion;
   }

   public Integer getReflexVersion() {
      return reflexVersion;
   }
}

