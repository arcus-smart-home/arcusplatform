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
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.inject.Module;
import com.netflix.governator.guice.BootstrapModule;

public interface IrisHalInternal {
   void start(File base, Set<File> configs);
   void shutdown();

   /////////////////////////////////////////////////////////////////////////////
   // System level utility methods
   /////////////////////////////////////////////////////////////////////////////

   /**
    * Restarts the software stack without rebooting the entire machine.
    */
   void restart();

   /**
    * Reboots the entire machine.
    */
   void reboot();

   /**
    * Restarts the software stack and runs some self checks.
    */
   void rebootAndSelfCheck();

   /**
    * Performs a factory reset on the hub.
    */
   void factoryReset();

   /**
    * Determines the directory where firmware's are downloaded.
    */
   File getFirmwareDownloadDir(String type) throws IOException;

   /**
    * Installs a downloaded firmware file.
    */
   void installHubFirmware(File file, String type, boolean force) throws IOException;

   /////////////////////////////////////////////////////////////////////////////
   // Watchdog utility methods
   /////////////////////////////////////////////////////////////////////////////

   void startWatchdog();
   void shutdownWatchdog();
   void pokeWatchdog();

   /////////////////////////////////////////////////////////////////////////////
   // LED control and state utility methods
   /////////////////////////////////////////////////////////////////////////////

   LEDState getLedState();
   void setLedState(LEDState state);
   void setLedState(LEDState state, int timer);

   /////////////////////////////////////////////////////////////////////////////
   // Sounder control and state utility methods
   /////////////////////////////////////////////////////////////////////////////

   void playSound(String url, int repeat);
   void setSounderMode(SounderMode mode, int duration);

   /////////////////////////////////////////////////////////////////////////////
   // Radio chip utility methods
   /////////////////////////////////////////////////////////////////////////////


   int getZWaveNvmCapacity();
   boolean resetZWaveChip();
   boolean resetZigbeeChip();
   boolean resetBluetoothChip();
   boolean isZigbeeRtsCts();
   boolean isZigbeeXonXoff();
   int getZigbeeBaudRate();

   /////////////////////////////////////////////////////////////////////////////
   // System information utility methods
   /////////////////////////////////////////////////////////////////////////////

   String getDataDiskName();
   String getHubId();
   String getAgentVersion();
   String getVendor();
   String getModel();
   String getSerialNumber();
   String getHardwareVersion();
   Long getHardwareFlashSize();
   String getMacAddress();
   String getManufacturingInfo();
   String getManufacturingBatchNumber();
   Date getManufacturingDate();
   int getManufacturingFactoryID();
   String getOperatingSystemVersion();
   String getBootloaderVersion();
   File getHubCertificateFile();
   File getHubKeyFile();

   /////////////////////////////////////////////////////////////////////////////
   // Battery utility methods
   /////////////////////////////////////////////////////////////////////////////

   boolean isBatteryPowered();
   double getBatteryVoltage();
   String getPowerSource();
   int getBatteryLevel();
   void addBatteryStateListener(BatteryStateListener listener);
   void removeBatteryStateListener(BatteryStateListener listener);

   /////////////////////////////////////////////////////////////////////////////
   // Button utility methods
   /////////////////////////////////////////////////////////////////////////////

   boolean hasButton();
   String getButtonState();
   int getButtonDuration();
   Long getButtonLastPressed();
   void addButtonListener(ButtonListener listener);
   void removeButtonListener(ButtonListener listener);
   
   /////////////////////////////////////////////////////////////////////////////
   // Network utility methods
   /////////////////////////////////////////////////////////////////////////////

   IrisHal.NetworkInfo getNetworkInfo();
   String getPrimaryNetworkInterfaceName();
   String getSecondaryNetworkInterfaceName();
   String getGateway();
   List<String> getDNS();
   List<String> getRoutingTable();
   List<String> getInterfaces();
   List<String> getLocalMulticastInterfaces();

   String getSSHD();

   /////////////////////////////////////////////////////////////////////////////
   // Wireless utility methods
   /////////////////////////////////////////////////////////////////////////////

   boolean isWirelessSupported();
   boolean isWirelessEnabled();
   String getWirelessState();
   String getWirelessSSID();
   String getWirelessBSSID();
   String getWirelessSecurity();
   int getWirelessChannel();
   int getWirelessNoise();
   int getWirelessRSSI();
   void wirelessConnect(String SSID, String BSSID, String security, String key) throws IOException;
   void wirelessDisconnect() throws IOException;
   List<Map<String, Object>> wirelessScanStart(int timeout) throws IOException;
   void wirelessScanEnd() throws IOException;

   /////////////////////////////////////////////////////////////////////////////
   // Application bootstrapping methods
   /////////////////////////////////////////////////////////////////////////////

   Collection<? extends BootstrapModule> getBootstrapModules();
   Collection<Class<? extends BootstrapModule>> getBootstrapModuleClasses();

   Collection<? extends Module> getApplicationModules();
   Collection<Class<? extends Module>> getApplicationModuleClasses();

   /////////////////////////////////////////////////////////////////////////////
   // Debugging
   /////////////////////////////////////////////////////////////////////////////

   boolean isInDebugMode();
   String getSyslog();
   String getBootlog();
   String getAgentDb();
   String getFiles(List<String> paths);
   String getProcesses();
   String getLoad();

}

