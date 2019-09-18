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

import com.datastax.driver.core.*;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.iris.Utils;
import com.iris.modelmanager.engine.ExecutionContext;
import com.iris.modelmanager.engine.command.CommandExecutionException;
import com.iris.modelmanager.engine.command.ExecutionCommand;
import com.iris.security.crypto.AES;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MigrateCryptoLibrary implements ExecutionCommand {
   private static final Logger logger = LoggerFactory.getLogger(MigrateCryptoLibrary.class);

   private static final String SELECT = "SELECT id, pinPerPlace, securityAnswers FROM person";
   private static final String UPDATE =
         "UPDATE person " +
         "SET securityAnswers = ?, " +
         "pinPerPlace = ?" +
         "WHERE id = ?";

   private final AES aes;

   @Inject
   @Named("questions.aes.secret")
   private String questionsAesSecret;

   @Inject
   public MigrateCryptoLibrary(AES aes) throws NoSuchAlgorithmException {
      this.aes = aes;
   }

   @Override
   public void execute(ExecutionContext context, boolean autoRollback) throws CommandExecutionException {
      if (questionsAesSecret == null) {
         throw new IllegalArgumentException("questions.aes.secret must be provided");
      }

      // TODO: Add this elsewhere?
      Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

      Session session = context.getSession();
      PreparedStatement update = session.prepare(UPDATE);

      ResultSet rs = context.getSession().execute(SELECT);
      for (Row row : rs) {
         UUID id = row.getUUID("id");
         // Handle Security Questions
         Map<String, String> oldQuestions = row.getMap("securityAnswers", String.class, String.class);
         logger.debug("Re-encrypting security answers for [{}]...", id);
         Map<String, String> newQuestions = reencryptSecurityQuestion(oldQuestions);

         // Handle user pins
         Map<String, String> newPins = null;
         Map<String, String> pinPerPlace = row.getMap("pinPerPlace", String.class, String.class);
         if (pinPerPlace != null && !pinPerPlace.isEmpty()) {
            newPins = new HashMap<>(pinPerPlace);
            logger.debug("Re-encrypting pins answers for [{}]...", id);
            newPins.replaceAll((k, v) -> {
               return aes.encrypt(id.toString(), aes.decrypt(id.toString(), v)); // re-encrypt
            });
         }
         BoundStatement bs = update.bind(newQuestions, newPins, id);
         session.execute(bs);
      }
   }

   @Override
   public void rollback(ExecutionContext context, boolean autoRollback) throws CommandExecutionException {
      logger.warn("Rollback is not supported for {}", this);
   }

   @SuppressWarnings("deprecation")
   private Map<String, String> reencryptSecurityQuestion(Map<String, String> answers) {
      Map<String, String> result = new HashMap<>(3);
      for (Map.Entry<String, String> entry : answers.entrySet()) {
         result.put(entry.getKey(), aes.encrypt(entry.getKey(), Utils.aesDecrypt(questionsAesSecret, entry.getValue())));
      }
      return result;
   }

}

