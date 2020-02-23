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

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.put;
import static com.datastax.driver.core.querybuilder.QueryBuilder.putAll;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import com.google.common.util.concurrent.MoreExecutors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Update;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.Utils;
import com.iris.capability.key.NamespacedKey;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.core.dao.HubAttributesPersistenceFilter;
import com.iris.core.dao.HubDAO;
import com.iris.core.dao.cassandra.DeviceDAOImpl.NonEntityColumns;
import com.iris.core.dao.metrics.DaoMetrics;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.device.model.AttributeDefinition;
import com.iris.device.model.CapabilityDefinition;
import com.iris.io.json.JSON;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.HubAdvancedCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.HubConnectionCapability;
import com.iris.messages.errors.NotFoundException;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.model.Hub;
import com.iris.messages.services.PlatformConstants;
import com.iris.platform.PagedResults;
import com.iris.platform.model.ModelEntity;
import com.iris.platform.partition.Partitioner;
import com.iris.platform.partition.PlatformPartition;
import com.iris.util.TypeMarker;


@Singleton
public class HubDAOImpl extends BaseCassandraCRUDDao<String, Hub> implements HubDAO {
   private static final Logger log = LoggerFactory.getLogger(HubDAOImpl.class);

   private static final Timer findByMacAddrTimer = DaoMetrics.readTimer(HubDAO.class, "findByMacAddr");
   private static final Timer findHubIdsByAccountTimer = DaoMetrics.readTimer(HubDAO.class, "findHubIdsByAccount");
   private static final Timer findHubForPlaceTimer = DaoMetrics.readTimer(HubDAO.class, "findHubForPlace");
   private static final Timer listHubsTimer = DaoMetrics.readTimer(HubDAO.class, "listHubs");
   private static final Timer streamByPartitionIdTimer = DaoMetrics.readTimer(HubDAO.class, "streamByPartitionId");
   private static final Timer insertCellBackupTimesTimer = DaoMetrics.insertTimer(HubDAO.class, "insertCellBackupTimes");
   private static final Timer updateDisallowCellTimer = DaoMetrics.updateTimer(HubDAO.class, "updateDisallowCell");
   private static final Timer updateAttributesTimer = DaoMetrics.updateTimer(HubDAO.class, "updateAttributes");
   private static final Timer connectedTimer = DaoMetrics.updateTimer(HubDAO.class, "connected");
   private static final Timer disconnectedTimer = DaoMetrics.updateTimer(HubDAO.class, "disconnected");
   private static final Timer findHubModelTimer = DaoMetrics.readTimer(HubDAO.class, "findHubModel");
   private static final Timer findHubModelForPlaceTimer = DaoMetrics.readTimer(HubDAO.class, "findHubModelForPlace");
   private static final Counter hubInsertCellBackupFailure = DaoMetrics.counter(HubDAO.class, "insert.cellbackup.failure");
   private static final Counter hubUpdateAttributesFailure = DaoMetrics.counter(HubDAO.class, "update.attributes.failure");

   private static final String TABLE = "hub";
   private static final String HUB_MAC_INDEX_TABLE = "hub_macaddr";
   private static final String HUB_ACCOUNT_INDEX_TABLE = "hub_accountid";
   private static final String HUB_PLACE_INDEX_TABLE = "hub_placeid";

   static class Cols {
   	final static String MACADDR_0_7 = "macaddr_0_7";
   	final static String MACADDR = "macaddr";
   	final static String HUB_ID = "hubid";
   	final static String ACCOUNT_ID = "accountId";
   	final static String PLACE_ID = "placeid";
   }

   static class HubIdIndexColumns {
      final static String ID = "id";
      final static String HUB_MAC = "macAddress";
      final static String HUB_ACCOUNT_ID = "accountId";
   }

   static class HubEntityColumns {
      final static String ID = "id";
      final static String CREATED = "created";
      final static String MODIFIED = "modified";
      final static String TAGS = "tags";
      final static String STATE = "state";
      final static String ACCOUNT_ID = "accountId";
      final static String PLACE_ID = "placeId";
      final static String CAPS = "caps";
      final static String NAME = "name";
      final static String VENDOR = "vendor";
      final static String MODEL = "model";
      final static String SERIAL_NUM = "serialNum";
      final static String HARDWARE_VER = "hardwareVer";
      final static String MAC_ADDRESS = "macAddress";
      final static String MFG_INFO = "mfgInfo";
      final static String FIRMWARE_GROUP = "firmwareGroup";
      final static String OS_VER = "osVer";
      final static String AGENT_VER = "agentVer";
      final static String BOOTLOADER_VER = "bootloaderVer";
      final static String REGISTRATIONSTATE = "registrationState";
      final static String PARTITION_ID = "partitionId";
      final static String LAST_DEVICE_ADD_REMOVE = "lastDeviceAddRemove";
      final static String LAST_RESET = "lastReset";
      final static String DISALLOW_CELL = "disallowCell";
      final static String DISALLOW_CELL_REASON = "disallowCellReason";
   };

   private static final String[] COLUMN_ORDER = {
      HubEntityColumns.STATE,
      HubEntityColumns.ACCOUNT_ID,
      HubEntityColumns.PLACE_ID,
      HubEntityColumns.CAPS,
      HubEntityColumns.NAME,
      HubEntityColumns.VENDOR,
      HubEntityColumns.MODEL,
      HubEntityColumns.SERIAL_NUM,
      HubEntityColumns.HARDWARE_VER,
      HubEntityColumns.MAC_ADDRESS,
      HubEntityColumns.MFG_INFO,
      HubEntityColumns.FIRMWARE_GROUP,
      HubEntityColumns.OS_VER,
      HubEntityColumns.AGENT_VER,
      HubEntityColumns.BOOTLOADER_VER,
      HubEntityColumns.REGISTRATIONSTATE,
      HubEntityColumns.PARTITION_ID,
      HubEntityColumns.LAST_DEVICE_ADD_REMOVE,
      HubEntityColumns.LAST_RESET,
      HubEntityColumns.DISALLOW_CELL,
      HubEntityColumns.DISALLOW_CELL_REASON
   };

   private static final String ATTRIBUTES_COLUMN = "attributes";

   private static final String CELLBACKUP_TIME_TABLE = "cellbackup_time";

   private static enum CellBackupCol {
      dayhour, minute, hubid, simid;
      private static String[] names() {
         CellBackupCol[] cols = CellBackupCol.values();
         String[] names = new String[cols.length];
         for(int i = 0; i < cols.length; i++) {
            names[i] = cols[i].name();
         }
         return names;
      }
  }

   private PreparedStatement streamByPartition;
   private PreparedStatement insertMacAddrIdx;
   private PreparedStatement findIdByMacAddr;
   private PreparedStatement deleteMacAddrIdx;
   private PreparedStatement insertAccountIdx;
   private PreparedStatement findIdsByAccount;
   private PreparedStatement deleteAccountIdx;
   private PreparedStatement insertPlaceIdx;
   private PreparedStatement findIdForPlace;
   private PreparedStatement deletePlaceIdx;
   private PreparedStatement insertCellBackupTime;
   private PreparedStatement updateCellDisallow;
   private PreparedStatement connected;
   private PreparedStatement disconnected;

   private final PreparedStatement listPaged;

   private final Partitioner partitioner;
   private final CapabilityRegistry registry;

   private final HubAttributesPersistenceFilter filter = new HubAttributesPersistenceFilter();

   @Inject
   public HubDAOImpl(Session session, Partitioner partitioner, CapabilityRegistry registry) {
      super(session, TABLE, COLUMN_ORDER);

      streamByPartition =
            CassandraQueryBuilder
               .select(TABLE)
               .addColumns(BASE_COLUMN_ORDER)
               .addColumns(COLUMN_ORDER)
               .addWhereColumnEquals("partitionId")
               .prepare(session);
      insertMacAddrIdx = prepareInsertMacAddrIdx();
      findIdByMacAddr  = prepareFindIdByMacaddr();
      deleteMacAddrIdx = prepareDeleteMacaddrIndex();
      insertAccountIdx = prepareInsertAccountIndex();
      findIdsByAccount = prepareFindIdsByAccount();
      deleteAccountIdx = prepareDeleteAccountIndex();
      insertPlaceIdx   = prepareInsertPlaceIndex();
      findIdForPlace   = prepareFindIdForPlace();
      deletePlaceIdx   = prepareDeletePlaceIndex();

      listPaged =
            CassandraQueryBuilder
               .select(TABLE)
               .addColumns(BASE_COLUMN_ORDER)
               .addColumns(COLUMN_ORDER)
               // TODO push this down into the query builder
               .where("token(" + HubEntityColumns.ID + ") >= token(?) LIMIT ?")
               .prepare(session);

      insertCellBackupTime = CassandraQueryBuilder.insert(CELLBACKUP_TIME_TABLE)
            .addColumns(CellBackupCol.names())
            .ifNotExists()
            .prepare(session);

      updateCellDisallow = CassandraQueryBuilder.update(TABLE)
            .addColumns(HubEntityColumns.DISALLOW_CELL, HubEntityColumns.DISALLOW_CELL_REASON)
            .addWhereColumnEquals(HubEntityColumns.ID)
            .ifExists()
            .prepare(session);

      connected = 
             CassandraQueryBuilder
                .update(TABLE)
                .addColumns(
                      HubEntityColumns.STATE,
                      String.format("%s['%s']", ATTRIBUTES_COLUMN, HubConnectionCapability.ATTR_STATE),
                      String.format("%s['%s']", ATTRIBUTES_COLUMN, HubConnectionCapability.ATTR_LASTCHANGE),
                      BaseEntityColumns.MODIFIED
                )
                .addWhereColumnEquals(HubEntityColumns.ID)
                .ifClause(HubEntityColumns.STATE + " = '" + HubCapability.STATE_DOWN + "'")
                .prepare(session);

      disconnected = 
            CassandraQueryBuilder
               .update(TABLE)
               .addColumns(
                     HubEntityColumns.STATE,
                     String.format("%s['%s']", ATTRIBUTES_COLUMN, HubConnectionCapability.ATTR_STATE),
                     String.format("%s['%s']", ATTRIBUTES_COLUMN, HubConnectionCapability.ATTR_LASTCHANGE),
                     BaseEntityColumns.MODIFIED
               )
               .addWhereColumnEquals(HubEntityColumns.ID)
               .ifExists()
               .prepare(session);

      this.partitioner = partitioner;
      this.registry = registry;
   }

   @Override
   protected CassandraQueryBuilder selectNonEntityColumns(CassandraQueryBuilder queryBuilder) {
      queryBuilder.addColumns(ATTRIBUTES_COLUMN);
      return super.selectNonEntityColumns(queryBuilder);
   }

   @Override
   protected List<Statement> prepareIndexInserts(String id, Hub entity) {
      List<Statement> indexInserts = new ArrayList<>();
      addMacAddrIndexInsert(indexInserts, id, entity);
      addAccountIndexInsert(indexInserts, id, entity);
      addPlaceIndexInsert(indexInserts, id, entity);
      return indexInserts;
   }

   @Override
   protected List<Statement> prepareIndexUpdates(Hub entity) {
      Hub currentHub = findById(entity.getId());
      List<Statement> statements = new ArrayList<>();
      if (currentHub == null) {
         statements.addAll(prepareIndexInserts(entity.getId(), entity));
      }
      else {
         if (!StringUtils.equals(entity.getMac(), currentHub.getMac())) {
            addMacAddrIndexInsert(statements, entity.getId(), entity);
         }
         if (!Objects.equal(entity.getAccount(), currentHub.getAccount())) {
            addAccountIndexDelete(statements, currentHub);
            addAccountIndexInsert(statements, entity.getId(), entity);
         }
         if (!Objects.equal(entity.getPlace(), currentHub.getPlace())) {
            addPlaceIndexDelete(statements, currentHub);
            addPlaceIndexInsert(statements, entity.getId(), entity);
         }
      }
      return statements;
   }

   @Override
   protected List<Statement> prepareIndexDeletes(Hub entity) {
      List<Statement> indexDeletes = new ArrayList<>();
      addMacAddrIndexDelete(indexDeletes, entity);
      addAccountIndexDelete(indexDeletes, entity);
      addPlaceIndexDelete(indexDeletes, entity);
      return indexDeletes;
   }

   @Override
   protected List<Object> getValues(Hub entity) {
      List<Object> values = new LinkedList<Object>();
      values.add(entity.getState());
      values.add(entity.getAccount());
      values.add(entity.getPlace());
      values.add(entity.getCaps());
      values.add(entity.getName());
      values.add(entity.getVendor());
      values.add(entity.getModel());
      values.add(entity.getSerialNum());
      values.add(entity.getHardwarever());
      values.add(entity.getMac());
      values.add(entity.getMfgInfo());
      values.add(entity.getFirmwareGroup());
      values.add(entity.getOsver());
      values.add(entity.getAgentver());
      values.add(entity.getBootloaderVer());
      values.add(entity.getRegistrationState());
      UUID placeId = entity.getPlace();
      PlatformPartition partition =
            placeId == null ?
                  partitioner.getPartitionForHubId(entity.getId()) :
                  partitioner.getPartitionForPlaceId(placeId);
      values.add(partition.getId());
      values.add(entity.getLastDeviceAddRemove());
      values.add(entity.getLastReset());
      values.add(entity.isDisallowCell());
      values.add(entity.getDisallowCellReason());

      log.trace("Hub:Values = [{}]", values );
      return values;
   }

   @Override
   protected Hub createEntity() {
      return new Hub();
   }

   @Override
   protected void populateEntity(Row row, Hub entity) {
      entity.setState(row.getString(HubEntityColumns.STATE));
      entity.setAccount(row.getUUID(HubEntityColumns.ACCOUNT_ID));
      entity.setPlace(row.getUUID(HubEntityColumns.PLACE_ID));
      entity.setCaps(row.getSet(HubEntityColumns.CAPS, String.class));
      entity.setName(row.getString(HubEntityColumns.NAME));
      entity.setVendor(row.getString(HubEntityColumns.VENDOR));
      entity.setModel(row.getString(HubEntityColumns.MODEL));
      entity.setSerialNum(row.getString(HubEntityColumns.SERIAL_NUM));
      entity.setHardwarever(row.getString(HubEntityColumns.HARDWARE_VER));
      entity.setMac(row.getString(HubEntityColumns.MAC_ADDRESS));
      entity.setMfgInfo(row.getString(HubEntityColumns.MFG_INFO));
      entity.setFirmwareGroup(row.getString(HubEntityColumns.FIRMWARE_GROUP));
      entity.setOsver(row.getString(HubEntityColumns.OS_VER));
      entity.setAgentver(row.getString(HubEntityColumns.AGENT_VER));
      entity.setBootloaderVer(row.getString(HubEntityColumns.BOOTLOADER_VER));
      entity.setRegistrationState(row.getString(HubEntityColumns.REGISTRATIONSTATE));
      entity.setLastDeviceAddRemove(row.getUUID(HubEntityColumns.LAST_DEVICE_ADD_REMOVE));
      entity.setLastReset(row.getUUID(HubEntityColumns.LAST_RESET));
      entity.setDisallowCell(row.getBool(HubEntityColumns.DISALLOW_CELL));
      entity.setDisallowCellReason(row.getString(HubEntityColumns.DISALLOW_CELL_REASON));

   }

   @Override
   protected String getIdFromRow(Row row) {
      return row.getString(BaseEntityColumns.ID);
   }

   @Override
   protected String nextId(Hub hub) {
      return hub.getId();
   }

   @Override
   public Hub findByMacAddr(String macAddr) {
      if(StringUtils.isBlank(macAddr)) {
         return null;
      }
      try(Context ctx = findByMacAddrTimer.time()) {
          String hubId = findIdByMacAddr(macAddr);
          return hubId == null ? null : findById(hubId);
      }
   }

   @Override
   public Set<String> findHubIdsByAccount(UUID accountId) {
      Set<String> hubIds = new HashSet<String>();
      if (accountId != null) {
         BoundStatement boundStatement = new BoundStatement(findIdsByAccount);
         ResultSet resultSet;
         try(Context ctxt = findHubIdsByAccountTimer.time()) {
        	 resultSet = session.execute(boundStatement.bind(accountId));
         }

         List<Row> rows = resultSet.all();
         for (Row row : rows) {
            hubIds.add(row.getString("hubid"));
         }
      }
      return hubIds;
   }

   @Override
   public Hub findHubForPlace(UUID placeId) {
      if(placeId == null) {
         return null;
      }
      BoundStatement boundStatement = new BoundStatement(findIdForPlace);
      try(Context ctxt = findHubForPlaceTimer.time()) {
    	  Row row = session.execute(boundStatement.bind(placeId)).one();
    	  if(row == null) {
    		  return null;
    	  }
    	  return findById(row.getString("hubid"));
      }
   }

   /* (non-Javadoc)
    * @see com.iris.core.dao.HubDAO#listHubs(com.iris.core.dao.HubDAO.HubQuery)
    */
   @Override
   public PagedResults<Hub> listHubs(HubQuery query) {
      BoundStatement bs = listPaged.bind(Optional.fromNullable(query.getToken()).or(""), query.getLimit() + 1);
      try(Context ctxt = listHubsTimer.time()) {
         return doList(bs, query.getLimit());
      }
   }

   @Override
   public Stream<Hub> streamByPartitionId(int partitionId) {
      BoundStatement bs = streamByPartition.bind(partitionId);
      try(Context ctxt = streamByPartitionIdTimer.time()) {
         ResultSet rs = session.execute(bs);
         return stream(rs, (row) -> buildEntity(row));
      }
   }

   @Override
   public void insertCellBackupTimes(Calendar now, Map<String, String> hubSimIdMap, int snapTo) {
      Calendar snapped = snappedMinutes(now, snapTo);
      Calendar dayHour = DateUtils.truncate(snapped, Calendar.HOUR);
      hubSimIdMap.forEach((h,s) -> {
         BoundStatement stmt = new BoundStatement(insertCellBackupTime);
         stmt.setTimestamp(CellBackupCol.dayhour.name(), dayHour.getTime());
         stmt.setInt(CellBackupCol.minute.name(), snapped.get(Calendar.MINUTE));
         stmt.setString(CellBackupCol.hubid.name(), h);
         stmt.setString(CellBackupCol.simid.name(), s);
         // not sure we really care about the result here
         final Context ctxt = insertCellBackupTimesTimer.time();
         Futures.addCallback(session.executeAsync(stmt), new FutureCallback<ResultSet>() {
            @Override
            public void onSuccess(ResultSet result) {
               ctxt.stop();
               if(!result.wasApplied()) {
                  hubInsertCellBackupFailure.inc();
               }
            }
            @Override
            public void onFailure(Throwable t) {
               ctxt.stop();
               hubInsertCellBackupFailure.inc();
            }
         }, MoreExecutors.directExecutor());
      });
   }

   @Override
   public void disallowCell(String hubId, String reason) {
      updateDisallowCell(hubId, true, reason);
   }

   @Override
   public void allowCell(String hubId) {
      updateDisallowCell(hubId, false, null);
   }

   @Override
   public Map<String, Object> connected(String hubId) {
      Date ts = new Date();
      BoundStatement bs = connected.bind(HubCapability.STATE_NORMAL, JSON.toJson(HubConnectionCapability.STATE_ONLINE), JSON.toJson(ts), ts, hubId);
      Address hubAddress = Address.hubService(hubId, HubCapability.NAMESPACE);
      try(Context ctx = connectedTimer.time()) {
         ResultSet rs = session.execute(bs);
         if(rs.wasApplied()) {
            return ImmutableMap.of(
                  HubCapability.ATTR_STATE, HubCapability.STATE_NORMAL,
                  HubConnectionCapability.ATTR_STATE, HubConnectionCapability.STATE_ONLINE,
                  HubConnectionCapability.ATTR_LASTCHANGE, ts
            );
         }
         else {
            Row row = rs.one();
            if(row.getColumnDefinitions().contains(HubEntityColumns.STATE)) {
               return ImmutableMap.of();
            }
            else {
               throw new NotFoundException(hubAddress);
            }
         }
      }
   }

   @Override
   public Map<String, Object> disconnected(String hubId) {
      Date ts = new Date();
      BoundStatement bs = disconnected.bind(HubCapability.STATE_DOWN, JSON.toJson(HubConnectionCapability.STATE_OFFLINE), JSON.toJson(ts), ts, hubId);
      try(Context ctx = disconnectedTimer.time()) {
         ResultSet rs = session.execute(bs);
         if( !rs.wasApplied() ) {
            throw new NotFoundException( Address.hubService(hubId, HubCapability.NAMESPACE) );
         }
      }
      return ImmutableMap.of(
            HubCapability.ATTR_STATE, HubCapability.STATE_DOWN,
            HubConnectionCapability.ATTR_STATE, HubConnectionCapability.STATE_OFFLINE,
            HubConnectionCapability.ATTR_LASTCHANGE, ts
      );
   }

	private void updateDisallowCell(String hubId, boolean disallow, String reason) {
      Preconditions.checkNotNull(hubId, "hubId is required");
      BoundStatement stmt = new BoundStatement(updateCellDisallow);
      stmt.setBool(HubEntityColumns.DISALLOW_CELL, disallow);
      stmt.setString(HubEntityColumns.DISALLOW_CELL_REASON, reason);
      stmt.setString(HubEntityColumns.ID, hubId);
      try(Context ctxt = updateDisallowCellTimer.time()) {
         session.execute(stmt);
      }
   }

   private Calendar snappedMinutes(Calendar time, int snapTo) {
      int minutes = time.get(Calendar.MINUTE);
      int mod = minutes % snapTo;
      int mid = (int) Math.floor(snapTo / 2);
      time = DateUtils.truncate(time, Calendar.MINUTE);
      time.add(Calendar.MINUTE, mod < mid ? -mod : snapTo - mod);
      return time;
   }

   @Override
   public void updateAttributes(String hubId, Map<String, Object> attrs) {
      Preconditions.checkNotNull(hubId, "device cannot be null");

      Map<String, Object> filtered = filter.filter(attrs);
      Map<String, String> attributesAsStrings = new HashMap<>();
      Update update = QueryBuilder.update(TABLE);
      update.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
      update.where(eq(BaseEntityColumns.ID, hubId));

      filtered.forEach((k,v) -> {
         if(v == null) {
            update.with(put(ATTRIBUTES_COLUMN, k, null));
         } else {
            attributesAsStrings.put(k, JSON.toJson(v));
         }
      });

      update.with(putAll(ATTRIBUTES_COLUMN, attributesAsStrings));
      final Context ctxt = updateAttributesTimer.time();
      Futures.addCallback(session.executeAsync(update), new FutureCallback<ResultSet>() {
         @Override
         public void onSuccess(ResultSet result) {
            ctxt.stop();
            if(!result.wasApplied()) {
               hubUpdateAttributesFailure.inc();
            }
         }
         @Override
         public void onFailure(Throwable t) {
            ctxt.stop();
            hubUpdateAttributesFailure.inc();
         }
      }, MoreExecutors.directExecutor());
   }

   @Override
   public ModelEntity findHubModel(String id) {
      try(Context ctxt = findHubModelTimer.time()) {
         BoundStatement boundStatement = new BoundStatement(findById);
         boundStatement.bind(id);
         Row r = session.execute(boundStatement).one();
         return toModel(r);
      }
   }

   @Override
   public ModelEntity findHubModelForPlace(UUID placeId) {
      if(placeId == null) {
         return null;
      }
      try(Context ctxt = findHubModelForPlaceTimer.time()) {
         BoundStatement boundStatement = new BoundStatement(findIdForPlace);
         Row row = session.execute(boundStatement.bind(placeId)).one();
         if(row == null) {
            return null;
         }
         return findHubModel(row.getString("hubid"));
      }
   }

   private ModelEntity toModel(Row r) {
      if(r == null) {
         return null;
      }

      Map<String, Object> attributes = toAttributes(r);

      ModelEntity entity = new ModelEntity(attributes);
      entity.setCreated(r.getTimestamp(BaseEntityColumns.CREATED));
      entity.setModified(r.getTimestamp(BaseEntityColumns.MODIFIED));
      return entity;
   }

   private Map<String,Object> toAttributes(Row r) {
      Map<String, String> encoded = r.getMap(NonEntityColumns.ATTRIBUTES, String.class, String.class);
      AttributeMap attributes = AttributeMap.newMap();
      for(Map.Entry<String, String> entry: encoded.entrySet()) {
         AttributeKey<?> key = key(entry.getKey());
         if(key == null) {
            continue;
         }
         Object value = deserialize(key, entry.getValue());
         attributes.add(key.coerceToValue(value));
      }
      Map<String, Object> model = attributes.toMap();

      // base attributes
      String id = r.getString(HubEntityColumns.ID);
      Utils.setIf(Capability.ATTR_TYPE, HubCapability.NAMESPACE, model);
      Utils.setIf(Capability.ATTR_ID, id, model);
      Utils.setIf(Capability.ATTR_ADDRESS, Address.hubService(id, PlatformConstants.SERVICE_HUB).getRepresentation(), model);
      setOrDefault(Capability.ATTR_TAGS, r.getSet(BaseEntityColumns.TAGS, String.class), ImmutableSet.of(), model);
      setOrDefault(Capability.ATTR_CAPS, r.getSet(HubEntityColumns.CAPS, String.class), ImmutableSet.of(Capability.NAMESPACE, HubCapability.NAMESPACE), model);

      // hub attributes
      Utils.setIf(HubCapability.ATTR_ID, id, model);
      Utils.setIf(HubCapability.ATTR_ACCOUNT, Utils.coerceToString(r.getUUID(HubEntityColumns.ACCOUNT_ID)), model);
      Utils.setIf(HubCapability.ATTR_PLACE, Utils.coerceToString(r.getUUID(HubEntityColumns.PLACE_ID)), model);
      setOrDefault(HubCapability.ATTR_NAME, r.getString(HubEntityColumns.NAME), "My Hub", model);
      Utils.setIf(HubCapability.ATTR_VENDOR, r.getString(HubEntityColumns.VENDOR), model);
      Utils.setIf(HubCapability.ATTR_MODEL, r.getString(HubEntityColumns.MODEL), model);
      Utils.setIf(HubCapability.ATTR_STATE, r.getString(HubEntityColumns.STATE), model);
      Utils.setIf(HubCapability.ATTR_REGISTRATIONSTATE, r.getString(HubEntityColumns.REGISTRATIONSTATE), model);

      // hub advanced attributes
      Utils.setIf(HubAdvancedCapability.ATTR_MAC, r.getString(HubEntityColumns.MAC_ADDRESS), model);
      Utils.setIf(HubAdvancedCapability.ATTR_HARDWAREVER, r.getString(HubEntityColumns.HARDWARE_VER), model);
      Utils.setIf(HubAdvancedCapability.ATTR_OSVER, r.getString(HubEntityColumns.OS_VER), model);
      Utils.setIf(HubAdvancedCapability.ATTR_AGENTVER, r.getString(HubEntityColumns.AGENT_VER), model);
      Utils.setIf(HubAdvancedCapability.ATTR_SERIALNUM, r.getString(HubEntityColumns.SERIAL_NUM), model);
      Utils.setIf(HubAdvancedCapability.ATTR_MFGINFO, r.getString(HubEntityColumns.MFG_INFO), model);
      Utils.setIf(HubAdvancedCapability.ATTR_BOOTLOADERVER, r.getString(HubEntityColumns.BOOTLOADER_VER), model);
      Utils.setIf(HubAdvancedCapability.ATTR_FIRMWAREGROUP, r.getString(HubEntityColumns.FIRMWARE_GROUP), model);
      Utils.setIf(HubAdvancedCapability.ATTR_LASTRESET, Utils.coerceToString(r.getUUID(HubEntityColumns.LAST_RESET)), model);
      Utils.setIf(HubAdvancedCapability.ATTR_LASTDEVICEADDREMOVE, Utils.coerceToString(r.getUUID(HubEntityColumns.LAST_DEVICE_ADD_REMOVE)), model);

      return model;
   }

   // TODO: move to utils
   private <V> void setOrDefault(String key, V value, V defaultValue, Map<String, Object> model) {
      if(value == null) {
         model.put(key, defaultValue);
      }
      else {
         model.put(key, value);
      }
   }

   @Nullable
   private AttributeKey<?> key(String attributeName) {
      NamespacedKey name = NamespacedKey.parse(attributeName);
      CapabilityDefinition capability = registry.getCapabilityDefinitionByNamespace(name.getNamespace());
      if(capability == null) {
         log.warn("Unable to find capability namespace [{}]", name.getNamespace());
         return null;
      }
      AttributeDefinition attribute = capability.getAttributes().get(name.getNamedRepresentation());
      if(attribute == null) {
         log.warn("Unable to find attribute [{}]", name.getNamedRepresentation());
         return null;
      }
      if(name.isInstanced()) {
         return attribute.getKey().instance(name.getInstance());
      }
      return attribute.getKey();
   }

   private Object deserialize(AttributeKey<?> key, String value) {
      if(value == null) {
         return null;
      }
      return JSON.fromJson(value, TypeMarker.wrap(key.getType()));
   }

   private PreparedStatement prepareInsertMacAddrIdx() {
   	return CassandraQueryBuilder.insert(HUB_MAC_INDEX_TABLE)
   					.addColumn(Cols.MACADDR_0_7)
   					.addColumn(Cols.MACADDR)
   					.addColumn(Cols.HUB_ID)
   					.prepare(session);
   }

   private PreparedStatement prepareFindIdByMacaddr() {
   	return CassandraQueryBuilder.select(HUB_MAC_INDEX_TABLE)
   					.addColumn(Cols.HUB_ID)
   					.addWhereColumnEquals(Cols.MACADDR_0_7)
   					.addWhereColumnEquals(Cols.MACADDR)
   					.prepare(session);
   }

   private PreparedStatement prepareDeleteMacaddrIndex() {
   	return CassandraQueryBuilder.delete(HUB_MAC_INDEX_TABLE)
   					.addWhereColumnEquals(Cols.MACADDR_0_7)
   					.addWhereColumnEquals(Cols.MACADDR)
   					.prepare(session);
   }

   private PreparedStatement prepareInsertAccountIndex() {
   	return CassandraQueryBuilder.insert(HUB_ACCOUNT_INDEX_TABLE)
   					.addColumn(Cols.ACCOUNT_ID)
   					.addColumn(Cols.HUB_ID)
   					.prepare(session);
   }

   private PreparedStatement prepareFindIdsByAccount() {
   	return CassandraQueryBuilder.select(HUB_ACCOUNT_INDEX_TABLE)
   					.addColumn(Cols.HUB_ID)
   					.addWhereColumnEquals(Cols.ACCOUNT_ID)
   					.prepare(session);
   }

   private PreparedStatement prepareDeleteAccountIndex() {
   	return CassandraQueryBuilder.delete(HUB_ACCOUNT_INDEX_TABLE)
   					.addWhereColumnEquals(Cols.HUB_ID)
   					.addWhereColumnEquals(Cols.ACCOUNT_ID)
   					.prepare(session);
   }

   private PreparedStatement prepareInsertPlaceIndex() {
   	return CassandraQueryBuilder.insert(HUB_PLACE_INDEX_TABLE)
   					.addColumn(Cols.PLACE_ID)
   					.addColumn(Cols.HUB_ID)
   					.prepare(session);
   }

   private PreparedStatement prepareFindIdForPlace() {
   	return CassandraQueryBuilder.select(HUB_PLACE_INDEX_TABLE)
   					.addColumn(Cols.HUB_ID)
   					.addWhereColumnEquals(Cols.PLACE_ID)
   					.prepare(session);
   }

   private PreparedStatement prepareDeletePlaceIndex() {
   	return CassandraQueryBuilder.delete(HUB_PLACE_INDEX_TABLE)
   					.addWhereColumnEquals(Cols.HUB_ID)
   					.addWhereColumnEquals(Cols.PLACE_ID)
   					.prepare(session);
   }

   private void addMacAddrIndexInsert(List<Statement> statements, String id, Hub entity) {
      if(!StringUtils.isBlank(entity.getMac())) {
         ParsedMacAddr parsed = ParsedMacAddr.parse(entity.getMac());
         statements.add(new BoundStatement(insertMacAddrIdx).bind(parsed.macAddr_0_7, parsed.macAddr, id));
      }
   }

   private void addAccountIndexInsert(List<Statement> statements, String id, Hub entity) {
      if (entity.getAccount() != null) {
         statements.add(new BoundStatement(insertAccountIdx).bind(entity.getAccount(), entity.getId()));
      }
   }

   private void addPlaceIndexInsert(List<Statement> statements, String id, Hub entity) {
      if (entity.getPlace() != null) {
         statements.add(new BoundStatement(insertPlaceIdx).bind(entity.getPlace(), entity.getId()));
      }
   }

   private void addMacAddrIndexDelete(List<Statement> statements, Hub entity) {
      if(!StringUtils.isBlank(entity.getMac())) {
         ParsedMacAddr parsed = ParsedMacAddr.parse(entity.getMac());
         statements.add(new BoundStatement(deleteMacAddrIdx).bind(parsed.macAddr_0_7, parsed.macAddr));
      }
   }

   private void addAccountIndexDelete(List<Statement> statements, Hub entity) {
      if(entity.getAccount() != null) {
         statements.add(new BoundStatement(deleteAccountIdx).bind(entity.getId(), entity.getAccount()));
      }
   }

   private void addPlaceIndexDelete(List<Statement> statements, Hub entity) {
      if(entity.getPlace() != null) {
         statements.add(new BoundStatement(deletePlaceIdx).bind(entity.getId(), entity.getPlace()));
      }
   }

   private String findIdByMacAddr(String macAddr) {
      ParsedMacAddr parsed = ParsedMacAddr.parse(macAddr);
      BoundStatement boundStatement = new BoundStatement(findIdByMacAddr);
      Row row = session.execute(boundStatement.bind(parsed.macAddr_0_7, parsed.macAddr)).one();
      return row == null ? null : row.getString("hubId");
   }

   private static class ParsedMacAddr {
      String macAddr_0_7;
      String macAddr;

      static ParsedMacAddr parse(String macAddr) {
         ParsedMacAddr parsed = new ParsedMacAddr();

         parsed.macAddr = macAddr;

         if(parsed.macAddr.length() > 8) {
            parsed.macAddr_0_7 = parsed.macAddr.substring(0, 8);
         } else {
            parsed.macAddr_0_7 = parsed.macAddr;
         }
         return parsed;
      }
   }

}

