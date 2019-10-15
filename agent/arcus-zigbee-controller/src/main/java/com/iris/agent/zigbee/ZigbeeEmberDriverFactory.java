/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Arcus Project
 *
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
package com.iris.agent.zigbee;

import com.iris.agent.hal.IrisHal;
import com.iris.agent.zigbee.ember.ZigbeeEmberDriver;
import com.zsmartsystems.zigbee.serial.ZigBeeSerialPort;
import com.zsmartsystems.zigbee.transport.ZigBeePort;

public class ZigbeeEmberDriverFactory extends ZigbeeDriverFactory {
   String port;

   public ZigbeeEmberDriverFactory(String port) {
      this.port = port;
   }

   public ZigbeeEmberDriver create() {
      ZigBeePort.FlowControl flowControl = ZigBeePort.FlowControl.FLOWCONTROL_OUT_NONE;

      if (IrisHal.isZigbeeXonXoff()) {
         flowControl = ZigBeePort.FlowControl.FLOWCONTROL_OUT_XONOFF;
      } else if (IrisHal.isZigbeeRtsCts()) {
         flowControl = ZigBeePort.FlowControl.FLOWCONTROL_OUT_RTSCTS;
      }

      final ZigBeePort serialPort = new ZigBeeSerialPort(port, IrisHal.getZigbeeBaudRate(), flowControl);

      return new ZigbeeEmberDriver(serialPort);
   }
}
