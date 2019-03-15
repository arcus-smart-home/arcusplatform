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
package com.iris.driver.service.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.iris.driver.service.DeviceService;
import com.iris.driver.service.DeviceService.UpgradeDriverResponse;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.DriverId;
import com.iris.model.Version;

public class UpgradeDriverRequestHandler implements DriverServiceRequestHandler {

   private static final Logger logger = LoggerFactory.getLogger(UpgradeDriverRequestHandler.class);

   private final DeviceService service;

   @Inject
   public UpgradeDriverRequestHandler(DeviceService service) {
      this.service = service;
   }

   @Override
   public String getMessageType() {
      return DeviceAdvancedCapability.UpgradeDriverRequest.NAME;
   }

   @Override
   public MessageBody handleMessage(PlatformMessage message) throws Exception {
      return upgradeDriver(message.getDestination(), message.getValue());
   }

   private MessageBody upgradeDriver(Address address, MessageBody request) {
      String newDriverName = DeviceAdvancedCapability.UpgradeDriverRequest.getDriverName(request);
      String newDriverVersion = DeviceAdvancedCapability.UpgradeDriverRequest.getDriverVersion(request);

      if((newDriverName == null && newDriverVersion != null) || (newDriverName != null && newDriverVersion == null)) {
         return Errors.invalidRequest("Both driverName and driverVersion must be specified.");
      }

      try {
         UpgradeDriverResponse response;
         if(newDriverName == null) {
            response = service.upgradeDriver(address);
         }
         else {
            DriverId driverId = new DriverId(newDriverName, Version.fromRepresentation(newDriverVersion));
            response = service.upgradeDriver(address, driverId);
         }
         return DeviceAdvancedCapability.UpgradeDriverResponse.builder()
               .withUpgraded(response.isUpgraded())
               .withDriverName(response.getDriverId().getName())
               .withDriverVersion(response.getDriverId().getVersion().getRepresentation())
               .build()
               ;
      } 
      catch(Exception e) {
         logger.error("Failed to upgrade driver due to an exception managing the context", e);
         return Errors.fromException(e);
      }
   }
}

