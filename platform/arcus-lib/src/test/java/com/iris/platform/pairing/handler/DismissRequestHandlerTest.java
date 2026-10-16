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

import org.easymock.EasyMock;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.PairingDeviceCapability;
import com.iris.messages.capability.PairingDeviceCapability.DismissResponse;
import com.iris.messages.errors.InvalidRequestException;
import com.iris.messages.model.serv.PairingDeviceModel;
import com.iris.platform.model.ModelEntity;
import com.iris.platform.model.PersistentModelWrapper;
import com.iris.platform.pairing.PairingDevice;
import com.iris.platform.pairing.PairingDeviceDao;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

@Mocks({ PairingDeviceDao.class, DeviceDAO.class })
@Modules({ InMemoryMessageModule.class })
public class DismissRequestHandlerTest extends IrisMockTestCase {

   @Inject private InMemoryPlatformMessageBus platformBus;
   @Inject private PairingDeviceDao pairingDeviceDao;
   @Inject private DeviceDAO deviceDao;
   @Inject private DismissRequestHandler handler;

   private final UUID placeId = UUID.randomUUID();
   private final UUID deviceId = UUID.randomUUID();

   @Test
   public void testDismissPaired() throws Exception {
      PairingDevice model = pairingDevice(PairingDeviceCapability.PAIRINGSTATE_PAIRED, PairingDeviceCapability.PAIRINGPHASE_PAIRED);
      pairingDeviceDao.delete(model);
      EasyMock.expectLastCall();
      replay();

      assertEquals(DismissResponse.instance(), handler.dismiss(wrap(model)));

      assertDeletedEvent();
      verify();
   }

   @Test
   public void testDismissMispairedWhenDeviceRecovered() throws Exception {
      PairingDevice model = pairingDevice(PairingDeviceCapability.PAIRINGSTATE_MISPAIRED, PairingDeviceCapability.PAIRINGPHASE_FAILED);
      EasyMock.expect(deviceDao.modelById(deviceId)).andReturn(device(ImmutableMap.of()));
      pairingDeviceDao.delete(model);
      EasyMock.expectLastCall();
      replay();

      assertEquals(DismissResponse.instance(), handler.dismiss(wrap(model)));

      assertDeletedEvent();
      verify();
   }

   @Test
   public void testRejectMispairedWhilePairingFailed() throws Exception {
      PairingDevice model = pairingDevice(PairingDeviceCapability.PAIRINGSTATE_MISPAIRED, PairingDeviceCapability.PAIRINGPHASE_FAILED);
      EasyMock.expect(deviceDao.modelById(deviceId)).andReturn(device(
            ImmutableMap.of(DeviceAdvancedCapability.PairingFailedException.CODE_PAIRING_FAILED, "Driver could not be determined.")
      ));
      replay();

      assertRejected(model);
      verify();
   }

   @Test
   public void testRejectMispairedWhenDeviceMissing() throws Exception {
      PairingDevice model = pairingDevice(PairingDeviceCapability.PAIRINGSTATE_MISPAIRED, PairingDeviceCapability.PAIRINGPHASE_FAILED);
      EasyMock.expect(deviceDao.modelById(deviceId)).andReturn(null);
      replay();

      assertRejected(model);
      verify();
   }

   @Test
   public void testRejectMispairedWithoutDeviceAddress() throws Exception {
      PairingDevice model = pairingDevice(PairingDeviceCapability.PAIRINGSTATE_MISPAIRED, PairingDeviceCapability.PAIRINGPHASE_FAILED);
      PairingDeviceModel.setDeviceAddress(model, null);
      replay();

      assertRejected(model);
      verify();
   }

   @Test
   public void testRejectPairing() throws Exception {
      PairingDevice model = pairingDevice(PairingDeviceCapability.PAIRINGSTATE_PAIRING, PairingDeviceCapability.PAIRINGPHASE_CONFIGURE);
      replay();

      assertRejected(model);
      verify();
   }

   private void assertRejected(PairingDevice model) {
      PersistentModelWrapper<PairingDevice> wrapper = wrap(model);
      try {
         handler.dismiss(wrapper);
         fail("Expected the dismiss to be rejected");
      }
      catch(InvalidRequestException e) {
         // expected
      }
      assertFalse(wrapper.isDeleted());
      assertNull(platformBus.poll());
   }

   private void assertDeletedEvent() throws Exception {
      PlatformMessage deleted = platformBus.take();
      assertEquals(Capability.EVENT_DELETED, deleted.getMessageType());
   }

   private PairingDevice pairingDevice(String pairingState, String pairingPhase) {
      PairingDevice model = new PairingDevice();
      model.setId(placeId, 1);
      PairingDeviceModel.setDeviceAddress(model, Address.platformDriverAddress(deviceId).getRepresentation());
      PairingDeviceModel.setPairingState(model, pairingState);
      PairingDeviceModel.setPairingPhase(model, pairingPhase);
      return model;
   }

   private PersistentModelWrapper<PairingDevice> wrap(PairingDevice model) {
      return new PersistentModelWrapper<>(platformBus, pairingDeviceDao, placeId, "general", model);
   }

   private ModelEntity device(Map<String, String> errors) {
      ModelEntity device = new ModelEntity();
      device.setAttribute(DeviceAdvancedCapability.ATTR_DRIVERSTATE, DeviceAdvancedCapability.DRIVERSTATE_ACTIVE);
      device.setAttribute(DeviceAdvancedCapability.ATTR_DRIVERNAME, "ZWTestDriver");
      device.setAttribute(DeviceAdvancedCapability.ATTR_ERRORS, errors);
      return device;
   }
}
