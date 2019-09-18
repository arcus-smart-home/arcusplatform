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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
import com.iris.util.IrisCollections;

public class UpdateSecurityQuestions implements ExecutionCommand {
   private static final Map<String, String> oldToNew = 
         IrisCollections
            .<String, String>immutableMap()
            .put("question1", "question4")
            .put("question2", "question7")
            .put("question3", "question1")
            .put("question4", "question5")
            .put("question6", "question2")
            .put("question7", "question9")
            .put("question8", "question3")
            .put("question9", "question11")
            .put("question11", "question8")
            .put("question12", "question12")
            .put("question13", "question13")
            .put("question14", "question14")
            .create()
            ;
            
   private static final Logger logger = LoggerFactory.getLogger(UpdateSecurityQuestions.class);
   
   private static final String SELECT = "SELECT id, securityAnswers FROM person";
   private static final String UPDATE =
         "UPDATE person " +
         "SET securityAnswers = ? " +
         "WHERE id = ?";
   
   public UpdateSecurityQuestions() {
      // TODO Auto-generated constructor stub
   }

   @Override
   public void execute(ExecutionContext context, boolean autoRollback) throws CommandExecutionException {
      Session session = context.getSession();
      PreparedStatement update = session.prepare(UPDATE);
      
      ResultSet rs = context.getSession().execute(SELECT);
      for(Row row: rs) {
         UUID id = row.getUUID("id");
         Map<String, String> oldQuestions = row.getMap("securityAnswers", String.class, String.class);
         Map<String, String> newQuestions = rewrite(oldQuestions);

         logger.debug("Remapping security answers for [{}]...", id);

         BoundStatement bs = update.bind(newQuestions, id);
         session.execute(bs);
      }
   }
   
   @Override
   public void rollback(ExecutionContext context, boolean autoRollback) throws CommandExecutionException {
      logger.warn("Rollback is not supported for {}", this);
   }

   private Map<String, String> rewrite(Map<String, String> answers) {
      Map<String, String> result = new HashMap<>(3);
      for(Map.Entry<String, String> entry: answers.entrySet()) {
         String oldId = entry.getKey();
         String newId = oldToNew.get(oldId);
         if(newId == null) {
            logger.debug("Dropping deprecated answer for [{}]", oldId);
            continue;
         }
         
         result.put(newId, entry.getValue());
      }
      return result;
   }

}

