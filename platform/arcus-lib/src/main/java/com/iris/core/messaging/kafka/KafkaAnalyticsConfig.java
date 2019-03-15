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
import com.google.inject.name.Named;
import com.netflix.governator.configuration.ConfigurationProvider;

public class KafkaAnalyticsConfig extends AbstractKafkaConfig {
	@Inject(optional = true) @Named("kafka.topic.analyticstags")
   private String topicAnalyticsTags = "tags";
   @Inject(optional = true) @Named("analytics.kafka.message.timeout")
   private long messageTimeoutMs = 0;

	@Inject
	public KafkaAnalyticsConfig(ConfigurationProvider configProvider) {
		// currently uses the same kafka bus as platform, but just change this constant to move it
		super(configProvider, "");
	}
	
	public String getTopicAnalyticsTags() {
		return topicAnalyticsTags;
	}

	public void setTopicAnalyticsTags(String topicAnalyticsTags) {
		this.topicAnalyticsTags = topicAnalyticsTags;
	}

	public long getMessageTimeoutMs() {
		return messageTimeoutMs;
	}

	public void setMessageTimeoutMs(long messageTimeoutMs) {
		this.messageTimeoutMs = messageTimeoutMs;
	}

	@Override
	protected String getDefaultBootstrapServers() {
		return "kafka.eyeris:9092";
	}

	@Override
	protected String getDefaultSerializer() {
		return null;
	}

	@Override
	protected String getDefaultKeySerializer() {
		return null;
	}

	@Override
	protected String getDefaultPartitioner() {
		return null;
	}

}

