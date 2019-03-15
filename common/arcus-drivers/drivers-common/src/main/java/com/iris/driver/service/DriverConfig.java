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
package com.iris.driver.service;

import java.lang.reflect.Field;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
// TODO why is this in common and not platform, it is config options for the platform services and
//      would not generally be applicable to the hub
public class DriverConfig {
   private static final Logger LOGGER = LoggerFactory.getLogger(DriverConfig.class);

   public static final String NAMED_EXECUTOR = "DriverExecutor";

   @Inject(optional=true)
   @Named("application.dir")
   private String applicationDirectory = "";

   @Inject(optional=true)
   @Named("driver.directory")
   private String driverDirectory = "drivers";

   @Inject(optional=true)
   @Named("driver.pool.size")
   private int driverThreadPoolSize = 100;

   @Inject(optional=true)
   @Named("scheduler.pool.size")
   private int schedulerThreadPoolSize = 1;

   @Inject(optional=true)
   @Named("driver.backlog.size")
   private int driverBacklogSize = 100;

   @Inject(optional=true)
   @Named("driver.filterPattern")
   private String driverFilterPattern = null;

   @Inject(optional=true)
   @Named("driver.filter.pattern")
   private String driverFilterPattern2 = null;

   @Inject(optional = true)
   @Named("driver.tombstoneTimeoutSec")
   private long driverTombstoneTimeoutSec = TimeUnit.MINUTES.toSeconds(5);
   
   public String getApplicationDirectory() {
      return applicationDirectory;
   }

   public void setApplicationDirectory(String applicationDirectory) {
      this.applicationDirectory = applicationDirectory;
   }

   public String getDriverDirectory() {
      return driverDirectory;
   }

   public DriverConfig setDriverDirectory(String driverDirectory) {
      this.driverDirectory = driverDirectory;
      return this;
   }

   public int getDriverThreadPoolSize() {
      return driverThreadPoolSize;
   }

   public void setDriverThreadPoolSize(int driverThreadPoolSize) {
      this.driverThreadPoolSize = driverThreadPoolSize;
   }

   public int getDriverBacklogSize() {
      return driverBacklogSize;
   }

   public void setDriverBacklogSize(int driverBacklogSize) {
      this.driverBacklogSize = driverBacklogSize;
   }

   /**
    * @return the schedulerThreadPoolSize
    */
   public int getSchedulerThreadPoolSize() {
      return schedulerThreadPoolSize;
   }

   /**
    * @param schedulerThreadPoolSize the schedulerThreadPoolSize to set
    */
   public void setSchedulerThreadPoolSize(int schedulerThreadPoolSize) {
      this.schedulerThreadPoolSize = schedulerThreadPoolSize;
   }

   public String evaluateAbsoluteDriverDirectory() {
      String driverDirectory = getDriverDirectory();
      if(driverDirectory.isEmpty()) {
         return getApplicationDirectory();
      }
      if(driverDirectory.charAt(0) == '/') {
         return driverDirectory;
      }
      String applicationDirectory = getApplicationDirectory();
      if(StringUtils.isEmpty(applicationDirectory)) {
         return driverDirectory;
      }
      return applicationDirectory + "/" + driverDirectory;
   }

   public String getDriverFilterPattern() {
	   return (driverFilterPattern) != null ? driverFilterPattern : driverFilterPattern2;
   }

   public void setDriverFilterPattern(String driverFilterPattern) {
	   this.driverFilterPattern = driverFilterPattern;
   }

   /**
    * @return the driverTombstoneTimeoutSec
    */
   public long getDriverTombstoneTimeoutSec() {
      return driverTombstoneTimeoutSec;
   }

   public long getDriverTombstoneTimeout(TimeUnit unit) {
      return driverTombstoneTimeoutSec <= 0 ? 0 : unit.convert(driverTombstoneTimeoutSec, TimeUnit.SECONDS);
   }

   /**
    * @param driverTombstoneTimeoutSec the driverTombstoneTimeoutSec to set
    */
   public void setDriverTombstoneTimeoutSec(long driverTombstoneTimeoutSec) {
      this.driverTombstoneTimeoutSec = driverTombstoneTimeoutSec;
   }

   // TODO move this to a utility
   public Properties toProperties() {
      Properties props = new Properties();
      Field [] fields = getClass().getFields();
      Field.setAccessible(fields, true);
      for(Field f: getClass().getFields()) {
         Named named = f.getAnnotation(Named.class);
         if(named == null) {
            continue;
         }
         String name = named.value();
         try {
            props.setProperty(name, String.valueOf(f.get(this)));
         }
         catch (IllegalArgumentException | IllegalAccessException e) {
            LOGGER.debug("Unable to retrieve property [{}]", name);
            e.printStackTrace();
         }
      }
      props.setProperty("driver.directory", getDriverDirectory());
      return props;
   }
}

