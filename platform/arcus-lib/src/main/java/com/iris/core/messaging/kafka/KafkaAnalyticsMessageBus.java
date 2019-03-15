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
package com.iris.core.messaging.kafka;

import com.google.inject.Inject;
import com.iris.core.platform.AnalyticsMessageBus;
import com.iris.io.json.JSON;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.AddressMatcher;
import com.iris.platform.partition.Partitioner;

public class KafkaAnalyticsMessageBus extends KafkaMessageBus<PlatformMessage, KafkaAnalyticsConfig> implements AnalyticsMessageBus {
	
	@Inject
	public KafkaAnalyticsMessageBus(
			KafkaMessageSender sender,
			KafkaDispatcher dispatcher,
			KafkaAnalyticsConfig config,
			Partitioner partitioner) 
	{
		super(
				"analytics",
				sender,
				dispatcher,
				config,
				partitioner,
				JSON.createSerializer(PlatformMessage.class),
				JSON.createDeserializer(PlatformMessage.class)
				);
	}

	@Override
	protected KafkaAnalyticsConfig getConfig() {
		return (KafkaAnalyticsConfig) super.getConfig();
	}

	@Override
	protected String getTopic(Address address) {
		return getConfig().getTopicAnalyticsTags();
	}

	@Override
	protected String getTopic(AddressMatcher matcher) throws IllegalArgumentException {
		return getConfig().getTopicAnalyticsTags();
	}	

	@Override
	protected boolean isLogged() {
		return true;
	}

}

