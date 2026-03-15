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
package com.iris.driver.groovy.devicesettings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.devicesettings.DeviceSettingsContext;
import com.iris.driver.devicesettings.DeviceSettingsParamDefinition;
import com.iris.driver.handler.ContextualEventHandler;
import com.iris.protocol.zwave.ZWaveProtocol;
import com.iris.protocol.zwave.message.ZWaveCommandMessage;
import com.iris.protocol.zwave.model.ZWaveAllCommandClasses;
import com.iris.protocol.zwave.model.ZWaveCommand;
import com.iris.protocol.zwave.model.ZWaveCommandClass;

/**
 * Handles Z-Wave Configuration Report messages.
 * Extracts the parameter number and value, looks up the parameter in
 * DeviceSettingsContext, and updates the driver variable.
 *
 * Returns false to allow existing driver-specific onZWaveMessage.configuration.report
 * handlers to also process the message.
 */
public class DeviceSettingsConfigReportHandler implements ContextualEventHandler<Object> {
   private static final Logger log = LoggerFactory.getLogger(DeviceSettingsConfigReportHandler.class);

   private static final ZWaveCommandClass CONFIGURATION_CC = ZWaveAllCommandClasses.getClass("configuration");
   private static final ZWaveCommand CONFIGURATION_REPORT_CMD = CONFIGURATION_CC.commandsByName.get("report");

   private final DeviceSettingsContext settingsContext;

   public DeviceSettingsConfigReportHandler(DeviceSettingsContext settingsContext) {
      this.settingsContext = settingsContext;
   }

   @Override
   public boolean handleEvent(DeviceDriverContext context, Object event) throws Exception {
      if (!(event instanceof com.iris.protocol.ProtocolMessage)) {
         return false;
      }

      com.iris.protocol.ProtocolMessage protocolMessage = (com.iris.protocol.ProtocolMessage) event;
      com.iris.protocol.zwave.message.ZWaveMessage zwaveMessage;
      try {
         zwaveMessage = protocolMessage.getValue(ZWaveProtocol.INSTANCE);
      } catch (Exception e) {
         log.debug("Failed to decode Z-Wave message", e);
         return false;
      }

      if (!(zwaveMessage instanceof ZWaveCommandMessage)) {
         return false;
      }

      ZWaveCommand command = ((ZWaveCommandMessage) zwaveMessage).getCommand();
      if (command == null) {
         return false;
      }

      // Only handle Configuration Report
      if (command.commandClass != CONFIGURATION_CC.number || command.commandNumber != CONFIGURATION_REPORT_CMD.commandNumber) {
         return false;
      }

      // Extract parameter number from received bytes
      // Configuration Report format: [parameterNumber, level(size), value1, value2?, value3?, value4?]
      byte[] recvBytes = command.recvBytes;
      if (recvBytes == null || recvBytes.length < 3) {
         // Try named receive variables
         try {
            int paramNo = command.get("param") & 0xFF;
            int byteCnt = command.get("level") & 0x07;
            long val = extractValue(command, byteCnt);

            return processReport(context, paramNo, val);
         } catch (Exception e) {
            log.trace("Could not extract config report from named vars", e);
         }

         // Try raw bytes
         if (recvBytes != null && recvBytes.length >= 3) {
            int paramNo = recvBytes[0] & 0xFF;
            int byteCnt = recvBytes[1] & 0x07;
            long val = extractValueFromBytes(recvBytes, 2, byteCnt);
            return processReport(context, paramNo, val);
         }

         return false;
      }

      int paramNo = recvBytes[0] & 0xFF;
      int byteCnt = recvBytes[1] & 0x07;
      long val = extractValueFromBytes(recvBytes, 2, byteCnt);

      return processReport(context, paramNo, val);
   }

   private boolean processReport(DeviceDriverContext context, int paramNo, long val) {
      DeviceSettingsParamDefinition param = settingsContext.getByZwaveParamNo(paramNo);
      if (param == null) {
         log.trace("Config report for unknown param {}, ignoring", paramNo);
         // Return false so other handlers can process it
         return false;
      }

      log.debug("Config report: param {}({}) = {}", param.getParamId(), paramNo, val);
      context.setVariable(param.getVariableKey(), val);

      // Emit value change for devsettings:params
      DeviceSettingsUtil.emitParamsValueChange(context, settingsContext);

      // Return false to allow driver-specific handlers to also process
      return false;
   }

   private long extractValue(ZWaveCommand command, int byteCnt) {
      long val1 = command.get("val1") & 0xFF;
      if (byteCnt >= 2) {
         long val2 = command.get("val2") & 0xFF;
         val1 = (val1 << 8) | val2;
      }
      if (byteCnt >= 4) {
         // Re-read from val1 for 4-byte values
         val1 = command.get("val1") & 0xFF;
         long val2 = command.get("val2") & 0xFF;
         long val3 = command.get("val3") & 0xFF;
         long val4 = command.get("val4") & 0xFF;
         val1 = (val1 << 24) | (val2 << 16) | (val3 << 8) | val4;
      }
      return val1;
   }

   private long extractValueFromBytes(byte[] bytes, int offset, int count) {
      long value = 0;
      for (int i = 0; i < count && (offset + i) < bytes.length; i++) {
         value = (value << 8) | (bytes[offset + i] & 0xFF);
      }
      return value;
   }
}
