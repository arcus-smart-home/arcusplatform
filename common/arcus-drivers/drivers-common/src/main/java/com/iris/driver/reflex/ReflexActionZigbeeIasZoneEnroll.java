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
package com.iris.driver.reflex;

import org.eclipse.jdt.annotation.Nullable;

public final class ReflexActionZigbeeIasZoneEnroll implements ReflexAction {
   private final int endpointId;
   private final int profileId;
   private final int clusterId;

   public ReflexActionZigbeeIasZoneEnroll(int endpointId, int profileId, int clusterId) {
      this.endpointId = endpointId;
      this.profileId = profileId;
      this.clusterId = clusterId;
   }

   public int getEndpointId() {
      return endpointId;
   }

   public int getProfileId() {
      return profileId;
   }

   public int getClusterId() {
      return clusterId;
   }

   @Override
   public String toString() {
      return "ReflexActionZigbeeIasZoneEnroll [" + 
         "profile=" + profileId +
         ",endpoint=" + endpointId +
         ",cluster=" + clusterId +
         "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + clusterId;
      result = prime * result + endpointId;
      result = prime * result + profileId;
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
      ReflexActionZigbeeIasZoneEnroll other = (ReflexActionZigbeeIasZoneEnroll) obj;
      if (clusterId != other.clusterId)
         return false;
      if (endpointId != other.endpointId)
         return false;
      if (profileId != other.profileId)
         return false;
      return true;
   }
}

