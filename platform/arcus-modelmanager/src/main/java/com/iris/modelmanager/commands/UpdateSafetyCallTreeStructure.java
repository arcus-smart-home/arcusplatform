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

import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.iris.modelmanager.engine.ExecutionContext;
import com.iris.modelmanager.engine.command.CommandExecutionException;
import com.iris.modelmanager.engine.command.ExecutionCommand;


public class UpdateSafetyCallTreeStructure implements ExecutionCommand {

   private static final String callTreeKey = "subsafety:callTree";

   private static final String loadSubsystems = "select * from subsystem";
   private static final String loadPeopleForPlace = "select entityid from authorization_grant_by_place WHERE placeId = ?";
   private static final String updateCallTree = "update subsystem set attributes['subsafety:callTree'] = ? WHERE placeId = ? AND namespace = ?";
   private static final Type oldCallTreeType = new TypeToken<List<String>>(){}.getType();
   private static final Type newCallTreeType = new TypeToken<List<Map<String,Object>>>(){}.getType();

   private final Gson gson = new GsonBuilder().create();

   @Override
   public void execute(ExecutionContext context, boolean autoRollback) throws CommandExecutionException {
      try {
         updateToCallTreeEntry(context.getSession());
      } catch(Exception e) {
         throw new CommandExecutionException(e);
      }
   }

   @Override
   public void rollback(ExecutionContext context, boolean autoRollback) throws CommandExecutionException {
      try {
         rollbackFromCallTreeEntry(context.getSession());
      } catch(Exception e) {
         throw new CommandExecutionException(e);
      }
   }

   private void updateToCallTreeEntry(Session session) {
      PreparedStatement peopleForPlaceStmt = session.prepare(loadPeopleForPlace);
      PreparedStatement updateStmt = session.prepare(updateCallTree);

      List<Row> safetySubsystems = listAllSafetySubsystems(session);
      safetySubsystems.forEach((r) -> {
         if(isOldCallTree(r)) {
            List<String> allPeople = getAddressesForPeopleAtPlace(r.getUUID("placeid"), session, peopleForPlaceStmt);
            List<String> currentCallTree = getCurrentCallTree(r);
            session.execute(new BoundStatement(updateStmt).bind(createNewCallTree(currentCallTree, allPeople), r.getUUID("placeid"), "subsafety"));
         }
      });
   }

   private boolean isOldCallTree(Row r) {
      Map<String,String> attributes = r.getMap("attributes", String.class, String.class);
      String callTree = attributes.get(callTreeKey);
      // new call tree entries contain an object
      if(callTree.contains("{")) {
         return false;
      }
      return true;
   }

   private void rollbackFromCallTreeEntry(Session session) {
      PreparedStatement updateStmt = session.prepare(updateCallTree);

      List<Row> safetySubsystems = listAllSafetySubsystems(session);
      safetySubsystems.forEach((r) -> {
         if(!isOldCallTree(r)) {
            List<Map<String,Object>> currentCallTree = getCallTreeEntries(r);
            session.execute(new BoundStatement(updateStmt).bind(
                  serializeOldCallTree(transformToOld(currentCallTree)),
                  r.getUUID("placeid"),
                  "subsafety"));
         }
      });
   }

   private List<Row> listAllSafetySubsystems(Session session) {
      return session.execute(loadSubsystems).all().stream()
            .filter((r) -> { return r.getString("namespace").equals("subsafety"); })
            .collect(Collectors.toList());
   }

   private String createNewCallTree(List<String> currentCallTree, List<String> allPeople) {
      List<Map<String,Object>> callTree = new LinkedList<Map<String,Object>>();
      currentCallTree.forEach((s) -> { callTree.add(createCallTreeEntry(s, true)); });
      allPeople.stream().filter((s) -> { return !currentCallTree.contains(s); })
         .forEach((s) -> { callTree.add(createCallTreeEntry(s, false)); });
      return serializeCallTree(callTree);
   }

   private List<String> getAddressesForPeopleAtPlace(UUID place, Session session, PreparedStatement stmt) {
      List<Row> rows = session.execute(new BoundStatement(stmt).bind(place)).all();
      return rows.stream()
            .map((r) -> { return "SERV:person:" + r.getUUID("entityid").toString(); })
            .collect(Collectors.toList());
   }

   private List<String> getCurrentCallTree(Row r) {
      List<String> callTree = gson.fromJson(r.getMap("attributes", String.class, String.class).get(callTreeKey), oldCallTreeType);
      return callTree;
   }

   private Map<String,Object> createCallTreeEntry(String address, boolean enabled) {
      return ImmutableMap.<String,Object>of("person", address, "enabled", enabled);
   }

   private String serializeCallTree(List<Map<String,Object>> callTree) {
      return gson.toJson(callTree, newCallTreeType);
   }

   private List<Map<String,Object>> getCallTreeEntries(Row r) {
      List<Map<String,Object>> callTree = gson.fromJson(r.getMap("attributes", String.class, String.class).get(callTreeKey), newCallTreeType);
      return callTree;
   }

   private List<String> transformToOld(List<Map<String,Object>> callTree) {
      return callTree.stream().filter((m) -> { return Boolean.TRUE.equals(m.get("enabled")); })
            .map((m) -> { return (String) m.get("person"); })
            .collect(Collectors.toList());
   }

   private String serializeOldCallTree(List<String> callTree) {
      return gson.toJson(callTree, oldCallTreeType);
   }
}

