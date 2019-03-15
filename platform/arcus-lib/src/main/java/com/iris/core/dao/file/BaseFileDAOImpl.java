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
package com.iris.core.dao.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.iris.core.dao.CRUDDao;
import com.iris.gson.AddressTypeAdapter;
import com.iris.gson.DateTypeAdapter;
import com.iris.messages.address.Address;
import com.iris.messages.model.BaseEntity;

public abstract class BaseFileDAOImpl<I, T extends BaseEntity<I, T>> implements CRUDDao<I, T> {

   private static final Logger LOGGER = LoggerFactory.getLogger(BaseFileDAOImpl.class);

   protected final Map<I, T> entities = new HashMap<>();

   protected BaseFileDAOImpl(String resource) {
      try {
         String json = readJSON(resource);
         Gson gson = createGson();
         T[] entityArray = parseJSON(gson, json);
         for(T entity : entityArray) {
            entities.put(entity.getId(), entity);
         }
      } catch (Exception ex) {
         LOGGER.error("Exception while reading file-based dao data from '" + resource + "'", ex);
      }
   }

   protected String readJSON(String resource) throws IOException {
      InputStream input = null;
      Reader reader = null;
      try {
         input = this.getClass().getClassLoader().getResourceAsStream(resource);
         if (input != null) {
             reader = new InputStreamReader(input, "UTF-8");
             return CharStreams.toString(reader);
         } else {
            throw new IOException("Resource '" + resource + "' could not be found on the classpath");
         }
      } finally {
         if (reader != null) {
            try {
               reader.close();
            } catch (IOException e) {
               LOGGER.error("Unable to cleanly close reader for file-based dao data '" + resource + "'", e);
            }
         }
         else if (input != null) {
            try {
               input.close();
            } catch (IOException e) {
               LOGGER.error("Unable to cleanly close inputstream for file-based dao data '" + resource + "'", e);
            }
         }
      }
   }

   protected abstract T[] parseJSON(Gson gson, String json) throws IOException;

   @Override
   public T save(T entity) {

      if(entity.getId() == null) {
         entity.setCreated(new Date());
         entity.setId(nextId());
      }
      entity.setModified(new Date());
      entities.put(entity.getId(), entity.copy());
      T savedEntity = entities.get(entity.getId());
      postSave(savedEntity);
      return savedEntity;
   }

   protected void postSave(T entity) {
      // no op hook
   }

   @Override
   public T findById(I id) {
      T entity = entities.get(id);
      return entity != null ? entity.copy() : null;
   }

   @Override
   public void delete(T entity) {
      entities.remove(entity.getId());
   }

   protected abstract I nextId();

   protected void beforeDelete(T entity) {
      // no op hook
   }

   protected Gson createGson() {
      GsonBuilder builder = new GsonBuilder();
      builder.registerTypeAdapter(Date.class, new DateTypeAdapter());
      builder.registerTypeAdapter(Address.class, new AddressTypeAdapter());
      return builder.create();
   }
}

