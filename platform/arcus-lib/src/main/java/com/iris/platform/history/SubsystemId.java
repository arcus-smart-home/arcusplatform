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
package com.iris.platform.history;

import java.util.UUID;

import com.iris.messages.address.Address;
import com.iris.messages.model.CompositeId;

public class SubsystemId implements CompositeId<UUID,String> {

	private UUID placeId;
	private String subsystemName;
	
	public SubsystemId(Address address) {
	   try {
	      this.placeId = (UUID) address.getId();
	      this.subsystemName = (String) address.getGroup();
	   }
	   catch(ClassCastException e) {
	      throw new IllegalArgumentException("Invalid subsytem address [" + address.getRepresentation() + "]");
	   }
	}
	
	public SubsystemId(UUID placeId, String subsystemName) {
		this.placeId = placeId;
		this.subsystemName = subsystemName;
	}
	
	@Override
	public UUID getPrimaryId() {
		return placeId;
	}

	@Override
	public String getSecondaryId() {
		return subsystemName;
	}

	@Override
	public String getRepresentation() {
		return subsystemName + ":" + placeId.toString();
	}

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "SubsystemId [" + getRepresentation() + "]";
   }

   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((placeId == null) ? 0 : placeId.hashCode());
      result = prime * result
            + ((subsystemName == null) ? 0 : subsystemName.hashCode());
      return result;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      SubsystemId other = (SubsystemId) obj;
      if (placeId == null) {
         if (other.placeId != null) return false;
      }
      else if (!placeId.equals(other.placeId)) return false;
      if (subsystemName == null) {
         if (other.subsystemName != null) return false;
      }
      else if (!subsystemName.equals(other.subsystemName)) return false;
      return true;
   }
	
}

