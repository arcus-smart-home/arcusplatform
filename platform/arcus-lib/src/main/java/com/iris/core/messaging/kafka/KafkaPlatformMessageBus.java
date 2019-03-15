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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.io.json.JSON;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.AddressMatcher;
import com.iris.platform.partition.Partitioner;
import com.iris.platform.partition.PlatformPartition;

/**
 *
 */
@Singleton
public class KafkaPlatformMessageBus extends KafkaMessageBus<PlatformMessage, KafkaConfig> implements PlatformMessageBus {

	@Inject
	public KafkaPlatformMessageBus(
			KafkaMessageSender sender,
			KafkaDispatcher dispatcher,
			KafkaConfig config,
			Partitioner partitioner
	) {
	   super(
	         "platform",
	         sender,
	         dispatcher,
	         config,
	         partitioner,
	         // TODO switch to binary serializer
	         JSON.createSerializer(PlatformMessage.class),
	         JSON.createDeserializer(PlatformMessage.class)
   	);
   }
	
	

   @Override
	public ListenableFuture<Void> send(PlatformPartition partition, PlatformMessage message) {
   	//TODO - removeMe in the future.  This warning is used to catch places missing population header
		if(message.getPopulation() == null) {
			logger.warn("PlatformMessage with type [{}] and source [{}] does not have population header", message.getMessageType(), message.getSource().getRepresentation());
		}
		return super.send(partition, message);
	}



	@Override
   protected String getTopic(Address address) {
      return getTopic(address.getNamespace(), address.getGroup(), address.getId(), address.isHubAddress());
   }

   @Override
   protected boolean isLogged() {
      return true;
   }

   @Override
   protected String getTopic(AddressMatcher matcher) throws IllegalArgumentException {
      return getTopic(matcher.getNamespace(), matcher.getGroup(), matcher.getId(), matcher.isHubAddress());
   }

   private String getTopic(String namespace, Object group, Object id, boolean hubAddress) {
      switch(namespace) {
      case MessageConstants.BROADCAST:
      case MessageConstants.BRIDGE:
      case MessageConstants.CLIENT:
      case MessageConstants.DRIVER:
      case MessageConstants.HUB:
         return getConfig().getTopicPlatform();

      case MessageConstants.SERVICE:
         if(hubAddress) {
            return getConfig().getTopicPlatform();
         }
         else {
            return getConfig().getTopicService((String) group);
         }

      default:
         throw new IllegalArgumentException("Unrecognized platform address namespace [" + namespace + "]");
      }
   }

}

