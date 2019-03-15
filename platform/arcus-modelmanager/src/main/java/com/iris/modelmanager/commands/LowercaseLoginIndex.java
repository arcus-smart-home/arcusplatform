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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.iris.modelmanager.engine.ExecutionContext;
import com.iris.modelmanager.engine.command.CommandExecutionException;
import com.iris.modelmanager.engine.command.ExecutionCommand;

public class LowercaseLoginIndex implements ExecutionCommand {
   private static final Logger logger = LoggerFactory.getLogger(LowercaseLoginIndex.class);
   
   private static final String SELECT = "SELECT * FROM login";
   private static final String INSERT_LOGIN =
         "INSERT INTO login (domain, user_0_3, user, password, password_salt, personid) " +
         "VALUES (?, ?, ?, ?, ?, ?) " +
         "IF NOT EXISTS";
   private static final String DELETE_LOGIN =
         "DELETE FROM login WHERE domain = ? AND user_0_3 = ? AND user = ?";

   public LowercaseLoginIndex() {
      // TODO Auto-generated constructor stub
   }

   public void execute(ExecutionContext context, boolean autoRollback) throws CommandExecutionException {
      Session session = context.getSession();
      PreparedStatement insert = session.prepare(INSERT_LOGIN);
      PreparedStatement delete = session.prepare(DELETE_LOGIN);
      
      ResultSet rs = context.getSession().execute(SELECT);
      for(Row row: rs) {
         String domain = row.getString("domain");
         String user_0_3 = row.getString("user_0_3");
         String user = row.getString("user");
         if(
               !domain.equals(domain.toLowerCase()) ||
               !user_0_3.equals(user_0_3.toLowerCase()) ||
               !user.equals(user.toLowerCase())
         ) {
            // TODO async this?
            logger.debug("Converting [{}] to lower case", user);
            BoundStatement bs = insert.bind(
                  domain.toLowerCase(), 
                  user_0_3.toLowerCase(), 
                  user.toLowerCase(),
                  row.getString("password"),
                  row.getString("password_salt"),
                  row.getUUID("personid")
            );
            if(session.execute(bs).wasApplied()) {
               session.execute( delete.bind(domain, user_0_3, user) );
            }
            else {
               logger.warn("Unable to convert [{}] to lower case", user);
            }
         }
      }
   }
   
   public void rollback(ExecutionContext context, boolean autoRollback) throws CommandExecutionException {
      logger.warn("Rollback is not supported for {}", this);
   }
}

