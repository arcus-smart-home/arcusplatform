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

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.iris.io.json.JSON;
import com.iris.modelmanager.engine.ExecutionContext;
import com.iris.modelmanager.engine.command.CommandExecutionException;
import com.iris.modelmanager.engine.command.ExecutionCommand;
import com.iris.util.TypeMarker;

public class SetSceneEnabled implements ExecutionCommand {
   private static final Logger logger = LoggerFactory.getLogger(SetSceneEnabled.class);
   private static final TypeMarker<List<Map<String, Object>>> TYPE_ACTION = new TypeMarker<List<Map<String, Object>>>() {
   };

   private static final String SELECT_RULEENVIRONMENT = "SELECT id, placeid, action, type FROM ruleenvironment";
   private static final String SELECT_FROM_PLACE = "SELECT id, servicelevel FROM place";
   private static final String UPDATE_SCENEENABLED = "UPDATE ruleenvironment set sceneenabled = ? WHERE placeid=? AND id = ? and type = ?";

   @Override
   public void execute(ExecutionContext context, boolean autoRollback) throws CommandExecutionException {
      PreparedStatement stmt = context.getSession().prepare(UPDATE_SCENEENABLED);

      List<Row> rows = context.getSession().execute(SELECT_FROM_PLACE).all();

      Set<UUID> premiumPlaces = rows.stream()
            .filter(r -> r.getString("servicelevel") != null && r.getString("servicelevel").equals("PREMIUM"))
            .map(row -> row.getUUID("id"))
            .collect(Collectors.toSet());

      ResultSet rs = context.getSession().execute(SELECT_RULEENVIRONMENT);
      BatchStatement batch = new BatchStatement();
      for (Row row : rs){
         int id = row.getInt("id");
         String type = row.getString("type");
         UUID place = row.getUUID("placeid");
         ByteBuffer actionBytes = row.getBytes("action");
         if (!"scene".equals(type)) {
            continue;

         }

         boolean hasAction = false;

         if (actionBytes != null) {
            byte[] array = new byte[actionBytes.remaining()];
            actionBytes.get(array);
            hasAction = hasAction(array);
         }

         boolean sceneenabled = false;
         if (premiumPlaces.contains(place) && hasAction) {
            sceneenabled = true;
         }
         BoundStatement statement = new BoundStatement(stmt)
               .setBool("sceneenabled", sceneenabled)
               .setInt("id", id)
               .setString("type", type)
               .setUUID("placeid", place);
         context.getSession().execute(statement);
      }

   }

   private boolean hasAction(byte[] action) {
      if (action.length == 0) {
         return false;
      }
      try{
         String json = new String(action);
         List actions = JSON.fromJson(json, TYPE_ACTION);
         if (!actions.isEmpty()) {
            return true;
         }
      }catch (Exception e){
         logger.warn("error parsing json {}", e);
      }
      return false;
   }

   @Override
   public void rollback(ExecutionContext context, boolean autoRollback) throws CommandExecutionException {
      logger.warn("Rollback is not supported for {}", this);
   }
}

