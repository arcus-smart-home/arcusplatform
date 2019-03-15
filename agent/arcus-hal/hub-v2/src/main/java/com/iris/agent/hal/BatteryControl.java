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

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.hal.BatteryStateListener;
import com.iris.agent.storage.FileContentListener;
import com.iris.agent.storage.FileContentMonitor;
import com.iris.agent.storage.StorageService;
import com.iris.messages.capability.HubPowerCapability;

class BatteryControl {
   private static final Logger log = LoggerFactory.getLogger(BatteryControl.class);

   public static final double DEFAULT_VOLTAGE = 6.2;
   public static final double DEFAULT_LEVEL = 100.0;

   public static final String BATTERY_MODE = "tmp:///battery_on";
   public static final String BATTERY_VOLTAGE = "tmp:///battery_voltage";
   public static final String BATTERY_LEVEL = "tmp:///battery_level";
   private static final double BATTERY_LOG_DELTA = 0.05;

   private FileContentMonitor monitorBatteryOn;
   private FileContentMonitor monitorBatteryVoltage;
   private FileContentMonitor monitorBatteryLevel;

   private boolean isBatteryPowered = false;
   private double actualBatteryVoltage = DEFAULT_VOLTAGE;
   private double reportedBatteryVoltage = DEFAULT_VOLTAGE;
   private double actualBatteryLevel = DEFAULT_LEVEL;
   private double reportedBatteryLevel = DEFAULT_LEVEL;

   private static final CopyOnWriteArraySet<BatteryStateListener> listeners = new CopyOnWriteArraySet<>();

   private BatteryControl() throws IOException {
      this.monitorBatteryOn = StorageService.getFileMonitor(URI.create(BATTERY_MODE));
      this.monitorBatteryOn.addListener(new BatteryOnMonitor());

      this.monitorBatteryVoltage = StorageService.getFileMonitor(URI.create(BATTERY_VOLTAGE));
      this.monitorBatteryVoltage.addListener(new BatteryVoltageMonitor());

      this.monitorBatteryLevel = StorageService.getFileMonitor(URI.create(BATTERY_LEVEL));
      this.monitorBatteryLevel.addListener(new BatteryLevelMonitor());

      try {
         this.isBatteryPowered = StorageService.getFile(BATTERY_MODE).exists();
      } catch (Exception ex) {
      }
   }

   public static final BatteryControl create() {
      try {
         return new BatteryControl();
      } catch (IOException ex) {
         log.warn("could not install battery state monitor: {}", ex.getMessage(), ex);
         throw new RuntimeException(ex);
      }
   }

   public void shutdown() {
      monitorBatteryOn.cancel();
      monitorBatteryVoltage.cancel();
      monitorBatteryLevel.cancel();
   }

   public boolean isBatteryPowered() {
      return isBatteryPowered;
   }

   public double getBatteryVoltage() {
      return reportedBatteryVoltage;
   }

   public int getBatteryLevel() {
      return (int)reportedBatteryLevel;
   }

   public String getPowerSource() {
       if (isBatteryPowered) {
          return HubPowerCapability.SOURCE_BATTERY;
       } else {
          return HubPowerCapability.SOURCE_MAINS;
       }
   }

   public void addBatteryStateListener(BatteryStateListener listener) {
      listeners.add(listener);
      listener.batteryStateChanged(isBatteryPowered, isBatteryPowered);
   }

   public void removeBatteryStateListener(BatteryStateListener listener) {
      listeners.remove(listener);
   }

   private final class BatteryOnMonitor implements FileContentListener {
      @Override
      public void fileContentsModified(FileContentMonitor monitor) {
         boolean oldState = isBatteryPowered;
         isBatteryPowered = monitor.getContents() != null;

         log.debug("battery mode changed: {}", isBatteryPowered);
         for (BatteryStateListener listener : listeners) {
            listener.batteryStateChanged(oldState, isBatteryPowered);
         }
      }
   }

   private final class BatteryVoltageMonitor implements FileContentListener {
      @Override
      public void fileContentsModified(FileContentMonitor monitor) {
         try {
            String contents = monitor.getContents();
            if (contents != null) {
               actualBatteryVoltage = Double.parseDouble(contents.trim());
            } else {
               actualBatteryVoltage = DEFAULT_VOLTAGE;
            }

            // Only report battery voltage change if > +/- X%
            double voltageDelta = DEFAULT_VOLTAGE * BATTERY_LOG_DELTA;
            if ((actualBatteryVoltage > (reportedBatteryVoltage + voltageDelta)) ||
                (actualBatteryVoltage < (reportedBatteryVoltage - voltageDelta))) {
               log.debug("battery voltage changed: {}", actualBatteryVoltage);

               double oldReportedVoltage = reportedBatteryVoltage;
               reportedBatteryVoltage = actualBatteryVoltage;
               for (BatteryStateListener listener : listeners) {
                  listener.batteryVoltageChanged(oldReportedVoltage, reportedBatteryVoltage);
               }
            }
         } catch (Exception ex) {
            if (log.isTraceEnabled()) {
               log.warn("could not read battery voltage: {}", ex.getMessage(), ex);
            } else {
               log.warn("could not read battery voltage: {}", ex.getMessage());
            }

            reportedBatteryVoltage = DEFAULT_VOLTAGE;
            actualBatteryVoltage = DEFAULT_VOLTAGE;
         }
      }
   }

   private final class BatteryLevelMonitor implements FileContentListener {
      @Override
      public void fileContentsModified(FileContentMonitor monitor) {
         try {
            String contents = monitor.getContents();
            if (contents != null) {
               actualBatteryLevel = Double.parseDouble(contents.trim());
            } else {
               actualBatteryLevel = DEFAULT_LEVEL;
            }

            // Only report battery level change if > +/- X%
            double levelDelta = DEFAULT_LEVEL * BATTERY_LOG_DELTA;
            if ((actualBatteryLevel > (reportedBatteryLevel + levelDelta)) ||
		          (actualBatteryLevel < (reportedBatteryLevel - levelDelta))) {
               log.debug("battery level changed: {}", actualBatteryLevel);
               double oldReportedBatteryLevel = actualBatteryLevel;
               reportedBatteryLevel = actualBatteryLevel;
               for (BatteryStateListener listener : listeners) {
                  listener.batteryLevelChanged(oldReportedBatteryLevel, reportedBatteryLevel);
               }
            }
         } catch (Exception ex) {
            if (log.isTraceEnabled()) {
               log.warn("could not read battery level: {}", ex.getMessage(), ex);
            } else {
               log.warn("could not read battery level: {}", ex.getMessage());
            }

            actualBatteryLevel = DEFAULT_LEVEL;
            reportedBatteryLevel = DEFAULT_LEVEL;
         }
      }
   }
}

