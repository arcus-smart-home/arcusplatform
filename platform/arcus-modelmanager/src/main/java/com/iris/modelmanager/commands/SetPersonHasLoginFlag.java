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

import org.apache.commons.lang3.StringUtils;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.iris.modelmanager.engine.ExecutionContext;
import com.iris.modelmanager.engine.command.CommandExecutionException;
import com.iris.modelmanager.engine.command.ExecutionCommand;

public class SetPersonHasLoginFlag implements ExecutionCommand {

   private static final String update = "UPDATE person set hasLogin = ? WHERE id = ?";

   @Override
   public void execute(ExecutionContext context, boolean autoRollback) throws CommandExecutionException {
      PreparedStatement stmt = context.getSession().prepare(update);

      List<Row> rows = context.getSession().execute("SELECT id, email FROM person").all();

      BatchStatement batch = new BatchStatement();

      rows.forEach((r) -> {
         batch.add(new BoundStatement(stmt)
            .setBool("hasLogin", !StringUtils.isBlank(r.getString("email")))
            .setUUID("id", r.getUUID("id")));
      });

      context.getSession().execute(batch);
   }

   @Override
   public void rollback(ExecutionContext context, boolean autoRollback) throws CommandExecutionException {
      PreparedStatement stmt = context.getSession().prepare(update);
      List<Row> rows = context.getSession().execute("SELECT id FROM person").all();

      BatchStatement batch = new BatchStatement();

      rows.forEach((r) -> {
         batch.add(new BoundStatement(stmt)
            .setToNull("hasLogin")
            .setUUID("id", r.getUUID("id")));
      });
      context.getSession().execute(batch);
   }
}

