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
package com.iris.protocol;

import java.util.UUID;

import com.iris.capability.definition.ProtocolDefinition;
import com.iris.io.Deserializer;
import com.iris.io.Serializer;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;

/**
 * Defines a protocol recognized in the system. Currently
 * defines a serializer/deserializer factory, may be
 * more in the future.
 */
public interface Protocol<T> {

	/**
	 * A unique name for the protocol.
	 * @return
	 */
	String getName();
	
	/**
	 * A unique four character namespace for the protocol.
	 * @return protocol namespace
	 */
	String getNamespace();
	
	/**
	 * The Protocol Definition
	 * @return protocol definition
	 */
	ProtocolDefinition getDefinition();

	Serializer<T> createSerializer();

	Deserializer<T> createDeserializer();
	
	boolean isTransientAddress();
	
	PlatformMessage remove(RemoveProtocolRequest device);

}

