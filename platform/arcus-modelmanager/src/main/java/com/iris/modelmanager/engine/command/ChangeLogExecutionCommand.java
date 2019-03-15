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
import com.iris.modelmanager.changelog.ChangeLog;
import com.iris.modelmanager.changelog.ChangeSet;
import com.iris.modelmanager.engine.ExecutionContext;
import com.iris.modelmanager.version.VersionHistory;

public class ChangeLogExecutionCommand extends CompositeExecutionCommand {

   private final ChangeLog changeLog;

   public ChangeLogExecutionCommand(ChangeLog changeLog) {
      super(createCommands(changeLog));
      this.changeLog = changeLog;
   }

   private static List<ExecutionCommand> createCommands(ChangeLog changeLog) {
      List<ExecutionCommand> commands = new LinkedList<ExecutionCommand>();
      for(ChangeSet changeSet : changeLog.getChangeSets()) {
         commands.add(new ChangeSetExecutionCommand(changeSet));
      }
      return commands;
   }

   @Override
   protected void afterExecute(ExecutionContext context) throws CommandExecutionException {
      insertHistory(context, Status.APPLIED);
   }

   @Override
   protected void afterRollback(ExecutionContext context) throws CommandExecutionException {
      insertHistory(context, Status.ROLLED_BACK);
   }

   private void insertHistory(ExecutionContext context, Status status) throws CommandExecutionException {
      try {
         VersionHistory history = new VersionHistory();
         history.setStatus(status);
         history.setTimestamp(new Date());
         history.setUsername(context.getManagerContext().getProfile().getUsername());
         history.setVersion(changeLog.getVersion());
         context.getVersionHistoryDAO().insert(history);
      } catch(QueryExecutionException qee) {
         throw new CommandExecutionException(qee);
      }
   }
}

