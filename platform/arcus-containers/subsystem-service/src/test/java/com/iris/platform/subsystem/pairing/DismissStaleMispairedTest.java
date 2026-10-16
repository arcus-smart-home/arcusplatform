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
package com.iris.platform.subsystem.pairing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.iris.common.subsystem.event.SubsystemLifecycleEvent;
import com.iris.messages.address.Address;
import com.iris.messages.capability.ButtonCapability;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.PairingDeviceCapability;
import com.iris.messages.capability.PairingDeviceCapability.DismissRequest;
import com.iris.messages.capability.PairingSubsystemCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.PairingDeviceModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.platform.pairing.PairingDevice;
import com.iris.platform.subsystem.pairing.state.PairingSubsystemTestCase;
import com.iris.prodcat.ProductCatalogEntry;

public class DismissStaleMispairedTest extends PairingSubsystemTestCase {

   private static final Map<String, String> PAIRING_FAILED =
         ImmutableMap.of(DeviceAdvancedCapability.PairingFailedException.CODE_PAIRING_FAILED, "Driver could not be determined.");

   private int nextSequenceId = 1;

   @Before
   public void expectPairingDeviceLoads() {
      // the pairing devices attribute loads each pairing device it adds
      EasyMock.expect(pairingDeviceDao.findByAddress(EasyMock.anyObject())).andReturn(null).anyTimes();
   }

   @Test
   public void testDismissesStaleMispairedOnStart() throws Exception {
      Model stale = addMispaired(addDevice(ImmutableMap.of()));
      replay();

      start();

      assertEquals(PairingSubsystemCapability.PAIRINGMODE_IDLE, context.model().getPairingMode());
      assertEquals(ImmutableList.of(stale.getAddress()), dismissed());
   }

   @Test
   public void testKeepsMispairedWhilePairingFailed() throws Exception {
      addMispaired(addDevice(PAIRING_FAILED));
      replay();

      start();

      assertEquals(PairingSubsystemCapability.PAIRINGMODE_IDLE, context.model().getPairingMode());
      assertEquals(ImmutableList.of(), dismissed());
   }

   @Test
   public void testKeepsMispairedWithoutDevice() throws Exception {
      addMispaired(Address.platformDriverAddress(UUID.randomUUID()));
      replay();

      start();

      assertEquals(PairingSubsystemCapability.PAIRINGMODE_IDLE, context.model().getPairingMode());
      assertEquals(ImmutableList.of(), dismissed());
   }

   @Test
   public void testKeepsStaleMispairedWhilePairing() throws Exception {
      ProductCatalogEntry product = productBuilder().zigbee().build();
      expectLoadProductAndReturn(product).anyTimes();
      expectCurrentProductAndReturn(product).anyTimes();
      replay();
      stageSearchingHub();
      addMispaired(addDevice(ImmutableMap.of()));
      requests.reset();
      requestAddresses.reset();

      // the subsystem restarts in the middle of pairing
      subsystem.onEvent(SubsystemLifecycleEvent.started(context.model().getAddress()), context);

      assertEquals(ImmutableList.of(), dismissed());
   }

   private Address addDevice(Map<String, String> errors) {
      Model device = addModel(
            ModelFixtures
               .buildDeviceAttributes(ButtonCapability.NAMESPACE)
               .put(DeviceAdvancedCapability.ATTR_DRIVERSTATE, DeviceAdvancedCapability.DRIVERSTATE_ACTIVE)
               .put(DeviceAdvancedCapability.ATTR_DRIVERNAME, "ZWTestDriver")
               .put(DeviceAdvancedCapability.ATTR_ERRORS, errors)
               .create()
      );
      return device.getAddress();
   }

   private Model addMispaired(Address deviceAddress) {
      PairingDevice pairingDevice = new PairingDevice();
      pairingDevice.setId(placeId, nextSequenceId++);
      PairingDeviceModel.setDeviceAddress(pairingDevice, deviceAddress.getRepresentation());
      PairingDeviceModel.setPairingState(pairingDevice, PairingDeviceCapability.PAIRINGSTATE_MISPAIRED);
      PairingDeviceModel.setPairingPhase(pairingDevice, PairingDeviceCapability.PAIRINGPHASE_FAILED);
      return addModel(pairingDevice.toMap());
   }

   private List<Address> dismissed() {
      List<Address> dismissed = new ArrayList<>();
      for(int i = 0; i < requests.getValues().size(); i++) {
         if(DismissRequest.NAME.equals(requests.getValues().get(i).getMessageType())) {
            dismissed.add(requestAddresses.getValues().get(i));
         }
      }
      return dismissed;
   }
}
