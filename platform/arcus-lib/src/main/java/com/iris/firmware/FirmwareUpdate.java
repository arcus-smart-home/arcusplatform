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
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.iris.model.Version;

public class FirmwareUpdate {
   public enum MatchType { NONE, VERSION, VERSION_AND_POPULATION }
   
   private final Version min;
   private final Version max;
   private final String target;
   private final Set<String> populations;
   
   public FirmwareUpdate(String min, String max, String target, Set<String> populations) {
      Preconditions.checkArgument(!StringUtils.isEmpty(target), "The firmware target cannot be empty");
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
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((max == null) ? 0 : max.hashCode());
      result = prime * result + ((min == null) ? 0 : min.hashCode());
      result = prime * result
            + ((populations == null) ? 0 : populations.hashCode());
      result = prime * result + ((target == null) ? 0 : target.hashCode());
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
      FirmwareUpdate other = (FirmwareUpdate) obj;
      if (max == null) {
         if (other.max != null)
            return false;
      } else if (!max.equals(other.max))
         return false;
      if (min == null) {
         if (other.min != null)
            return false;
      } else if (!min.equals(other.min))
         return false;
      if (populations == null) {
         if (other.populations != null)
            return false;
      } else if (!populations.equals(other.populations))
         return false;
      if (target == null) {
         if (other.target != null)
            return false;
      } else if (!target.equals(other.target))
         return false;
      return true;
   }
}

