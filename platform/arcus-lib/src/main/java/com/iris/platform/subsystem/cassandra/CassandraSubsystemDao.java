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
package com.iris.platform.subsystem.cassandra;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.common.base.Function;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.capability.definition.DefinitionRegistry;
import com.iris.core.dao.cassandra.CassandraQueryBuilder;
import com.iris.core.dao.cassandra.CassandraQueryBuilder.CassandraUpdateBuilder;
import com.iris.messages.address.Address;
import com.iris.messages.errors.NotFoundException;
import com.iris.platform.model.ModelEntity;
import com.iris.platform.model.cassandra.BaseModelDao;
import com.iris.platform.subsystem.SubsystemDao;
import com.iris.platform.subsystem.cassandra.SubsystemTable.Columns;

/**
 *
 */
@Singleton
public class CassandraSubsystemDao extends BaseModelDao implements SubsystemDao {
   static final Logger logger = LoggerFactory.getLogger(CassandraSubsystemDao.class);

   private final PreparedStatement findById;
   private final PreparedStatement listByPlace;
   private final PreparedStatement upsert;
   private final PreparedStatement deleteById;
   private final PreparedStatement deleteByPlace;

   /**
    *
    */
   @Inject
   public CassandraSubsystemDao(
         DefinitionRegistry registry,
         Session session
   ) {
      super(registry, session);
      this.findById =
            CassandraQueryBuilder
               .select(SubsystemTable.NAME)
               .addColumns(Columns.ATTRIBUTES, Columns.CREATED, Columns.MODIFIED, Columns.NAMESPACE, Columns.PLACE_ID)
               .addWhereColumnEquals(Columns.PLACE_ID)
               .addWhereColumnEquals(Columns.NAMESPACE)
               .prepare(session);
      this.listByPlace =
            CassandraQueryBuilder
               .select(SubsystemTable.NAME)
               .addColumns(Columns.ATTRIBUTES, Columns.CREATED, Columns.MODIFIED, Columns.NAMESPACE, Columns.PLACE_ID)
               .addWhereColumnEquals(Columns.PLACE_ID)
               .prepare(session);
      this.upsert =
            CassandraQueryBuilder
               .update(SubsystemTable.NAME)
               .addColumns(
                     Columns.CREATED,
                     Columns.MODIFIED,
                     Columns.ATTRIBUTES
               )
               .addWhereColumnEquals(Columns.PLACE_ID)
               .addWhereColumnEquals(Columns.NAMESPACE)
               .prepare(session);
      this.deleteById =
            CassandraQueryBuilder
               .delete(SubsystemTable.NAME)
               .addWhereColumnEquals(Columns.PLACE_ID)
               .addWhereColumnEquals(Columns.NAMESPACE)
               .prepare(session);
      this.deleteByPlace =
            CassandraQueryBuilder
               .delete(SubsystemTable.NAME)
               .addWhereColumnEquals(Columns.PLACE_ID)
               .prepare(session);
   }

   /* (non-Javadoc)
    * @see com.iris.platform.subsystem.SubsystemDao#listByPlace(java.util.UUID)
    */
   @Override
   public List<ModelEntity> listByPlace(UUID placeId) {
      ResultSet rs = session().execute( listByPlace.bind(placeId) );
      return
            StreamSupport
               .stream(rs.spliterator(), false)
               .map((row) -> toModel(row))
               .collect(Collectors.toList())
               ;
   }

   /* (non-Javadoc)
    * @see com.iris.platform.subsystem.SubsystemDao#findByAddress(com.iris.messages.address.Address)
    */
   @Override
   @Nullable
   public ModelEntity findByAddress(Address address) {
      UUID placeId = (UUID) address.getId();
      String namespace = (String) address.getGroup();
      ResultSet rs = session().execute( findById.bind(placeId, namespace) );
      Row row = rs.one();
      if(row == null) {
         return null;
      }
      return toModel(row);
   }

   @Override
   public ModelEntity copyAndSave(ModelEntity model) {
      return copyAndSave(model,null);
   }

   @Override
   public ModelEntity copyAndSave(ModelEntity model, Map<String,Object> failedFromPrevious) {
      Date modified = save(model,failedFromPrevious);
      ModelEntity copy = model.copy();
      if (!copy.isPersisted()) {
         copy.setCreated(modified);
      }

      copy.setModified(modified);
      return copy;
   }

   /* (non-Javadoc)
    * @see com.iris.platform.subsystem.SubsystemDao#save(com.iris.messages.model.subs.SubsystemModel)
    */
   @Override
   public Date save(ModelEntity model) {
      return save(model,null);
   }

   /* (non-Javadoc)
    * @see com.iris.platform.subsystem.SubsystemDao#save(com.iris.messages.model.subs.SubsystemModel,java.util.Map)
    */
   @Override
   public Date save(ModelEntity model, Map<String,Object> failedFromPrevious) {
      Date modified = new Date();
      if(model.isPersisted()) {
         // Examine the failedFromPrevious map to determine if we need to add
         // in some additional updates to the DB operation to correct a
         // previous, temporary failure. If this is the case then we need to
         // make sure that the current dirty values take precedent over the
         // previous values. We also need to be sure here that deletes that
         // have happened in the current model entity take precedent over
         // previously failed changes.
         //
         // The logic is as follows:
         //    * If there are failed previous updates
         //       * Form an updated dirty set that contains all of the
         //         previously failed updates and all of the current
         //         updates.
         //       * Attempt to persist this entire dirty set using the
         //         current model's value if the current model has
         //         marked the attribute as dirty or using the failed
         //         previous value if the current model hasn't marked
         //         it as dirty.
         //       * This will correctly handle the delete case because
         //         the current model will have the attribute marked
         //         dirty and will have a null value for it.
         //    * Otherwise
         //       * Attempt to persist the current set of changes
         if (failedFromPrevious != null && !failedFromPrevious.isEmpty()) {
            Map<String, Object> updates = new HashMap<>((model.getDirtyAttributeNames().size() + failedFromPrevious.size() + 1)*4/3, 0.75f);
            updates.putAll(failedFromPrevious);
            updates.putAll(model.getDirtyAttributes());

            updateAttributes(
                  model.getAddress(),
                  modified,
                  updates.keySet(),
                  (name) -> updates.get(name)
            );
         } else {
            updateAttributes(
                  model.getAddress(),
                  modified,
                  model.getDirtyAttributeNames(),
                  (name) -> model.getAttribute(name)
            );
         }
      }
      else {
         // We don't need to check the failedFromPrevious map here because
         // we are inserting the entire state of the current model entity.
         session().execute(
            upsert.bind(
               modified,
               modified,
               encode(model.toMap()),
               (UUID) model.getAddress().getId(),
               (String) model.getAddress().getGroup()
            )
         );
      }
      return modified;
   }

   /* (non-Javadoc)
    * @see com.iris.platform.subsystem.SubsystemDao#deleteByAddress(com.iris.messages.address.Address)
    */
   @Override
   public void deleteByAddress(Address address) {
      session().execute( deleteById.bind( (UUID) address.getId(), (String) address.getGroup() ) );
   }

   /* (non-Javadoc)
    * @see com.iris.platform.subsystem.SubsystemDao#deleteByPlace(java.util.UUID)
    */
   @Override
   public void deleteByPlace(UUID placeId) {
      session().execute( deleteByPlace.bind(placeId) );
   }

   protected ModelEntity toModel(Row row) {
      Map<String, String> attributes = row.getMap(Columns.ATTRIBUTES, String.class, String.class);
      ModelEntity model = new ModelEntity( decode( attributes ) );
      model.setCreated( row.getDate(Columns.CREATED) );
      model.setModified( row.getDate(Columns.MODIFIED) );
      return model;
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
               .update(SubsystemTable.NAME)
               .addColumn(Columns.MODIFIED)
               .usePreparedStatementCache()
               ;
      values.add(timestamp);

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
      builder.where(Columns.PLACE_ID + "=? AND " + Columns.NAMESPACE + "=? IF " + Columns.CREATED + " != null");
      values.add((UUID) address.getId());
      values.add((String) address.getGroup());
      ResultSet rs = builder.execute(session(), values.toArray());
      if(!rs.wasApplied()) {
         throw new NotFoundException(address);
      }
   }

}

