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
package com.iris.messages.model;

import com.iris.model.Version;

/**
 *
 */
public class DriverId implements Comparable<DriverId> {
	private final String name;
	private final Version version;

	public DriverId(String name, Version version) {
		this.name = name;
		this.version = version;
	}

	public String getName() {
		return name;
	}

	public Version getVersion() {
		return version;
	}

	public boolean isVersioned() {
	   return !Version.UNVERSIONED.equals(version);
   }

   @Override
   public int compareTo(DriverId o) {
      if (o == null) {
         return -1;
      }

      int comparison = getName().compareTo(o.getName());
      if (comparison == 0) {
         comparison = getVersion().compareTo(o.getVersion());
      }
      return comparison;
   }

	@Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((version == null) ? 0 : version.hashCode());
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
      DriverId other = (DriverId) obj;
      if (name == null) {
         if (other.name != null)
            return false;
      } else if (!name.equals(other.name))
         return false;
      if (version == null) {
         if (other.version != null)
            return false;
      } else if (!version.equals(other.version))
         return false;
      return true;
   }

	@Override
   public String toString() {
	   return "DriverId [name=" + name + ", version=" + (isVersioned() ? version : "<unspecified>") + "]";
   }

}

