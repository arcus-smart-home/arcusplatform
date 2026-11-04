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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.PasskeyDAO;
import com.iris.core.dao.metrics.DaoMetrics;
import com.iris.security.passkey.Passkey;

@Singleton
public class PasskeyDAOImpl implements PasskeyDAO {

   private static final String PASSKEY_TABLE = "passkey";
   private static final String PASSKEY_BY_CREDENTIAL_TABLE = "passkey_by_credential";
   private static final String PASSKEY_REQUEST_TABLE = "passkey_request";

   private static final Timer saveTimer = DaoMetrics.upsertTimer(PasskeyDAO.class, "save");
   private static final Timer findByPersonTimer = DaoMetrics.readTimer(PasskeyDAO.class, "findByPerson");
   private static final Timer findByCredentialIdTimer = DaoMetrics.readTimer(PasskeyDAO.class, "findByCredentialId");
   private static final Timer updateUsageTimer = DaoMetrics.upsertTimer(PasskeyDAO.class, "updateUsage");
   private static final Timer deleteTimer = DaoMetrics.deleteTimer(PasskeyDAO.class, "delete");
   private static final Timer deleteForPersonTimer = DaoMetrics.deleteTimer(PasskeyDAO.class, "deleteForPerson");
   private static final Timer saveRequestTimer = DaoMetrics.insertTimer(PasskeyDAO.class, "saveRequest");
   private static final Timer takeRequestTimer = DaoMetrics.readTimer(PasskeyDAO.class, "takeRequest");

   private static class Cols {
      public static final String PERSON_ID = "personId";
      public static final String CREDENTIAL_ID = "credentialId";
      public static final String USER_HANDLE = "userHandle";
      public static final String PUBLIC_KEY = "publicKey";
      public static final String SIGN_COUNT = "signCount";
      public static final String NAME = "name";
      public static final String CREATED = "created";
      public static final String LAST_USED = "lastUsed";
      public static final String REQUEST_ID = "requestId";
      public static final String REQUEST = "request";
   }

   private final CqlSession session;
   // Prepared on first use, not at startup, so services start without the passkey tables
   // while passkeys are off. A prepare that fails is tried again on the next call.
   private final Supplier<Statements> statements;

   // Every write to the passkey table is conditional (a lightweight transaction), so
   // Cassandra orders them itself. A plain write carries the writer's clock instead, and
   // mixed with conditional ones, an earlier write can win: a login's update just before a
   // delete would bring back part of the deleted row. The two tables can't share a
   // conditional batch, so each write is two statements, passkey row first. A lookup row
   // without its passkey row can't log in, and doesn't block registering the credential.

   @Inject
   public PasskeyDAOImpl(CqlSession session) {
      this.session = session;
      this.statements = Suppliers.memoize(() -> new Statements(session));
   }

   private static class Statements {
      private final PreparedStatement insertPasskey;
      private final PreparedStatement insertByCredential;
      private final PreparedStatement findByPerson;
      private final PreparedStatement findCredentialIds;
      private final PreparedStatement findOne;
      private final PreparedStatement findPersonByCredential;
      private final PreparedStatement updateUsage;
      private final PreparedStatement deletePasskey;
      private final PreparedStatement deleteByCredential;
      private final PreparedStatement insertRequest;
      private final PreparedStatement findRequest;
      private final PreparedStatement deleteRequest;

      Statements(CqlSession session) {
         String columns = String.join(", ",
               Cols.PERSON_ID, Cols.CREDENTIAL_ID, Cols.USER_HANDLE, Cols.PUBLIC_KEY,
               Cols.SIGN_COUNT, Cols.NAME, Cols.CREATED, Cols.LAST_USED);
         this.insertPasskey = session.prepare(
               "INSERT INTO " + PASSKEY_TABLE + " (" + columns + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?) IF NOT EXISTS");
         this.insertByCredential = session.prepare(
               "INSERT INTO " + PASSKEY_BY_CREDENTIAL_TABLE + " (" + Cols.CREDENTIAL_ID + ", " + Cols.PERSON_ID + ") VALUES (?, ?)");
         this.findByPerson = session.prepare(
               "SELECT " + columns + " FROM " + PASSKEY_TABLE + " WHERE " + Cols.PERSON_ID + " = ?");
         this.findCredentialIds = session.prepare(
               "SELECT " + Cols.CREDENTIAL_ID + " FROM " + PASSKEY_TABLE + " WHERE " + Cols.PERSON_ID + " = ?");
         this.findOne = session.prepare(
               "SELECT " + columns + " FROM " + PASSKEY_TABLE + " WHERE " + Cols.PERSON_ID + " = ? AND " + Cols.CREDENTIAL_ID + " = ?");
         this.findPersonByCredential = session.prepare(
               "SELECT " + Cols.PERSON_ID + " FROM " + PASSKEY_BY_CREDENTIAL_TABLE + " WHERE " + Cols.CREDENTIAL_ID + " = ?");
         this.updateUsage = session.prepare(
               "UPDATE " + PASSKEY_TABLE + " SET " + Cols.SIGN_COUNT + " = ?, " + Cols.LAST_USED + " = ? WHERE " + Cols.PERSON_ID + " = ? AND " + Cols.CREDENTIAL_ID + " = ? IF EXISTS");
         this.deletePasskey = session.prepare(
               "DELETE FROM " + PASSKEY_TABLE + " WHERE " + Cols.PERSON_ID + " = ? AND " + Cols.CREDENTIAL_ID + " = ? IF EXISTS");
         this.deleteByCredential = session.prepare(
               "DELETE FROM " + PASSKEY_BY_CREDENTIAL_TABLE + " WHERE " + Cols.CREDENTIAL_ID + " = ?");
         this.insertRequest = session.prepare(
               "INSERT INTO " + PASSKEY_REQUEST_TABLE + " (" + Cols.REQUEST_ID + ", " + Cols.PERSON_ID + ", " + Cols.REQUEST + ") VALUES (?, ?, ?) USING TTL ?");
         this.findRequest = session.prepare(
               "SELECT " + Cols.PERSON_ID + ", " + Cols.REQUEST + " FROM " + PASSKEY_REQUEST_TABLE + " WHERE " + Cols.REQUEST_ID + " = ?");
         this.deleteRequest = session.prepare(
               "DELETE FROM " + PASSKEY_REQUEST_TABLE + " WHERE " + Cols.REQUEST_ID + " = ? IF EXISTS");
      }
   }

   @Override
   public boolean save(Passkey passkey) {
      Preconditions.checkNotNull(passkey, "passkey must not be null");
      Preconditions.checkNotNull(passkey.getPersonId(), "personId must not be null");
      Preconditions.checkNotNull(passkey.getCredentialId(), "credentialId must not be null");

      try (Context ctxt = saveTimer.time()) {
         Statements s = statements.get();
         boolean applied = session.execute(s.insertPasskey.bind(
               passkey.getPersonId(),
               passkey.getCredentialId(),
               passkey.getUserHandle(),
               passkey.getPublicKey(),
               passkey.getSignCount(),
               passkey.getName(),
               passkey.getCreated(),
               passkey.getLastUsed())).wasApplied();
         if (!applied) {
            // the person has a row for this credential already, left by a save whose lookup
            // row failed; writing the lookup row now would activate that row's key
            return false;
         }
         session.execute(s.insertByCredential.bind(passkey.getCredentialId(), passkey.getPersonId()));
         return true;
      }
   }

   @Override
   public List<Passkey> findByPerson(UUID personId) {
      Preconditions.checkNotNull(personId, "personId must not be null");
      try (Context ctxt = findByPersonTimer.time()) {
         return session.execute(statements.get().findByPerson.bind(personId)).all().stream()
               .map(this::buildFromRow)
               .collect(Collectors.toList());
      }
   }

   @Override
   @Nullable
   public Passkey findByCredentialId(String credentialId) {
      Preconditions.checkNotNull(credentialId, "credentialId must not be null");
      try (Context ctxt = findByCredentialIdTimer.time()) {
         Row owner = session.execute(statements.get().findPersonByCredential.bind(credentialId)).one();
         if (owner == null) {
            return null;
         }
         Row row = session.execute(statements.get().findOne.bind(owner.getUuid(Cols.PERSON_ID), credentialId)).one();
         return row == null ? null : buildFromRow(row);
      }
   }

   @Override
   public boolean updateUsage(UUID personId, String credentialId, long signCount, Instant lastUsed) {
      Preconditions.checkNotNull(personId, "personId must not be null");
      Preconditions.checkNotNull(credentialId, "credentialId must not be null");
      try (Context ctxt = updateUsageTimer.time()) {
         return session.execute(statements.get().updateUsage.bind(signCount, lastUsed, personId, credentialId)).wasApplied();
      }
   }

   @Override
   public boolean delete(UUID personId, String credentialId) {
      Preconditions.checkNotNull(personId, "personId must not be null");
      Preconditions.checkNotNull(credentialId, "credentialId must not be null");
      try (Context ctxt = deleteTimer.time()) {
         return delete(statements.get(), personId, credentialId);
      }
   }

   @Override
   public void deleteForPerson(UUID personId) {
      Preconditions.checkNotNull(personId, "personId must not be null");
      try (Context ctxt = deleteForPersonTimer.time()) {
         Statements s = statements.get();
         // one row at a time, since a partition delete couldn't be conditional
         for (Row row : session.execute(s.findCredentialIds.bind(personId)).all()) {
            delete(s, personId, row.getString(Cols.CREDENTIAL_ID));
         }
      }
   }

   private boolean delete(Statements s, UUID personId, String credentialId) {
      boolean applied = session.execute(s.deletePasskey.bind(personId, credentialId)).wasApplied();
      session.execute(s.deleteByCredential.bind(credentialId));
      return applied;
   }

   @Override
   public void saveRequest(String requestId, @Nullable UUID personId, String request, int ttlSeconds) {
      Preconditions.checkNotNull(requestId, "requestId must not be null");
      Preconditions.checkNotNull(request, "request must not be null");
      try (Context ctxt = saveRequestTimer.time()) {
         session.execute(statements.get().insertRequest.bind(requestId, personId, request, ttlSeconds));
      }
   }

   @Override
   public Optional<PasskeyRequest> takeRequest(String requestId) {
      Preconditions.checkNotNull(requestId, "requestId must not be null");
      try (Context ctxt = takeRequestTimer.time()) {
         Row row = session.execute(statements.get().findRequest.bind(requestId)).one();
         if (row == null) {
            return Optional.empty();
         }
         // conditional, so a request can only be completed once even if two arrive together
         if (!session.execute(statements.get().deleteRequest.bind(requestId)).wasApplied()) {
            return Optional.empty();
         }
         return Optional.of(new PasskeyRequest(row.getUuid(Cols.PERSON_ID), row.getString(Cols.REQUEST)));
      }
   }

   private Passkey buildFromRow(Row row) {
      Passkey passkey = new Passkey();
      passkey.setPersonId(row.getUuid(Cols.PERSON_ID));
      passkey.setCredentialId(row.getString(Cols.CREDENTIAL_ID));
      passkey.setUserHandle(row.getString(Cols.USER_HANDLE));
      passkey.setPublicKey(row.getString(Cols.PUBLIC_KEY));
      passkey.setSignCount(row.getLong(Cols.SIGN_COUNT));
      passkey.setName(row.getString(Cols.NAME));
      passkey.setCreated(row.getInstant(Cols.CREATED));
      passkey.setLastUsed(row.getInstant(Cols.LAST_USED));
      return passkey;
   }
}
