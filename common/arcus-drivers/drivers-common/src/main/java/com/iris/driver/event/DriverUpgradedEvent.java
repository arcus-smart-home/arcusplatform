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

import com.iris.messages.model.DriverId;
import com.iris.model.Version;

/**
 * 
 */
public class DriverUpgradedEvent extends DriverEvent {
   DriverId oldDriverId;
   
   DriverUpgradedEvent(DriverId oldVersion) {
      this.oldDriverId = oldVersion;
   }
   
   public DriverId getOldDriverId() {
      return oldDriverId;
   }

   public Version getOldVersion() {
      return oldDriverId.getVersion();
   }

   public String getOldName() {
      return oldDriverId.getName();
   }

   @Override
   public String toString() { return "DriverUpgradedEvent [oldVersion=" + oldDriverId + "]"; }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
            + ((oldDriverId == null) ? 0 : oldDriverId.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      DriverUpgradedEvent other = (DriverUpgradedEvent) obj;
      if (oldDriverId == null) {
         if (other.oldDriverId != null) return false;
      }
      else if (!oldDriverId.equals(other.oldDriverId)) return false;
      return true;
   }


   @Override
   public ActionAfterHandled getActionAfterHandled() {
      return ActionAfterHandled.NONE;
   }
}

