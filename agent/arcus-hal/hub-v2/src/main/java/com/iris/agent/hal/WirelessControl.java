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
package com.iris.agent.hal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.attributes.HubAttributesService;
import com.iris.agent.util.ThreadUtils;
import com.iris.io.json.JSON;
import com.iris.messages.capability.HubWiFiCapability;
import com.iris.util.TypeMarker;

class WirelessControl {
   private static final Logger log = LoggerFactory.getLogger(WirelessControl.class);
   private static final long WIRELESS_CHECK_SECONDS = 15;
   private static final int  RSSI_DELTA = 5;

   private static final HubAttributesService.Attribute<Boolean> enabled = HubAttributesService.persisted(Boolean.class, HubWiFiCapability.ATTR_WIFIENABLED, false);
   private static final HubAttributesService.Attribute<String> state = HubAttributesService.ephemeral(String.class, HubWiFiCapability.ATTR_WIFISTATE, HubWiFiCapability.WIFISTATE_DISCONNECTED);
   private static final HubAttributesService.Attribute<String> ssid = HubAttributesService.ephemeral(String.class, HubWiFiCapability.ATTR_WIFISSID, "");
   private static final HubAttributesService.Attribute<String> bssid = HubAttributesService.ephemeral(String.class, HubWiFiCapability.ATTR_WIFIBSSID, "00:00:00:00:00:00");
   private static final HubAttributesService.Attribute<String> security = HubAttributesService.ephemeral(String.class, HubWiFiCapability.ATTR_WIFISECURITY, HubWiFiCapability.WIFISECURITY_NONE);
   private static final HubAttributesService.Attribute<Integer> channel = HubAttributesService.ephemeral(Integer.class, HubWiFiCapability.ATTR_WIFICHANNEL, 0);
   private static final HubAttributesService.Attribute<Integer> noise = HubAttributesService.ephemeral(Integer.class, HubWiFiCapability.ATTR_WIFINOISE, 0);
   private static final HubAttributesService.Attribute<Integer> rssi = HubAttributesService.ephemeral(Integer.class, HubWiFiCapability.ATTR_WIFIRSSI, 0);

   private static final String WIFI_INTF = "wlan0";
   private static final String WIFI_CFG_FILE = "/data/config/wifiCfg";
   private static final String TEST_WIFI_CFG_FILE = "/tmp/testWifiCfg";
   private static final String PROVISIONED_FILE = "/data/config/provisioned";

   private static Boolean scanKilled = false;

   private WirelessControl() {
   }

   public static final WirelessControl create() {
      return new WirelessControl();
   }

   private @Nullable WirelessProcess wp;

   public synchronized void start() {
      shutdown();
      wp = new WirelessBackground();
   }

   public synchronized void shutdown() {
      if (wp != null) {
         wp.shutdown();
      }
      wp = null;
   }

   private interface WirelessProcess {
      void shutdown();
   }

   private List<String> runCommand(String cmd) {
      Process pr = null;

      try {
         pr = Runtime.getRuntime().exec(cmd);
         pr.getErrorStream().close();
         pr.getOutputStream().close();
         BufferedReader stdInput = new BufferedReader(new InputStreamReader(pr.getInputStream()));
         List<String> output = new ArrayList<String>();
         String s;
         while ((s = stdInput.readLine()) != null) {
            output.add(s);
         }
         return output;
      } catch (IOException e) {
         log.debug("Cannot execute '{}' because: [{}]", cmd, e);
      } finally {
         if (pr != null) {
            IOUtils.closeQuietly(pr.getInputStream());
            pr.destroy();
         }
      }
      return Collections.emptyList();
   }

   private void readConfiguration() {
      try (InputStream is = new FileInputStream(WIFI_CFG_FILE)) {
         List<String> lines = IOUtils.readLines(is, StandardCharsets.UTF_8);
         for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains("ssid:")) {
               ssid.set(lines.get(i).substring(6).trim());
            } else if (lines.get(i).contains("security:")) {
               String s = lines.get(i).substring(10).trim();
               // Don't allow empty string
               if (s.isEmpty()) {
                  s = "NONE";
               }
               security.set(s);
            }
         }
      } catch (Exception e) {
         // ignore
      }
   }

   private void getStatus() {
      String cmd;
      List<String> response;

      cmd = String.format("/sbin/iwconfig %s", WIFI_INTF);
      response = runCommand(cmd);
      if (response.size() == 0) {
         state.set(HubWiFiCapability.WIFISTATE_DISCONNECTED);
         bssid.set("00:00:00:00:00:00");
         noise.set(0);
         rssi.set(0);
      } else {
         String line;
         line = response.get(0);
         // Use the current interface to tell if we are really connected as Ethernet may be plugged in!
         if (IrisHal.getPrimaryNetworkInterfaceName().contains("wlan")) {
            state.set(HubWiFiCapability.WIFISTATE_CONNECTED);
         } else {
            state.set(HubWiFiCapability.WIFISTATE_DISCONNECTED);
         }
         line = response.get(1);
         if (line.contains("Not-Associated")) {
            bssid.set("00:00:00:00:00:00");
         } else {
            bssid.set(line.substring(line.lastIndexOf("Access Point:") + 14).trim());
         }

         // Look for signal strength info in the 7th line
         // e.g: "		Link Quality:70/70  Signal level=-6 dBm"
         if (response.size() >= 7) {
            line = response.get(6);
            if ((line != null) && line.contains("Signal level")) {
               String level[] = line.substring(line.lastIndexOf("Signal level=") + 13).trim().split("\\s+");
               if (!level[0].isEmpty()) {
                  // Seems this needs to be adjusted by max power (30 dBm = 1 watt) to get in correct range
                  //  as the value returned here can be > 0 if very close to AP
                  int newRssi = Integer.parseInt(level[0]) - 30;
                  int curRssi = rssi.get();

                  // Only update if we are +/- a delta from the last value to avoid a lot of updates
                  if ((newRssi >= (curRssi + RSSI_DELTA)) || (newRssi <= (curRssi - RSSI_DELTA))) {
                     rssi.set(newRssi);
                  }
               } else {
                  rssi.set(0);
               }
               // Noise is not reported by this driver, leave at 0
               noise.set(0);
            }
         } else {
            rssi.set(0);
            noise.set(0);
         }
      }

      cmd = String.format("/sbin/iwlist %s channel", WIFI_INTF);
      response = runCommand(cmd);
      if (response.size() == 0) {
         channel.set(0);
      } else {
         for (int i = 0; i < response.size(); i++) {
            if (response.get(i).contains("Current")) {
               channel.set(Integer.parseInt(response.get(i).replace(")","\0").substring(response.get(i).lastIndexOf("Channel") + 8).trim()));
            }
         }
      }
   }

   private final class WirelessBackground implements WirelessProcess, Runnable {
      private final Thread thr;
      private boolean running;

      WirelessBackground() {
         this.thr = new Thread(this);
         this.thr.setName("wifi");
         this.thr.setDaemon(true);
         this.running = true;
         this.thr.start();
      }

      public void shutdown() {
         running = false;
      }

      @Override
      public void run() {
         try {
            File f = new File(PROVISIONED_FILE);

            // If we are provisioned, then enable Wifi (even if provisioned for Ethernet, in case
            //  we need to switch later...)
            if (f.isFile()) {
               enabled.set(true);
            }

            // Get current wireless configuration
            readConfiguration();

            // Initialize status if enabled
            if (enabled.get()) {
               getStatus();
            } else {
               // Otherwise, reset to defaults
               state.set(HubWiFiCapability.WIFISTATE_DISCONNECTED);
               bssid.set("00:00:00:00:00:00");
               channel.set(0);
               noise.set(0);
               rssi.set(0);

               // Clear saved config
               clearWirelessConfig();
            }

            // Check periodically for updates
            while (running) {
               ThreadUtils.sleep(WIRELESS_CHECK_SECONDS, TimeUnit.SECONDS);

               // Skip if not enabled yet
               if (enabled.get()) {

                  // Settings may have been updated on successful connect
                  readConfiguration();

                  getStatus();
               }
            }
            log.warn("wireless background process exiting normally");;
         } catch (Throwable th) {
            log.error("WIRELESS BACKGROUND PROCESS EXITED ABNORMALLY:", th);
         }
      }
   }

   public boolean isWirelessEnabled() {
      return enabled.get();
   }

   public String getWirelessState() {
      return state.get();
   }

   public String getWirelessSSID() {
      return ssid.get();
   }

   public String getWirelessBSSID() {
      return bssid.get();
   }

   public String getWirelessSecurity() {
      return security.get();
   }

   public int getWirelessChannel() {
      return channel.get();
   }

   public int getWirelessNoise() {
      return noise.get();
   }

   public int getWirelessRSSI() {
      return rssi.get();
   }

   public void wirelessConnect(String SSID, String BSSID, String sec, String key) throws IOException {
      // First, check if wireless is enabled
      if (!enabled.get()) {
         throw new RuntimeException("Wireless is not enabled");
      }

      OutputStream os = new FileOutputStream(TEST_WIFI_CFG_FILE);
      String data = String.format("ssid: %s\nsecurity: %s\nkey: %s\n", SSID, sec, key);
      os.write(data.getBytes());
      os.close();
   }

   private void clearWirelessConfig() throws IOException {
      OutputStream os = new FileOutputStream(WIFI_CFG_FILE);
      String data = String.format("ssid: \nsecurity: NONE\nkey: \n");
      os.write(data.getBytes());
      os.close();

      // Clear local config info
      ssid.set("");
      security.set(HubWiFiCapability.WIFISECURITY_NONE);
   }

   public void wirelessDisconnect() throws IOException {
      // First, check if wireless is enabled
      if (!enabled.get()) {
         throw new RuntimeException("Wireless is not enabled");
      }

      // Remove save config
      clearWirelessConfig();
      state.set(HubWiFiCapability.WIFISTATE_DISCONNECTED);
   }

   @SuppressWarnings("unchecked")
   public List<Map<String, Object>> wirelessScanStart(int timeout) throws IOException {
      // First, check if wireless is enabled
      if (!enabled.get()) {
         throw new RuntimeException("Wireless is not enabled");
      }

      List<String> response;
      List<Map<String, Object>> scanResults;

      scanKilled = false;

      // Run scan command in hubOS
      response = runCommand("/usr/bin/wifi_scan");
      String json = "";
      for (String s : response) {
          json += s;
      }
      // Returned data is in JSON format, just grab "scanresults"
      Map<String, Object> data = JSON.fromJson(json, TypeMarker.mapOf(String.class,Object.class));
      scanResults = (List<Map<String, Object>>) data.get("scanresults");

      // If scan was killed along the way, return null list
      if (scanKilled) {
         return Collections.emptyList();
      }
      return scanResults;
   }

   public void wirelessScanEnd() throws IOException {
      // First, check if wireless is enabled
      if (!enabled.get()) {
         throw new RuntimeException("Wireless is not enabled");
      }

      // The scan doesn't take that long, but skip returning info if it was killed
      scanKilled = true;
   }

}

