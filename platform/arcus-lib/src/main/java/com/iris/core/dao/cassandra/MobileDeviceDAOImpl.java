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

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchStatementBuilder;
import com.datastax.oss.driver.api.core.cql.BatchableStatement;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.DefaultBatchType;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.MobileDeviceDAO;
import com.iris.core.dao.metrics.DaoMetrics;
import com.iris.core.dao.metrics.TimingIterator;
import com.iris.core.dao.support.MobileDeviceSaveResult;
import com.iris.messages.model.MobileDevice;
import com.iris.messages.model.Person;

@Singleton
public class MobileDeviceDAOImpl implements MobileDeviceDAO {

   private static final Timer upsertTimer = DaoMetrics.upsertTimer(MobileDeviceDAO.class, "save");
   private static final Timer deleteTimer = DaoMetrics.deleteTimer(MobileDeviceDAO.class, "delete");
   private static final Timer findOneTimer = DaoMetrics.readTimer(MobileDeviceDAO.class, "findOne");
   private static final Timer listForPersonTimer = DaoMetrics.readTimer(MobileDeviceDAO.class, "listForPerson");
   private static final Timer findWithTokenTimer = DaoMetrics.readTimer(MobileDeviceDAO.class, "findWithToken");
   private static final Timer streamAllTimer = DaoMetrics.readTimer(MobileDeviceDAO.class, "streamAll");

   private final CqlSession session;

   private final PreparedStatement personCurrentMobileQuery;
   private final PreparedStatement personUpdateCurrentMobile;
   private final PreparedStatement initialPersonUpdateCurrentMobile;
   private final PreparedStatement upsertStatement;
   private final PreparedStatement deleteStatement;
   private final PreparedStatement findOneQuery;
   private final PreparedStatement listForPersonQuery;
   private final PreparedStatement insertTokenIndex;
   private final PreparedStatement deleteTokenIndex;
   private final PreparedStatement getPersonAndIdForToken;
   private final PreparedStatement optimisticInsertTokenIndex;
   private final PreparedStatement optimisticUpdateTokenIndex;
   private final PreparedStatement streamAll;

   @Inject
   public MobileDeviceDAOImpl(CqlSession session) {
      this.session = session;
      personCurrentMobileQuery = prepareCurrentMobileCountStatement();
      personUpdateCurrentMobile = prepareUpdateCurrentMobileStatement();
      initialPersonUpdateCurrentMobile = prepareInsertInitialUpdateCurrentStatement();
      upsertStatement = prepareUpsertStatement();
      deleteStatement = prepareDeleteStatement();
      findOneQuery = prepareFindOneStatement();
      listForPersonQuery = prepareListForPersonStatement();
      insertTokenIndex = prepareInsertTokenIndexStatement();
      deleteTokenIndex = prepareDeleteTokenIndexStatement();
      getPersonAndIdForToken = prepareGetPersonAndIdForTokenStatement();
      optimisticInsertTokenIndex = prepareOptimisticTokenIndexInsertStatement();
      optimisticUpdateTokenIndex = prepareOptimisticTokenIndexUpdateStatement();
      streamAll = prepareStreamAll();
   }

   @Override
   public MobileDeviceSaveResult save(MobileDevice device) {
      if (device == null){
         return null;
      }

      Preconditions.checkNotNull(device.getPersonId(), "A mobile device must have a person identifier set");

      // always update and return the copy
      MobileDevice deviceCopy = device.copy();

      try(Context ctxt = upsertTimer.time()){
         if (device.getDeviceIndex() == 0){
            return doInsert(deviceCopy);
         }else{
            return doUpdate(deviceCopy);
         }
      }
   }

   private MobileDeviceSaveResult doInsert(MobileDevice device) {
      int nextMobileId = incCount(device.getPersonId());
      device.setDeviceIndex(nextMobileId);
      device.setAssociated(new Date());

      MobileDeviceSaveResult saveResult = new MobileDeviceSaveResult(device);

      if (StringUtils.isBlank(device.getNotificationToken()) || tryTokenIndexInsertForDevice(device)){
         executeUpsert(device);
      }else{
         // do a reverse lookup to find the mobile device to delete
         MobileDevice toDelete = findWithToken(device.getNotificationToken());

         // Remove Old Owner Mobile Device and NotificationToken
         if (toDelete != null){
            // delete old md
            session.execute(deleteStatement.bind(toDelete.getPersonId(), toDelete.getDeviceIndex()));
            // delete old token in side table
            executeTokenIndexDelete(toDelete);
            saveResult.setOwnerChangedForId(toDelete.getPersonId());
         }

         // insert new rows
         executeUpsert(device);
         executeTokenIndexInsert(device);
      }

      return saveResult;
   }

   private MobileDeviceSaveResult doUpdate(MobileDevice device) {
      MobileDevice current = findOne(device.getPersonId(), device.getDeviceIndex());
      if (ObjectUtils.notEqual(current.getLastLatitude(), device.getLastLatitude()) ||
            ObjectUtils.notEqual(current.getLastLongitude(), device.getLastLongitude())){
         device.setLastLocationTime(new Date());
      }

      BatchStatementBuilder batch = BatchStatement.builder(DefaultBatchType.LOGGED);
      addUpsertToBatch(batch, device);

      // only update the token if it exists already
      if (!tryTokenIndexUpdateForDevice(device)){
         /*
          * if the tryTokenIndexUpdateForDevice failed the new key didn't exist
          * already or device.getNotificationToken() was blank or null
          */
         addTokenIndexDeleteToBatch(batch, current); // delete the old row
         addTokenIndexInsertToBatch(batch, device); // insert the new row
      }

      session.execute(batch.build());
      MobileDeviceSaveResult saveResult = new MobileDeviceSaveResult(device);
      return saveResult;
   }

   private void addUpsertToBatch(BatchStatementBuilder batch, MobileDevice device) {
      batch.addStatement(mobileDeviceUpsert(device));
   }

   private void executeUpsert(MobileDevice device) {
      session.execute(mobileDeviceUpsert(device));
   }

   private BoundStatement mobileDeviceUpsert(MobileDevice device) {
      return upsertStatement.bind()
      .setUuid("personId", device.getPersonId())
      .setInt("deviceIndex", device.getDeviceIndex())
      .setInstant("associated", device.getAssociated() == null ? null : device.getAssociated().toInstant())
      .setString("osType", device.getOsType())
      .setString("osVersion", device.getOsVersion())
      .setString("formFactor", device.getFormFactor())
      .setString("phoneNumber", device.getPhoneNumber())
      .setString("deviceIdentifier", device.getDeviceIdentifier())
      .setString("deviceModel", device.getDeviceModel())
      .setString("deviceVendor", device.getDeviceVendor())
      .setString("resolution", device.getResolution())
      .setString("notificationToken", device.getNotificationToken())
      .setDouble("lastLatitude", device.getLastLatitude())
      .setDouble("lastLongitude", device.getLastLongitude())
      .setInstant("lastLocationTime", device.getLastLocationTime() == null ? null : device.getLastLocationTime().toInstant())
      .setString("name", device.getName())
      .setString("appVersion", device.getAppVersion());
   }

   private void addTokenIndexDeleteToBatch(BatchStatementBuilder batch, MobileDevice device) {
      if (!StringUtils.isBlank(device.getNotificationToken())){
         batch.addStatement(deleteTokenIndex.bind(device.getNotificationToken()));
      }
   }

   private void addTokenIndexInsertToBatch(BatchStatementBuilder batch, MobileDevice device) {
      if (!StringUtils.isBlank(device.getNotificationToken())){
         batch.addStatement(insertTokenIndex.bind(device.getNotificationToken(), device.getPersonId(), device.getDeviceIndex()));
      }
   }

   private boolean tryTokenIndexUpdateForDevice(MobileDevice device) {
      if (!StringUtils.isBlank(device.getNotificationToken())){
         ResultSet rs = session.execute(optimisticUpdateTokenIndex.bind(device.getPersonId(), device.getDeviceIndex(), device.getNotificationToken()));
         return rs.wasApplied();
      }

      return false; // isblank or failure
   }

   private boolean tryTokenIndexInsertForDevice(MobileDevice device) {
      if (!StringUtils.isBlank(device.getNotificationToken())){
         ResultSet rs = session.execute(optimisticInsertTokenIndex.bind(device.getNotificationToken(), device.getPersonId(), device.getDeviceIndex()));
         return rs.wasApplied();
      }

      return false; // isblank or failure
   }

   private void executeTokenIndexDelete(MobileDevice device) {
      if (!StringUtils.isBlank(device.getNotificationToken())){
         session.execute(deleteTokenIndex.bind(device.getNotificationToken()));
      }
   }

   private void executeTokenIndexInsert(MobileDevice device) {
      if (!StringUtils.isBlank(device.getNotificationToken())){
         session.execute(insertTokenIndex.bind(device.getNotificationToken(), device.getPersonId(), device.getDeviceIndex()));
      }
   }

   private int incCount(UUID personId) {
      int currentId = session.execute(personCurrentMobileQuery.bind(personId)).one().getInt("mobileDeviceSequence");
      int nextId = currentId + 1;
      ResultSet rs = currentId == 0 ? session.execute(initialPersonUpdateCurrentMobile.bind(nextId, personId)) : session.execute(personUpdateCurrentMobile.bind(nextId,
            personId, currentId));
      // TODO: should we try multiple times?
      if (!rs.wasApplied()){
         throw new IllegalStateException("Failed to retrieve new identifier for mobile device.");
      }
      return nextId;
   }

   @Override
   public void delete(MobileDevice device) {
      if (device != null){
         try(Context ctxt = deleteTimer.time()){
            BatchStatementBuilder batch = BatchStatement.builder(DefaultBatchType.LOGGED);
            batch.addStatement(deleteStatement.bind(device.getPersonId(), device.getDeviceIndex()));
            addTokenIndexDeleteToBatch(batch, device);
            session.execute(batch.build());
         }
      }
   }

   @Override
   public MobileDevice findOne(UUID personId, int instance) {
      Preconditions.checkNotNull(personId, "The person ID cannot be null");

      try(Context ctxt = findOneTimer.time()) {
         Row row = session.execute(findOneQuery.bind(personId, instance)).one();
         return row == null ? null : createMobileDevice(row);
      }
   }

   private MobileDevice createMobileDevice(Row row) {
      MobileDevice device = new MobileDevice();
      device.setAssociated(row.isNull("associated") ? null : Date.from(row.getInstant("associated")));
      device.setDeviceIdentifier(row.getString("deviceIdentifier"));
      device.setDeviceIndex(row.getInt("deviceIndex"));
      device.setDeviceModel(row.getString("deviceModel"));
      device.setDeviceVendor(row.getString("deviceVendor"));
      device.setFormFactor(row.getString("formFactor"));
      device.setLastLatitude(row.getDouble("lastLatitude"));
      device.setLastLocationTime(row.isNull("lastLocationTime") ? null : Date.from(row.getInstant("lastLocationTime")));
      device.setLastLongitude(row.getDouble("lastLongitude"));
      device.setNotificationToken(row.getString("notificationToken"));
      device.setOsType(row.getString("osType"));
      device.setOsVersion(row.getString("osVersion"));
      device.setPersonId(row.getUuid("personId"));
      device.setPhoneNumber(row.getString("phoneNumber"));
      device.setResolution(row.getString("resolution"));
      device.setName(row.getString("name"));
      device.setAppVersion(row.getString("appVersion"));
      return device;
   }

   @Override
   public List<MobileDevice> listForPerson(Person person) {
      Preconditions.checkNotNull(person, "Person cannot be null");
      return listForPerson(person.getId());
   }

   @Override
   public List<MobileDevice> listForPerson(UUID personId) {
      Preconditions.checkNotNull(personId, "Person must be saved and have an identifier");

      try(Context ctxt = listForPersonTimer.time()){
         List<Row> rows = session.execute(listForPersonQuery.bind(personId)).all();
         return rows.stream().map((r) -> {
            return createMobileDevice(r);
         }).collect(Collectors.toList());
      }
   }

   @Override
   public MobileDevice findWithToken(String token) {
      if (StringUtils.isBlank(token)){
         return null;
      }

      try(Context ctxt = findWithTokenTimer.time()){
         Row row = session.execute(getPersonAndIdForToken.bind(token)).one();
         if (row == null){
            return null;
         }
         UUID personId = row.getUuid("personId");
         int deviceIndex = row.getInt("deviceIndex");
         return findOne(personId, deviceIndex);
      }
   }

   private PreparedStatement prepareCurrentMobileCountStatement() {
      return CassandraQueryBuilder.select(Tables.PERSON)
            .addColumn(Tables.PersonCols.MOBILE_DEVICE_SEQUENCE)
            .addWhereColumnEquals(Tables.PersonCols.ID)
            .prepare(session);
   }

   private PreparedStatement prepareUpdateCurrentMobileStatement() {
      return CassandraQueryBuilder.update(Tables.PERSON)
            .addColumn(Tables.PersonCols.MOBILE_DEVICE_SEQUENCE)
            .addWhereColumnEquals(Tables.PersonCols.ID)
            .ifClause(Tables.PersonCols.MOBILE_DEVICE_SEQUENCE + " = ?")
            .prepare(session);
   }

   private PreparedStatement prepareInsertInitialUpdateCurrentStatement() {
      return CassandraQueryBuilder.update(Tables.PERSON)
            .addColumn(Tables.PersonCols.MOBILE_DEVICE_SEQUENCE)
            .addWhereColumnEquals(Tables.PersonCols.ID)
            .prepare(session);
   }

   private PreparedStatement prepareUpsertStatement() {
      return CassandraQueryBuilder.insert(Tables.MOBILE_DEVICES)
            .addColumn(Tables.MobileDevicesCols.PERSON_ID)
            .addColumn(Tables.MobileDevicesCols.DEVICE_INDEX)
            .addColumn(Tables.MobileDevicesCols.ASSOCIATED)
            .addColumn(Tables.MobileDevicesCols.OS_TYPE)
            .addColumn(Tables.MobileDevicesCols.OS_VERSION)
            .addColumn(Tables.MobileDevicesCols.FORM_FACTOR)
            .addColumn(Tables.MobileDevicesCols.PHONE_NUMBER)
            .addColumn(Tables.MobileDevicesCols.DEVICE_IDENTIFIER)
            .addColumn(Tables.MobileDevicesCols.DEVICE_MODEL)
            .addColumn(Tables.MobileDevicesCols.DEVICE_VENDOR)
            .addColumn(Tables.MobileDevicesCols.RESOLUTION)
            .addColumn(Tables.MobileDevicesCols.NOTIFICATION_TOKEN)
            .addColumn(Tables.MobileDevicesCols.LAST_LATITUDE)
            .addColumn(Tables.MobileDevicesCols.LAST_LONGITUDE)
            .addColumn(Tables.MobileDevicesCols.LAST_LOCATION_TIME)
            .addColumn(Tables.MobileDevicesCols.NAME)
            .addColumn(Tables.MobileDevicesCols.APP_VERSION)
            .prepare(session);
   }

   private PreparedStatement prepareDeleteStatement() {
      return CassandraQueryBuilder.delete(Tables.MOBILE_DEVICES)
            .addWhereColumnEquals(Tables.MobileDevicesCols.PERSON_ID)
            .addWhereColumnEquals(Tables.MobileDevicesCols.DEVICE_INDEX)
            .prepare(session);
   }

   private PreparedStatement prepareFindOneStatement() {
      CassandraQueryBuilder queryBuilder = CassandraQueryBuilder.select(Tables.MOBILE_DEVICES)
            .addWhereColumnEquals(Tables.MobileDevicesCols.PERSON_ID)
            .addWhereColumnEquals(Tables.MobileDevicesCols.DEVICE_INDEX);
      return addAllColumns(queryBuilder).prepare(session);
   }

   private PreparedStatement prepareListForPersonStatement() {
      CassandraQueryBuilder queryBuilder = CassandraQueryBuilder.select(Tables.MOBILE_DEVICES)
            .addWhereColumnEquals(Tables.MobileDevicesCols.PERSON_ID);
      return addAllColumns(queryBuilder).prepare(session);
   }

   private PreparedStatement prepareInsertTokenIndexStatement() {
      return CassandraQueryBuilder.insert(Tables.NOTIFICATION_TOKEN_MOBILE_DEVICE)
            .addColumn(Tables.NotificationTokenMobileDeviceCols.NOTIFICATION_TOKEN)
            .addColumn(Tables.NotificationTokenMobileDeviceCols.PERSON_ID)
            .addColumn(Tables.NotificationTokenMobileDeviceCols.DEVICE_INDEX)
            .prepare(session);
   }

   private PreparedStatement prepareDeleteTokenIndexStatement() {
      return CassandraQueryBuilder.delete(Tables.NOTIFICATION_TOKEN_MOBILE_DEVICE)
            .addWhereColumnEquals(Tables.NotificationTokenMobileDeviceCols.NOTIFICATION_TOKEN)
            .prepare(session);
   }

   private PreparedStatement prepareGetPersonAndIdForTokenStatement() {
      return CassandraQueryBuilder.select(Tables.NOTIFICATION_TOKEN_MOBILE_DEVICE)
            .addColumns(
                  Tables.NotificationTokenMobileDeviceCols.DEVICE_INDEX,
                  Tables.NotificationTokenMobileDeviceCols.NOTIFICATION_TOKEN,
                  Tables.NotificationTokenMobileDeviceCols.PERSON_ID
             )
            .addWhereColumnEquals(Tables.NotificationTokenMobileDeviceCols.NOTIFICATION_TOKEN)
            .prepare(session);
   }

   // optimistic insert if not exists
   private PreparedStatement prepareOptimisticTokenIndexInsertStatement() {
      return CassandraQueryBuilder.insert(Tables.NOTIFICATION_TOKEN_MOBILE_DEVICE)
            .addColumn(Tables.NotificationTokenMobileDeviceCols.NOTIFICATION_TOKEN)
            .addColumn(Tables.NotificationTokenMobileDeviceCols.PERSON_ID)
            .addColumn(Tables.NotificationTokenMobileDeviceCols.DEVICE_INDEX)
            .ifNotExists()
            .prepare(session);
   }

   private PreparedStatement prepareOptimisticTokenIndexUpdateStatement() {
      return CassandraQueryBuilder.update(Tables.NOTIFICATION_TOKEN_MOBILE_DEVICE)
            .addColumn(Tables.NotificationTokenMobileDeviceCols.PERSON_ID)
            .addColumn(Tables.NotificationTokenMobileDeviceCols.DEVICE_INDEX)
            .addWhereColumnEquals(Tables.NotificationTokenMobileDeviceCols.NOTIFICATION_TOKEN)
            .ifExists()
            .prepare(session);
   }

   private CassandraQueryBuilder addAllColumns(CassandraQueryBuilder queryBuilder) {
      queryBuilder.addColumns(
            Tables.MobileDevicesCols.APP_VERSION, Tables.MobileDevicesCols.ASSOCIATED,
            Tables.MobileDevicesCols.DEVICE_IDENTIFIER, Tables.MobileDevicesCols.DEVICE_INDEX,
            Tables.MobileDevicesCols.DEVICE_MODEL, Tables.MobileDevicesCols.DEVICE_VENDOR,
            Tables.MobileDevicesCols.FORM_FACTOR, Tables.MobileDevicesCols.LAST_LATITUDE,
            Tables.MobileDevicesCols.LAST_LOCATION_TIME, Tables.MobileDevicesCols.LAST_LONGITUDE,
            Tables.MobileDevicesCols.NAME, Tables.MobileDevicesCols.NOTIFICATION_TOKEN,
            Tables.MobileDevicesCols.OS_TYPE, Tables.MobileDevicesCols.OS_VERSION,
            Tables.MobileDevicesCols.PERSON_ID, Tables.MobileDevicesCols.PHONE_NUMBER,
            Tables.MobileDevicesCols.RESOLUTION
      );
      return queryBuilder;
   }

   private PreparedStatement prepareStreamAll() {
      CassandraQueryBuilder queryBuilder = CassandraQueryBuilder.select(Tables.MOBILE_DEVICES);
      return addAllColumns(queryBuilder).prepare(session);
   }

   @Override
   public Stream<MobileDevice> streamAll() {
      Context timer = streamAllTimer.time();
      Iterator<Row> rows = session.execute(streamAll.bind()).iterator();
      Iterator<MobileDevice> result = TimingIterator.time(
            Iterators.transform(rows, (row) -> createMobileDevice(row)),
            timer);
      Spliterator<MobileDevice> stream = Spliterators.spliteratorUnknownSize(result, Spliterator.IMMUTABLE | Spliterator.NONNULL);
      return StreamSupport.stream(stream, false);
   }
}
