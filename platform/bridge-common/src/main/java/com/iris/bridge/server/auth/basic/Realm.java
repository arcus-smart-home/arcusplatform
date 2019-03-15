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
package com.iris.bridge.server.auth.basic;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

public class Realm {
   private String realmName;
   private List<RealmUser>users;
   private Map<String,RealmUser>index;
   
   public List<RealmUser> getUsers() {
      return users;
   }

   public void setUsers(List<RealmUser> users) {
      this.users = users;
   }

   public String getRealmName() {
      return realmName;
   }

   public void setRealmName(String realmName) {
      this.realmName = realmName;
   }
   
   public void init(){
      index=Maps.uniqueIndex(users, RealmUser::getUsername);
   }
   
   public boolean authenticates(String username,String password){
      if(index.containsKey(username) && index.get(username).getPassword().equals(password)){
         return true;
      }
      return false;
   }
   public RealmUser load(String username){
      RealmUser user = index.get(username);
      if(user!=null){
         return user;
      }
      else{
         throw new RuntimeException("could not locate RealmUser with name "+username);
      }
   }
   
   
   
   public static class RealmUser {
      
      private String username;
      private String password;
      
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
   }
}

