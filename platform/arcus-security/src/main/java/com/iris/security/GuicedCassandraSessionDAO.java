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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.ShiroException;
import org.apache.shiro.io.DefaultSerializer;
import org.apache.shiro.io.Serializer;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.session.mgt.eis.AbstractSessionDAO;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.apache.shiro.util.Destroyable;
import org.apache.shiro.util.Initializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.gson.GsonFactory;
import com.iris.security.principal.DefaultPrincipalTypeAdapter;
import com.iris.security.principal.PrincipalCollectionTypeAdapter;

/**
 * @since 2013-06-09
 *
 * Shiro calls this to store session data.
 * Initialized by shiro.ini
 *
 * Need to refactor so table isn't dynamically created (move to ModelManager)
 */
@Singleton
public class GuicedCassandraSessionDAO extends AbstractSessionDAO implements Initializable, Destroyable {
   private static final Logger logger = LoggerFactory.getLogger(GuicedCassandraSessionDAO.class);
   private static final NoSessionException NO_SESSION_EXCEPTION = new NoSessionException();
  
   private static final ConsistencyLevel consistencyLevel = ConsistencyLevel.LOCAL_QUORUM;
   
   public static final String TABLE_NAME = "sessions";
   
   public static final class Columns {
   	public static final String ID = "id";
   	public static final String START = "start_ts";
   	public static final String STOP = "stop_ts";
   	public static final String LAST_ACCESS = "last_access_ts";
   	public static final String TIMEOUT = "timeout";
   	public static final String EXPIRED = "expired";
   	public static final String HOST = "host";
   	public static final String SERIALIZED = "serialized_value";
   	public static final String ATTRIBUTES = "attributes";
   	
   	public static List<String> VALUES = ImmutableList.of(ID, START, STOP, LAST_ACCESS, TIMEOUT, EXPIRED, HOST, SERIALIZED, ATTRIBUTES);
   }
   
   private Serializer<SimpleSession> serializer;

   private final com.datastax.driver.core.Session cassandraSession; //acquired during init();
   private final Gson gson;
   
   @Inject(optional=true) @Named("session.cache.timeoutMs")
   private long sessionCacheTimeoutMs = TimeUnit.MINUTES.toMillis(5);

   private Cache<Serializable, Session> sessionCache;
   
   private final PreparedStatement deletePreparedStatement;
   private final PreparedStatement savePreparedStatement;
   private final PreparedStatement readPreparedStatement;

   @Inject
   public GuicedCassandraSessionDAO(com.datastax.driver.core.Session cassandraSession) {
      GsonFactory gsonFactory = new GsonFactory(
            ImmutableSet.of(),
            ImmutableSet.of(),
            ImmutableSet.of(new DefaultPrincipalTypeAdapter(), new PrincipalCollectionTypeAdapter()),
            ImmutableSet.of(new DefaultPrincipalTypeAdapter(), 	new PrincipalCollectionTypeAdapter())
      );
      this.gson = gsonFactory.get();
      this.setSessionIdGenerator(new TimeUuidSessionIdGenerator());
      this.serializer = new DefaultSerializer<SimpleSession>();
      this.cassandraSession = cassandraSession;
      this.sessionCache = 
            CacheBuilder
               .newBuilder()
               .concurrencyLevel(32)
               .expireAfterWrite(sessionCacheTimeoutMs, TimeUnit.MILLISECONDS)
               .build();
      this.readPreparedStatement = prepareReadStatement();
      this.savePreparedStatement = prepareSaveStatement();
      this.deletePreparedStatement = prepareDeleteStatement();
   }

   private SimpleSession assertSimpleSession(Session session) {
   	Preconditions.checkArgument(session instanceof SimpleSession, "%s implementations only support %s instances", GuicedCassandraSessionDAO.class.getName(), SimpleSession.class.getName());
      return (SimpleSession) session;
   }

   private PreparedStatement prepareReadStatement() {
      String query = 
            "SELECT " + StringUtils.join(Columns.VALUES, ',') + " " + 
            "FROM " + TABLE_NAME + " " + 
            "WHERE " + Columns.ID + " = ?";
      return cassandraSession.prepare(query);
   }

   private PreparedStatement prepareSaveStatement() {
      String query = 
            "UPDATE " + TABLE_NAME + " USING TTL ? " +
            "SET " +
            Columns.START + " = ?, " +
            Columns.STOP + " = ?, " +
            Columns.LAST_ACCESS + " = ?, " +
            Columns.TIMEOUT + " = ?, " +
            Columns.EXPIRED + " = ?, " +
            Columns.HOST + " = ?, " +
            Columns.ATTRIBUTES + " = ?, " +
            Columns.SERIALIZED + " = ? " +
            "WHERE " +
            Columns.ID + " = ?";
      return cassandraSession.prepare(query);
   }

   private PreparedStatement prepareDeleteStatement() {
      String query = "DELETE FROM " + TABLE_NAME + " WHERE " + Columns.ID + " = ?";
      return cassandraSession.prepare(query);
   }

   @Override
   public void init() throws ShiroException {
   }

	@Override
   public void destroy() throws Exception {
   }

   @Override
   public void update(Session session) throws UnknownSessionException {
      SimpleSession ss = assertSimpleSession(session);
      save(ss);
   }

   @Override
   public void delete(Session session) {
      BoundStatement bs = new BoundStatement(this.deletePreparedStatement);
      bs.bind(session.getId());
      bs.setConsistencyLevel(consistencyLevel);
      cassandraSession.execute(bs);
      sessionCache.invalidate(session.getId());
   }

   @Override
   public Collection<Session> getActiveSessions() {
      return Collections.emptyList();
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
            logger.debug("Unable to parse sessionId string (not a UUID?): {}", sessionId);
            return null;
         }
      } else {
         String msg = "Specified sessionId is of type [" + sessionId.getClass().getName() + "].  Only UUIDs and UUID strings are supported.";
         logger.warn(msg);
         throw new IllegalArgumentException(msg);
      }

      return id;
   }

   @Override
   protected Session doReadSession(Serializable sessionId) {
      try {
      	Session session = sessionCache.getIfPresent(sessionId);
      	if(session != null) {
	      	if(isExpired(session)) {
	      		sessionCache.asMap().remove(sessionId, session);
	      	}
	      	else {
	      		return session;
	      	}
      	}
      	else {
	         return sessionCache.get(
	               sessionId,
	               () -> doCassandraReadSession(sessionId)
	         );
      	}
      }
      catch(UncheckedExecutionException e) {
         Throwable cause = e.getCause();
         if(cause == NO_SESSION_EXCEPTION || cause instanceof NullPointerException) {
         	logger.warn("Session not found for SessionId: {}", sessionId);
            return null;
         }
         logger.warn("Error loading session {}", sessionId, e);
      }
      catch(Exception e) {
         logger.warn("Error loading session {}", sessionId, e);
      }
      return null;
   }
      
   private Session doCassandraReadSession(Serializable sessionId) {
      UUID id = toUuid(sessionId);
      BoundStatement bs = new BoundStatement(this.readPreparedStatement);
      bs.bind(id);
      bs.setConsistencyLevel(consistencyLevel);

      ResultSet results = cassandraSession.execute(bs);

      for(Row row : results) {
         Session session = hydrateSession(id, row);
         if (session != null && !isExpired(session)) {
         	return session;
         }
      }

      throw NO_SESSION_EXCEPTION;
   }
   
   private boolean isExpired(Session session) {
   	if(session.getLastAccessTime() == null) {
   		return false;
   	}
		return System.currentTimeMillis() > (session.getLastAccessTime().getTime() + session.getTimeout());
	}

	private Session hydrateSession(UUID id, Row row) {
   	UUID rowId = row.getUUID(Columns.ID);
   	if (id.equals(rowId)) {
   		Date start = row.getDate(Columns.START);
   		// If this is null, then the row is a tombstone.
   		if (start != null) {
   			ByteBuffer buffer = row.getBytes(Columns.SERIALIZED);
   			// If the buffer has anything, then it is an old style serialized session.
   			if (buffer != null && buffer.remaining() > 0) {
   				byte[] bytes = new byte[buffer.remaining()];
               buffer.get(bytes);
               return serializer.deserialize(bytes);
   			}
   			else {
   				// New style session. Read the fields and create a session.
   				Date stop = row.getDate(Columns.STOP);
   				Date lastAccess = row.getDate(Columns.LAST_ACCESS);
   				long timeout = row.getLong(Columns.TIMEOUT);
   				boolean expired = row.getBool(Columns.EXPIRED);
   				String host = row.getString(Columns.HOST);
   				
   				// Read the attributes
   				Map<String, String> serialized_attrs = row.getMap(Columns.ATTRIBUTES, String.class, String.class);
   				Map<Object, Object> attributes = new HashMap<>();
   				for (Map.Entry<String, String> entry : serialized_attrs.entrySet()) {
   					String json = entry.getValue();
   					if (json != null && !json.isEmpty()) {
   						attributes.put(entry.getKey(), deserializeAttribute(entry.getKey(), json));
   					}
   				}
   				
   				// Create and populate the session.
   				SimpleSession session = new SimpleSession();
   				session.setId(rowId);
   				session.setStartTimestamp(start);
   				session.setStopTimestamp(stop);
   				session.setLastAccessTime(lastAccess);
   				session.setTimeout(timeout);
   				session.setExpired(expired);
   				session.setHost(host);
   				session.setAttributes(attributes);
   				
   				return session;
   			}
   		}
   	}
   	return null;
   }
   
   private Object deserializeAttribute(String key, String json) {
   	if (key.equals(DefaultSubjectContext.AUTHENTICATED_SESSION_KEY)) {
   		return gson.fromJson(json, Boolean.class);
   	}
   	if (key.equals(DefaultSubjectContext.PRINCIPALS_SESSION_KEY)) {
   		return gson.fromJson(json, SimplePrincipalCollection.class);
   	}
   	logger.error("Unknown key: {}. Unable to deserialize value: {}", key, json);
   	return null;
   }

   //In CQL, insert and update are effectively the same, so we can use a single query for both:
   protected void save(SimpleSession ss) {
   	
      //Cassandra TTL values are in seconds, so we need to convert from Shiro's millis:
      int timeoutInSeconds = (int)(ss.getTimeout() / 1000);

      BoundStatement bs = new BoundStatement(this.savePreparedStatement);
      
      Map<String,String> attributes = new HashMap<>();
      for (Object key : ss.getAttributeKeys()) {
      	if (key instanceof String) {
      		Object value = ss.getAttribute(key);
      		if (value instanceof Serializable) {
      			attributes.put((String)key, gson.toJson(value));
      		}
      		else {
      			logger.error("Could not store un-serializable attribute in Session. Session {}: Key{}", ss.getId(), key);
      		}
      	}
      	else {
      		logger.error("Session attributes with non-string keys are not supported. Session {}: Key {}", ss.getId(), key);
      	}
      }
          
      // FIXME if isExpired() == true we should just delete the row...
      bs.bind(
            timeoutInSeconds,
            ss.getStartTimestamp(),
            ss.getStopTimestamp(),
            ss.getLastAccessTime(),
            ss.getTimeout(),
            ss.isExpired(),
            ss.getHost(),
            attributes,
            null,
            ss.getId()
      );
      bs.setConsistencyLevel(consistencyLevel);
      
      cassandraSession.execute(bs);      
      sessionCache.put(ss.getId(), ss);
   }

   private static final class NoSessionException extends RuntimeException {
      private static final long serialVersionUID = 1581080973099175462L;
   }
}
