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
package com.iris.firmware;

/**
 * Encapsulates the results of looking for a firmware update.
 * 
 * There are four types of results that are currently defined. The target firmware will only
 * be set if the result is Result.UPGRADE_NEEDED, otherwise it will be null.
 * 
 * Result.UPGRADE_NEEDED - A firmware upgrade is needed. The target will be set to the firmware to use.
 * 
 * Result.UPGRADE_NOT_POSSBILE - The current firmware cannot be upgraded to a current version. This is an error condition.
 * 
 * Result.UPGRADE_NOT_NEEDED - The current firmware does not need to be upgraded. This is not an error condition.
 * 
 * Result.NO_UPGRADES_AVAILABLE - There are not firmware targets available. This indicates the platform is jacked.
 * 
 * @author Erik Larson
 */
public class FirmwareResult {
   public final static FirmwareResult UPGRADE_NOT_POSSIBLE = new FirmwareResult(Result.UPGRADE_NOT_POSSIBLE, null);
   public final static FirmwareResult UPGRADE_NOT_NEEDED = new FirmwareResult(Result.UPGRADE_NOT_NEEDED, null);
   public final static FirmwareResult NO_UPGRADES_AVAILABLE = new FirmwareResult(Result.NO_UPGRADES_AVAILABLE, null);

   public enum Result { UPGRADE_NEEDED, UPGRADE_NOT_POSSIBLE, UPGRADE_NOT_NEEDED, NO_UPGRADES_AVAILABLE };
   
   private final Result result;  
   private final String target;
   
   public FirmwareResult(Result result, String target) {
      this.result = result;
      this.target = target;
   }

   public Result getResult() {
      return result;
   }

   public String getTarget() {
      return target;
   }
}

