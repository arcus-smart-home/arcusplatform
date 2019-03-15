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
package com.iris.platform.services.ipcd.registry;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.protocol.ProtocolMessageBus;
import com.iris.core.protocol.ipcd.IpcdDeviceDao;
import com.iris.core.protocol.ipcd.exceptions.DeviceNotFoundException;
import com.iris.core.protocol.ipcd.exceptions.PlaceMismatchException;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.DeviceProtocolAddress;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Fixtures;
import com.iris.messages.model.Place;
import com.iris.messages.service.BridgeService;
import com.iris.platform.partition.PartitionConfig;
import com.iris.platform.partition.Partitioner;
import com.iris.platform.partition.PlatformPartition;
import com.iris.platform.partition.simple.SimplePartitioner;
import com.iris.platform.services.ipcd.IpcdService;
import com.iris.protocol.ipcd.IpcdProtocol;
import com.iris.test.IrisTestCase;
import org.easymock.EasyMock;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;

import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import static org.easymock.EasyMock.*;

@Mocks({ExecutorService.class, Clock.class, PlaceDAO.class, IpcdDeviceDao.class, DeviceDAO.class, PlatformBusClient.class, ProtocolMessageBus.class})
public class TestIpcdRegistry extends IrisMockTestCase {

   @Inject
   private ExecutorService mockExecutorService;

   @Provides @Singleton @Named(IpcdService.PROP_THREADPOOL)
   public  Executor getExecutor() {
      return mockExecutorService;
   }

   @Inject
   private Clock mockClock;

   @Inject
   private PlaceDAO mockPlaceDao;

   @Inject
   private IpcdDeviceDao mockIpcdDeviceDao;

   @Inject
   private DeviceDAO mockDeviceDao;

   @Inject
   private PlatformBusClient mockPlatformBusClient;

   @Inject
   private ProtocolMessageBus mockProtocolMessageBus;

   @Provides @Singleton
   private Partitioner getPartitioner() { return new SimplePartitioner(new PartitionConfig(), Optional.absent()); }

   @Inject
   private IpcdRegistry ipcdRegistry;

   @Test
   public void TestRegisterDevice_HappyPath() {
      Place place = Fixtures.createPlace();
      place.setId(UUID.randomUUID());
      expect(mockPlaceDao.findById(place.getId())).andReturn(place);

      DeviceProtocolAddress dpa = Fixtures.createProtocolAddress("IPCD");
      expect(mockIpcdDeviceDao
            .claimAndGetProtocolAddress(anyObject(com.iris.protocol.ipcd.message.model.Device.class), anyObject(UUID.class), eq(place.getId())))
            .andReturn(dpa.getRepresentation());

      final SettableFuture<PlatformMessage> future = SettableFuture.create();
      expect(mockPlatformBusClient.request(anyObject(PlatformMessage.class), anyInt())).andReturn(future);

      mockPlatformBusClient.sendEvent(anyObject(PlatformMessage.class));
      EasyMock.expectLastCall().anyTimes();

      EasyMock.replay(mockPlaceDao, mockIpcdDeviceDao, mockPlatformBusClient);

      MessageBody mb = BridgeService.RegisterDeviceRequest.builder()
            .withAttrs(
               ImmutableMap.of(
                  IpcdProtocol.ATTR_SN, "123456",
                  IpcdProtocol.ATTR_V1DEVICETYPE, IpcdProtocol.V1_DEVICE_TYPE_ECOWATER_SOFTENER
               )
            )
            .build();

      MessageBody result = ipcdRegistry.registerDevice(mb, place.getId().toString());
      assertNotNull(result);
      assertNotEquals("Error", result.getMessageType());
   }

   @Test
   public void TestRegisterDevice_DeviceNotFound() {
      Place place = Fixtures.createPlace();
      place.setId(UUID.randomUUID());
      expect(mockPlaceDao.findById(place.getId())).andReturn(place);

      DeviceProtocolAddress dpa = Fixtures.createProtocolAddress("IPCD");
      expect(mockIpcdDeviceDao
            .claimAndGetProtocolAddress(anyObject(com.iris.protocol.ipcd.message.model.Device.class), anyObject(UUID.class), eq(place.getId())))
            .andThrow(new DeviceNotFoundException(dpa.getRepresentation()));

      EasyMock.replay(mockPlaceDao, mockIpcdDeviceDao);

      MessageBody mb = BridgeService.RegisterDeviceRequest.builder()
            .withAttrs(
                  ImmutableMap.of(
                        IpcdProtocol.ATTR_SN, "123456",
                        IpcdProtocol.ATTR_V1DEVICETYPE, IpcdProtocol.V1_DEVICE_TYPE_ECOWATER_SOFTENER
                  )
            )
            .build();

      MessageBody result = ipcdRegistry.registerDevice(mb, place.getId().toString());
      assertNotNull(result);
      assertEquals("Error", result.getMessageType());
      assertEquals(Errors.CODE_NOT_FOUND, result.getAttributes().get("code"));
   }

   @Test
   public void TestRegisterDevice_PlaceMismatchException() {
      Place place = Fixtures.createPlace();
      place.setId(UUID.randomUUID());
      expect(mockPlaceDao.findById(place.getId())).andReturn(place);

      DeviceProtocolAddress dpa = Fixtures.createProtocolAddress("IPCD");
      expect(mockIpcdDeviceDao
            .claimAndGetProtocolAddress(anyObject(com.iris.protocol.ipcd.message.model.Device.class), anyObject(UUID.class), eq(place.getId())))
            .andThrow(new PlaceMismatchException(place.getId(), UUID.randomUUID()));

      EasyMock.replay(mockPlaceDao, mockIpcdDeviceDao);

      MessageBody mb = BridgeService.RegisterDeviceRequest.builder()
            .withAttrs(
                  ImmutableMap.of(
                        IpcdProtocol.ATTR_SN, "123456",
                        IpcdProtocol.ATTR_V1DEVICETYPE, IpcdProtocol.V1_DEVICE_TYPE_ECOWATER_SOFTENER
                  )
            )
            .build();

      MessageBody result = ipcdRegistry.registerDevice(mb, place.getId().toString());
      assertNotNull(result);
      assertEquals("Error", result.getMessageType());
      assertEquals(Errors.CODE_INVALID_REQUEST, result.getAttributes().get("code"));
   }

   @Test
   public void TestRegisterDevice_AlreadyRegisteredAtSamePlace() {
      Place place = Fixtures.createPlace();
      place.setId(UUID.randomUUID());
      expect(mockPlaceDao.findById(place.getId())).andReturn(place);

      DeviceProtocolAddress dpa = Fixtures.createProtocolAddress("IPCD");
      expect(mockIpcdDeviceDao
            .claimAndGetProtocolAddress(anyObject(com.iris.protocol.ipcd.message.model.Device.class), anyObject(UUID.class), eq(place.getId())))
            .andThrow(new PlaceMismatchException(place.getId(), place.getId()));


      final SettableFuture<PlatformMessage> future = SettableFuture.create();
      expect(mockPlatformBusClient.request(anyObject(PlatformMessage.class), anyInt())).andReturn(future);

      mockPlatformBusClient.sendEvent(anyObject(PlatformMessage.class));
      EasyMock.expectLastCall().anyTimes();

      EasyMock.replay(mockPlaceDao, mockIpcdDeviceDao, mockPlatformBusClient);

      MessageBody mb = BridgeService.RegisterDeviceRequest.builder()
            .withAttrs(
                  ImmutableMap.of(
                        IpcdProtocol.ATTR_SN, "123456",
                        IpcdProtocol.ATTR_V1DEVICETYPE, IpcdProtocol.V1_DEVICE_TYPE_ECOWATER_SOFTENER
                  )
            )
            .build();

      MessageBody result = ipcdRegistry.registerDevice(mb, place.getId().toString());
      assertNotNull(result);
      assertNotEquals("Error", result.getMessageType());
   }

   @Test
   public void TestRegisterDevice_TwoModelsOneNotFound() {
      Place place = Fixtures.createPlace();
      place.setId(UUID.randomUUID());
      expect(mockPlaceDao.findById(place.getId())).andReturn(place);

      DeviceProtocolAddress dpa = Fixtures.createProtocolAddress("IPCD");
      expect(mockIpcdDeviceDao
            .claimAndGetProtocolAddress(anyObject(com.iris.protocol.ipcd.message.model.Device.class), anyObject(UUID.class), eq(place.getId())))
            .andThrow(new DeviceNotFoundException(dpa.getRepresentation()))
            .once();

      expect(mockIpcdDeviceDao
            .claimAndGetProtocolAddress(anyObject(com.iris.protocol.ipcd.message.model.Device.class), anyObject(UUID.class), eq(place.getId())))
            .andReturn(dpa.getRepresentation())
            .once();

      final SettableFuture<PlatformMessage> future = SettableFuture.create();
      expect(mockPlatformBusClient.request(anyObject(PlatformMessage.class), anyInt())).andReturn(future);

      mockPlatformBusClient.sendEvent(anyObject(PlatformMessage.class));
      EasyMock.expectLastCall().anyTimes();

      EasyMock.replay(mockPlaceDao, mockIpcdDeviceDao, mockPlatformBusClient);

      MessageBody mb = BridgeService.RegisterDeviceRequest.builder()
            .withAttrs(
                  ImmutableMap.of(
                        IpcdProtocol.ATTR_SN, "123456",
                        IpcdProtocol.ATTR_V1DEVICETYPE, IpcdProtocol.V1_DEVICE_TYPE_AOSMITH_WATER_HEATER
                  )
            )
            .build();

      MessageBody result = ipcdRegistry.registerDevice(mb, place.getId().toString());
      assertNotNull(result);
      assertNotEquals("Error", result.getMessageType());
   }

   @Test
   public void TestRegisterDevice_TwoModelsOneRegisteredToAnotherPlace() {
      Place place = Fixtures.createPlace();
      place.setId(UUID.randomUUID());
      expect(mockPlaceDao.findById(place.getId())).andReturn(place);

      DeviceProtocolAddress dpa = Fixtures.createProtocolAddress("IPCD");
      expect(mockIpcdDeviceDao
            .claimAndGetProtocolAddress(anyObject(com.iris.protocol.ipcd.message.model.Device.class), anyObject(UUID.class), eq(place.getId())))
            .andThrow(new PlaceMismatchException(place.getId(), UUID.randomUUID()))
            .once();

      expect(mockIpcdDeviceDao
            .claimAndGetProtocolAddress(anyObject(com.iris.protocol.ipcd.message.model.Device.class), anyObject(UUID.class), eq(place.getId())))
            .andReturn(dpa.getRepresentation())
            .once();

      final SettableFuture<PlatformMessage> future = SettableFuture.create();
      expect(mockPlatformBusClient.request(anyObject(PlatformMessage.class), anyInt())).andReturn(future);

      mockPlatformBusClient.sendEvent(anyObject(PlatformMessage.class));
      EasyMock.expectLastCall().anyTimes();

      EasyMock.replay(mockPlaceDao, mockIpcdDeviceDao, mockPlatformBusClient);

      MessageBody mb = BridgeService.RegisterDeviceRequest.builder()
            .withAttrs(
                  ImmutableMap.of(
                        IpcdProtocol.ATTR_SN, "123456",
                        IpcdProtocol.ATTR_V1DEVICETYPE, IpcdProtocol.V1_DEVICE_TYPE_AOSMITH_WATER_HEATER
                  )
            )
            .build();

      MessageBody result = ipcdRegistry.registerDevice(mb, place.getId().toString());
      assertNotNull(result);
      assertNotEquals("Error", result.getMessageType());
   }

   @Test
   public void TestRegisterDevice_TwoModelsDeviceNotFound() {
      Place place = Fixtures.createPlace();
      place.setId(UUID.randomUUID());
      expect(mockPlaceDao.findById(place.getId())).andReturn(place);

      DeviceProtocolAddress dpa = Fixtures.createProtocolAddress("IPCD");
      expect(mockIpcdDeviceDao
            .claimAndGetProtocolAddress(anyObject(com.iris.protocol.ipcd.message.model.Device.class), anyObject(UUID.class), eq(place.getId())))
            .andThrow(new DeviceNotFoundException(dpa.getRepresentation()))
            .times(2);

      EasyMock.replay(mockPlaceDao, mockIpcdDeviceDao);

      MessageBody mb = BridgeService.RegisterDeviceRequest.builder()
            .withAttrs(
                  ImmutableMap.of(
                        IpcdProtocol.ATTR_SN, "123456",
                        IpcdProtocol.ATTR_V1DEVICETYPE, IpcdProtocol.V1_DEVICE_TYPE_AOSMITH_WATER_HEATER
                  )
            )
            .build();

      MessageBody result = ipcdRegistry.registerDevice(mb, place.getId().toString());
      assertNotNull(result);
      assertEquals("Error", result.getMessageType());
      assertEquals(Errors.CODE_NOT_FOUND, result.getAttributes().get("code"));
   }

   @Test
   public void TestRegisterDevice_TwoModelsPlaceMismatched() {
      Place place = Fixtures.createPlace();
      place.setId(UUID.randomUUID());
      expect(mockPlaceDao.findById(place.getId())).andReturn(place);

      DeviceProtocolAddress dpa = Fixtures.createProtocolAddress("IPCD");
      expect(mockIpcdDeviceDao
            .claimAndGetProtocolAddress(anyObject(com.iris.protocol.ipcd.message.model.Device.class), anyObject(UUID.class), eq(place.getId())))
            .andThrow(new PlaceMismatchException(place.getId(), UUID.randomUUID()))
            .times(2);

      EasyMock.replay(mockPlaceDao, mockIpcdDeviceDao);

      MessageBody mb = BridgeService.RegisterDeviceRequest.builder()
            .withAttrs(
                  ImmutableMap.of(
                        IpcdProtocol.ATTR_SN, "123456",
                        IpcdProtocol.ATTR_V1DEVICETYPE, IpcdProtocol.V1_DEVICE_TYPE_AOSMITH_WATER_HEATER
                  )
            )
            .build();

      MessageBody result = ipcdRegistry.registerDevice(mb, place.getId().toString());
      assertEquals("Error", result.getMessageType());
      assertEquals(Errors.CODE_INVALID_REQUEST, result.getAttributes().get("code"));
   }

   @Test
   public void TestRegisterDevice_TwoModelsPlaceMismatchedAndNotFound_NotFoundTakesPrecedence() {
      Place place = Fixtures.createPlace();
      place.setId(UUID.randomUUID());
      expect(mockPlaceDao.findById(place.getId())).andReturn(place);

      DeviceProtocolAddress dpa = Fixtures.createProtocolAddress("IPCD");
      expect(mockIpcdDeviceDao
            .claimAndGetProtocolAddress(anyObject(com.iris.protocol.ipcd.message.model.Device.class), anyObject(UUID.class), eq(place.getId())))
            .andThrow(new PlaceMismatchException(place.getId(), UUID.randomUUID()))
            .once();

      expect(mockIpcdDeviceDao
            .claimAndGetProtocolAddress(anyObject(com.iris.protocol.ipcd.message.model.Device.class), anyObject(UUID.class), eq(place.getId())))
            .andThrow(new DeviceNotFoundException(dpa.getRepresentation()))
            .once();

      EasyMock.replay(mockPlaceDao, mockIpcdDeviceDao);

      MessageBody mb = BridgeService.RegisterDeviceRequest.builder()
            .withAttrs(
                  ImmutableMap.of(
                        IpcdProtocol.ATTR_SN, "123456",
                        IpcdProtocol.ATTR_V1DEVICETYPE, IpcdProtocol.V1_DEVICE_TYPE_AOSMITH_WATER_HEATER
                  )
            )
            .build();

      MessageBody result = ipcdRegistry.registerDevice(mb, place.getId().toString());
      assertEquals("Error", result.getMessageType());
      assertEquals(Errors.CODE_NOT_FOUND, result.getAttributes().get("code"));
   }
}

