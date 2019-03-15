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
package com.iris.client.impl;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.iris.capability.definition.StaticDefinitionRegistry;
import com.iris.client.ClientMessage;
import com.iris.client.impl.json.ClientMessageTypeAdapter;
import com.iris.client.impl.json.DateTypeAdapter;

/**
 * 
 */
public class ClientMessageSerializer {
   private static final Logger logger = LoggerFactory.getLogger(ClientMessageSerializer.class);
   
   private final Gson gson;
   
   private static class ClientInstanceHolder {
      private static final ClientMessageSerializer instance = new ClientMessageSerializer();       
   }

   private ClientMessageSerializer() {
      gson =
         new GsonBuilder()
            .registerTypeAdapter(ClientMessage.class, new ClientMessageTypeAdapter(StaticDefinitionRegistry.getInstance()))
            .registerTypeAdapter(Date.class, new DateTypeAdapter())
            .create();
   }

    private static ClientMessageSerializer instance() {
        return ClientInstanceHolder.instance;
    }
    
    /**
     * @param json The JSON string to deserialize
     * @param clazz The class of object to deserialize into
     * @param <T> Type of object deserialized
     * @return The instance of the deserialized class
     */
    public static <T> T deserialize(String json, Class<T> clazz) {
        return instance().gson.fromJson(json, clazz);
    }

    /**
     * @param obj The object to serialize
     * @return JSON string representation of the object
     */
    public static String serialize(Object obj) {
        return instance().gson.toJson(obj);
    }
}

