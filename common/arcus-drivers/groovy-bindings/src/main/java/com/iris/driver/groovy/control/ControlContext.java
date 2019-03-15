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
/**
 * Lowe's Confidential ---------------------------------------
 * Copyright 2014-2015 Lowe's Companies, Inc. All rights reserved.
 *
 * NOTICE: All information contained herein is and remains the property of
 * Lowe's Companies Inc. The technical concepts contained herein are proprietary
 * to Lowe's Companies Inc. and may be covered by U.S. and foreign patents, or
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination or reproduction of this material is strictly forbidden unless
 * prior written permission is obtained from Lowe's Companies, Inc.
 **/

/**
 * @author paul@couto-software.com
 *
 */

package com.iris.driver.groovy.control;

import groovy.lang.GroovyObjectSupport;

import java.util.Map;

import com.iris.driver.service.executor.DriverExecutors;
import com.iris.messages.MessageBody;
import com.iris.protocol.control.ControlProtocol;


public class ControlContext extends GroovyObjectSupport {

   void send(String type, Map<String, Object> attributes) {
      send (type, attributes, -1);
   }

   void send(String type, Map<String, Object> attributes, int timeoutMs) {
       MessageBody payload = MessageBody.buildMessage(type, attributes);
       DriverExecutors.get().context().sendToDevice(ControlProtocol.INSTANCE, payload, timeoutMs);
   }
}

