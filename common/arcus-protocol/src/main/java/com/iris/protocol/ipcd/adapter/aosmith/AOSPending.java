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
package com.iris.protocol.ipcd.adapter.aosmith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.iris.protocol.ipcd.message.model.SetParameterValuesCommand;
import com.iris.protocol.ipcd.message.model.Status;
import com.iris.protocol.ipcd.message.model.StatusType;

public class AOSPending {
   private final SetParameterValuesCommand command;
   // The expected parameter values after the update
   private final Map<String,String> expected = new HashMap<>();
   // List of failure messages for bad parameter settings
   private final List<String> failures = new ArrayList<>();
   
   public AOSPending(SetParameterValuesCommand command) {
      this.command = command;
   }

   public SetParameterValuesCommand getCommand() {
      return command;
   }
   
   public Map<String,String> getExpected() {
      return expected;
   }
   
   public Status getStatus() {
      Status status = new Status();
      if (failures.isEmpty()) {
         status.setResult(StatusType.success);
      }
      else {
         status.setResult(StatusType.fail);
         status.setMessages(failures);
      }
      return status;
   }
   
   public void addFail(String msg) {
      failures.add(msg);
   }
   
   public void addExpected(String param, String value) {
      expected.put(param, value);
   }
   
   public void removeExpected(String param) {
      expected.remove(param);
   }
}

