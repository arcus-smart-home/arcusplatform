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

public enum SounderMode {  
// Base Sounds/No Sound
    NO_SOUND,
    UNKNOWN,
    USER_DEFINED,
 
// V2 Sounds.
    ARMING,
    ARMED,
    CHIME,
    INTRUDER,
    LOW_BATTERY,
    PAIRED,
    SAFETY,
    UNPAIRED,

// V3 Sounds
    BUTTON_PRESS,
    CARE_ALARM,
    CO_ALARM,
    DOOR_CHIME_1,
    DOOR_CHIME_2,
    DOOR_CHIME_3,
    DOOR_CHIME_4,
    DOOR_CHIME_5,
    DOOR_CHIME_6,
    DOOR_CHIME_7,
    DOOR_CHIME_8,
    DOOR_CHIME_9,
    DOOR_CHIME_10,
    DOUBLE_BUTTON_PRESS,
    FAILED,
    PANIC_ALARM,
    SMOKE_ALARM,
    STARTUP,
    SECURITY_ALARM,
    SUCCESS_DISARM,
    SUCCESS_REMOVAL,
    SUCCESS_REBOOT,
    SUCCESS_TRIPLE,
    SUCCESS_SINGLE,
    WATER_LEAK_ALARM,

// V3 Voices
    AWESOME_ALL_SETUP,
    BUTTON_PRESS_BACKUP,
    BUTTON_PRESS_BACKUP_BATT,
    BUTTON_PRESS_BACKUP_OFFLINE,
    BUTTON_PRESS_BATTERY,
    BUTTON_PRESS_NORMAL,
    BUTTON_PRESS_OFFLINE,
    BUTTON_PRESS_OFFLINE_BATT,
    BUTTON_PRESS_STATUS,
    BUTTON_PRESS_NOPLACE,
    CARE_CANCELLED,
    CARE_TRIGGERED,
    CO_ALARM_CANCELLED,
    CO_TRIGGERED_MONITORING_NOTIFIED,
    CO_TRIGGERED,
    CONNECTED_WIFI,
    DEVICE_REMOVED,
    EVERYTHING_GREAT,
    FACTORY_RESET_OFFLINE,
    FIRMWARE_UPDATE_NEEDED,
    FIRST_BOOTUP,
    GREATNEWS_CONNECTED_CLOUD,
    GREATNEWS_INTERNET_CONNECTED,
    HUB_FACTORY_RESET,
    HUB_REMOVED,
    PANIC_ALARM_CANCELLED,
    PANIC_TRIGGERED_MONITORING_NOTIFIED,
    REBOOT_HUB,
    REGISTER_SUCCESS,
    SECURITY_ALARM_OFF,
    SECURITY_ALARM_ON,
    SECURITY_ALARM_PARTIAL,
    SECURITY_ALARM_TRIGGERED,
    SECURITY_ALARM_FAILED,
    SECURITY_TRIGGERED_MONITORING_NOTIFIED,
    SMOKE_ALARM_CANCELLED,
    SMOKE_ALARM_TRIGGERED,
    SMOKE_TRIGGERED_MONITORING_NOTIFIED,
    SUCCESS_DEVICE_PAIRED,
    TRIGGER_15_SECONDS,
    TURNING_OFF,
    WATER_LEAK_ALARM_CANCELLED,
    WATER_LEAK_DETECTED,
    WIFI_CONNECTION_ISSUE,

    // Arming grace period voices - just use ARMING sound in v2 case
    ARMING_GRACE_EXIT,
    ARMING_GRACE_EXIT_PARTIAL,
    ARMING_GRACE_ENTER
}


