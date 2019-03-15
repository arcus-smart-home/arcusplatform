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
package com.iris.notification.provider.metrics;

import org.junit.Test;

import com.codahale.metrics.Counter;
import com.datastax.driver.core.Session;
import com.google.inject.Inject;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.platform.notification.audit.CassandraAuditor;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;

@Mocks({ Session.class })
public class GcmApnsMetricsInitializationTest extends IrisMockTestCase {

   @Inject
   protected CassandraAuditor aud;

   private static final IrisMetricSet METRICS = IrisMetrics.metrics(CassandraAuditor.SERVICE_NAME);

   @Test
   public void testInitialization() throws Exception {
      /* Test each default metric to ensure that it is initialized to 0l */
      CassandraAuditor.DEFAULT_METRICS_COUNTERS.stream()
         .forEach(s -> assertEquals(0L, 
               ((Counter) METRICS.getMetrics().get(String.format("%s.%s",CassandraAuditor.SERVICE_NAME,s))).getCount()));

   }
}

