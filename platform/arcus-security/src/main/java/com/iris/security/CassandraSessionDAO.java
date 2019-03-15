/*
 * Used from a Shiro example with changes for IRIS environment
 *
 * Copyright (C) 2013 Les Hazlewood
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.iris.security;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import org.apache.shiro.ShiroException;
import org.apache.shiro.io.DefaultSerializer;
import org.apache.shiro.io.Serializer;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.session.mgt.eis.AbstractSessionDAO;
import org.apache.shiro.util.Destroyable;
import org.apache.shiro.util.Initializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.exceptions.InvalidQueryException;

/**
 * @since 2013-06-09
 *
 * Shiro calls this to store session data.
 * Initialized by shiro.ini
 *
 * Need to refactor so table isn't dynamically created (move to ModelManager)
 *
 * @deprecated This class has been replaced with the GuicedIrisRealm.
 */
@Deprecated
public class CassandraSessionDAO extends AbstractSessionDAO implements Initializable, Destroyable {
   private static final Logger LOG = LoggerFactory.getLogger(CassandraSessionDAO.class);

   private String keyspaceName;
   private String tableName;
   private Cluster cluster; //created during init

   private Serializer<SimpleSession> serializer;

   private com.datastax.driver.core.Session cassandraSession; //acquired during init();

   private PreparedStatement deletePreparedStatement;
   private PreparedStatement savePreparedStatement;
   private PreparedStatement readPreparedStatement;

   public CassandraSessionDAO() {
      setSessionIdGenerator(new TimeUuidSessionIdGenerator());
      this.serializer = new DefaultSerializer<SimpleSession>();
   }

   private SimpleSession assertSimpleSession(Session session) {
      if (!(session instanceof SimpleSession)) {
         throw new IllegalArgumentException(CassandraSessionDAO.class.getName() + " implementations only support " +
                                                  SimpleSession.class.getName() + " instances.");
      }
      return (SimpleSession) session;
   }

   public Cluster getCluster() {
      return cluster;
   }

   public void setCluster(Cluster cluster) {
      this.cluster = cluster;
   }

   public String getKeyspaceName() {
      return keyspaceName;
   }

   public void setKeyspaceName(String keyspaceName) {
      this.keyspaceName = keyspaceName;
   }

   public String getTableName() {
      return tableName;
   }

   public void setTableName(String tableName) {
      this.tableName = tableName;
   }

   @Override
   public void init() throws ShiroException {
      //create the necessary schema if possible:
      com.datastax.driver.core.Session systemSession = cluster.connect();

      try {
         if (!isKeyspacePresent(systemSession)) {
            createKeyspace(systemSession);
            if (!isKeyspacePresent(systemSession)) {
               throw new IllegalStateException("Unable to create keyspace " + keyspaceName);
            }
         }
      } finally {
         systemSession.close();
      }

      cassandraSession = cluster.connect(keyspaceName);
      createTable();

      prepareReadStatement();
      prepareSaveStatement();
      prepareDeleteStatement();
   }

   @Override
   public void destroy() throws Exception {
      if (cassandraSession != null) {
         cassandraSession.close();
      }
   }

   protected boolean isKeyspacePresent(com.datastax.driver.core.Session systemSession) {
      PreparedStatement ps = systemSession.prepare("select * from system.schema_keyspaces where keyspace_name = ?");
      BoundStatement bs = new BoundStatement(ps);
      bs.bind(keyspaceName);
      ResultSet results = systemSession.execute(bs);

      for (Row row : results) {
         if (row.getString("keyspace_name").equals(keyspaceName)) {
            return true;
         }
      }
      return false;
   }

   protected void createKeyspace(com.datastax.driver.core.Session systemSession) {
      //Use NetworkTopologyStrategy in production and probably replication factor of 3
      String query = "create keyspace " + this.keyspaceName + " with replication = {'class': 'SimpleStrategy', 'replication_factor': 1};";
      systemSession.execute(query);
   }

   protected void createTable() {
      try {
         cassandraSession.execute("select count(*) from " + tableName);
      } catch(InvalidQueryException ive) {
         String query =
               "CREATE TABLE " + tableName + " ( " +
                     "    id timeuuid PRIMARY KEY, " +
                     "    start_ts timestamp, " +
                     "    stop_ts timestamp, " +
                     "    last_access_ts timestamp, " +
                     "    timeout bigint, " +
                     "    expired boolean, " +
                     "    host varchar, " +
                     "    serialized_value blob " +
                     ") " +
                     "WITH " +
                     "    gc_grace_seconds = 86400 AND " +
                     "    compaction = {'class':'LeveledCompactionStrategy'};";
         cassandraSession.execute(query);
      }
   }

   @Override
   protected Serializable doCreate(Session session) {
      SimpleSession ss = assertSimpleSession(session);
      Serializable timeUuid = generateSessionId(session);
      assignSessionId(ss, timeUuid);
      save(ss);
      return timeUuid;
   }

   protected UUID toUuid(Serializable sessionId) {
      if (sessionId == null) {
         throw new IllegalArgumentException("sessionId argument cannot be null.");
      }

      UUID id;

      if (sessionId instanceof UUID) {
         id = (UUID)sessionId;
      } else if (sessionId instanceof String) {

         String sid = (String)sessionId;

         if (sid.indexOf('-') < 0) {

            char[] sidChars = sid.toCharArray();

            StringBuilder sb = new StringBuilder();

            for(int i = 0; i < sidChars.length; i++) {
               char c = sidChars[i];
               sb.append(c);
               if (i == 7 || i == 11 || i == 15 || i == 19) {
                  sb.append('-');
               }
            }

            sid = sb.toString();
         }

         try {
            id = UUID.fromString(sid);
         } catch (Exception e) {
            LOG.debug("Unable to parse sessionId string (not a UUID?): {}", sessionId);
            return null;
         }
      } else {
         String msg = "Specified sessionId is of type [" + sessionId.getClass().getName() + "].  Only UUIDs and UUID strings are supported.";
         throw new IllegalArgumentException(msg);
      }

      return id;
   }

   @Override
   protected Session doReadSession(Serializable sessionId) {

      UUID id = toUuid(sessionId);

      PreparedStatement ps = prepareReadStatement();
      BoundStatement bs = new BoundStatement(ps);
      bs.bind(id);

      ResultSet results = cassandraSession.execute(bs);

      for(Row row : results) {
         UUID rowId = row.getUUID("id");
         if (id.equals(rowId)) {
            ByteBuffer buffer = row.getBytes("serialized_value");
            if (buffer != null) { //could be null if a tombstone due to TTL removal
               byte[] bytes = new byte[buffer.remaining()];
               buffer.get(bytes);
               return serializer.deserialize(bytes);
            }
         }
      }

      return null;
   }

   private PreparedStatement prepareReadStatement() {
      if (this.readPreparedStatement == null) {
         String query = "SELECT * from " + tableName + " where id = ?";
         this.readPreparedStatement = cassandraSession.prepare(query);
      }
      return this.readPreparedStatement;
   }

   //In CQL, insert and update are effectively the same, so we can use a single query for both:
   protected void save(SimpleSession ss) {

      //Cassandra TTL values are in seconds, so we need to convert from Shiro's millis:
      int timeoutInSeconds = (int)(ss.getTimeout() / 1000);

      PreparedStatement ps = prepareSaveStatement();
      BoundStatement bs = new BoundStatement(ps);

      byte[] serialized = serializer.serialize(ss);

      ByteBuffer bytes = ByteBuffer.wrap(serialized);

      bs.bind(
            timeoutInSeconds,
            ss.getStartTimestamp(),
            ss.getStopTimestamp() != null ? ss.getStartTimestamp() : null,
            ss.getLastAccessTime(),
            ss.getTimeout(),
            ss.isExpired(),
            ss.getHost(),
            bytes,
            ss.getId()
      );
      // TODO drop this down when we add session caching
      bs.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);

      cassandraSession.execute(bs);
   }

   private PreparedStatement prepareSaveStatement() {
      if (this.savePreparedStatement == null) {
         String query = "UPDATE " + tableName + " USING TTL ? " +
               "SET " +
               "start_ts = ?, " +
               "stop_ts = ?, " +
               "last_access_ts = ?, " +
               "timeout = ?, " +
               "expired = ?, " +
               "host = ?, " +
               "serialized_value = ? " +
               "WHERE " +
               "id = ?";
         this.savePreparedStatement = cassandraSession.prepare(query);
      }
      return this.savePreparedStatement;
   }

   @Override
   public void update(Session session) throws UnknownSessionException {
      SimpleSession ss = assertSimpleSession(session);
      save(ss);
   }

   @Override
   public void delete(Session session) {
      PreparedStatement ps = prepareDeleteStatement();
      BoundStatement bs = new BoundStatement(ps);
      bs.bind(session.getId());
      cassandraSession.execute(bs);
   }

   private PreparedStatement prepareDeleteStatement() {
      if (this.deletePreparedStatement == null) {
         String query = "DELETE from " + tableName + " where id = ?";
         this.deletePreparedStatement = cassandraSession.prepare(query);
      }
      return this.deletePreparedStatement;
   }

   @Override
   public Collection<Session> getActiveSessions() {
      return Collections.emptyList();
   }
}