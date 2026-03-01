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

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchStatementBuilder;
import com.datastax.oss.driver.api.core.cql.DefaultBatchType;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.iris.modelmanager.engine.ExecutionContext;
import com.iris.modelmanager.engine.command.CommandExecutionException;
import com.iris.modelmanager.engine.command.ExecutionCommand;

public class SetPrimaryPlace implements ExecutionCommand {

   private static final String update = "UPDATE place set is_primary = ? WHERE id = ?";

   @Override
   public void execute(ExecutionContext context, boolean autoRollback) throws CommandExecutionException {
      PreparedStatement stmt = context.getSession().prepare(update);

      List<Row> rows = context.getSession().execute("SELECT id, accountid, created FROM place").all();
      List<Row> ordered = rows.stream().sorted((r1, r2) -> {
         Date d1 = r1.isNull("created") ? null : Date.from(r1.getInstant("created"));
         Date d2 = r2.isNull("created") ? null : Date.from(r2.getInstant("created"));
         if(d1 == null && d2 == null) return 0;
         if(d1 == null) return -1;
         if(d2 == null) return 1;
         return d1.compareTo(d2);
      }).collect(Collectors.toList());
      Set<UUID> accountsSeen = new HashSet<>();

      BatchStatementBuilder batch = BatchStatement.builder(DefaultBatchType.LOGGED);

      ordered.forEach((r) -> {
         batch.addStatement(stmt.bind()
            .setBoolean("is_primary", !accountsSeen.contains(r.getUuid("accountid")))
            .setUuid("id", r.getUuid("id")));
         accountsSeen.add(r.getUuid("accountid"));
      });

      context.getSession().execute(batch.build());
   }

   @Override
   public void rollback(ExecutionContext context, boolean autoRollback) throws CommandExecutionException {
      PreparedStatement stmt = context.getSession().prepare(update);
      List<Row> rows = context.getSession().execute("SELECT id FROM place").all();

      BatchStatementBuilder batch = BatchStatement.builder(DefaultBatchType.LOGGED);

      rows.forEach((r) -> {
         batch.addStatement(stmt.bind()
            .setToNull("is_primary")
            .setUuid("id", r.getUuid("id")));
      });
      context.getSession().execute(batch.build());
   }
}
