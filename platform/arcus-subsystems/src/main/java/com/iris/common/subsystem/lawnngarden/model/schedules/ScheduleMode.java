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
package com.iris.common.subsystem.lawnngarden.model.schedules;

import com.iris.messages.capability.LawnNGardenSubsystemCapability;

public enum ScheduleMode {
   INTERVAL(LawnNGardenSubsystemCapability.ATTR_INTERVALSCHEDULES),
   ODD(LawnNGardenSubsystemCapability.ATTR_ODDSCHEDULES),
   EVEN(LawnNGardenSubsystemCapability.ATTR_EVENSCHEDULES),
   WEEKLY(LawnNGardenSubsystemCapability.ATTR_WEEKLYSCHEDULES);

   private final String modelAttribute;

   ScheduleMode(String modelAttribute) {
      this.modelAttribute = modelAttribute;
   }

   public String getModelAttribute() {
      return modelAttribute;
   }
}

