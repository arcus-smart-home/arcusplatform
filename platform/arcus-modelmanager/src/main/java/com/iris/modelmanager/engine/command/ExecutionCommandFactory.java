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
package com.iris.modelmanager.engine.command;

import com.iris.bootstrap.ServiceLocator;
import com.iris.modelmanager.changelog.CQLCommand;
import com.iris.modelmanager.changelog.Command;
import com.iris.modelmanager.changelog.JavaCommand;

public final class ExecutionCommandFactory {

   private ExecutionCommandFactory() {
   }

   public static ExecutionCommand create(Command command) {
      if(command instanceof CQLCommand) {
         return createCQLCommand((CQLCommand) command);
      } else if(command instanceof JavaCommand) {
         return createJavaCommand((JavaCommand) command);
      }
      throw new RuntimeException(command + " must be either a CQL or Java command");
   }

   private static ExecutionCommand createCQLCommand(CQLCommand command) {
      return new CQLExecutionCommand(command);
   }

   @SuppressWarnings("rawtypes")
   private static ExecutionCommand createJavaCommand(JavaCommand command) {
      try {
         Class clazz = Class.forName(command.getClassName());
         if(ExecutionCommand.class.isAssignableFrom(clazz)) {
            return (ExecutionCommand) ServiceLocator.getInstance(clazz);
         }
      } catch(Exception e) {
         throw new RuntimeException(command + " could not be found or instantiated.", e);
      }
      throw new RuntimeException(command + " does not implement the ExecutionCommand interface.");
   }
}

