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
package com.iris.platform.notification.audit;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.platform.notification.Notification;
import com.iris.platform.notification.NotificationMethod;

@Singleton
public class CassandraAuditor implements NotificationAuditor {

   public static final String SERVICE_NAME = "cassandra.notification.auditor";
   
    private static final IrisMetricSet METRICS = IrisMetrics.metrics(CassandraAuditor.SERVICE_NAME);
    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraAuditor.class);
    
    private final static String NOTIFICATION_AUDIT_TABLE = "notification_audit";
    private final static String COL_ID = "id";
    private final static String COL_TIME = "time";
    private final static String COL_EVENT_LOG = "eventLog";

    private final Session session;
    private final PreparedStatement upsert;

    private final Timer auditDbTimer = METRICS.timer("audit.db.latency");

    /* 
     * Default Metrics Counters For GMC, APNS, IVR and EMAIL 
     * Using this list makes it easier to test.
     * @see <code>GcmApnsMetricsInitializationTest</code>
     */
    public static final List<String> DEFAULT_METRICS_COUNTERS = 
          ImmutableList.of("audit.event.gcm.failed", 
                           "audit.event.apns.failed",
                           "audit.event.ivr.failed",
                           "audit.event.email.failed");
    
    @PostConstruct
    public void init() {
       /* Initialize the default metrics counters */
       DEFAULT_METRICS_COUNTERS.stream().forEach(s -> METRICS.counter(s).inc(0));
    }
    
    @Inject
    public CassandraAuditor(Session session) {
        this.session = session;
        upsert = prepareUpsert();
    }

    private PreparedStatement prepareUpsert() {

        StringBuilder statement = new StringBuilder();
        statement.append("UPDATE ");
        statement.append(NOTIFICATION_AUDIT_TABLE);
        statement.append(" SET ");
        statement.append(COL_EVENT_LOG);
        statement.append("[?] = ? WHERE ");
        statement.append(COL_ID);
        statement.append(" = ? and ");
        statement.append(COL_TIME);
        statement.append(" = ?");

        return session.prepare(statement.toString());
    }

    @Override
    public void log(Notification notification, AuditEventState state) {
        incrementMetricForAuditEventState(state, notification.getMethod());
        upsert(notification.getEventIdentifier(), notification.getRxTimestamp(), state.toString() + "::" + notification.toJsonString());
    }

    @Override
    public void log(Notification notification, AuditEventState state, String message) {
        incrementMetricForAuditEventState(state, notification.getMethod());
        upsert(notification.getEventIdentifier(), notification.getRxTimestamp(), state.toString() + "::" + notification.toJsonString() + "::" + message);
    }
    
    public void log(String id, Instant rxTimestamp, AuditEventState state,String message) {
       upsert(id, rxTimestamp, state.toString() + "::{}::" + message);
   }    

    @Override
    public void log(Notification notification, AuditEventState state, Exception exception) {
        incrementMetricForAuditEventState(state, notification.getMethod());
        upsert(notification.getEventIdentifier(), notification.getRxTimestamp(), state.toString() + "::" + notification.toJsonString() + "::" + exception.toString());
    }

    private void incrementMetricForAuditEventState(AuditEventState state, NotificationMethod method) {
        METRICS.counter("audit.event." + state.toString().toLowerCase()).inc();
        METRICS.counter("audit.event." + method.toString().toLowerCase() + "." + state.toString().toLowerCase()).inc();
    }

    private void upsert (String id, Instant rxTimestamp, String eventLog) {
        BoundStatement statement = new BoundStatement(upsert).bind(
                Date.from(Instant.now()),		// Time the event is written to the log
                eventLog,						// Composite event log message (state, notification, error message, etc.)
                id,                       // Notification id that event belongs to (i.e., person:<uuid>, place:<uuid>)
                Date.from(rxTimestamp)
                );

        Context timer = auditDbTimer.time();
        session.execute(statement);
        timer.stop();

        LOGGER.debug(eventLog);
    }
}

