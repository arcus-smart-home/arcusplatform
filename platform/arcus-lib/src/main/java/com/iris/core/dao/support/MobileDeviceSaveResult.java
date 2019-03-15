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
package com.iris.core.dao.support;

import java.util.UUID;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.iris.messages.model.MobileDevice;

/*
 * class to track if the associated person changed on MobileDeviceAdd
 */
public class MobileDeviceSaveResult {
   private final MobileDevice device;   
   private boolean ownerChanged; 
   private UUID oldOwnerId; 

   public MobileDeviceSaveResult(MobileDevice device) {
      super();
      this.device = device;
   }
   // wrapping these two up into one call for safety
   public void setOwnerChangedForId(UUID oldOwnerId) {
      this.oldOwnerId = oldOwnerId;
      ownerChanged = true;
   }
   public boolean isOwnerChanged() {
      return ownerChanged;
   }

   public UUID getOldOwnerId() {
      return oldOwnerId;
   }

   public MobileDevice getDevice() {
      return device;
   }

   @Override
   public String toString() {
      return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
   }

}

