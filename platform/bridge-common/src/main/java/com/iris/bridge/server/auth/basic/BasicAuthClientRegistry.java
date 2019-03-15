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
package com.iris.bridge.server.auth.basic;


import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.bridge.server.auth.basic.Realm.RealmUser;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.resource.Resource;
import com.iris.resource.Resources;

@Singleton
public class BasicAuthClientRegistry implements ClientFactory {
   
   private static final Logger LOGGER = LoggerFactory.getLogger(BasicAuthClientRegistry.class);
   
   private String realmConfigFile;
   private boolean useBasicAuth =false;
   
   private Realm realm;

   @Inject
   public BasicAuthClientRegistry(@Named("auth.basic.realmconfig")String realmConfigFile,@Named("auth.basic")boolean useBasicAuth) {
      this.realmConfigFile=realmConfigFile;
      this.useBasicAuth=useBasicAuth;
      registerRealmFile();
   }
   
   public Client create() {
      return new BasicAuthClient();
   }
   
   public Client load(String username) {
      if(useBasicAuth==true){
         RealmUser user = realm.load(username);
         return new BasicAuthClient(user.getUsername(),user.getPassword());
      }
      return new BasicAuthClient();
   }
   
   private void registerRealmFile(){
      try{
         Resource resource = Resources.getResource(realmConfigFile);
         realm=createRealm(resource.getFile());
         if(resource.isWatchable()){
            resource.addWatch(()->{
               LOGGER.info("Realm file reloading");
               realm=createRealm(resource.getFile());
            });
         }
      }
      catch(Exception e){
         LOGGER.warn("Error registering Realm File");
         throw new RuntimeException(e);
      }
      
   }
   
   private synchronized Realm createRealm(File realmFile){
      try{
         ObjectMapper om = new ObjectMapper();
         Realm realm = om.readValue(realmFile, Realm.class);
         realm.init();
         return realm;
      }
      catch(Exception e){
         LOGGER.warn("Error Configurin Realm");
         throw new RuntimeException(e);
      }
   }
}

