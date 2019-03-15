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
package com.iris.driver.service.registry;

import java.util.LinkedList;
import java.util.List;

public class DriversLoadedEvent {
   private final List<DriverScriptInfo> drivers = new LinkedList<>();
   private final int exceptionCount;
   
   DriversLoadedEvent(List<DriverScriptInfo> drivers) {
      int exceptions = 0;
      for (DriverScriptInfo driver : drivers) {
         if (driver.getLoadingEx() != null) {
            exceptions++;
         }
         this.drivers.add(driver);
      }
      exceptionCount = exceptions;
   }
   
   public List<DriverScriptInfo> getLoadedDriverScripts() {
      List<DriverScriptInfo> list = new LinkedList<>();
      list.addAll(drivers);
      return list;
   }
   
   public boolean hasErrors() {
      return exceptionCount > 0;
   }
}

