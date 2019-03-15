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
package com.iris.platform.services.ipcd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.platform.AbstractPlatformMessageListener;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.core.protocol.ipcd.IpcdDeviceDao;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.AddressMatchers;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.service.BridgeService;
import com.iris.messages.service.IpcdService.DeviceConnectedEvent;
import com.iris.messages.service.IpcdService.DeviceHeartBeatEvent;
import com.iris.messages.service.IpcdService.FindDeviceRequest;
import com.iris.messages.service.IpcdService.FindDeviceResponse;
import com.iris.messages.service.IpcdService.ForceUnregisterRequest;
import com.iris.messages.service.IpcdService.ForceUnregisterResponse;
import com.iris.messages.service.IpcdService.ListDeviceTypesRequest;
import com.iris.messages.service.IpcdService.ListDeviceTypesResponse;
import com.iris.messages.services.PlatformConstants;
import com.iris.messages.type.IpcdDeviceType;
import com.iris.platform.services.ipcd.registry.IpcdRegistry;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.protocol.ipcd.IpcdDevice;
import com.iris.protocol.ipcd.IpcdDeviceTypeRegistry;
import com.iris.protocol.ipcd.IpcdProtocol;
import com.iris.protocol.ipcd.message.model.Device;
import com.iris.util.IrisUUID;

@Singleton
public class IpcdService extends AbstractPlatformMessageListener {

   public static final String PROP_THREADPOOL = "service.ipcd.threadpool";

   private static final Logger logger = LoggerFactory.getLogger(IpcdService.class);
   private static final Address ADDRESS = Address.platformService(com.iris.messages.service.IpcdService.NAMESPACE);

   private final IpcdRegistry registry;
   private final PlatformMessageBus bus;
   private final IpcdDeviceDao ipcdDeviceDao;
   private final DeviceDAO devDao;
   private final BeanAttributesTransformer<com.iris.messages.model.Device> devTransformer;
   private final PlacePopulationCacheManager populationCacheMgr;

   @Inject
   public IpcdService(
         PlatformMessageBus platformBus,
         @Named(PROP_THREADPOOL) Executor executor,
         IpcdRegistry registry,
         IpcdDeviceDao ipcdDeviceDao,
         DeviceDAO devDao,
         BeanAttributesTransformer<com.iris.messages.model.Device> devTransformer,
         PlacePopulationCacheManager populationCacheMgr
   ) {
      super(platformBus, executor);
      this.registry = registry;
      this.bus = platformBus;
      this.ipcdDeviceDao = ipcdDeviceDao;
      this.devDao = devDao;
      this.devTransformer = devTransformer;
      this.populationCacheMgr = populationCacheMgr;
   }

   @Override
   protected void onStart() {
      listen();
   }

   protected void listen() {
      addListeners(
            AddressMatchers.equals(Address.broadcastAddress()),
            AddressMatchers.equals(ADDRESS),
            AddressMatchers.equals(Address.bridgeAddress(IpcdProtocol.NAMESPACE))
      );
   }

   @Override
   protected MessageBody handleRequest(PlatformMessage message) throws Exception {
      logger.trace("incoming platform message [{}]", message);
      switch(message.getMessageType()) {
      case BridgeService.RegisterDeviceRequest.NAME:
         return registry.registerDevice(message.getValue(), message.getPlaceId());
      case BridgeService.RemoveDeviceRequest.NAME:
         return registry.unregisterDevice(message.getValue(), message.getPlaceId());
      case ListDeviceTypesRequest.NAME:
         return handleListDeviceTypes();
      case FindDeviceRequest.NAME:
         return handleFindDevice(message.getValue());
      case ForceUnregisterRequest.NAME:
         return handleForceUnregister(message.getValue());
      default:
         return super.handleRequest(message);
      }
   }

   @Override
   protected void handleEvent(PlatformMessage message) throws Exception {
      logger.trace("incoming event [{}]", message);
      switch(message.getMessageType()) {
      case MessageConstants.MSG_ADD_DEVICE_RESPONSE:
         registry.onEvent(message);
         break;
      case MessageConstants.MSG_ERROR:
         if(Objects.equals(Address.platformService(PlatformConstants.SERVICE_DEVICES), message.getSource())) {
            registry.onErrorEvent(message);
         } else {
            super.handleEvent(message);
         }
         break;
      case DeviceConnectedEvent.NAME:
         registry.onConnected(message);
         break;
      // TODO:  This can be removed after 2.13.  During the 2.12 upgrade this needs to remain so heartbeats during the
      // rolling restart are handled by both this and IpcdHeartbeatListener.
      case DeviceHeartBeatEvent.NAME:
         registry.onHeartBeat(message);
         break;
      default:
         super.handleEvent(message);
         break;
      }
   }

   private MessageBody handleListDeviceTypes() {
      Collection<Map<String,Object>> typesRaw = IpcdDeviceTypeRegistry.INSTANCE.listTypesAsMaps();
      List<Map<String,Object>> types = new ArrayList<>(typesRaw);
      Collections.sort(types, (l,r) -> {
         String lVendor = (String)l.get(IpcdDeviceType.ATTR_VENDOR);
         String rVendor = (String)r.get(IpcdDeviceType.ATTR_VENDOR);
         int vCmp = Comparator.<String>naturalOrder().compare(lVendor,rVendor);
         if (vCmp != 0) {
            return vCmp;
         }

         String lModel = (String)l.get(IpcdDeviceType.ATTR_MODEL);
         String rModel = (String)r.get(IpcdDeviceType.ATTR_MODEL);
         return Comparator.<String>naturalOrder().compare(lModel, rModel);
      });

      return ListDeviceTypesResponse.builder()
            .withDeviceTypes(types)
            .build();
   }

   private MessageBody handleFindDevice(MessageBody body) {
      Map<String,Object> devTypeMap = FindDeviceRequest.getDeviceType(body);
      if(devTypeMap == null || devTypeMap.isEmpty()) {
         throw new ErrorEventException(Errors.missingParam(FindDeviceRequest.ATTR_DEVICETYPE));
      }
      IpcdDeviceType devType = new IpcdDeviceType(devTypeMap);
      if(StringUtils.isBlank(devType.getVendor())) {
         throw new ErrorEventException(Errors.missingParam(IpcdDeviceType.ATTR_VENDOR));
      }
      if(StringUtils.isBlank(devType.getModel())) {
         throw new ErrorEventException(Errors.missingParam(IpcdDeviceType.ATTR_MODEL));
      }
      String sn = FindDeviceRequest.getSn(body);
      if(StringUtils.isBlank(sn)) {
         throw new ErrorEventException(Errors.missingParam(FindDeviceRequest.ATTR_SN));
      }
      Device d = IpcdDeviceTypeRegistry.INSTANCE.createDeviceFromType(devType, sn);
      Address protocolAddress = IpcdProtocol.ipcdAddress(d);
      IpcdDevice ipcdDev = ipcdDeviceDao.findByProtocolAddress(protocolAddress);
      if(ipcdDev == null) {
         throw new ErrorEventException(Errors.notFound(protocolAddress));
      }
      com.iris.messages.model.Device dev = devDao.findByProtocolAddress(protocolAddress.getRepresentation());
      FindDeviceResponse.Builder b = FindDeviceResponse.builder()
            .withIpcdDevice(createIpcdDev(ipcdDev));
      if(dev != null) {
         b.withDevice(devTransformer.transform(dev));
      }
      return b.build();
   }

   private MessageBody handleForceUnregister(MessageBody body) {
      String protocolAddress = ForceUnregisterRequest.getProtocolAddress(body);
      if(StringUtils.isBlank(protocolAddress)) {
         throw new ErrorEventException(Errors.missingParam(ForceUnregisterRequest.ATTR_PROTOCOLADDRESS));
      }
      IpcdDevice device = ipcdDeviceDao.findByProtocolAddress(protocolAddress);
      if(device == null) {
         throw new ErrorEventException(Errors.notFound(Address.fromString(protocolAddress)));
      }
      if(IpcdDevice.RegistrationState.UNREGISTERED != device.getRegistrationState()) {
         com.iris.messages.model.Device dev = devDao.findByProtocolAddress(protocolAddress);
         if(dev != null) {
            forceRemove(dev); // already causes unregistration
         } else {
            registry.forceUnregister(device);
         }
      }
      return ForceUnregisterResponse.instance();
   }

   private void forceRemove(com.iris.messages.model.Device dev) {
      MessageBody body = DeviceCapability.ForceRemoveRequest.instance();
      PlatformMessage msg = PlatformMessage.buildRequest(
            body,
            ADDRESS,
            Address.fromString(dev.getAddress()))
            .withCorrelationId(IrisUUID.randomUUID().toString())
            .withPlaceId(dev.getPlace())
            .withPopulation(populationCacheMgr.getPopulationByPlaceId(dev.getPlace()))
            .create();
      bus.send(msg);
   }

   private Map<String, Object> createIpcdDev(IpcdDevice ipcdDev) {
      com.iris.messages.type.IpcdDevice d = new com.iris.messages.type.IpcdDevice();
      if(ipcdDev.getAccountId() != null) {
         d.setAccountId(ipcdDev.getAccountId().toString());
      }
      if(ipcdDev.getPlaceId() != null) {
         d.setPlaceId(ipcdDev.getPlaceId().toString());
      }
      d.setConnState(ipcdDev.getConnState() == null ? null : ipcdDev.getConnState().name());
      d.setCreated(ipcdDev.getCreated());
      d.setDriverAddress(ipcdDev.getDriverAddress());
      d.setFirmware(ipcdDev.getFirmware());
      d.setIpcdver(ipcdDev.getIpcdver());
      d.setLastConnected(ipcdDev.getLastConnected());
      d.setModel(ipcdDev.getModel());
      d.setModified(ipcdDev.getModified());
      d.setProtocolAddress(ipcdDev.getProtocolAddress());
      d.setRegistrationState(ipcdDev.getRegistrationState() == null ? null : ipcdDev.getRegistrationState().name());
      d.setSn(ipcdDev.getSn());
      d.setVendor(ipcdDev.getVendor());
      return d.toMap();
   }
}

