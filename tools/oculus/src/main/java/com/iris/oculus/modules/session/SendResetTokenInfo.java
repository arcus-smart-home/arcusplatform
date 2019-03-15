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

/**
 * @author tweidlin
 *
 */
public class SendResetTokenInfo {
   public static enum NotificationMethod {
      EMAIL, IVR;
   }
   
   private String username;
   private NotificationMethod method;

   public SendResetTokenInfo() {
      
   }
   
   public SendResetTokenInfo(String username, NotificationMethod method) {
      this.username = username;
      this.method = method;
   }

   /**
    * @return the username
    */
   public String getUsername() {
      return username;
   }

   /**
    * @param username the username to set
    */
   public void setUsername(String username) {
      this.username = username;
   }

   /**
    * @return the method
    */
   public NotificationMethod getMethod() {
      return method;
   }

   /**
    * @param method the method to set
    */
   public void setMethod(NotificationMethod method) {
      this.method = method;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "ResetPasswordInfo [username=" + username + ", method=" + method
            + "]";
   }

   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((method == null) ? 0 : method.hashCode());
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
      SendResetTokenInfo other = (SendResetTokenInfo) obj;
      if (method != other.method) return false;
      if (username == null) {
         if (other.username != null) return false;
      }
      else if (!username.equals(other.username)) return false;
      return true;
   }
   
}

