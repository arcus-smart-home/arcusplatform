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
package com.iris.core.dao.cassandra;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.google.common.cache.CacheBuilder;
import com.iris.bootstrap.ServiceLocator;

public enum CassandraPreparedBuilder {
   STANDARD(StandardPreparedStatementFactory.INSTANCE),
   CACHED(new CachedPreparedStatementFactory(getPreparedStatementCache()));

   private final PreparedStatementFactory factory;
   private CassandraPreparedBuilder(PreparedStatementFactory factory) {
      this.factory = factory;
   }

   public PreparedStatement prepare(CqlSession session, String query) {
      return factory.prepare(session, query);
   }

   private interface PreparedStatementFactory {
      PreparedStatement prepare(CqlSession session, String query);
   }

   private static enum StandardPreparedStatementFactory implements PreparedStatementFactory {
      INSTANCE;

      @Override
      public PreparedStatement prepare(CqlSession session, String query) {
         return session.prepare(query);
      }
   }

   private static final class CachedPreparedStatementFactory implements PreparedStatementFactory {
      private final ConcurrentMap<String,PreparedStatement> cache;

      private CachedPreparedStatementFactory(ConcurrentMap<String,PreparedStatement> cache) {
         this.cache = cache;
      }

      @Override
      public PreparedStatement prepare(CqlSession session, String query) {
         PreparedStatement result = cache.get(query);
         if (result != null) {
            return result;
         }

         PreparedStatement newPrepared = session.prepare(query);
         PreparedStatement existingPrepared = cache.putIfAbsent(query, newPrepared);
         return (existingPrepared != null) ? existingPrepared : newPrepared;
      }
   }

   private static ConcurrentMap<String,PreparedStatement> getPreparedStatementCache() {
      try {
         return ServiceLocator.getNamedInstance(ConcurrentMap.class, "CassandraPreparedStatementCache");
      } catch (Exception ex) {
         return CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .<String,PreparedStatement>build()
            .asMap();
      }
   }
}
