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
package com.iris.platform.pairing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.iris.messages.type.PairingStep;
import com.iris.prodcat.ProductCatalogEntry;
import com.iris.prodcat.Step;

public final class PairingRemovalUtils {
	private static final String DEFAULT_REMOVE_STEP_TITLE = "Removing %s";
	private static final String DEFAULT_REMOVE_DEVICE_NAME = "Device";
	private static final String DEFAULT_REMOVE_STEP_DESC_FOR_HUB_DEVICE = "The Hub will beep when the device has been removed.";
	private static final String DEFAULT_REMOVE_STEP_DESC_FOR_NONHUB_DEVICE = "This should only take a moment.";

	private PairingRemovalUtils() {
		
	}

	public static List<Map<String, Object>> loadRemovalSteps(ProductCatalogEntry entry) {
		List<Step> removal = entry.getRemoval();
		if(removal == null || removal.isEmpty()) {
			return createDefaultRemovalStep(entry.getName(), !Boolean.FALSE.equals(entry.getHubRequired()));
		}
		
		List<Map<String, Object>> removalSteps = new ArrayList<>(removal.size());
		String title = null;
		for(Step step: removal) {	
			if(step.getOrder() == 0) {
				//step0 is handled specially.  The text will become the title of step1
				title = step.getText();
			}else{
				PairingStep s = new PairingStep();
				s.setId("pair/remove" + step.getOrder());
				s.setOrder(step.getOrder());
				s.setInfo(step.getSubText());
				s.setInstructions(ImmutableList.of(step.getText()));
				if(title != null) {
					s.setTitle(title);
					title = null;
				}
				removalSteps.add(s.toMap());
			}
		}
		return removalSteps;
	}	

	public static List<Map<String, Object>> createDefaultRemovalStep() {
		return createDefaultRemovalStep(DEFAULT_REMOVE_DEVICE_NAME, false);
	}
	
	public static List<Map<String, Object>> createDefaultRemovalStep(String productName, boolean hubDevice) {
		int i = 0;
		PairingStep s = new PairingStep();
		s.setId("pair/remove" + i);
		s.setOrder(i);
		s.setTitle(String.format(DEFAULT_REMOVE_STEP_TITLE, productName));
		s.setInfo(hubDevice ? DEFAULT_REMOVE_STEP_DESC_FOR_HUB_DEVICE : DEFAULT_REMOVE_STEP_DESC_FOR_NONHUB_DEVICE);
		return ImmutableList.<Map<String,Object>>of(s.toMap());
	}	
	
}

