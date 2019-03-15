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
package com.iris.agent.controller.hub.lights;


import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.hal.LEDState;
import com.iris.agent.lifecycle.LifeCycle;

public class LEDConfig {
    private static final Logger log = LoggerFactory.getLogger(LEDConfig.class);
    
    public static LEDConfig INSTANCE = new LEDConfig();
    
    Map<LEDKey,LEDState>  ledStates = new HashMap<>();

    Map<LEDKey,LEDState>  buttonStates = new HashMap<>();
   
    public LEDConfig() {
        AddV2Permutations();
    }
    
    private void AddV2Permutations() {
        //                 back   batt   auth   pair   conn
        ledStates.put ( V2(true,  true,  true,  true,  true),  LEDState.PAIRING_CONN_BACKUP_BATT );
        ledStates.put ( V2(true,  true,  true,  true,  false), LEDState.PAIRING_DISCONN_BATT );
        ledStates.put ( V2(true,  true,  true,  false, true),  LEDState.ASSOC_CONN_BACKUP_BATT );
        ledStates.put ( V2(true,  true,  true,  false, false), LEDState.ASSOC_DISCONN_BATT );
        ledStates.put ( V2(true,  true,  false, true,  true),  LEDState.UNASSOC_CONN_BACKUP_BATT );
        ledStates.put ( V2(true,  true,  false, true,  false), LEDState.UNASSOC_DISCONN_BATT );
        ledStates.put ( V2(true,  true,  false, false, true),  LEDState.UNASSOC_CONN_BACKUP_BATT );
        ledStates.put ( V2(true,  true,  false, false, false), LEDState.UNASSOC_DISCONN_BATT );
                
        ledStates.put ( V2(true,  false, true,  true,  true),  LEDState.PAIRING_CONN_BACKUP );
        ledStates.put ( V2(true,  false, true,  true,  false), LEDState.PAIRING_DISCONN );
        ledStates.put ( V2(true,  false, true,  false, true),  LEDState.ASSOC_CONN_BACKUP );
        ledStates.put ( V2(true,  false, true,  false, false), LEDState.ASSOC_DISCONN );
        ledStates.put ( V2(true,  false, false, true,  true),  LEDState.UNASSOC_CONN_BACKUP );
        ledStates.put ( V2(true,  false, false, true,  false), LEDState.UNASSOC_DISCONN );
        ledStates.put ( V2(true,  false, false, false, true),  LEDState.UNASSOC_CONN_BACKUP );
        ledStates.put ( V2(true,  false, false, false, false), LEDState.UNASSOC_DISCONN );
        
        ledStates.put ( V2(false, true,  true,  true,  true),  LEDState.PAIRING_CONN_PRI_BATT );
        ledStates.put ( V2(false, true,  true,  true,  false), LEDState.PAIRING_DISCONN_BATT );
        ledStates.put ( V2(false, true,  true,  false, true),  LEDState.ASSOC_CONN_PRI_BATT );
        ledStates.put ( V2(false, true,  true,  false, false), LEDState.ASSOC_DISCONN_BATT );
        ledStates.put ( V2(false, true,  false, true,  true),  LEDState.UNASSOC_CONN_PRI_BATT );
        ledStates.put ( V2(false, true,  false, true,  false), LEDState.UNASSOC_DISCONN_BATT );
        ledStates.put ( V2(false, true,  false, false, true),  LEDState.UNASSOC_CONN_PRI_BATT );
        ledStates.put ( V2(false, true,  false, false, false), LEDState.UNASSOC_DISCONN_BATT );
    
        ledStates.put ( V2(false, false, true,  true,  true),  LEDState.PAIRING_CONN_PRI );
        ledStates.put ( V2(false, false, true,  true,  false), LEDState.PAIRING_DISCONN );
        ledStates.put ( V2(false, false, true,  false, true),  LEDState.ASSOC_CONN_PRI );
        ledStates.put ( V2(false, false, true,  false, false), LEDState.ASSOC_DISCONN );
        ledStates.put ( V2(false, false, false, true,  true),  LEDState.UNASSOC_CONN_PRI );
        ledStates.put ( V2(false, false, false, true,  false), LEDState.UNASSOC_DISCONN );
        ledStates.put ( V2(false, false, false, false, true),  LEDState.UNASSOC_CONN_PRI );
        ledStates.put ( V2(false, false, false, false, false), LEDState.UNASSOC_DISCONN );
        
        // V3 persistent states
        // Alarm states are handle by ReflexLocalProcessing and HubController Interactions.
        // TODO:  Move alarm states to this class for full state
        ledStates.put( LEDKey.builder().isV3().isDisconnected().isNotAuthorized().isNotPairing().build(), LEDState.INTERNET_IN_PROGRESS);
        ledStates.put( LEDKey.builder().isV3().isConnected().isNotAuthorized().isNotPairing().build(), LEDState.CLOUD_IN_PROGRESS);
        ledStates.put( LEDKey.builder().isV3().isConnected().isAuthorized().isNotPairing().build(), LEDState.ALL_OFF);
        ledStates.put( LEDKey.builder().isV3().isConnected().isAuthorized().isPairing().build(), LEDState.PAIRING_CONN_PRI);

        // V3 Buttons:
        //                    back   batt   auth   pair   conn
        buttonStates.put ( V3(false, false, true,  false, true ),  LEDState.BUTTON_PRESS_NORMAL);
        buttonStates.put ( V3(false, false, true,  true,  true ),  LEDState.BUTTON_PRESS_NORMAL);
        buttonStates.put ( V3(false, true,  true,  false, true ),  LEDState.BUTTON_PRESS_BATTERY);
        buttonStates.put ( V3(false, true,  true,  true,  true ),  LEDState.BUTTON_PRESS_BATTERY);

        buttonStates.put ( V3(false, true,  false, true,  false ), LEDState.BUTTON_PRESS_OFFLINE_BATT);
        buttonStates.put ( V3(false, true,  false, false, false ), LEDState.BUTTON_PRESS_OFFLINE_BATT);
        buttonStates.put ( V3(false, false, false, true,  false ), LEDState.BUTTON_PRESS_OFFLINE);
        buttonStates.put ( V3(false, false, false, false, false ), LEDState.BUTTON_PRESS_OFFLINE);
        buttonStates.put ( V3(true,  true,  false, true,  false ), LEDState.BUTTON_PRESS_OFFLINE_BATT);
        buttonStates.put ( V3(true,  true,  false, false, false ), LEDState.BUTTON_PRESS_OFFLINE_BATT);
        buttonStates.put ( V3(true,  false, false, true,  false ), LEDState.BUTTON_PRESS_BACKUP_OFFLINE);
        buttonStates.put ( V3(true,  false, false, false, false ), LEDState.BUTTON_PRESS_BACKUP_OFFLINE);
        
        buttonStates.put ( V3(true,  false, true,  true,  true ),  LEDState.BUTTON_PRESS_BACKUP);
        buttonStates.put ( V3(true,  false, true,  false, true ),  LEDState.BUTTON_PRESS_BACKUP);
        buttonStates.put ( V3(true,  true,  true,  true,  true ),  LEDState.BUTTON_PRESS_BACKUP_BATT);
        buttonStates.put ( V3(true,  true,  true,  false, true ),  LEDState.BUTTON_PRESS_BACKUP_BATT);

        buttonStates.put ( V3(false, false, false, false, true ),  LEDState.BUTTON_PRESS_NORMAL);
        buttonStates.put ( V3(false, true,  false, false, true ),  LEDState.BUTTON_PRESS_BATTERY);
    }
    
    private LEDKey V2(boolean isBackupConnection, boolean isBattery, boolean isAuthorized, boolean pairing, boolean connected) {
        return LEDKey.builder().V2(isBackupConnection, isBattery, isAuthorized, pairing, connected).build();
    }

    private LEDKey V3(boolean isBackupConnection, boolean isBattery, boolean isAuthorized, boolean pairing, boolean connected) {
        return LEDKey.builder().V3(isBackupConnection, isBattery, isAuthorized, pairing, connected).build();
    }
    
    public static LEDState get(String model, boolean isBackupConnection, boolean isBattery, boolean isAuthorized, boolean pairing, boolean connected) {
        return INSTANCE.ledStates.getOrDefault((LEDKey.builder()
                                .isBackup(isBackupConnection)
                                .isBattery(isBattery)
                                .isAuthorized(isAuthorized)
                                .isPairing(pairing)
                                .isConnected(connected)
                                .withModel(model)
                                .build()),
                                LEDState.ALL_OFF);		// Going with all off as the default state.                                
    }
    
    public LEDState getButtonPressed(boolean isBackupConnection, boolean isBattery, boolean isAuthorized, boolean connected ) {
        return INSTANCE.buttonStates.getOrDefault((LEDKey.builder()
                .isBackup(isBackupConnection)
                .isBattery(isBattery)
                .isAuthorized(isAuthorized)
                .isConnected(connected)
                .isV3()
                .build()),
                LEDState.ALL_OFF);		// Going with all off as the default state.                                    	
    }
}

