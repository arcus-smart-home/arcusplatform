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
package com.iris.agent.zwave.code.cmdclass;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.iris.agent.zwave.code.AbstractByteCmd;
import com.iris.agent.zwave.code.Command;
import com.iris.agent.zwave.code.CommandClass;

public abstract class AbstractCmdClass extends AbstractByteCmd implements CommandClass {
   private final Map<Integer, Command> cmdMap = new HashMap<>();
   
   AbstractCmdClass(Command... cmds) {
      super();
      for (Command cmd : cmds) {
         cmdMap.put(cmd.intId(), cmd);
      }
      CmdClasses.register(this);
   }

   @Override
   public Collection<Command> commands() {
      return cmdMap.values();
   }

   @Override
   public Command command(int id) {
      return cmdMap.get(id);
   }
}


