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
package com.iris.google.model;

import com.google.common.collect.ImmutableList;
import com.iris.messages.type.GoogleDevice;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExecutePayload {

   private List<Command> commands;

   public List<Command> getCommands() {
      return commands;
   }

   public void setCommands(List<Command> commands) {
      this.commands = commands;
   }

   public static class Command {

      private List<Map<String,Object>> devices;
      private List<Execution> execution;

      public List<Map<String, Object>> getDevices() {
         return devices;
      }

      public void setDevices(List<Map<String, Object>> devices) {
         this.devices = devices;
      }

      public List<GoogleDevice> devicesAsBeans() {
         if(this.devices == null) {
            return ImmutableList.of();
         }
         return devices.stream().map(GoogleDevice::new).collect(Collectors.toList());
      }

      public List<Execution> getExecution() {
         return execution;
      }

      public void setExecution(List<Execution> execution) {
         this.execution = execution;
      }
   }

   public static class Execution {

      private String command;
      private Map<String,Object> params;

      public String getCommand() {
         return command;
      }

      public void setCommand(String command) {
         this.command = command;
      }

      public Map<String, Object> getParams() {
         return params;
      }

      public void setParams(Map<String, Object> params) {
         this.params = params;
      }
   }

}

