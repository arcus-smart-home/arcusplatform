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
/**
 *
 */
package com.iris.platform.scheduler.cassandra;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.capability.definition.DefinitionRegistry;
import com.iris.core.dao.cassandra.CassandraQueryBuilder;
import com.iris.core.dao.cassandra.CassandraQueryBuilder.CassandraUpdateBuilder;
import com.iris.core.dao.exception.DaoException;
import com.iris.core.dao.metrics.DaoMetrics;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.SchedulerCapability;
import com.iris.messages.capability.WeeklyScheduleCapability;
import com.iris.messages.errors.NotFoundException;
import com.iris.messages.model.serv.SchedulerModel;
import com.iris.messages.model.serv.WeeklyScheduleModel;
import com.iris.platform.model.ModelEntity;
import com.iris.platform.model.cassandra.BaseModelDao;
import com.iris.platform.scheduler.SchedulerModelDao;
import com.iris.platform.scheduler.cassandra.SchedulerAddressIndex.Relationship;
import com.iris.platform.scheduler.cassandra.SchedulerTable.Columns;
import com.iris.util.IrisCollections;

/**
 *
 */
@Singleton
public class CassandraSchedulerModelDao extends BaseModelDao implements SchedulerModelDao {
   static final Logger logger = LoggerFactory.getLogger(CassandraSchedulerModelDao.class);

   @Inject(optional=true)
   @Named("cassandra.scheduler.asyncTimeoutMs")
   private long asyncTimeoutMs = 30000;

   private final SchedulerMetrics metrics = new SchedulerMetrics();

   private final PreparedStatement listByAddress;
   private final PreparedStatement findById;
   private final PreparedStatement insert;
   private final PreparedStatement deleteById;

   private final PreparedStatement insertTargetIndex;
   private final PreparedStatement addToPlaceIndex;
   private final PreparedStatement deleteTargetIndex;
   private final PreparedStatement removeFromPlaceIndex;

   /**
    *
    */
   @Inject
   public CassandraSchedulerModelDao(
         DefinitionRegistry registry,
         CqlSession session
   ) {
      super(registry, session);
      this.listByAddress =
            CassandraQueryBuilder
               .select(SchedulerAddressIndex.NAME)
               .addColumns(SchedulerAddressIndex.Columns.ALL)
               .addWhereColumnEquals(SchedulerAddressIndex.Columns.ADDRESS)
               .prepare(session);
      this.findById =
            CassandraQueryBuilder
               .select(SchedulerTable.NAME)
               .addColumns(SchedulerTable.Columns.ALL)
               .addWhereColumnEquals(Columns.ID)
               .prepare(session);
      this.insert =
              CassandraQueryBuilder
                 .insert(SchedulerTable.NAME)
                 .addColumns(
                       Columns.CREATED,
                       Columns.MODIFIED,
                       Columns.ATTRIBUTES,
                       Columns.PLACE_ID,
                       Columns.TARGET,
                       Columns.ID
                 )
                 .ifNotExists()
                 .prepare(session);
      this.deleteById =
            CassandraQueryBuilder
               .delete(SchedulerTable.NAME)
               .addWhereColumnEquals(Columns.ID)
               .prepare(session);

      this.insertTargetIndex =
            CassandraQueryBuilder
               .insert(SchedulerAddressIndex.NAME)
               .addColumns(
                     SchedulerAddressIndex.Columns.ADDRESS,
                     SchedulerAddressIndex.Columns.SCHEDULER_IDS,
                     SchedulerAddressIndex.Columns.RELATIONSHIP
               )
               .ifNotExists()
               .prepare(session);
      this.deleteTargetIndex =
            CassandraQueryBuilder
               .delete(SchedulerAddressIndex.NAME)
               .addWhereColumnEquals(SchedulerAddressIndex.Columns.ADDRESS)
               .prepare(session);

      this.addToPlaceIndex =
            session.prepare(
                  "UPDATE " + SchedulerAddressIndex.NAME + " " +
                  "SET " + SchedulerAddressIndex.Columns.RELATIONSHIP + " = '" + Relationship.REFERENCE + "', " +
                        SchedulerAddressIndex.Columns.SCHEDULER_IDS + " = " + SchedulerAddressIndex.Columns.SCHEDULER_IDS + " + ? " +
                  "WHERE " + SchedulerAddressIndex.Columns.ADDRESS + " = ?"
            );
      this.removeFromPlaceIndex =
            session.prepare(
                  "UPDATE " + SchedulerAddressIndex.NAME + " " +
                  "SET " + SchedulerAddressIndex.Columns.SCHEDULER_IDS + " = " + SchedulerAddressIndex.Columns.SCHEDULER_IDS + " - ? " +
                  "WHERE " + SchedulerAddressIndex.Columns.ADDRESS + " = ?"
            );
   }

   /* (non-Javadoc)
    * @see com.iris.platform.subsystem.SubsystemDao#listByPlace(java.util.UUID)
    */
   @Override
   public List<ModelEntity> listByPlace(UUID placeId, boolean includeWeekdays) {
      try(Context cx = SchedulerMetrics.listByPlaceTimer.time()) {
         String placeAddress = Address.platformService(placeId, PlaceCapability.NAMESPACE).getRepresentation();
         ResultSet rs = session().execute( listByAddress.bind(placeAddress) );
         Row row = rs.one();
         if(row == null) {
            return ImmutableList.of();
         }

         Set<UUID> schedulerIds = row.getSet(SchedulerAddressIndex.Columns.SCHEDULER_IDS, UUID.class);
         if(schedulerIds == null || schedulerIds.isEmpty()) {
            return ImmutableList.of();
         }

         List<CompletionStage<AsyncResultSet>> resultFutures = new ArrayList<>(schedulerIds.size());
         for(UUID schedulerId: schedulerIds) {
            CompletionStage<AsyncResultSet> resultFuture = session().executeAsync( findById.bind(schedulerId) );
            resultFutures.add(resultFuture);
         }

         List<ModelEntity> models = new ArrayList<>(schedulerIds.size());
         List<Throwable> errors = new ArrayList<>();
         CompletableFuture<?>[] allFutures = resultFutures.stream()
               .map(cf -> cf.thenApply(asyncRs -> toModel(asyncRs.one(), includeWeekdays))
                     .toCompletableFuture()
                     .thenAccept(model -> { if(model != null) synchronized(models) { models.add(model); } })
                     .exceptionally(ex -> { synchronized(errors) { errors.add(ex); } return null; }))
               .toArray(CompletableFuture[]::new);

         try {
            CompletableFuture.allOf(allFutures).get(asyncTimeoutMs, TimeUnit.MILLISECONDS);
         }
         catch(TimeoutException e) {
            throw new DaoException("Request timed out", e);
         }
         catch (InterruptedException e) {
            throw new DaoException(e);
         }
         catch (ExecutionException e) {
            throw new DaoException(e.getCause());
         }
         return models;
      }
   }

   /* (non-Javadoc)
    * @see com.iris.platform.subsystem.SubsystemDao#findByAddress(com.iris.messages.address.Address)
    */
   @Override
   @Nullable
   public ModelEntity findByAddress(Address address) {
      if(SchedulerCapability.NAMESPACE.equals(address.getGroup())) {
         try(Context cx = SchedulerMetrics.findByIdTimer.time()) {
            return findById((UUID) address.getId());
         }
      }

      try(Context cx = SchedulerMetrics.findByAddressTimer.time()) {
         Row row = session().execute( listByAddress.bind(address.getRepresentation()) ).one();
         if(row == null) {
            return null;
         }

         Set<UUID> schedulerIds = row.getSet(SchedulerAddressIndex.Columns.SCHEDULER_IDS, UUID.class);
         UUID schedulerId = Iterables.getFirst(schedulerIds, null);
         if(schedulerId == null) {
            return null;
         }

         return findById(schedulerId);
      }
   }

   /* (non-Javadoc)
    * @see com.iris.platform.scheduler.SchedulerDao#findOrCreateByAddress(com.iris.messages.address.Address)
    */
   @Override
   public ModelEntity findOrCreateByTarget(UUID placeId, Address targetAddress) {
	  try(Context cx = SchedulerMetrics.findOrCreateByTargetTimer.time()) {
		  Preconditions.checkArgument(!PlaceCapability.NAMESPACE.equals(targetAddress.getGroup()), "Places are not currently schedulable");
	      Preconditions.checkArgument(!SchedulerCapability.NAMESPACE.equals(targetAddress.getGroup()), "Target address may not be a scheduler");
	      String placeAddress =
	            Address
	               .platformService(placeId, PlaceCapability.NAMESPACE)
	               .getRepresentation();

	      UUID modelId = UUID.randomUUID();
	      UUID id = insertIndices(modelId, targetAddress.getRepresentation(), placeAddress);
	      if(id != null) {
	         modelId = id;
	      }

	      Date timestamp = new Date();
	      Map<String, Object> attributes =
	            IrisCollections
	               .<String, Object>map()
	               .put(Capability.ATTR_ID, modelId.toString())
	               .put(Capability.ATTR_ADDRESS, Address.platformService(modelId, SchedulerCapability.NAMESPACE).getRepresentation())
	               .put(Capability.ATTR_TYPE, SchedulerCapability.NAMESPACE)
	               .put(Capability.ATTR_CAPS, ImmutableSet.of(Capability.NAMESPACE, SchedulerCapability.NAMESPACE))
	               .put(Capability.ATTR_INSTANCES, ImmutableMap.of())
	               .put(Capability.ATTR_TAGS, ImmutableSet.of())
	               .put(SchedulerCapability.ATTR_PLACEID, placeId.toString())
	               .put(SchedulerCapability.ATTR_TARGET, targetAddress.getRepresentation())
	               .create();

	      ModelEntity model = new ModelEntity(attributes);
	      model.setCreated(timestamp);
	      model.setModified(timestamp);
	      boolean inserted = insertModel(model, placeId, modelId);
	      if(!inserted) {
	    	  model = findById(modelId);
	      }
	      return model;
	  }

   }

   /* (non-Javadoc)
    * @see com.iris.platform.subsystem.SubsystemDao#save(com.iris.messages.model.subs.SubsystemModel)
    */
   @Override
   public ModelEntity save(ModelEntity model) {
      Date modified = new Date();
      ModelEntity rval = new ModelEntity(model);
      if(model.isPersisted()) {
         rval.setModified(modified);

         // copy doesn't track dirty, so pass in orginal
         try(Context cx = SchedulerMetrics.updateTimer.time()) {
            updateModel(model, modified);
         }
      }
      else {
         UUID id = UUID.randomUUID();
         rval.setAttribute(Capability.ATTR_ID, id.toString());
         rval.setAttribute(Capability.ATTR_ADDRESS, Address.platformService(id, SchedulerCapability.NAMESPACE).getRepresentation());
         rval.setCreated(modified);
         rval.setModified(modified);

         try(Context cx = SchedulerMetrics.insertTimer.time()) {
            insert(rval);
         }
         rval.clearDirty();
      }
      return rval;
   }

   /* (non-Javadoc)
    * @see com.iris.platform.subsystem.SubsystemDao#update(java.util.Map)
    */
   @Override
   public void updateAttributes(Address address, Map<String, Object> attributes) {
      if(attributes == null || attributes.isEmpty()) {
         return;
      }

      try(Context cx = SchedulerMetrics.updateAttributesTimer.time()) {
         updateAttributes(address, new Date(), attributes.keySet(), (name) -> attributes.get(name));
      }
   }

   /* (non-Javadoc)
    * @see com.iris.platform.subsystem.SubsystemDao#remove(com.iris.messages.address.Address, java.util.Set)
    */
   @Override
   public void removeAttributes(Address address, Set<String> attributes) {
      if(attributes == null || attributes.isEmpty()) {
         return;
      }

      try(Context cx = SchedulerMetrics.removeAttributesTimer.time()) {
         updateAttributes(address, new Date(), attributes, BaseModelDao::remove);
      }
   }

   /* (non-Javadoc)
    * @see com.iris.platform.subsystem.SubsystemDao#deleteByAddress(com.iris.messages.address.Address)
    */
   @Override
   public void deleteByAddress(Address address) {
      if(PlaceCapability.NAMESPACE.equals(address.getGroup())) {
         deleteByPlace((UUID) address.getId());
      }
      else {
         ModelEntity model = findByAddress(address);
         delete(model);
      }
   }

   @Override
   public void deleteByPlace(UUID placeId) {
      try(Context cx = SchedulerMetrics.deleteByPlaceTimer.time()) {
         for(ModelEntity model: listByPlace(placeId, true)) {
            delete(model);
         }

         String placeAddress =
               Address
                  .platformService(placeId, PersonCapability.NAMESPACE)
                  .getRepresentation();
         session().execute( deleteTargetIndex.bind(placeAddress) );
      }
   }

   @Override
   public void delete(ModelEntity model) {
      try(Context cx = SchedulerMetrics.deleteTimer.time()) {
         UUID modelId = UUID.fromString(model.getId());
         String placeId = SchedulerModel.getPlaceId(model);
         String targetAddress = SchedulerModel.getTarget(model);
         String placeAddress =
               Address
                  .platformService(placeId, PlaceCapability.NAMESPACE)
                  .getRepresentation();

         session().execute( deleteById.bind(modelId) );
         deleteIndices(modelId, targetAddress, placeAddress);
      }
   }

   protected ModelEntity toModel(Row row, boolean includeWeekdays) {
      Map<String, String> attributes = row.getMap(Columns.ATTRIBUTES, String.class, String.class);
      ModelEntity model = new ModelEntity( decode( attributes ) );
      model.setCreated( row.isNull(Columns.CREATED) ? null : Date.from(row.getInstant(Columns.CREATED)) );
      model.setModified( row.isNull(Columns.MODIFIED) ? null : Date.from(row.getInstant(Columns.MODIFIED)) );
      if(!includeWeekdays && model.getInstances() != null) {
    	  //Remove WeeklyScheduleCapability instance data to reduce size of the model
    	  Map<String, Set<String>> instances = model.getInstances();
    	  instances.forEach((k, v) -> {
    		  if(v.contains(WeeklyScheduleCapability.NAMESPACE)) {
    			  WeeklyScheduleModel.setMon(k, model, null);
    		      WeeklyScheduleModel.setTue(k, model, null);
    		      WeeklyScheduleModel.setWed(k, model, null);
    		      WeeklyScheduleModel.setThu(k, model, null);
    		      WeeklyScheduleModel.setFri(k, model, null);
    		      WeeklyScheduleModel.setSat(k, model, null);
    		      WeeklyScheduleModel.setSun(k, model, null);
    		  }
    	  });
      }
      return model;
   }

   protected ModelEntity toModel(Row row) {
      return toModel(row, true);
   }

   protected ModelEntity findById(UUID id) {
      Row row = session().execute( findById.bind(id) ).one();
      if(row == null) {
         return null;
      }

      return toModel(row);
   }

   protected void updateAttributes(
         Address address,
         Date timestamp,
         Set<String> attributes,
         Function<String, Object> provider
   ) {
      List<Object> values = new ArrayList<>(2 * attributes.size() + 3);
      CassandraUpdateBuilder builder =
            CassandraQueryBuilder
               .update(SchedulerTable.NAME)
               .addColumn(Columns.MODIFIED)
               ;
      values.add(timestamp.toInstant());

      attributes.forEach((attribute) -> {
         builder.addColumn(Columns.ATTRIBUTES + "[?]");
         values.add(attribute);
         Object value = provider.apply(attribute);
         if(value == null) {
            values.add(null);
         }
         else {
            values.add( encode(attribute, value) );
         }
      });
      builder
         .addWhereColumnEquals(Columns.ID)
         .ifClause(Columns.CREATED + " != null");

      values.add((UUID) address.getId());
      ResultSet rs = session().execute(builder.toQuery().toString(), values.toArray());
      if(!rs.wasApplied()) {
         throw new NotFoundException(address);
      }
   }

   protected void insert(ModelEntity model) {
      boolean success = false;
      UUID modelId = UUID.fromString(model.getId());
      UUID placeId = UUID.fromString( SchedulerModel.getPlaceId(model) );
      String targetAddress = SchedulerModel.getTarget(model);
      String placeAddress =
            Address
               .platformService(placeId, PlaceCapability.NAMESPACE)
               .getRepresentation();

      if(insertIndices(modelId, targetAddress, placeAddress) != null) {
         throw new DaoException("A scheduler already exists for " + targetAddress);
      }
      try {
         insertModel(model, placeId, modelId);
         success = true;
      }
      finally {
         if(!success) {
            deleteIndices(modelId, targetAddress, placeAddress);
         }
      }
   }

   protected void updateModel(ModelEntity model, Date modified) {
      Set<String> dirtyAttributeNames = model.getDirtyAttributeNames();
      Map<String, Object> updates = model.getUpdatedAttributes();

      Preconditions.checkArgument(!dirtyAttributeNames.contains(SchedulerCapability.ATTR_PLACEID), "Can't change placeId");
      Preconditions.checkArgument(!dirtyAttributeNames.contains(SchedulerCapability.ATTR_TARGET), "Can't change target");

      updateAttributes(
            model.getAddress(),
            modified,
            dirtyAttributeNames,
            (name) -> updates.get(name)
      );
   }

   protected boolean insertModel(
         ModelEntity model,
         UUID placeId,
         UUID modelId
   ) {
      ResultSet rs = session().execute(
            insert.bind(
               model.getCreated().toInstant(),
               model.getModified().toInstant(),
               encode( model.toMap() ),
               placeId,
               SchedulerModel.getTarget(model),
               modelId
            )
         );
      return rs.wasApplied();
   }

   protected UUID insertIndices(UUID modelId, String targetAddress, String placeAddress) {
      ResultSet rs = session().execute( insertTargetIndex.bind(targetAddress, ImmutableSet.of(modelId), Relationship.TARGET.name()) );
      if(!rs.wasApplied()) {
         Set<UUID> ids = rs.one().getSet(SchedulerAddressIndex.Columns.SCHEDULER_IDS, UUID.class);
         if(ids.isEmpty()) {
            // TODO repair this index
            throw new DaoException("Corrupt scheduler index for target: " + targetAddress);
         }
         return ids.iterator().next();
      }
      session().execute( addToPlaceIndex.bind(ImmutableSet.of(modelId), placeAddress) );
      return null;
   }

   protected void deleteIndices(UUID modelId, String targetAddress, String placeAddress) {
      session().execute( deleteTargetIndex.bind(targetAddress) );
      session().execute( removeFromPlaceIndex.bind(ImmutableSet.of(modelId), placeAddress) );
   }

   private static class SchedulerMetrics {
      static final Timer insertTimer = DaoMetrics.insertTimer(SchedulerModelDao.class, "save");
      static final Timer updateTimer = DaoMetrics.updateTimer(SchedulerModelDao.class, "save");
      static final Timer updateAttributesTimer = DaoMetrics.updateTimer(SchedulerModelDao.class, "updateAttributes");
      static final Timer removeAttributesTimer = DaoMetrics.updateTimer(SchedulerModelDao.class, "removeAttributes");
      static final Timer listByPlaceTimer = DaoMetrics.readTimer(SchedulerModelDao.class, "listByPlace");
      static final Timer findByAddressTimer = DaoMetrics.readTimer(SchedulerModelDao.class, "findByAddress");
      static final Timer findByIdTimer = DaoMetrics.readTimer(SchedulerModelDao.class, "findById");
      static final Timer deleteByPlaceTimer = DaoMetrics.deleteTimer(SchedulerModelDao.class, "deleteByPlace");
      static final Timer findOrCreateByTargetTimer = DaoMetrics.upsertTimer(SchedulerModelDao.class, "findOrCreateByTarget");
      static final Timer deleteTimer = DaoMetrics.deleteTimer(SchedulerModelDao.class, "delete");
  }
}
