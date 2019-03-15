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

import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.firmware.FirmwareUpdate.MatchType;
import com.iris.model.Version;

@Singleton
public class XMLFirmwareResolver implements FirmwareUpdateResolver {
   private final FirmwareManager manager;

   @Inject
   public XMLFirmwareResolver(FirmwareManager manager) {
      this.manager = manager;
   }

   @Override
   public FirmwareResult getTargetForVersion(String version) {
      return getTargetForVersion(Version.fromRepresentation(version), null);
   }

   @Override
   public FirmwareResult getTargetForVersion(Version version) {
      return getTargetForVersion(version, null);
   }

   @Override
   public FirmwareResult getTargetForVersion(String version, String population) {
      return getTargetForVersion(Version.fromRepresentation(version), population);
   }

   @Override
   public FirmwareResult getTargetForVersion(Version version, String population) {
      List<FirmwareUpdate> updates = manager.getParsedData();
      if (updates == null || updates.isEmpty()) {
         return FirmwareResult.NO_UPGRADES_AVAILABLE;
      }
      String target = null;
      boolean exceedsMaximum = false;
      for (FirmwareUpdate update : updates) {
         FirmwareUpdate.MatchType match = update.matches(version, population);
         if (match == MatchType.VERSION_AND_POPULATION) {
            return new FirmwareResult(FirmwareResult.Result.UPGRADE_NEEDED, update.getTarget());
         }
         else if (match == MatchType.VERSION) {
            if (population == null || population.isEmpty()) {
               return new FirmwareResult(FirmwareResult.Result.UPGRADE_NEEDED, update.getTarget());
            }
            target = update.getTarget();
         }
         else if (update.exceedsMaximum(version, population)) {
            exceedsMaximum = true;
         }
      }
      return target != null 
            ? new FirmwareResult(FirmwareResult.Result.UPGRADE_NEEDED, target)
            : exceedsMaximum 
               ? FirmwareResult.UPGRADE_NOT_NEEDED 
               : FirmwareResult.UPGRADE_NOT_POSSIBLE;
   }


}

