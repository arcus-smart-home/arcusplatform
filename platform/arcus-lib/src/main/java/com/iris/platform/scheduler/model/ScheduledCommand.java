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
package com.iris.platform.scheduler.model;

import java.util.Date;
import java.util.UUID;

import com.iris.messages.address.Address;

/**
 * 
 */
public class ScheduledCommand {
   private PartitionOffset offset;
   private UUID placeId;
   private Address schedulerAddress;
   private Date scheduledTime;
   private Date expirationTime;

   /**
    * 
    */
   public ScheduledCommand() {
      // TODO Auto-generated constructor stub
   }

   /**
    * @return the offset
    */
   public PartitionOffset getOffset() {
      return offset;
   }

   /**
    * @param offset the offset to set
    */
   public void setOffset(PartitionOffset offset) {
      this.offset = offset;
   }

   /**
    * @return the placeId
    */
   public UUID getPlaceId() {
      return placeId;
   }

   /**
    * @param placeId the placeId to set
    */
   public void setPlaceId(UUID placeId) {
      this.placeId = placeId;
   }

   /**
    * @return the schedulerAddress
    */
   public Address getSchedulerAddress() {
      return schedulerAddress;
   }

   /**
    * @return the scheduledTime
    */
   public Date getScheduledTime() {
      return scheduledTime;
   }

   /**
    * @param scheduledTime the scheduledTime to set
    */
   public void setScheduledTime(Date scheduledTime) {
      this.scheduledTime = scheduledTime;
   }

   /**
    * @return the expirationTime
    */
   public Date getExpirationTime() {
      return expirationTime;
   }

   /**
    * @param expirationTime the expirationTime to set
    */
   public void setExpirationTime(Date expirationTime) {
      this.expirationTime = expirationTime;
   }

   /**
    * @param schedulerAddress the schedulerAddress to set
    */
   public void setSchedulerAddress(Address schedulerAddress) {
      this.schedulerAddress = schedulerAddress;
   }

   public boolean isExpired() {
      return isExpired(System.currentTimeMillis());
   }
   
   public boolean isExpired(Date now) {
      return now != null ? isExpired(now.getTime()) : isExpired();
   }
   
   private boolean isExpired(long ts) {
      return expirationTime != null && expirationTime.getTime() <= ts;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "ScheduledCommand [placeId=" + placeId + ", schedulerAddress="
            + schedulerAddress + ", scheduledTime=" + scheduledTime
            + ", expirationTime=" + (expirationTime != null ? expirationTime : "never")+ "]";
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
            + ((scheduledTime == null) ? 0 : scheduledTime.hashCode());
      result = prime * result
            + ((schedulerAddress == null) ? 0 : schedulerAddress.hashCode());
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
      if (placeId == null) {
         if (other.placeId != null) return false;
      }
      else if (!placeId.equals(other.placeId)) return false;
      if (scheduledTime == null) {
         if (other.scheduledTime != null) return false;
      }
      else if (!scheduledTime.equals(other.scheduledTime)) return false;
      if (schedulerAddress == null) {
         if (other.schedulerAddress != null) return false;
      }
      else if (!schedulerAddress.equals(other.schedulerAddress)) return false;
      return true;
   }

}

