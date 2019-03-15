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
package com.iris.core.messaging.kafka;

import com.google.inject.Inject;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.core.messaging.MessagesModule;
import com.iris.core.platform.AnalyticsMessageBus;
import com.iris.core.platform.IntraServiceMessageBus;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.core.protocol.ProtocolMessageBus;
import com.iris.platform.partition.PartitionListener;
import com.iris.platform.partition.cluster.ClusteredPartitionModule;
import com.netflix.governator.annotations.Modules;

/**
 *
 */
// if you're using kafka now, you also need the partitioner, so lets control it all from here
@Modules(include = { ClusteredPartitionModule.class })
public class KafkaModule extends AbstractIrisModule {

   @Inject
   public KafkaModule(MessagesModule messages) {
      
   }

	/* (non-Javadoc)
	 * @see com.google.inject.AbstractModule#configure()
	 */
	@Override
	protected void configure() {
		bind(KafkaConfig.class);
		bind(KafkaMessageSender.class);

		bind(KafkaDispatcher.class).to(KafkaDispatcherImpl.class);
		bindSetOf(PartitionListener.class).addBinding().to(KafkaDispatcherImpl.class);

		bind(PlatformMessageBus.class).to(KafkaPlatformMessageBus.class).asEagerSingleton();;
		bind(ProtocolMessageBus.class).to(KafkaProtocolMessageBus.class).asEagerSingleton();;
		bind(AnalyticsMessageBus.class).to(KafkaAnalyticsMessageBus.class).asEagerSingleton();
		bind(IntraServiceMessageBus.class).to(KafkaIntraServiceMessageBus.class).asEagerSingleton();;
	}
	
}

