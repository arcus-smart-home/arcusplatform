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
package com.iris.platform.services.intraservice.handlers;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.core.platform.AbstractPlatformMessageListener;
import com.iris.core.platform.IntraServiceMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.platform.hubbridge.HeartbeatMessage;
import com.iris.platform.partition.Partitioner;
import com.iris.platform.services.hub.HubRegistry;

@Singleton
public class HubHeartbeatListener extends AbstractPlatformMessageListener {
   private static final Logger logger = LoggerFactory.getLogger(HubHeartbeatListener.class);
   
   private final HubRegistry hubs;
   private final Partitioner partitioner;
   
   @Inject
   public HubHeartbeatListener(
         IntraServiceMessageBus bus,
         HubRegistry hubs,
         Partitioner partitioner
   ) {
      super(bus);

      this.hubs = hubs;
      this.partitioner = partitioner;
   }

   /* (non-Javadoc)
    * @see com.iris.core.platform.AbstractPlatformMessageListener#onStart()
    */
   @Override
   protected void onStart() {
      super.onStart();
      addListeners(Address.broadcastAddress());
   }
   
   /* (non-Javadoc)
    * @see com.iris.core.platform.AbstractPlatformMessageListener#handleMessage(com.iris.messages.PlatformMessage)
    */
   @Override
   public void handleMessage(PlatformMessage message) {
      super.handleMessage(message);
   }

   /* (non-Javadoc)
    * @see com.iris.core.platform.AbstractPlatformMessageListener#handleEvent(com.iris.messages.PlatformMessage)
    */
   @Override
   protected void handleEvent(PlatformMessage message) throws Exception {
      int partitionId = partitioner.getPartitionForMessage(message).getId();
      if(HeartbeatMessage.NAME.equals(message.getMessageType())) {
         onHeartbeat(partitionId, message.getSource(), message.getValue());
      }
   }

   public void onHeartbeat(int partitionId, Address source, MessageBody payload) {
      Collection<String> hubIds = HeartbeatMessage.getConnectedHubIds(payload.getAttributes());
      String bridgeId = (String) source.getId(); 
      logger.debug("Received hub bridge heartbeat on intraservice topic for partition [{}] from [{}] with [{}] hubs", partitionId, source, hubIds.size());
      for(String hubId: hubIds) {
         hubs.online(hubId, partitionId, bridgeId);
      }
   }
   
}

