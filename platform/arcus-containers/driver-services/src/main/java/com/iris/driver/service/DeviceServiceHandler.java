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
package com.iris.driver.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.HubDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.platform.AbstractPlatformService;
import com.iris.core.protocol.ipcd.IpcdDeviceDao;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.device.attributes.AttributeMap;
import com.iris.driver.platform.PlatformDriverService;
import com.iris.driver.service.DeviceService.CreateDeviceRequest;
import com.iris.io.json.JSON;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.HubAddress;
import com.iris.messages.address.HubServiceAddress;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.HubAdvancedCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Device;
import com.iris.messages.model.Hub;
import com.iris.messages.model.Person;
import com.iris.messages.services.PlatformConstants;
import com.iris.protocol.ipcd.IpcdProtocol;
import com.iris.util.IrisUUID;

@Singleton
public class DeviceServiceHandler extends AbstractPlatformService {
   private static final Logger logger = LoggerFactory.getLogger(DeviceServiceHandler.class);
	public static final String NAME = PlatformConstants.SERVICE_DEVICES;
    public static final String LEGACY_DEVICESYNCRESPONSE_NAMESPACE = "device:SyncDevicesResponse";
    public static final String LEGACY_DEVICESYNC_NAMESPACE = "device:SyncDevices";


    private final DeviceDAO deviceDao;
   private final HubDAO hubDao;
   private final IpcdDeviceDao ipcdDeviceDao;
   private final PersonDAO personDao;
   private final PersonPlaceAssocDAO personPlaceAssocDao;
	private final DeviceService service;
   private final PlatformDriverService driverService;
   private final int syncSizeWarning;

	@Inject
	public DeviceServiceHandler(
	      PlatformMessageBus platformBus,
	      DriverServiceConfig config,
	      DeviceDAO deviceDao,
	      HubDAO hubDao,
	      IpcdDeviceDao ipcdDeviceDao,
	      PersonDAO personDao,
	      PersonPlaceAssocDAO personPlaceAssocDao,
	      DeviceService service,
	      PlatformDriverService driverService
   ) {
	   super(platformBus, NAME, config.getMaxThreads(), config.getThreadKeepAliveMs());
		this.deviceDao = deviceDao;
		this.hubDao = hubDao;
		this.ipcdDeviceDao = ipcdDeviceDao;
		this.personDao = personDao;
		this.personPlaceAssocDao = personPlaceAssocDao;
		this.service = service;
		this.driverService = driverService;
		this.syncSizeWarning = config.getSyncSizeWarning();
	}

	@Override
    public MessageBody handleRequest(PlatformMessage message) throws Exception {
        MessageBody body = message.getValue();
        String type = body.getMessageType();
        switch (type) {
            case MessageConstants.MSG_ADD_DEVICE_REQUEST:
                return addDevice(body);
            case com.iris.messages.service.DeviceService.SyncDevicesRequest.NAME:
                return syncDevices(message, body);
            // TODO: Adding this case for i2-1932 this should be changed in the hub to use dev namespace and removed at some point in the fufutre
            case LEGACY_DEVICESYNC_NAMESPACE:
                MessageBody m = syncDevices(message, body);
                if (com.iris.messages.service.DeviceService.SyncDevicesResponse.NAME.equals(m.getMessageType()))
                    return MessageBody.buildMessage(LEGACY_DEVICESYNCRESPONSE_NAMESPACE, m.getAttributes());
                else
                    return m;
            default:
                return super.handleRequest(body);
        }
    }

   @Override
   protected void handleEvent(PlatformMessage message) throws Exception {
      MessageBody body = message.getValue();
      String type = body.getMessageType();
      switch (type) {
      case DeviceAdvancedCapability.RemovedDeviceEvent.NAME:
         handleRemovedDeviceEvent(message.getValue());
         break;
      case Capability.EVENT_DELETED:
         handleDeletedEvent(message);
         break;
      case HubAdvancedCapability.GetDeviceInfoResponse.NAME:
         addDevice(message.getValue());
         break;
		case com.iris.messages.service.DeviceService.DevicesDegradedEvent.NAME:
		   syncDegradedDevices(message, body);
		   break;
      default:
			super.handleEvent(message);
			break;
      }
   }

   public MessageBody addDevice(MessageBody request) {
	   CreateDeviceRequest createRequest = new CreateDeviceRequest(request);
	   try {
	      AttributeMap protocolAttributes = AttributeMap.newMap();
	      if(request.getAttributes().containsKey("protocolAttributes")) {
	         String protocolAttrs = JSON.toJson(request.getAttributes().get("protocolAttributes"));
	         protocolAttributes = JSON.fromJson(protocolAttrs, AttributeMap.class);
	      }
	      Device device = service.create(createRequest, protocolAttributes);
	      ImmutableMap.Builder<String, Object> response = ImmutableMap.builder();
	      if(device.getAccount() != null) {
	         response.put(MessageConstants.ATTR_ACCOUNTID, device.getAccount().toString());
	      }
	      response.put(MessageConstants.ATTR_DEVICEID, device.getId().toString());
	      response.put(MessageConstants.ATTR_DEVICEADDRESS, device.getAddress());
	      return
	            MessageBody.buildResponse(
	                  request,
	                  response.build()
               );
	   }
	   catch(Exception e) {
	      logger.warn("Error adding device", e);
	      return Errors.fromException(e);
	   }
	}

   public void handleRemovedDeviceEvent(MessageBody request) {
      String protocolName = DeviceAdvancedCapability.RemovedDeviceEvent.getProtocol(request);
      String protocolId = DeviceAdvancedCapability.RemovedDeviceEvent.getProtocolId(request);
      String hubId = DeviceAdvancedCapability.RemovedDeviceEvent.getHubId(request);
      String status = DeviceAdvancedCapability.RemovedDeviceEvent.getStatus(request);

      Address address = hubId == null ?
            Address.protocolAddress(protocolName, ProtocolDeviceId.fromRepresentation(protocolId)) :
            Address.hubProtocolAddress(hubId, protocolName, ProtocolDeviceId.fromRepresentation(protocolId));
      Device device = deviceDao.findByProtocolAddress(address.getRepresentation());
      if(device == null) {
         logger.debug("Received remove for unrecognized device [{}]", address);
         return;
      }

      if(device.isTombstoned()) {
         logger.debug("Received remove event for tombstoned device [{}], removing tombstone", address);
         deleteDevice(device);
         return; // we're done, this device was already a zombie
      }

      if(status == null) {
         logger.warn("Received device removed with no status, assuming clean removal and deleting device [{}]", address);
         status = "unknown";
      }

      switch(status) {
      case "unknown":
      case DeviceAdvancedCapability.RemovedDeviceEvent.STATUS_CLEAN:
      case DeviceAdvancedCapability.RemovedDeviceEvent.STATUS_FORCED:
         logger.info("Received [{}] remove event for device [{}], deleting from db", status, address);
         deleteDevice(device);
         break;

      case DeviceAdvancedCapability.RemovedDeviceEvent.STATUS_SPONTANEOUS:
      default:
         logger.info("Received [{}] remove event for device [{}], marking as lost", status, address);
         lostDevice(address);
         break;

      }
   }

   public MessageBody syncDevices(PlatformMessage msg, MessageBody request) {
      logger.trace("syncing reflexes with hub: {}", request);

      UUID place = IrisUUID.fromString(com.iris.messages.service.DeviceService.SyncDevicesRequest.getPlaceId(request));
      Errors.assertPlaceMatches(msg, place);

      Hub hub = hubDao.findHubForPlace(place);
      List<Device> devices = deviceDao.listDevicesByPlaceId(place, true);
      
      try {
         String reportedDevices = com.iris.messages.service.DeviceService.SyncDevicesRequest.getDevices(request);
         Integer reflexVersion = com.iris.messages.service.DeviceService.SyncDevicesRequest.getReflexVersion(request);
         List<Map<String,Object>> reportedStates = decompress(reportedDevices, List.class);

         try {
            Pair<List<Map<String,Object>>,List<JsonObject>> rsp = driverService.syncDevices(hub.getId(), place, reflexVersion, devices, reportedStates);
            List<Map<String,Object>> deviceStates = rsp.getLeft();
            List<JsonObject> driverReflexes = rsp.getRight();

            String syncDeviceStates = compress(deviceStates);
            String syncDriverReflexes = compress(driverReflexes);

            int size = syncDeviceStates.length() + syncDriverReflexes.length();
            if (size > syncSizeWarning) {
               logger.warn("sync device response contains {} bytes worth of device state and driver reflexes", size);
            }

            return com.iris.messages.service.DeviceService.SyncDevicesResponse.builder()
               .withPins(doGetAllPinHashes(place,personDao,personPlaceAssocDao))
               .withDevices(syncDeviceStates)
               .withDrivers(syncDriverReflexes)
               .build();
         } catch (Exception ex) {
            logger.warn("failed to sync devices:", ex);
            return Errors.fromException(ex);
         }
      } catch (Exception ex) {
         logger.warn("failed to sync devices:", ex);
	      return Errors.fromException(ex);
      }
   }

   private static Map<String,String> doGetAllPinHashes(UUID placeId, PersonDAO personDao, PersonPlaceAssocDAO personPlaceAssocDao) {
      if (placeId == null) {
         return ImmutableMap.of();
      }

      String placeUuid = IrisUUID.toString(placeId);
      ImmutableMap.Builder<String,String> result = ImmutableMap.builder();
      Set<UUID> peopleWithAccess = personPlaceAssocDao.findPersonIdsByPlace(placeId);
      for(UUID personId : peopleWithAccess) {
         try {
            Person person = personDao.findById(personId);
            String pin = person.getPinAtPlace(placeId);

            String personUuid = IrisUUID.toString(personId);

            result.put(personUuid, hashPin(placeUuid, pin));
         } catch (Exception ex) {
            logger.warn("could not get pin for person: {}", personId);
         }
      }

      return result.build();
   }

   private static final String hashPin(String uuid, String pin) {
      try {
         MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
         return Base64.getEncoder().encodeToString(sha1.digest((uuid + pin).getBytes(StandardCharsets.UTF_8)));
      } catch (Exception ex) {
         throw new RuntimeException(ex);
      }
   }

   public void syncDegradedDevices(PlatformMessage msg, MessageBody request) {
      logger.trace("syncing degraded devices from the hub: {}", request);

      UUID place = UUID.fromString(com.iris.messages.service.DeviceService.DevicesDegradedEvent.getPlaceId(request));
      Errors.assertPlaceMatches(msg, place);

      Hub hub = hubDao.findHubForPlace(place);
      List<Device> devices = deviceDao.listDevicesByPlaceId(place, true);
      
      try {
         String reportedDevices = com.iris.messages.service.DeviceService.DevicesDegradedEvent.getDevices(request);
         List<Map<String,Object>> reportedStates = decompress(reportedDevices, List.class);

         driverService.syncDegradedDevices(hub.getId(), place, devices, reportedStates);
      } catch (Exception ex) {
         logger.warn("failed to sync devices:", ex);
      }
   }

   private static <T> String compress(T value) throws IOException {
      String json = JSON.toJson(value);
      byte[] rsppayload = json.getBytes(StandardCharsets.UTF_8);
      
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (GZIPOutputStream os = new GZIPOutputStream(baos)) {
         os.write(rsppayload);
      }

      byte[] compressed = baos.toByteArray();
      return Base64.getEncoder().encodeToString(compressed);
   }

   private static <T> T decompress(String compressed, Class<T> type) throws IOException {
      T value;
      byte[] payload = Base64.getDecoder().decode(compressed);
      ByteArrayInputStream bais = new ByteArrayInputStream(payload);
      try (GZIPInputStream is = new GZIPInputStream(bais)) {
         value = JSON.fromJson(new InputStreamReader(is), type);
      }

      return value;
   }

   private void deleteDevice(Device device) {
      try {
         service.delete(device);
      }
      catch (Exception e) {
         logger.warn("Unable to delete device [{}]", device, e);
      }
   }

   private void lostDevice(Address address) {
      try {
         service.lostDevice(address);
      }
      catch(Exception e) {
         logger.warn("Unable to mark device [{}] as lost", address);
      }
   }

   private void handleDeletedEvent(PlatformMessage message) {
      Address source = message.getSource();
      if(source instanceof HubServiceAddress || source instanceof HubAddress) {
         onHubDeleted(source.getHubId());
      } else if(PlatformConstants.SERVICE_PLACES.equals(source.getGroup())) {
         onPlaceDeleted((UUID)source.getId());
      }
   }

   private void onHubDeleted(String hubId) {
      List<Device> devices = deviceDao.findByHubId(hubId, true);
      for(Device d : devices) {
         deleteDevice(d);
      }
   }

   private void onPlaceDeleted(UUID placeId) {
      List<Device> devices = deviceDao.listDevicesByPlaceId(placeId, true);
      for(Device d : devices) {
         // only delete those that are not associated with a hub
         if(StringUtils.isBlank(d.getHubId())) {
            deleteDevice(d);
            if(IpcdProtocol.NAMESPACE.equals(d.getProtocol())) {
               ipcdDeviceDao.delete(d.getProtocolAddress(), placeId);
            }
         }
      }
   }
}

