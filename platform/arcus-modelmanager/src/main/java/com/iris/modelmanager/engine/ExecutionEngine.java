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
package com.iris.modelmanager.engine;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang3.StringUtils;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.InvalidQueryException;
import com.iris.modelmanager.Status;
import com.iris.modelmanager.changelog.ChangeLog;
import com.iris.modelmanager.changelog.ChangeLogSet;
import com.iris.modelmanager.changelog.ChangeSet;
import com.iris.modelmanager.changelog.checksum.ChecksumInvalidException;
import com.iris.modelmanager.changelog.deserializer.ChangeLogDeserializer;
import com.iris.modelmanager.context.ManagerContext;
import com.iris.modelmanager.context.Operation;
import com.iris.modelmanager.context.Profile;
import com.iris.modelmanager.engine.command.ChangeLogExecutionCommand;
import com.iris.modelmanager.engine.command.CommandExecutionException;
import com.iris.modelmanager.engine.command.CompositeExecutionCommand;
import com.iris.modelmanager.engine.command.CreateMetadataTablesCommand;
import com.iris.modelmanager.engine.command.ExecutionCommand;
import com.iris.modelmanager.engine.command.ImportLocalizationKeysCommand;
import com.iris.modelmanager.version.Version;
import com.iris.modelmanager.version.VersionHistory;

public class ExecutionEngine {

   private final ExecutionContext context;

   public ExecutionEngine(ManagerContext managerContext) {
      context = new ExecutionContext(createSession(managerContext.getProfile()), managerContext);
   }

   ExecutionContext getExecutionContext() {
      return context;
   }

   public void execute() throws CommandExecutionException, XMLStreamException, ChecksumInvalidException {
      try {
         if(!metadataTablesExist()) {
            createMetadataTables();
         }

         ChangeLogSet xmlChanges = loadXML();
         ChangeLogSet dbState = loadDB();

         try {
         	xmlChanges.verify(dbState);
         }
         catch(ChecksumInvalidException e) {
         	if(context.getManagerContext().isAuto()) {
         		throw e;
         	}
         	else {
	         	System.out.println("Invalid checksum: " + e.getMessage());
	         	System.out.println("WARNING: This schema appears to be corrupt, continuing execution could result in data loss, are you sure you want to continue?");
	         	if(promptToContinue()) {
	         		xmlChanges = loadXML();
	         		xmlChanges.verify(dbState, false);
	         	}
	         	else {
	         		System.exit(0);
	         	}
         	}
         }

         List<ChangeLog> changesToApply = identifyChanges(xmlChanges, dbState);

         dumpChanges(changesToApply);

         if(!promptToContinue()) {
            System.exit(0);
         }

         List<ExecutionCommand> commands = new LinkedList<ExecutionCommand>();
         for(ChangeLog cl : changesToApply) {
            commands.add(new ChangeLogExecutionCommand(cl));
         }
         commands.add(new ImportLocalizationKeysCommand());

         System.out.println("\nExecuting...");

         executeCommands(commands);

         // if we've rolled back make sure that the target version is now the latest version installed
         if(context.getManagerContext().getOperation() == Operation.ROLLBACK) {
            VersionHistory history = new VersionHistory();
            history.setStatus(Status.APPLIED);
            history.setTimestamp(new Date());
            history.setUsername(context.getManagerContext().getProfile().getUsername());
            history.setVersion(context.getManagerContext().getRollbackTarget());
            context.getVersionHistoryDAO().insert(history);
         }


         System.out.println("...Done!");

      } finally {
         Session session = context.getSession();
         if(session != null) {
            Cluster cluster = session.getCluster();
            session.close();
            cluster.close();
         }
      }
   }

   void executeCommands(List<ExecutionCommand> commands) throws CommandExecutionException {
      CompositeExecutionCommand command = new CompositeExecutionCommand(commands);
      if(context.getManagerContext().getOperation() == Operation.UPGRADE) {
         command.execute(context, true);
      } else {
         command.rollback(context, true);
      }
   }

   private void dumpChanges(List<ChangeLog> changeLogs) {
      System.out.println("The following " + context.getManagerContext().getOperation().toString().toLowerCase() + " changes will be executed:\n");
      int counter = 1;
      for(ChangeLog changelog : changeLogs) {
         for(ChangeSet cs : changelog.getChangeSets()) {
            System.out.print(counter++ + ")  ");
            System.out.println(cs.getSource() + "::" + cs.getUniqueIdentifier() + " - " + cs.getDescription());
         }
      }
      System.out.println(counter++ + ")  Import localization strings");
   }

   private boolean promptToContinue() {
      if(context.getManagerContext().isAuto()) {
         return true;
      }

      System.out.print("\nPress Y and enter to continue:  ");
      String result = System.console().readLine();
      if(result.toLowerCase().startsWith("y")) {
         return true;
      }
      return false;
   }

   private List<ChangeLog> identifyChanges(ChangeLogSet xmlChanges, ChangeLogSet dbSet) {
      if(context.getManagerContext().getOperation() == Operation.UPGRADE) {
         return xmlChanges.diff(dbSet);
      }

      List<ChangeLog> pruned = xmlChanges.prune(Version.valueOf(context.getManagerContext().getRollbackTarget()));
      Collections.reverse(pruned);
      return pruned;
   }

   private void createMetadataTables() throws CommandExecutionException {
      CreateMetadataTablesCommand command = new CreateMetadataTablesCommand();
      command.execute(context, true);
   }

   private boolean metadataTablesExist() {
      try {
         context.getSession().execute("select * from changeset");
         return true;
      } catch(InvalidQueryException iqe) {
         return false;
      }
   }

   private Session createSession(Profile profile) {
   	QueryOptions options = new QueryOptions();
   	options.setConsistencyLevel(profile.getConsistencyLevel());
      Cluster.Builder builder = Cluster.builder();
      builder.addContactPoints(profile.getNodes().toArray(new String[0]));
      builder.withPort(profile.getPort());
      builder.withQueryOptions(options);

      if(!StringUtils.isBlank(profile.getUsername()) && !StringUtils.isBlank(profile.getPassword())) {
         builder.withCredentials(profile.getUsername(), profile.getPassword());
      }

      Cluster cluster = builder.build();
      Session session = cluster.connect(profile.getKeyspace());
      return session;
   }

   private ChangeLogSet loadXML() throws XMLStreamException {
      ChangeLogDeserializer deserializer = new ChangeLogDeserializer(context.getManagerContext());
      return deserializer.deserialize(context.getManagerContext().getChangelog());
   }

   private ChangeLogSet loadDB() {
      List<ChangeSet> appliedSets = context.getChangeSetDAO().getAppliedChangeSets();
      return ChangeLogSet.fromChangesets(appliedSets);
   }
}

