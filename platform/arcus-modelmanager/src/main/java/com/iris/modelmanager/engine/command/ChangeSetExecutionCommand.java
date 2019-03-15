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

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.datastax.driver.core.exceptions.QueryExecutionException;
import com.iris.modelmanager.Status;
import com.iris.modelmanager.changelog.ChangeSet;
import com.iris.modelmanager.changelog.Command;
import com.iris.modelmanager.engine.ExecutionContext;

public class ChangeSetExecutionCommand extends CompositeExecutionCommand {

   private final ChangeSet changeSet;

   public ChangeSetExecutionCommand(ChangeSet changeSet) {
      super(createCommands(changeSet));
      this.changeSet = changeSet;
   }

   private static List<ExecutionCommand> createCommands(ChangeSet changeSet) {
      List<ExecutionCommand> commands = new LinkedList<ExecutionCommand>();
      for(Command command : changeSet.getCommands()) {
         commands.add(ExecutionCommandFactory.create(command));
      }
      return commands;
   }

   @Override
   protected void afterExecute(ExecutionContext context) throws CommandExecutionException {
      updateState(context, Status.APPLIED);
   }

   @Override
   protected void afterRollback(ExecutionContext context) throws CommandExecutionException {
      updateState(context, Status.ROLLED_BACK);
   }

   private void updateState(ExecutionContext context, Status status) throws CommandExecutionException {
      try {
         changeSet.setStatus(status);
         changeSet.setTimestamp(new Date());
         context.getChangeSetDAO().update(changeSet);
      } catch(QueryExecutionException qee) {
         throw new CommandExecutionException(qee);
      }
   }
}

