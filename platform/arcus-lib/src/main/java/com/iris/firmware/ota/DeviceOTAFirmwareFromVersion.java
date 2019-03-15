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
package com.iris.firmware.ota;

import java.util.regex.Pattern;

public class DeviceOTAFirmwareFromVersion {

      public static final String REGEX_TYPE = "regex";
      public static final String EXACT_TYPE = "exact";
      
      private String type;
      private String value;
      
      private Pattern regex;
      
      public DeviceOTAFirmwareFromVersion(String type, String value) {
         this.type = type;
         this.value = value;

         switch (type) {
            case REGEX_TYPE:
               regex = Pattern.compile(value);
               break;
            case EXACT_TYPE:
               // everything is ok.
               break;
            default:
               throw new IllegalArgumentException("Type must either " + REGEX_TYPE + " or " + EXACT_TYPE +".");               
         }
      }
      
      public boolean matches(String version) {
         if (version == null) {            
            return false;
         }
         switch (type) {
            case REGEX_TYPE:
               return version.matches(value);
            case EXACT_TYPE:
               return version.equals(value);
            default:
               throw new IllegalArgumentException("Type must either " + REGEX_TYPE + " or " + EXACT_TYPE +".");                 
         }
      }
}

