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
package com.iris.core.driver;

import java.util.Arrays;

import com.iris.messages.model.DriverId;

public class DeviceDriverEntity {

   private final DriverId driverId;
   private final String description;
   private final byte[] implementation;

   public DeviceDriverEntity(DriverId driverId, String description, String scriptContents) {
      this(driverId, description, scriptContents.getBytes());
   }

   public DeviceDriverEntity(DriverId driverId, String description, byte[] implementation) {
      this.driverId = driverId;
      this.description = description;
      this.implementation = new byte[implementation.length];
      System.arraycopy(implementation, 0, this.implementation, 0, implementation.length);
   }

   public DriverId getDriverId() {
      return driverId;
   }

   public String getDescription() {
      return description;
   }

   public byte[] getImplementation() {
      return implementation;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
            + ((description == null) ? 0 : description.hashCode());
      result = prime * result + ((driverId == null) ? 0 : driverId.hashCode());
      result = prime * result + Arrays.hashCode(implementation);
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      DeviceDriverEntity other = (DeviceDriverEntity) obj;
      if (description == null) {
         if (other.description != null)
            return false;
      } else if (!description.equals(other.description))
         return false;
      if (driverId == null) {
         if (other.driverId != null)
            return false;
      } else if (!driverId.equals(other.driverId))
         return false;
      if (!Arrays.equals(implementation, other.implementation))
         return false;
      return true;
   }
}

