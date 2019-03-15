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

import java.util.UUID;
import java.util.concurrent.TimeUnit;

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

/**
 *
 */
public class GenerateIpcdPartitionId implements ExecutionCommand {

   private static final Logger logger = LoggerFactory.getLogger(GenerateIpcdPartitionId.class);

   private static final String SELECT = "SELECT protocoladdress, placeid FROM ipcd_device";
   private static final String UPSERT_PARTITIONID =
         "UPDATE ipcd_device " +
         "SET partitionId = ? " +
         "WHERE protocoladdress = ?";

   private int partitionCount = 128;

   public GenerateIpcdPartitionId() {
   }

   public void execute(ExecutionContext context, boolean autoRollback) throws CommandExecutionException {
      Session session = context.getSession();
      PreparedStatement update = session.prepare(UPSERT_PARTITIONID);

      BoundStatement select = session.prepare(SELECT).bind();
      select.setConsistencyLevel(ConsistencyLevel.ALL);
      ResultSet rs = context.getSession().execute(select);
      int count = 0;
      int [] devsPerPartition = new int[partitionCount];
      logger.info("Preparing to partition ipcd devices...");
      long startTimeNs = System.nanoTime();
      for(Row row: rs) {
         String protocolAddress = row.getString("protocoladdress");
         UUID placeId = row.getUUID("placeid");
         int partitionId;
         if(placeId == null) {
            partitionId = 0;
         } else {
            partitionId = (int) (Math.floorMod(placeId.getLeastSignificantBits(), partitionCount));
         }

         logger.debug("Adding [{}] to partition [{}]", protocolAddress, partitionId);
         BoundStatement bs = update.bind(partitionId, protocolAddress);
         session.execute(bs);

         count++;
         devsPerPartition[partitionId]++;
      }
      long duration = System.nanoTime() - startTimeNs;
      logger.info("Partitioned {} ipcd devices in {} secs", count, duration / (float) TimeUnit.NANOSECONDS.toSeconds(1));
      for(int i=0; i<partitionCount; i++) {
         logger.info(String.format("%03d: %3d devs", i, devsPerPartition[i]));
      }
   }

   public void rollback(ExecutionContext context, boolean autoRollback) throws CommandExecutionException {
      logger.warn("Rollback is not supported for {}", this);
   }
}

