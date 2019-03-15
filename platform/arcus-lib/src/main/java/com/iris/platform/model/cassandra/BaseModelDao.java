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
package com.iris.platform.model.cassandra;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.common.collect.ImmutableMap;
import com.iris.capability.definition.AttributeDefinition;
import com.iris.capability.definition.DefinitionRegistry;
import com.iris.capability.key.NamespacedKey;
import com.iris.io.json.JSON;
import com.iris.messages.model.Entity;
import com.iris.util.TypeMarker;

public abstract class BaseModelDao<M extends Entity<?, M>> {
   private static final Logger logger = LoggerFactory.getLogger(BaseModelDao.class);

   private final DefinitionRegistry registry;
   private final Session session;
   
   public BaseModelDao(
         DefinitionRegistry registry,
         Session session
   ) {
      this.registry = registry;
      this.session = session;
   }
   
   protected DefinitionRegistry registry() {
      return registry;
   }
   
   protected Session session() {
      return session;
   }

   protected abstract M toModel(Row row);

   protected Optional<M> find(BoundStatement stmt) {
      ResultSet rs = session().execute( stmt );
      return Optional.ofNullable( rs.one() ).map( this::toModel );
   }
   
   protected List<M> list(BoundStatement stmt) {
      ResultSet rs = session().execute( stmt );
      List<M> results = new ArrayList<>(rs.getAvailableWithoutFetching());
      rs.forEach(
         (row) -> 
            Optional
               .ofNullable( toModel(row) )
               .ifPresent( results::add )
      );
      return results;
   }
   
   protected Map<String, Object> decode(Map<String, String> encoded) {
      if(encoded == null) {
         return ImmutableMap.of();
      }
      Map<String, Object> decoded = new HashMap<>((encoded.size()+1)*4/3,0.75f);
      encoded
         .forEach(
               (key, value) -> decoded.put(key, decode(key, value))
         );
      return decoded;
   }

   protected Object decode(String attributeName, String encoded) {
      TypeMarker<?> marker = typeOf(attributeName);
      try {
         return JSON.fromJson(encoded, marker);
      }
      catch(Exception e) {
         logger.warn("Unable to decode attribute [{}]", attributeName, e);
         return null;
      }
   }

   protected Map<String, String> encode(Map<String, Object> decoded) {
      if(decoded == null) {
         return ImmutableMap.of();
      }
      Map<String, String> encoded = new HashMap<>((decoded.size()+1)*4/3,0.75f);
      decoded
         .forEach(
               (key, value) -> encoded.put(key, encode(key, value))
         );
      return encoded;
   }

   protected String encode(String attributeName, Object value) {
   //      TypeMarker<?> marker = typeOf(attributeName);
         // TODO ensure proper type when serializing?
         return JSON.toJson(value);
      }

   protected TypeMarker<?> typeOf(String attributeName) {
   	if(attributeName.startsWith("_")) {
   		logger.trace("Ignoring private attribute [{}]", attributeName);
   		return TypeMarker.object();
   	}
   	
      NamespacedKey key = NamespacedKey.parse(attributeName);
      AttributeDefinition ad = registry.getAttribute(key.getNamedRepresentation());
      if(ad == null) {
         if (!attributeName.startsWith("_")) {
            logger.warn("Unrecognized attribute [{}] may lose type information when deserialized", attributeName);
         }

         return TypeMarker.object(); 
      }
      return TypeMarker.wrap(ad.getType().getJavaType());
   }

   /**
    * The key for removing an attribute.
    * @param name
    * @return
    */
   protected static final Object remove(String name) { 
      return null; 
   }

}

