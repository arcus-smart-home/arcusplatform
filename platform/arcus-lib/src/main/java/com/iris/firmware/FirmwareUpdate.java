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

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.iris.model.Version;

public class FirmwareUpdate {
   public enum MatchType { NONE, VERSION, VERSION_AND_POPULATION }
   private final String model;
   private final Version min;
   private final Version max;
   private final String target;
   private final Set<String> populations;
   
   public FirmwareUpdate(String model, String min, String max, String target, Set<String> populations) {
      Preconditions.checkArgument(!StringUtils.isEmpty(model), "The firmware hardware model cannot be empty");
      Preconditions.checkArgument(!StringUtils.isEmpty(target), "The firmware target cannot be empty");
      this.model = model;
      this.min = Version.fromRepresentation(min);
      this.max = Version.fromRepresentation(max);
      Preconditions.checkArgument(this.min.compareTo(this.max) >= 0, "The max version cannot be less than the min version for target " + target);
      this.target = target;
      this.populations = populations != null ? Collections.unmodifiableSet(populations) : Collections.emptySet();
   }
   
   public MatchType matches(Version version) {
   	return matches(version, null);
   }
   
   public MatchType matches(Version version, String population) {
   	if(StringUtils.isNotBlank(population)) {
   		if(matchVersion(version) && populations.contains(population.toLowerCase())) {   			
   				return MatchType.VERSION_AND_POPULATION;
   		}
   	}else{
   		if(matchVersion(version)) {
   			return MatchType.VERSION;
   		}
   	}
      return MatchType.NONE;
   }
   
   /**
    * Determines if the specified version exceeds the maximum version of this
    * update. This will be true if the major version matches the major version
    * of the maximum version and the version as a whole is greater than the maximum 
    * version.
    * 
    * The major version must match because there may be more than separate updates
    * defined for different major versions.
    * 
    * This check is done to determine if an upgrade cannot be found because the current
    * version is already up to date or better or because the current version cannot
    * be upgraded.
    * 
    * If no population is specified, then population will be ignored.
    * 
    * @param version The version to check.
    * @param population The population to use. This value can be null.
    * @return true if this version is the same major version as the maximum version of this update and exceeds it.
    */
   public boolean exceedsMaximum(Version version, String population) {
      if (StringUtils.isEmpty(population) || getPopulations().contains(population)) {
         return matchMaxMajorVersion(version) && (max.compareTo(version) > 0);
      }
      return false;
   }
   
   public Version getMin() {
      return min;
   }
   
   public Version getMax() {
      return max;
   }
   
   public String getTarget() {
      return target;
   }   
   
   public Set<String> getPopulations() {
      return populations;
   }

   public String getModel() {
      return model;
   }
   
   private boolean matchVersion(Version version) {
      return (min.compareTo(version) >= 0) && (max.compareTo(version) <= 0);
   }
   
   private boolean matchMaxMajorVersion(Version version) {
      return max.getMajor() == version.getMajor();
   }

   @Override
   public String toString() {
      return "FirmwareUpdate [min=" + min + ", max=" + max + ", target="
            + target + ", populations=" + populations + "]";
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      FirmwareUpdate that = (FirmwareUpdate) o;
      return model.equals(that.model) &&
              min.equals(that.min) &&
              max.equals(that.max) &&
              target.equals(that.target) &&
              populations.equals(that.populations);
   }

   @Override
   public int hashCode() {
      return Objects.hash(model, min, max, target, populations);
   }
}

