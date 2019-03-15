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
package com.iris.core.dao.cassandra;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.iris.platform.PagedResults;

/**
 * 
 */
public class CassandraQueryExecutor {
   private static final Logger log =
      LoggerFactory.getLogger(CassandraQueryExecutor.class);


   public static <T> List<T> list(ResultSet rs, Function<Row, T> transformer) {
      List<T> results = new ArrayList<T>(rs.getAvailableWithoutFetching());
      for(Row row: rs) {
         T result = transformer.apply(row);
         Preconditions.checkNotNull(result);
         results.add(result);
      }
      return results;
   }

   public static <T> List<T> listOptional(ResultSet rs, Function<Row, Optional<T>> transformer) {
      List<T> results = new ArrayList<T>(rs.getAvailableWithoutFetching());
      for(Row row: rs) {
         Optional<T> result = transformer.apply(row);
         if(result.isPresent()) {
            results.add(result.get());
         }
      }
      return results;
   }
   
   public static <T> PagedResults<T> page(Session session, BoundStatement statement, int limit, Function<Row, T> transformer, String tokenColumn) {
   	return page(session, statement, limit, transformer, (row) -> row.getUUID(tokenColumn).toString());
   }
   
   public static <T> PagedResults<T> page(Session session, BoundStatement statement, int limit, Function<Row, T> transformer, Function<Row, String> token) {
      List<T> results = new ArrayList<>(limit);
      statement.setFetchSize(limit + 1);
      ResultSet rs = session.execute( statement );
      Row row = rs.one();
      while(row != null && results.size() < limit) {
         try {
            T result = transformer.apply(row);
            results.add(result);
         }
         catch(Exception e) {
            log.warn("Unable to deserialize row {}", row, e);
         }
         row = rs.one();
      }
      if(row == null) {
         return PagedResults.newPage(results); 
      }
      else {
         return PagedResults.newPage(results, token.apply(row));
      }
   }

   public static <G,V> Stream<V> streamWithGrouping(ResultSet rs, Function<Row,G> extractor, Function<G,V> factory, BiConsumer<Row,V> aggregator) {
      final GroupingState<G,V> state = new GroupingState<>(factory, aggregator);
      Spliterator<Row> spliterator = Spliterators.spliteratorUnknownSize(rs.iterator(), Spliterator.ORDERED);
      return StreamSupport.stream(spliterator, false).map((Function<Row, V>) (row) -> {
         G group = extractor.apply(row);
         return rs.isExhausted()
            ? state.emitFinal(row, group)
            : state.emitIfNewGroup(row, group);
      }).filter((val) -> val != null);
   }
   
   public static <T> Stream<T> stream(ResultSet rs, Function<Row, T> transformer) {
      return 
            doStream(rs, transformer)
               .peek(Preconditions::checkNotNull);
   }

   public static <T> Stream<T> streamOptional(ResultSet rs, Function<Row, Optional<T>> transformer) {
      return 
            doStream(rs, transformer)
               .filter(Optional::isPresent)
               .map(Optional::get);
               
   }
   
   public static <T> Stream<T> stream(Iterator<Row> rowIterator, Function<Row, T> transformer) {
	  return 
	        doStream(rowIterator, transformer)
	        	.peek(Preconditions::checkNotNull);
	               
   } 

   private static <T> Stream<T> doStream(ResultSet rs, Function<Row, T> transformer) {
	   return doStream(rs.iterator(), transformer);
   }
   
   private static <T> Stream<T> doStream(Iterator<Row> rowIterator, Function<Row, T> transformer) {
      Spliterator<Row> spliterator = Spliterators.spliteratorUnknownSize(rowIterator, Spliterator.ORDERED);
      // TODO enable multi-threaded streaming
      return StreamSupport
               .stream(spliterator, false)
               .map(transformer);
   }

   private static final class GroupingState<G,V> {
      final Function<G,V> factory;
      final BiConsumer<Row,V> aggregator;

      @Nullable G currentGroup = null;
      @Nullable V currentValue = null;

      private GroupingState(Function<G,V> factory, BiConsumer<Row,V> aggregator) {
         this.factory = factory;
         this.aggregator = aggregator;
      }

      @Nullable
      private V emitIfNewGroup(Row row, G group) {
         V result = null;
         if (!Objects.equals(currentGroup,group)) {
            result = currentValue;
            currentGroup = group;
            currentValue = factory.apply(currentGroup);
         }

         aggregator.accept(row,currentValue);
         return result;
      }

      @Nullable
      private V emitFinal(Row row, G group) {
         V result = currentValue;
         if (!Objects.equals(currentGroup,group)) {
            log.warn("extracted group changed on final row, this is not supported and the final value was dropped");
         } else {
            aggregator.accept(row, currentValue);
         }

         return result;
      }
   }
}

