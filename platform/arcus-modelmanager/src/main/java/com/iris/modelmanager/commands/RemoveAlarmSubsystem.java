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

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.iris.modelmanager.engine.ExecutionContext;
import com.iris.modelmanager.engine.command.CommandExecutionException;
import com.iris.modelmanager.engine.command.ExecutionCommand;

/**
 * Removes alarm subsystem on rollback.
 * @author tweidlin
 *
 */
public class RemoveAlarmSubsystem implements ExecutionCommand {
   private static final Logger logger = LoggerFactory.getLogger(RemoveAlarmSubsystem.class);

   private static final String SELECT_PLACE = "SELECT id FROM place";
   private static final String SUSPEND_ALARMSUBSYSTEM = "DELETE FROM subsystem WHERE placeid = ? AND namespace = 'subalarm'";

   @Override
   public void execute(ExecutionContext context, boolean autoRollback) throws CommandExecutionException {
      
   }

   @Override
   public void rollback(ExecutionContext context, boolean autoRollback) throws CommandExecutionException {
      doExecute(context);
   }
   
   protected void doExecute(ExecutionContext context) throws CommandExecutionException {
      PreparedStatement stmt = context.getSession().prepare(SUSPEND_ALARMSUBSYSTEM);

      for(Row row: context.getSession().execute(SELECT_PLACE)) {
   		UUID placeId = row.getUUID("id");
      	logger.debug("Disabling for place [{}]", placeId);
   		context.getSession().execute( stmt.bind(placeId) );
      }
   }
}

