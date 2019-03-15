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
package com.iris.hubcom.authz;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.bridge.bus.PlatformBusService;
import com.iris.hubcom.server.session.HubSession;
import com.iris.hubcom.server.session.HubSession.State;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.HubAdvancedCapability;
import com.iris.protocol.ProtocolMessage;
import com.iris.security.authz.AuthzUtil;

@Singleton
public class DefaultHubMessageFilterImpl implements HubMessageFilter {
   private static final Logger logger = LoggerFactory.getLogger(DefaultHubMessageFilterImpl.class);

   public static final String ADMIN_ADDRESS_PROP = "hub.bridge.admin.address.predicate";
   public static final String ADMIN_ONLY_MESSAGES_PROP = "hub.bridge.admin.only.message.predicate";

   private final Predicate<Address> adminAddress;
   private final Predicate<String> adminOnlyMessage;
   private final PlatformBusService platformBusService;

   @Inject
   public DefaultHubMessageFilterImpl(
      @Named(ADMIN_ADDRESS_PROP) Predicate<Address> adminAddress,
      @Named(ADMIN_ONLY_MESSAGES_PROP) Predicate<String> adminOnlyMessage,
      PlatformBusService platformBusService) {
      this.adminAddress = adminAddress;
      this.adminOnlyMessage = adminOnlyMessage;
      this.platformBusService = platformBusService;
   }

   @Override
   public boolean acceptFromHub(HubSession session, PlatformMessage msg) {
      String hubId = session.getHubId();
      Address messageAddress = msg.getSource();

      if(!Objects.equals(hubId, messageAddress.getHubId())) {
         logger.debug("Dropping message because source address [{}] with hubId [{}] does not match session address [{}]", messageAddress, messageAddress.getHubId(), session);
         return false;
      }

      // TODO:  presumably we want to allow firmware related messages regardless of whether or not
      // the hub has been fully authorized to allow a firmware update to take place the moment the
      // hub comes online
      String type = msg.getMessageType();
      if(
            session.getState() == State.AUTHORIZED ||
            MessageConstants.MSG_HUB_CONNECTED_EVENT.equals(type) ||
            HubAdvancedCapability.FirmwareUpdateResponse.NAME.equals(type) ||
            HubAdvancedCapability.FirmwareUpgradeProcessEvent.NAME.equals(type) ||
            ErrorEvent.MESSAGE_TYPE.equals(msg.getMessageType()) ||
            // allow responses to admin requests
            (!msg.isRequest() && adminAddress.apply(msg.getDestination()))
      ) {

         return true;
      }

      if(session.getState() == State.PENDING_REG_ACK && MessageConstants.MSG_HUB_REGISTERED_RESPONSE.equals(msg.getMessageType())) {
         session.setState(State.REGISTERED);
         return true;
      }

      logger.debug("Dropping message because hub is not authorized yet: {}", session);
      session.metrics().incPlatformMsgDiscardedCounter();
      return false;
   }

   @Override
   public boolean acceptFromHub(HubSession session, ProtocolMessage msg) {
      return session.getState() == State.AUTHORIZED;
   }

   @Override
   public boolean acceptFromPlatform(HubSession session, PlatformMessage msg) {
      String placeId = msg.getPlaceId();

      // FIXME replace this with the admin place
      if(StringUtils.isEmpty(placeId)) {
         return true;
      }

      if(adminAddress.apply(msg.getSource())) {
         return true;
      }

      if (adminOnlyMessage.apply(msg.getMessageType())) {
         logger.warn("Rejecting admin only message from non-admin client: [{}]", msg.getMessageType());
         if (msg.isRequest()) {
            MessageBody body = AuthzUtil.createUnauthorizedEvent();
            PlatformMessage rsp = PlatformMessage.buildResponse(msg,body).create();
            platformBusService.placeMessageOnPlatformBus(rsp);
         }

         return false;
      }

      if(placeId.equals(session.getActivePlace())) {
         return true;
      }

      logger.warn("Rejecting message with incorrect place header [{}] from [{}]", placeId, msg.getSource());
      return false;
   }

   @Override
   public boolean acceptFromProtocol(HubSession session, ProtocolMessage msg) {
      String placeId = msg.getPlaceId();
      if(placeId != null && placeId.equals(session.getActivePlace())) {
         return true;
      }

      logger.warn("Rejecting protocol message with incorrect place header [{}] from [{}]", placeId, msg.getSource());
      return false;
   }
}

