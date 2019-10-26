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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.Inet4Address;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.iris.agent.zwave.ZWaveController;
import com.iris.agent.zwave.ZWaveLocalProcessing;
import com.iris.agent.zwave.ZWaveLocalProcessingDefault;
import com.iris.agent.zwave.ZWaveLocalProcessingNoop;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.iris.agent.hal.sound.SounderControl;
import com.iris.agent.controller.spy.SpyController;
import com.iris.agent.alarm.AlarmController;
import com.iris.agent.lifecycle.LifeCycle;
import com.iris.agent.lifecycle.LifeCycleService;
import com.iris.agent.reflex.ReflexController;
import com.iris.agent.reflex.ReflexLocalProcessing;
import com.iris.agent.spy.SpyService;
import com.iris.agent.storage.StorageService;
import com.iris.agent.util.IpUtil;
import com.iris.agent.util.ThreadUtils;
import com.iris.agent.zigbee.ZigbeeController;
import com.iris.agent.zigbee.ZigbeeEmberDriverFactory;
import com.iris.agent.zigbee.ZigbeeDriverFactory;
import com.iris.agent.zigbee.ZigbeeLocalProcessing;
import com.iris.agent.zigbee.ZigbeeLocalProcessingDefault;
import com.iris.agent.zigbee.ZigbeeLocalProcessingNoop;
import com.iris.messages.capability.HubAdvancedCapability;
import com.iris.messages.capability.HubNetworkCapability;
import com.iris.messages.capability.HubPowerCapability;
import com.iris.agent.util.MfgBatchInfo;

/**
 * Implementation of Iris HAL for the Iris V2 Hub.
 */
public final class IrisHalImpl extends AbstractIrisHalCommon {
   private static final Logger log = LoggerFactory.getLogger(IrisHalImpl.class);

   private static final long NETWORK_UPDATE_MINIMUM = TimeUnit.NANOSECONDS.convert(5, TimeUnit.SECONDS);
   private static volatile @Nullable IrisHal.NetworkInfo currentNetworkInfo = null;
   private static long lastNetworkInfoFetch = Long.MIN_VALUE;

   // NOTE: These exit codes must match up with what is in the
   // hub-v2 startup script. If you change these values be sure
   // to change the startup script as well.
   private static final int EXIT_CODE_RESTART = 80;
   private static final int EXIT_CODE_REBOOT = 81;
   private static final int EXIT_CODE_FIRMWARE_UPDATE = 82;
   private static final int EXIT_CODE_AGENT_UPDATE = 83;
   private static final int EXIT_CODE_FACTORY_RESET = 84;
   private static final int EXIT_CODE_RESTART_SELF_CHECK = 85;

   private static final String FIRMWARE_VERSION = "tmp:///version";
   private static final String MODEL_NAME = "tmp:///mfg/config/model";
   private static final String CUSTOMER_NAME = "tmp:///mfg/config/customer";
   private static final String BATCH_INFO = "tmp:///mfg/config/batchNo";
   private static final String HUBID = "tmp:///mfg/config/hubID";
   private static final String HARDWARE_VERSION = "tmp:///mfg/config/hwVer";
   private static final String FLASH_SIZE = "tmp:///mfg/config/hwFlashSize";
   private static final String MACADDR = "tmp:///mfg/config/macAddr1";
   private static final String CERT = "tmp:///mfg/certs/";
   private static final String KEY = "tmp:///mfg/keys/";
   private static final String DEBUG_FILE = "tmp:///debug";
   private static final String LTE_INTERFACE_FILE = "tmp:///lte_interface";
   private static final String PRI_INTERFACE_FILE = "tmp:///pri_interface";

   // These will vary by platform as well, so hubOS will create a link to exact file
   private static final String ZIGBEE_RESET_GPIO = "/tmp/io/zigbeeReset";
   private static final String ZWAVE_RESET_GPIO = "/tmp/io/zwaveReset";
   private static final String BLUETOOTH_RESET_GPIO = "/tmp/io/bleReset";

   private static final String NET_DNS = "/etc/resolv.conf";
   private static final String SSHD = "/usr/sbin/dropbear";
   private static final String HUBOS_SYSLOG = "/var/volatile/log/messages";
   private static final String HUBOS_BOOTLOG = "/var/volatile/log/dmesg";
   private static final String HUBAGENT_DB = "/data/iris/db/iris.db";
   private static final String RAW_MODEL_FILE = "/tmp/mfg/config/model";
   private static final String UNKNOWN = "unknown";

   // The watchdog max period can depend on hardware
   private static final long HUBV2_WATCHDOG_MAX = 300;
   private static final long HUBV3_WATCHDOG_MAX = 120;

   private String firmwareVersion;
   private String model = null;
   private String customer;
   private String mfgBatchNumber;
   private Date mfgDate;
   private int  mfgFactoryID;
   private String hubId;
   private String hardwareVersion;
   private Long flashSize;
   private String macAddr;
   private String agentVersion;

   private boolean isInDebugMode = false;
   private long lastDebugModeCheck = System.nanoTime();

   // NOTE: This variable is set true if the environment variable
   // IRIS_AGENT_HUBV2_FAKE is defined. This is used when running the Hub V2
   // HAL on a normal. If the value is true then many Hub V2 pieces of hardware
   // are disabled because they are presumably not available on the machine
   // that the agent is actually running on.
   static boolean isFakeHub;

   private LEDControl ledControl;
   private SounderControl sounderControl;
   private ButtonIrisControl buttonControl;
   private BatteryControl batteryControl;
   private WatchdogControl watchdogControl;
   private WirelessControl wirelessControl;
   private List<String> zipFilelist = new ArrayList<String>();

   public IrisHalImpl() {
      watchdogControl = WatchdogControl.create();
   }

   @Override
   public void start(File base, Set<File> configs) {
      IrisHalImpl.isFakeHub = System.getenv("IRIS_AGENT_HUBV2_FAKE") != null;

      super.startInitialServices(base, configs);

      ledControl = LEDControl.create();
      sounderControl = SounderControl.create();
      batteryControl = BatteryControl.create();

      this.firmwareVersion = stripVersionV(readFirmwareFile(FIRMWARE_VERSION, UNKNOWN));
      this.model = readFirmwareFile(MODEL_NAME, UNKNOWN);
      this.customer = readFirmwareFile(CUSTOMER_NAME, UNKNOWN);
      this.mfgBatchNumber = readFirmwareFile(BATCH_INFO, UNKNOWN);
      this.hardwareVersion = stripVersionV(readFirmwareFile(HARDWARE_VERSION, UNKNOWN));
      String flashSizeStr = readFirmwareFile(FLASH_SIZE);
      if (flashSizeStr == null) {
         this.flashSize = (long)0;
      } else {
         this.flashSize = Long.parseLong(flashSizeStr);
      }
      this.macAddr = readFirmwareFile(MACADDR, UNKNOWN);

      // Translate manufacturing data
      MfgBatchInfo batchInfo = new MfgBatchInfo(this.mfgBatchNumber);
      this.mfgDate = batchInfo.getMfgDate();
      this.mfgFactoryID = batchInfo.getMfgFactoryID();

      File agentConfDir = new File(baseAgentDir, "conf");
      File agentVersionFile = new File(agentConfDir, "agent.version");
      this.agentVersion = stripVersionVNoSuffix(readFirmwareFile(agentVersionFile, UNKNOWN));

      String readHubId = readFirmwareFile(HUBID);
      if (readHubId == null) {
         throw new RuntimeException("hub id cannot be found");
      }

      this.hubId = readHubId.trim();

      // Wireless support depends on model
      if (this.isWirelessSupported()) {
         wirelessControl = WirelessControl.create();
         wirelessControl.start();
      }
      
      if (this.hasButton()) {
          buttonControl = ButtonIrisControl.create();
      }
 
      super.startExtraServices();
      log.info("iris hub settings: model={}, vendor={}, fwVer={}, hwVer={}, agentVer={}, mac={}, hubId={}", this.model, this.customer, this.firmwareVersion, this.hardwareVersion, this.agentVersion, this.macAddr, this.hubId);

      LifeCycleService.setState(LifeCycle.STARTING_UP);
   }

   @Nullable
   private static String readFirmwareFile(String file) {
      try (InputStream is = StorageService.getInputStream(URI.create(file))) {
         return IOUtils.toString(is).trim();
      } catch (Exception ex) {
         log.warn("could not read {}: {}", file, ex.getMessage());
         return null;
      }
   }

   private static String readFirmwareFile(String file, String def) {
      try (InputStream is = StorageService.getInputStream(URI.create(file))) {
         return IOUtils.toString(is).trim();
      } catch (Exception ex) {
         log.debug("could not read {}: {}", file, ex.getMessage());
         return def;
      }
   }

   private static String readFirmwareFile(File file, String def) {
      try (InputStream is = StorageService.getInputStream(file)) {
         return IOUtils.toString(is).trim();
      } catch (Throwable ex) {
         log.debug("could not read {}: {}", file, ex.getMessage());
         return def;
      }
   }

   private String stripVersionV(String version) {
      String vers = version.startsWith("v") ? version.substring(1) : version;
      return vers;
   }

   private String stripVersionVNoSuffix(String version) {
      String vers = version.startsWith("v") ? version.substring(1) : version;

      int idx = vers.indexOf('-');
      // Allow special build (non-SNAPSHOT) suffix values to be displayed
      return ((idx >= 0) && vers.contains("SNAPSHOT")) ? vers.substring(0,idx) : vers;
   }

   @Override
   public boolean isInDebugMode() {
      long diff = System.nanoTime() - lastDebugModeCheck;
      if (diff < 60000000000L) {
         return isInDebugMode;
      }

      isInDebugMode = StorageService.getFile(DEBUG_FILE).exists();
      return isInDebugMode;
   }

   @Override
   public void shutdown() {
      if (ledControl != null) {
         ledControl.shutdown();
      }

      if (batteryControl != null) {
         batteryControl.shutdown();
      }

      if (wirelessControl != null) {
         wirelessControl.shutdown();
      }
      
      if (buttonControl != null) {
          buttonControl.shutdown();
      }

      ledControl = null;
      batteryControl = null;
      wirelessControl = null;
      super.shutdown();
   }

   @Override
   protected void startStorageService(File base) {
      super.startStorageService(base);

      String tmpDir = System.getenv("IRIS_AGENT_HUBV2_TMPDIR");
      if (tmpDir == null) {
         tmpDir = "file:///tmp";
      }

      String dataDir = System.getenv("IRIS_AGENT_HUBV2_DATADIR");
      if (dataDir == null) {
         dataDir = "file:///data";
      }

      StorageService.addRootMapping("agent://", "file://" + base.getAbsolutePath());
      StorageService.addRootMapping("tmp://", tmpDir);
      StorageService.addRootMapping("file://", dataDir + "/iris/data");
      StorageService.addRootMapping("db://", dataDir + "/iris/db");
      StorageService.addRootMapping("data://", dataDir);
   }

   private static void exitWithFailsafe(final int code) {
      // Allow upto 5 minutes for the shutdown. If this thread is still running
      // after that amount of time then the process will be forcibly halted.
      Thread thr = new Thread(new Runnable() {
         @Override
         public void run() {
            ThreadUtils.sleep(5, TimeUnit.MINUTES);
            Runtime.getRuntime().halt(code);
         }
      });

      thr.setName("isfs");
      thr.setDaemon(true);
      thr.start();

      // The NOSONAR comment below disables the sonar warning for
      // this line of code. This line of code implements the
      // policy for restarting the agent.
      System.exit(code); // NOSONAR
   }

   @Override
   public void restart() {
      // The Hub V2 startup script will interpret this exit code
      // to mean restart the software. If you change this value
      // be sure to change the startup script as well.
      exitWithFailsafe(EXIT_CODE_RESTART); // NOSONAR
   }

   @Override
   public void reboot() {
      // The Hub V2 startup script will interpret this exit code
      // to mean reboot the system. If you change this value
      // be sure to change the startup script as well.
	   setSounderMode(SounderMode.REBOOT_HUB,1);
	   exitWithFailsafe(EXIT_CODE_REBOOT); // NOSONAR
   }

   @Override
   public void rebootAndSelfCheck() {
      // The Hub V2 startup script will interpret this exit code
      // to mean restart the software. If you change this value
      // be sure to change the startup script as well.
      exitWithFailsafe(EXIT_CODE_RESTART_SELF_CHECK); // NOSONAR
   }

   @Override
   public void factoryReset() {
      this.setLedState(LEDState.FACTORY_RESET_ACK);
      this.setSounderMode(SounderMode.HUB_FACTORY_RESET, 1);
      // Delay for a moment to make sure voice file plays completely on v3 hub...
      if (Model.isV3(model)) {
         ThreadUtils.sleep(5, TimeUnit.SECONDS);
      }
      // The Hub V2 startup script will interpret this exit code
      // to mean reboot the system. If you change this value
      // be sure to change the startup script as well.
      exitWithFailsafe(EXIT_CODE_FACTORY_RESET); // NOSONAR
   }

   @Override
   public File getFirmwareDownloadDir(String type) throws IOException {
      // The Hub V2 startup script depends on the directory used here
      // to find downloaded firmwares. If you change this value
      // be sure to change the startup script as well.
      File dst = StorageService.getFile("file:///tmp");
      dst.mkdirs();

      return dst;
   }

   @Override
   public void installHubFirmware(File file, String type, boolean force) throws IOException {
      File downloadDir = getFirmwareDownloadDir(type);

      switch (type) {
      case HubAdvancedCapability.FirmwareUpdateRequest.TYPE_FIRMWARE:
         File firmwareDst = new File(downloadDir, "hubOS.bin");
         if (!file.renameTo(firmwareDst)) {
            throw new IOException("could not rename file for installation: " + file + " to " + firmwareDst);
         }

         // The Hub V2 startup script will interpret this exit code
         // to mean install a new agent. If you change this value
         // be sure to change the startup script as well.
         //
         // The NOSONAR comment below disables the sonar warning for
         // this line of code. This line of code implements the
         // policy for rebooting the agent.
         exitWithFailsafe(EXIT_CODE_FIRMWARE_UPDATE); // NOSONAR
         break;

      case HubAdvancedCapability.FirmwareUpdateRequest.TYPE_AGENT:
         File agentDst = new File(downloadDir, "hubAgent.bin");
         if (!file.renameTo(agentDst)) {
            throw new IOException("could not rename file for installation: " + file + " to " + agentDst);
         }

         // The Hub V2 startup script will interpret this exit code
         // to mean install a new agent. If you change this value
         // be sure to change the startup script as well.
         //
         // The NOSONAR comment below disables the sonar warning for
         // this line of code. This line of code implements the
         // policy for rebooting the agent.
         exitWithFailsafe(EXIT_CODE_AGENT_UPDATE); // NOSONAR
         break;

      default:
         throw new IOException("unknown firmware update type: " + type);
      }
   }

   @Override
   public void startWatchdog() {
      String model = this.model;

      // HAL not initialized yet?
      if (model == null) {
         try (InputStream is = new FileInputStream(RAW_MODEL_FILE)) {
            model = IOUtils.toString(is).trim();
         } catch (Exception e) {
            model = "Unknown";
         }
      }
      // Max watchdog period will vary by model
      if (Model.isV3(model)) {
         watchdogControl.start(HUBV3_WATCHDOG_MAX);
      } else {
         watchdogControl.start(HUBV2_WATCHDOG_MAX);
      }
   }

   @Override
   public void shutdownWatchdog() {
      watchdogControl.shutdown(true);
   }

   @Override
   public void pokeWatchdog() {
      watchdogControl.poke();
   }

   @Override
   public LEDState getLedState() {
      return ledControl.get();
   }

   @Override
   public void setLedState(LEDState state) {
      ledControl.set(state);
   }

   @Override
   public void setLedState(LEDState state,int duration) {
      ledControl.set(state,duration);
   }

   @Override
   public void setSounderMode(SounderMode mode, int duration) {
      sounderControl.play(mode, duration);
   }

   @Override
   public void playSound(String url, int repeat) {
      sounderControl.play(url, repeat);
   }

   private static boolean gpioReset(String file, String reset, String normal) {
      // NOTE: Thread.sleep is considered by most to be poor programming
      //       practice. This code uses the method because the GPIO pin
      //       must be held in the reset state for approximately one
      //       second and this coded is executed so infrequently that
      //       avoiding the Thread.sleep is not worthwhile.

      try (Writer out = new FileWriter(file)) {
         IOUtils.write(reset, out);
         out.flush();
         Thread.sleep(1000);
         IOUtils.write(normal, out);
         out.flush();
         Thread.sleep(1000);
         return true;
      } catch (IOException ex) {
         if (log.isTraceEnabled()) {
            log.warn("gpio reset failed {}: {}", file, ex.getMessage(), ex);
         } else {
            log.warn("gpio reset failed {}: {}", file, ex.getMessage());
         }

         return false;
      } catch (InterruptedException ex) {
         return false;
      }
   }

   @Override
   public int getZWaveNvmCapacity() {
      if (isFakeHub) {
         return 0;
      }

      return 32 * 1024;
   }

   @Override
   public boolean resetZWaveChip() {
      if (isFakeHub || !Files.isRegularFile(Paths.get(ZWAVE_RESET_GPIO))) {
         log.debug("skipping z-wave reset because we are running on a fake hub");
         return false;
      }

      return gpioReset(ZWAVE_RESET_GPIO, "0", "1");
   }

   @Override
   public boolean resetZigbeeChip() {
      if (isFakeHub || !Files.isRegularFile(Paths.get(ZIGBEE_RESET_GPIO))) {
         log.debug("skipping zigbee reset because we are running on a fake hub");
         return false;
      }

      // Due to problems seen on the v3 Zigbee module, pull the reset line twice.
      //  Otherwise, can see issues where the chip returns an error after the initial reset
      if (Model.isV3(this.model)) {
         gpioReset(ZIGBEE_RESET_GPIO, "0", "1");
      }
      return gpioReset(ZIGBEE_RESET_GPIO, "0", "1");
   }

   @Override
   public boolean resetBluetoothChip() {
      if (isFakeHub || !Files.isRegularFile(Paths.get(BLUETOOTH_RESET_GPIO))) {
         log.debug("skipping bluetooth reset because we are running on a fake hub");
         return false;
      }

      return gpioReset(BLUETOOTH_RESET_GPIO, "0", "1");
   }

   @Override
   public boolean isZigbeeRtsCts() {
      // Only applies to IH200 hub - other hubs always have RTS/CTS
      if (Model.isV3(this.model)) {
         return true;
      }
      try {
         int hwVersion = Integer.parseInt(getHardwareVersion());
         return hwVersion >= 2;
      } catch (NumberFormatException ex) {
         return false;
      }
   }

   @Override
   public boolean isZigbeeXonXoff() {
      // Only applies to IH200 hub - other hubs never have XON/XOFF
      if (Model.isV3(this.model)) {
         return false;
      }
      try {
         int hwVersion = Integer.parseInt(getHardwareVersion());
         return hwVersion < 2;
      } catch (NumberFormatException ex) {
         return false;
      }
   }

   @Override
   public int getZigbeeBaudRate() {
      if (isZigbeeRtsCts()) {
         return 115200;
      }

      return 57600;
   }

   @Override
   public File getHubCertificateFile() {
      return StorageService.getFile(CERT + macAddr + ".crt");
   }

   @Override
   public File getHubKeyFile() {
      return StorageService.getFile(KEY + macAddr + ".key");
   }

   @Override
   public String getDataDiskName() {
      // Later hubs will use mmcblk1 as this is the default for eMMC in later kernels
      if (Model.isV3(this.model)) { 
         return "/dev/mmcblk2";
      }
      return "/dev/mmcblk0";
   }

   @Override
   public String getHubId() {
      return hubId;
   }

   @Override
   public String getAgentVersion() {
      return agentVersion;
   }

   @Override
   public String getVendor() {
      return customer;
   }

   @Override
   public String getModel() {
      return model;
   }

   @Override
   public String getSerialNumber() {
      return "1";
   }

   @Override
   public String getHardwareVersion() {
      return hardwareVersion;
   }

   @Override
   public Long getHardwareFlashSize() {
      return flashSize;
   }

   @Override
   public String getMacAddress() {
      return macAddr;
   }

   @Override
   public String getManufacturingInfo() {
      return customer;
   }

   @Override
   public String getManufacturingBatchNumber() {
      return mfgBatchNumber;
   }

   @Override
   public Date getManufacturingDate() {
      return mfgDate;
   }

   @Override
   public int getManufacturingFactoryID() {
      return mfgFactoryID;
   }

   @Override
   public String getOperatingSystemVersion() {
      return firmwareVersion;
   }

   @Override
   public String getBootloaderVersion() {
      return firmwareVersion;
   }

   @Override
   public boolean isBatteryPowered() {
      return batteryControl != null && batteryControl.isBatteryPowered();
   }

   @Override
   public double getBatteryVoltage() {
      return (batteryControl != null) ? batteryControl.getBatteryVoltage() : BatteryControl.DEFAULT_VOLTAGE;
   }

   @Override
   public int getBatteryLevel() {
      return (batteryControl != null) ? batteryControl.getBatteryLevel() : (int)BatteryControl.DEFAULT_LEVEL;
   }

   @Override
   public String getButtonState() {
       return buttonControl.getState();
   }
   
   @Override
   public int getButtonDuration() {
       return buttonControl.getDuration();
   }
   
   @Override
   public Long getButtonLastPressed() {
       return buttonControl.getLastPressed();
   }
   
   @Override
   public boolean hasButton() {
       return Model.isV3(this.model);
   }
   
   @Override
   public void addButtonListener(ButtonListener listener) {
       buttonControl.addButtonListener(listener);
   }

   @Override
   public void removeButtonListener(ButtonListener listener) {
       buttonControl.removeButtonListener(listener);
   }
   
   @Override
   public String getPowerSource() {
      return (batteryControl != null) ? batteryControl.getPowerSource() : HubPowerCapability.SOURCE_MAINS;
   }

   @Override
   public void addBatteryStateListener(BatteryStateListener listener) {
      batteryControl.addBatteryStateListener(listener);
   }

   @Override
   public void removeBatteryStateListener(BatteryStateListener listener) {
      batteryControl.removeBatteryStateListener(listener);
   }

   @Override
   public Class<? extends Module> getZWaveControllerModuleClass() {
      return ZWaveModuleHubV2.class;
   }

   @Override
   public Class<? extends Module> getZigbeeControllerModuleClass() {
      return ZigbeeModuleHubV2.class;
   }

   @Override
   public Class<? extends Module> getSercommControllerModuleClass() {
      return SercommModuleHubV2.class;
   }

   @Override
   public Class<? extends Module> get4gControllerModuleClass() {
      return FourGModuleHubV2.class;
   }

   @Override
   public Class<? extends Module> getReflexControllerModuleClass() {
      return ReflexModuleHubV2.class;
   }

   @Override
   public Class<? extends Module> getAlarmControllerModuleClass() {
      return AlarmModuleHubV2.class;
   }

   @Nullable
   @Override
   protected Class<? extends Module> getHueControllerModuleClass() {
      return HueModuleHubV2.class;
   }
   
   @Nullable
   @Override
   protected Class<? extends Module> getSpyModuleClass() {
   	return SpyModuleHubV2.class;
   }

   public static final class ZWaveModuleHubV2 extends AbstractModule {
      @Override
      protected void configure() {
         String disable = System.getenv("ZWAVE_DISABLE");
         if (disable != null) {
             bind(ZWaveLocalProcessing.class).to(ZWaveLocalProcessingNoop.class).asEagerSingleton();
            return;
         }

         String port = System.getenv("ZWAVE_PORT");
         if (port == null) {
            bind(String.class).annotatedWith(Names.named("iris.zwave.port")).toInstance("/dev/ttyO1");
         } else {
            bind(String.class).annotatedWith(Names.named("iris.zwave.port")).toInstance(port);
         }

//         bind(ZWaveDriverFactory.class).in(Singleton.class);
         bind(ZWaveController.class).in(Singleton.class);
         bind(ZWaveLocalProcessing.class).to(ZWaveLocalProcessingDefault.class).asEagerSingleton();
      }
   }

   public static final class ZigbeeModuleHubV2 extends AbstractModule {
      @Override
      protected void configure() {
         String disable = System.getenv("ZIGBEE_DISABLE");

         if (disable != null) {
            bind(ZigbeeLocalProcessing.class).to(ZigbeeLocalProcessingNoop.class).asEagerSingleton();
            return;
         }

         String port = System.getenv("ZIGBEE_PORT");
         if (port == null) {
            port = "/dev/ttyO2";
         }

         ZigbeeEmberDriverFactory factory = new ZigbeeEmberDriverFactory(port);
         bind(ZigbeeDriverFactory.class).toInstance(factory);
         bind(ZigbeeController.class).in(Singleton.class);
         bind(ZigbeeLocalProcessing.class).to(ZigbeeLocalProcessingDefault.class).asEagerSingleton();

      }
   }

   public static final class SercommModuleHubV2 extends AbstractModule {
      @Override
      protected void configure() {
         String disable = System.getenv("SERCOMM_DISABLE");
         /*
         if (disable != null) {
            bind(SercommLocalProcessing.class).to(SercommLocalProcessingNoop.class).asEagerSingleton();
            return;
         }

         bind(SercommCameraController.class).in(Singleton.class);
         bind(SercommLocalProcessing.class).to(SercommLocalProcessingDefault.class).asEagerSingleton();
         */
      }
   }

   public static final class FourGModuleHubV2 extends AbstractModule {
      @Override
      protected void configure() {
         String disable = System.getenv("FOURG_DISABLE");
         /*
         if (disable != null) {
            return;
         }

         bind(FourGController.class).in(Singleton.class);
         */
      }
   }

   public static final class ReflexModuleHubV2 extends AbstractModule {
      @Override
      protected void configure() {
         String disable = System.getenv("REFLEX_DISABLE");
         if (disable != null) {
            return;
         }

         bind(ReflexLocalProcessing.class).in(Singleton.class);
         bind(ReflexController.class).in(Singleton.class);
      }
   }

   public static final class AlarmModuleHubV2 extends AbstractModule {
      @Override
      protected void configure() {
         String disable = System.getenv("ALARM_DISABLE");
         if (disable != null) {
            return;
         }

         bind(AlarmController.class).in(Singleton.class);
      }
   }

   public static final class HueModuleHubV2 extends AbstractModule {
      @Override
      protected void configure() {
         String disable = System.getenv("HUE_DISABLE");
         if(disable != null) {
            return;
         }

         // bind(HueController.class).in(Singleton.class);
      }
   }
   
   public static final class SpyModuleHubV2 extends AbstractModule {
   	@Override
   	protected void configure() {
   		if (SpyService.INSTANCE.isActive()) {
   			bind(SpyController.class).in(Singleton.class);
   		}
   	}
   }

   /////////////////////////////////////////////////////////////////////////////
   // Network utility methods
   /////////////////////////////////////////////////////////////////////////////

   @Nullable
   private NetworkInterface getNetworkInterface(@Nullable String name) {
      if (name == null) {
         return null;
      }

      try {
         return NetworkInterface.getByName(name);
      } catch (SocketException e) {
         return null;
      }
   }

   @Nullable
   private final InterfaceAddress getNetworkInterfaceAddress(@Nullable NetworkInterface ni) {
      if (ni == null) {
         return null;
      }

      List<InterfaceAddress> addrs = ni.getInterfaceAddresses();
      if (addrs == null || addrs.isEmpty()) {
         return null;
      }

      // Prefer IPv4 addresses
      for (InterfaceAddress address : addrs) {
         if (address.getAddress() instanceof Inet4Address) {
            return address;
         }
      }

      // Use first IPv6 address if no others are present
      return addrs.get(0);
   }

   @Override
   public String getPrimaryNetworkInterfaceName() {
      return getPrimaryNetworkInterfaceName(getSecondaryNetworkInterfaceName());
   }

   private String getPrimaryNetworkInterfaceName(String secName) {
      if (isFakeHub) {
         String priName = System.getenv("IRIS_AGENT_PRIMARY_INTERFACE");
         if (priName == null) {
            priName = getGatewayNetworkInterfaceNameForFakeHub();
         }

         return (priName == null || priName.equals(secName)) ? "eth0" : priName;
      }

      // Check for actual interface if wireless is supported
      if (this.isWirelessSupported()) {
         String intf = readFirmwareFile(PRI_INTERFACE_FILE);
         if ((intf != null) && !intf.isEmpty()) {
            return intf;
         }
      }
      return "eth0";
   }

   @Override
   public String getSecondaryNetworkInterfaceName() {
      if (isFakeHub) {
         String secName = System.getenv("IRIS_AGENT_SECONDARY_INTERFACE");
         if (secName == null) {
            secName = getFourgNetworkInterfaceNameForFakeHub();
         }

         return (secName == null) ? "eth1" : secName;
      }

      // Will depend on the LTE dongle type, so hubOS will provide details
      return readFirmwareFile(LTE_INTERFACE_FILE, UNKNOWN);
   }

   @Override
   public IrisHal.NetworkInfo getNetworkInfo() {
      long curTime = System.nanoTime();

      IrisHal.NetworkInfo cn = currentNetworkInfo;
      if (cn != null && (lastNetworkInfoFetch == Long.MIN_VALUE || (curTime-lastNetworkInfoFetch) <= NETWORK_UPDATE_MINIMUM)) {
         return cn;
      }

      lastNetworkInfoFetch = curTime;
      String secondary = getSecondaryNetworkInterfaceName();
      String primary = getPrimaryNetworkInterfaceName(secondary);
      NetworkInterface priNi = getNetworkInterface(primary);
      NetworkInterface secNi = getNetworkInterface(secondary);

      InterfaceAddress priAddr = getNetworkInterfaceAddress(priNi);
      InterfaceAddress secAddr = getNetworkInterfaceAddress(secNi);
      currentNetworkInfo = new IrisHal.NetworkInfo(
            priNi,
            primary,
            ((primary.contains("eth")) ? HubNetworkCapability.TYPE_ETH : HubNetworkCapability.TYPE_WIFI),
            (priAddr != null) ? priAddr.getAddress().getHostAddress() : "0.0.0.0",
            (priAddr != null) ? IpUtil.genNetmask(priAddr.getNetworkPrefixLength()) : "0.0.0.0",
            secNi,
            secondary,
            HubNetworkCapability.TYPE_3G,
            (secAddr != null) ? secAddr.getAddress().getHostAddress() : "0.0.0.0",
            (secAddr != null) ? IpUtil.genNetmask(secAddr.getNetworkPrefixLength()) : "0.0.0.0");

      return currentNetworkInfo;
   }

   @Nullable
   private String getGatewayNetworkInterfaceNameForFakeHub() {
      return getNetworkInterfaceNameForFakeHub(getGatewayLine());
   }

   @Nullable
   private String getFourgNetworkInterfaceNameForFakeHub() {
      return getNetworkInterfaceNameForFakeHub(getFourgLineForFakeHub());
   }

   @Nullable
   private String getNetworkInterfaceNameForFakeHub(@Nullable String[] routingEntry) {
      if (IrisHal.isMac()) {
         if (routingEntry == null) {
            return null;
         }

         if (routingEntry.length > 5) {
            return routingEntry[5];
         }

         if (routingEntry.length > 3) {
            return routingEntry[3];
         }
      } else {
         if (routingEntry == null) {
            return null;
         }

         if (routingEntry.length > 7) {
            return routingEntry[7];
         }
      }

      return null;
   }

   @Nullable
   private String[] getGatewayLine() {
      return getRoutingTableLine(new Predicate<String>() {
         @Override
         public boolean apply(@Nullable String entry) {
            return (entry != null && (entry.startsWith("default") || entry.startsWith("0.0.0.0")));
         }
      });
   }

   @Nullable
   private String[] getFourgLineForFakeHub() {
      return getRoutingTableLine(new Predicate<String>() {
         @Override
         public boolean apply(@Nullable String entry) {
            return (entry != null && entry.startsWith("192.168.8."));
         }
      });
   }

   @Nullable
   private String[] getRoutingTableLine(Predicate<String> matches) {
      List<String> routes = getRoutingTable();
      if (routes.isEmpty()) {
         return null;
      }

      for (String entry : routes) {
         if (entry == null) {
            continue;
         }

         if (matches.apply(entry)) {
            return entry.split("\\s+");
         }
      }

      return null;
   }

   @Override
   public String getGateway() {
      String[] gateway = getGatewayLine();
      if (gateway != null && gateway.length > 1) {
         return gateway[1];
      }

      return UNKNOWN;
   }

   @Override
   public List<String> getDNS() {
      try {
         try (InputStream is = new FileInputStream(NET_DNS)) {
            return IOUtils.readLines(is, StandardCharsets.UTF_8);
         } catch (Exception e) {
            // ignore
         }
      } catch (Exception ex) {
         // ignore
      }

      return Collections.emptyList();
   }

   @Override
   public List<String> getRoutingTable() {
      final String program = "netstat -nr";
      Process pr = null;

      try {
         pr = Runtime.getRuntime().exec(program);
         pr.getErrorStream().close();
         pr.getOutputStream().close();

         return IOUtils.readLines(pr.getInputStream(), StandardCharsets.UTF_8);
      } catch (IOException e) {
         log.warn("Cannot execute '{}' because: [{}]", program, e);
      } finally {
         if (pr != null) {
            pr.destroy();
         }
      }

      return Collections.emptyList();
   }

   private List<NetworkInterface> getNetworkInterfaces(Predicate<? super NetworkInterface> filter) {
      List<NetworkInterface> interfaces = new ArrayList<>();
      try {
         Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
         if (nis != null) {
            for (NetworkInterface ni : Collections.list(nis)) {
               if (filter.apply(ni)) {
                  interfaces.add(ni);
               }
            }
         }
      } catch (SocketException e) {
         log.error("cannot get network interfaces: [{}]", e);
      }

      return interfaces;
   }

   @Override
   public List<String> getInterfaces() {
      return Lists.transform(getNetworkInterfaces(Predicates.alwaysTrue()), new Function<NetworkInterface, String>() {
         @Override
         public String apply(@Nullable NetworkInterface ni) {
            Preconditions.checkNotNull(ni);
            return ni.getDisplayName();
         }
      });
   }

   @Override
   public List<String> getLocalMulticastInterfaces() {
      if (isFakeHub) {
         Predicate<NetworkInterface> filter = new Predicate<NetworkInterface>() {
            @Override
            public boolean apply(@Nullable NetworkInterface ni) {
               try {
                  if (ni == null) {
                     return false;
                  }

                  String dn = ni.getDisplayName().toLowerCase().trim();
                  return ni.supportsMulticast() && !ni.isLoopback() && !ni.isPointToPoint() && 
                        !dn.startsWith("vmnet") && !dn.startsWith("vboxnet") &&
                        !dn.startsWith("vnic") && !dn.contains("virtual");
               } catch (Throwable th) {
                  return false;
               }
            }
         };

         return Lists.transform(getNetworkInterfaces(filter), new Function<NetworkInterface, String>() {
            @Override
            public String apply(@Nullable NetworkInterface ni) {
               Preconditions.checkNotNull(ni);
               return ni.getDisplayName();
            }
         });
      }

      String secName = getSecondaryNetworkInterfaceName();
      return Lists.newArrayList(getPrimaryNetworkInterfaceName(secName));
   }

   @Override
   public String getSSHD() {
      return SSHD;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Debugging and triage utility methods
   /////////////////////////////////////////////////////////////////////////////

   private String compressAndEncodeInput(InputStream is) throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (GZIPOutputStream gos = new GZIPOutputStream(baos)) {
         gos.write(IOUtils.toByteArray(is));
      } catch (Exception ex) {
         throw new IOException("could not compress data", ex);
      }

      return Base64.encodeBase64String(baos.toByteArray());
   }

   @Override
   public String getSyslog() {
      try (InputStream is = new FileInputStream(HUBOS_SYSLOG)) {
         return compressAndEncodeInput(is);
      } catch (Exception e) {
         log.warn("Cannot open {} because [{}]", HUBOS_SYSLOG, e);
      }
      return "";
   }

   @Override
   public String getBootlog() {
      try (InputStream is = new FileInputStream(HUBOS_BOOTLOG)) {
         return compressAndEncodeInput(is);
      } catch (Exception e) {
         log.warn("Cannot open {} because [{}]", HUBOS_BOOTLOG, e);
      }
      return "";
   }

   @Override
   public String getAgentDb() {
      try (InputStream is = new FileInputStream(HUBAGENT_DB)) {
         return compressAndEncodeInput(is);
      } catch (Exception e) {
         log.warn("Cannot open {} because [{}]", HUBAGENT_DB, e);
      }
      return "";
   }

   private void createFilelist(File file) {
      // Is this a file, then add directly
      if (file.isFile() || (file.isHidden() && !file.isDirectory())) {
         zipFilelist.add(file.getPath());
         return;
      }
      // Symlink?
      try {
         if (FileUtils.isSymlink(file)) {
            zipFilelist.add(Files.readSymbolicLink(file.toPath()).toString());
            return;
         }
      } catch (IOException e) {
         // Skip on error
         return;
      }
      // If directory, dig down
      if (file.isDirectory()) {
         String[] dirContents = file.list();
         for (String filename : dirContents) {
            createFilelist(new File(file, filename));
         }
      }
   }

   @Override
   public String getFiles(List<String> paths) {
      // Get list of all files
      zipFilelist.clear();
      for (String f : paths) {
         createFilelist(new File(f));
      }

      // Zip them up
      byte[] buf = new byte[1024];
      ByteArrayOutputStream bos = null;
      ZipOutputStream zos = null;

      try {
         bos = new ByteArrayOutputStream();
         zos = new ZipOutputStream(bos);
         FileInputStream in = null;

         for (String f : zipFilelist) {
            ZipEntry ze = new ZipEntry(f);
            zos.putNextEntry(ze);
            try {
               in = new FileInputStream(f);
               int len;
               while ((len = in.read(buf)) > 0) {
                  zos.write(buf, 0, len);
               }
            }
            catch (IOException e) {
               log.debug("Cannot zip up file {} due to an error [{}]", f, e.getMessage());
            }
            finally {
               in.close();
            }
         }
      }
      catch (IOException e) {
         log.warn("Error during zip file creation [{}]", e.getMessage());
      }
      finally {
         try {
            zos.close();
            return Base64.encodeBase64String(bos.toByteArray());
         }
         catch (IOException e) {
            log.warn("Cannot complete file zip because [{}]", e.getMessage());
         }
      }
      return "";
   }

   @Override
   public String getProcesses() {
      final String program = "ps";
      Process pr = null;

      try {
         pr = Runtime.getRuntime().exec(program);
         pr.getErrorStream().close();
         pr.getOutputStream().close();
         return compressAndEncodeInput(pr.getInputStream());
      } catch (IOException e) {
         log.warn("Cannot execute '{}' because: [{}]", program, e);
      } finally {
         if (pr != null) {
            pr.destroy();
         }
      }
      return "";
   }

   @Override
   public String getLoad() {
      final String program = "top -b -n1";
      Process pr = null;

      try {
         pr = Runtime.getRuntime().exec(program);
         pr.getErrorStream().close();
         pr.getOutputStream().close();
         return compressAndEncodeInput(pr.getInputStream());
      } catch (IOException e) {
         log.warn("Cannot execute '{}' because: [{}]", program, e);
      } finally {
         if (pr != null) {
            pr.destroy();
         }
      }
      return "";
   }

   /////////////////////////////////////////////////////////////////////////////
   // Wireless utility methods
   /////////////////////////////////////////////////////////////////////////////

   @Override
   public boolean isWirelessSupported() {
      // Only supported on some models
      return Model.isV3(this.model);
   }

   @Override
   public boolean isWirelessEnabled() {
      return wirelessControl.isWirelessEnabled();
   }

   @Override
   public String getWirelessState() {
      return wirelessControl.getWirelessState();
   }

   @Override
   public String getWirelessSSID() {
      return wirelessControl.getWirelessSSID();
   }

   @Override
   public String getWirelessBSSID() {
      return wirelessControl.getWirelessBSSID();
   }

   @Override
   public String getWirelessSecurity() {
      return wirelessControl.getWirelessSecurity();
   }

   @Override
   public int getWirelessChannel() {
      return wirelessControl.getWirelessChannel();
   }

   @Override
   public int getWirelessNoise() {
      return wirelessControl.getWirelessNoise();
   }

   @Override
   public int getWirelessRSSI() {
      return wirelessControl.getWirelessRSSI();
   }

   @Override
   public void wirelessConnect(String SSID, String BSSID, String security, String key) throws IOException {
      wirelessControl.wirelessConnect(SSID, BSSID, security, key);
   }

   @Override
   public void wirelessDisconnect() throws IOException {
      wirelessControl.wirelessDisconnect();
   }

   @Override
   public List<Map<String, Object>> wirelessScanStart(int timeout) throws IOException {
      return wirelessControl.wirelessScanStart(timeout);
   }

   @Override
   public void wirelessScanEnd() throws IOException {
      wirelessControl.wirelessScanEnd();
   }

}

