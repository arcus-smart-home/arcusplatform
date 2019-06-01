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
package com.iris.modelmanager.changelog.dao;

import java.util.LinkedList;
import java.util.List;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.iris.modelmanager.Status;
import com.iris.modelmanager.changelog.CQLCommand;
import com.iris.modelmanager.changelog.ChangeSet;
import com.iris.modelmanager.changelog.Command;
import com.iris.modelmanager.changelog.JavaCommand;

public class ChangeSetDAO {

   private static final String UPSERT_STATEMENT =
         "INSERT INTO changeset (id, author, identifier, description, tracking, checksum, updateStatements, rollbackStatements, " +
         "status, timestamp, source, version) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

   private static final String FIND_APPLIED = "SELECT * FROM changeset WHERE status=?";

   private final Session session;
   private PreparedStatement upsertStatement;
   private PreparedStatement findApplied;

   public ChangeSetDAO(Session session) {
      this.session = session;
   }

   public void update(ChangeSet changeset) {

      // can't prepare these statements early because the tables may not exist until the bootstrapping
      if(upsertStatement == null) {
         upsertStatement = this.session.prepare(UPSERT_STATEMENT);
      }

      BoundStatement boundStatement = new BoundStatement(upsertStatement);
      this.session.execute(boundStatement.bind(
            changeset.getUniqueIdentifier(),
            changeset.getAuthor(),
            changeset.getIdentifier(),
            changeset.getDescription(),
            changeset.getTracking(),
            changeset.getChecksum(),
            formatUpdateStatements(changeset.getCommands()),
            formatRollbackStatements(changeset.getCommands()),
            changeset.getStatus().toString(),
            changeset.getTimestamp(),
            changeset.getSource(),
            changeset.getVersion()
      ));
   }

   public List<ChangeSet> getAppliedChangeSets() {

      // can't prepare these statements early because the tables may not exist until the bootstrapping
      if(findApplied == null) {
         findApplied = this.session.prepare(FIND_APPLIED);
      }

      BoundStatement boundStatement = new BoundStatement(findApplied);
      List<Row> rows = this.session.execute(boundStatement.bind(Status.APPLIED.toString())).all();
      List<ChangeSet> changesets = new LinkedList<ChangeSet>();
      for(Row r : rows) {
         changesets.add(buildChangeSet(r));
      }
      return changesets;
   }

   private ChangeSet buildChangeSet(Row row) {
      ChangeSet cs = new ChangeSet();
      cs.setAuthor(row.getString("author"));
      cs.setChecksum(row.getString("checksum"));
      cs.setDescription(row.getString("description"));
      cs.setIdentifier(row.getString("identifier"));
      cs.setSource(row.getString("source"));
      cs.setStatus(Status.valueOf(row.getString("status")));
      cs.setTimestamp(row.getTimestamp("timestamp"));
      cs.setTracking(row.getString("tracking"));
      cs.setVersion(row.getString("version"));
      return cs;
   }

   private String formatUpdateStatements(List<Command> commands) {
      StringBuilder sb = new StringBuilder();
      for(Command command : commands) {
         if(command instanceof CQLCommand) {
            sb.append(((CQLCommand) command).getUpdateCql());
         } else if(command instanceof JavaCommand) {
            sb.append(((JavaCommand) command).getClassName() + "::execute");
         }
         sb.append("; ");
      }
      return sb.toString();
   }

   private String formatRollbackStatements(List<Command> commands) {
      StringBuilder sb = new StringBuilder();
      for(Command command : commands) {
         if(command instanceof CQLCommand) {
            sb.append(((CQLCommand) command).getRollbackCql());
         } else if(command instanceof JavaCommand) {
            sb.append(((JavaCommand) command).getClassName() + "::rollback");
         }
         sb.append("; ");
      }
      return sb.toString();
   }
}

