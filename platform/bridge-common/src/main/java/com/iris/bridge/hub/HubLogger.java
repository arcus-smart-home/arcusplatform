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
package com.iris.bridge.hub;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.annotation.PreDestroy;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.messaging.kafka.KafkaOpsConfig;
import com.iris.core.messaging.kafka.KafkaSerializers;
import com.iris.messages.HubMessage;
import com.iris.messages.HubMessage.Type;

@Singleton
public class HubLogger {
   public static final String LOG_TOPIC = "irisLog";

   private final KafkaProducer<Void, JsonObject> irisLogSender;

   @Inject
   public HubLogger(KafkaOpsConfig config) {
      this.irisLogSender = new KafkaProducer<>(config.toNuProducerProperties(), KafkaSerializers.voidSerializer(), KafkaSerializers.jsonSerializer());
   }

   public void log(HubMessage message, String placeId, String hubId) {
      Preconditions.checkArgument(message.getType() == Type.LOG, "Only log messages can be handled by the logger");
      JsonArray logs = bytesToLogs(message.getPayload());
      log(logs, placeId, hubId);
   }

   public void log(JsonArray logs, String placeId, String hubId) {
      for (JsonElement elem : logs) {
         JsonObject object = (JsonObject)elem;
         object.addProperty("hub", hubId);
         object.addProperty("svc", "hub-agent");
         object.addProperty("svr", "unknown");
         object.addProperty("place", placeId);
         irisLogSender.send(new ProducerRecord<Void, JsonObject>(LOG_TOPIC, object));
      }
   }

   private JsonArray bytesToLogs(byte[] payload) {
      String spayload = new String(payload, StandardCharsets.UTF_8);
      JsonParser parser = new JsonParser();
      JsonElement elem = parser.parse(spayload);
      return elem.getAsJsonArray();
   }

   @PreDestroy
   public void close() throws IOException {
      // we opened it, we need to close it
      irisLogSender.close();
   }

}

