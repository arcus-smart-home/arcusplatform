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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.iris.modelmanager.engine.ExecutionContext;
import com.iris.modelmanager.engine.command.CommandExecutionException;
import com.iris.modelmanager.engine.command.ExecutionCommand;

public class SetPrimaryPlace implements ExecutionCommand {

   private static final String update = "UPDATE place set is_primary = ? WHERE id = ?";

   @Override
   public void execute(ExecutionContext context, boolean autoRollback) throws CommandExecutionException {
      PreparedStatement stmt = context.getSession().prepare(update);

      List<Row> rows = context.getSession().execute("SELECT id, accountid, created FROM place").all();
      List<Row> ordered = rows.stream().sorted((r1, r2) -> r1.getTimestamp("created").compareTo(r2.getTimestamp("created"))).collect(Collectors.toList());
      Set<UUID> accountsSeen = new HashSet<>();

      BatchStatement batch = new BatchStatement();

      ordered.forEach((r) -> {
         batch.add(new BoundStatement(stmt)
            .setBool("is_primary", !accountsSeen.contains(r.getUUID("accountid")))
            .setUUID("id", r.getUUID("id")));
         accountsSeen.add(r.getUUID("accountid"));
      });

      context.getSession().execute(batch);
   }

   @Override
   public void rollback(ExecutionContext context, boolean autoRollback) throws CommandExecutionException {
      PreparedStatement stmt = context.getSession().prepare(update);
      List<Row> rows = context.getSession().execute("SELECT id FROM place").all();

      BatchStatement batch = new BatchStatement();

      rows.forEach((r) -> {
         batch.add(new BoundStatement(stmt)
            .setToNull("is_primary")
            .setUUID("id", r.getUUID("id")));
      });
      context.getSession().execute(batch);
   }
}

