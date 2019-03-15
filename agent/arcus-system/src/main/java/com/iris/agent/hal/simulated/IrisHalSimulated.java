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
package com.iris.agent.hal.simulated;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.iris.agent.scene.SceneService;
import org.eclipse.jdt.annotation.Nullable;

import com.google.inject.Module;
import com.iris.agent.attributes.HubAttributesService;
import com.iris.agent.backup.BackupService;
import com.iris.agent.config.ConfigService;
import com.iris.agent.db.DbService;
import com.iris.agent.exec.ExecService;
import com.iris.agent.hal.AbstractIrisHal;
import com.iris.agent.hal.BatteryStateListener;
import com.iris.agent.hal.ButtonListener;
import com.iris.agent.hal.IrisHal;
import com.iris.agent.hal.LEDState;
import com.iris.agent.hal.SounderMode;
import com.iris.agent.http.HttpService;
import com.iris.agent.lifecycle.LifeCycleService;
import com.iris.agent.metrics.MetricsService;
import com.iris.agent.storage.StorageService;
import com.iris.agent.upnp.IrisUpnpService;
import com.iris.messages.capability.ButtonCapability;
import com.iris.messages.capability.HubPowerCapability;

/**
 * Implementation of Iris HAL for simulated devices.
 */
public class IrisHalSimulated extends AbstractIrisHal {
   private LEDState ledState = LEDState.UNKNOWN;
   private SounderMode sounderMode = SounderMode.UNKNOWN;

   @Override
   public boolean isInDebugMode() {
      return false;
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
      // policy for restarting the agent.
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
      // policy for factory resetting the simulated hub.
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
      return false;
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
   protected void startStorageService(File base) {
      String basePath = base.getPath();

      StorageService.start(10, TimeUnit.SECONDS);
      StorageService.addRootMapping("agent://", "file://" + base.getAbsolutePath());
      StorageService.addRootMapping("tmp://", "file://" + basePath + "/tmp");
      StorageService.addRootMapping("file://", "file://" + basePath + "/data");
      StorageService.addRootMapping("db://", "file://" + basePath + "/db");
   }

   @Override
   protected void startDatabaseService() {
      DbService.start();
   }

   @Override
   protected void startConfigService(Set<File> configs) {
      ConfigService.start(configs);
   }

   @Override
   protected void startExecService() {
      ExecService.start();
   }

   @Override
   protected void startAttributesService() {
      HubAttributesService.start();
   }

   @Override
   protected void startLifeCycleService() {
      LifeCycleService.start();
   }

   @Override
   protected void startHttpService() {
      HttpService.start();
   }

   @Override
   protected void startUpnpService() {
      IrisUpnpService.start();
   }

   @Override
   protected void startFourgService() {
   }

   @Override
   protected void startBackupService() {
      BackupService.start();
   }

   @Override
   protected void startSceneService() {
      SceneService.start();
   }

   @Override
   protected void startMetricsService() {
      MetricsService.start();
   }

   @Override
   protected void shutdownStorageService() {
      StorageService.shutdown();
   }

   @Override
   protected void shutdownDatabaseService() {
      DbService.shutdown();
   }

   @Override
   protected void shutdownConfigService() {
      ConfigService.shutdown();
   }

   @Override
   protected void shutdownExecService() {
      ExecService.shutdown();
   }

   @Override
   protected void shutdownAttributesService() {
      HubAttributesService.shutdown();
   }

   @Override
   protected void shutdownLifeCycleService() {
      LifeCycleService.shutdown();
   }

   @Override
   protected void shutdownHttpService() {
      HttpService.shutdown();
   }

   @Override
   protected void shutdownUpnpService() {
      IrisUpnpService.shutdown();
   }

   @Override
   protected void shutdownFourgService() {
   }

   @Override
   protected void shutdownBackupService() {
      BackupService.shutdown();
   }

   @Override
   protected void shutdownSceneService() {
      SceneService.shutdown();
   }

   @Override
   protected void shutdownMetricsService() {
      MetricsService.shutdown();
   }

   @Override
   @Nullable
   protected Class<? extends Module> getRouterModuleClass() {
      return null;
   }

   @Override
   @Nullable
   protected Class<? extends Module> getGatewayModuleClass() {
      return null;
   }

   @Override
   @Nullable
   protected Class<? extends Module> getHubControllerModuleClass() {
      return null;
   }

   @Override
   @Nullable
   protected Class<? extends Module> getZWaveControllerModuleClass() {
      return null;
   }

   @Override
   @Nullable
   protected Class<? extends Module> getZigbeeControllerModuleClass() {
      return null;
   }

   @Override
   @Nullable
   protected Class<? extends Module> getSercommControllerModuleClass() {
      return null;
   }

   @Override
   @Nullable
   protected Class<? extends Module> get4gControllerModuleClass() {
      return null;
   }

   @Override
   @Nullable
   protected Class<? extends Module> getReflexControllerModuleClass() {
      return null;
   }

   @Override
   @Nullable
   protected Class<? extends Module> getAlarmControllerModuleClass() {
      return null;
   }

   @Nullable
   protected Class<? extends Module> getHueControllerModuleClass() {
      return null;
   }
   
   @Nullable
   protected Class<? extends Module> getSpyModuleClass() {
      return null;
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
      return "sshd";
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

