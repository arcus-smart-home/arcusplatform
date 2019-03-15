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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.iris.agent.attributes.HubAttributesService;
import com.iris.agent.storage.StorageService;
import com.iris.messages.capability.ButtonCapability;
import com.iris.messages.capability.HubPowerCapability;
import com.iris.messages.capability.HubWiFiCapability;

/**
 * Implementation of Iris HAL for simulated devices.
 */
public final class IrisHalImpl extends AbstractIrisHalCommon {
   private static final Logger log = LoggerFactory.getLogger(IrisHalImpl.class);

   private static final String UNKNOWN = "unknown";
   private static final String SSHD = "sshd";

   private LEDState ledState = LEDState.UNKNOWN;
   private SounderMode sounderMode = SounderMode.UNKNOWN;

   @Override
   public boolean isInDebugMode() {
      return false;
   }

   @Override
   protected void startStorageService(File base) {
      super.startStorageService(base);
      StorageService.addRootMapping("agent://", "file://" + base.getAbsolutePath());
      StorageService.addRootMapping("tmp://", "file:///tmp/iris/tmp");
      StorageService.addRootMapping("file://", "file:///tmp/iris/data");
      StorageService.addRootMapping("db://", "file:///tmp/iris/db");
   }

   @Override
   public void restart() {
      // The NOSONAR comment below disables the sonar warning for
      // this line of code. This line of code implements the
      // policy for restarting the agent.
      System.exit(0); // NOSONAR
   }

   @Override
   public void reboot() {
      // The NOSONAR comment below disables the sonar warning for
      // this line of code. This line of code implements the
      // policy for rebooting the agent.
      System.exit(0); // NOSONAR
   }

   @Override
   public void rebootAndSelfCheck() {
      // The NOSONAR comment below disables the sonar warning for
      // this line of code. This line of code implements the
      // policy for restarting the agent.
      System.exit(0); // NOSONAR
   }

   @Override
   public void factoryReset() {
      // The NOSONAR comment below disables the sonar warning for
      // this line of code. This line of code implements the
      // policy for factory resetting the hub.
      System.exit(0); // NOSONAR
   }

   @Override
   public File getFirmwareDownloadDir(String type) {
      File dst = StorageService.getFile("file:///tmp");
      dst.mkdirs();

      return dst;
   }

   @Override
   public void installHubFirmware(File file, String type, boolean force) {
      file.delete();
      reboot();
   }

   @Override
   public void startWatchdog() {
   }

   @Override
   public void shutdownWatchdog() {
   }

   @Override
   public void pokeWatchdog() {
   }

   @Override
   public LEDState getLedState() {
      return ledState;
   }

   @Override
   public void setLedState(LEDState state) {
      ledState = state;
   }

   @Override
   public void setLedState(LEDState state, int timer) {
	   ledState = state;
   }


   @Override
   public void setSounderMode(SounderMode mode, int duration) {
      sounderMode = mode;
   }

   @Override
   public void playSound(String url, int repeat) {
      sounderMode = SounderMode.USER_DEFINED;
   }

   @Override
   public int getZWaveNvmCapacity() {
      return -1;
   }

   @Override
   public boolean resetZWaveChip() {
      return false;
   }

   @Override
   public boolean resetZigbeeChip() {
      return false;
   }

   @Override
   public boolean resetBluetoothChip() {
      return false;
   }

   @Override
   public boolean isZigbeeRtsCts() {
      return false;
   }

   @Override
   public boolean isZigbeeXonXoff() {
      return true;
   }

   @Override
   public int getZigbeeBaudRate() {
      return 115200;
   }

   @Override
   public File getHubCertificateFile() {
      return new File("/tmp/hub.crt");
   }

   @Override
   public File getHubKeyFile() {
      return new File("/tmp/hub.key");
   }

   @Override
   public String getDataDiskName() {
      return "";
   }

   @Override
   public String getHubId() {
      return "ABC-2345";
   }

   @Override
   public String getAgentVersion() {
      return "1.0.0";
   }

   @Override
   public String getVendor() {
      return "Iris Development Team";
   }

   @Override
   public String getModel() {
      return "Simulated Hub";
   }

   @Override
   public String getSerialNumber() {
      return "1";
   }

   @Override
   public String getHardwareVersion() {
      return "1.0.0";
   }

   @Override
   public Long getHardwareFlashSize() {
      return (long)1000000000;
   }

   @Override
   public String getMacAddress() {
      return "00:11:22:33:44:55";
   }

   @Override
   public String getManufacturingInfo() {
      return "Simulated Hub";
   }

   @Override
   public String getManufacturingBatchNumber() {
      return "1234567890";
   }

   @Override
   public Date getManufacturingDate() {
      return new Date();
   }

   @Override
   public int getManufacturingFactoryID() {
      return 1;
   }

   @Override
   public String getOperatingSystemVersion() {
      return "1.0.0";
   }

   @Override
   public String getBootloaderVersion() {
      return "1.0.0";
   }

   @Override
   public boolean isBatteryPowered() {
      return false;
   }

   @Override
   public double getBatteryVoltage() {
      return 5.00;
   }

   @Override
   public int getBatteryLevel() {
      return 100;
   }

   @Override
   public String getPowerSource() {
      return HubPowerCapability.SOURCE_MAINS;
   }

   @Override
   public void addBatteryStateListener(BatteryStateListener listener) {
   }

   @Override
   public void removeBatteryStateListener(BatteryStateListener listener) {
   }

   /////////////////////////////////////////////////////////////////////////////
   // Iris Hal Startup and Shutdown
   /////////////////////////////////////////////////////////////////////////////

   @Override
   @Nullable
   protected Class<? extends Module> getZWaveControllerModuleClass() {
      return ZWaveModuleSimulated.class;
   }

   @Override
   @Nullable
   protected Class<? extends Module> getZigbeeControllerModuleClass() {
      return ZigbeeModuleSimulated.class;
   }

   @Override
   @Nullable
   protected Class<? extends Module> getSercommControllerModuleClass() {
      return SercommModuleSimulated.class;
   }

   @Override
   @Nullable
   protected Class<? extends Module> get4gControllerModuleClass() {
      return FourGModuleSimulated.class;
   }

   @Override
   @Nullable
   protected Class<? extends Module> getReflexControllerModuleClass() {
      return ReflexModuleSimulated.class;
   }

   @Override
   @Nullable
   protected Class<? extends Module> getAlarmControllerModuleClass() {
      return AlarmModuleSimulated.class;
   }

   @Nullable
   @Override
   protected Class<? extends Module> getHueControllerModuleClass() {
      return HueModuleSimulated.class;
   }
   
   @Nullable
   @Override
   protected Class<? extends Module> getSpyModuleClass() {
   	return SpyModuleSimulated.class;
   }

   public static final class ZWaveModuleSimulated extends AbstractModule {
      @Override
      protected void configure() {
         String port = System.getenv("ZWAVE_PORT");
         if (port != null) {
            // bind(ZWaveDriverFactory.class).in(Singleton.class);
            // bind(ZWaveController.class).in(Singleton.class);
         }

      }
   }

   public static final class ZigbeeModuleSimulated extends AbstractModule {
      @Override
      protected void configure() {
         String port = System.getenv("ZIGBEE_PORT");
         /*
         if (port != null) {
            ZigbeeDriverFactory factory = new ZigbeeEmberDriverFactory(port);
            bind(ZigbeeDriverFactory.class).toInstance(factory);
            bind(ZigbeeController.class).in(Singleton.class);
         }
         */
      }
   }

   public static final class SercommModuleSimulated extends AbstractModule {
      @Override
      protected void configure() {
      }
   }

   public static final class FourGModuleSimulated extends AbstractModule {
      @Override
      protected void configure() {
      }
   }

   public static final class ReflexModuleSimulated extends AbstractModule {
      @Override
      protected void configure() {
      }
   }

   public static final class AlarmModuleSimulated extends AbstractModule {
      @Override
      protected void configure() {
      }
   }

   public static final class HueModuleSimulated extends AbstractModule {
      @Override
      protected void configure() {
      }
   }
   
   public static final class SpyModuleSimulated extends AbstractModule {
   	@Override
   	protected void configure() {
   	}
   }

   @Override
   public String getPrimaryNetworkInterfaceName() {
      return "eth0";
   }

   @Override
   public String getSecondaryNetworkInterfaceName() {
      return "eth1";
   }
   
   @Override
   public IrisHal.NetworkInfo getNetworkInfo() {
      return new IrisHal.NetworkInfo(null, "eth0", "eth", "192.168.1.100", "255.255.255.0", null, null, null, null, null);
   }

   @Override
   public String getGateway() {
      return "192.168.1.1";
   }

   @Override
   public List<String> getDNS() {
      return Collections.emptyList();
   }

   @Override
   public List<String> getRoutingTable() {
      return Collections.emptyList();
   }

   @Override
   public List<String> getInterfaces() {
      return Collections.emptyList();
   }

   @Override
   public List<String> getLocalMulticastInterfaces() {
      return Collections.emptyList();
   }

   @Override
   public String getSSHD() {
      return SSHD;
   }

   @Override
   public String getSyslog() {
      return "";
   }

   @Override
   public String getBootlog() {
      return "";
   }

   @Override
   public String getAgentDb() {
      return "";
   }

   @Override
   public String getFiles(List<String> paths) {
      return "";
   }

   @Override
   public String getProcesses() {
      return "";
   }

   @Override
   public String getLoad() {
      return "";
   }

   @SuppressWarnings("unused")
   private static final HubAttributesService.Attribute<Boolean> enabled = HubAttributesService.ephemeral(Boolean.class, HubWiFiCapability.ATTR_WIFIENABLED, true);
   @SuppressWarnings("unused")
   private static final HubAttributesService.Attribute<String> state = HubAttributesService.ephemeral(String.class, HubWiFiCapability.ATTR_WIFISTATE, HubWiFiCapability.WIFISTATE_CONNECTED);
   @SuppressWarnings("unused")
   private static final HubAttributesService.Attribute<String> ssid = HubAttributesService.ephemeral(String.class, HubWiFiCapability.ATTR_WIFISSID, IrisHal.getWirelessSSID());
   @SuppressWarnings("unused")
   private static final HubAttributesService.Attribute<String> bssid = HubAttributesService.ephemeral(String.class, HubWiFiCapability.ATTR_WIFIBSSID, IrisHal.getWirelessBSSID());
   @SuppressWarnings("unused")
   private static final HubAttributesService.Attribute<String> security = HubAttributesService.ephemeral(String.class, HubWiFiCapability.ATTR_WIFISECURITY, HubWiFiCapability.WIFISECURITY_NONE);
   @SuppressWarnings("unused")
   private static final HubAttributesService.Attribute<Integer> channel = HubAttributesService.ephemeral(Integer.class, HubWiFiCapability.ATTR_WIFICHANNEL, IrisHal.getWirelessChannel());
   @SuppressWarnings("unused")
   private static final HubAttributesService.Attribute<Integer> noise = HubAttributesService.ephemeral(Integer.class, HubWiFiCapability.ATTR_WIFINOISE, IrisHal.getWirelessNoise());
   @SuppressWarnings("unused")
   private static final HubAttributesService.Attribute<Integer> rssi = HubAttributesService.ephemeral(Integer.class, HubWiFiCapability.ATTR_WIFIRSSI, IrisHal.getWirelessRSSI());

   @Override
   public boolean isWirelessSupported() {
      return true;
   }

   @Override
   public boolean isWirelessEnabled() {
      return true;
   }

   @Override
   public String getWirelessState() {
      return "CONNECTED";
   }

   @Override
   public String getWirelessSSID() {
      return "test";
   }

   @Override
   public String getWirelessBSSID() {
      return "00:00:00:00:00:00";
   }

   @Override
   public String getWirelessSecurity() {
      return "NONE";
   }

   @Override
   public int getWirelessChannel() {
      return 1;
   }

   @Override
   public int getWirelessNoise() {
      return 0;
   }

   @Override
   public int getWirelessRSSI() {
      return 0;
   }

   @Override
   public void wirelessConnect(String SSID, String BSSID, String security, String key) throws IOException {
   }

   @Override
   public void wirelessDisconnect() throws IOException {
   }

   @Override
   public List<Map<String, Object>> wirelessScanStart(int timeout) throws IOException {
      return Collections.emptyList();
   }

   @Override
   public void wirelessScanEnd() throws IOException {
   }

    @Override
    public boolean hasButton() {
        return false;
    }
    
    @Override
    public int getButtonDuration() {
        return 0;
    }

    @Override
    public Long getButtonLastPressed() {
        return new Date().toInstant().toEpochMilli();
    }
    
    @Override
    public String getButtonState() {
        return ButtonCapability.STATE_RELEASED;
    }

    @Override
    public void addButtonListener(ButtonListener listener) {
    }

    @Override
    public void removeButtonListener(ButtonListener listener) {
    }


}

