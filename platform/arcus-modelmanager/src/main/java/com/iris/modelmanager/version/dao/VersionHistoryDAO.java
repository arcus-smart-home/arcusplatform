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
package com.iris.modelmanager.version.dao;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.iris.modelmanager.Status;
import com.iris.modelmanager.version.VersionHistory;

public class VersionHistoryDAO {

   private static final String IDENTIFIER = "version_history";
   private static final String INSERT = "INSERT INTO versionhistory (id, version, timestamp, status, username) VALUES (?, ?, ?, ?, ?)";
   private static final String FIND_LATEST = "SELECT * FROM versionhistory WHERE id=? AND status=? LIMIT 1";

   private final Session session;
   private PreparedStatement insert;
   private PreparedStatement findLatest;

   public VersionHistoryDAO(Session session) {
      this.session = session;
   }

   public void insert(VersionHistory history) {

      // can't prepare these early because they may not exist if the system has not been bootstrapped
      if(insert == null) {
         insert = this.session.prepare(INSERT);
      }

      BoundStatement boundStatement = new BoundStatement(insert);
      boundStatement.setConsistencyLevel(ConsistencyLevel.ALL);
      session.execute(boundStatement.bind(
            IDENTIFIER,
            history.getVersion(),
            history.getTimestamp(),
            history.getStatus().toString(),
            history.getUsername()
      ));
   }

   public VersionHistory findLatest() {

      // can't prepare these early because they may not exist if the system has not been bootstrapped
      if(findLatest == null) {
         findLatest = this.session.prepare(FIND_LATEST);
      }

      BoundStatement boundStatement = new BoundStatement(findLatest);
      Row row = session.execute(boundStatement.bind(IDENTIFIER, Status.APPLIED.toString())).one();

      if(row == null) {
         return null;
      }

      return buildVersionHistory(row);
   }

   private VersionHistory buildVersionHistory(Row row) {
      VersionHistory history = new VersionHistory();
      history.setStatus(Status.valueOf(row.getString("status")));
      history.setTimestamp(row.getDate("timestamp"));
      history.setUsername(row.getString("username"));
      history.setVersion(row.getString("version"));
      return history;
   }
}

