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

public interface CassandraConstants {

   public final static int CASSANDRA_PORT_DEFAULT = 9042;
   public final static String CASSANDRA_CONTACTPOINTS_DEFAULT = "cassandra.eyeris";
   public final static String CASSANDRA_KEYSPACE_DEFAULT = "dev";
   
   public final static String CASSANDRA_CONTACTPOINTS_PROP = "cassandra.contactPoints";
   public final static String CASSANDRA_KEYSPACE_PROP = "cassandra.keyspace";
   public final static String CASSANDRA_PORT_PROP = "cassandra.port";
   public final static String CASSANDRA_USER_PROP = "cassandra.username";
   public final static String CASSANDRA_PASSWORD_PROP = "cassandra.password";
   public final static String CASSANDRA_COMPRESSION_PROP = "cassandra.compression";
   public final static String CASSANDRA_PROTOVER_PROP = "cassandra.protocol.version";
   public final static String CASSANDRA_SSL_PROP = "cassandra.use.ssl";
   public final static String CASSANDRA_LOADBALANCINGPOLICY = "cassandra.loadbalancingpolicy";

   public final static String CASSANDRA_POOL_CONN_CORE_PROP = "cassandra.pool.connections.core";
   public final static String CASSANDRA_POOL_CONN_MAX_PROP = "cassandra.pool.connections.max";
   public final static String CASSANDRA_POOL_CONN_NEW_PROP = "cassandra.pool.connections.new.threshold";
   public final static String CASSANDRA_POOL_REQMAX_PROP = "cassandra.pool.connections.request.max";
   public final static String CASSANDRA_POOL_IDLE_PROP = "cassandra.pool.idle.timeout";
   public final static String CASSANDRA_POOL_HEARTBEAT_PROP = "cassandra.pool.heartbeat.interval";
   public final static String CASSANDRA_POOL_TIMEOUT_PROP = "cassandra.pool.timeout";

   public final static String CASSANDRA_QUERY_CONSIST_PROP = "cassandra.query.consistency.level";
   public final static String CASSANDRA_QUERY_CONSISTSER_PROP = "cassandra.query.consistency.level.serial";
   public final static String CASSANDRA_QUERY_IDEM_PROP = "cassandra.query.default.idempotent";
   public final static String CASSANDRA_QUERY_FETCH_PROP = "cassandra.query.fetch.size";
   public final static String CASSANDRA_QUERY_PREPARE_ALL_PROP = "cassandra.query.prepare.all.hosts";
   public final static String CASSANDRA_QUERY_PREPARE_UP_PROP = "cassandra.query.prepare.onup";

   public final static String CASSANDRA_X_CONTACTPOINTS_PROP = "cassandra.%s.contactPoints";
   public final static String CASSANDRA_X_KEYSPACE_PROP = "cassandra.%s.keyspace";
   public final static String CASSANDRA_X_PORT_PROP = "cassandra.%s.port";
   public final static String CASSANDRA_X_USER_PROP = "cassandra.%s.username";
   public final static String CASSANDRA_X_PASSWORD_PROP = "cassandra.%s.password";
   public final static String CASSANDRA_X_COMPRESSION_PROP = "cassandra.%s.compression";
   public final static String CASSANDRA_X_PROTOVER_PROP = "cassandra.%s.protocol.version";
   public final static String CASSANDRA_X_SSL_PROP = "cassandra.%s.use.ssl";
   public final static String CASSANDRA_X_LOADBALANCINGPOLICY = "cassandra.%s.loadbalancingpolicy";

   public final static String CASSANDRA_X_POOL_CONN_CORE_PROP = "cassandra.%s.pool.connections.core";
   public final static String CASSANDRA_X_POOL_CONN_MAX_PROP = "cassandra.%s.pool.connections.max";
   public final static String CASSANDRA_X_POOL_CONN_NEW_PROP = "cassandra.%s.pool.connections.new.threshold";
   public final static String CASSANDRA_X_POOL_REQMAX_PROP = "cassandra.%s.pool.connections.requests.max";
   public final static String CASSANDRA_X_POOL_IDLE_PROP = "cassandra.%s.pool.idle.timeout";
   public final static String CASSANDRA_X_POOL_HEARTBEAT_PROP = "cassandra.%s.pool.heartbeat.interval";
   public final static String CASSANDRA_X_POOL_TIMEOUT_PROP = "cassandra.%s.pool.timeout";

   public final static String CASSANDRA_X_QUERY_CONSIST_PROP = "cassandra.%s.query.consistency.level";
   public final static String CASSANDRA_X_QUERY_CONSISTSER_PROP = "cassandra.%s.query.consistency.level.serial";
   public final static String CASSANDRA_X_QUERY_IDEM_PROP = "cassandra.%s.query.default.idempotent";
   public final static String CASSANDRA_X_QUERY_FETCH_PROP = "cassandra.%s.query.fetch.size";
   public final static String CASSANDRA_X_QUERY_PREPARE_ALL_PROP = "cassandra.%s.query.prepare.all.hosts";
   public final static String CASSANDRA_X_QUERY_PREPARE_UP_PROP = "cassandra.%s.query.prepare.onup";

}

