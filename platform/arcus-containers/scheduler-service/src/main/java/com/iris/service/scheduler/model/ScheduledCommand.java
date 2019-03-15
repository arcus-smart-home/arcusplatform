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
package com.iris.service.scheduler.model;

import java.util.Map;

/**
 * 
 */
public class ScheduledCommand {
   private String id;
   private String scheduleId;
   private String messageType;
   private Map<String, Object> attributes;
   
   /**
    * 
    */
   public ScheduledCommand() {
      // TODO Auto-generated constructor stub
   }

   /**
    * @return the id
    */
   public String getId() {
      return id;
   }

   /**
    * @param id the id to set
    */
   public void setId(String id) {
      this.id = id;
   }

   /**
    * @return the scheduleId
    */
   public String getScheduleId() {
      return scheduleId;
   }

   /**
    * @param scheduleId the scheduleId to set
    */
   public void setScheduleId(String scheduleId) {
      this.scheduleId = scheduleId;
   }

   /**
    * @return the messageType
    */
   public String getMessageType() {
      return messageType;
   }

   /**
    * @param messageType the messageType to set
    */
   public void setMessageType(String messageType) {
      this.messageType = messageType;
   }

   /**
    * @return the attributes
    */
   public Map<String, Object> getAttributes() {
      return attributes;
   }

   /**
    * @param attributes the attributes to set
    */
   public void setAttributes(Map<String, Object> attributes) {
      this.attributes = attributes;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
            + ((attributes == null) ? 0 : attributes.hashCode());
      result = prime * result + ((id == null) ? 0 : id.hashCode());
      result = prime * result
            + ((messageType == null) ? 0 : messageType.hashCode());
      result = prime * result
            + ((scheduleId == null) ? 0 : scheduleId.hashCode());
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
      ScheduledCommand other = (ScheduledCommand) obj;
      if (attributes == null) {
         if (other.attributes != null) return false;
      }
      else if (!attributes.equals(other.attributes)) return false;
      if (id == null) {
         if (other.id != null) return false;
      }
      else if (!id.equals(other.id)) return false;
      if (messageType == null) {
         if (other.messageType != null) return false;
      }
      else if (!messageType.equals(other.messageType)) return false;
      if (scheduleId == null) {
         if (other.scheduleId != null) return false;
      }
      else if (!scheduleId.equals(other.scheduleId)) return false;
      return true;
   }

}

