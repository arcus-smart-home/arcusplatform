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
import java.net.NetworkInterface;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;

import com.google.inject.Module;
import com.netflix.governator.guice.BootstrapModule;

public final class IrisHal {
   public static enum OsType {
      UNKNOWN,
      LINUX,
      MAC,
      WINDOWS
   }


   private static final Object STARTUP_LOCK = new Object();
   private static final OsType OS;

   private static volatile @Nullable IrisHalInternal internal;


   static {
      String os = System.getProperty("os.name").toLowerCase();
      if (os.contains("mac")) {
         OS = OsType.MAC;
      } else if (os.contains("linux")) {
         OS = OsType.LINUX;
      } else if (os.contains("windows")) {
         OS = OsType.WINDOWS;
      } else {
         OS = OsType.UNKNOWN;
      }
   }

   private IrisHal() {
   }

   public static void start(File base, Set<File> configs, IrisHalInternal intern) {
      IrisHalInternal startup = internal;

      synchronized (STARTUP_LOCK) {
         if (startup != null) {
            throw new IllegalStateException("iris hal already started");
         }

         startup = intern;
         internal = startup;
      }

      startup.start(base, configs);
   }

   public static void start(File base, Set<File> configs) {
      get().start(base, configs);
   }

   public static void shutdown() {
      synchronized (STARTUP_LOCK) {
         try {
            if (internal != null) {
               internal.shutdown();
            }

            internal = null;
         } finally {
            STARTUP_LOCK.notifyAll();
         }
      }
   }

   public static void waitForShutdown() {
      while (true) {
         try {
            synchronized (STARTUP_LOCK) {
               STARTUP_LOCK.wait();
            }
            break;
         } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
         }
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // System level utility methods
   /////////////////////////////////////////////////////////////////////////////

   /**
    * Restarts the software stack without rebooting the entire machine.
    */
   public static void restart() {
      get().restart();
   }

   /**
    * Reboots the entire machine.
    */
   public static void reboot() {
      get().reboot();
   }

   /**
    * Restarts the software stack after running some self checks.
    */
   public static void rebootAndSelfCheck() {
      get().rebootAndSelfCheck();
   }

   /**
    * Performs a factory reset.
    */
   public static void factoryReset() {
      get().factoryReset();
   }

   /**
    * Determines the base directory where firmware updates are downloaded.
    */
   public static File getFirmwareDownloadDir(String type) throws Exception {
      return get().getFirmwareDownloadDir(type);
   }

   /**
    * Installs a downloaded firmware file.
    */
   public static void installHubFirmware(File file, String type, boolean force) throws Exception {
      get().installHubFirmware(file, type, force);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Watchdog utility methods
   /////////////////////////////////////////////////////////////////////////////

   public static void startWatchdog() {
      get().startWatchdog();
   }

   public static void shutdownWatchdog() {
      get().shutdownWatchdog();
   }

   public static void pokeWatchdog() {
      get().pokeWatchdog();
   }

   /////////////////////////////////////////////////////////////////////////////
   // LED control and state utility methods
   /////////////////////////////////////////////////////////////////////////////

   public static LEDState getLedState() {
      return get().getLedState();
   }

   public static void setLedState(LEDState state) {
      get().setLedState(state);
   }

   public static void setLedState(LEDState state,int timer) {
	      get().setLedState(state,timer);
   }

   
   /////////////////////////////////////////////////////////////////////////////
   // Sounder control and state utility methods
   /////////////////////////////////////////////////////////////////////////////

   public static void setSounderMode(SounderMode sounderMode) {
      setSounderMode(sounderMode, 0);
   }

   public static void setSounderMode(SounderMode sounderMode, int duration) {
      get().setSounderMode(sounderMode,duration);
   }

   public static void playSound(String url, int repeat) {
      get().playSound(url, repeat);
   }


   /////////////////////////////////////////////////////////////////////////////
   // Radio chip utility methods
   /////////////////////////////////////////////////////////////////////////////

   public static int getZWaveNvmCapacity() {
      return get().getZWaveNvmCapacity();
   }

   public static boolean resetZWaveChip() {
      return get().resetZWaveChip();
   }

   public static boolean resetZigbeeChip() {
      return get().resetZigbeeChip();
   }

   public static boolean resetBluetoothChip() {
      return get().resetBluetoothChip();
   }

   public static int getZigbeeBaudRate() {
      return get().getZigbeeBaudRate();
   }

   public static boolean isZigbeeRtsCts() {
      return get().isZigbeeRtsCts();
   }

   public static boolean isZigbeeXonXoff() {
      return get().isZigbeeXonXoff();
   }

   /////////////////////////////////////////////////////////////////////////////
   // System information utility methods
   /////////////////////////////////////////////////////////////////////////////
   
   public static String getDataDiskName() {
      return get().getDataDiskName();
   }

   public static File getHubCertificateFile() {
      return get().getHubCertificateFile();
   }

   public static File getHubKeyFile() {
      return get().getHubKeyFile();
   }

   public static String getHubId() {
      return get().getHubId();
   }

   @Nullable
   public static String getHubIdOrNull() {
      IrisHalInternal result = internal;
      if (result == null) {
         return null;
      }

      return result.getHubId();
   }

   public static String getAgentVersion() {
      return get().getAgentVersion();
   }

   public static String getVendor() {
      return get().getVendor();
   }

   public static String getModel() {
      return get().getModel();
   }

   public static String getSerialNumber() {
      return get().getSerialNumber();
   }

   public static String getHardwareVersion() {
      return get().getHardwareVersion();
   }

   public static Long getHardwareFlashSize() {
      return get().getHardwareFlashSize();
   }

   public static String getMacAddress() {
      return get().getMacAddress();
   }

   public static String getManufacturingInfo() {
      return get().getManufacturingInfo();
   }

   public static String getManufacturingBatchNumber() {
      return get().getManufacturingBatchNumber();
   }

   public static Date getManufacturingDate() {
      return get().getManufacturingDate();
   }

   public static int getManufacturingFactoryID() {
      return get().getManufacturingFactoryID();
   }

   public static String getOperatingSystemVersion() {
      return get().getOperatingSystemVersion();
   }

   @Nullable
   public static String getOperatingSystemVersionOrNull() {
      IrisHalInternal result = internal;
      if (result == null) {
         return null;
      }

      return result.getOperatingSystemVersion();
   }

   public static String getBootloaderVersion() {
      return get().getBootloaderVersion();
   }

   /////////////////////////////////////////////////////////////////////////////
   // Battery utilities
   /////////////////////////////////////////////////////////////////////////////

   public static boolean isBatteryPowered() {
       return get().isBatteryPowered();
   }

   public static double getBatteryVoltage() {
       return get().getBatteryVoltage();
   }

   public static int getBatteryLevel() {
       return get().getBatteryLevel();
   }

   public static String getPowerSource() {
      return get().getPowerSource();
  }

   public static void addBatteryStateListener(BatteryStateListener listener) {
       get().addBatteryStateListener(listener);
   }

   public static void removeBatteryStateListener(BatteryStateListener listener) {
       get().removeBatteryStateListener(listener);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Button utilities
   /////////////////////////////////////////////////////////////////////////////

   public static boolean hasButton() {
       return get().hasButton();
   }
   
   public static String getButtonState() {
       return get().getButtonState();
   }

   public static int getButtonDuration() {
       return get().getButtonDuration();
   }
   
   public static Long getButtonLastPressed() {
       return get().getButtonLastPressed();
   }
   
   public static void addButtonListener(ButtonListener listener) {
       get().addButtonListener(listener);
   }

   public static void removeButtonListener(ButtonListener listener) {
       get().removeButtonListener(listener);
   }

   
   /////////////////////////////////////////////////////////////////////////////
   // Network utility methods
   /////////////////////////////////////////////////////////////////////////////

   public static NetworkInfo getNetworkInfo() {
      return get().getNetworkInfo();
   }

   public static String getPrimaryNetworkInterfaceName() {
      return get().getPrimaryNetworkInterfaceName();
   }

   public static String getSecondaryNetworkInterfaceName() {
      return get().getSecondaryNetworkInterfaceName();
   }

   public static String getPrimaryIP() {
      return getNetworkInfo().primaryIp;
   }

   public static String getSecondaryIP() {
      return getNetworkInfo().secondaryIp;
   }

   public static String getGateway() {
      return get().getGateway();
   }

   public static List<String> getInterfaces() {
      return get().getInterfaces();
   }

   public static List<String> getLocalMulticastInterfaces() {
      return get().getLocalMulticastInterfaces();
   }

   public static List<String> getDNS() {
      return get().getDNS();
   }

   public static List<String> getRoutingTable() {
      return get().getRoutingTable();
   }

   public static String getSSHD() {
      return get().getSSHD();
   }

   /////////////////////////////////////////////////////////////////////////////
   // Wireless utility methods
   /////////////////////////////////////////////////////////////////////////////

   public static boolean isWirelessSupported() {
      return get().isWirelessSupported();
   }

   public static boolean isWirelessEnabled() {
      return get().isWirelessEnabled();
   }

   public static String getWirelessState() {
      return get().getWirelessState();
   }

   public static String getWirelessSSID() {
      return get().getWirelessSSID();
   }

   public static String getWirelessBSSID() {
      return get().getWirelessBSSID();
   }

   public static String getWirelessSecurity() {
      return get().getWirelessSecurity();
   }

   public static int getWirelessChannel() {
      return get().getWirelessChannel();
   }

   public static int getWirelessNoise() {
      return get().getWirelessNoise();
   }

   public static int getWirelessRSSI() {
      return get().getWirelessRSSI();
   }

   public static void wirelessConnect(String SSID, String BSSID, String security, String key) throws IOException {
      get().wirelessConnect(SSID, BSSID, security, key);
   }

   public static void wirelessDisconnect() throws IOException {
      get().wirelessDisconnect();
   }

   public static List<Map<String, Object>> wirelessScanStart(int timeout) throws IOException {
      return get().wirelessScanStart(timeout);
   }

   public static void wirelessScanEnd() throws IOException {
      get().wirelessScanEnd();
   }

   /////////////////////////////////////////////////////////////////////////////
   // System bootstrapping methods
   /////////////////////////////////////////////////////////////////////////////

   public static Collection<? extends BootstrapModule> getBootstrapModules() {
      return get().getBootstrapModules();
   }

   public static Collection<Class<? extends BootstrapModule>> getBootstrapModuleClasses() {
      return get().getBootstrapModuleClasses();
   }

   public static Collection<? extends Module> getApplicationModules() {
      return get().getApplicationModules();
   }

   public static Collection<Class<? extends Module>> getApplicationModuleClasses() {
      return get().getApplicationModuleClasses();
   }

   /////////////////////////////////////////////////////////////////////////////
   // System utility methods
   /////////////////////////////////////////////////////////////////////////////

   public static OsType getOsType() {
      return OS;
   }

   public static boolean isMac() {
      return OS == OsType.MAC;
   }

   public static boolean isLinux() {
      return OS == OsType.LINUX;
   }

   public static boolean isWindows() {
      return OS == OsType.WINDOWS;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Debugging
   /////////////////////////////////////////////////////////////////////////////

   public static boolean isInDebugMode() {
      return get().isInDebugMode();
   }

   public static String getSyslog() {
      return get().getSyslog();
   }

   public static String getBootlog() {
      return get().getBootlog();
   }

   public static String getAgentDb() {
      return get().getAgentDb();
   }

   public static String getFiles(List<String> paths) {
      return get().getFiles(paths);
   }

   public static String getProcesses() {
      return get().getProcesses();
   }

   public static String getLoad() {
      return get().getLoad();
   }

   /////////////////////////////////////////////////////////////////////////////
   // Internal implementation details
   /////////////////////////////////////////////////////////////////////////////

   private static IrisHalInternal get() {
      IrisHalInternal result = internal;
      if (result == null) {
         result = create();
      }

      return result;
   }

   private static IrisHalInternal create() {
      synchronized (STARTUP_LOCK) {
         IrisHalInternal startup = internal;
         if (startup != null) {
            throw new IllegalStateException("iris hal already started");
         }

         // Route JUL logging messages through slf4j
         /*
          * Does not work in combined openiris build.
         SLF4JBridgeHandler.removeHandlersForRootLogger();
         SLF4JBridgeHandler.install();
         */

         startup = loadIrisHAL();
         internal = startup;

         return startup;
      }
   }

   private static IrisHalInternal loadIrisHAL() {
      try {
         Class<?> halClazz = Class.forName("com.iris.agent.hal.IrisHalImpl");
         Object halObject = halClazz.newInstance();
         if (!(halObject instanceof IrisHalInternal)) {
            throw new Exception();
         }

         return (IrisHalInternal)halObject;
      } catch (Exception ex) {
         throw new IllegalStateException("FATAL!!! No Iris HAL implementation found.", ex);
      }
   }

   public static final class NetworkInfo {
      public final @Nullable NetworkInterface primary;
      public final String primaryInterface;
      public final String primaryInterfaceType;
      public final String primaryIp;
      public final String primaryNetmask;

      public final @Nullable NetworkInterface secondary;
      public final String secondaryInterface;
      public final String secondaryInterfaceType;
      public final String secondaryIp;
      public final String secondaryNetmask;

      public NetworkInfo(
            @Nullable NetworkInterface primary, String primaryInterface, String primaryInterfaceType, 
            String primaryIp, String primaryNetmask,

            @Nullable NetworkInterface secondary, String secondaryInterface, String secondaryInterfaceType, 
            String secondaryIp, String secondaryNetmask) {
         this.primary = primary;
         this.primaryInterface = primaryInterface;
         this.primaryInterfaceType = primaryInterfaceType;
         this.primaryIp = primaryIp;
         this.primaryNetmask = primaryNetmask;

         this.secondary = secondary;
         this.secondaryInterface = secondaryInterface;
         this.secondaryInterfaceType = secondaryInterfaceType;
         this.secondaryIp = secondaryIp;
         this.secondaryNetmask = secondaryNetmask;
      }
   }



}

