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
package com.iris.driver.groovy.pin;

import groovy.lang.GroovyObjectSupport;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.pin.PinManager;
import com.iris.driver.service.executor.DriverExecutor;
import com.iris.driver.service.executor.DriverExecutors;

public class PinManagementContext extends GroovyObjectSupport {

   public byte[] getPin(String personId) {
      return getPinManager().getPin(getPlaceId(), UUID.fromString(personId));
   }

   public void setActor(String personId) {
      getPinManager().setActor(StringUtils.isBlank(personId) ? null : UUID.fromString(personId));
   }

   public String validatePin(String pin) {
      return getPinManager().validatePin(getPlaceId(), pin).toString();
   }

   public String validatePin(byte[] pin) {
      return getPinManager().validatePin(getPlaceId(), pin).toString();
   }

   public String accumulatePin(int code) {
      UUID person = getPinManager().accumulatePin(getPlaceId(), code);
      return person == null ? null : person.toString();
   }

   private PinManager getPinManager() {
      return getDriverContext().getPinManager();
   }

   private UUID getPlaceId() {
      return getDriverContext().getPlaceId();
   }

   private DeviceDriverContext getDriverContext() {
      DriverExecutor executor = DriverExecutors.get();
      return executor.context();
   }
}

