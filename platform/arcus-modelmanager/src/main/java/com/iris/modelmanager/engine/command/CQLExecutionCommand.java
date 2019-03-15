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

import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.QueryExecutionException;
import com.datastax.driver.core.exceptions.QueryValidationException;
import com.iris.modelmanager.changelog.CQLCommand;
import com.iris.modelmanager.context.Operation;
import com.iris.modelmanager.engine.ExecutionContext;

public class CQLExecutionCommand implements ExecutionCommand {

   private final CQLCommand command;

   public CQLExecutionCommand(CQLCommand command) {
      this.command = command;
   }

   @Override
   public void execute(ExecutionContext context, boolean autoRollback) throws CommandExecutionException {
      executeCommand(context.getSession(), Operation.UPGRADE);
   }

   @Override
   public void rollback(ExecutionContext context, boolean autoRollback) throws CommandExecutionException {
      executeCommand(context.getSession(), Operation.ROLLBACK);
   }

   private void executeCommand(Session session, Operation operation) throws CommandExecutionException {
      try {
         if(operation == Operation.UPGRADE) {
            session.execute(command.getUpdateCql());
         } else {
            session.execute(command.getRollbackCql());
         }
      } catch(QueryExecutionException | QueryValidationException e) {
         throw new CommandExecutionException(e);
      }
   }

}

