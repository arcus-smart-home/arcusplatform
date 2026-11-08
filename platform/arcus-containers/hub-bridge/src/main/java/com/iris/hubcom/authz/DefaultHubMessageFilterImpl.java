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

import com.codahale.metrics.Counter;
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
import com.iris.messages.capability.HubZwaveCapability;
import com.iris.messages.capability.PairingSubsystemCapability;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.protocol.ProtocolMessage;
import com.iris.security.authz.AuthzUtil;

@Singleton
public class DefaultHubMessageFilterImpl implements HubMessageFilter {
   private static final Logger logger = LoggerFactory.getLogger(DefaultHubMessageFilterImpl.class);

   private static final IrisMetricSet METRICS = IrisMetrics.metrics("bridge.hub.authz");
   // a hub claiming another hub's address is a security event, not an operational blip
   private static final Counter SOURCE_MISMATCH = METRICS.counter("source.mismatch");
   private static final Counter UNAUTHORIZED_PLATFORM_DROPPED = METRICS.counter("unauthorized.platform.dropped");
   private static final Counter UNAUTHORIZED_PROTOCOL_DROPPED = METRICS.counter("unauthorized.protocol.dropped");
   private static final Counter ADMIN_ONLY_REJECTED = METRICS.counter("adminonly.rejected");
   private static final Counter PLACE_MISMATCH_PLATFORM = METRICS.counter("place.mismatch.platform");
   private static final Counter PLACE_MISMATCH_PROTOCOL = METRICS.counter("place.mismatch.protocol");
   private static final Counter S2_PIN_REJECTED = METRICS.counter("s2pin.rejected");

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

      // the DSK must not get out onto a broadcast, however the hub addressed it
      if(HubZwaveCapability.S2PinRequiredEvent.NAME.equals(msg.getMessageType()) && !isPairingSubsystem(session, msg.getDestination())) {
         S2_PIN_REJECTED.inc();
         logger.warn("Dropping [{}] from hub [{}] not addressed to its pairing subsystem", msg.getMessageType(), hubId);
         return false;
      }

      if(!Objects.equals(hubId, messageAddress.getHubId())) {
         SOURCE_MISMATCH.inc();
         // warn once per session so a misbehaving hub can't flood the log at message rate
         if(session.firstSourceMismatch()) {
            logger.warn("Dropping message because source address [{}] with hubId [{}] does not match session address [{}]; further mismatches on this session log at debug", messageAddress, messageAddress.getHubId(), session);
         } else {
            logger.debug("Dropping message because source address [{}] with hubId [{}] does not match session address [{}]", messageAddress, messageAddress.getHubId(), session);
         }
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

      UNAUTHORIZED_PLATFORM_DROPPED.inc();
      logger.debug("Dropping message because hub is not authorized yet: {}", session);
      session.metrics().incPlatformMsgDiscardedCounter();
      return false;
   }

   @Override
   public boolean acceptFromHub(HubSession session, ProtocolMessage msg) {
      if(session.getState() == State.AUTHORIZED) {
         return true;
      }
      UNAUTHORIZED_PROTOCOL_DROPPED.inc();
      return false;
   }

   @Override
   public boolean acceptFromPlatform(HubSession session, PlatformMessage msg) {
      String placeId = msg.getPlaceId();

      // The pairing subsystem checks who is answering, which request, and the PIN format.
      // This comes first since it applies to admin addresses and to messages without a place.
      if(isS2PinAnswer(msg) && !(isPairingSubsystem(session, msg.getSource()) && Objects.equals(placeId, session.getActivePlace()))) {
         S2_PIN_REJECTED.inc();
         logger.warn("Rejecting [{}] that did not come from the pairing subsystem: [{}]", msg.getMessageType(), msg.getSource());
         if (msg.isRequest()) {
            platformBusService.placeMessageOnPlatformBus(PlatformMessage.buildResponse(msg, AuthzUtil.createUnauthorizedEvent()).create());
         }
         return false;
      }

      // FIXME replace this with the admin place
      if(StringUtils.isEmpty(placeId)) {
         return true;
      }

      if(adminAddress.apply(msg.getSource())) {
         return true;
      }

      if (adminOnlyMessage.apply(msg.getMessageType())) {
         ADMIN_ONLY_REJECTED.inc();
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

      PLACE_MISMATCH_PLATFORM.inc();
      logger.warn("Rejecting message with incorrect place header [{}] from [{}]", placeId, msg.getSource());
      return false;
   }

   private static boolean isS2PinAnswer(PlatformMessage msg) {
      String type = msg.getMessageType();
      return HubZwaveCapability.SubmitS2PinRequest.NAME.equals(type) || HubZwaveCapability.CancelS2PinRequest.NAME.equals(type);
   }

   private static boolean isPairingSubsystem(HubSession session, Address address) {
      String placeId = session.getActivePlace();
      return
            !StringUtils.isEmpty(placeId) &&
            PairingSubsystemCapability.NAMESPACE.equals(address.getGroup()) &&
            placeId.equals(String.valueOf(address.getId()));
   }

   @Override
   public boolean acceptFromProtocol(HubSession session, ProtocolMessage msg) {
      String placeId = msg.getPlaceId();
      if(placeId != null && placeId.equals(session.getActivePlace())) {
         return true;
      }

      PLACE_MISMATCH_PROTOCOL.inc();
      logger.warn("Rejecting protocol message with incorrect place header [{}] from [{}]", placeId, msg.getSource());
      return false;
   }
}

