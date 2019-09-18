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
/**
 * 
 */
package com.iris.modelmanager.commands;

import java.util.TimeZone;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
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
import com.iris.util.TimeZones;

/**
 * 
 */
public class GenerateTimeZoneId implements ExecutionCommand {
   private static final Logger logger = LoggerFactory.getLogger(GenerateTimeZoneId.class);
   
   private static final String SELECT = "SELECT id, tzName, tzOffset, tzUsesDst FROM place";
   private static final String UPDATE_PLACE =
         "UPDATE place " +
         "SET tzId = ?, tzName = ?, tzOffset = ?, tzUsesDst = ? " +
         "WHERE id = ?";

   public GenerateTimeZoneId() {
      // TODO Auto-generated constructor stub
   }

   public void execute(ExecutionContext context, boolean autoRollback) throws CommandExecutionException {
      Session session = context.getSession();
      PreparedStatement update = session.prepare(UPDATE_PLACE);
      
      ResultSet rs = context.getSession().execute(SELECT);
      for(Row row: rs) {
         UUID placeId = row.getUUID("id");
         String tzName = row.getString("tzName");
         if(StringUtils.isEmpty(tzName)) {
            continue;
         }

         logger.debug("Attmempting to repair timzone [{}]...", tzName);
         Double offset = row.getDouble("tzOffset");
         Boolean usesDst = row.getBool("tzUsesDst");
         
         TimeZone tz;
         try {
            tz = TimeZones.guessTimezone(tzName, offset, usesDst);
         }
         catch(Exception e) {
            logger.warn("Unable to determine timezone for place [{}]", placeId, e);
            continue;
         }

         logger.info("Found timezone [{}] for name [{}]", tz.getID(), tzName);
         BoundStatement bs = update.bind(
               tz.getID(), 
               tzName, 
               TimeZones.getOffsetAsHours(tz.getRawOffset()), 
               tz.useDaylightTime(),
               placeId
         );
         session.execute(bs);
      }
   }
   
   public void rollback(ExecutionContext context, boolean autoRollback) throws CommandExecutionException {
      logger.warn("Rollback is not supported for {}", this);
   }
}

