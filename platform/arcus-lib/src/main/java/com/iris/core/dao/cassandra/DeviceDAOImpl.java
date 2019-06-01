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
import static com.datastax.driver.core.querybuilder.QueryBuilder.putAll;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Update;
import com.datastax.driver.core.utils.Bytes;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.capability.key.NamespacedKey;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.metrics.ColumnRepairMetrics;
import com.iris.core.dao.metrics.DaoMetrics;
import com.iris.core.driver.DeviceDriverStateHolder;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.device.model.AttributeDefinition;
import com.iris.device.model.CapabilityDefinition;
import com.iris.io.Deserializer;
import com.iris.io.Serializer;
import com.iris.io.json.JSON;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.model.Device;
import com.iris.messages.model.DriverId;
import com.iris.model.Version;
import com.iris.model.type.AttributeType;
import com.iris.model.type.AttributeTypes;
import com.iris.model.type.EnumType;
import com.iris.model.type.PrimitiveType;
import com.iris.model.type.TimestampType;
import com.iris.platform.model.ModelEntity;
import com.iris.util.IrisCollections;
import com.iris.util.TypeMarker;

@Singleton
public class DeviceDAOImpl extends BaseCassandraCRUDDao<UUID, Device> implements DeviceDAO {

   org.slf4j.Logger log = LoggerFactory.getLogger(DeviceDAOImpl.class); // Where to log information and debugging strings.

   private static final Timer findByHubIdTimer = DaoMetrics.readTimer(DeviceDAO.class, "findByHubId");
   private static final Timer findByProtocolAddressTimer = DaoMetrics.readTimer(DeviceDAO.class, "findByProtocolAddress");
   private static final Timer listDeviceAttributesByAccountIdTimer = DaoMetrics.readTimer(DeviceDAO.class, "listDeviceAttributesByAccountId");
   private static final Timer listDeviceAttributesByPlaceIdTimer = DaoMetrics.readTimer(DeviceDAO.class, "listDeviceAttributesByPlaceId");
   private static final Timer listDevicesByPlaceIdTimer = DaoMetrics.readTimer(DeviceDAO.class, "listDevicesByPlaceId");
   private static final Timer streamDeviceModelByPlaceIdTimer = DaoMetrics.readTimer(DeviceDAO.class, "streamDeviceModelByPlaceId");
   private static final Timer loadDriverStateTimer = DaoMetrics.readTimer(DeviceDAO.class, "loadDriverState");
   private static final Timer replaceDriverStateTimer = DaoMetrics.updateTimer(DeviceDAO.class, "replaceDriverState");
   private static final Timer updateDriverStateTimer = DaoMetrics.updateTimer(DeviceDAO.class, "updateDriverState");
   private static final Timer removeAttributesTimer = DaoMetrics.deleteTimer(DeviceDAO.class, "removeAttributes");
   private static final Timer modelByIdTimer = DaoMetrics.readTimer(DeviceDAO.class, "modelById");


   private static final String TABLE = "device";
   private static final String HUB_ID_INDEX_TABLE = "device_hubid";
   private static final String PROTOCOL_ADDR_INDEX_TABLE = "device_protocoladdress";
   private static final String PLACE_INDEX_TABLE = "device_placeid";

   private static final String DFLT_NAME = "New Device";

   static class HubIndexColumns {
      final static String DEVICE_ID = "devId";
      final static String HUB_ID = "hubId";
   }


   static class ProtocolAddrIndexColumns {
      final static String DEVICE_ID = "id";
      final static String PROTOCOL_ADDRESS = "protocolAddress";
   }

   static class DeviceEntityColumns {
      final static String STATE = "state";
      final static String PROTOCOL_ID = "protocolId";
      final static String DRIVER_NAME = "driverName";
      @Deprecated
      final static String DRIVER_VERSION = "driverVersion";
      final static String DRIVER_VERSION2 = "driverVersion2";
      final static String PROTOCOL_NAME = "protocolName";
      final static String ACCOUNT_ID = "accountId";
      final static String DRIVER_ADDRESS = "driverAddress";
      final static String PROTOCOL_ADDRESS = "protocolAddress";
      @Deprecated
      final static String HUB_ID = "hubId";
      final static String HUB_ID2 = "hubId2";
      final static String PLACE_ID = "placeId";
      final static String CAPS = "caps";
      final static String DEVTYPEHINT = "devTypeHint";
      final static String NAME = "name";
      final static String VENDOR = "vendor";
      final static String MODEL = "model";
      final static String PRODUCTID = "productId";
      final static String SUBPROTOCOL = "subprotocol";
      final static String PROTOCOL_ATTRS = "protocolattrs";
      final static String DEGRADED = "degraded";
      final static String HUBLOCAL = "hublocal";
   };

   static class NonEntityColumns {
      final static String ATTRIBUTES = "attributes";
      final static String VARIABLES = "variables";
   }

   private static final String[] COLUMN_ORDER = {
      DeviceEntityColumns.ACCOUNT_ID,
      DeviceEntityColumns.DRIVER_ADDRESS,
      DeviceEntityColumns.PROTOCOL_ADDRESS,
      DeviceEntityColumns.HUB_ID2,
      DeviceEntityColumns.PROTOCOL_NAME,
      DeviceEntityColumns.PROTOCOL_ID,
      DeviceEntityColumns.DRIVER_NAME,
      DeviceEntityColumns.DRIVER_VERSION2,
      DeviceEntityColumns.STATE,
      DeviceEntityColumns.PLACE_ID,
      DeviceEntityColumns.CAPS,
      DeviceEntityColumns.DEVTYPEHINT,
      DeviceEntityColumns.NAME,
      DeviceEntityColumns.VENDOR,
      DeviceEntityColumns.MODEL,
      DeviceEntityColumns.PRODUCTID,
      DeviceEntityColumns.SUBPROTOCOL,
      DeviceEntityColumns.PROTOCOL_ATTRS,
      DeviceEntityColumns.DEGRADED,
      DeviceEntityColumns.HUBLOCAL
   };

   // the only driver mutable fields are NAME and IMAGE, these aren't
   // indexed so they can be mutated via update/replace attributes
   private static final Map<String, String> ATTR_TO_COLUMN_MAP =
         IrisCollections
            .<String, String>immutableMap()
            .put(DeviceCapability.ATTR_NAME, DeviceEntityColumns.NAME)
            .put(Capability.ATTR_IMAGES, BaseEntityColumns.IMAGES)
            .create();

   private final CapabilityRegistry registry;
   private final Serializer<AttributeMap> attributeMapSerializer;
   private final Deserializer<AttributeMap> attributeMapDeserializer;

   private PreparedStatement findByHubId;
   private PreparedStatement deleteHubIndex;
   private PreparedStatement insertHubIndex;
   private PreparedStatement findByProtocolAddr;
   private PreparedStatement deleteProtocolAddrIndex;
   private PreparedStatement insertProtocolAddrIndex;
   private PreparedStatement findByAccountId;
   private PreparedStatement loadState;
   private PreparedStatement insertPlaceIndex;
   private PreparedStatement deletePlaceIndex;
   private PreparedStatement findIdsByPlace;

   @Inject(optional = true) @Named("dao.device.readconsistency")
   private ConsistencyLevel readConsistency = ConsistencyLevel.LOCAL_QUORUM;
   @Inject(optional = true) @Named("dao.device.writeconsistency")
   private ConsistencyLevel writeConsistency = ConsistencyLevel.LOCAL_QUORUM;
   @Inject(optional = true) @Named("dao.device.asynctimeoutms")
   private long asyncTimeoutMs = 30000;


   @Inject
   public DeviceDAOImpl(Session session, CapabilityRegistry registry) {
      super(session, TABLE, COLUMN_ORDER);
      this.registry = registry;

      findByHubId = CassandraQueryBuilder.select(HUB_ID_INDEX_TABLE)
            .addColumns(HubIndexColumns.HUB_ID, HubIndexColumns.DEVICE_ID)
            .addWhereColumnEquals(HubIndexColumns.HUB_ID)
            .withConsistencyLevel(readConsistency)
            .prepare(session);

      deleteHubIndex = CassandraQueryBuilder.delete(HUB_ID_INDEX_TABLE)
            .addWhereColumnEquals(HubIndexColumns.HUB_ID)
            .addWhereColumnEquals(HubIndexColumns.DEVICE_ID)
            .withConsistencyLevel(writeConsistency)
            .prepare(session);

      insertHubIndex = CassandraQueryBuilder.insert(HUB_ID_INDEX_TABLE)
            .addColumns(HubIndexColumns.HUB_ID, HubIndexColumns.DEVICE_ID)
            .withConsistencyLevel(writeConsistency)
            .prepare(session);

      findByProtocolAddr = CassandraQueryBuilder.select(PROTOCOL_ADDR_INDEX_TABLE)
            .addColumns(ProtocolAddrIndexColumns.DEVICE_ID, ProtocolAddrIndexColumns.PROTOCOL_ADDRESS)
            .addWhereColumnEquals(ProtocolAddrIndexColumns.PROTOCOL_ADDRESS)
            .withConsistencyLevel(readConsistency)
            .prepare(session);

      deleteProtocolAddrIndex = CassandraQueryBuilder.delete(PROTOCOL_ADDR_INDEX_TABLE)
            .addWhereColumnEquals(ProtocolAddrIndexColumns.PROTOCOL_ADDRESS)
            .withConsistencyLevel(writeConsistency)
            .prepare(session);

      insertProtocolAddrIndex = CassandraQueryBuilder.insert(PROTOCOL_ADDR_INDEX_TABLE)
            .addColumns(ProtocolAddrIndexColumns.DEVICE_ID, ProtocolAddrIndexColumns.PROTOCOL_ADDRESS)
            .withConsistencyLevel(writeConsistency)
            .prepare(session);

      findByAccountId = prepareFindByAccountId();

      loadState = CassandraQueryBuilder.select(TABLE)
            .addColumns(NonEntityColumns.ATTRIBUTES, NonEntityColumns.VARIABLES)
            .addWhereColumnEquals(BaseEntityColumns.ID)
            .withConsistencyLevel(readConsistency)
            .prepare(session);

      insertPlaceIndex = CassandraQueryBuilder.insert(PLACE_INDEX_TABLE)
            .addColumns("placeid", "devid")
            .withConsistencyLevel(writeConsistency)
            .prepare(session);

      deletePlaceIndex = CassandraQueryBuilder.delete(PLACE_INDEX_TABLE)
            .addWhereColumnEquals("devid")
            .addWhereColumnEquals("placeid")
            .withConsistencyLevel(writeConsistency)
            .prepare(session);

      findIdsByPlace = CassandraQueryBuilder.select(PLACE_INDEX_TABLE)
            .addColumn("devid")
            .addWhereColumnEquals("placeid")
            .withConsistencyLevel(readConsistency)
            .prepare(session);

      this.attributeMapSerializer = JSON.createSerializer(AttributeMap.class);
      this.attributeMapDeserializer = JSON.createDeserializer(AttributeMap.class);
   }

   private PreparedStatement prepareFindByAccountId() {
      CassandraQueryBuilder queryBuilder = CassandraQueryBuilder.select(TABLE)
            .addColumns(BASE_COLUMN_ORDER)
            .addColumns(COLUMN_ORDER)
            .addWhereColumnEquals(DeviceEntityColumns.ACCOUNT_ID)
            .withConsistencyLevel(readConsistency);
      return selectNonEntityColumns(queryBuilder).prepare(session);
   }

   @Override
   protected CassandraQueryBuilder selectNonEntityColumns(CassandraQueryBuilder queryBuilder) {
      queryBuilder.addColumns(DeviceEntityColumns.DRIVER_VERSION, DeviceEntityColumns.HUB_ID, NonEntityColumns.ATTRIBUTES, NonEntityColumns.VARIABLES);
      return super.selectNonEntityColumns(queryBuilder);
   }

   @Override
   protected List<Object> getValues(Device entity) {
      List<Object> values = new LinkedList<Object>();
      values.add(entity.getAccount());
      values.add(entity.getAddress());
      values.add(entity.getProtocolAddress());
      values.add(entity.getHubId());
      values.add(entity.getProtocol());
      values.add(entity.getProtocolid());
      values.add(entity.getDriverId() != null ? entity.getDriverId().getName() : null);
      values.add(entity.getDriverId() != null ? entity.getDriverId().getVersion().getRepresentation() : null);
      values.add(entity.getState());
      values.add(entity.getPlace());
      values.add(entity.getCaps());
      values.add(entity.getDevtypehint());
      values.add(entity.getName());
      values.add(entity.getVendor());
      values.add(entity.getModel());
      values.add(entity.getProductId());
      values.add(entity.getSubprotocol());

      byte[] protocolAttributes = protocolAttributesToBytes(entity);
      ByteBuffer buffer = null;

      if(protocolAttributes != null) {
         buffer = ByteBuffer.wrap(protocolAttributes);
      }

      values.add(buffer == null ? null : buffer);
      values.add(entity.getDegradedCode());
      values.add(entity.isHubLocal());

      return values;
   }

   @Override
   protected Device createEntity() {
      return new Device();
   }

   @Override
   protected void populateEntity(Row row, Device entity) {
      entity.setAccount(row.getUUID(DeviceEntityColumns.ACCOUNT_ID));
      entity.setAddress(row.getString(DeviceEntityColumns.DRIVER_ADDRESS));
      entity.setProtocolAddress(row.getString(DeviceEntityColumns.PROTOCOL_ADDRESS));

      String hubId = row.getString(DeviceEntityColumns.HUB_ID2);
      if(hubId == null) {
         ColumnRepairMetrics.incHubIdCounter();
         hubId = row.getString(DeviceEntityColumns.HUB_ID);
      }

      entity.setHubId(hubId);
      entity.setProtocol(row.getString(DeviceEntityColumns.PROTOCOL_NAME));
      entity.setProtocolid(row.getString(DeviceEntityColumns.PROTOCOL_ID));

      String version = row.getString(DeviceEntityColumns.DRIVER_VERSION2);
      if(version == null) {
         ColumnRepairMetrics.incDriverVersionCounter();
         version = row.getString(DeviceEntityColumns.DRIVER_VERSION);
      }

      DriverId driverId = new DriverId(row.getString(DeviceEntityColumns.DRIVER_NAME), version == null ? Version.UNVERSIONED : Version.fromRepresentation(version));
      entity.setDriverId(driverId);
      entity.setState(row.getString(DeviceEntityColumns.STATE));
      entity.setPlace(row.getUUID(DeviceEntityColumns.PLACE_ID));
      entity.setCaps(row.getSet(DeviceEntityColumns.CAPS, String.class));
      entity.setDevtypehint(row.getString(DeviceEntityColumns.DEVTYPEHINT));
      entity.setName(row.getString(DeviceEntityColumns.NAME));

      entity.setVendor(row.getString(DeviceEntityColumns.VENDOR));
      entity.setModel(row.getString(DeviceEntityColumns.MODEL));
      entity.setProductId(row.getString(DeviceEntityColumns.PRODUCTID));
      entity.setSubprotocol(row.getString(DeviceEntityColumns.SUBPROTOCOL));
      ByteBuffer buf = row.getBytes(DeviceEntityColumns.PROTOCOL_ATTRS);
      if (buf != null) {
         entity.setProtocolAttributes(bytesToProtocolAttributes(Bytes.getArray(buf)));
      }
      entity.setDegradedCode(row.getString(DeviceEntityColumns.DEGRADED));
      entity.setHubLocal(row.getBool(DeviceEntityColumns.HUBLOCAL));
   }

   private Map<String, Object> toAttributes(Row row, boolean includeTombstoned) {
      if(!acceptRow(row, includeTombstoned)) {
         return null;
      }

      Map<String, String> encoded = row.getMap(NonEntityColumns.ATTRIBUTES, String.class, String.class);
      // decode
      AttributeMap attributes = AttributeMap.newMap();
      for(Map.Entry<String, String> entry: encoded.entrySet()) {
         AttributeKey<?> key = key(entry.getKey());
         if(key == null) {
            continue;
         }
         try {
            Object value = deserialize(key, entry.getValue());
            attributes.add(key.coerceToValue(value));
         }
         catch(Exception e) {
            log.warn("Invalid value [{}] for attribute [{}]", entry.getValue(), entry.getKey());
         }
      }
      Map<String, Object> model = attributes.toMap();

      // base attributes
      setIf(Capability.ATTR_TYPE, DeviceCapability.NAMESPACE, model);
      setIf(Capability.ATTR_ID, getIdFromRow(row).toString(), model);
      setIf(Capability.ATTR_ADDRESS, row.getString(DeviceEntityColumns.DRIVER_ADDRESS), model);
      setOrDefault(Capability.ATTR_TAGS, row.getSet(BaseEntityColumns.TAGS, String.class), ImmutableSet.of(), model);
      setOrDefault(Capability.ATTR_IMAGES, coerceToStringMap(row.getMap(BaseEntityColumns.IMAGES, String.class, UUID.class)), ImmutableMap.of(), model);
      setOrDefault(Capability.ATTR_CAPS, row.getSet(DeviceEntityColumns.CAPS, String.class), ImmutableSet.of(Capability.NAMESPACE, DeviceCapability.NAMESPACE), model);

      // device attributes
      setIf(DeviceCapability.ATTR_DEVTYPEHINT, row.getString(DeviceEntityColumns.DEVTYPEHINT), model);
      setOrDefault(DeviceCapability.ATTR_NAME, row.getString(DeviceEntityColumns.NAME), DFLT_NAME, model);
      setIf(DeviceCapability.ATTR_ACCOUNT, coerceToString(row.getUUID(DeviceEntityColumns.ACCOUNT_ID)), model);
      setIf(DeviceCapability.ATTR_PLACE, coerceToString(row.getUUID(DeviceEntityColumns.PLACE_ID)), model);
      setIf(DeviceCapability.ATTR_MODEL, row.getString(DeviceEntityColumns.MODEL), model);
      setIf(DeviceCapability.ATTR_VENDOR, row.getString(DeviceEntityColumns.VENDOR), model);
      setIf(DeviceCapability.ATTR_PRODUCTID, row.getString(DeviceEntityColumns.PRODUCTID), model);

      // device advanced attributes
      setIf(DeviceAdvancedCapability.ATTR_ADDED, row.getTimestamp(BaseEntityColumns.CREATED), model);
      setIf(DeviceAdvancedCapability.ATTR_DRIVERNAME, row.getString(DeviceEntityColumns.DRIVER_NAME), model);

      String version = row.getString(DeviceEntityColumns.DRIVER_VERSION2);
      if(version == null) {
         ColumnRepairMetrics.incDriverVersionCounter();
         version = row.getString(DeviceEntityColumns.DRIVER_VERSION);
      }

      String driverState = row.getString(DeviceEntityColumns.STATE);
      try {
         // NOTE the coerce handles the fact that the enum is upper case, but historically the values in the db are lower case
         setIf(DeviceAdvancedCapability.ATTR_DRIVERSTATE, DeviceAdvancedCapability.KEY_DRIVERSTATE.coerceToValue(driverState).getValue(), model);
      }
      catch(Exception e) {
         log.warn("Invalid value [{}] for attribute [{}]", driverState, DeviceAdvancedCapability.ATTR_DRIVERSTATE);
      }
      setIf(DeviceAdvancedCapability.ATTR_DRIVERVERSION, version, model);
      setIf(DeviceAdvancedCapability.ATTR_PROTOCOL, row.getString(DeviceEntityColumns.PROTOCOL_NAME), model);
      setIf(DeviceAdvancedCapability.ATTR_PROTOCOLID, row.getString(DeviceEntityColumns.PROTOCOL_ID), model);
      setIf(DeviceAdvancedCapability.ATTR_SUBPROTOCOL, row.getString(DeviceEntityColumns.SUBPROTOCOL), model);

      String degraded = row.getString(DeviceEntityColumns.DEGRADED);
      setIf(DeviceAdvancedCapability.ATTR_DEGRADED, degraded != null && !Device.DEGRADED_CODE_NONE.equals(degraded), model);
      setIf(DeviceAdvancedCapability.ATTR_DEGRADEDCODE, degraded, model);

      setOrDefault(DeviceAdvancedCapability.ATTR_HUBLOCAL, row.getBool(DeviceEntityColumns.HUBLOCAL), false, model);

      return model;
   }

   private ModelEntity toModel(Row row, boolean includeTombstoned) {
      if(row == null) {
         return null;
      }

      Map<String, Object> attributes = toAttributes(row, includeTombstoned);
      if(attributes == null) {
         // tombstoned
         return null;
      }

      ModelEntity entity = new ModelEntity(attributes);
      entity.setCreated(row.getTimestamp(BaseEntityColumns.CREATED));
      entity.setModified(row.getTimestamp(BaseEntityColumns.MODIFIED));
      return entity;
   }

   private boolean acceptRow(Row row, boolean includeTombstoned) {
      if(row == null) {
         return false;
      }
      if(includeTombstoned) {
         return true;
      }
      return !Device.STATE_TOMBSTONED.equals(row.getString(DeviceEntityColumns.STATE));
   }

   @Override
   public List<Device> findByHubId(String hubId, boolean includeTombstoned) {
      Preconditions.checkArgument(!StringUtils.isEmpty(hubId), "must specify hubId");

      try (Context timerContext = findByHubIdTimer.time())
      {
         Statement associationQuery = findByHubId.bind(hubId);

         Function<Row, UUID> entityIdTransform =
            row -> row.getUUID("devid");

         Function<ResultSet, Device> entityTransform =
            resultSet -> {
               Row row = resultSet.one();
               return acceptRow(row, includeTombstoned) ? buildEntity(row) : null;
            };

         return listByAssociation(associationQuery, entityIdTransform, entityTransform, asyncTimeoutMs);
      }
   }

   @Override
   public Device findByProtocolAddress(String protocolAddress) {
      BoundStatement boundStatement = new BoundStatement(findByProtocolAddr);
      try(Context ctxt = findByProtocolAddressTimer.time()) {
    	  Row row = session.execute(boundStatement.bind(protocolAddress)).one();

    	  if (row == null) {
    		  return null;
    	  }

    	  UUID deviceId = row.getUUID("id");
    	  return findById(deviceId);
      }
   }

   @Override
   public List<Map<String, Object>> listDeviceAttributesByAccountId(UUID accountId, boolean includeTombstoned) {
      if(accountId == null) {
         return Collections.<Map<String, Object>>emptyList();
      }

      BoundStatement boundStatement = new BoundStatement(findByAccountId);
      ResultSet results;
      try(Context ctxt = listDeviceAttributesByAccountIdTimer.time()) {
    	  results = session.execute(boundStatement.bind(accountId));
      }
      List<Map<String, Object>> devices = new LinkedList<>();
      for(Row row:  results) {
         Map<String, Object> deviceAttributes = toAttributes(row, includeTombstoned);
         if(deviceAttributes != null) {
            devices.add(deviceAttributes);
         }
      }
      return devices;
   }

   @Override
   public List<Map<String, Object>> listDeviceAttributesByPlaceId(UUID placeId, boolean includeTombstoned) {
      try (Context timerContext = listDeviceAttributesByPlaceIdTimer.time())
      {
         if (placeId == null) {
            return Collections.<Map<String, Object>>emptyList();
         }

         Statement associationQuery = findIdsByPlace.bind(placeId);

         Function<Row, UUID> entityIdTransform =
            row -> row.getUUID("devid");

         Function<ResultSet, Map<String, Object>> entityTransform =
            resultSet -> toAttributes(resultSet.one(), includeTombstoned);

         return listByAssociation(associationQuery, entityIdTransform, entityTransform, asyncTimeoutMs);
      }
   }

   @Override
   public List<Device> listDevicesByPlaceId(UUID placeId, boolean includeTombstoned) {
      try (Context timerContext = listDevicesByPlaceIdTimer.time())
      {
         Statement associationQuery = findIdsByPlace.bind(placeId);

         Function<Row, UUID> entityIdTransform =
            row -> row.getUUID("devid");

         Function<ResultSet, Device> entityTransform =
            resultSet -> {
               Row row = resultSet.one();
               return acceptRow(row, includeTombstoned) ? buildEntity(row) : null;
            };

         return listByAssociation(associationQuery, entityIdTransform, entityTransform, asyncTimeoutMs);
      }
   }

   @Override
   public Stream<ModelEntity> streamDeviceModelByPlaceId(UUID placeId, boolean includeTombstoned) {
      if(placeId == null) {
         return Stream.empty();
      }

      try (Context timerContext = streamDeviceModelByPlaceIdTimer.time())
      {
         Statement associationQuery = findIdsByPlace.bind(placeId);

         Function<Row, UUID> entityIdTransform =
            row -> row.getUUID("devid");

         Function<ResultSet, ModelEntity> entityTransform =
            resultSet -> toModel(resultSet.one(), includeTombstoned);

         return listByAssociation(associationQuery, entityIdTransform, entityTransform, asyncTimeoutMs).stream();
      }
   }

   @Override
   public ModelEntity modelById(UUID id) {
      if(id == null) {
         return null;
      }
      try(Context ctxt = modelByIdTimer.time()) {
         BoundStatement stmt = new BoundStatement(findById).bind(id);
         Row r = session.execute(stmt).one();
         if(r == null) {
            return null;
         }
         return toModel(r, true);
      }
   }

   @Override
   protected Device doInsert(UUID id, Device entity) {
      entity.setAddress(Address.platformDriverAddress(id).getRepresentation());

      return super.doInsert(id, entity);
   }

   @Override
   protected List<Statement> prepareIndexInserts(UUID id, Device entity) {
      List<Statement> indexInserts = new ArrayList<>();
      addHubIdIndexInsert(indexInserts, id, entity);
      addProtocolAddressIndexInsert(indexInserts, id, entity);
      addPlaceIndexInsert(indexInserts, id, entity);
      return indexInserts;
   }

   @Override
   protected List<Statement> prepareIndexUpdates(Device entity) {
      Device currentDevice = findById(entity.getId());
      if(currentDevice == null) {
         return prepareIndexInserts(entity.getId(), entity);
      }
      List<Statement> statements = new ArrayList<>();
      if(!StringUtils.equals(entity.getProtocolAddress(), currentDevice.getProtocolAddress())) {
         addProtocolAddressIndexDelete(statements, currentDevice);
         addProtocolAddressIndexInsert(statements, entity.getId(), entity);
      }
      if(!Objects.equal(entity.getPlace(), currentDevice.getPlace())) {
         addPlaceIndexDelete(statements, currentDevice);
         addPlaceIndexInsert(statements, entity.getId(), entity);
      }



      return statements;
   }

   @Override
   protected List<Statement> prepareIndexDeletes(Device entity) {
      log.info("Deleting Device {}", entity);
      List<Statement> indexDeletes = new ArrayList<>();
      addProtocolAddressIndexDelete(indexDeletes, entity);
      addHubIdIndexDelete(indexDeletes, entity);
      addPlaceIndexDelete(indexDeletes, entity);
      return indexDeletes;
   }

   private void addHubIdIndexInsert(List<Statement> statements, UUID id, Device entity) {
      if(!StringUtils.isBlank(entity.getHubId())) {
         statements.add(new BoundStatement(insertHubIndex).bind(entity.getHubId(), id));
      }
   }

   private void addHubIdIndexDelete(List<Statement> statements, Device entity) {
      if(!StringUtils.isBlank(entity.getHubId())) {
         statements.add(new BoundStatement(deleteHubIndex).bind(entity.getHubId(), entity.getId()));
      }
   }

   private void addProtocolAddressIndexInsert(List<Statement> statements, UUID id, Device entity) {
      if(!StringUtils.isBlank(entity.getProtocolAddress())) {
         statements.add(new BoundStatement(insertProtocolAddrIndex).bind(id, entity.getProtocolAddress()));
      }
   }

   private void addProtocolAddressIndexDelete(List<Statement> statements, Device entity) {
      if(!StringUtils.isBlank(entity.getProtocolAddress())) {
         statements.add(new BoundStatement(deleteProtocolAddrIndex).bind(entity.getProtocolAddress()));
      }
   }

   private void addPlaceIndexInsert(List<Statement> statements, UUID id, Device entity) {
      if(entity.getPlace() != null) {
         statements.add(new BoundStatement(insertPlaceIndex).bind(entity.getPlace(), id));
      }
   }

   private void addPlaceIndexDelete(List<Statement> statements, Device entity) {
      if(entity.getPlace() != null) {
         statements.add(new BoundStatement(deletePlaceIndex).bind(entity.getId(), entity.getPlace()));
      }
   }

   @Override
   public DeviceDriverStateHolder loadDriverState(Device device) {
      Preconditions.checkNotNull(device, "device cannot be null");
      Preconditions.checkNotNull(device.getId(), "device must have an id");

      BoundStatement bound = new BoundStatement(loadState);
      Row r;
      try(Context ctxt = loadDriverStateTimer.time()) {
         r = session.execute(bound.bind(device.getId())).one();
      }

      if(r == null) {
         return null;
      }

      Map<String,String> encoded = r.getMap(NonEntityColumns.ATTRIBUTES, String.class, String.class);
      AttributeMap attributes = AttributeMap.newMap();
      for(Map.Entry<String, String> entry: encoded.entrySet()) {
         AttributeKey<?> key = key(entry.getKey());
         if(key == null) {
            continue;
         }
         Object value = deserialize(key, entry.getValue());
         // this shouldn't be necessary...
         attributes.add(key.coerceToValue(value));
      }


      Map<String,Object> variables = new HashMap<>();
      ByteBuffer buf = r.getBytes(NonEntityColumns.VARIABLES);
      if (buf != null) {
         variables = SerializationUtils.deserialize(Bytes.getArray(buf));
      }

      return new DeviceDriverStateHolder(attributes, variables);
   }

   @Override
   public void replaceDriverState(Device device, DeviceDriverStateHolder state) {
      try(Timer.Context ctx = replaceDriverStateTimer.time()) {
         executeStateUpdate(device, state, true);
      }
   }

   @Override
   public void updateDriverState(Device device, DeviceDriverStateHolder state) {
      try(Timer.Context ctx = updateDriverStateTimer.time()) {
         executeStateUpdate(device, state, false);
      }
   }

   @SuppressWarnings("rawtypes")
   @Override
   public void removeAttributes(Device device, Collection<AttributeKey> attributeKeys) {
      Preconditions.checkNotNull(device, "device cannot be null");
      Preconditions.checkNotNull(device.getId(), "device must have an id");

      Delete.Selection deleteSelection = QueryBuilder.delete();

      attributeKeys.stream()
         .filter(key -> !isStrictColumn(key))
         .forEach((key) -> { deleteSelection.mapElt(NonEntityColumns.ATTRIBUTES, key.getName()); });

      Delete delete = deleteSelection.from(TABLE);
      delete.where(eq(BaseEntityColumns.ID, device.getId()));
      try(Context ctxt = removeAttributesTimer.time()) {
         session.execute(delete);
      }
   }

   @Override
   protected UUID getIdFromRow(Row row) {
      return row.getUUID(BaseEntityColumns.ID);
   }

   @Override
   protected UUID nextId(Device device) {
      return device.getId() != null ? device.getId() : UUID.randomUUID();
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

   private void executeStateUpdate(Device device, DeviceDriverStateHolder state, boolean replace) {
      Preconditions.checkNotNull(device, "device cannot be null");
      Preconditions.checkNotNull(device.getId(), "device must have an id");

      Map<String,String> attributesAsStrings = new HashMap<>();
      Update update = QueryBuilder.update(TABLE);
      update.where(eq(BaseEntityColumns.ID, device.getId()));

      // allow entries defined in ATTR_TO_COLUMN_MAP to be
      // edited here, however this call is mainly intended for
      // drivers, so any updates to other columns which are not
      // allowed fail fast
      List<Object> values = new ArrayList<>();
      if(state.getAttributes() != null) {
         state.getAttributes().entries().forEach((value) -> {
            AttributeKey<?> attributeKey = value.getKey();
            if(isStrictColumn(attributeKey)) {
               String columnName = ATTR_TO_COLUMN_MAP.get(attributeKey.getName());
               if(columnName == null) {
                  throw new IllegalArgumentException("Attempted to modify core property '" + value.getKey() + "' via update or replace attributes.  This property may not be updated from a driver.");
               }
               Object val = value.getValue();
               if(columnName.equals(BaseEntityColumns.IMAGES)) {
                  val = convertImageMap((Map<String,String>) val);
               }

               update.with(set(columnName, val));
            }
            else {
               if(value.getValue() == null) {
                  if(!replace) {
                     update.with(set(NonEntityColumns.ATTRIBUTES + "[?]", null));
                     values.add(attributeKey.getName());
                  }
               }
               else {
                  attributesAsStrings.put(attributeKey.getName(), serialize(attributeKey, value.getValue()));
               }
            }
         });
      }

      if(state.getVariables().size() > 0) {
         HashMap<String,Object> vars = new HashMap<String,Object>(state.getVariables());
         ByteBuffer buffer = ByteBuffer.wrap(SerializationUtils.serialize(vars));
         update.with(set(NonEntityColumns.VARIABLES, buffer));
      }

      if(replace) {
         update.with(set(NonEntityColumns.ATTRIBUTES, attributesAsStrings));
      } else {
         update.with(putAll(NonEntityColumns.ATTRIBUTES, attributesAsStrings));
      }

      session.execute(update.toString(), values.toArray());
   }

   private Map<String,UUID> convertImageMap(Map<String,String> images) {
      if(images == null) {
         return null;
      }
      Map<String,UUID> imageMap = new HashMap<>();
      for(Map.Entry<String, String> entry : images.entrySet()) {
         imageMap.put(entry.getKey(), UUID.fromString(entry.getValue()));
      }
      return imageMap;
   }

   @SuppressWarnings("rawtypes")
   private boolean isStrictColumn(AttributeKey attributeKey) {
      // instances goes in the untyped attribute bag
      if(Capability.ATTR_INSTANCES.equals(attributeKey.getName())) {
         return false;
      } else if(DeviceAdvancedCapability.ATTR_DRIVERCOMMIT.equals(attributeKey.getName())) {
         return false;
      } else if(DeviceAdvancedCapability.ATTR_DRIVERHASH.equals(attributeKey.getName())) {
         return false;
      } else if(DeviceAdvancedCapability.ATTR_ERRORS.equals(attributeKey.getName())) {
         return false;
      } else if (DeviceAdvancedCapability.ATTR_FIRMWAREVERSION.equals(attributeKey.getName())){
         return false;
      }
      String namespace = attributeKey.getNamespace();
      return Capability.NAMESPACE.equals(namespace) || DeviceCapability.NAMESPACE.equals(namespace) || DeviceAdvancedCapability.NAMESPACE.equals(namespace);
   }

   private boolean setIf(String key, Object value, Map<String, Object> model) {
      if(value == null) {
         return false;
      }
      model.put(key, value);
      return true;
   }

   private <V> void setOrDefault(String key, V value, V defaultValue, Map<String, Object> model) {
      if(value == null) {
         model.put(key, defaultValue);
      }
      else {
         model.put(key, value);
      }
   }

   private String coerceToString(Object value) {
      if(value == null) {
         return null;
      }

      if(value instanceof Date) {
         return String.valueOf(((Date) value).getTime());
      }
      return value.toString();
   }

   private Map<String, String> coerceToStringMap(Map<String, ?> value) {
      if(value == null || value.isEmpty()) {
         return null;
      }

      Map<String, String> result = new HashMap<>(value.size());
      for(Map.Entry<String, ?> entry: value.entrySet()) {
         result.put(entry.getKey(), coerceToString(entry.getValue()));
      }
      return result;
   }

   private byte[] protocolAttributesToBytes(Device entity) {
      return entity.getProtocolAttributes() != null
            ? attributeMapSerializer.serialize(entity.getProtocolAttributes())
            : null;
   }

   private AttributeMap bytesToProtocolAttributes(byte[] bytes) {
      return bytes != null && bytes.length > 0
            ? attributeMapDeserializer.deserialize(bytes)
            : null;
   }

   private String serialize(AttributeKey<?> key, Object value) {
      if(value == null) {
         return null;
      }
      // non-json serialization for primitive types, legacy behavior
      AttributeType type = AttributeTypes.fromJavaType(key.getType());
      if(type == TimestampType.INSTANCE) {
         return String.valueOf(TimestampType.INSTANCE.coerce(value).getTime());
      }
      else if(type instanceof PrimitiveType) {
         return String.valueOf(value);
      }
      else if(type instanceof EnumType) {
         // TODO assert enumerated value?
         return String.valueOf(value);
      }
      else {
         return JSON.toJson(value);
      }
   }

   private Object deserialize(AttributeKey<?> key, String value) {
      if(value == null) {
         return null;
      }
      // non-json serialization for primitive types, legacy behavior
      AttributeType type = AttributeTypes.fromJavaType(key.getType());
      if(
            type instanceof PrimitiveType ||
            type instanceof EnumType
      ) {
         return type.coerce(value);
      }
      else {
         // TODO assert enumerated value?
         return JSON.fromJson(value, TypeMarker.wrap(key.getType()));
      }
   }

}

