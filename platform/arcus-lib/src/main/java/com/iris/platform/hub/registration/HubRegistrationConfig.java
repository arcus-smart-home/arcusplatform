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
package com.iris.platform.hub.registration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class HubRegistrationConfig {
	public static final String PROP_UPGRADE_TIMEOUT_MIN = "hub.registration.upgradeTimeoutMin";
	public static final String PROP_OFFLINE_TIMEOUT_MIN = "hub.registration.offlineTimeoutMin";
	public static final String PROP_TIMEOUT_INTERVAL_SEC = "hub.registration.timeoutIntervalSec";
	public static final String PROP_UPGRADE_ERROR_MAX_AGO_MIN = "hub.registration.error.maxage.minutes";  
   
	@Inject(optional = true) @Named(PROP_UPGRADE_TIMEOUT_MIN)
	private int upgradeTimeoutMin = 20;
	@Inject(optional = true) @Named(PROP_OFFLINE_TIMEOUT_MIN)
	private int offlineTimeoutMin = 10;
	@Inject(optional = true) @Named(PROP_TIMEOUT_INTERVAL_SEC)
	private int timeoutIntervalSec = 30;
	@Inject(optional = true) @Named(PROP_UPGRADE_ERROR_MAX_AGO_MIN)
   private int upgradeErrorMaxAgeInMin = 5;
	
   public HubRegistrationConfig() {
	}

   /**
	* @return the offlineTimeoutMin
	*/
	public int getOfflineTimeoutMin() {
		return offlineTimeoutMin;
	}

   /**
    * @param offlineTimeoutMin the offlineTimeoutMin to set
    */
	public void setOfflineTimeoutMin(int offlineTimeoutMin) {
		this.offlineTimeoutMin = offlineTimeoutMin;
	}

   /**
    * @return the timeoutIntervalSec
    */
	public int getTimeoutIntervalSec() {
		return timeoutIntervalSec;
	}

   /**
    * @param timeoutIntervalSec the timeoutIntervalSec to set
    */
	public void setTimeoutIntervalSec(int timeoutIntervalSec) {
		this.timeoutIntervalSec = timeoutIntervalSec;
	}
	
	public int getUpgradeTimeoutMin() {
		return upgradeTimeoutMin;
	}

	public void setUpgradeTimeoutMin(int upgradeTimeoutMin) {
		this.upgradeTimeoutMin = upgradeTimeoutMin;
	}
	
	public int getUpgradeErrorMaxAgeInMin()
   {
      return upgradeErrorMaxAgeInMin;
   }

   public void setUpgradeErrorMaxAgeInMin(int upgradeErrorMaxAgeInMin)
   {
      this.upgradeErrorMaxAgeInMin = upgradeErrorMaxAgeInMin;
   }
}

