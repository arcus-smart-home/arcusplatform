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
package com.iris.platform.subsystem.placemonitor;

import java.util.Map;
import java.util.Optional;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.iris.common.subsystem.Subsystem;
import com.iris.common.subsystem.SubsystemTestCase;
import com.iris.core.template.TemplateService;
import com.iris.firmware.ota.DeviceOTAFirmwareResolver;
import com.iris.firmware.ota.DeviceOTAFirmwareResponse;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceOtaCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.PlaceMonitorSubsystemCapability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.dev.DeviceModel;
import com.iris.messages.model.dev.DeviceOtaModel;
import com.iris.messages.model.subs.PlaceMonitorSubsystemModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.messages.type.Population;
import com.iris.platform.subsystem.placemonitor.ota.DeviceOTAHandler;
import com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlertHandler;

public class TestPlaceMonitorDeviceOTA extends SubsystemTestCase<PlaceMonitorSubsystemModel> {
   
   private PlaceMonitorSubsystem subsystem = null;
   private DeviceOTAFirmwareResolver firmwareResolver=null;
   private DeviceOTAHandler handler=null;
   
   Model otaDevice = new SimpleModel(ModelFixtures.buildDeviceAttributes(DeviceOtaCapability.NAMESPACE).create());
   Model otaDevice2 = new SimpleModel(ModelFixtures.buildDeviceAttributes(DeviceOtaCapability.NAMESPACE).create());

   Map<String, Object> place = ModelFixtures.createPlaceAttributes();

   TemplateService templateService = EasyMock.createMock(TemplateService.class);
   
   @Override
   protected PlaceMonitorSubsystemModel createSubsystemModel() {
      Map<String, Object> attributes = ModelFixtures.createServiceAttributes(SubsystemCapability.NAMESPACE, PlaceMonitorSubsystemCapability.NAMESPACE);
      return new PlaceMonitorSubsystemModel(new SimpleModel(attributes));
   }

   @Override
   protected Subsystem<PlaceMonitorSubsystemModel> subsystem() {
      return subsystem;
   }

   @Before
   public void init() throws Exception {
      firmwareResolver=EasyMock.createNiceMock(DeviceOTAFirmwareResolver.class);
      subsystem = new PlaceMonitorSubsystem(ImmutableMap.<String, PlaceMonitorHandler>of(),new PlaceMonitorNotifications(null), templateService);
      handler = new DeviceOTAHandler(firmwareResolver);
      subsystem.handlers = ImmutableMap.<String, PlaceMonitorHandler>of(DeviceOTAHandler.class.getName(), handler);

      placeModel.setAttribute(PlaceCapability.ATTR_POPULATION,Population.NAME_GENERAL);
      placeModel.setAttribute(PlaceCapability.ATTR_ID,placeId.toString());
      placeModel.setAttribute(PlaceCapability.ATTR_ADDRESS,Address.platformService(placeId.toString(), PlaceCapability.NAMESPACE).getRepresentation());

      init(subsystem);
   }

   @Test
   public void testOnOTADeviceAdded() {
      DeviceModel.setProductId(otaDevice, "abd1");
      DeviceOtaModel.setCurrentVersion(otaDevice,"1.1");

      EasyMock.expect(firmwareResolver.resolve(Optional.of(Population.NAME_GENERAL), DeviceModel.getProductId(otaDevice), DeviceOtaModel.getCurrentVersion(otaDevice)))
         .andReturn(new DeviceOTAFirmwareResponse(true,"1.2","/somepath",5,5)).anyTimes();
      EasyMock.replay(firmwareResolver);

      store.addModel(otaDevice2.toMap());
      store.addModel(otaDevice.toMap());
      
      assertTrue("should contain device", !model.getUpdatedDevices().contains(otaDevice2.getAttribute(Capability.ATTR_ADDRESS)));
      
      assertTrue("should contain device", model.getUpdatedDevices().contains(otaDevice.getAttribute(Capability.ATTR_ADDRESS)));
      MessageBody message = requests.getValue();
      assertEquals("should send ota request", DeviceOtaCapability.FirmwareUpdateRequest.NAME,message.getMessageType());
      assertEquals("should send ota request", "/somepath",DeviceOtaCapability.FirmwareUpdateRequest.getUrl(message));

      store.removeModel(otaDevice2.getAddress());
      assertFalse("should not contain device", model.getUpdatedDevices().contains(otaDevice2.getAttribute(Capability.ATTR_ADDRESS)));
   }

   @Test
   public void testOnFailDoRetry() {
      DeviceModel.setProductId(otaDevice, "abd1");
      DeviceOtaModel.setCurrentVersion(otaDevice,"1.1");
      store.updateModel(otaDevice.getAddress(), otaDevice.toMap());
      
      EasyMock.expect(firmwareResolver.resolve(Optional.of(Population.NAME_GENERAL), DeviceModel.getProductId(otaDevice), DeviceOtaModel.getCurrentVersion(otaDevice)))
      .andReturn(new DeviceOTAFirmwareResponse(true,"1.2","/somepath",5,5)).anyTimes();
      EasyMock.replay(firmwareResolver);


      DeviceOtaModel.setStatus(otaDevice, DeviceOtaCapability.STATUS_FAILED);
      store.updateModel(otaDevice.getAddress(), otaDevice.toMap());
      //TODO:fix this test case
      handler.onStatusChange(otaDevice, context);
      
      //Date timeout = SubsystemUtils.getTimeout(context,DeviceOTAHandler.FIRMWARE_RETRY_TIMEOUT_KEY).get();
      //assertNotNull("should have given a timeout",timeout.getTime());
      //timeout(DeviceOTAHandler.FIRMWARE_RETRY_TIMEOUT_KEY);
      
      //MessageBody message = requests.getValue();
      //assertEquals("should send ota request", DeviceOtaCapability.FirmwareUpdateRequest.NAME,message.getMessageType());

   }

}

