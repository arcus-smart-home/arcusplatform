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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

import com.iris.modelmanager.context.Operation;
import com.iris.modelmanager.engine.ExecutionContext;

public class CompositeExecutionCommand implements ExecutionCommand {

   private final Queue<ExecutionCommand> pending;
   private final Queue<ExecutionCommand> rollbackPending;
   private final Stack<ExecutionCommand> completed;

   public CompositeExecutionCommand(List<ExecutionCommand> commands) {
      pending = new LinkedList<ExecutionCommand>(commands);

      LinkedList<ExecutionCommand> rollbacks = new LinkedList<ExecutionCommand>(commands);
      Collections.reverse(rollbacks);

      rollbackPending = rollbacks;
      completed = new Stack<ExecutionCommand>();
   }

   @Override
   public void execute(ExecutionContext context, boolean autoRollback) throws CommandExecutionException {
      execute(context, Operation.UPGRADE, autoRollback);
      afterExecute(context);
   }

   protected void afterExecute(ExecutionContext context) throws CommandExecutionException {
      // no op hook
   }

   @Override
   public void rollback(ExecutionContext context, boolean autoRollback) throws CommandExecutionException {
      execute(context, Operation.ROLLBACK, autoRollback);
      afterRollback(context);
   }

   protected void afterRollback(ExecutionContext context) throws CommandExecutionException {
      // no op hook
   }

   private void execute(ExecutionContext context, Operation operation, boolean autoRollback) throws CommandExecutionException {
      ExecutionCommand command = null;

      Queue<ExecutionCommand> activeQueue = operation == Operation.UPGRADE ? pending : rollbackPending;

      try {
         while((command = activeQueue.poll()) != null) {

            if(operation == Operation.UPGRADE) { command.execute(context, autoRollback); }
            else { command.rollback(context, autoRollback); }

            completed.push(command);
         }
      } catch(CommandExecutionException cee) {
         // try to rollback, if that fails we'll need to log a message because there isn't a recovery
         try {
            while(!completed.empty()) {
               command = completed.pop();
               if(operation == Operation.UPGRADE) {  command.rollback(context, false); }
               else { command.execute(context, false); }
            }
         } catch(CommandExecutionException rollbackException) {
            // FIXME:  log!, this isn't really recoverable
         }

         throw cee;
      }
   }
}

