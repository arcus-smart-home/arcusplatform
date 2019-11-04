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
package com.iris.agent.controller.hub;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.math.NumberUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.iris.agent.hal.IrisHal;
import com.iris.agent.router.Port;
import com.iris.agent.router.PortHandler;
import com.iris.agent.storage.FileContentListener;
import com.iris.agent.storage.FileContentMonitor;
import com.iris.agent.storage.StorageService;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.HubWiFiCapability;
import com.iris.protocol.ProtocolMessage;

enum WirelessHandler implements PortHandler {
   INSTANCE;

   private static final Logger log = LoggerFactory.getLogger(WirelessHandler.class);
   private Port port;
   private FileContentMonitor wifiConnectionStatus = null;

   private static int MAX_SSID_SIZE = 32;
   private static int MAX_WPA_KEY_SIZE = 64;
   private static int MIN_WPA_KEY_SIZE = 8;
   private static int WEP_64_ASCII_SIZE = 5;
   private static int WEP_64_HEX_SIZE = 10;
   private static int WEP_128_ASCII_SIZE = 13;
   private static int WEP_128_HEX_SIZE = 26;
   private static final String WIFI_CONN_STATUS_FILE = "tmp:///wifiTestResult";

   // Translate connection status from hubOS decimal value
   //  0 - OK
   //  1 - Bad password
   //  2 - No IP address
   //  3 - Unable to connect to Internet
   private static final Map<Integer,String> connectStatus = new ImmutableMap.Builder<Integer,String>()
         .put(0, HubWiFiCapability.WiFiConnectResultEvent.STATUS_OK)
         .put(1, HubWiFiCapability.WiFiConnectResultEvent.STATUS_BAD_PASSWD)
         .put(2, HubWiFiCapability.WiFiConnectResultEvent.STATUS_NO_ADDR)
         .put(3, HubWiFiCapability.WiFiConnectResultEvent.STATUS_NO_INTERNET)
         .build();
   private static final Map<Integer,String> connectMessage = new ImmutableMap.Builder<Integer,String>()
         .put(0, "Connection succeeded")
         .put(1, "Bad wireless password")
         .put(2, "No IP address")
         .put(3, "No Internet connection")
         .build();

   void start(Port parent) {
      this.port = parent;
      parent.delegate(
         this,
         HubWiFiCapability.WiFiConnectRequest.NAME,
         HubWiFiCapability.WiFiDisconnectRequest.NAME,
         HubWiFiCapability.WiFiStartScanRequest.NAME,
         HubWiFiCapability.WiFiEndScanRequest.NAME
      );
   }

   @Nullable
   @Override
   public Object recv(Port port, PlatformMessage message) throws Exception {
      String type = message.getMessageType();
      switch (type) {
      case HubWiFiCapability.WiFiConnectRequest.NAME:
         return handleConnectRequest(message);

      case HubWiFiCapability.WiFiDisconnectRequest.NAME:
         return handleDisconnectRequest(message);

      case HubWiFiCapability.WiFiStartScanRequest.NAME:
         return handleStartScanRequest(message);

      case HubWiFiCapability.WiFiEndScanRequest.NAME:
         return handleEndScanRequest(message);

      default:
         return null;
      }
   }

   @Override
   public void recv(Port port, ProtocolMessage message) {
   }

   @Override
   public void recv(Port port, Object message) {
   }

   private final class WifiConnectionMonitor implements FileContentListener {
      private String lastResult = null;

      @Override
      public void fileContentsModified(FileContentMonitor monitor) {
         // Send an event when the connection status has been updated
         String result = monitor.getContents().trim();

         // An empty status is returned when the test is started to make sure the agent catches the later change
         if ((result != null) && !result.isEmpty() && ((lastResult == null) || !result.contentEquals(lastResult))) {
            lastResult = result;

            // The hubOS will return a 0-3 value for the status, convert
            Integer value = Integer.parseInt(result);
            String message = connectMessage.get(value);
            String status = connectStatus.get(value);

            log.debug("Returning WifiConnectResult: {}", status);

            // Send out connect event with details of attempt
            MessageBody msg = HubWiFiCapability.WiFiConnectResultEvent.builder()
                  .withMessage(message)
                  .withStatus(status)
                  .build();
            port.sendEvent(msg);

            // Once we have finished with the connection attempt, there is no point in monitoring the
            //  wifi status file
            if (wifiConnectionStatus != null) {
               wifiConnectionStatus.cancel();
               wifiConnectionStatus = null;
            }
         }
      }
   }

   private boolean validKey(String security, String key) {
      boolean valid = true;

      switch (security) {
      case HubWiFiCapability.WIFISECURITY_NONE:
      default:
         break;

      case HubWiFiCapability.WIFISECURITY_WEP:
         // Valid lengths depend on ASCII or hex - only support 64/128 bit values
         if (NumberUtils.isCreatable(key)) {
            if ((key.length() != WEP_64_HEX_SIZE) && (key.length() != WEP_128_HEX_SIZE)) {
               valid = false;
            }
         } else {
            if ((key.length() != WEP_64_ASCII_SIZE) && (key.length() != WEP_128_ASCII_SIZE)) {
               valid = false;
            }
         }
         break;

      case HubWiFiCapability.WIFISECURITY_WPA_PSK:
      case HubWiFiCapability.WIFISECURITY_WPA2_PSK:
         if ((key.length() < MIN_WPA_KEY_SIZE) || (key.length() > MAX_WPA_KEY_SIZE)) {
            valid = false;
         }
         break;
      }
      return valid;
   }

   private Object handleConnectRequest(PlatformMessage message) {
      log.trace("handling wireless connect request: {}", message);
      String ssid = (String) message.getValue().getAttributes().get(HubWiFiCapability.WiFiConnectRequest.ATTR_SSID);
      String bssid = (String) message.getValue().getAttributes().get(HubWiFiCapability.WiFiConnectRequest.ATTR_BSSID);
      String security = (String) message.getValue().getAttributes().get(HubWiFiCapability.WiFiConnectRequest.ATTR_SECURITY);
      String key = (String) message.getValue().getAttributes().get(HubWiFiCapability.WiFiConnectRequest.ATTR_KEY);
      String msg = "";
      String status = HubWiFiCapability.WiFiConnectResponse.STATUS_CONNECTING;

      // Valid ssid?
      if (ssid.length() > MAX_SSID_SIZE) {
         status = HubWiFiCapability.WiFiConnectResponse.STATUS_INVALID_SSID;
         msg = "SSID is too large";
      } else if (!security.contentEquals(HubWiFiCapability.WIFISECURITY_NONE) &&
            !security.contentEquals(HubWiFiCapability.WIFISECURITY_WEP) &&
            !security.contentEquals(HubWiFiCapability.WIFISECURITY_WPA_PSK) &&
            !security.contentEquals(HubWiFiCapability.WIFISECURITY_WPA2_PSK)) {
         // We don't support the enterprise security mode...
         status = HubWiFiCapability.WiFiConnectResponse.STATUS_INVALID_SECURITY;
         msg = "Invalid security mode";
      } else if (!validKey(security, key)) {
         // Valid key size/format depends on mode
         status = HubWiFiCapability.WiFiConnectResponse.STATUS_INVALID_KEY;
         msg = "Invalid key";
      } else {
         // Monitor the wifi status file so we get feedback once a connection is started
         try {
            this.wifiConnectionStatus = StorageService.getFileMonitor(URI.create(WIFI_CONN_STATUS_FILE));
         } catch (IOException e) {
            log.debug("Cannot monitor '{}' because: [{}]", WIFI_CONN_STATUS_FILE, e);
         }
         if (this.wifiConnectionStatus != null) {
            this.wifiConnectionStatus.addListener(new WifiConnectionMonitor());
         }

         // Try to connect
         try {
            IrisHal.wirelessConnect(ssid, bssid, security, key);
         } catch (IOException ex) {
            status = HubWiFiCapability.WiFiConnectResponse.STATUS_REFUSED;
            msg = ex.getMessage();
         }
      }
      return HubWiFiCapability.WiFiConnectResponse.builder()
               .withStatus(status)
               .withMessage(msg)
               .build();
   }

   private Object handleDisconnectRequest(PlatformMessage message) {
      log.trace("handling wireless disconnect request: {}", message);
      String msg = "";
      String status = HubWiFiCapability.WiFiDisconnectResponse.STATUS_OK;
      try {
         IrisHal.wirelessDisconnect();
      } catch (IOException ex) {
         status = HubWiFiCapability.WiFiDisconnectResponse.STATUS_REFUSED;
         msg = ex.getMessage();
      }
      return HubWiFiCapability.WiFiDisconnectResponse.builder()
               .withStatus(status)
               .withMessage(msg)
               .build();
   }

   private Object handleStartScanRequest(PlatformMessage message) {
      log.trace("handling wireless start scan request: {}", message);
      Integer timeout = ((Number)message.getValue().getAttributes().get(HubWiFiCapability.WiFiStartScanRequest.ATTR_TIMEOUT)).intValue();

      // Runs in a separate thread because the scan can take a while
      Thread thr = new Thread(new Runnable() {
         @Override
         public void run() {
            List<Map<String, Object>> results = null;
            String msg = "";
            try {
               results = IrisHal.wirelessScanStart(timeout);
            } catch (IOException e) {
               msg = e.getMessage();
            } finally {
               MessageBody message = HubWiFiCapability.WiFiScanResultsEvent.builder()
                     .withMessage(msg)
                     .withScanResults(results)
                     .build();
               port.sendEvent(message);
            }
         }
      });
      thr.setName("wfsn");
      thr.setDaemon(false);
      thr.start();
      return null;
   }

   private Object handleEndScanRequest(PlatformMessage message) {
      log.trace("handling wireless end scan request: {}", message);
      try {
         IrisHal.wirelessScanEnd();
      } catch (IOException ex) {
         String msg = ex.getMessage();
         List<Map<String, Object>> results = null;
         return HubWiFiCapability.WiFiScanResultsEvent.builder()
               .withMessage(msg)
               .withScanResults(results)
               .build();
      }
      return null;
   }

}

