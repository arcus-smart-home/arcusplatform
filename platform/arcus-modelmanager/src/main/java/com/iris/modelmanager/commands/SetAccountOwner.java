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
package com.iris.modelmanager.commands;

import java.util.List;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.iris.modelmanager.engine.ExecutionContext;
import com.iris.modelmanager.engine.command.CommandExecutionException;
import com.iris.modelmanager.engine.command.ExecutionCommand;


public class SetAccountOwner implements ExecutionCommand {

   private static final String update = "UPDATE account set owner = ? WHERE id = ?";

   @Override
   public void execute(ExecutionContext context, boolean autoRollback) throws CommandExecutionException {
      PreparedStatement stmt = context.getSession().prepare(update);

      List<Row> rows = context.getSession().execute("SELECT * FROM authorization_grant").all();
      BatchStatement batch = new BatchStatement();
      rows.forEach((r) -> {
         if(r.getBool("accountowner")) {
            batch.add(new BoundStatement(stmt)
               .setUUID("owner", r.getUUID("entityId"))
               .setUUID("id", r.getUUID("accountid")));
         }
      });

      context.getSession().execute(batch);
   }

   @Override
   public void rollback(ExecutionContext context, boolean autoRollback) throws CommandExecutionException {
      PreparedStatement stmt = context.getSession().prepare(update);

      List<Row> rows = context.getSession().execute("SELECT id FROM account").all();
      BatchStatement batch = new BatchStatement();
      rows.forEach((r) -> {
         batch.add(new BoundStatement(stmt)
            .setToNull("owner")
            .setUUID("id", r.getUUID("id")));
      });
      context.getSession().execute(batch);
   }
}

