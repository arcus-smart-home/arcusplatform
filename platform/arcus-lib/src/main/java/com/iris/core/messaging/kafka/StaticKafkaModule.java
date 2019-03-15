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

import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.core.messaging.MessagesModule;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.core.protocol.ProtocolMessageBus;
import com.iris.platform.partition.PartitionListener;
import com.iris.platform.partition.simple.SimplePartitionModule;
import com.netflix.governator.annotations.Modules;

/**
 * This creates a statically configured kafka which does not depend on dynamic
 * member id discovery.
 */
@Modules(include = { MessagesModule.class, SimplePartitionModule.class })
public class StaticKafkaModule extends AbstractIrisModule {

   public StaticKafkaModule() {
      
   }
   
	/* (non-Javadoc)
	 * @see com.google.inject.AbstractModule#configure()
	 */
	@Override
	protected void configure() {
		bind(KafkaDispatcher.class).to(KafkaDispatcherImpl.class);
		bindSetOf(PartitionListener.class).addBinding().to(KafkaDispatcherImpl.class);

		bind(PlatformMessageBus.class).to(KafkaPlatformMessageBus.class).asEagerSingleton();
		bind(ProtocolMessageBus.class).to(KafkaProtocolMessageBus.class).asEagerSingleton();
	}
	
}

