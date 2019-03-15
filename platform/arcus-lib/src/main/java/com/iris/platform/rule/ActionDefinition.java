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
package com.iris.platform.rule;

import java.util.Arrays;
import java.util.Date;

public class ActionDefinition extends BaseDefinition<ActionDefinition> {
   private Date lastExecuted;
   // the contents are not strictly enforced by the DAO, up to the service
   // to guarantee consistent usage
   private byte [] action;

   public ActionDefinition() {
      
   }

   @Override
   public String getType() {
      // not directly exposed right now...
      throw new UnsupportedOperationException();
   }

   public Date getLastExecuted() {
      return lastExecuted;
   }

   public void setLastExecuted(Date lastExecuted) {
      this.lastExecuted = lastExecuted;
   }

   public byte[] getAction() {
      return action;
   }

   public void setAction(byte[] action) {
      this.action = action;
   }

   @Override
   public ActionDefinition copy() {
      ActionDefinition copy = super.copy();
      if(lastExecuted != null) {
         copy.lastExecuted = new Date(lastExecuted.getTime());
      }
      if(action != null) {
         copy.action = new byte[action.length];
         System.arraycopy(action, 0, copy.action, 0, action.length);
      }
      return copy;
   }

   @Override
   public String toString() {
      return "ActionDefinition [placeId=" + getPlaceId() + ", actionId=" + getSequenceId()
            + ", name=" + getName() + ", description=" + getDescription() + ", created="
            + getCreated() + ", lastUpdated=" + getModified() + ", lastExecuted="
            + lastExecuted + "]";
   }

   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + Arrays.hashCode(action);
      result = prime * result
            + ((lastExecuted == null) ? 0 : lastExecuted.hashCode());
      return result;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!super.equals(obj)) return false;
      if (getClass() != obj.getClass()) return false;
      ActionDefinition other = (ActionDefinition) obj;
      if (!Arrays.equals(action, other.action)) return false;
      if (lastExecuted == null) {
         if (other.lastExecuted != null) return false;
      }
      else if (!lastExecuted.equals(other.lastExecuted)) return false;
      return true;
   }

}

