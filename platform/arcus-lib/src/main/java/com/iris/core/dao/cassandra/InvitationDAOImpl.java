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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select.Selection;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.core.dao.InvitationDAO;
import com.iris.core.dao.metrics.DaoMetrics;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.type.Invitation;
import com.iris.util.TokenUtil;

@Singleton
public class InvitationDAOImpl implements InvitationDAO {

   private static final String TABLE = "invitation";
   private static final String TABLE_PLACE_IDX = "invitation_place_idx";
   private static final String TABLE_PERSON_IDX = "invitation_person_idx";

   private static final long TTL = TimeUnit.SECONDS.convert(7, TimeUnit.DAYS);

   private static final Timer insertTimer = DaoMetrics.insertTimer(InvitationDAO.class, "insert");
   private static final Timer acceptTimer = DaoMetrics.updateTimer(InvitationDAO.class, "accept");
   private static final Timer acceptWithInviteeTimer = DaoMetrics.updateTimer(InvitationDAO.class, "acceptwithinvitee");
   private static final Timer rejectTimer = DaoMetrics.updateTimer(InvitationDAO.class, "reject");
   private static final Timer findTimer = DaoMetrics.readTimer(InvitationDAO.class, "find");
   private static final Timer pendingForInviteeTimer = DaoMetrics.readTimer(InvitationDAO.class, "pendingForInvitee");
   private static final Timer listForPlaceTimer = DaoMetrics.readTimer(InvitationDAO.class, "listForPlace");
   private static final Timer cancelTimer = DaoMetrics.deleteTimer(InvitationDAO.class, "cancel");
   private static final Counter invitationCodeConflictCounter = DaoMetrics.counter(InvitationDAO.class, "code.conflict");

   private enum Column {
      code, placeId, placeName, streetAddress1, streetAddress2, city, stateProv,
      zipCode, inviteeId, inviteeEmail, inviteeFirstName, inviteeLastName, invitorId,
      invitorFirstName, invitorLastName, placeOwnerId, placeOwnerFirstName,
      placeOwnerLastName, created, accepted, rejected, rejectReason, relationship, invitationText,
      personalizedGreeting;


      private static String[] names() {
         String[] names = new String[Column.values().length];
         Column[] columns = Column.values();
         for(int i = 0; i < columns.length; i++) {
            names[i] = columns[i].name();
         }
         return names;
      }
   }

   @Named("invitation.dao.token.length")
   @Inject(optional = true)
   private int tokenLength = 6;

   @Named("invitation.dao.token.conflict.retries")
   @Inject(optional = true)
   private int tokenRetries = 2;

   private final Session session;
   private final PreparedStatement insert;
   private final PreparedStatement insertPlaceIdx;
   private final PreparedStatement insertPersonIdx;
   private final PreparedStatement acceptExistingPerson;
   private final PreparedStatement accept;
   private final PreparedStatement reject;
   private final PreparedStatement select;
   private final PreparedStatement selectCodesForPerson;
   private final PreparedStatement selectCodesForPlace;
   private final PreparedStatement delete;
   private final PreparedStatement deletePersonIdx;
   private final PreparedStatement deletePlaceIdx;

   @Inject
   public InvitationDAOImpl(Session session) {
      this.session = session;
      insert = prepareInsert(session);
      insertPlaceIdx = prepareInsertPlaceIdx(session);
      insertPersonIdx = prepareInsertPersonIdx(session);
      acceptExistingPerson = prepareAcceptExistingPerson(session);
      accept = prepareAccept(session);
      reject = prepareReject(session);
      select = prepareSelect(session);
      selectCodesForPerson = prepareSelectCodesForPerson(session);
      selectCodesForPlace = prepareSelectCodesForPlace(session);
      delete = prepareDelete(session);
      deletePersonIdx = prepareDeletePersonIdx(session);
      deletePlaceIdx = prepareDeletePlaceIdx(session);
   }

   private PreparedStatement prepareInsert(Session session) {
      return CassandraQueryBuilder.insert(TABLE)
            .addColumns(Column.names())
            .withTtlSec(TTL)
            .ifNotExists()
            .prepare(session);
   }

   private PreparedStatement prepareInsertPlaceIdx(Session session) {
      return CassandraQueryBuilder.insert(TABLE_PLACE_IDX)
            .addColumns(Column.placeId.name(), Column.code.name())
            .withTtlSec(TTL)
            .prepare(session);
   }

   private PreparedStatement prepareInsertPersonIdx(Session session) {
      return CassandraQueryBuilder.insert(TABLE_PERSON_IDX)
            .addColumns(Column.inviteeId.name(), Column.code.name())
            .withTtlSec(TTL)
            .prepare(session);
   }

   private PreparedStatement prepareAcceptExistingPerson(Session session) {
      return CassandraQueryBuilder.update(TABLE)
            .addColumn(Column.accepted.name())
            .addWhereColumnEquals(Column.code.name())
            .prepare(session);
   }

   private PreparedStatement prepareAccept(Session session) {
      return CassandraQueryBuilder.update(TABLE)
            .addColumns(Column.accepted.name(), Column.inviteeId.name())
            .addWhereColumnEquals(Column.code.name())
            .prepare(session);
   }

   private PreparedStatement prepareReject(Session session) {
      return CassandraQueryBuilder.update(TABLE)
            .addColumns(Column.rejected.name(), Column.rejectReason.name())
            .addWhereColumnEquals(Column.code.name())
            .prepare(session);
   }

   private PreparedStatement prepareSelect(Session session) {
      return CassandraQueryBuilder.select(TABLE)
            .addColumns(Column.names())
            .addWhereColumnEquals(Column.code.name())
            .prepare(session);
   }

   private PreparedStatement prepareSelectCodesForPerson(Session session) {
      return CassandraQueryBuilder.select(TABLE_PERSON_IDX)
            .addColumn(Column.code.name())
            .addWhereColumnEquals(Column.inviteeId.name())
            .prepare(session);
   }

   private PreparedStatement prepareSelectCodesForPlace(Session session) {
      return CassandraQueryBuilder.select(TABLE_PLACE_IDX)
            .addColumn(Column.code.name())
            .addWhereColumnEquals(Column.placeId.name())
            .prepare(session);
   }

   private PreparedStatement prepareDelete(Session session) {
      return CassandraQueryBuilder.delete(TABLE)
            .addWhereColumnEquals(Column.code.name())
            .prepare(session);
   }

   private PreparedStatement prepareDeletePersonIdx(Session session) {
      return CassandraQueryBuilder.delete(TABLE_PERSON_IDX)
            .addWhereColumnEquals(Column.code.name())
            .addWhereColumnEquals(Column.inviteeId.name())
            .prepare(session);
   }

   private PreparedStatement prepareDeletePlaceIdx(Session session) {
      return CassandraQueryBuilder.delete(TABLE_PLACE_IDX)
            .addWhereColumnEquals(Column.code.name())
            .addWhereColumnEquals(Column.placeId.name())
            .prepare(session);
   }

   @Override
   public Invitation insert(Invitation invitation) {
      Preconditions.checkNotNull(invitation, "invitation is required");

      try(Context timer = insertTimer.time()) {
         for(int i = 0; i < tokenRetries; i++) {
            String token = generateToken();
            Date created = new Date();
            BoundStatement stmt = new BoundStatement(insert);
            stmt.setString(Column.code.name(), token);
            stmt.setUUID(Column.placeId.name(), UUID.fromString(invitation.getPlaceId()));
            stmt.setString(Column.placeName.name(), invitation.getPlaceName());
            stmt.setString(Column.streetAddress1.name(), invitation.getStreetAddress1());
            stmt.setString(Column.streetAddress2.name(), invitation.getStreetAddress2());
            stmt.setString(Column.city.name(), invitation.getCity());
            stmt.setString(Column.stateProv.name(), invitation.getStateProv());
            stmt.setString(Column.zipCode.name(), invitation.getZipCode());
            stmt.setUUID(Column.inviteeId.name(), invitation.getInviteeId() == null ? null : UUID.fromString(invitation.getInviteeId()));
            stmt.setString(Column.inviteeEmail.name(), invitation.getInviteeEmail().toLowerCase());
            stmt.setString(Column.inviteeFirstName.name(), invitation.getInviteeFirstName());
            stmt.setString(Column.inviteeLastName.name(), invitation.getInviteeLastName());
            stmt.setUUID(Column.invitorId.name(), UUID.fromString(invitation.getInvitorId()));
            stmt.setString(Column.invitorFirstName.name(), invitation.getInvitorFirstName());
            stmt.setString(Column.invitorLastName.name(), invitation.getInvitorLastName());
            stmt.setUUID(Column.placeOwnerId.name(), UUID.fromString(invitation.getPlaceOwnerId()));
            stmt.setString(Column.placeOwnerFirstName.name(), invitation.getPlaceOwnerFirstName());
            stmt.setString(Column.placeOwnerLastName.name(), invitation.getPlaceOwnerLastName());
            stmt.setTimestamp(Column.created.name(), created);
            stmt.setTimestamp(Column.accepted.name(), null);
            stmt.setTimestamp(Column.rejected.name(), null);
            stmt.setString(Column.rejectReason.name(), invitation.getRejectReason());
            stmt.setString(Column.relationship.name(), invitation.getRelationship());
            stmt.setString(Column.invitationText.name(), invitation.getInvitationText());
            stmt.setString(Column.personalizedGreeting.name(), invitation.getPersonalizedGreeting());
            ResultSet rs = session.execute(stmt);
            if(rs.wasApplied()) {
               insertIndexes(invitation.getPlaceId(), invitation.getInviteeId(), token);
               invitation.setCode(token);
               invitation.setCreated(created);
               return invitation;
            }
         }
         invitationCodeConflictCounter.inc();
         throw new ErrorEventException(Errors.CODE_GENERIC, "unique token could not be found after " + tokenRetries + " attempts");
      }
   }

   private void insertIndexes(String placeId, String inviteeId, String token) {
      BatchStatement batch = new BatchStatement();
      BoundStatement placeIdx = new BoundStatement(insertPlaceIdx);
      placeIdx.setUUID(Column.placeId.name(), UUID.fromString(placeId));
      placeIdx.setString(Column.code.name(), token);
      batch.add(placeIdx);
      if(inviteeId != null) {
         batch.add(bindInsertPersonIdx(UUID.fromString(inviteeId), token));
      }
      session.execute(batch);
   }

   private Statement bindInsertPersonIdx(UUID inviteeId, String token) {
      BoundStatement personIdx = new BoundStatement(insertPersonIdx);
      personIdx.setUUID(Column.inviteeId.name(), inviteeId);
      personIdx.setString(Column.code.name(), token);
      return personIdx;
   }

   @Override
   public void accept(String code) {
      Preconditions.checkNotNull(code, "code is required");

      try(Context timer = acceptTimer.time()) {
         BoundStatement stmt = new BoundStatement(acceptExistingPerson);
         stmt.setString(Column.code.name(), StringUtils.lowerCase(code));
         stmt.setTimestamp(Column.accepted.name(), new Date());
         session.execute(stmt);
      }
   }

   @Override
   public void accept(String code, UUID inviteeId) {
      Preconditions.checkNotNull(code, "code is required");
      Preconditions.checkNotNull(inviteeId, "inviteeId is required");

      try(Context timer = acceptWithInviteeTimer.time()) {
         BatchStatement batch = new BatchStatement();
         BoundStatement stmt = new BoundStatement(accept);
         stmt.setString(Column.code.name(), StringUtils.lowerCase(code));
         stmt.setTimestamp(Column.accepted.name(), new Date());
         stmt.setUUID(Column.inviteeId.name(), inviteeId);
         batch.add(stmt);
         batch.add(bindInsertPersonIdx(inviteeId, code));
         session.execute(batch);
      }
   }

   @Override
   public void reject(String code, String reason) {
      Preconditions.checkNotNull(code, "code is required");

      try(Context timer = rejectTimer.time()) {
         BoundStatement stmt = new BoundStatement(reject);
         stmt.setString(Column.code.name(), StringUtils.lowerCase(code));
         stmt.setTimestamp(Column.rejected.name(), new Date());
         stmt.setString(Column.rejectReason.name(), reason);
         session.execute(stmt);
      }
   }

   @Override
   public Invitation find(String code) {
      Preconditions.checkNotNull(code, "code is required");

      try(Context timer = findTimer.time()) {
         BoundStatement stmt = new BoundStatement(select);
         stmt.setString(Column.code.name(), StringUtils.lowerCase(code));
         return build(session.execute(stmt).one());
      }
   }

   @Override
   public List<Invitation> pendingForInvitee(UUID inviteeId) {
      Preconditions.checkNotNull(inviteeId, "inviteeId is required");

      try(Context timer = pendingForInviteeTimer.time()) {
         BoundStatement stmt = new BoundStatement(selectCodesForPerson);
         stmt.setUUID(Column.inviteeId.name(), inviteeId);
         return listByIndex(stmt, (r) -> { return r.getTimestamp(Column.accepted.name()) == null && r.getTimestamp(Column.rejected.name()) == null; });
      }
   }

   @Override
   public List<Invitation> listForPlace(UUID placeId) {
      Preconditions.checkNotNull(placeId, "placeId is required");

      try(Context timer = listForPlaceTimer.time()) {
         BoundStatement stmt = new BoundStatement(selectCodesForPlace);
         stmt.setUUID(Column.placeId.name(), placeId);
         return listByIndex(stmt, (r) -> { return true; });
      }
   }



   @Override
   public void cancel(Invitation invitation) {
      Preconditions.checkNotNull(invitation, "invitation is required");

      try(Context timer = cancelTimer.time()) {
         BatchStatement stmt = new BatchStatement();
         BoundStatement tblDel = new BoundStatement(delete);
         tblDel.setString(Column.code.name(), invitation.getCode());
         stmt.add(tblDel);
         BoundStatement placeIdxDel = new BoundStatement(deletePlaceIdx);
         placeIdxDel.setString(Column.code.name(), invitation.getCode());
         placeIdxDel.setUUID(Column.placeId.name(), UUID.fromString(invitation.getPlaceId()));
         stmt.add(placeIdxDel);
         if(invitation.getInviteeId() != null) {
            BoundStatement personIdxDel = new BoundStatement(deletePersonIdx);
            personIdxDel.setString(Column.code.name(), invitation.getCode());
            personIdxDel.setUUID(Column.inviteeId.name(), UUID.fromString(invitation.getInviteeId()));
            stmt.add(personIdxDel);
         }
         session.execute(stmt);
      }
   }

   private List<Invitation> listByIndex(Statement idxStmt, Predicate<Row> filter) {
      final List<String> codes = new ArrayList<>();
      session.execute(idxStmt).forEach((r) -> { codes.add(r.getString(Column.code.name())); });
      if(codes.isEmpty()) {
         return Collections.emptyList();
      }
      Selection sel = QueryBuilder.select();
      for(Column c : Column.values()) {
         sel.column(c.name());
      }
      Statement getInvites = sel
            .from(TABLE)
            .where(QueryBuilder.in(Column.code.name(), codes.toArray()))
            .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
      List<Invitation> invitations = new ArrayList<>(codes.size());
      session.execute(getInvites).forEach((r) -> { if(filter.test(r)) { invitations.add(build(r)); } });
      return invitations;
   }

   private Invitation build(Row r) {
      if(r == null) {
         return null;
      }
      Invitation invitation = new Invitation();
      invitation.setAccepted(r.getTimestamp(Column.accepted.name()));
      invitation.setCity(r.getString(Column.city.name()));
      invitation.setCode(r.getString(Column.code.name()));
      invitation.setCreated(r.getTimestamp(Column.created.name()));
      invitation.setInvitationText(r.getString(Column.invitationText.name()));
      invitation.setInviteeEmail(r.getString(Column.inviteeEmail.name()));
      invitation.setInviteeFirstName(r.getString(Column.inviteeFirstName.name()));

      UUID inviteeId = r.getUUID(Column.inviteeId.name());
      invitation.setInviteeId(inviteeId == null ? null : inviteeId.toString());

      invitation.setInviteeLastName(r.getString(Column.inviteeLastName.name()));
      invitation.setInvitorFirstName(r.getString(Column.invitorFirstName.name()));
      invitation.setInvitorId(r.getUUID(Column.invitorId.name()).toString());
      invitation.setInvitorLastName(r.getString(Column.invitorLastName.name()));
      invitation.setPersonalizedGreeting(r.getString(Column.personalizedGreeting.name()));
      invitation.setPlaceId(r.getUUID(Column.placeId.name()).toString());
      invitation.setPlaceName(r.getString(Column.placeName.name()));
      invitation.setPlaceOwnerFirstName(r.getString(Column.placeOwnerFirstName.name()));
      invitation.setPlaceOwnerId(r.getUUID(Column.placeOwnerId.name()).toString());
      invitation.setPlaceOwnerLastName(r.getString(Column.placeOwnerLastName.name()));
      invitation.setRejected(r.getTimestamp(Column.rejected.name()));
      invitation.setRejectReason(r.getString(Column.rejectReason.name()));
      invitation.setRelationship(r.getString(Column.relationship.name()));
      invitation.setStateProv(r.getString(Column.stateProv.name()));
      invitation.setStreetAddress1(r.getString(Column.streetAddress1.name()));
      invitation.setStreetAddress2(r.getString(Column.streetAddress2.name()));
      invitation.setZipCode(r.getString(Column.zipCode.name()));

      return invitation;
   }

   private String generateToken() {
      return TokenUtil.randomTokenString(tokenLength);
   }

}

