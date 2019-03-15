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
package com.iris.platform.pairing.customization;

import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class PairingCustomizationConfig {
	@Inject(optional = true) @Named("pairing.customization.rules.requesttimeoutms")
	private long ruleTemplateRequestTimeoutMs = TimeUnit.SECONDS.toMillis(10); // leave plenty of time for the whole request to succeed
	@Inject(optional = true) @Named("pairing.customization.rules.maxrecommendations")
	private int maxRuleRecommendations = 4;

	public long getRuleTemplateRequestTimeoutMs() {
		return ruleTemplateRequestTimeoutMs;
	}

	public void setRuleTemplateRequestTimeoutMs(long ruleTemplateRequestTimeoutMs) {
		this.ruleTemplateRequestTimeoutMs = ruleTemplateRequestTimeoutMs;
	}

	public int getMaxRuleRecommendations() {
		return maxRuleRecommendations;
	}

	public void setMaxRuleRecommendations(int maxRuleRecommendations) {
		this.maxRuleRecommendations = maxRuleRecommendations;
	}

}

