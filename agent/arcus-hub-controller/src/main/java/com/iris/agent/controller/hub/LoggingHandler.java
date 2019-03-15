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
package com.iris.agent.controller.hub;

import org.eclipse.jdt.annotation.Nullable;

import com.iris.agent.config.ConfigService;
import com.iris.agent.logging.IrisAgentAppender;
import com.iris.agent.logging.IrisAgentLogging;
import com.iris.agent.router.Port;
import com.iris.agent.router.PortHandler;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.HubCapability;
import com.iris.protocol.ProtocolMessage;

import ch.qos.logback.classic.spi.ILoggingEvent;

enum LoggingHandler implements PortHandler, IrisAgentAppender.Listener {
   INSTANCE;

   private static final String CONFIG_STREAMEND = "iris.logging.streamend";
   private static final String CONFIG_STREAMLVL = "iris.logging.streamlvl";
   private long endStreamTime = Long.MIN_VALUE;

   void start(Port parent) {
      parent.delegate(
         this,
         HubCapability.SetLogLevelRequest.NAME,
         HubCapability.GetLogsRequest.NAME,
         HubCapability.ResetLogLevelsRequest.NAME,
         HubCapability.StreamLogsRequest.NAME
      );

      long curTime = System.currentTimeMillis();
      endStreamTime = ConfigService.get(CONFIG_STREAMEND, Long.class, Long.MIN_VALUE);

      String lvl = ConfigService.get(CONFIG_STREAMLVL, String.class, "DEBUG");

      if (curTime < endStreamTime) {
         IrisAgentLogging.getInMemoryAppender().setListenerLogLevel(IrisAgentLogging.getLogLevel(lvl));
         IrisAgentLogging.getInMemoryAppender().setNotifyListeners(true);
      }

      IrisAgentLogging.getInMemoryAppender().addListener(this);
   }

   @Nullable
   @Override
   public Object recv(Port port, PlatformMessage message) throws Exception {
      String type = message.getMessageType();
      switch (type) {
      case HubCapability.SetLogLevelRequest.NAME:
         return handleSetLogLevel(port, message);

      case HubCapability.GetLogsRequest.NAME:
         return handleGetLogs(port, message);

      case HubCapability.ResetLogLevelsRequest.NAME:
         return handleResetLogLevels(port, message);

      case HubCapability.StreamLogsRequest.NAME:
         return handleStreamLogs(port, message);

      default:
         throw new Exception("hub cannot handle logging message: " + type);
      }
  }

   @Override
   public void recv(Port port, ProtocolMessage message) {
   }

   @Override
   public void recv(Port port, Object message) {
   }

   @Override
   public void appendLogEntry(ILoggingEvent event) {
      long curTime = event.getTimeStamp();
      if (curTime > endStreamTime) {
         IrisAgentLogging.getInMemoryAppender().setNotifyListeners(false);
         return;
      }
   }

   @Nullable
   private Object handleSetLogLevel(Port port, PlatformMessage message) {
      MessageBody body = message.getValue();
      String level = HubCapability.SetLogLevelRequest.getLevel(body);
      String scope = HubCapability.SetLogLevelRequest.getScope(body);
      if (scope == null) {
         scope = HubCapability.SetLogLevelRequest.SCOPE_ROOT;
      }

      String lvl;
      switch (level) {
      case HubCapability.SetLogLevelRequest.LEVEL_TRACE: lvl = "trace"; break;
      case HubCapability.SetLogLevelRequest.LEVEL_DEBUG: lvl = "debug"; break;
      case HubCapability.SetLogLevelRequest.LEVEL_INFO: lvl = "info"; break;
      case HubCapability.SetLogLevelRequest.LEVEL_WARN: lvl = "warn"; break;
      case HubCapability.SetLogLevelRequest.LEVEL_ERROR: lvl = "error"; break;
      default: throw new RuntimeException("unknown log level: " + level);
      }

      switch (scope) {
      case HubCapability.SetLogLevelRequest.SCOPE_ROOT: IrisAgentLogging.setRootLogLevel(lvl); break;
      case HubCapability.SetLogLevelRequest.SCOPE_AGENT: IrisAgentLogging.setAgentLogLevel(lvl); break;
      case HubCapability.SetLogLevelRequest.SCOPE_ZIGBEE: IrisAgentLogging.setZigbeeLogLevel(lvl); break;
      case HubCapability.SetLogLevelRequest.SCOPE_ZWAVE: IrisAgentLogging.setZWaveLogLevel(lvl); break;
      case HubCapability.SetLogLevelRequest.SCOPE_BLE: IrisAgentLogging.setBleLogLevel(lvl); break;
      case HubCapability.SetLogLevelRequest.SCOPE_SERCOMM: IrisAgentLogging.setSercommLogLevel(lvl); break;
      default: throw new RuntimeException("unknown log scope: " + scope);
      }

      return HubCapability.SetLogLevelResponse.instance();
   }

   @Nullable
   private Object handleResetLogLevels(Port port, PlatformMessage message) {
      IrisAgentLogging.resetLogLevels();
      return HubCapability.ResetLogLevelsResponse.instance();
   }

   @Nullable
   private Object handleGetLogs(Port port, PlatformMessage message) {
      return HubCapability.GetLogsResponse.builder()
         .withLogs(IrisAgentLogging.getInMemoryLogs(true))
         .build();
   }

   @Nullable
   private Object handleStreamLogs(Port port, PlatformMessage message) {
      MessageBody msg = message.getValue();

      String lvl = HubCapability.StreamLogsRequest.getSeverity(msg);
      if (lvl == null || lvl.isEmpty()) {
         lvl = "DEBUG";
      }

      long duration = HubCapability.StreamLogsRequest.getDuration(msg);
      long curTime = System.currentTimeMillis();

      endStreamTime = curTime + duration;

      ConfigService.put(CONFIG_STREAMEND, endStreamTime);
      ConfigService.put(CONFIG_STREAMLVL, lvl);

      IrisAgentLogging.getInMemoryAppender().setListenerLogLevel(IrisAgentLogging.getLogLevel(lvl));
      if (curTime < endStreamTime) {
         IrisAgentLogging.getInMemoryAppender().setNotifyListeners(true);
      } else {
         IrisAgentLogging.getInMemoryAppender().setNotifyListeners(false);
      }

      return HubCapability.StreamLogsResponse.instance();
   }
}

