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

public enum LEDState {
      UNKNOWN,
      
      
      // V2
      BOOT_POST,
      BOOT_LINUX,
      BOOT_AGENT,
      UNASSOC_CONN_PRI,
      UNASSOC_CONN_PRI_BATT,
      UNASSOC_CONN_BACKUP,
      UNASSOC_CONN_BACKUP_BATT,
      UNASSOC_DISCONN,
      UNASSOC_DISCONN_BATT,
      ASSOC_CONN_PRI,
      ASSOC_CONN_PRI_BATT,
      ASSOC_CONN_BACKUP,
      ASSOC_CONN_BACKUP_BATT,
      ASSOC_DISCONN,
      ASSOC_DISCONN_BATT,
      PAIRING_CONN_PRI,
      PAIRING_CONN_PRI_BATT,
      PAIRING_CONN_BACKUP,
      PAIRING_CONN_BACKUP_BATT,
      PAIRING_DISCONN,
      PAIRING_DISCONN_BATT,
      UPGRADE_DECRYPT,
      UPGRADE_DECRYPT_ERR,
      UPGRADE_UNPACK,
      UPGRADE_UNPACK_ERR,
      UPGRADE_BOOTLOADER,
      UPGRADE_BOOTLOADER_ERR,
      UPGRADE_KERNEL,
      UPGRADE_KERNEL_ERR,
      UPGRADE_ROOTFS,
      UPGRADE_ROOTFS_ERR,
      UPGRADE_ZIGBEE,
      UPGRADE_ZIGBEE_ERR,
      UPGRADE_ZWAVE,
      UPGRADE_ZWAVE_ERR,
      UPGRADE_BTE,
      UPGRADE_BTE_ERR,
      ALL_ON,
      ALL_OFF,
      
      // V3
      FIRST_BOOTUP,
      ETHRNET_INSERTED,
      WIFI_IN_PROGRESS,
      WIFI_SECCESS,
      WIFI_FAILURE,
      INTERNET_IN_PROGRESS,
      INTERNET_SUCCESS,
      INTERNET_FAILURE,
      CLOUD_IN_PROGRESS,
      CLOUD_SUCCESS,
      CLOUD_FAILURE,
      REGISTER_IN_PROGRESS,
      REGISTER_SUCCESS,
      REGISTER_FAILURE,
      DEVICE_PAIRED,
      DEVICE_REMOVED,
      BUTTON_PRESS_NORMAL,
      BUTTON_PRESS_STATUS,
      BUTTON_PRESS_BACKUP,
      BUTTON_PRESS_BACKUP_BATT,
      BUTTON_PRESS_BACKUP_OFFLINE,
      BUTTON_PRESS_BATTERY,
      BUTTON_PRESS_OFFLINE,
      BUTTON_PRESS_OFFLINE_BATT,
      BUTTON_PRESS_WIFI_RECONNECT,
      DEREGISTERING,
      FACTORY_RESET_ACK,
      BUTTON_FACTOY_DEFAULT,
      BUTTON_REBOOT,
      TURNING_OFF,
      ALARM_GRACE_ENTER,
      ALARM_GRACE_EXIT,
      ALARM_OFF,
      ALARM_ON,
      ALARM_PARTIAL,
      ALARM_FAILURE,
      ALARMING_SECURITY_BATT,
      ALARMING_SECURITY,
      ALARMING_PANIC_BATT,
      ALARMING_PANIC,
      ALARMING_SMOKE_BATT,
      ALARMING_SMOKE,
      ALARMING_CO_BATT,
      ALARMING_CO,
      ALARMING_LEAKING_BATT,
      ALARMING_LEAKING,
      
}

