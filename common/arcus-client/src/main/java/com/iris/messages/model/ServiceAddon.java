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
package com.iris.messages.model;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public enum ServiceAddon {
	CELLBACKUP;
	
	
	private final static Table<ServiceAddon, ServiceLevel, String> addonCodeMap = HashBasedTable.create();
	static {
		//Make sure these codes match with what is configured in recurly
		addonCodeMap.put(CELLBACKUP, ServiceLevel.PREMIUM, "CELLBACKUP") ;
		addonCodeMap.put(CELLBACKUP, ServiceLevel.BASIC, "CELLBACKUP") ;
		addonCodeMap.put(CELLBACKUP, ServiceLevel.PREMIUM_FREE, "CELLBACKUP") ;
      addonCodeMap.put(CELLBACKUP, ServiceLevel.PREMIUM_PROMON, "CELLBACKUP") ;
      addonCodeMap.put(CELLBACKUP, ServiceLevel.PREMIUM_PROMON_FREE, "CELLBACKUP") ;
      addonCodeMap.put(CELLBACKUP, ServiceLevel.PREMIUM_PROMON_ANNUAL, "CELLBACKUP") ;
      addonCodeMap.put(CELLBACKUP, ServiceLevel.PREMIUM_ANNUAL, "CELLBACKUP") ;
	}
	
   
   public static ServiceAddon fromString(String addon) {
      if ("cellbackup".equalsIgnoreCase(addon)) {
         return CELLBACKUP;
      }
     
      throw new IllegalArgumentException("The string " + addon + " is an invalid service addon.");
   }
   
   
   public static String getAddonCode(ServiceAddon addon, ServiceLevel serviceLevel) {
	   return addonCodeMap.get(addon, serviceLevel);
   }

   
}

