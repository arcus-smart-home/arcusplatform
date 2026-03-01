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

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.datastax.oss.driver.api.core.cql.Statement;
import com.iris.core.dao.CRUDDao;
import com.iris.core.dao.cassandra.CassandraQueryBuilder.CassandraInsertBuilder;
import com.iris.core.dao.cassandra.CassandraQueryBuilder.CassandraUpdateBuilder;
import com.iris.core.dao.metrics.ColumnRepairMetrics;
import com.iris.core.dao.metrics.DaoMetrics;
import com.iris.messages.model.BaseEntity;
import com.iris.platform.PagedResults;

public abstract class BaseCassandraCRUDDao<I, T extends BaseEntity<I, T>> implements CRUDDao<I, T> {
   private static final Logger logger = LoggerFactory.getLogger(BaseCassandraCRUDDao.class);

   static class BaseEntityColumns {
      final static String ID = "id";
      final static String CREATED = "created";
      final static String MODIFIED = "modified";
      final static String TAGS = "tags";
      final static String IMAGES = "imageMap";
   }

   static final String[] BASE_COLUMN_ORDER = {
      BaseEntityColumns.ID,
      BaseEntityColumns.CREATED,
      BaseEntityColumns.MODIFIED,
      BaseEntityColumns.TAGS,
      BaseEntityColumns.IMAGES
   };

   private final String table;
   private final String[] columns;
   private final String[] readOnlyColumns;
   protected final CqlSession session;
   private final PreparedStatement insert;
   private final PreparedStatement update;
   protected final PreparedStatement findById;
   private final PreparedStatement delete;

   private final Timer findByIdTimer;
   private final Timer insertTimer;
   private final Timer updateTimer;
   private final Timer deleteTimer;
   private final long ttl;	//TTL value in seconds

   protected BaseCassandraCRUDDao(CqlSession session, String table, String[] columns) {
      this(session, table, columns, new String[0]);
   }

   /**
    * Columns should only be the entity specific columns, not those that already exist on the
    * BaseEntity (id, created, modified and tags), which are already handled by this base class.
    *
    * @param session
    * @param table
    * @param columns
    * @param readOnlyColumns
    */
   protected BaseCassandraCRUDDao(CqlSession session, String table, String[] columns, String[] readOnlyColumns) {
	   this(session, table, columns, readOnlyColumns, 0);
   }

   protected BaseCassandraCRUDDao(CqlSession session, String table, String[] columns, String[] readOnlyColumns, long ttl) {
	   this.ttl = ttl;
	   this.findByIdTimer = DaoMetrics.readTimer(getClass(), "findById");
	   this.insertTimer = DaoMetrics.insertTimer(getClass(), "save");
	   this.updateTimer = DaoMetrics.insertTimer(getClass(), "save");
	   this.deleteTimer = DaoMetrics.deleteTimer(getClass(), "delete");

	   this.table = table;
	   this.columns = columns;
	   this.readOnlyColumns = readOnlyColumns;
	   this.session = session;

	   this.insert = prepareInsert();
	   this.update = prepareUpdate();
	   this.findById = prepareFindById();
	   this.delete = prepareDelete();
   }

   private PreparedStatement prepareInsert() {
      CassandraInsertBuilder builder = CassandraQueryBuilder
		   .insert(table)
		   .addColumns(BASE_COLUMN_ORDER)
		   .addColumns(columns);
      if(this.ttl > 0) {
    	  builder.withTtlSec(this.ttl);
      }
	  return builder.prepare(session);
   }

   private PreparedStatement prepareUpdate() {
	   CassandraUpdateBuilder builder =
            CassandraQueryBuilder
               .update(table)
               .addColumn(BaseEntityColumns.MODIFIED)
               .addColumn(BaseEntityColumns.TAGS)
               .addColumn(BaseEntityColumns.IMAGES)
               .addColumns(columns)
               .addWhereColumnEquals(BaseEntityColumns.ID);
	   if(this.ttl > 0) {
    	  builder.withTtlSec(this.ttl);
	   }
	   return builder.prepare(session);
   }

   private PreparedStatement prepareFindById() {
      CassandraQueryBuilder queryBuilder = CassandraQueryBuilder
               .select(table)
               .addColumns(BASE_COLUMN_ORDER)
               .addColumns(columns)
               .addColumns(readOnlyColumns)
               .addWhereColumnEquals(BaseEntityColumns.ID);
      return selectNonEntityColumns(queryBuilder).prepare(session);
   }

   private PreparedStatement prepareDelete() {
      return
            CassandraQueryBuilder
               .delete(table)
               .addWhereColumnEquals(BaseEntityColumns.ID)
               .prepare(session)
               ;
   }

   protected CassandraQueryBuilder selectNonEntityColumns(CassandraQueryBuilder queryBuilder) {
      return queryBuilder;
   }

   @Override
   public T save(T entity) {
      if(entity.getCreated() == null) {
         return doInsert(nextId(entity), entity);
      }
      return doUpdate(entity);
   }

   protected T doInsert(I id, T entity) {
      Date created = new Date();

      List<Object> allValues = new LinkedList<Object>();
      allValues.add(id);
      allValues.add(created.toInstant());
      allValues.add(created.toInstant()); // modified date
      allValues.add(entity.getTags());
      allValues.add(entity.getImages());
      allValues.addAll(getValues(entity));

      Statement<?> statement = insert.bind(allValues.toArray());

      List<Statement<?>> indexInserts = prepareIndexInserts(id, entity);
      if(!indexInserts.isEmpty()) {
         BatchStatementBuilder batch = BatchStatement.builder(DefaultBatchType.LOGGED);
         batch.addStatement((BatchableStatement<?>) statement);
         addToBatch(batch, indexInserts);
         statement = batch.build();
      }

      try(Context ctxt = insertTimer.time()) {
    	  session.execute(statement);
      }

      T copy = entity.copy();
      copy.setId(id);
      copy.setCreated(created);
      copy.setModified(created);
      return copy;
   }

   protected List<Statement<?>> prepareIndexInserts(I id, T entity) {
      return Collections.emptyList();
   }

   protected T doUpdate(T entity) {
      Date modified = new Date();

      List<Object> allValues = new LinkedList<Object>();
      allValues.add(modified.toInstant());
      allValues.add(entity.getTags());
      allValues.add(entity.getImages());
      allValues.addAll(getValues(entity));
      allValues.add(entity.getId());

      Statement<?> statement = update.bind(allValues.toArray());

      // TODO - implement smarter indexing
      List<Statement<?>> indexUpdateStatements = prepareIndexUpdates(entity);
      if(!indexUpdateStatements.isEmpty()) {
         BatchStatementBuilder batch = BatchStatement.builder(DefaultBatchType.LOGGED);
         batch.addStatement((BatchableStatement<?>) statement);
         addToBatch(batch, indexUpdateStatements);
         statement = batch.build();
      }

      try(Context ctxt = updateTimer.time()) {
    	  session.execute(statement);
      }

      T copy = entity.copy();
      copy.setModified(modified);
      return copy;
   }

   protected List<Statement<?>> prepareIndexUpdates(T entity) {
      return Collections.emptyList();
   }

   // TODO push this down to CrudDao?
   protected PagedResults<T> doList(BoundStatement select, int limit) {
      List<T> result = new ArrayList<>(limit);
      select = select.setPageSize(limit + 1);

      ResultSet rs = session.execute( select );
      Row row = rs.one();
      for(int i=0; i<limit && row != null; i++) {
         try {
            T entity = buildEntity(row);
            result.add(entity);
         }
         catch(Exception e) {
            logger.warn("Unable to deserialize row {}", row, e);
         }
         row = rs.one();
      }
      if(row == null) {
         return PagedResults.newPage(result);
      }
      else {
         return PagedResults.newPage(result, String.valueOf(getIdFromRow(row)));
      }
   }

   protected <E> Stream<E> stream(ResultSet rs, Function<Row, E> builder) {
      return
            StreamSupport
               .stream(rs.spliterator(), false)
               .map(builder);
   }

   /**
    * Must be returned in the order of the columns passed into the constructor
    * @return
    */
   protected abstract List<Object> getValues(T entity);

   protected <U> List<U> listByAssociation(Statement<?> associationQuery,
      com.google.common.base.Function<Row, I> entityIdTransform,
      com.google.common.base.Function<ResultSet, U> entityTransform, long asyncTimeoutMs)
   {
      ResultSet associationResultSet = session.execute(associationQuery);

      List<I> entityIds = new ArrayList<>();

      for (Row associationRow : associationResultSet)
      {
         I entityId = entityIdTransform.apply(associationRow);

         entityIds.add(entityId);
      }

      return listByIdsAsync(entityIds, entityTransform, asyncTimeoutMs);
   }

   protected <U> List<U> listByIdsAsync(Iterable<I> ids, com.google.common.base.Function<ResultSet, U> entityTransform,
      long asyncTimeoutMs)
   {
      List<CompletableFuture<U>> entityFutures = new ArrayList<>();

      try
      {
         for (I indexTableRowId : ids)
         {
            BoundStatement entityQuery = findById.bind(indexTableRowId);

            CompletionStage<U> entityFuture = session.executeAsync(entityQuery)
               .thenApply(asyncRs -> {
                  // Convert AsyncResultSet to a single-row ResultSet-like access
                  Row row = asyncRs.one();
                  if (row == null) return null;
                  // We need a ResultSet for the transform function - execute synchronously
                  return entityTransform.apply(session.execute(entityQuery));
               });

            entityFutures.add(entityFuture.toCompletableFuture());
         }

         List<U> results = new ArrayList<>();
         for (CompletableFuture<U> future : entityFutures) {
            try {
               U result = future.get(asyncTimeoutMs, MILLISECONDS);
               if (result != null) {
                  results.add(result);
               }
            } catch (ExecutionException e) {
               // Skip failed futures (matching Futures.successfulAsList behavior)
               logger.warn("Failed to load entity", e.getCause());
            }
         }
         return results;
      }
      catch (InterruptedException e)
      {
         Thread.currentThread().interrupt();

         throw new RuntimeException("Interrupted while executing query", e);
      }
      catch (TimeoutException e)
      {
         for (CompletableFuture<U> future : entityFutures) {
            future.cancel(true);
         }

         throw new RuntimeException("Query timed out", e);
      }
   }

   @Nullable
   @Override
   public T findById(I id) {
      if(id == null) {
         return null;
      }

      BoundStatement boundStatement = findById.bind(id);

      Row row;
    	try(Context ctxt = findByIdTimer.time()) {
    		row = session.execute(boundStatement).one();
    	}

      if(row == null) {
         return null;
      }

      return buildEntity(row);
   }

   @Override
   public void delete(T entity) {
      if(entity == null || entity.getId() == null) {
         return;
      }

      Statement<?> statement = delete.bind(entity.getId());

      List<Statement<?>> indexDeletes = prepareIndexDeletes(entity);
      if(!indexDeletes.isEmpty()) {
         BatchStatementBuilder batch = BatchStatement.builder(DefaultBatchType.LOGGED);
         batch.addStatement((BatchableStatement<?>) statement);
         addToBatch(batch, indexDeletes);
         statement = batch.build();
      }
      try(Context ctxt = deleteTimer.time()) {
    	  session.execute(statement);
      }
   }

   protected List<Statement<?>> prepareIndexDeletes(T entity) {
      return Collections.emptyList();
   }

   protected final T buildEntity(Row row) {
      T entity = createEntity();
      populateBaseEntity(row, entity);
      populateEntity(row, entity);
      return entity;
   }

   protected final void populateBaseEntity(Row row, T entity) {
      entity.setId(getIdFromRow(row));
      Date created = row.isNull(BaseEntityColumns.CREATED) ? null : Date.from(row.getInstant(BaseEntityColumns.CREATED));
      Date modified = row.isNull(BaseEntityColumns.MODIFIED) ? null : Date.from(row.getInstant(BaseEntityColumns.MODIFIED));
      Set<String> tags = row.getSet(BaseEntityColumns.TAGS, String.class);
      if(created == null) {
      	logger.debug("Repairing entity [{}] from [{}] with null created date", entity.getId(), getClass());
      	ColumnRepairMetrics.incCreatedCounter(getClass());
      	created = new Date(0);
      }
      entity.setCreated(created);
      entity.setModified(modified);
      entity.setTags(tags);

      Map<String,UUID> images = row.getMap(BaseEntityColumns.IMAGES, String.class, UUID.class);
      entity.setImages(images.isEmpty() ? null : images);
   }

   protected abstract I getIdFromRow(Row row);

   protected abstract I nextId(T entity);

   /**
    * Instantiate a new instance of the entity
    * @return
    */
   protected abstract T createEntity();

   /**
    * Populate the entity with entity specific data.  The base entity fields are already populated
    * by this implementation.
    *
    * @param row
    * @param entity
    * @return
    */
   protected abstract void populateEntity(Row row, T entity);

   private void addToBatch(BatchStatementBuilder batch, List<Statement<?>> statements) {
      for(Statement<?> statement : statements) {
         batch.addStatement((BatchableStatement<?>) statement);
      }
   }
}
