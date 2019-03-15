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
package com.iris.platform.subsystem.placemonitor.config;

import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.resource.Resource;
import com.iris.resource.Resources;

public class WatchedJAXBConfigFileReference<M extends WatchedJAXBConfig> {
   private static final Logger LOGGER = LoggerFactory.getLogger(WatchedJAXBConfigFileReference.class);
   final Class<M> configTypeClass;
   private final String configFile;
   private AtomicReference<M>configRef;
   
   public WatchedJAXBConfigFileReference(String configFile, Class<M> clazz){
      this.configFile=configFile;
      this.configRef=new AtomicReference<M>(null);
      configTypeClass=clazz;
      init();
   }
   
   public AtomicReference<M> getReference(){
      return configRef;
   }
   
   public void init() {
      Resource resource = Resources.getResource(configFile);
      if (resource.isWatchable()){
         resource.addWatch(() -> {
            reloadConfig();
         });
      }
      reloadConfig();
   }

   protected void reloadConfig() {
      LOGGER.debug("reloading config [{}]", configFile);
      Resource resource = loadResource(configFile); 
      try{
         M config = XMLUtil.deserializeJAXB(resource,configTypeClass);
         config.afterLoad();
         configRef.set(config);
      }
      catch(Exception e){
         LOGGER.error("error parsing config file. using old or no config. "+configFile,e);
      }
   }
   protected Resource loadResource(String resourceName){
      Resource resource =Resources.getResource(configFile);
      if(!resource.exists()){
         throw new RuntimeException("JAXBconfig could not locate resource "+resourceName);
      }
      return resource;
   }
}

