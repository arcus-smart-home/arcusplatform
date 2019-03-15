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
package com.iris.platform.cluster;

import java.time.Instant;

import com.iris.messages.model.Copyable;

/**
 * 
 */
public class ClusterServiceRecord implements Copyable<ClusterServiceRecord> {
   private String host;
   private String service;
   private int memberId;
   private Instant registered;
   private Instant lastHeartbeat;

   /**
    * 
    */
   public ClusterServiceRecord() {
      // TODO Auto-generated constructor stub
   }
   
   protected ClusterServiceRecord(ClusterServiceRecord copy) {
      this.host = copy.getHost();
      this.service = copy.getService();
      this.memberId = copy.getMemberId();
      this.registered = copy.getRegistered();
      this.lastHeartbeat = copy.getLastHeartbeat();
   }

   /**
    * @return the host
    */
   public String getHost() {
      return host;
   }

   /**
    * @param host the host to set
    */
   public void setHost(String host) {
      this.host = host;
   }

   /**
    * @return the service
    */
   public String getService() {
      return service;
   }

   /**
    * @param service the service to set
    */
   public void setService(String service) {
      this.service = service;
   }

   /**
    * @return the memberId
    */
   public int getMemberId() {
      return memberId;
   }

   /**
    * @param memberId the memberId to set
    */
   public void setMemberId(int memberId) {
      this.memberId = memberId;
   }

   /**
    * @return the registered
    */
   public Instant getRegistered() {
      return registered;
   }

   /**
    * @param registered the registered to set
    */
   public void setRegistered(Instant registered) {
      this.registered = registered;
   }

   /**
    * @return the lastHeartbeat
    */
   public Instant getLastHeartbeat() {
      return lastHeartbeat;
   }

   /**
    * @param lastHeartbeat the lastHeartbeat to set
    */
   public void setLastHeartbeat(Instant lastHeartbeat) {
      this.lastHeartbeat = lastHeartbeat;
   }

   public ClusterServiceRecord copy() {
      return new ClusterServiceRecord(this);
   }
   
   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "ClusterServiceRecord [host=" + host + ", service=" + service
            + ", memberId=" + memberId + ", registered=" + registered
            + ", lastHeartbeat=" + lastHeartbeat + "]";
   }

   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((host == null) ? 0 : host.hashCode());
      result = prime * result
            + ((lastHeartbeat == null) ? 0 : lastHeartbeat.hashCode());
      result = prime * result + memberId;
      result = prime * result
            + ((registered == null) ? 0 : registered.hashCode());
      result = prime * result + ((service == null) ? 0 : service.hashCode());
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
      ClusterServiceRecord other = (ClusterServiceRecord) obj;
      if (host == null) {
         if (other.host != null) return false;
      }
      else if (!host.equals(other.host)) return false;
      if (lastHeartbeat == null) {
         if (other.lastHeartbeat != null) return false;
      }
      else if (!lastHeartbeat.equals(other.lastHeartbeat)) return false;
      if (memberId != other.memberId) return false;
      if (registered == null) {
         if (other.registered != null) return false;
      }
      else if (!registered.equals(other.registered)) return false;
      if (service == null) {
         if (other.service != null) return false;
      }
      else if (!service.equals(other.service)) return false;
      return true;
   }

}

