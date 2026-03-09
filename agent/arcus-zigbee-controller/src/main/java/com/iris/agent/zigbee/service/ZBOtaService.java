/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Arcus Project
 *
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

package com.iris.agent.zigbee.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.zigbee.ZBServices;
import com.zsmartsystems.zigbee.IeeeAddress;
import com.zsmartsystems.zigbee.ZigBeeEndpoint;
import com.zsmartsystems.zigbee.ZigBeeNetworkManager;
import com.zsmartsystems.zigbee.ZigBeeNode;
import com.zsmartsystems.zigbee.app.otaserver.ZclOtaUpgradeServer;
import com.zsmartsystems.zigbee.app.otaserver.ZigBeeOtaFile;
import com.zsmartsystems.zigbee.app.otaserver.ZigBeeOtaServerStatus;
import com.zsmartsystems.zigbee.app.otaserver.ZigBeeOtaStatusCallback;
import com.zsmartsystems.zigbee.zcl.ZclCluster;
import com.zsmartsystems.zigbee.zcl.clusters.ZclOtaUpgradeCluster;

public class ZBOtaService {
   private static final Logger logger = LoggerFactory.getLogger(ZBOtaService.class);

   private static final int OTA_CLUSTER_ID = 0x0019;
   private static final File OTA_DIR = new File("/data/iris/data/tmp/ota");

   // Active OTA servers keyed by IEEE address
   private final Map<Long, ZclOtaUpgradeServer> activeServers = new ConcurrentHashMap<>();

   // Callback for reporting progress to the platform
   private OtaProgressCallback progressCallback;

   public interface OtaProgressCallback {
      void onProgress(long ieeeAddr, ZigBeeOtaServerStatus status, int percentComplete);
   }

   public void setProgressCallback(OtaProgressCallback callback) {
      this.progressCallback = callback;
   }

   public void startFirmwareUpdate(long ieeeAddr, String url, String md5) {
      logger.info("Starting OTA firmware update for IEEE={} from {}", String.format("%016X", ieeeAddr), url);

      ZigBeeNetworkManager nwkMgr = getNetworkManager();
      if (nwkMgr == null) {
         logger.error("Cannot start OTA: network manager not available");
         reportProgress(ieeeAddr, ZigBeeOtaServerStatus.OTA_UPGRADE_FAILED, 0);
         return;
      }

      // Download firmware file in background
      com.iris.agent.zigbee.util.ZBScheduler.INSTANCE.startProcess(() -> {
         try {
            byte[] firmwareData = downloadFirmware(url, ieeeAddr);
            if (firmwareData == null) {
               reportProgress(ieeeAddr, ZigBeeOtaServerStatus.OTA_UPGRADE_FAILED, 0);
               return;
            }

            ZigBeeOtaFile otaFile = new ZigBeeOtaFile(firmwareData);
            logger.info("OTA file loaded: mfr=0x{} type=0x{} version=0x{} size={}",
                  String.format("%04X", otaFile.getManufacturerCode()),
                  String.format("%04X", otaFile.getImageType()),
                  String.format("%08X", otaFile.getFileVersion()),
                  otaFile.getImageSize());

            attachOtaServer(ieeeAddr, otaFile, nwkMgr);
         } catch (Exception ex) {
            logger.error("OTA firmware update failed for IEEE={}: {}",
                  String.format("%016X", ieeeAddr), ex.getMessage(), ex);
            reportProgress(ieeeAddr, ZigBeeOtaServerStatus.OTA_UPGRADE_FAILED, 0);
         }
      }, 0);
   }

   public void cancelFirmwareUpdate(long ieeeAddr) {
      ZclOtaUpgradeServer server = activeServers.remove(ieeeAddr);
      if (server != null) {
         logger.info("Cancelling OTA firmware update for IEEE={}", String.format("%016X", ieeeAddr));
         server.cancelUpgrade();
      }
   }

   public void reset() {
      for (Map.Entry<Long, ZclOtaUpgradeServer> entry : activeServers.entrySet()) {
         entry.getValue().cancelUpgrade();
      }
      activeServers.clear();
      logger.info("Cleared all active OTA servers");
   }

   private void attachOtaServer(long ieeeAddr, ZigBeeOtaFile otaFile, ZigBeeNetworkManager nwkMgr) {
      IeeeAddress addr = new IeeeAddress(String.format("%016X", ieeeAddr));
      ZigBeeNode zsNode = nwkMgr.getNode(addr);
      if (zsNode == null) {
         logger.error("Cannot start OTA: zsmartsystems node not found for IEEE={}", String.format("%016X", ieeeAddr));
         reportProgress(ieeeAddr, ZigBeeOtaServerStatus.OTA_UPGRADE_FAILED, 0);
         return;
      }

      // Find or create an endpoint with the OTA cluster
      ZigBeeEndpoint endpoint = findOtaEndpoint(zsNode);
      if (endpoint == null) {
         // Create endpoint 1 with OTA output cluster if no endpoint exists
         endpoint = zsNode.getEndpoint(1);
         if (endpoint == null) {
            endpoint = new ZigBeeEndpoint(zsNode, 1);
            endpoint.setProfileId(0x0104); // HA
            zsNode.addEndpoint(endpoint);
         }
         // Register OTA as an output cluster (server-side, device is client)
         ZclCluster otaCluster = new ZclOtaUpgradeCluster(endpoint);
         endpoint.addOutputCluster(otaCluster);
         logger.debug("Registered OTA output cluster on endpoint {} for IEEE={}",
               endpoint.getEndpointId(), String.format("%016X", ieeeAddr));
      }

      // Create and attach the OTA server application
      ZclOtaUpgradeServer otaServer = new ZclOtaUpgradeServer();
      endpoint.addApplication(otaServer);

      otaServer.addListener(new ZigBeeOtaStatusCallback() {
         @Override
         public void otaStatusUpdate(ZigBeeOtaServerStatus status, int percent) {
            logger.info("OTA status for IEEE={}: {} ({}%)",
                  String.format("%016X", ieeeAddr), status, percent);
            reportProgress(ieeeAddr, status, percent);

            if (status == ZigBeeOtaServerStatus.OTA_TRANSFER_COMPLETE) {
               otaServer.completeUpgrade();
            }

            if (isTerminalStatus(status)) {
               activeServers.remove(ieeeAddr);
            }
         }
      });

      otaServer.setAutoUpgrade(true);
      otaServer.setDataSize(64);
      otaServer.setFirmware(otaFile);

      activeServers.put(ieeeAddr, otaServer);
      reportProgress(ieeeAddr, ZigBeeOtaServerStatus.OTA_WAITING, 0);
      logger.info("OTA server attached for IEEE={}, waiting for device to query",
            String.format("%016X", ieeeAddr));
   }

   private ZigBeeEndpoint findOtaEndpoint(ZigBeeNode zsNode) {
      for (ZigBeeEndpoint ep : zsNode.getEndpoints()) {
         if (ep.getOutputCluster(OTA_CLUSTER_ID) != null) {
            return ep;
         }
      }
      return null;
   }

   private byte[] downloadFirmware(String url, long ieeeAddr) {
      logger.info("Downloading OTA firmware from {} for IEEE={}", url, String.format("%016X", ieeeAddr));
      try {
         OTA_DIR.mkdirs();
         File tempFile = new File(OTA_DIR, String.format("ota_%016X.bin", ieeeAddr));

         URL firmwareUrl = new URL(url);
         try (InputStream in = firmwareUrl.openStream();
              FileOutputStream out = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
               out.write(buffer, 0, bytesRead);
            }
         }

         byte[] data = Files.readAllBytes(tempFile.toPath());
         tempFile.delete();
         logger.info("Downloaded {} bytes of OTA firmware for IEEE={}",
               data.length, String.format("%016X", ieeeAddr));
         return data;
      } catch (Exception ex) {
         logger.error("Failed to download OTA firmware from {}: {}", url, ex.getMessage(), ex);
         return null;
      }
   }

   private void reportProgress(long ieeeAddr, ZigBeeOtaServerStatus status, int percentComplete) {
      if (progressCallback != null) {
         progressCallback.onProgress(ieeeAddr, status, percentComplete);
      }
   }

   private boolean isTerminalStatus(ZigBeeOtaServerStatus status) {
      return status == ZigBeeOtaServerStatus.OTA_UPGRADE_COMPLETE
            || status == ZigBeeOtaServerStatus.OTA_UPGRADE_FAILED
            || status == ZigBeeOtaServerStatus.OTA_CANCELLED;
   }

   private ZigBeeNetworkManager getNetworkManager() {
      com.iris.agent.zigbee.ember.ZigbeeDriver driver = ZBServices.INSTANCE.getDriver();
      return driver != null ? driver.getNetworkManager() : null;
   }
}
