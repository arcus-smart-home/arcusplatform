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

import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.hal.IrisHal;
import com.iris.agent.router.Port;
import com.iris.agent.router.PortHandler;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.HubDebugCapability;
import com.iris.protocol.ProtocolMessage;

enum DebugHandler implements PortHandler {
      INSTANCE;

      private static final Logger log = LoggerFactory.getLogger(DebugHandler.class);

      void start(Port parent) {
         parent.delegate(
            this,
            HubDebugCapability.GetFilesRequest.NAME,
            HubDebugCapability.GetAgentDbRequest.NAME,
            HubDebugCapability.GetSyslogRequest.NAME,
            HubDebugCapability.GetBootlogRequest.NAME,
            HubDebugCapability.GetProcessesRequest.NAME,
            HubDebugCapability.GetLoadRequest.NAME
         );
      }

      @Nullable
      @Override
      public Object recv(Port port, PlatformMessage message) throws Exception {
         String type = message.getMessageType();
         switch (type) {
         case HubDebugCapability.GetFilesRequest.NAME:
            return handleGetFilesRequest(message);

         case HubDebugCapability.GetAgentDbRequest.NAME:
            return handleGetAgentDbRequest(message);

         case HubDebugCapability.GetSyslogRequest.NAME:
            return handleGetSyslogRequest(message);

         case HubDebugCapability.GetBootlogRequest.NAME:
            return handleGetBootlogRequest(message);

         case HubDebugCapability.GetProcessesRequest.NAME:
            return handleGetProcessesRequest(message);

         case HubDebugCapability.GetLoadRequest.NAME:
            return handleGetLoadRequest(message);
            
         default:
            return null;
         }
      }

      @Override
      public void recv(Port port, ProtocolMessage message) {
      }

      @Override
      public void recv(Port port, Object message) {
      }

      @SuppressWarnings("unchecked")
      private Object handleGetFilesRequest(PlatformMessage message) {
         log.debug("handling hubdebug getFiles request: {}", message);
         List<String> paths = (List<String>) message.getValue().getAttributes().get(HubDebugCapability.GetFilesRequest.ATTR_PATHS);
         String content = IrisHal.getFiles(paths);
         return HubDebugCapability.GetFilesResponse.builder()
                  .withContent(content)
                  .build();
      }
      
      private Object handleGetLoadRequest(PlatformMessage message) {
         log.debug("handling hubdebug getLoad request: {}", message);
         String load = IrisHal.getLoad();
         return HubDebugCapability.GetLoadResponse.builder()
                  .withLoad(load)
                  .build();
      }

      private Object handleGetProcessesRequest(PlatformMessage message) {
         log.debug("handling hubdebug getProcesses request: {}", message);
         String processes = IrisHal.getProcesses();
         return HubDebugCapability.GetProcessesResponse.builder()
                  .withProcesses(processes)
                  .build();
     }

      private Object handleGetBootlogRequest(PlatformMessage message) {
         log.debug("handling hubdebug getBootlog request: {}", message);
         String bootlog = IrisHal.getBootlog();
         return HubDebugCapability.GetBootlogResponse.builder()
                  .withBootlogs(bootlog)
                  .build();
      }

      private Object handleGetSyslogRequest(PlatformMessage message) {
         log.debug("handling hubdebug getSyslog request: {}", message);
         String syslog = IrisHal.getSyslog();
         return HubDebugCapability.GetSyslogResponse.builder()
                  .withSyslogs(syslog)
                  .build();
      }

      private Object handleGetAgentDbRequest(PlatformMessage message) {
         log.debug("handling hubdebug getAgentDb request: {}", message);
         String db = IrisHal.getAgentDb();
         return HubDebugCapability.GetAgentDbResponse.builder()
                  .withDb(db)
                  .build();
      }
}

