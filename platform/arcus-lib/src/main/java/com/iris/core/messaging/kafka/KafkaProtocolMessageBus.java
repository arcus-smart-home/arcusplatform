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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.Utils;
import com.iris.core.protocol.ProtocolMessageBus;
import com.iris.io.json.JSON;
import com.iris.messages.MessageConstants;
import com.iris.messages.address.Address;
import com.iris.messages.address.AddressMatcher;
import com.iris.platform.partition.Partitioner;
import com.iris.platform.partition.PlatformPartition;
import com.iris.protocol.ProtocolMessage;

/**
 *
 */
@Singleton
public class KafkaProtocolMessageBus extends KafkaMessageBus<ProtocolMessage, KafkaConfig> implements ProtocolMessageBus {
	static final Logger logger = LoggerFactory.getLogger(KafkaProtocolMessageBus.class);
	
	@Inject
	public KafkaProtocolMessageBus(
			KafkaMessageSender sender,
			KafkaDispatcher dispatcher,
			KafkaConfig config,
			Partitioner partitioner
	) {
	   super(
	         "protocol",
	         sender,
	         dispatcher,
	         config,
	         partitioner,
	         JSON.createSerializer(ProtocolMessage.class),
	         JSON.createDeserializer(ProtocolMessage.class)
	   );
   }	

   @Override
	public ListenableFuture<Void> send(PlatformPartition partition, ProtocolMessage message) {
   	//TODO - removeMe in the future.  This warning is used to catch places missing population header
		if(message.getPopulation() == null) {
			logger.warn("ProtocolMessage with type [{}] and source [{}] does not have population header", message.getMessageType(), message.getSource().getRepresentation());
		}
		return super.send(partition, message);
	}



	@Override
   protected String getTopic(Address address) {
      Utils.assertNotNull(address);
      return getTopic(address.getNamespace(), address.getGroup(), address.getId(), address.isHubAddress());
   }

   @Override
   protected String getTopic(AddressMatcher matcher) throws IllegalArgumentException {
      Utils.assertNotNull(matcher);
      return getTopic(matcher.getNamespace(), matcher.getGroup(), matcher.getId(), matcher.isHubAddress());
   }

   private String getTopic(String namespace, Object group, Object id, boolean hubAddress) {
      if(hubAddress) {
         return getHubTopic(namespace);
      }
      else if(MessageConstants.PROTOCOL.equals(namespace)) {
         return getProtocolTopic((String) group);
      }
      else {
         return getPlatformTopic(namespace);
      }
   }
         
   private String getProtocolTopic(String protocolName) {
      String topic = getConfig().getTopicProtocol(protocolName);
      if(StringUtils.isEmpty(topic)) {
         throw new IllegalArgumentException("Unrecognized protocol name [" + protocolName + "]");
      }
      return topic;
   }
   
   private String getPlatformTopic(String namespace) {
      switch(namespace) {
      case MessageConstants.BROADCAST: // TODO broadcast will be deprecated in the future
      case MessageConstants.DRIVER:
         return getConfig().getTopicToDriver();

      default:
         throw new IllegalArgumentException("Unrecognized protocol address namespace [" + namespace + "]");
      }
   }

   private String getHubTopic(String namespace) {
      switch(namespace) {
      case MessageConstants.BROADCAST: // TODO broadcast will be deprecated in the future
      case MessageConstants.DRIVER:
      case MessageConstants.PROTOCOL:
         return getConfig().getTopicToHub();

      default:
         throw new IllegalArgumentException("Unrecognized protocol address namespace [" + namespace + "]");
      }
   }

   @Override
   protected boolean isLogged() {
      return false;
   }
}

