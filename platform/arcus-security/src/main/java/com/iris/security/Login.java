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
package com.iris.security;

import java.util.Date;
import java.util.UUID;

// TODO:  here because it shouldn't be shared with clients and security probably shouldn't
// depend on lib, but should be ok to depend on common
public class Login {

   private UUID userId;
   private String username;
   private String password;
   private String passwordSalt;
   private Date lastPasswordChange;

   public UUID getUserId() {
      return userId;
   }

   public void setUserId(UUID userId) {
      this.userId = userId;
   }

   public String getUsername() {
      return username;
   }

   public void setUsername(String username) {
      this.username = username;
   }

   public String getPassword() {
      return password;
   }

   public void setPassword(String password) {
      this.password = password;
   }

   public String getPasswordSalt() {
      return passwordSalt;
   }

   public void setPasswordSalt(String passwordSalt) {
      this.passwordSalt = passwordSalt;
   }

   public Date getLastPasswordChange() {
      return lastPasswordChange;
   }

   public void setLastPasswordChange(Date lastPasswordChange) {
      this.lastPasswordChange = lastPasswordChange;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((password == null) ? 0 : password.hashCode());
      result = prime * result
            + ((passwordSalt == null) ? 0 : passwordSalt.hashCode());
      result = prime * result + ((userId == null) ? 0 : userId.hashCode());
      result = prime * result + ((username == null) ? 0 : username.hashCode());
      result = prime * result + ((lastPasswordChange == null) ? 0 : lastPasswordChange.hashCode());
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
      Login other = (Login) obj;
      if (password == null) {
         if (other.password != null)
            return false;
      } else if (!password.equals(other.password))
         return false;
      if (passwordSalt == null) {
         if (other.passwordSalt != null)
            return false;
      } else if (!passwordSalt.equals(other.passwordSalt))
         return false;
      if (userId == null) {
         if (other.userId != null)
            return false;
      } else if (!userId.equals(other.userId))
         return false;
      if (username == null) {
         if (other.username != null)
            return false;
      } else if (!username.equals(other.username))
         return false;
      if(lastPasswordChange == null) {
         if(other.lastPasswordChange != null)
            return false;
      } else if(!lastPasswordChange.equals(other.lastPasswordChange))
         return false;
      return true;
   }

   @Override
   public String toString() {
      return "Login [userId=" + userId + ", username=" + username
            + ", password=" + password + ", passwordSalt=" + passwordSalt + ", lastPasswordChange=" + lastPasswordChange + "]";
   }
}

