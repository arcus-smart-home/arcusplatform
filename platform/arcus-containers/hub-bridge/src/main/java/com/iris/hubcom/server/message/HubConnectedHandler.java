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
package com.iris.hubcom.server.message;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.bridge.bus.PlatformBusService;
import com.iris.bridge.server.session.Session;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.core.dao.HubDAO;
import com.iris.hubcom.server.session.HubSession;
import com.iris.hubcom.server.session.HubSession.State;
import com.iris.hubcom.server.session.HubSession.UnauthorizedReason;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.Hub4gCapability;
import com.iris.messages.capability.HubAdvancedCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.HubNetworkCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Hub;
import com.iris.messages.services.PlatformConstants;
import com.iris.platform.hub.registration.HubRegistrationRegistry;
import com.iris.platform.partition.Partitioner;
import com.iris.platform.partition.PlatformPartition;
import com.iris.population.HubPopulationResolver;
import com.iris.population.PlacePopulationCacheManager;

@Singleton
public class HubConnectedHandler extends DirectMessageHandler {

   public static final String INCORRECT_ACCOUNT_CODE = "error.hub.incorrectaccount";
   public static final String INCORRECT_ACCOUNT_MSG = "Reported incorrect account.";
   public static final String DEFAULT_HUB_NAME = "Smart Hub";

   private static final Logger log = LoggerFactory.getLogger(HubConnectedHandler.class);

   private final HubDAO hubDao;
   private final BeanAttributesTransformer<Hub> hubTransformer;
   private final Partitioner partitioner;
   private final HubRegistrationRegistry hubRegistrationRegistry;
   private final HubPopulationResolver hubPopulationResolver;

   @Inject(optional = true)
   @Named("hub.auto_deregister")
   private boolean autoDeregister = false;

   @Inject(optional = true)
   @Named("hub.4g.simid.attr")
   private String simIdAttr = Hub4gCapability.ATTR_ICCID;

   @Inject(optional = true)
   @Named("hub.allow.update.attributes")
   private boolean allowUpdateAttributes = true;

   @Inject
   public HubConnectedHandler(
         PlatformBusService bus,
         BeanAttributesTransformer<Hub> hubTransformer,
         HubDAO hubDao,
         Partitioner partitioner,
         HubRegistrationRegistry hubRegistrationRegistry,
         HubPopulationResolver hubPopulationResolver,
         PlacePopulationCacheManager populationCacheMgr
   ) {
      super(bus, populationCacheMgr);
      this.hubDao = hubDao;
      this.hubTransformer = hubTransformer;
      this.partitioner = partitioner;
      this.hubRegistrationRegistry = hubRegistrationRegistry;
      this.hubPopulationResolver = hubPopulationResolver;
   }

   @Override
   public String supportsMessageType() {
      return MessageConstants.MSG_HUB_CONNECTED_EVENT;
   }

   @Override
   protected void doHandle(Session session, PlatformMessage msg) {
      try {
         onConnected(session, msg);
      }
      catch(Exception e) {
         log.warn("Error processing connection for hub [{}], closing session", msg.getSource(), e);
         session.disconnect(1002);
         session.destroy();
      }
   }

   protected void onConnected(Session session, PlatformMessage msg) {
      MessageBody hubConnectedEvent = msg.getValue();

      String connectionType = (String) hubConnectedEvent.getAttributes().get(HubNetworkCapability.ATTR_TYPE);
      String agentVersion = HubAdvancedCapability.getAgentver(hubConnectedEvent);
      session.setClientVersion(agentVersion);

      ((HubSession) session).setConnectionType(connectionType);
      ((HubSession) session).setSimId((String) hubConnectedEvent.getAttributes().get(simIdAttr));
      ((HubSession) session).setFirmwareVersion(HubAdvancedCapability.getOsver(hubConnectedEvent));
      String hubId = msg.getSource().getHubId();
      log.trace("Handling Hub Connected Event:  Hub Id = {}", hubId );

      Hub persistedHub = hubDao.findById(hubId);

      if(persistedHub != null && Objects.equals(HubNetworkCapability.TYPE_3G, connectionType) && persistedHub.isDisallowCell()) {
         updateUnauthorizedReason(session, UnauthorizedReason.BANNED_CELL);
         return;
      }

      if(persistedHub == null && HubCapability.getAccount(hubConnectedEvent) != null && autoDeregister) {
         sendDeregister(session,  hubId, msg.getPlaceId());
         return;
      }

      Hub minimalHub = new Hub();
      minimalHub.setId(hubId);
      minimalHub.setPlace(persistedHub == null ? null : persistedHub.getPlace());
      minimalHub.setModel(persistedHub == null ? HubCapability.getModel(hubConnectedEvent) : persistedHub.getModel());
      final boolean isBelowMinimumFirmwareVersion = upgradeIfNeeded(session, minimalHub, hubConnectedEvent);
      if (!session.getClient().isAuthenticated()) {
         updateUnauthorizedReason(session, UnauthorizedReason.UNAUTHENTICATED);
         log.info("Not saving unauthenticated hub at ip [{}]", session.getChannel().remoteAddress());
         return;
      }
      
      if (isBelowMinimumFirmwareVersion) {
         updateUnauthorizedReason(session, UnauthorizedReason.BELOW_MIN_FW);
      }
      if (isBelowMinimumFirmwareVersion && persistedHub == null) {
         log.info("Not saving hub [{}] because it is below min version and has not yet been registered", msg.getSource());
         return;
      }

      // this is from the factory firmware, but it isn't a legal enumeration value so it
      // will blow out the transformer, so we have to remove it
      if("INITIAL".equals(hubConnectedEvent.getAttributes().get(HubCapability.ATTR_STATE))) {
         hubConnectedEvent.getAttributes().remove(HubCapability.ATTR_STATE);
      }
      Hub incomingHub = hubTransformer.transform(hubConnectedEvent.getAttributes());
      if(StringUtils.isBlank(incomingHub.getId())) {
         incomingHub.setId(hubId);
      }
      if(persistedHub == null) {
         if(!isBelowMinimumFirmwareVersion) {
	         if(incomingHub.getAccount() != null) {
	            log.warn("Orphaned hub detected.  Hub [{}] connected with no record and account id [{}].", incomingHub.getId(), incomingHub.getAccount());
	            updateUnauthorizedReason(session, UnauthorizedReason.ORPHANED);
	            incomingHub.setAccount(null);
	            incomingHub.setPlace(null);
	            incomingHub.setRegistrationState(HubCapability.REGISTRATIONSTATE_ORPHANED);
	         } else {
	            updateUnauthorizedReason(session, UnauthorizedReason.UNREGISTERED);
	            incomingHub.setRegistrationState(HubCapability.REGISTRATIONSTATE_UNREGISTERED);
	         }
         }
         log.debug("Creating hub record", hubId );
         
         createHubRecord(incomingHub, hubConnectedEvent);

      } else {
         log.trace("Verifying hub", hubId );
         verifyHub(session, persistedHub, incomingHub, msg.getCorrelationId(), isBelowMinimumFirmwareVersion, hubConnectedEvent);
      }

      // at this point we have a hub record so we can associate it with the proper partition
      PlatformPartition partition = null;
      if(incomingHub.getPlace() == null) {
         partition = partitioner.getPartitionForHubId(incomingHub.getId());
      }
      else {
         partition = partitioner.getPartitionForPlaceId(incomingHub.getPlace());
      }
      ((HubSession) session).setPartition(partition);
   }



   private boolean upgradeIfNeeded(Session session, Hub hub, MessageBody request) {
      String hardwareVersion = HubAdvancedCapability.getHardwarever(request);
      String firmwareVersion = HubAdvancedCapability.getOsver(request);
      String agentVersion = HubAdvancedCapability.getAgentver(request);

      log.info("{} firmware versions: model={}, hwVer={}, osVer={}, agentVer={}", hub.getId(), hub.getModel(), hardwareVersion, firmwareVersion, agentVersion);

      if (firmwareVersion == null || firmwareVersion.equalsIgnoreCase("unknown")) {
         log.warn("Hub [{}] firmware version is unkown.");
         return false;
      }

      //require upgrade
      hubRegistrationRegistry.online(hub.getId());
      
      MessageBody msgBody = hubRegistrationRegistry.upgradeIfNeeded(hub, firmwareVersion, hubPopulationResolver.getPopulationNameForHub(hub));
      if (msgBody == null) {
    	  return false;
      }
      PlatformMessage msg = PlatformMessage.buildMessage(
         msgBody,
         Address.platformService(PlatformConstants.SERVICE_HUB),
         Address.fromString(hub.getAddress()))
         .withPlaceId(hub.getPlace())
         .create();
      sendToHub(session, msg);
   
      return true;
   }

   private void createHubRecord(Hub hub, MessageBody evt) {
      // make sure account and place are null just in case the hub is returning stale data
      hub.setAccount(null);
      hub.setPlace(null);
      hub.setName(DEFAULT_HUB_NAME);
      hubDao.save(hub);

      if (allowUpdateAttributes) {
         hubDao.updateAttributes(hub.getId(), evt.getAttributes());
      }
      

  }

   private void verifyHub(Session session, Hub persistedHub, Hub incomingHub, String correlationId, boolean isBelowMinimumFirmwareVersion, MessageBody evt) {
      // update the version information on the persisted hub always to make sure the db is consistent
      // with the value reported by the hub and send ValueChange's to ensure the other systems are in sync as well
      // several of these fields really shouldn't change ever, but just in case...

      Map<String, Object> changes = new HashMap<>();
      persistedHub.setCaps(updateIfChanged(persistedHub.getCaps(), incomingHub.getCaps(), Capability.ATTR_CAPS, changes));
      persistedHub.setAgentver(updateIfChanged(persistedHub.getAgentver(), incomingHub.getAgentver(), HubAdvancedCapability.ATTR_AGENTVER, changes));
      persistedHub.setBootloaderVer(updateIfChanged(persistedHub.getBootloaderVer(), incomingHub.getBootloaderVer(), HubAdvancedCapability.ATTR_BOOTLOADERVER, changes));
      persistedHub.setHardwarever(updateIfChanged(persistedHub.getHardwarever(), incomingHub.getHardwarever(), HubAdvancedCapability.ATTR_HARDWAREVER, changes));
      persistedHub.setMfgInfo(updateIfChanged(persistedHub.getMfgInfo(), incomingHub.getMfgInfo(), HubAdvancedCapability.ATTR_MFGINFO, changes));
      persistedHub.setModel(updateIfChanged(persistedHub.getModel(), incomingHub.getModel(), HubCapability.ATTR_MODEL, changes));
      persistedHub.setOsver(updateIfChanged(persistedHub.getOsver(), incomingHub.getOsver(), HubAdvancedCapability.ATTR_OSVER, changes));
      persistedHub.setSerialNum(updateIfChanged(persistedHub.getSerialNum(), incomingHub.getSerialNum(), HubAdvancedCapability.ATTR_SERIALNUM, changes));
      persistedHub.setState(updateIfChanged(persistedHub.getState(), incomingHub.getState(), HubCapability.ATTR_STATE, changes));
      persistedHub.setName(updateIfChanged(persistedHub.getName(), incomingHub.getName(), HubCapability.ATTR_NAME, changes));
      // skip firmware group

      // TODO record hub last connected and last authorized timestamps

      if(!isBelowMinimumFirmwareVersion && persistedHub.getAccount() == null) {
         updateUnauthorizedReason(session, UnauthorizedReason.UNREGISTERED);
         persistedHub.setRegistrationState(HubCapability.REGISTRATIONSTATE_UNREGISTERED);
      }

      if (StringUtils.isBlank(persistedHub.getName())) {
         persistedHub.setName(DEFAULT_HUB_NAME);
         changes.put(HubCapability.ATTR_NAME, DEFAULT_HUB_NAME);
      }

      hubDao.save(persistedHub);
      if (allowUpdateAttributes) {
         hubDao.updateAttributes(persistedHub.getId(), evt.getAttributes());
      }

      // if the persisted hub doesn't have an account yet, then bail because we are waiting for
      // the client to send the register message. If it is it below the minimum firmware, then bail
      // as well.
      if(isBelowMinimumFirmwareVersion || persistedHub.getAccount() == null) {
         return;
      }

      // TODO:  checks for registration or lost data, for now as long as the account's line up send the hey hey
      // for now if the incomingHub doesn't have an account id, but the record does, we'll just send the
      // hub the account and place information from the existing hub so that restarting the agent, which
      // doesn't have
      if(incomingHub.getAccount() == null && persistedHub.getAccount() != null) {
         // don't need to worry about the changes because this will result in a base:Added when it succeeds
         sendHubRegistered(session, incomingHub.getId(), persistedHub.getAccount(), persistedHub.getPlace());

      }
      else if(Objects.equals(persistedHub.getAccount(), incomingHub.getAccount())) {
         notifyAuthorized(session, incomingHub, correlationId, changes, evt);
      }
      else {
         updateUnauthorizedReason(session, UnauthorizedReason.INVALID_ACCOUNT);
         sendToHub(session, PlatformMessage.buildMessage(
               Errors.fromCode(INCORRECT_ACCOUNT_CODE, INCORRECT_ACCOUNT_MSG),
               Address.platformService(PlatformConstants.SERVICE_HUB),
               Address.fromString(incomingHub.getAddress()))
               .withCorrelationId(correlationId)
               .withPlaceId(incomingHub.getPlace())
               .create());
      }
   }

   private <V> V updateIfChanged(V persistedValue, V incomingValue, String attribute, Map<String, Object> changes) {
      if(incomingValue == null) {
         return persistedValue;
      }
      if(Objects.equals(persistedValue, incomingValue)) {
         return persistedValue;
      }
      changes.put(attribute, incomingValue);
      return incomingValue;
   }

   private void sendHubRegistered(Session session, String hubId, UUID accountId, UUID placeId) {
      Map<String,Object> attrs = new HashMap<>();
         attrs.put(HubCapability.ATTR_ACCOUNT, String.valueOf(accountId));
         attrs.put(HubCapability.ATTR_PLACE, String.valueOf(placeId));

      updateSessionState(session, State.PENDING_REG_ACK);
      updateUnauthorizedReason(session, UnauthorizedReason.REGISTERING);
      sendToHub(session, PlatformMessage.buildMessage(
            MessageBody.buildMessage(MessageConstants.MSG_HUB_REGISTERED_REQUEST, attrs),
            Address.platformService(PlatformConstants.SERVICE_HUB),
            Address.hubService(hubId, "hub"))
            .withPlaceId(placeId)
            .create());
   }

   private void notifyAuthorized(Session session, Hub hub, String correlationId, Map<String, Object> changes, MessageBody evt) {
      Address hubAddress = Address.fromString(hub.getAddress());
      authorized(session, hub, correlationId);
      // even with the attribute report, send the old value change in case the UI's are dependent on it.
      if(!changes.isEmpty()) {
         sendToPlatform(
            PlatformMessage
               .buildBroadcast(
                  MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, changes),
                  hubAddress
               )
               .withPlaceId(hub.getPlace())
               .withPopulation(getPlacePopulationCacheManager().getPopulationByPlaceId(hub.getPlace()))
               .create()
         );
      }
      sendToPlatform(
         PlatformMessage
            .buildBroadcast(
               MessageBody.buildMessage(Capability.EVENT_REPORT, evt.getAttributes()),
               hubAddress
            )
            .withPlaceId(hub.getPlace())
            .withPopulation(getPlacePopulationCacheManager().getPopulationByPlaceId(hub.getPlace()))
            .create()
      );
   }

   private void sendDeregister(Session session, String hubId, String placeId) {
      PlatformMessage deregister = PlatformMessage.buildMessage(HubAdvancedCapability.DeregisterEvent.instance(),
            Address.platformService(PlatformConstants.SERVICE_HUB),
            Address.hubAddress(hubId))
            .withPlaceId(placeId)
            .create();
      sendToHub(session, deregister);
   }
   

}

