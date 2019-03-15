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
package com.iris.messages.model;

import java.util.Date;

public class BlacklistedHub implements Copyable<BlacklistedHub> {
   private String certSn;
   private Date blacklistedDate;
   private String hubid;
   private String reason;
   
   public String getCertSn() {
      return certSn;
   }

   public void setCertSn(String certSn) {
      this.certSn = certSn;
   }

   public Date getBlacklistedDate() {
      return blacklistedDate;
   }

   public void setBlacklistedDate(Date blacklistedDate) {
      this.blacklistedDate = blacklistedDate;
   }

   public String getHubid() {
      return hubid;
   }

   public void setHubid(String hubid) {
      this.hubid = hubid;
   }

   public String getReason() {
      return reason;
   }

   public void setReason(String reason) {
      this.reason = reason;
   }
   
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
            + ((blacklistedDate == null) ? 0 : blacklistedDate.hashCode());
      result = prime * result + ((certSn == null) ? 0 : certSn.hashCode());
      result = prime * result + ((hubid == null) ? 0 : hubid.hashCode());
      result = prime * result + ((reason == null) ? 0 : reason.hashCode());
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
      BlacklistedHub other = (BlacklistedHub) obj;
      if (blacklistedDate == null) {
         if (other.blacklistedDate != null)
            return false;
      } else if (!blacklistedDate.equals(other.blacklistedDate))
         return false;
      if (certSn == null) {
         if (other.certSn != null)
            return false;
      } else if (!certSn.equals(other.certSn))
         return false;
      if (hubid == null) {
         if (other.hubid != null)
            return false;
      } else if (!hubid.equals(other.hubid))
         return false;
      if (reason == null) {
         if (other.reason != null)
            return false;
      } else if (!reason.equals(other.reason))
         return false;
      return true;
   }

   @Override
   public BlacklistedHub copy() {
      try {
         return (BlacklistedHub) clone();
      } catch(CloneNotSupportedException cnse) {
         throw new RuntimeException(cnse);
      }
   }

   @Override
   protected Object clone() throws CloneNotSupportedException {
      BlacklistedHub hub = (BlacklistedHub)super.clone();
      hub.blacklistedDate = this.blacklistedDate != null ? (Date) this.blacklistedDate.clone() : null;
      return hub;
   }
   
   

}

