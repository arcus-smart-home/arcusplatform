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
package com.iris.protocol.control.adapter.aosmith;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.iris.protocol.ipcd.adapter.aosmith.AOSmithAdapter;
import com.iris.protocol.ipcd.message.model.SetParameterValuesCommand;

public class TestAOSmtihAdaptor {

   @Test
   public void testMultipleEntries() {
      List<SetParameterValuesCommand> cmds = new ArrayList<SetParameterValuesCommand>();

      int n = 10;
      SetParameterValuesCommand commands[] = new SetParameterValuesCommand[n];
      for (int i=0;i<n;i++) {
         commands[i] = new SetParameterValuesCommand();
         Map<String,Object> map = new HashMap<String,Object>();
         map.put("aos.setpoint",new Integer(i));
         commands[i].setValues(map);
         cmds.add(commands[i]);
      }
      
      AOSmithAdapter adapter = new AOSmithAdapter();
      String response = adapter.createAOSReturnJson(null,cmds);     
      Assert.assertTrue(response.equals("{\"Success\":\"0\",\"SetPoint\":\"9\"}"));
   }   
}

