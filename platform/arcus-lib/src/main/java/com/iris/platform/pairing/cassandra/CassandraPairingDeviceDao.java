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
package com.iris.platform.pairing.cassandra;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import com.codahale.metrics.Timer.Context;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.iris.capability.definition.DefinitionRegistry;
import com.iris.core.dao.cassandra.CassandraQueryBuilder;
import com.iris.messages.address.Address;
import com.iris.messages.address.DeviceProtocolAddress;
import com.iris.messages.capability.PairingDeviceMockCapability;
import com.iris.messages.errors.NotFoundException;
import com.iris.platform.model.cassandra.BaseModelDao;
import com.iris.platform.pairing.PairingDevice;
import com.iris.platform.pairing.PairingDeviceDao;
import com.iris.platform.pairing.PairingDeviceMock;
import com.iris.population.PlacePopulationCacheManager;

public class CassandraPairingDeviceDao extends BaseModelDao<PairingDevice> implements PairingDeviceDao {
   private final PreparedStatement listByPlace;
   private final PreparedStatement findByPlaceAndProtocolAddress;
   private final PreparedStatement upsertIfSequenceIs;
   private final PreparedStatement updateIf;
   private final PreparedStatement deleteByPlace;
   private final PreparedStatement deleteByPlaceAndProtocolAddress;
   private final PreparedStatement findSequenceIdByPlace;
   private final PlacePopulationCacheManager populationCacheMgr;
   
   @Inject
   public CassandraPairingDeviceDao(DefinitionRegistry registry, Session session, PlacePopulationCacheManager populationCacheMgr) {
      super(registry, session);
      this.listByPlace =
         CassandraQueryBuilder
            .select(PairingDeviceTable.NAME)
            .addColumns(PairingDeviceTable.Column.values())
            .addWhereColumnEquals(PairingDeviceTable.Column.placeId)
            .prepare(session);
      this.findByPlaceAndProtocolAddress = 
         CassandraQueryBuilder
            .select(PairingDeviceTable.NAME)
            .addColumns(PairingDeviceTable.Column.values())
            .addWhereColumnEquals(PairingDeviceTable.Column.placeId)
            .addWhereColumnEquals(PairingDeviceTable.Column.protocolAddress)
            .prepare(session);
      this.upsertIfSequenceIs =
         CassandraQueryBuilder
            .update(PairingDeviceTable.NAME)
            .addColumns(PairingDeviceTable.StaticColumn.IdSequence)
            .addColumns(PairingDeviceTable.Column.sequenceId)
            .addColumns(PairingDeviceTable.Column.attributes)
            .addColumns(PairingDeviceTable.Column.modified)
            .addColumns(PairingDeviceTable.Column.created)
            .addWhereColumnEquals(PairingDeviceTable.Column.placeId)
            .addWhereColumnEquals(PairingDeviceTable.Column.protocolAddress)
            .ifClause(String.format("%s = ?", PairingDeviceTable.StaticColumn.IdSequence.name()))
            .prepare(session);
      this.updateIf =
            CassandraQueryBuilder
               .update(PairingDeviceTable.NAME)
               .addColumns(PairingDeviceTable.Column.attributes)
               .addColumns(PairingDeviceTable.Column.modified)
               .addWhereColumnEquals(PairingDeviceTable.Column.placeId)
               .addWhereColumnEquals(PairingDeviceTable.Column.protocolAddress)
               .ifExists()
               .prepare(session);
      this.deleteByPlace =
         CassandraQueryBuilder
            .delete(PairingDeviceTable.NAME)
            .addWhereColumnEquals(PairingDeviceTable.Column.placeId)
            .prepare(session);
      this.deleteByPlaceAndProtocolAddress =
            CassandraQueryBuilder
               .delete(PairingDeviceTable.NAME)
               .addWhereColumnEquals(PairingDeviceTable.Column.placeId)
               .addWhereColumnEquals(PairingDeviceTable.Column.protocolAddress)
               .prepare(session);
      this.findSequenceIdByPlace =
         CassandraQueryBuilder
            .select(PairingDeviceTable.NAME)
            .addColumn(PairingDeviceTable.StaticColumn.IdSequence)
            .addWhereColumnEquals(PairingDeviceTable.Column.placeId)
            .limit(1)
            .prepare(session);
      this.populationCacheMgr = populationCacheMgr;
   }

   @Override
   public List<PairingDevice> listByPlace(UUID placeId) {
      try(Context ctx = Metrics.listByPlaceTimer.time()) {
         return list( listByPlace.bind(placeId) );
      }
   }

   @Override
   @Nullable
   public PairingDevice findById(UUID placeId, int contextId) {
      return 
         listByPlace(placeId)
            .stream()
            .filter((m) -> contextId == m.getContextId())
            .findAny()
            .orElse(null);
   }

   @Override
   @Nullable
   public PairingDevice findByProtocolAddress(UUID placeId, Address protocolAddress) {
      try(Context ctx = Metrics.findByProtocolAddressTimer.time()) {
         return find( findByPlaceAndProtocolAddress.bind(placeId, protocolAddress.getRepresentation()) ).orElse( null );
      }
   }

   @Override
   public PairingDevice save(PairingDevice entity) throws IllegalArgumentException {
      Preconditions.checkArgument(entity.getPlaceId() != null, "Must specify a place id");
      Preconditions.checkArgument(entity.getProtocolAddress() != null, "Must specify a protocol address");
      entity.setPopulation(populationCacheMgr.getPopulationByPlaceId(entity.getPlaceId()));
      if(!entity.isPersisted()) {
         return insert(entity);
      }
      else {
         return update(entity);
      }
   }

   private PairingDevice insert(PairingDevice entity) {
      try(Context ctx = Metrics.insertTimer.time()) {
         PairingDevice copy = entity.copy();
         copy.setCreated(new Date());
         copy.setModified(copy.getCreated());
         OptionalInt currentId = OptionalInt.of( getCurrentId( entity.getPlaceId() ) );
         // FIXME add a max retry?
         while(currentId.isPresent()) {
            copy.setId(copy.getPlaceId(), currentId.getAsInt());
            currentId = tryInsert(currentId.getAsInt(), copy);
         }
         return copy;
      }
   }

   // package scope for testing
   OptionalInt tryInsert(int currentId, PairingDevice copy) {
      BoundStatement bs = upsertIfSequenceIs.bind(
            currentId + 1, // update idSequence to next value
            currentId,     // set this row's id to the current id
            encode( copy.getAttributes() ), 
            copy.getModified(), 
            copy.getCreated(), 
            copy.getPlaceId(), 
            copy.getProtocolAddress().getRepresentation(), 
            currentId > 0 ? currentId : null
      );
      ResultSet rs = session().execute( bs );
      if(!rs.wasApplied()) {
         // assume failures are do to idSequence having changed, get the new value to allow retries
         return OptionalInt.of( rs.one().getInt( PairingDeviceTable.StaticColumn.IdSequence.name() ) );
      }
      else {
         return OptionalInt.empty();
      }
   }

   private int getCurrentId(UUID placeId) {
      ResultSet rs = session().execute( findSequenceIdByPlace.bind( placeId ) );
      return
         Optional
            .ofNullable( rs.one() )
            .map( (row) -> row.getInt(PairingDeviceTable.StaticColumn.IdSequence.name()) )
            .orElse( 0 );
   }

   private PairingDevice update(PairingDevice entity) {
      Preconditions.checkState(entity.getId() != null, "Attempting to update a pairing device model with a create date but no secondary id");
      try(Context ctx = Metrics.updateTimer.time()) {
         Date modified = new Date();
         BoundStatement bs = updateIf.bind( encode( entity.getAttributes() ), modified, entity.getPlaceId(), entity.getProtocolAddress().getRepresentation() );
         if(!session().execute( bs ).wasApplied()) {
            throw new NotFoundException(entity.getAddress());
         }
         PairingDevice copy = entity.copy();
         copy.setModified(modified);
         return copy;
      }
   }

   @Override
   public void deleteByPlace(UUID placeId) {
      try(Context ctx = Metrics.deleteByPlaceTimer.time()) {
         session().execute( deleteByPlace.bind( placeId ) );
      }
   }

   @Override
   public void delete(PairingDevice model) {
      try(Context ctx = Metrics.deleteTimer.time()) {
         session().execute( deleteByPlaceAndProtocolAddress.bind(model.getPlaceId(), model.getProtocolAddress().getRepresentation()) );
      }
   }

   @Override
   protected PairingDevice toModel(Row row) {
      // strange side-effect of using a static column is that there is a special row with a null protocoladdress that holds static value when all other rows have been deleted
      if(row.isNull(PairingDeviceTable.Column.protocolAddress.name())) {
         return null;
      }

      Map<String, Object> attributes = decode( row.getMap(PairingDeviceTable.Column.attributes.name(), String.class, String.class) );
      // construct with attributes so those fields aren't marked as dirty
      // the existence of the mock attribute indicates this is a mock or not
      PairingDevice entity = attributes.containsKey(PairingDeviceMockCapability.ATTR_TARGETPRODUCTADDRESS) ? new PairingDeviceMock(attributes) : new PairingDevice(attributes);
      entity.setId( row.getUUID(PairingDeviceTable.Column.placeId.name()), row.getInt(PairingDeviceTable.Column.sequenceId.name()) );
      entity.setProtocolAddress( (DeviceProtocolAddress) Address.fromString( row.getString(PairingDeviceTable.Column.protocolAddress.name()) ) );
      entity.setModified( row.getTimestamp(PairingDeviceTable.Column.modified.name()) );
      entity.setCreated( row.getTimestamp(PairingDeviceTable.Column.created.name()) );
      entity.setPopulation(populationCacheMgr.getPopulationByPlaceId(entity.getPlaceId()));
      return entity;
   }

}

