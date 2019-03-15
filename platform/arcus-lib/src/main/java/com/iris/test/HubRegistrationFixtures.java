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
package com.iris.test;

import java.security.SecureRandom;
import java.util.Date;

import com.iris.messages.model.HubRegistration;
import com.iris.messages.model.HubRegistration.RegistrationState;

public class HubRegistrationFixtures {

	public static HubRegistration createHubRegistration() {
		HubRegistration hubReg = new HubRegistration();
		SecureRandom rand = new SecureRandom();
		hubReg.setId("PIE-" + rand.nextInt(9) + rand.nextInt(9) + rand.nextInt(9)+ rand.nextInt(9));
		hubReg.setState(RegistrationState.DOWNLOADING);
		hubReg.setFirmwareVersion("1.2");
		hubReg.setLastConnected(new Date());
		hubReg.setTargetVersion("2.3");
		hubReg.setUpgradeRequestTime(new Date());
		//hubReg.setUpgradeErrorCode(randomHubId);
		//hubReg.setUpgradeErrorMessage(randomHubId + " - test");
		hubReg.setDownloadProgress(50);
		return hubReg;
	}

}

