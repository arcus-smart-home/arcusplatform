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
package com.iris.hubcom.server.session.listener;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.iris.bridge.server.session.Session;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.hubcom.server.session.HubSession;
import com.iris.io.json.JSON;
import com.iris.messages.HubMessage;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.HubConnectionCapability;
import com.iris.messages.capability.HubNetworkCapability;
import com.iris.messages.model.Place;
import com.iris.messages.services.PlatformConstants;
import com.iris.platform.hub.registration.HubRegistrationRegistry;
import com.iris.platform.partition.Partitioner;
import com.iris.population.PlacePopulationCacheManager;

import io.netty.channel.Channel;

/**
 *
 */
public class HubConnectionSessionListener extends HubSessionAdapter {
   private final Partitioner partitioner; // just need this for the id, kind of annoying
   private final PlatformMessageBus platformBus;
   private final PlaceDAO placeDao;
   private final HubRegistrationRegistry hubRegistrationRegistry;
   private final PlacePopulationCacheManager populationCacheMgr;

   @Inject
   public HubConnectionSessionListener(
         Partitioner partitioner,
         PlatformMessageBus platformBus,
         PlaceDAO placeDao,
         HubRegistrationRegistry hubRegistrationRegistry,
         PlacePopulationCacheManager populationCacheMgr
   ) {
      this.partitioner = partitioner;
      this.platformBus = platformBus;
      this.placeDao = placeDao;
      this.hubRegistrationRegistry = hubRegistrationRegistry;
      this.populationCacheMgr = populationCacheMgr;
   }

   /* (non-Javadoc)
    * @see com.iris.hubcom.server.session.listener.HubSessionAdapter#onConnected(com.iris.hubcom.server.session.HubSession)
    */
   @Override
   public void onConnected(HubSession session) {
      super.onConnected(session);
      broadcastState(session, HubConnectionCapability.STATE_HANDSHAKE);
   }

   /* (non-Javadoc)
    * @see com.iris.hubcom.server.session.listener.HubSessionAdapter#onAuthorized(com.iris.hubcom.server.session.HubSession)
    */
   @Override
   public void onAuthorized(HubSession session) {
      super.onAuthorized(session);
      broadcastState(session, HubConnectionCapability.STATE_ONLINE);
      broadcast(session, HubCapability.HubConnectedEvent.instance());

      Channel channel = session.getChannel();
      SocketAddress addr = channel != null ? channel.remoteAddress() : null;
      if (addr == null || !(addr instanceof InetSocketAddress)) {
         return;
      }

      Place place = placeDao.findById(UUID.fromString(session.getActivePlace()));
      String timezone = (place != null && place.getTzId() != null) ? place.getTzId() : "";

      InetSocketAddress inet = (InetSocketAddress)addr;
      MessageBody body = MessageBody.buildMessage(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.of(
         HubCapability.ATTR_TZ, timezone,
         HubNetworkCapability.ATTR_EXTERNALIP, inet.getAddress().getHostAddress()
      ));
      PlatformMessage msg = PlatformMessage.buildRequest(body, Address.platformService(PlatformConstants.SERVICE_HUB), Address.hubService(session.getHubId(), "hub")).create();

      byte[] payload = JSON.createSerializer(PlatformMessage.class).serialize(msg);
      byte[] message = JSON.createSerializer(HubMessage.class).serialize(HubMessage.createPlatform(payload));

      session.sendMessage(message);
   }

   /* (non-Javadoc)
    * @see com.iris.hubcom.server.session.listener.HubSessionAdapter#onDisconnected(com.iris.hubcom.server.session.HubSession)
    */
   @Override
   public void onDisconnected(HubSession session) {
      super.onDisconnected(session);
      // note the state change isn't handled here, that's up to the HubService on platform-services
      // to ensure this isn't incorrect as a hub bounces between bridges
      hubRegistrationRegistry.offline(session.getHubId());  
      broadcast(session, HubCapability.HubDisconnectedEvent.instance());
   }

   private void broadcastState(Session session, String state) {
      Map<String, Object> attributes =
            ImmutableMap
               .<String, Object>of(
                     HubConnectionCapability.ATTR_STATE, state,
                     HubConnectionCapability.ATTR_LASTCHANGE, new Date()
               );

      broadcast(session, MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, attributes));
   }

   private void broadcast(Session session, MessageBody body) {
      Address address = Address.hubService(session.getClientToken().getRepresentation(), HubCapability.NAMESPACE);
      PlatformMessage msg =
            PlatformMessage
               .buildBroadcast(body, address)
               .withPlaceId(session.getActivePlace())
               .withPopulation(populationCacheMgr.getPopulationByPlaceId(session.getActivePlace()))
               .withActor(Address.clientAddress("hub-bridge", String.valueOf(partitioner.getMemberId())))
               .create();
      platformBus.send(msg);
   }
}

