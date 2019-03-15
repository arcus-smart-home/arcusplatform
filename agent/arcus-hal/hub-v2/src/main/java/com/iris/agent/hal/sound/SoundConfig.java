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
package com.iris.agent.hal.sound;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.hal.Model;
import com.iris.agent.hal.SounderMode;

public class SoundConfig {
    private static final Logger log = LoggerFactory.getLogger(SoundConfig.class);
   
    Map<SoundKey,SoundFile> sounds = new HashMap<>(); 
 
    public static final String SOUND_DIR = "/data/agent/conf/";
    
    
    private SoundConfig() {
        // V2 for V2 Sound Files
        putV2(SounderMode.NO_SOUND,      loadTone("nosound.txt"));
        putV2(SounderMode.ARMED,         loadTone("armed.txt"));
        putV2(SounderMode.ARMING,        loadTone("arming.txt"));
        putV2(SounderMode.CHIME,         loadTone("paired.txt"));
        putV2(SounderMode.INTRUDER,      loadTone("intruder.txt"));
        putV2(SounderMode.LOW_BATTERY,   loadTone("lowbattery.txt"));
        putV2(SounderMode.PAIRED,        loadTone("paired.txt"));
        putV2(SounderMode.SAFETY,        loadTone("safety.txt"));
        putV2(SounderMode.UNPAIRED,      loadTone("unpaired.txt"));
        
        // V3 Sound Definitions for V2 Hardware
        // That is are the closest equals for V2 hardware tones (until new ones are made).
        putV2(SounderMode.SUCCESS_TRIPLE,      loadTone("paired.txt"));
        putV2(SounderMode.SUCCESS_SINGLE,      loadTone("nosound.txt"));
        putV2(SounderMode.SUCCESS_REMOVAL,     loadTone("unpaired.txt"));
        putV2(SounderMode.STARTUP,             loadTone("nosound.txt"));
        putV2(SounderMode.FAILED,              loadTone("nosound.txt"));
        putV2(SounderMode.SUCCESS_DISARM,      loadTone("nosound.txt"));
        putV2(SounderMode.SECURITY_ALARM,      loadTone("intruder.txt"));
        putV2(SounderMode.PANIC_ALARM,         loadTone("intruder.txt"));
        putV2(SounderMode.SMOKE_ALARM,         loadTone("safety.txt"));
        putV2(SounderMode.CO_ALARM,            loadTone("safety.txt"));
        putV2(SounderMode.WATER_LEAK_ALARM,    loadTone("safety.txt"));
        putV2(SounderMode.CARE_ALARM,          loadTone("safety.txt"));
        putV2(SounderMode.BUTTON_PRESS,        loadTone("nosound.txt"));
        putV2(SounderMode.DOUBLE_BUTTON_PRESS, loadTone("nosound.txt"));
        putV2(SounderMode.SUCCESS_REBOOT,      loadTone("paired.txt"));
        putV2(SounderMode.DOOR_CHIME_1,        loadTone("paired.txt"));
        putV2(SounderMode.DOOR_CHIME_2,        loadTone("paired.txt"));
        putV2(SounderMode.DOOR_CHIME_3,        loadTone("paired.txt"));
        putV2(SounderMode.DOOR_CHIME_4,        loadTone("paired.txt"));
        putV2(SounderMode.DOOR_CHIME_5,        loadTone("paired.txt"));
        putV2(SounderMode.DOOR_CHIME_6,        loadTone("paired.txt"));
        putV2(SounderMode.DOOR_CHIME_7,        loadTone("paired.txt"));
        putV2(SounderMode.DOOR_CHIME_8,        loadTone("paired.txt"));
        putV2(SounderMode.DOOR_CHIME_9,        loadTone("paired.txt"));
        putV2(SounderMode.DOOR_CHIME_10,       loadTone("paired.txt"));
        putV2(SounderMode.ARMING_GRACE_EXIT,   loadTone("arming.txt"));
        putV2(SounderMode.ARMING_GRACE_EXIT_PARTIAL,   loadTone("arming.txt"));
        putV2(SounderMode.ARMING_GRACE_ENTER,   loadTone("arming.txt"));

        putV2(SounderMode.AWESOME_ALL_SETUP,                      loadTone("nosound.txt"));
        putV2(SounderMode.BUTTON_PRESS_BACKUP,                    loadTone("nosound.txt"));
        putV2(SounderMode.BUTTON_PRESS_BACKUP_BATT,               loadTone("nosound.txt"));
        putV2(SounderMode.BUTTON_PRESS_BACKUP_OFFLINE,            loadTone("nosound.txt"));
        putV2(SounderMode.BUTTON_PRESS_BATTERY,                   loadTone("nosound.txt"));
        putV2(SounderMode.BUTTON_PRESS_NORMAL,                    loadTone("nosound.txt"));
        putV2(SounderMode.BUTTON_PRESS_OFFLINE,                   loadTone("nosound.txt"));
        putV2(SounderMode.BUTTON_PRESS_OFFLINE_BATT,              loadTone("nosound.txt"));
        putV2(SounderMode.CARE_CANCELLED,                         loadTone("nosound.txt"));
        putV2(SounderMode.CARE_TRIGGERED,                         loadTone("safety.txt"));
        putV2(SounderMode.CO_ALARM_CANCELLED,                     loadTone("nosound.txt"));
        putV2(SounderMode.CO_TRIGGERED_MONITORING_NOTIFIED,       loadTone("safety.txt"));
        putV2(SounderMode.CO_TRIGGERED,                           loadTone("safety.txt"));
        putV2(SounderMode.CONNECTED_WIFI,                         loadTone("nosound.txt"));
        putV2(SounderMode.DEVICE_REMOVED,                         loadTone("unpaired.txt"));
        putV2(SounderMode.EVERYTHING_GREAT,                       loadTone("paired.txt"));
        putV2(SounderMode.FACTORY_RESET_OFFLINE,                  loadTone("nosound.txt"));
        putV2(SounderMode.FIRMWARE_UPDATE_NEEDED,                 loadTone("nosound.txt"));
        putV2(SounderMode.FIRST_BOOTUP, 			              loadTone("nosound.txt"));
        putV2(SounderMode.GREATNEWS_CONNECTED_CLOUD,              loadTone("nosound.txt"));
        putV2(SounderMode.GREATNEWS_INTERNET_CONNECTED,           loadTone("nosound.txt"));
        putV2(SounderMode.HUB_FACTORY_RESET,                      loadTone("nosound.txt"));
        putV2(SounderMode.HUB_REMOVED,                            loadTone("unpaired.txt"));
        putV2(SounderMode.PANIC_ALARM,                            loadTone("intruder.txt"));
        putV2(SounderMode.PANIC_ALARM_CANCELLED,                  loadTone("nosound.txt"));
        putV2(SounderMode.PANIC_TRIGGERED_MONITORING_NOTIFIED,    loadTone("intruder.txt"));
        putV2(SounderMode.REBOOT_HUB,                             loadTone("nosound.txt"));
        putV2(SounderMode.SECURITY_ALARM_OFF,                     loadTone("nosound.txt"));
        putV2(SounderMode.SECURITY_ALARM_ON,                      loadTone("armed.txt"));
        putV2(SounderMode.SECURITY_ALARM_PARTIAL,                 loadTone("armed.txt"));
        putV2(SounderMode.SECURITY_ALARM_TRIGGERED,               loadTone("intruder.txt"));
        putV2(SounderMode.SECURITY_ALARM_FAILED,           	      loadTone("nosound.txt"));
        putV2(SounderMode.SECURITY_TRIGGERED_MONITORING_NOTIFIED, loadTone("intruder.txt"));
        putV2(SounderMode.SMOKE_ALARM_CANCELLED,                  loadTone("nosound.txt"));
        putV2(SounderMode.SMOKE_ALARM_TRIGGERED,                  loadTone("safety.txt"));
        putV2(SounderMode.SMOKE_TRIGGERED_MONITORING_NOTIFIED,    loadTone("safety.txt"));
        putV2(SounderMode.SUCCESS_DEVICE_PAIRED,                  loadTone("paired.txt"));
        putV2(SounderMode.TRIGGER_15_SECONDS,                     loadTone("nosound.txt"));
        putV2(SounderMode.TURNING_OFF,                            loadTone("nosound.txt"));
        putV2(SounderMode.WATER_LEAK_ALARM_CANCELLED,             loadTone("nosound.txt"));
        putV2(SounderMode.WATER_LEAK_DETECTED,                    loadTone("safety.txt"));
        putV2(SounderMode.WIFI_CONNECTION_ISSUE,                  loadTone("nosound.txt"));
        
        // V2 Sound Definitions for V3 Hardware
        putV3(SounderMode.NO_SOUND,            null);
        putV3(SounderMode.PAIRED,              "sounds/triple.mp3", "voice/SuccessDevicePaired.mp3");
        putV3(SounderMode.UNPAIRED,            "sounds/Success.mp3", "voice/DeviceRemoved.mp3");
        // "ARMING" doesn't really make sense in the v3 case - should use enter/exit states for clarity!
        putV3(SounderMode.ARMING,              "sounds/triple.mp3");
        putV3(SounderMode.ARMED,               "voice/SecurityAlarmOn.mp3");
        putV3(SounderMode.CHIME,               "sounds/triple.mp3");
        putV3(SounderMode.INTRUDER,            "voice/SecurityAlarmTriggered.mp3");
        putV3(SounderMode.SAFETY,              "sounds/triple.mp3");
        putV3(SounderMode.LOW_BATTERY,         "sounds/triple.mp3");
        
        // New Sounds.
        putV3(SounderMode.BUTTON_PRESS,        "sounds/triple.mp3");
        putV3(SounderMode.CARE_ALARM,          "voice/CareTriggered.mp3");
        putV3(SounderMode.CO_ALARM,            "voice/CO_Triggered.mp3");
        putV3(SounderMode.DOUBLE_BUTTON_PRESS, "sounds/triple.mp3");
        putV3(SounderMode.DOOR_CHIME_1,        "sounds/triple.mp3");
        putV3(SounderMode.DOOR_CHIME_2,        "sounds/triple.mp3");
        putV3(SounderMode.DOOR_CHIME_3,        "sounds/triple.mp3");
        putV3(SounderMode.DOOR_CHIME_4,        "sounds/triple.mp3");
        putV3(SounderMode.DOOR_CHIME_5,        "sounds/triple.mp3");
        putV3(SounderMode.DOOR_CHIME_6,        "sounds/triple.mp3");
        putV3(SounderMode.DOOR_CHIME_7,        "sounds/triple.mp3");
        putV3(SounderMode.DOOR_CHIME_8,        "sounds/triple.mp3");
        putV3(SounderMode.DOOR_CHIME_9,        "sounds/triple.mp3");
        putV3(SounderMode.DOOR_CHIME_10,       "sounds/triple.mp3");
        putV3(SounderMode.FAILED,              "sounds/triple.mp3");
        putV3(SounderMode.PANIC_ALARM,         "voice/SecurityAlarmTriggered.mp3");
        putV3(SounderMode.STARTUP,             "voice/EverythingGreat.mp3");
        putV3(SounderMode.SECURITY_ALARM,      "voice/SecurityAlarmTriggered.mp3");
        putV3(SounderMode.SMOKE_ALARM,         "voice/SmokeAlarmTriggered.mp3");
        putV3(SounderMode.SUCCESS_TRIPLE,      "sounds/triple.mp3");
        putV3(SounderMode.SUCCESS_SINGLE,      "sounds/triple.mp3");
        putV3(SounderMode.SUCCESS_REMOVAL,     "voice/DeviceRemoved.mp3");
        putV3(SounderMode.SUCCESS_DISARM,      "voice/SecurityAlarmOff.mp3");
        putV3(SounderMode.SUCCESS_REBOOT,      "sounds/Success.mp3");
        putV3(SounderMode.WATER_LEAK_ALARM,    "sounds/triple.mp3");

        // Voices 
        putV3(SounderMode.AWESOME_ALL_SETUP,                      "voice/AwesomeAllSetup.mp3");
        putV3(SounderMode.BUTTON_PRESS_BACKUP,                    "voice/button-press-backup.mp3");
        putV3(SounderMode.BUTTON_PRESS_BACKUP_BATT,               "voice/button-press-backup-batt.mp3");
        putV3(SounderMode.BUTTON_PRESS_BACKUP_OFFLINE,            "voice/button-press-backup-offline.mp3");
        putV3(SounderMode.BUTTON_PRESS_BATTERY,                   "voice/button-press-battery.mp3");
        putV3(SounderMode.BUTTON_PRESS_NORMAL,                    "voice/button-press-normal.mp3");
        putV3(SounderMode.BUTTON_PRESS_OFFLINE,                   "voice/button-press-offline.mp3");
        putV3(SounderMode.BUTTON_PRESS_OFFLINE_BATT,              "voice/button-press-offline-batt.mp3");
        putV3(SounderMode.BUTTON_PRESS_NOPLACE,         	      "voice/buttonPressNoPlace.mp3");
        putV3(SounderMode.CARE_CANCELLED,                         "voice/CareCancelled.mp3");
        putV3(SounderMode.CARE_TRIGGERED,                         "voice/CareTriggered.mp3", 	 					  "sounds/continuousSecurity_Alarm_Trim_75.mp3");
        putV3(SounderMode.CO_ALARM_CANCELLED,                     "voice/CO_AlarmCancelled.mp3");
        putV3(SounderMode.CO_TRIGGERED_MONITORING_NOTIFIED,       "voice/CO_Triggered_MonitoringNotified.mp3", 		  "sounds/continuousCO_Alarm_Trim.mp3");
        putV3(SounderMode.CO_TRIGGERED,                           "voice/CO_Triggered.mp3", 			     		  "sounds/continuousCO_Alarm_Trim.mp3");
        putV3(SounderMode.CONNECTED_WIFI,                         "voice/ConnectedWifi.mp3");
        putV3(SounderMode.DEVICE_REMOVED,                         "sounds/Success.mp3", "voice/DeviceRemoved.mp3");
        putV3(SounderMode.EVERYTHING_GREAT,                       "voice/EverythingGreat.mp3");
        putV3(SounderMode.FACTORY_RESET_OFFLINE,                  "voice/FactoryResetOffline.mp3");
        putV3(SounderMode.FIRMWARE_UPDATE_NEEDED,                 "voice/FirmwareUpdateNeeded.mp3");
        putV3(SounderMode.FIRST_BOOTUP,                           "sounds/Start_Up.mp3", "voice/first-bootup.mp3");
        putV3(SounderMode.GREATNEWS_CONNECTED_CLOUD,              "voice/GreatNewsConnectCloudPlatform.mp3");
        putV3(SounderMode.GREATNEWS_INTERNET_CONNECTED,           "voice/GreatNewsInternetConnection.mp3");
        putV3(SounderMode.HUB_FACTORY_RESET,                      "sounds/Success.mp3", "voice/HubFactoryReset.mp3");
        putV3(SounderMode.HUB_REMOVED,                            "sounds/Success.mp3", "voice/HubRemoved.mp3");
        putV3(SounderMode.PANIC_ALARM,                            "voice/PanicAlarmTriggered.mp3", 					   "sounds/continuousSecurity_Alarm_Trim.mp3");
        putV3(SounderMode.PANIC_ALARM_CANCELLED,                  "voice/PanicAlarmCancelled.mp3");
        putV3(SounderMode.PANIC_TRIGGERED_MONITORING_NOTIFIED,    "voice/PanicTriggered_MonitoringNotified.mp3", 	   "sounds/continuousSecurity_Alarm_Trim.mp3");
        putV3(SounderMode.REBOOT_HUB,                             "sounds/Success.mp3",								   "voice/YourHubWillRebootNow.mp3");
        putV3(SounderMode.REGISTER_SUCCESS,                       "sounds/triple.mp3",								   "voice/register-success.mp3");
        putV3(SounderMode.SECURITY_ALARM_OFF,                     "voice/SecurityAlarmOff.mp3");
        putV3(SounderMode.SECURITY_ALARM_ON,                      "voice/SecurityAlarmOn.mp3");
        putV3(SounderMode.SECURITY_ALARM_PARTIAL,                 "voice/SecurityAlarmPartial.mp3");
        putV3(SounderMode.SECURITY_ALARM_TRIGGERED,               "voice/SecurityAlarmTriggered.mp3", 					"sounds/continuousSecurity_Alarm_Trim.mp3");
        putV3(SounderMode.SECURITY_ALARM_FAILED,              	   "sounds/failure.mp3", "voice/alarm-failure.mp3");
        putV3(SounderMode.SECURITY_TRIGGERED_MONITORING_NOTIFIED, "voice/SecurityTriggeresssdMonitoringNotified.mp3",  	"sounds/continuousSecurity_Alarm_Trim.mp3");
        putV3(SounderMode.SMOKE_ALARM_CANCELLED,                  "voice/SmokeAlarmCancelled.mp3");
        putV3(SounderMode.SMOKE_ALARM_TRIGGERED,                  "voice/SmokeAlarmTriggered.mp3",   					"sounds/continuousSmoke_Alarm_Trim.mp3");
        putV3(SounderMode.SMOKE_TRIGGERED_MONITORING_NOTIFIED,    "voice/SmokeTriggered_MonitoringNotified.mp3", 	    "sounds/continuousSmoke_Alarm_Trim.mp3");
        putV3(SounderMode.SUCCESS_DEVICE_PAIRED,                  "sounds/triple.mp3",									"voice/SuccessDevicePaired.mp3");
        putV3(SounderMode.TRIGGER_15_SECONDS,                     "voice/Trigger15seconds.mp3");
        putV3(SounderMode.TURNING_OFF,                            "sounds/Success.mp3", "voice/TurningOff.mp3");
        putV3(SounderMode.WATER_LEAK_ALARM_CANCELLED,             "voice/WaterLeakAlarmCancelled.mp3");
        putV3(SounderMode.WATER_LEAK_DETECTED,                    "voice/WaterLeakDetected.mp3",						"sounds/continuousSecurity_Alarm_Trim_75.mp3");
        putV3(SounderMode.WIFI_CONNECTION_ISSUE,                  "voice/WiFiConnectionIssue.mp3");
        putV3(SounderMode.ARMING_GRACE_EXIT,                      "voice/SecurityAlarmArmingToOn.mp3");
        putV3(SounderMode.ARMING_GRACE_EXIT_PARTIAL,              "voice/SecurityAlarmArmingToPartial.mp3");
        putV3(SounderMode.ARMING_GRACE_ENTER,                     "voice/alarm-grace-enter.mp3");
    }
    
    public static SoundConfig getInstance() {
        return new SoundConfig();
    }
    
    public SoundFile get(Model version, SounderMode mode) {
        SoundKey key = SoundKey.builder().version(version).mode(mode).build();
        if (sounds.containsKey(key)) {
            return sounds.get(key);
        } else {
            // Try the older sound file.
            return sounds.get(SoundKey.builder().version(Model.IH200).mode(mode).build()); 
        }
    }
    
    private void putV2(SounderMode mode,String url) {
        put(Model.IH200,mode,url,null);
    }

    private void putV3(SounderMode mode,String url) {        
        put(Model.IH300,mode, SOUND_DIR + url, null);
    }

    private void putV3(SounderMode mode,String url, String next) {        
        put(Model.IH300,mode, SOUND_DIR + url, SOUND_DIR + next);
    }
    
    private void put(Model version, SounderMode mode, String info, String info2) {
        sounds.put(
                SoundKey.builder()
                            .mode(mode)
                            .version(version)
                            .build(),                
                SoundFile.builder()
                            .mode(mode)
                            .version(version)
                            .info(info)
                            .info2(info2)
                            .build());
    }

    private String loadTone(String resource) {
        URL url = null;
        try {
            url = SoundConfig.class.getResource(resource);
            return IOUtils.toString(url, StandardCharsets.UTF_8);
         } catch (Throwable th) {
            log.error("failed to load sound file: {}, {}", resource, url, th);
            return "";
         }
    }

}

