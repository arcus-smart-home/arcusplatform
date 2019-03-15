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
package com.iris.core.messaging.memory;

import java.util.concurrent.Executors;

import javax.inject.Singleton;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.iris.messages.PlatformMessage;
import com.iris.core.platform.IntraServiceMessageBus;
import com.iris.io.json.JSON;

/**
 * An in-memory implementation of the {@link IntraServiceMessageBus}, generally
 * used for test cases.
 */
@Singleton
public class InMemoryIntraServiceMessageBus extends InMemoryMessageBus<PlatformMessage> implements IntraServiceMessageBus {
   
	public InMemoryIntraServiceMessageBus() {
	   super(
            "intraservice-bus",
            JSON.createSerializer(PlatformMessage.class),
            JSON.createDeserializer(PlatformMessage.class),
      		Executors.newSingleThreadExecutor(
      				new ThreadFactoryBuilder()
      					.setNameFormat("intraservice-bus-dispatcher")
      					.build()
      		)
      );
	}

}

