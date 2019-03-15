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
import java.util.Date;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.iris.agent.storage.FileContentMonitor;
import com.iris.agent.storage.StorageService;
import com.iris.messages.capability.HubButtonCapability;

public class ButtonIrisControl {
    private static final Logger log = LoggerFactory.getLogger(ButtonIrisControl.class);
    
    public static final String DEFAULT_VALUE = HubButtonCapability.STATE_RELEASED;
    
    public static final String BUTTON_VALUE = "tmp:///irisButtonPushed";
    public static final String BUTTON_STATE = "tmp:///io/irisBtnValue";
    public static final String PRESSED = "0";
    public static final String RELEASED = "1";

    public static final ImmutableMap<String,String> fileToValueMap = new ImmutableMap.Builder<String,String> ()
            .put(PRESSED,HubButtonCapability.STATE_PRESSED)
            .put(RELEASED,HubButtonCapability.STATE_RELEASED)
            .build();
    
    private String state = DEFAULT_VALUE;
    private int duration = 0;
    private Long lastPressed = new Date().toInstant().toEpochMilli();
    private FileContentMonitor monitor;
    
    private static final CopyOnWriteArraySet<ButtonListener> listeners = new CopyOnWriteArraySet<>();
    
    private ButtonIrisControl() throws IOException {
        this.monitor = StorageService.getFileMonitor(BUTTON_VALUE);
        this.monitor.addListener((monitor) -> onButton(monitor));
    }
    
    public static final ButtonIrisControl create() {
        log.info("Starting Button Controls.");
        try {
            return new ButtonIrisControl();
        } catch (IOException e) {
            log.error("Could not create button controller {}", e);
            return null;
        }
    }
        
    public void onButton(FileContentMonitor monitor) {
        String contents = monitor.getContents().trim();
        duration = Integer.parseInt(contents);
        if (duration > 0) {
           log.debug("Button file change detected, contents = [{}]", contents );
           state = fileToValueMap.getOrDefault(contents,HubButtonCapability.STATE_PRESSED);
           lastPressed = new Date().toInstant().toEpochMilli();
           listeners.forEach(l -> l.buttonState(state,duration));
        }
        // Set to zero after we have read contents
        File file = StorageService.getFile(BUTTON_VALUE);
        try {
           FileUtils.writeStringToFile(file, "0");
        } catch (IOException e) {
           log.error("Unable to clear button value file: {}", BUTTON_VALUE);
        }
    }
    
    public void shutdown() {
        monitor.cancel();
    }
    
    public String getState() {
        return state;
    }

    public void addButtonListener(ButtonListener listener) {
        listeners.add(listener);
    }
    
    public void removeButtonListener(ButtonListener listener) {
        listeners.remove(listener);
    }

    public int getDuration() {
        return duration;
    }

    public Long getLastPressed() {
        return lastPressed;
    }

}

