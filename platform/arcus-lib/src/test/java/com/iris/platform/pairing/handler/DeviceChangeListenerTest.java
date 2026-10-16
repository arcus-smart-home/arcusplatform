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
package com.iris.platform.pairing.handler;

import java.util.Map;
import java.util.UUID;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.PairingDeviceCapability;
import com.iris.messages.model.serv.PairingDeviceModel;
import com.iris.platform.pairing.PairingDevice;
import com.iris.platform.pairing.PairingDeviceDao;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

@Mocks({ PairingDeviceDao.class })
@Modules({ InMemoryMessageModule.class })
public class DeviceChangeListenerTest extends IrisMockTestCase {

   @Inject private InMemoryPlatformMessageBus platformBus;
   @Inject private PairingDeviceDao pairingDeviceDao;
   @Inject private DeviceChangeListener listener;

   private final UUID placeId = UUID.randomUUID();
   private final Address deviceAddress = Address.platformDriverAddress(UUID.randomUUID());

   @Test
   public void testMispairedDeviceRecoversWhenErrorsCleared() throws Exception {
      PairingDevice model = pairingDevice(PairingDeviceCapability.PAIRINGSTATE_MISPAIRED, PairingDeviceCapability.PAIRINGPHASE_FAILED);
      Capture<PairingDevice> saved = expectListAndSave(model);
      replay();

      listener.onDeviceChanged(valueChange(DeviceAdvancedCapability.DRIVERSTATE_ACTIVE, ImmutableMap.of()));

      assertEquals(PairingDeviceCapability.PAIRINGSTATE_PAIRED, PairingDeviceModel.getPairingState(saved.getValue()));
      assertEquals(PairingDeviceCapability.PAIRINGPHASE_PAIRED, PairingDeviceModel.getPairingPhase(saved.getValue()));
      PlatformMessage changed = platformBus.take();
      assertEquals(Capability.EVENT_VALUE_CHANGE, changed.getMessageType());
      assertEquals(PairingDeviceCapability.PAIRINGSTATE_PAIRED, changed.getValue().getAttributes().get(PairingDeviceCapability.ATTR_PAIRINGSTATE));
      verify();
   }

   @Test
   public void testMispairedDeviceStaysMispairedWhileErrorPresent() throws Exception {
      PairingDevice model = pairingDevice(PairingDeviceCapability.PAIRINGSTATE_MISPAIRED, PairingDeviceCapability.PAIRINGPHASE_FAILED);
      EasyMock.expect(pairingDeviceDao.listByPlace(placeId)).andReturn(ImmutableList.of(model));
      replay();

      listener.onDeviceChanged(valueChange(
            DeviceAdvancedCapability.DRIVERSTATE_UNSUPPORTED,
            ImmutableMap.of(DeviceAdvancedCapability.PairingFailedException.CODE_PAIRING_FAILED, "Driver could not be determined.")
      ));

      assertEquals(PairingDeviceCapability.PAIRINGSTATE_MISPAIRED, PairingDeviceModel.getPairingState(model));
      assertEquals(PairingDeviceCapability.PAIRINGPHASE_FAILED, PairingDeviceModel.getPairingPhase(model));
      // nothing changed, so nothing is saved or sent (the strict DAO mock fails on a save)
      assertNull(platformBus.poll());
      verify();
   }

   @Test
   public void testMispairedDeviceIgnoresChangeWithoutErrors() throws Exception {
      PairingDevice model = pairingDevice(PairingDeviceCapability.PAIRINGSTATE_MISPAIRED, PairingDeviceCapability.PAIRINGPHASE_FAILED);
      EasyMock.expect(pairingDeviceDao.listByPlace(placeId)).andReturn(ImmutableList.of(model));
      replay();

      // the driver state changed but the errors didn't, so pairing.failed may still be set
      listener.onDeviceChanged(valueChange(ImmutableMap.of(DeviceAdvancedCapability.ATTR_DRIVERSTATE, DeviceAdvancedCapability.DRIVERSTATE_ACTIVE)));

      assertEquals(PairingDeviceCapability.PAIRINGSTATE_MISPAIRED, PairingDeviceModel.getPairingState(model));
      assertNull(platformBus.poll());
      verify();
   }

   @Test
   public void testMispairedDeviceRecoversOnErrorsChangeAlone() throws Exception {
      PairingDevice model = pairingDevice(PairingDeviceCapability.PAIRINGSTATE_MISPAIRED, PairingDeviceCapability.PAIRINGPHASE_FAILED);
      Capture<PairingDevice> saved = expectListAndSave(model);
      replay();

      // pairing.failed was cleared on a device whose driver state didn't change, e.g. a lost device
      listener.onDeviceChanged(valueChange(ImmutableMap.of(DeviceAdvancedCapability.ATTR_ERRORS, ImmutableMap.of())));

      assertEquals(PairingDeviceCapability.PAIRINGSTATE_PAIRED, PairingDeviceModel.getPairingState(saved.getValue()));
      assertEquals(PairingDeviceCapability.PAIRINGPHASE_PAIRED, PairingDeviceModel.getPairingPhase(saved.getValue()));
      verify();
   }

   @Test
   public void testIgnoresErrorsChangeThatKeepsPairingFailed() throws Exception {
      replay();

      // pairing.failed is still there, so no pairing device can change; don't look them up
      listener.onDeviceChanged(valueChange(ImmutableMap.of(
            DeviceAdvancedCapability.ATTR_ERRORS,
            ImmutableMap.of(DeviceAdvancedCapability.PairingFailedException.CODE_PAIRING_FAILED, "Driver could not be determined.", "other.error", "Something else")
      )));

      assertNull(platformBus.poll());
      verify();
   }

   @Test
   public void testProvisioningDeviceIgnoresErrorsChangeAlone() throws Exception {
      PairingDevice model = pairingDevice(PairingDeviceCapability.PAIRINGSTATE_PAIRING, PairingDeviceCapability.PAIRINGPHASE_CONFIGURE);
      EasyMock.expect(pairingDeviceDao.listByPlace(placeId)).andReturn(ImmutableList.of(model));
      replay();

      // an unrelated error while provisioning must not mark the device paired
      listener.onDeviceChanged(valueChange(ImmutableMap.of(DeviceAdvancedCapability.ATTR_ERRORS, ImmutableMap.of("other.error", "Something else"))));

      assertEquals(PairingDeviceCapability.PAIRINGSTATE_PAIRING, PairingDeviceModel.getPairingState(model));
      assertNull(platformBus.poll());
      verify();
   }

   @Test
   public void testMispairedDeviceRecoversFromAnnouncedPairingState() throws Exception {
      PairingDevice model = pairingDevice(PairingDeviceCapability.PAIRINGSTATE_MISPAIRED, PairingDeviceCapability.PAIRINGPHASE_FAILED);
      Capture<PairingDevice> saved = expectListAndSave(model);
      replay();

      // what driver services announces when it loads a device whose pairing.failed was cleared long ago,
      // with the driver state in the lower case the devices table stores
      listener.onDeviceChanged(valueChange(ImmutableMap.of(
            DeviceAdvancedCapability.ATTR_DRIVERSTATE, "active",
            DeviceAdvancedCapability.ATTR_ERRORS, ImmutableMap.of()
      )));

      assertEquals(PairingDeviceCapability.PAIRINGSTATE_PAIRED, PairingDeviceModel.getPairingState(saved.getValue()));
      assertEquals(PairingDeviceCapability.PAIRINGPHASE_PAIRED, PairingDeviceModel.getPairingPhase(saved.getValue()));
      verify();
   }

   @Test
   public void testMisconfiguredDeviceRecoversWhenErrorsCleared() throws Exception {
      PairingDevice model = pairingDevice(PairingDeviceCapability.PAIRINGSTATE_MISCONFIGURED, PairingDeviceCapability.PAIRINGPHASE_FAILED);
      Capture<PairingDevice> saved = expectListAndSave(model);
      replay();

      // the driver called provisioned on an active device, and driver services sends the driver state with the errors
      listener.onDeviceChanged(valueChange(DeviceAdvancedCapability.DRIVERSTATE_ACTIVE, ImmutableMap.of()));

      assertEquals(PairingDeviceCapability.PAIRINGSTATE_PAIRED, PairingDeviceModel.getPairingState(saved.getValue()));
      assertEquals(PairingDeviceCapability.PAIRINGPHASE_PAIRED, PairingDeviceModel.getPairingPhase(saved.getValue()));
      verify();
   }

   @Test
   public void testMisconfiguredDeviceStaysMisconfiguredWhenItComesBack() throws Exception {
      PairingDevice model = pairingDevice(PairingDeviceCapability.PAIRINGSTATE_MISCONFIGURED, PairingDeviceCapability.PAIRINGPHASE_FAILED);
      EasyMock.expect(pairingDeviceDao.listByPlace(placeId)).andReturn(ImmutableList.of(model));
      replay();

      // a lost device comes back still misconfigured
      listener.onDeviceChanged(valueChange(
            DeviceAdvancedCapability.DRIVERSTATE_ACTIVE,
            ImmutableMap.of(DeviceAdvancedCapability.PairingMisconfiguredException.CODE_PAIRING_MISCONFIGURED, "Unable to fully configure device")
      ));

      assertEquals(PairingDeviceCapability.PAIRINGSTATE_MISCONFIGURED, PairingDeviceModel.getPairingState(model));
      assertNull(platformBus.poll());
      verify();
   }

   @Test
   public void testMisconfiguredLostDeviceRecoversWhenErrorsCleared() throws Exception {
      PairingDevice model = pairingDevice(PairingDeviceCapability.PAIRINGSTATE_MISCONFIGURED, PairingDeviceCapability.PAIRINGPHASE_FAILED);
      Capture<PairingDevice> saved = expectListAndSave(model);
      replay();

      // what driver services announces when it loads a lost device whose misconfiguration was cleared
      listener.onDeviceChanged(valueChange(ImmutableMap.of(
            DeviceAdvancedCapability.ATTR_DRIVERSTATE, "recoverable",
            DeviceAdvancedCapability.ATTR_ERRORS, ImmutableMap.of()
      )));

      assertEquals(PairingDeviceCapability.PAIRINGSTATE_PAIRED, PairingDeviceModel.getPairingState(saved.getValue()));
      assertEquals(PairingDeviceCapability.PAIRINGPHASE_PAIRED, PairingDeviceModel.getPairingPhase(saved.getValue()));
      verify();
   }

   @Test
   public void testMisconfiguredDeviceIgnoresChangeWithoutErrors() throws Exception {
      PairingDevice model = pairingDevice(PairingDeviceCapability.PAIRINGSTATE_MISCONFIGURED, PairingDeviceCapability.PAIRINGPHASE_FAILED);
      EasyMock.expect(pairingDeviceDao.listByPlace(placeId)).andReturn(ImmutableList.of(model));
      replay();

      // the driver state changed but the errors didn't come with it, so pairing.misconfigured may still be set
      listener.onDeviceChanged(valueChange(ImmutableMap.of(DeviceAdvancedCapability.ATTR_DRIVERSTATE, DeviceAdvancedCapability.DRIVERSTATE_ACTIVE)));

      assertEquals(PairingDeviceCapability.PAIRINGSTATE_MISCONFIGURED, PairingDeviceModel.getPairingState(model));
      assertNull(platformBus.poll());
      verify();
   }

   @Test
   public void testPairingDeviceIgnoresLostDevice() throws Exception {
      PairingDevice model = pairingDevice(PairingDeviceCapability.PAIRINGSTATE_PAIRING, PairingDeviceCapability.PAIRINGPHASE_CONFIGURE);
      EasyMock.expect(pairingDeviceDao.listByPlace(placeId)).andReturn(ImmutableList.of(model));
      replay();

      // a device lost while it was being configured hasn't finished pairing
      listener.onDeviceChanged(valueChange(ImmutableMap.of(
            DeviceAdvancedCapability.ATTR_DRIVERSTATE, "recoverable",
            DeviceAdvancedCapability.ATTR_ERRORS, ImmutableMap.of()
      )));

      assertEquals(PairingDeviceCapability.PAIRINGSTATE_PAIRING, PairingDeviceModel.getPairingState(model));
      assertNull(platformBus.poll());
      verify();
   }

   @Test
   public void testPairedDeviceIgnoresLaterChanges() throws Exception {
      PairingDevice model = pairingDevice(PairingDeviceCapability.PAIRINGSTATE_PAIRED, PairingDeviceCapability.PAIRINGPHASE_PAIRED);
      EasyMock.expect(pairingDeviceDao.listByPlace(placeId)).andReturn(ImmutableList.of(model));
      replay();

      listener.onDeviceChanged(valueChange(
            DeviceAdvancedCapability.DRIVERSTATE_UNSUPPORTED,
            ImmutableMap.of(DeviceAdvancedCapability.PairingFailedException.CODE_PAIRING_FAILED, "Driver could not be determined.")
      ));

      assertEquals(PairingDeviceCapability.PAIRINGSTATE_PAIRED, PairingDeviceModel.getPairingState(model));
      assertNull(platformBus.poll());
      verify();
   }

   private PairingDevice pairingDevice(String pairingState, String pairingPhase) {
      PairingDevice model = new PairingDevice();
      model.setId(placeId, 1);
      PairingDeviceModel.setDeviceAddress(model, deviceAddress.getRepresentation());
      PairingDeviceModel.setPairingState(model, pairingState);
      PairingDeviceModel.setPairingPhase(model, pairingPhase);
      return model;
   }

   private Capture<PairingDevice> expectListAndSave(PairingDevice model) {
      EasyMock.expect(pairingDeviceDao.listByPlace(placeId)).andReturn(ImmutableList.of(model));
      Capture<PairingDevice> saved = EasyMock.newCapture();
      EasyMock.expect(pairingDeviceDao.save(EasyMock.capture(saved))).andAnswer(() -> saved.getValue());
      return saved;
   }

   private PlatformMessage valueChange(String driverState, Object errors) {
      return valueChange(ImmutableMap.of(
            DeviceAdvancedCapability.ATTR_DRIVERSTATE, driverState,
            DeviceAdvancedCapability.ATTR_ERRORS, errors
      ));
   }

   private PlatformMessage valueChange(Map<String, Object> attributes) {
      MessageBody body = MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, attributes);
      return PlatformMessage.builder()
            .from(deviceAddress)
            .withPlaceId(placeId)
            .withPayload(body)
            .create();
   }
}
