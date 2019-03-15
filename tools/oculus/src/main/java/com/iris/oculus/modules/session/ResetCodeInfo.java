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
public class ResetCodeInfo {
   
   public static ResetCodeInfo back(String username) {
      return new ResetCodeInfo(username);
   }
   
   public static ResetCodeInfo submit(String username, String code, String password) {
      return new ResetCodeInfo(username, code, password);
   }
   
   private boolean back;
   private String username;
   private String code;
   private String password;

   private ResetCodeInfo(String username) {
      this.back = true;
      this.username = username;
   }
   
   private ResetCodeInfo(String username, String code, String password) {
      this.back = false;
      this.username = username;
      this.code = code;
      this.password = password;
   }

   /**
    * @return the back
    */
   public boolean isBack() {
      return back;
   }

   /**
    * @param back the back to set
    */
   public void setBack(boolean back) {
      this.back = back;
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
    * @return the code
    */
   public String getCode() {
      return code;
   }

   /**
    * @param code the code to set
    */
   public void setCode(String code) {
      this.code = code;
   }

   /**
    * @return the password
    */
   public String getPassword() {
      return password;
   }

   /**
    * @param password the password to set
    */
   public void setPassword(String password) {
      this.password = password;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "ResetCodeInfo [username=" + username + ", code=" + code
            + ", password=****]";
   }

   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((code == null) ? 0 : code.hashCode());
      result = prime * result + ((password == null) ? 0 : password.hashCode());
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
      ResetCodeInfo other = (ResetCodeInfo) obj;
      if (code == null) {
         if (other.code != null) return false;
      }
      else if (!code.equals(other.code)) return false;
      if (password == null) {
         if (other.password != null) return false;
      }
      else if (!password.equals(other.password)) return false;
      if (username == null) {
         if (other.username != null) return false;
      }
      else if (!username.equals(other.username)) return false;
      return true;
   }

   
}

