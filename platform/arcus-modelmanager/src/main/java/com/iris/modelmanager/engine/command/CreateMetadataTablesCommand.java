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

import java.util.Arrays;

import com.iris.modelmanager.changelog.CQLCommand;

public class CreateMetadataTablesCommand extends CompositeExecutionCommand {

   private static final String CREATE_CHANGESET = "CREATE TABLE IF NOT EXISTS changeset (" +
         "id text PRIMARY KEY, " +
         "author text, " +
         "identifier text, " +
         "description text," +
         "tracking text, " +
         "checksum text, " +
         "updateStatements text, " +
         "rollbackStatements text, " +
         "status text, " +
         "timestamp timestamp, " +
         "source text, " +
         "version text)";
   private static final String DROP_CHANGESET = "DROP TABLE IF EXISTS changeset";

   private static final String CREATE_STATUS_INDEX = "CREATE INDEX IF NOT EXISTS changeset_status ON changeset(status)";
   private static final String DROP_STATUS_INDEX = "DROP INDEX IF EXISTS changeset_status";

   private static final String CREATE_VERSION_INDEX = "CREATE INDEX IF NOT EXISTS changeset_version ON changeset(version)";
   private static final String DROP_VERSION_INDEX = "DROP INDEX IF EXISTS changeset_version";

   private static final String CREATE_VERSIONHISTORY = "CREATE TABLE IF NOT EXISTS versionhistory (" +
         "id text, " +
         "version text, " +
         "timestamp timestamp, " +
         "status text, " +
         "username text, " +
         "PRIMARY KEY(id, timestamp)) " +
         "WITH CLUSTERING ORDER BY (timestamp DESC)";
   private static final String DROP_VERSIONHISTORY = "DROP TABLE versionhistory";

   private static final String CREATE_HISTORY_STATUS_INDEX = "CREATE INDEX IF NOT EXISTS versionhistory_status ON versionhistory(status)";
   private static final String DROP_HISTORY_STATUS_INDEX = "DROP INDEX IF EXISTS versionhistory_status";

   public CreateMetadataTablesCommand() {
      super(Arrays.asList(
            new CQLExecutionCommand(new CQLCommand(CREATE_CHANGESET, DROP_CHANGESET)),
            new CQLExecutionCommand(new CQLCommand(CREATE_STATUS_INDEX, DROP_STATUS_INDEX)),
            new CQLExecutionCommand(new CQLCommand(CREATE_VERSION_INDEX, DROP_VERSION_INDEX)),
            new CQLExecutionCommand(new CQLCommand(CREATE_VERSIONHISTORY, DROP_VERSIONHISTORY)),
            new CQLExecutionCommand(new CQLCommand(CREATE_HISTORY_STATUS_INDEX, DROP_HISTORY_STATUS_INDEX))
      ));
   }
}

