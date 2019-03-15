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
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.policies.RetryPolicy;
import com.google.common.base.Preconditions;

/**
 *
 */
public abstract class CassandraQueryBuilder<B extends CassandraQueryBuilder<B>> {
   protected String table;
   protected List<String> columns = new ArrayList<>();
   protected List<String> whereColumns = new ArrayList<>();
   protected String setString;
   protected String whereClause;
   protected String ifClause;
   protected String limitClause;
   protected ConsistencyLevel consistency = ConsistencyLevel.LOCAL_QUORUM;
   protected RetryPolicy retryPolicy;
   protected boolean usingTimestamp = false;
   protected boolean allowFiltering = false;
   private CassandraPreparedBuilder preparedBuilder = CassandraPreparedBuilder.STANDARD;
   
   CassandraQueryBuilder(String table) {
      this.table = table;
   }

   public static CassandraInsertBuilder insert(String table) {
      return new CassandraInsertBuilder(table);
   }

   public static CassandraUpdateBuilder update(String table) {
      return new CassandraUpdateBuilder(table);
   }

   public static CassandraSelectBuilder select(String table) {
      return new CassandraSelectBuilder(table);
   }

   public static CassandraDeleteBuilder delete(String table) {
      return new CassandraDeleteBuilder(table);
   }

   @SuppressWarnings("unchecked")
   protected B ths() {
      return (B) this;
   }

   public String getTable() {
      return table;
   }

   protected void appendColumnNames(StringBuilder sb) {
      appendColumnNames(sb,columns);
   }

   protected void appendColumnEquals(StringBuilder sb, String separator) {
      appendColumnEquals(sb,separator,columns);
   }

   protected boolean hasColumnNames() {
      return columns.isEmpty();
   }

   protected int getColumnCount() {
      return columns.size();
   }

   protected void appendWhereClause(StringBuilder sb) {
   	if (!whereColumns.isEmpty() || !StringUtils.isEmpty(whereClause)) {
   		sb.append(" WHERE ");
   		if (!whereColumns.isEmpty()) {
   			appendColumnEquals(sb, " AND ", whereColumns);
   			if (!StringUtils.isEmpty(whereClause)) {
   				sb.append(" AND ");
   				sb.append(whereClause);
   			}
   		}
   		else {
   			sb.append(whereClause);
   		}
   	}
      else if(whereClause != null) {
         sb.append(" WHERE ").append(whereClause);
      }
   }
   
   protected void appendSetClause(StringBuilder sb) {
   	if (!columns.isEmpty() || !StringUtils.isEmpty(setString)) {
   		sb.append(" SET ");
   		if (!columns.isEmpty()) {
   			appendColumnEquals(sb, ", ", columns);
   			if (!StringUtils.isEmpty(setString)) {
   				sb.append(", ");
   				sb.append(setString);
   			}
   		}
   		else {
   			sb.append(setString);
   		}
   	}
   }
   
   protected void appendLimitClause(StringBuilder sb) {
      if (limitClause != null && !limitClause.isEmpty()) {
         sb.append(" LIMIT " + limitClause );
      }
   }
   
   protected void appendIfClause(StringBuilder sb) {
      if(ifClause == null) {
         return;
      }

      sb.append(" IF ").append(ifClause);
   }
   
   protected void appendTtlClause(StringBuilder sb, long ttlSec) {
      if(ttlSec < 0) {
         return;
      }
      
      sb.append(" USING TTL ").append(ttlSec);
   }
   
   protected void appendUsingTimestamp(StringBuilder sb, long ttlSec) {
   	if(!usingTimestamp) {
   		return;
   	}
   	
   	if(ttlSec < 0) {
   		sb.append(" USING TIMESTAMP ?");
   	}
   	else {
   		sb.append(" AND TIMESTAMP ?");
   	}
   	
   }
   
   protected void appendAllowFiltering(StringBuilder sb) {
   	if(!allowFiltering) {
   		return;
   	}else{
   		sb.append(" ALLOW FILTERING");
   	}
   }

   public B usePreparedStatementCache() {
      this.preparedBuilder = CassandraPreparedBuilder.CACHED;
      return ths();
   }

   public B addColumn(String column) {
      this.columns.add(column);
      return ths();
   }

   public B addColumn(Enum<?> column) {
      this.columns.add(column.name());
      return ths();
   }

   public B addMapColumn(String mapColumn) {
      this.columns.add(mapColumn + "[?]");
      return ths();
   }

   public B addColumns(Collection<String> columns) {
      this.columns.addAll(columns);
      return ths();
   }

   public B addColumnsEnum(Collection<Enum<?>> columns) {
      this.columns.addAll(columns.stream().map(Enum::name).collect(Collectors.toList()));
      return ths();
   }

   public B addColumns(String... columns) {
      for(String column: columns) this.columns.add(column);
      return ths();
   }

   public B addColumns(Enum<?>... columns) {
      for(Enum<?> column: columns) this.columns.add(column.name());
      return ths();
   }

   public B where(String clause) {
      whereClause = clause;
      return ths();
   }
   
   public B boundLimit() {
   	limitClause = "?";
   	return ths();
   }
   
   public B limit(String clause) {
      limitClause = clause;
      return ths();
   }
   
   public B limit(int limit) {
      return limit(Integer.toString(limit));
   }
   
   public B addWhereColumnEquals(String whereColumn) {
      Preconditions.checkArgument(whereClause == null, "Can't specify both whereColumns and where clause");
      whereColumns.add(whereColumn);
      return ths();
   }
   
   public B addWhereColumnEquals(Enum<?> whereColumn) {
      Preconditions.checkArgument(whereClause == null, "Can't specify both whereColumns and where clause");
      whereColumns.add(whereColumn.name());
      return ths();
   }
   
   public B withConsistencyLevel(ConsistencyLevel consistency) {
      this.consistency = consistency;
      return ths();
   }
   
   public B withDefaultRetryPolicy() {
      this.retryPolicy = null;
      return ths();
   }
   
   public B withRetryPolicy(RetryPolicy retryPolicy) {
      this.retryPolicy = retryPolicy;
      return ths();
   }
   
   public B usingTimestamp() {
   	this.usingTimestamp = true;
   	return ths();
   }
   
   public B allowFiltering() {
   	this.allowFiltering = true;
   	return ths();
   }
   
   public abstract StringBuilder toQuery();
   
   public PreparedStatement prepare(Session session) {
      return preparedBuilder.prepare(session, toQuery().toString(), consistency, retryPolicy);
   }

   public ResultSet execute(Session session, Object... values) {
      return session.execute( prepare(session).bind(values) );
   }

   public static class CassandraInsertBuilder extends CassandraQueryBuilder<CassandraInsertBuilder> {
      private long ttlSec = -1;

      CassandraInsertBuilder(String table) {
         super(table);
      }
      
      public CassandraInsertBuilder ifNotExists() {
         ifClause = "NOT EXISTS";
         return ths();
      }
      
      public CassandraInsertBuilder withTtlSec(long ttlSec) {
         this.ttlSec = ttlSec;
         return this;
      }

      public StringBuilder toQuery() {
         StringBuilder sb = new StringBuilder("INSERT INTO ").append(getTable()).append(" (");

         appendColumnNames(sb);

         sb.append(") VALUES (");

         int count = getColumnCount();
         for(int i = 0; i < count; i++) {
            sb.append("?");
            if(i < count - 1) { sb.append(", "); }
         }
         sb.append(")");

         // used for transactions
         appendWhereClause(sb);
         appendIfClause(sb);
         appendTtlClause(sb, ttlSec);
         appendUsingTimestamp(sb, ttlSec);
         
         return sb;
      }

   }

   public static class CassandraUpdateBuilder extends CassandraQueryBuilder<CassandraUpdateBuilder> {
      private long ttlSec = -1;
      
      CassandraUpdateBuilder(String table) {
         super(table);
      }
      
      public CassandraUpdateBuilder set(String clause) {
      	Preconditions.checkArgument(setString == null, "Only one set clause may be specified");
      	setString = clause;
      	return ths();
      }
      
      public CassandraUpdateBuilder ifClause(String clause) {
         Preconditions.checkArgument(ifClause == null, "Only one if clause may be specified");
         ifClause = clause;
         return ths();
      }
      
      public CassandraUpdateBuilder withTtlSec(long ttlSec) {
         this.ttlSec = ttlSec;
         return this;
      }
      
      public CassandraUpdateBuilder ifExists() {
         ifClause = "EXISTS";
         return this;
      }   
      
      public StringBuilder toQuery() {
         StringBuilder sb = new StringBuilder("UPDATE ").append(getTable());
         appendTtlClause(sb, ttlSec);
         appendUsingTimestamp(sb, ttlSec);
         appendSetClause(sb);

         appendWhereClause(sb);
         appendIfClause(sb);
         return sb;
      }

   }

   public static class CassandraSelectBuilder extends CassandraQueryBuilder<CassandraSelectBuilder> {
      CassandraSelectBuilder(String table) {
         super(table);
      }

      public StringBuilder toQuery() {
         StringBuilder sb = new StringBuilder("SELECT ");
         if(hasColumnNames()) {
            sb.append("*");
         }
         else {
            appendColumnNames(sb);
         }
         sb.append(" FROM ").append(getTable());
         appendWhereClause(sb);
         appendLimitClause(sb);
         appendAllowFiltering(sb);
         return sb;
      }

   }

   public static class CassandraDeleteBuilder extends CassandraQueryBuilder<CassandraDeleteBuilder> {
      CassandraDeleteBuilder(String table) {
         super(table);
      }
      
      public CassandraDeleteBuilder ifExists() {
         Preconditions.checkArgument(ifClause == null, "Only one if clause may be specified");
         return ifClause("EXISTS");
      }
      
      public CassandraDeleteBuilder ifClause(String clause) {
         Preconditions.checkArgument(ifClause == null, "Only one if clause may be specified");
         ifClause = clause;
         return ths();
      }
      
      public StringBuilder toQuery() {
         StringBuilder sb = new StringBuilder("DELETE ");
         // This provides support for deletion of individual entries from map columns
         appendColumnNames(sb);
         if (!columns.isEmpty())
         {
            sb.append(" ");
         }
         sb.append("FROM ").append(getTable());
         appendWhereClause(sb);
         appendIfClause(sb);
         appendUsingTimestamp(sb, -1);
         return sb;
      }
   }

   protected static void appendColumnNames(StringBuilder sb, List<String> columnNames) {
      int size = columnNames.size();
      for(int i = 0; i < size; i++) {
         sb.append(columnNames.get(i));
         if(i < size - 1) { sb.append(", "); }
      }
   }

   protected static void appendColumnEquals(StringBuilder sb, String separator, List<String> columnNames) {
      int size = columnNames.size();
      for(int i = 0; i < size; i++) {
         sb.append(columnNames.get(i)).append(" = ?");
         if(i < size - 1) { sb.append(separator); }
      }
   }

}

