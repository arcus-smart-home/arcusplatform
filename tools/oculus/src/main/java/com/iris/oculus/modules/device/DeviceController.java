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
/**
 *
 */
package com.iris.oculus.modules.device;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.swing.Action;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.Utils;
import com.iris.capability.util.Addresses;
import com.iris.client.ClientEvent;
import com.iris.client.ClientRequest;
import com.iris.client.IrisClient;
import com.iris.client.capability.Account.ListDevicesResponse;
import com.iris.client.capability.Alert;
import com.iris.client.capability.Bridge;
import com.iris.client.capability.Contact;
import com.iris.client.capability.DayNightSensor;
import com.iris.client.capability.DeviceAdvanced;
import com.iris.client.capability.DeviceConnection;
import com.iris.client.capability.Dimmer;
import com.iris.client.capability.DoorLock;
import com.iris.client.capability.KeyPad;
import com.iris.client.capability.LeakH2O;
import com.iris.client.capability.Motion;
import com.iris.client.capability.MotorizedDoor;
import com.iris.client.capability.Place;
import com.iris.client.capability.Presence;
import com.iris.client.capability.Smoke;
import com.iris.client.capability.Switch;
import com.iris.client.capability.Thermostat;
import com.iris.client.capability.Valve;
import com.iris.client.capability.Vent;
import com.iris.client.event.ClientFuture;
import com.iris.client.event.Futures;
import com.iris.client.model.DeviceModel;
import com.iris.client.service.BridgeService;
import com.iris.messages.MessageConstants;
import com.iris.messages.address.Address;
import com.iris.messages.capability.DeviceCapability;
import com.iris.oculus.Oculus;
import com.iris.oculus.modules.BaseController;
import com.iris.oculus.modules.device.dialog.IpDeviceRegistrationPrompt;
import com.iris.oculus.modules.device.dialog.MockDevicePrompt;
import com.iris.oculus.util.Actions;
import com.iris.protocol.ipcd.IpcdProtocol;
import com.iris.protocol.mock.MockProtocol;
import com.iris.protocol.zigbee.ZigbeeProtocol;
import com.iris.protocol.zwave.ZWaveProtocol;


/**
 *
 */
public class DeviceController extends BaseController<DeviceModel> {
   private static Logger logger = LoggerFactory.getLogger(DeviceController.class);

   private IrisClient client;

   private Action createDevice = Actions.build("Create Mock Device", this::promptForCreateMockDevice);
   private Action registerIpDevice = Actions.build("Register IP Device", this::promptForRegisterIpDevice);
   
   @Inject
   public DeviceController(IrisClient client) {
      super(DeviceModel.class);
      this.client = client;
   }

   protected void promptForCreateMockDevice() {
      MockDevicePrompt.prompt().onSuccess(
            (mockCapability) -> createMockDevice(mockCapability)
               .onSuccess((event) -> Oculus.info("Created mock device: " + event))
               .onFailure((error) -> Oculus.error("Unable to create mock device", error)));
      // If it failed, no nothing.
   }

   protected void promptForRegisterIpDevice() {
      IpDeviceRegistrationPrompt.prompt().onSuccess(
               (ipDevice) -> registerIpDevice(ipDevice)
                  .onSuccess((event) -> Oculus.info("Registered ip device: " + event))
                  .onFailure((error) -> Oculus.error("Unable to register ip device", error)));
   }

   @Override
   protected ClientFuture<? extends Collection<Map<String, Object>>> doLoad() {
      String placeId = getPlaceId();
      if(placeId == null) {
         return Futures.failedFuture(new IllegalStateException("Can't load devices, no place selected"));
      }

      // TODO get account model reference
      ClientRequest request = new Place.ListDevicesRequest();
      request.setAddress(Addresses.toObjectAddress(Place.NAMESPACE, placeId));
      request.setTimeoutMs(30000);
      return
         client
            .request(request)
            .transform((response) -> new ListDevicesResponse(response).getDevices())
            .onSuccess((r) -> { if(r.isEmpty()) { Oculus.warn("No devices associated with the current account"); } });
   }

   @Override
   protected void onPlaceChanged(String newPlaceId) {
      super.onPlaceChanged(newPlaceId);
      reload();
   }

   public ClientFuture<ClientEvent> createMockDevice(String mockCapability) {
      UUID uuid = UUID.randomUUID();
      long msb = uuid.getMostSignificantBits();
      long lsb = uuid.getLeastSignificantBits();
      byte [] id = new byte[16];
      for(int i=0; i<8; i++) {
         id[i] = (byte)(msb & 0xff);
         id[i + 8] = (byte)(lsb & 0xff);
         lsb >>= 8;
         msb >>= 8;
      }

      List<List<String>> protocolAttributes = new ArrayList<List<String>>(1);
      protocolAttributes.add(Arrays.asList(
            MockProtocol.ATTR_CAPABILITY,
            String.class.getName(),
            mockCapability));


      ClientRequest request = new ClientRequest();
      // note there are a couple bugs around this:
      //  - the DeviceService.NAMESPACE is wrong
      //  -  Addresses.toServiceAddress is broken for 'dev' namespace
      request.setAddress("SERV:" + DeviceCapability.NAMESPACE + ":");
      request.setCommand(MessageConstants.MSG_ADD_DEVICE_REQUEST);
      request.setAttribute("protocolName", MockProtocol.NAMESPACE);
      request.setAttribute("deviceId", Utils.b64Encode(id));
      request.setAttribute("accountId", this.getSessionInfo().getAccountId());
      request.setAttribute("placeId", this.getPlaceId());
      request.setAttribute("protocolAttributes", protocolAttributes);
      request.setTimeoutMs(30000);
      return this.client.request(request);
   }

   public ClientFuture<ClientEvent> registerIpDevice(IpDevice ipDevice) {
      Map<String,String> idAttrs = new HashMap<>();
      idAttrs.put(IpcdProtocol.ATTR_V1DEVICETYPE, ipDevice.getDeviceType());
      idAttrs.put(IpcdProtocol.ATTR_SN, ipDevice.getSn());
      if (ipDevice.getModelCode() != null) {
         idAttrs.put(IpcdProtocol.ATTR_MODELCODE, ipDevice.getModelCode());
      }
      if (ipDevice.getSerialCode() != null) {
         idAttrs.put(IpcdProtocol.ATTR_SERIALCODE, ipDevice.getSerialCode());
      }

      BridgeService.RegisterDeviceRequest registerDeviceReq = new BridgeService.RegisterDeviceRequest();
      registerDeviceReq.setAttrs(idAttrs);
      registerDeviceReq.setAddress(Address.bridgeAddress(IpcdProtocol.NAMESPACE).getRepresentation());
      // Extra long time out since the water heater only polls every minute (give or take a few seconds).
      registerDeviceReq.setTimeoutMs(70000);

      return this.client.request(registerDeviceReq);
   }

   public Action actionCreateMockDevice() {
      return createDevice;
   }

   public Action actionRegisterIpDevice() {
      return registerIpDevice;
   }
   
   public static String getDriverInfo(DeviceModel model) {
      try {
         DeviceAdvanced advanced = (DeviceAdvanced) model;
         return advanced.getDrivername() + " v" + advanced.getDriverversion() + " (" + advanced.getDriverhash().substring(0, 7) + ")";
      }
      catch(Exception e) {
         Oculus.warn("Unable to determine driver version", e);
         return "unknown";
      }
   }

   public static String getDeviceState(DeviceModel model) {
      try {
         Object value = doGetDeviceState(model);
         if(value != null) {
            return String.valueOf(value);
         }
      }
      catch(Exception e) {
         Oculus.warn("Unable to load device state for device " + model.getName());
         logger.warn("Unable to load device state for device [{}]", model.getAddress());
      }
      return "Unknown";
   }

   public static String getProtocolId(DeviceModel model) {
      try {
         Object value = model.get(DeviceAdvanced.ATTR_PROTOCOLID);
         Object protocol = model.get(DeviceAdvanced.ATTR_PROTOCOL);
         if (!(protocol instanceof CharSequence)) {
            return "Unknown";
         }

         switch (String.valueOf(protocol)) {
         case ZWaveProtocol.NAMESPACE: {
            byte[] data = Base64.getDecoder().decode(String.valueOf(value));
            ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);

            int idx = buffer.remaining() - 1;
            while (idx >= 5) {
               if ((buffer.get(idx) & 0xFF) != 0) {
                  break;
               }

               idx--;
            }

            if (idx < 5) {
               return null;
            }

            byte nodeId = buffer.get(idx);
            int homeId = buffer.getInt(idx - 4);
            return toHex(homeId) + ":" + toZWave(nodeId);
         }

         case ZigbeeProtocol.NAMESPACE: {
            byte[] data = Base64.getDecoder().decode(String.valueOf(value));
            ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            if (buffer.remaining() < 8) {
               return null;
            }

            return toHex(buffer.getLong(0));
         }

         default:
            return null;
         }
      } catch(Exception e) {
         Oculus.warn("Unable to load device state for device " + model.getName());
         logger.warn("Unable to load device state for device [{}]", model.getAddress());
      }

      return "Unknown";
   }

   protected static String toZWave(byte value) {
      String result = String.valueOf(value & 0xFF);
      while (result.length() < 3) {
         result = "0" + result;
      }

      return result;
   }

   protected static String toHex(byte value) {
      if (value < 0x00) return Integer.toHexString(value & 0xFF).toUpperCase();
      if (value < 0x10) return "0" + Integer.toHexString(value & 0xFF).toUpperCase();
      return Integer.toHexString(value & 0xFF).toUpperCase();
   }

   protected static String toHex(short value) {
      if (value < 0x0000) return Integer.toHexString(value & 0xFFFF).toUpperCase();
      if (value < 0x0010) return "000" + Integer.toHexString(value & 0xFFFF).toUpperCase();
      if (value < 0x0100) return "00" + Integer.toHexString(value & 0xFFFF).toUpperCase();
      if (value < 0x1000) return "0" + Integer.toHexString(value & 0xFFFF).toUpperCase();
      return Integer.toHexString(value & 0xFFFF).toUpperCase();
   }

   protected static String toHex(int value) {
      if (value < 0x00000000) return Integer.toHexString(value).toUpperCase();
      if (value < 0x00000010) return "0000000" + Integer.toHexString(value).toUpperCase();
      if (value < 0x00000100) return "000000" + Integer.toHexString(value).toUpperCase();
      if (value < 0x00001000) return "00000" + Integer.toHexString(value).toUpperCase();
      if (value < 0x00010000) return "0000" + Integer.toHexString(value).toUpperCase();
      if (value < 0x00100000) return "000" + Integer.toHexString(value).toUpperCase();
      if (value < 0x01000000) return "00" + Integer.toHexString(value).toUpperCase();
      if (value < 0x10000000) return "0" + Integer.toHexString(value).toUpperCase();;
      return Integer.toHexString(value).toUpperCase();
   }

   protected static String toHex(long value) {
      if (value < 0x0000000000000000L) return Long.toHexString(value).toUpperCase();
      if (value < 0x0000000000000010L)  return "000000000000000" + Long.toHexString(value).toUpperCase();
      if (value < 0x0000000000000100L)  return "00000000000000" + Long.toHexString(value).toUpperCase();
      if (value < 0x0000000000001000L) return "0000000000000" + Long.toHexString(value).toUpperCase();
      if (value < 0x0000000000010000L) return "000000000000" + Long.toHexString(value).toUpperCase();
      if (value < 0x0000000000100000L) return "00000000000" + Long.toHexString(value).toUpperCase();
      if (value < 0x0000000001000000L) return "0000000000" + Long.toHexString(value).toUpperCase();
      if (value < 0x0000000010000000L) return "000000000" + Long.toHexString(value).toUpperCase();
      if (value < 0x0000000100000000L) return "00000000" + Long.toHexString(value).toUpperCase();
      if (value < 0x0000001000000000L) return "0000000" + Long.toHexString(value).toUpperCase();
      if (value < 0x0000010000000000L) return "000000" + Long.toHexString(value).toUpperCase();
      if (value < 0x0000100000000000L) return "00000" + Long.toHexString(value).toUpperCase();
      if (value < 0x0001000000000000L) return "0000" + Long.toHexString(value).toUpperCase();
      if (value < 0x0010000000000000L) return "000" + Long.toHexString(value).toUpperCase();
      if (value < 0x0100000000000000L) return "00" + Long.toHexString(value).toUpperCase();
      if (value < 0x1000000000000000L) return "0" + Long.toHexString(value).toUpperCase();
      return Long.toHexString(value).toUpperCase();
   }

   @Nullable
   private static Object doGetDeviceState(DeviceModel model) {
      // TODO make this pluggable
      if(DeviceConnection.STATE_OFFLINE.equals(model.get(DeviceConnection.ATTR_STATE))) {
         return DeviceConnection.STATE_OFFLINE;
      }
      if(model instanceof Alert) {
         return model.get(Alert.ATTR_STATE);
      }
      if(model instanceof Bridge) {
         return ((Bridge) model).getPairingState();
      }
      if(model instanceof Contact) {
         return model.get(Contact.ATTR_CONTACT);
      }
      if(model instanceof KeyPad) {
         return model.get(KeyPad.ATTR_ALARMSTATE);
      }
      if(model instanceof Motion) {
         return Motion.MOTION_DETECTED.equals(model.get(Motion.ATTR_MOTION)) ? "MOTION DETECTED" : "NO MOTION";
      }
      if(model instanceof Switch && model instanceof Dimmer) {
         return Switch.STATE_OFF.equals(model.get(Switch.ATTR_STATE)) ? Switch.STATE_OFF : model.get(Dimmer.ATTR_BRIGHTNESS);
      }
      if(model instanceof Switch) {
         return model.get(Switch.ATTR_STATE);
      }
      if(model instanceof Dimmer) {
         return model.get(Dimmer.ATTR_BRIGHTNESS);
      }
      if(model instanceof DayNightSensor) {
         return model.get(DayNightSensor.ATTR_MODE);
      }
      if(model instanceof Thermostat) {
         return model.get(Thermostat.ATTR_HVACMODE);
      }
      if(model instanceof MotorizedDoor) {
         return model.get (MotorizedDoor.ATTR_DOORSTATE);
      }
      if(model instanceof DoorLock) {
         return model.get (DoorLock.ATTR_LOCKSTATE);
      }
      if(model instanceof Vent) {
         return model.get (Vent.ATTR_LEVEL);
      }
      if(model instanceof Valve) {
         return model.get (Valve.ATTR_VALVESTATE);
      }
      if(model instanceof LeakH2O) {
         return model.get (LeakH2O.ATTR_STATE);
      }
      if(model instanceof Smoke) {
         return model.get (Smoke.ATTR_SMOKE);
      }
      if (model instanceof Presence) {
         return model.get (Presence.ATTR_PRESENCE);
      }

      return DeviceConnection.STATE_ONLINE;
   }

}

