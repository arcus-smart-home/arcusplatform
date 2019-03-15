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
package com.iris.platform.scene;

import java.util.Arrays;
import java.util.Date;

import com.iris.messages.capability.SceneCapability;
import com.iris.platform.rule.BaseDefinition;

public class SceneDefinition extends BaseDefinition<SceneDefinition> {
   private String template;
   private boolean satisfiable;
   private boolean notification;
   private Date lastFireTime;
   private String lastFireState;
   private boolean enabled;

   // the contents are not strictly enforced by the DAO, up to the service
   // to guarantee consistent usage
   private byte [] action;

   public SceneDefinition() {
      // TODO Auto-generated constructor stub
   }

   public boolean isEnabled() {
      return enabled;
   }
   

   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }

   /* (non-Javadoc)
    * @see com.iris.platform.rule.BaseDefinition#getType()
    */
   @Override
   public String getType() {
      return SceneCapability.NAMESPACE;
   }

   /**
    * @return the template
    */
   public String getTemplate() {
      return template;
   }

   /**
    * @param template the template to set
    */
   public void setTemplate(String template) {
      this.template = template;
   }

   /**
    * @return the satisfiable
    */
   public boolean isSatisfiable() {
      return satisfiable;
   }

   /**
    * @param satisfiable the satisfiable to set
    */
   public void setSatisfiable(boolean satisfiable) {
      this.satisfiable = satisfiable;
   }

   /**
    * @return the notification
    */
   public boolean isNotification() {
      return notification;
   }

   /**
    * @param notification the notification to set
    */
   public void setNotification(boolean notification) {
      this.notification = notification;
   }

   /**
    * @return the lastFireTime
    */
   public Date getLastFireTime() {
      return lastFireTime;
   }

   /**
    * @param lastFireTime the lastFireTime to set
    */
   public void setLastFireTime(Date lastFireTime) {
      this.lastFireTime = lastFireTime;
   }

   /**
    * @return the lastFireState
    */
   public String getLastFireState() {
      return lastFireState;
   }

   /**
    * @param lastFireState the lastFireState to set
    */
   public void setLastFireState(String lastFireState) {
      this.lastFireState = lastFireState;
   }

   /**
    * @return the action
    */
   public byte[] getAction() {
      return action;
   }

   /**
    * @param action the action to set
    */
   public void setAction(byte[] action) {
      this.action = action;
   }

   @Override
   public SceneDefinition copy() {
      SceneDefinition copy = super.copy();
      if(action != null) {
         copy.action = new byte[action.length];
         System.arraycopy(action, 0, copy.action, 0, action.length);
      }
      return copy;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "SceneDefinition [placeId=" + getPlaceId() + ", sequenceId="
            + getSequenceId() + ", name=" + getName() + ", description=" + getDescription()
            + ", template=" + template + ", satisfiable=" + satisfiable + ", created="
            + getCreated() + ", modified=" + getModified()
            + ", lastFireTime=" + lastFireTime + ", lastFireState="
            + lastFireState + ", action=" + Arrays.toString(action) + "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + Arrays.hashCode(action);
      result = prime * result + (enabled ? 1231 : 1237);
      result = prime * result + ((lastFireState == null) ? 0 : lastFireState.hashCode());
      result = prime * result + ((lastFireTime == null) ? 0 : lastFireTime.hashCode());
      result = prime * result + (notification ? 1231 : 1237);
      result = prime * result + (satisfiable ? 1231 : 1237);
      result = prime * result + ((template == null) ? 0 : template.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (!super.equals(obj))
         return false;
      if (getClass() != obj.getClass())
         return false;
      SceneDefinition other = (SceneDefinition) obj;
      if (!Arrays.equals(action, other.action))
         return false;
      if (enabled != other.enabled)
         return false;
      if (lastFireState == null) {
         if (other.lastFireState != null)
            return false;
      }else if (!lastFireState.equals(other.lastFireState))
         return false;
      if (lastFireTime == null) {
         if (other.lastFireTime != null)
            return false;
      }else if (!lastFireTime.equals(other.lastFireTime))
         return false;
      if (notification != other.notification)
         return false;
      if (satisfiable != other.satisfiable)
         return false;
      if (template == null) {
         if (other.template != null)
            return false;
      }else if (!template.equals(other.template))
         return false;
      return true;
   }

}

