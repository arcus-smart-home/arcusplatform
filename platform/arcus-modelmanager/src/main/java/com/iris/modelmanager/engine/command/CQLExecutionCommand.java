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

import java.util.regex.Pattern;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.Version;
import com.datastax.oss.driver.api.core.metadata.Node;
import com.datastax.oss.driver.api.core.servererrors.QueryExecutionException;
import com.datastax.oss.driver.api.core.servererrors.QueryValidationException;
import com.iris.modelmanager.changelog.CQLCommand;
import com.iris.modelmanager.context.Operation;
import com.iris.modelmanager.engine.ExecutionContext;

public class CQLExecutionCommand implements ExecutionCommand {

   // Cassandra 4.0 removed these table options, and rejects a statement that
   // sets one. Some history changesets set both. Their text can't change: an
   // existing deployment verifies the checksum of every changeset it applied.
   private static final Pattern REMOVED_OPTIONS = Pattern.compile(
         "\\s+AND\\s+(dclocal_)?read_repair_chance\\s*=\\s*[0-9.]+", Pattern.CASE_INSENSITIVE);

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

   private void executeCommand(CqlSession session, Operation operation) throws CommandExecutionException {
      String cql = operation == Operation.UPGRADE ? command.getUpdateCql() : command.getRollbackCql();
      if(hasCassandra4(session)) {
         cql = withoutRemovedOptions(cql);
      }
      try {
         session.execute(cql);
      } catch(QueryExecutionException | QueryValidationException e) {
         throw new CommandExecutionException(e);
      }
   }

   static String withoutRemovedOptions(String cql) {
      return REMOVED_OPTIONS.matcher(cql).replaceAll("");
   }

   // Any 4.0 node could coordinate the statement and reject the options.
   private static boolean hasCassandra4(CqlSession session) {
      for(Node node : session.getMetadata().getNodes().values()) {
         Version version = node.getCassandraVersion();
         if(version != null && version.compareTo(Version.V4_0_0) >= 0) {
            return true;
         }
      }
      return false;
   }

}
