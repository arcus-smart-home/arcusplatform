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
package com.iris.platform.subsystem.pairing.state;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.easymock.EasyMock;
import org.easymock.IExpectationSetters;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.iris.common.subsystem.SubsystemTestCase;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.core.dao.HubDAO;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.HubCapability.PairingRequestRequest;
import com.iris.messages.capability.HubCapability.PairingRequestResponse;
import com.iris.messages.capability.HubCapability.UnpairingRequestRequest;
import com.iris.messages.capability.HubCapability.UnpairingRequestResponse;
import com.iris.messages.capability.PairingSubsystemCapability;
import com.iris.messages.capability.PairingSubsystemCapability.DismissAllRequest;
import com.iris.messages.capability.PairingSubsystemCapability.FactoryResetRequest;
import com.iris.messages.capability.PairingSubsystemCapability.ListHelpStepsRequest;
import com.iris.messages.capability.PairingSubsystemCapability.SearchRequest;
import com.iris.messages.capability.PairingSubsystemCapability.StartPairingRequest;
import com.iris.messages.capability.PairingSubsystemCapability.StopSearchingRequest;
import com.iris.messages.capability.ProductCapability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.subs.PairingSubsystemModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.messages.service.BridgeService.RegisterDeviceRequest;
import com.iris.platform.manufacture.kitting.dao.ManufactureKittingDao;
import com.iris.platform.pairing.PairingDeviceDao;
import com.iris.platform.subsystem.pairing.BridgePairingInfo;
import com.iris.platform.subsystem.pairing.PairingConfig;
import com.iris.platform.subsystem.pairing.PairingSubsystem;
import com.iris.platform.subsystem.pairing.PairingUtils;
import com.iris.platform.subsystem.pairing.ProductFixtures;
import com.iris.platform.subsystem.pairing.ProductFixtures.ProductCatalogEntryBuilder;
import com.iris.platform.subsystem.pairing.ProductLoaderForPairing;
import com.iris.prodcat.ProductCatalogEntry;
import com.iris.test.Mocks;

@Mocks({ PairingDeviceDao.class, ProductLoaderForPairing.class, HubDAO.class, ManufactureKittingDao.class })
public class PairingSubsystemTestCase extends SubsystemTestCase<PairingSubsystemModel> {

   private boolean started = false;
   protected String productAddress = Address.platformService("test", ProductCapability.NAMESPACE).getRepresentation();
   @Inject protected PairingSubsystem subsystem;
   @Inject protected PairingDeviceDao pairingDeviceDao;
   @Inject protected ProductLoaderForPairing mockProductLoader;

   @Provides @Singleton
   public PairingConfig pairingConfig() {
      return new PairingConfig();
   }

   @Override
   protected PairingSubsystemModel createSubsystemModel() {
      Map<String,Object> attributes = ModelFixtures.createServiceAttributes(SubsystemCapability.NAMESPACE, PairingSubsystemCapability.NAMESPACE);
      return new PairingSubsystemModel(new SimpleModel(attributes));
   }

   @Override
   protected PairingSubsystem subsystem() {
      return subsystem;
   }

   protected void start() throws Exception {
      init(subsystem);
      started = true;
   }

   protected boolean isStarted() {
      return started;
   }

   protected void stageFactoryReset(String mode) throws Exception {
      init(subsystem);
      context.model().setPairingMode(mode);
      context.model().setSearchProductAddress(productAddress);
      PairingStateMachine.get(context).transition(PairingStateName.FactoryResetSteps.name());
      commit();
   }
   
   protected void stageFactoryResetIpcd() throws Exception {
      stageFactoryReset(PairingSubsystemCapability.PAIRINGMODE_IDLE);
   }

   protected Model stageFactoryResetHub() throws Exception {
      Model hub = addModel(ModelFixtures.buildHubAttributes().put(HubCapability.ATTR_STATE, HubCapability.STATE_NORMAL).create());
      stageFactoryReset(PairingSubsystemCapability.PAIRINGMODE_IDLE);
      return hub;
   }

   protected Model stageFactoryResetZWave() throws Exception {
      Model hub = addModel(ModelFixtures.buildHubAttributes().put(HubCapability.ATTR_STATE, HubCapability.STATE_UNPAIRING).create());
      stageFactoryReset(PairingSubsystemCapability.PAIRINGMODE_HUB_UNPAIRING);
      return hub;
   }

   protected void stagePairingSteps(String mode) throws Exception {
      init(subsystem);
      context.model().setPairingMode(mode);
      context.model().setSearchProductAddress(productAddress);
      PairingStateMachine.get(context).transition(PairingStateName.PairingSteps.name());
      commit();
   }
   
   protected void stagePairingStepsCloud() throws Exception {
      stagePairingSteps(PairingSubsystemCapability.PAIRINGMODE_CLOUD);
   }
   
   protected Model stagePairingStepsHub() throws Exception {
      Model hub = addModel(ModelFixtures.buildHubAttributes().put(HubCapability.ATTR_STATE, HubCapability.STATE_PAIRING).create());
      stagePairingSteps(PairingSubsystemCapability.PAIRINGMODE_HUB);
      return hub;
   }
   
   protected void stageSearching(String mode, String productAddress, boolean found) throws Exception {
      init(subsystem);
      context.model().setPairingMode(mode);
      context.model().setSearchDeviceFound(found);
      context.model().setSearchProductAddress(productAddress);
      PairingStateMachine.get(context).transition(PairingStateName.Searching.name());
      commit();
   }

   protected void stageSearchingIpcd() throws Exception {
      BridgePairingInfo info = new BridgePairingInfo();
      info.setAddress(Address.fromString("BRDG::IPCD"));
      info.setMessage(RegisterDeviceRequest.NAME);
      info.setAttributes(ProductFixtures.ipcdForm());
      PairingUtils.setBridgePairingInfo(context, info);
      stageSearching(PairingSubsystemCapability.PAIRINGMODE_CLOUD, productAddress, false);
   }

   protected Model stageSearchingHub() throws Exception {
      Model hub = addModel(ModelFixtures.buildHubAttributes().put(HubCapability.ATTR_STATE, HubCapability.STATE_PAIRING).create());
      stageSearching(PairingSubsystemCapability.PAIRINGMODE_HUB, productAddress, false);
      return hub;
   }

   /**
    * Variant of sendRequest that understands many of the pairing subsystem
    * requests respond asynchronously and acts appropriately.
    * @param message
    * @return
    */
   protected Optional<MessageBody> sendPairingRequest(MessageBody message) {
      MessageBody response = sendRequest(request(message));
      if(MessageBody.noResponse().equals(response)) {
         // grab the real response if it has been sent...
         if(responses.getValues().size() >= 2) {
            return Optional.of(responses.getValues().get(responses.getValues().size() - 2));
         }
         else {
            return Optional.empty();
         }
      }
      else {
         return Optional.of(response);
      }
   }

   protected Optional<MessageBody> factoryReset() {
      return sendPairingRequest( FactoryResetRequest.instance() );
   }

   protected Optional<MessageBody> dismissAll() {
      return sendPairingRequest( DismissAllRequest.instance() );
   }

   protected Optional<MessageBody> listHelpSteps() {
      return sendPairingRequest( ListHelpStepsRequest.instance() );
   }

   protected Optional<MessageBody> search() {
      return search(null, null);
   }
   
   protected Optional<MessageBody> search(@Nullable String productAddress, @Nullable Map<String, String> form) {
      MessageBody message =
         SearchRequest
            .builder()
            .withProductAddress(productAddress)
            .withForm(form)
            .build();
      return sendPairingRequest(message);
   }
   
   protected Optional<MessageBody> startPairing() {
      return startPairing(productAddress, false);
   }

   protected Optional<MessageBody> startPairing(String productAddress, boolean mock) {
      MessageBody message =
         StartPairingRequest
            .builder()
            .withMock(false)
            .withProductAddress(productAddress)
            .build();
      return sendPairingRequest(message);
   }

   protected Optional<MessageBody> stopSearching() {
      return sendPairingRequest( StopSearchingRequest.instance() );
   }

   protected SendAndExpect popRequest() {
      assertFalse(sendAndExpectOperations.isEmpty());
      return sendAndExpectOperations.remove(0);
   }

   /**
    * Dispatches a timeout event for the current state
    */
   protected void sendTimeout() {
      subsystem.onEvent(timeout(PairingStateMachine.get(context).current().name()), context);
   }

   protected PlatformMessage buildStartPairingResponse(Address hub) {
      return
         PlatformMessage
            .builder()
            .from(hub)
            .to(context.model().getAddress())
            .withPayload(PairingRequestResponse.instance())
            .create();
   }

   protected PlatformMessage buildStartUnpairingResponse(Address hub) {
      return
         PlatformMessage
            .builder()
            .from(hub)
            .to(context.model().getAddress())
            .withPayload(UnpairingRequestResponse.instance())
            .create();
   }

   protected void assertStartPairingRequestSent(Address expected, SendAndExpect sae) {
      assertEquals(expected, sae.getRequestAddress());
      assertEquals(PairingRequestRequest.NAME, sae.getMessage().getMessageType());
      assertEquals(PairingRequestRequest.ACTIONTYPE_START_PAIRING, sae.getMessage().getAttributes().get(PairingRequestRequest.ATTR_ACTIONTYPE));
   }

   protected void assertStartUnpairingRequestSent(Address expected, SendAndExpect sae) {
      assertEquals(expected, sae.getRequestAddress());
      assertEquals(UnpairingRequestRequest.NAME, sae.getMessage().getMessageType());
      assertEquals(UnpairingRequestRequest.ACTIONTYPE_START_UNPAIRING, sae.getMessage().getAttributes().get(UnpairingRequestRequest.ATTR_ACTIONTYPE));
   }

   protected void assertStopPairingRequestSent(Address expected) {
      assertContainsRequestMessageWithAttrs(PairingRequestRequest.NAME, ImmutableMap.of(PairingRequestRequest.ATTR_ACTIONTYPE, PairingRequestRequest.ACTIONTYPE_STOP_PAIRING));
   }

   protected void assertStopUnpairingRequestSent(Address expected) {
      assertContainsRequestMessageWithAttrs(UnpairingRequestRequest.NAME, ImmutableMap.of(UnpairingRequestRequest.ATTR_ACTIONTYPE, UnpairingRequestRequest.ACTIONTYPE_STOP_UNPAIRING));
   }

   protected void assertBridgePairingRequest(SendAndExpect pairingRequest) {
      assertEquals(Address.fromString("BRDG::IPCD"), pairingRequest.getRequestAddress());
      assertEquals(RegisterDeviceRequest.NAME, pairingRequest.getMessage().getMessageType());
      assertEquals(ProductFixtures.ipcdForm(), pairingRequest.getMessage().getAttributes().get(RegisterDeviceRequest.ATTR_ATTRS));
   }

   protected ProductCatalogEntryBuilder productBuilder() {
      return ProductFixtures.buildProduct().withId("test");
   }

   protected ProductCatalogEntry productBridged() { return productBuilder().bridged().build(); }
   
   protected ProductCatalogEntry productIpcd() {
      return productBuilder().ipcd().build();
   }
   
   protected ProductCatalogEntry productOauth() {
      return productBuilder().oauth().build();
   }
   
   protected ProductCatalogEntry productZigbee() {
      return productBuilder().zigbee().withPairingTimeoutMs(10000).build();
   }
   
   protected ProductCatalogEntry productZWave() {
      return productBuilder().zwave().withPairingTimeoutMs(20000).build();
   }

   protected IExpectationSetters<Optional<ProductCatalogEntry>> expectLoadProductAndReturn(@Nullable ProductCatalogEntry entry) {
   	return
            EasyMock
               .expect(mockProductLoader.get(context, productAddress))
               .andReturn(Optional.of(entry));
   }   

   protected IExpectationSetters<Optional<ProductCatalogEntry>> expectCurrentProductAndReturn(@Nullable ProductCatalogEntry entry) {
      return
         EasyMock
            .expect(mockProductLoader.getCurrent(context))
            .andReturn(Optional.of(entry));
   }

   protected void assertError(String expectedErrorCode, MessageBody response) {
      assertEquals(ErrorEvent.MESSAGE_TYPE, response.getMessageType());
      assertEquals(expectedErrorCode, ((ErrorEvent) response).getCode());
   }

   protected void assertTimeoutCleared(PairingStateName state) {
      assertTimeoutSet(state, null);
   }
   
   protected void assertTimeoutSet(PairingStateName state, Date expected) {
      assertEquals(expected, SubsystemUtils.getTimeout(context, state.name()).orNull());
   }

   protected void assertPairingState(PairingStateName expected) {
      assertEquals(expected.name(), context.getVariable("pairingState").as(String.class));
   }
   
   protected void assertFactoryResetIdle(String productAddress) {
      assertPairingState(PairingStateName.FactoryResetSteps);
      assertTimeoutSet(PairingStateName.FactoryResetSteps, context.model().getSearchTimeout());
      
      assertEquals(PairingSubsystemCapability.PAIRINGMODE_IDLE, context.model().getPairingMode());
      assertNotNull(context.model().getPairingModeChanged());
      assertEquals(productAddress, context.model().getSearchProductAddress());
      assertEquals(false, context.model().getSearchDeviceFound());
      assertEquals(new Date(0), context.model().getSearchIdleTimeout());
      assertTrue(context.model().getSearchTimeout().getTime() > 0);
   }

   protected void assertFactoryResetZWave(String productAddress) {
      assertPairingState(PairingStateName.FactoryResetSteps);
      assertTimeoutSet(PairingStateName.FactoryResetSteps, context.model().getSearchTimeout());
      
      assertEquals(PairingSubsystemCapability.PAIRINGMODE_HUB_UNPAIRING, context.model().getPairingMode());
      assertNotNull(context.model().getPairingModeChanged());
      assertEquals(productAddress, context.model().getSearchProductAddress());
      assertEquals(false, context.model().getSearchDeviceFound());
      assertEquals(new Date(0), context.model().getSearchIdleTimeout());
      assertTrue(context.model().getSearchTimeout().getTime() > 0);
   }

   protected void assertIdlePending(Optional<PairingStateName> pending, String... pairingDeviceAddresses) {
      assertPairingState(PairingStateName.Idle);
      assertTimeoutCleared(PairingStateName.Idle);
      
      assertEquals(PairingSubsystemCapability.PAIRINGMODE_IDLE, context.model().getPairingMode());
      assertNotNull(context.model().getPairingModeChanged());
      assertEquals(Arrays.<String>asList(pairingDeviceAddresses), context.model().getPairingDevices());
      assertEquals("", context.model().getSearchProductAddress());
      assertEquals(false, context.model().getSearchDeviceFound());
      assertEquals(new Date(0), context.model().getSearchIdleTimeout());
      assertEquals(new Date(0), context.model().getSearchTimeout());
      
      assertEquals(Optional.empty(), PairingUtils.getBridgePairingInfo(context));
      if(pending.isPresent()) {
         assertEquals(pending.get(), PairingUtils.getHubPairingInfo(context).get().getPending());
      }
      else {
         assertEquals(Optional.empty(), PairingUtils.getHubPairingInfo(context));
      }
      assertFalse(PairingUtils.isMockPairing(context));
   }

   protected void assertIdle(String... pairingDeviceAddresses) {
      assertIdlePending(Optional.empty(), pairingDeviceAddresses);
   }

   protected void assertIdlePendingPairingSteps(String... pairingDeviceAddresses) {
      assertIdlePending(Optional.of(PairingStateName.PairingSteps), pairingDeviceAddresses);
   }

   protected void assertIdlePendingSearching(String... pairingDeviceAddresses) {
      assertIdlePending(Optional.of(PairingStateName.Searching), pairingDeviceAddresses);
   }

   protected void assertPairingSteps(String pairingMode, String productAddress, String... pairingDeviceAddresses) {
      assertPairingState(PairingStateName.PairingSteps);
      assertTimeoutSet(PairingStateName.PairingSteps, context.model().getSearchTimeout());
      
      assertEquals(pairingMode, context.model().getPairingMode());
      assertNotNull(context.model().getPairingModeChanged());
      assertEquals(Arrays.asList(pairingDeviceAddresses), context.model().getPairingDevices());
      assertEquals(productAddress, context.model().getSearchProductAddress());
      assertEquals(false, context.model().getSearchDeviceFound());
      assertEquals(new Date(0), context.model().getSearchIdleTimeout());
      assertTrue(context.model().getSearchTimeout().getTime() > 0);
   }
   
   protected void assertPairingStepsCloud(String productAddress, String... pairingDeviceAddresses) {
   	assertPairingSteps(PairingSubsystemCapability.PAIRINGMODE_CLOUD, productAddress, pairingDeviceAddresses);
   }

   protected void assertPairingStepsHub(String productAddress, String... pairingDeviceAddresses) {
   	assertPairingSteps(PairingSubsystemCapability.PAIRINGMODE_HUB, productAddress, pairingDeviceAddresses);
   }
   
   protected void assertPairingStepsOAuth(String productAddress, String... pairingDeviceAddresses) {
   	assertPairingSteps(PairingSubsystemCapability.PAIRINGMODE_OAUTH, productAddress, pairingDeviceAddresses);
   }

   protected void assertSearchingHubNotFound(String productAddress, String... pairingDeviceAddresses) {
      assertSearching(productAddress, PairingSubsystemCapability.PAIRINGMODE_HUB, false, false, pairingDeviceAddresses);
   }

   protected void assertSearchingHubIdle(String productAddress, String... pairingDeviceAddresses) {
      assertSearching(productAddress, PairingSubsystemCapability.PAIRINGMODE_HUB, false, true, pairingDeviceAddresses);
   }

   protected void assertSearchingCloudNotFound(String productAddress, String... pairingDeviceAddresses) {
      assertSearching(productAddress, PairingSubsystemCapability.PAIRINGMODE_CLOUD, false, false, pairingDeviceAddresses);
   }

   protected void assertSearchingCloudIdle(String productAddress, String... pairingDeviceAddresses) {
      assertSearching(productAddress, PairingSubsystemCapability.PAIRINGMODE_CLOUD, false, true, pairingDeviceAddresses);
   }

   // FIXME should really require at least one pairingDevice in this case...
   protected void assertSearchingCloudFoundDevice(String productAddress, String... pairingDeviceAddresses) {
      assertSearching(productAddress, PairingSubsystemCapability.PAIRINGMODE_CLOUD, true, false, pairingDeviceAddresses);
   }

   protected void assertSearching(String productAddress, String mode, boolean found, boolean idle, String... pairingDeviceAddresses) {
      assertPairingState(PairingStateName.Searching);
      boolean isAdvancedPairingMode = StringUtils.isBlank(productAddress);
      if(idle || found || isAdvancedPairingMode) {
         assertTimeoutSet(PairingStateName.Searching, context.model().getSearchTimeout());
      }
      else {
      	assertTimeoutSet(PairingStateName.Searching, context.model().getSearchIdleTimeout());
      }
      
      assertEquals(mode, context.model().getPairingMode());
      assertNotNull(context.model().getPairingModeChanged());
      assertEquals(Arrays.asList(pairingDeviceAddresses), context.model().getPairingDevices());
      assertEquals(productAddress, context.model().getSearchProductAddress());
      assertEquals(found, context.model().getSearchDeviceFound());
      assertEquals(idle, context.model().getSearchIdle());
      if(found || isAdvancedPairingMode) {
         assertEquals(new Date(0), context.model().getSearchIdleTimeout());
      }
      else {
         assertTrue(context.model().getSearchIdleTimeout().getTime() > 0);
      }
      assertTrue(context.model().getSearchTimeout().getTime() > 0);
   }
}

