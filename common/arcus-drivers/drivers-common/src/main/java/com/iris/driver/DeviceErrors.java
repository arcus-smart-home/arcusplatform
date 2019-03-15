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
package com.iris.driver;

import org.apache.commons.lang3.StringUtils;

import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;

/**
 * 
 */
public class DeviceErrors {
   public static final String ERR_CODE_DEV_EXISTS = "device.exists";
   public static final String ERR_CODE_DELETED    = "device.deleted";

   private static final ErrorEvent DELETED = ErrorEvent.fromCode(ERR_CODE_DELETED, "This device has been deleted");
   
   private DeviceErrors() { }

   // TODO add counters here
   
   public static ErrorEvent deviceExists(String protocolId, String hubId) {
      return ErrorEvent.fromCode(
            ERR_CODE_DEV_EXISTS, 
            String.format("Device with protocol id %s for hub %s already exists", protocolId, hubId)
      );
   }
   
   public static ErrorEvent deviceDeleted() {
      return DELETED;
   }
   
}

