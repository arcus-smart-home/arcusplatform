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
package com.iris.platform.subsystem.placemonitor.offlinenotifications;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.xml.bind.annotation.XmlAccessType.FIELD;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.platform.subsystem.placemonitor.config.WatchedJAXBConfig;

@XmlRootElement
public class NotificationThresholdsConfig implements WatchedJAXBConfig {

   private static final Logger LOGGER = LoggerFactory.getLogger(NotificationThresholdsConfig.class);

   @XmlElement
   private ConfigDefaults configDefaults;

   @XmlElement(name = "deviceOverride")
   private List<DeviceOverride> deviceOverrides;

   private Map<String, DeviceOverride> deviceOverridesMap;

   @Override
   public void afterLoad() {
      LOGGER.info("Re-indexing notification thresholds config file");

      checkNotNull(configDefaults.getHubOfflineTimeoutSec());
      checkNotNull(configDefaults.getDeviceOfflineTimeoutSec());
      checkNotNull(configDefaults.getDeviceBatteryFull());
      checkNotNull(configDefaults.getDeviceBatteryFullClear());
      checkNotNull(configDefaults.getDeviceBatteryLow());
      checkNotNull(configDefaults.getDeviceBatteryLowClear());
      checkNotNull(configDefaults.getDeviceBatteryVeryLow());
      checkNotNull(configDefaults.getDeviceBatteryVeryLowClear());
      checkNotNull(configDefaults.getDeviceBatteryCritical());
      checkNotNull(configDefaults.getDeviceBatteryCriticalClear());
      checkNotNull(configDefaults.getDeviceBatteryDead());
      checkNotNull(configDefaults.getDeviceBatteryDeadClear());

      deviceOverridesMap = new HashMap<String, DeviceOverride>();
      for (DeviceOverride deviceOverride : deviceOverrides) {
         checkNotNull(deviceOverride.getProductId());
         deviceOverridesMap.put(deviceOverride.getProductId(), deviceOverride);
      }
   }

   public int getHubOfflineTimeoutSec() {
      return configDefaults.getHubOfflineTimeoutSec();
   }

   public int getDeviceOfflineTimeoutSec(String productId) {
      return getEffectiveValue(productId, ConfigDefaults::getDeviceOfflineTimeoutSec, DeviceOverride::getOfflineTimeoutSec);
   }

   public int getBatteryFull(String productId) {
      return getEffectiveValue(productId, ConfigDefaults::getDeviceBatteryFull, DeviceOverride::getBatteryFull);
   }

   public int getBatteryFullClear(String productId) {
      return getEffectiveValue(productId, ConfigDefaults::getDeviceBatteryFullClear, DeviceOverride::getBatteryFullClear);
   }

   public int getBatteryLow(String productId) {
      return getEffectiveValue(productId, ConfigDefaults::getDeviceBatteryLow, DeviceOverride::getBatteryLow);
   }

   public int getBatteryLowClear(String productId) {
      return getEffectiveValue(productId, ConfigDefaults::getDeviceBatteryLowClear, DeviceOverride::getBatteryLowClear);
   }

   public int getBatteryVeryLow(String productId) {
      return getEffectiveValue(productId, ConfigDefaults::getDeviceBatteryVeryLow, DeviceOverride::getBatteryVeryLow);
   }

   public int getBatteryVeryLowClear(String productId) {
      return getEffectiveValue(productId, ConfigDefaults::getDeviceBatteryVeryLowClear, DeviceOverride::getBatteryVeryLowClear);
   }

   public int getBatteryCritical(String productId) {
      return getEffectiveValue(productId, ConfigDefaults::getDeviceBatteryCritical, DeviceOverride::getBatteryCritical);
   }

   public int getBatteryCriticalClear(String productId) {
      return getEffectiveValue(productId, ConfigDefaults::getDeviceBatteryCriticalClear, DeviceOverride::getBatteryCriticalClear);
   }

   public int getBatteryDead(String productId) {
      return getEffectiveValue(productId, ConfigDefaults::getDeviceBatteryDead, DeviceOverride::getBatteryDead);
   }

   public int getBatteryDeadClear(String productId) {
      return getEffectiveValue(productId, ConfigDefaults::getDeviceBatteryDeadClear, DeviceOverride::getBatteryDeadClear);
   }

   private int getEffectiveValue(String productId, Function<ConfigDefaults, Integer> defaultValueFunction,
      Function<DeviceOverride, Integer> overrideValueFunction)
   {
      int effectiveValue = defaultValueFunction.apply(configDefaults);
      DeviceOverride deviceOverride = deviceOverridesMap.get(productId);
      if (deviceOverride != null) {
         Integer overrideValue = overrideValueFunction.apply(deviceOverride);
         if (overrideValue != null) {
            effectiveValue = overrideValue;
         }
      }
      return effectiveValue;
   }

   @XmlAccessorType(FIELD)
   private static class ConfigDefaults {
      @XmlAttribute
      private Integer hubOfflineTimeoutSec;
      @XmlAttribute
      private Integer deviceOfflineTimeoutSec;
      @XmlAttribute
      private Integer deviceBatteryFull;
      @XmlAttribute
      private Integer deviceBatteryFullClear;
      @XmlAttribute
      private Integer deviceBatteryLow;
      @XmlAttribute
      private Integer deviceBatteryLowClear;
      @XmlAttribute
      private Integer deviceBatteryVeryLow;
      @XmlAttribute
      private Integer deviceBatteryVeryLowClear;
      @XmlAttribute
      private Integer deviceBatteryCritical;
      @XmlAttribute
      private Integer deviceBatteryCriticalClear;
      @XmlAttribute
      private Integer deviceBatteryDead;
      @XmlAttribute
      private Integer deviceBatteryDeadClear;

      public Integer getHubOfflineTimeoutSec() {
         return hubOfflineTimeoutSec;
      }
      public Integer getDeviceOfflineTimeoutSec() {
         return deviceOfflineTimeoutSec;
      }
      public Integer getDeviceBatteryFull() {
         return deviceBatteryFull;
      }
      public Integer getDeviceBatteryFullClear() {
         return deviceBatteryFullClear;
      }
      public Integer getDeviceBatteryLow() {
         return deviceBatteryLow;
      }
      public Integer getDeviceBatteryLowClear() {
         return deviceBatteryLowClear;
      }
      public Integer getDeviceBatteryVeryLow() {
         return deviceBatteryVeryLow;
      }
      public Integer getDeviceBatteryVeryLowClear() {
         return deviceBatteryVeryLowClear;
      }
      public Integer getDeviceBatteryCritical() {
         return deviceBatteryCritical;
      }
      public Integer getDeviceBatteryCriticalClear() {
         return deviceBatteryCriticalClear;
      }
      public Integer getDeviceBatteryDead() {
         return deviceBatteryDead;
      }
      public Integer getDeviceBatteryDeadClear() {
         return deviceBatteryDeadClear;
      }
   }

   @XmlAccessorType(FIELD)
   private static class DeviceOverride {
      @XmlAttribute
      private String productId;
      @XmlAttribute
      private Integer offlineTimeoutSec;
      @XmlAttribute
      private Integer batteryFull;
      @XmlAttribute
      private Integer batteryFullClear;
      @XmlAttribute
      private Integer batteryLow;
      @XmlAttribute
      private Integer batteryLowClear;
      @XmlAttribute
      private Integer batteryVeryLow;
      @XmlAttribute
      private Integer batteryVeryLowClear;
      @XmlAttribute
      private Integer batteryCritical;
      @XmlAttribute
      private Integer batteryCriticalClear;
      @XmlAttribute
      private Integer batteryDead;
      @XmlAttribute
      private Integer batteryDeadClear;

      public String getProductId() {
         return productId;
      }
      public Integer getOfflineTimeoutSec() {
         return offlineTimeoutSec;
      }
      public Integer getBatteryFull() {
         return batteryFull;
      }
      public Integer getBatteryFullClear() {
         return batteryFullClear;
      }
      public Integer getBatteryLow() {
         return batteryLow;
      }
      public Integer getBatteryLowClear() {
         return batteryLowClear;
      }
      public Integer getBatteryVeryLow() {
         return batteryVeryLow;
      }
      public Integer getBatteryVeryLowClear() {
         return batteryVeryLowClear;
      }
      public Integer getBatteryCritical() {
         return batteryCritical;
      }
      public Integer getBatteryCriticalClear() {
         return batteryCriticalClear;
      }
      public Integer getBatteryDead() {
         return batteryDead;
      }
      public Integer getBatteryDeadClear() {
         return batteryDeadClear;
      }
   }
}

