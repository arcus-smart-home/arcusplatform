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
package com.iris.oculus.modules.session;

import java.util.Date;


/**
 *
 */
public class OculusSession {
   private String username;
   private String host;
   private String accountId;
   private String placeId;
   private String personId;
   private String billingPublicKey;
   private String sessionToken;
   private Date lastConnected;

   public OculusSession() {

   }

   public OculusSession(String userName, String accountId, String placeId) {
      this.username = userName;
      this.accountId = accountId;
      this.placeId = placeId;
   }
   
   public OculusSession(OculusSession other) {
      this.username = other.getUserName();
      this.host = other.getHost();
      this.accountId = other.getAccountId();
      this.placeId = other.getPlaceId();
      this.personId = other.getPersonId();
      this.billingPublicKey = other.getBillingPublicKey();
      this.sessionToken = other.getSessionToken();
      this.lastConnected = other.getLastConnected() == null ? null : new Date(other.getLastConnected().getTime());
   }

   public String getUserName() {
      return username;
   }

   public void setUserName(String userName) {
      this.username = userName;
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

   public String getAccountId() {
      return accountId;
   }

   public void setAccountId(String accountId) {
      this.accountId = accountId;
   }

   public String getPlaceId() {
      return placeId;
   }

   public void setPlaceId(String placeId) {
      this.placeId = placeId;
   }

   public void setPersonId(String personId) {
   	this.personId = personId;
   }

   public String getPersonId() {
   	return this.personId;
   }

   public String getBillingPublicKey() {
      return billingPublicKey;
   }

   public void setBillingPublicKey(String billingPublicKey) {
      this.billingPublicKey = billingPublicKey;
   }

   /**
    * @return the sessionToken
    */
   public String getSessionToken() {
      return sessionToken;
   }

   /**
    * @param sessionToken the sessionToken to set
    */
   public void setSessionToken(String sessionToken) {
      this.sessionToken = sessionToken;
   }

   /**
    * @return the lastConnected
    */
   public Date getLastConnected() {
      return lastConnected;
   }

   /**
    * @param lastConnected the lastConnected to set
    */
   public void setLastConnected(Date lastConnected) {
      this.lastConnected = lastConnected;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "Session [username=" + username + ", host=" + host
            + ", accountId=" + accountId + ", placeId=" + placeId
            + ", personId=" + personId + ", billingPublicKey="
            + billingPublicKey + ", sessionToken=" + sessionToken
            + ", lastConnected=" + lastConnected + "]";
   }

   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
            + ((accountId == null) ? 0 : accountId.hashCode());
      result = prime * result
            + ((billingPublicKey == null) ? 0 : billingPublicKey.hashCode());
      result = prime * result + ((host == null) ? 0 : host.hashCode());
      result = prime * result
            + ((lastConnected == null) ? 0 : lastConnected.hashCode());
      result = prime * result + ((personId == null) ? 0 : personId.hashCode());
      result = prime * result + ((placeId == null) ? 0 : placeId.hashCode());
      result = prime * result
            + ((sessionToken == null) ? 0 : sessionToken.hashCode());
      result = prime * result + ((username == null) ? 0 : username.hashCode());
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
      OculusSession other = (OculusSession) obj;
      if (accountId == null) {
         if (other.accountId != null) return false;
      }
      else if (!accountId.equals(other.accountId)) return false;
      if (billingPublicKey == null) {
         if (other.billingPublicKey != null) return false;
      }
      else if (!billingPublicKey.equals(other.billingPublicKey)) return false;
      if (host == null) {
         if (other.host != null) return false;
      }
      else if (!host.equals(other.host)) return false;
      if (lastConnected == null) {
         if (other.lastConnected != null) return false;
      }
      else if (!lastConnected.equals(other.lastConnected)) return false;
      if (personId == null) {
         if (other.personId != null) return false;
      }
      else if (!personId.equals(other.personId)) return false;
      if (placeId == null) {
         if (other.placeId != null) return false;
      }
      else if (!placeId.equals(other.placeId)) return false;
      if (sessionToken == null) {
         if (other.sessionToken != null) return false;
      }
      else if (!sessionToken.equals(other.sessionToken)) return false;
      if (username == null) {
         if (other.username != null) return false;
      }
      else if (!username.equals(other.username)) return false;
      return true;
   }

}

