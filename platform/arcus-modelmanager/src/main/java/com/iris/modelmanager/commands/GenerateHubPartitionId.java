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
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
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
import com.iris.util.TimeZones;

/**
 * 
 */
public class GenerateHubPartitionId implements ExecutionCommand {
   private static final Logger logger = LoggerFactory.getLogger(GenerateHubPartitionId.class);
   
   private static final String SELECT = "SELECT id, placeid FROM hub";
   private static final String UPSERT_PARTITIONID =
         "UPDATE hub " +
         "SET partitionId = ? " +
         "WHERE id = ?";
   
   private static final Pattern PATTERN_HUBID = Pattern.compile("\\w{3}-(\\d{4})");
   
   private int partitionCount = 128;
   
   public GenerateHubPartitionId() {
      // TODO Auto-generated constructor stub
   }

   public void execute(ExecutionContext context, boolean autoRollback) throws CommandExecutionException {
      Session session = context.getSession();
      PreparedStatement update = session.prepare(UPSERT_PARTITIONID);
      
      BoundStatement select = session.prepare(SELECT).bind();
      select.setConsistencyLevel(ConsistencyLevel.ALL);
      ResultSet rs = context.getSession().execute(select);
      int count = 0;
      int [] hubsPerPartition = new int[partitionCount];
      logger.info("Preparing to partition hub ids");
      long startTimeNs = System.nanoTime();
      for(Row row: rs) {
         String hubId = row.getString("id");
         UUID placeId = row.getUUID("placeid");
         int partitionId;
         if(placeId == null) {
            Matcher m = PATTERN_HUBID.matcher(hubId);
            if(!m.matches()) {
               logger.warn("Invalid hub id: [{}]", hubId);
               return;
            }
            String hubNum = m.group(1);
            partitionId = Integer.parseInt(hubNum) % partitionCount;  
         }
         else {
            partitionId = (int) (Math.floorMod(placeId.getLeastSignificantBits(), partitionCount));
         }

         logger.debug("Adding [{}] to partition [{}]", hubId, partitionId);
         BoundStatement bs = update.bind(partitionId, hubId);
         session.execute(bs);
         
         count++;
         hubsPerPartition[partitionId]++;
      }
      long duration = System.nanoTime() - startTimeNs;
      logger.info("Partitioned {} hubs in {} secs", count, duration / (float) TimeUnit.NANOSECONDS.toSeconds(1));
      for(int i=0; i<partitionCount; i++) {
         logger.info(String.format("%03d: %3d hubs", i, hubsPerPartition[i]));
      }
   }
   
   public void rollback(ExecutionContext context, boolean autoRollback) throws CommandExecutionException {
      logger.warn("Rollback is not supported for {}", this);
   }
}

