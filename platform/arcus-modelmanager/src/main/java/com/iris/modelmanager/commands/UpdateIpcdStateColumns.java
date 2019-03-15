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

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.iris.modelmanager.engine.ExecutionContext;
import com.iris.modelmanager.engine.command.CommandExecutionException;
import com.iris.modelmanager.engine.command.ExecutionCommand;

public class UpdateIpcdStateColumns implements ExecutionCommand {

   private static final Logger logger = LoggerFactory.getLogger(UpdateIpcdStateColumns.class);

   private static final String SELECT = "SELECT protocoladdress,placeid FROM ipcd_device";
   private static final String UPDATE_STATES = "UPDATE ipcd_device SET connState = ?, registrationState = ? WHERE protocoladdress = ? IF EXISTS";

   public UpdateIpcdStateColumns() {
   }

   public void execute(ExecutionContext context, boolean autoRollback) throws CommandExecutionException {
      Session session = context.getSession();
      PreparedStatement update = session.prepare(UPDATE_STATES);

      BoundStatement select = session.prepare(SELECT).bind();
      select.setConsistencyLevel(ConsistencyLevel.ALL);
      ResultSet rs = context.getSession().execute(select);
      for(Row row: rs) {
         String protocolAddress = row.getString("protocoladdress");
         UUID placeId = row.getUUID("placeid");
         BoundStatement bs = new BoundStatement(update);
         bs.setString("connState", "ONLINE");
         bs.setString("registrationState", placeId == null ? "UNREGISTERED" : "REGISTERED");
         bs.setString("protocolAddress", protocolAddress);
         session.execute(bs);
      }
   }

   public void rollback(ExecutionContext context, boolean autoRollback) throws CommandExecutionException {
      logger.warn("Rollback is not supported for {}", this);
   }

}

