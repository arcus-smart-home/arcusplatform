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
package com.iris.metrics;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.RatioGauge;
import com.codahale.metrics.Timer;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class TestIrisMetrics {
   // Defines the metrics set that bundles up all metrics for this class.
   private static final IrisMetricSet METRICS = IrisMetrics.metrics(TestIrisMetrics.class);

   // Defines a set of registered metrics that are reported.
   private static final Meter SAMPLE_RATE = METRICS.meter("example-meter");

   @SuppressWarnings("unused")
   private static final Counter NUM_SAMPLES = METRICS.counter("example-counter");

   @SuppressWarnings("unused")
   private static final Histogram SAMPLE_HIST = METRICS.histogram("example-histogram");

   // Defines a metric that is not reported, but can be used programatically.
   @SuppressWarnings("unused")
   private static final RatioGauge SAMPLE_RATIO = METRICS.ratio1(SAMPLE_RATE, SAMPLE_RATE);

   final ConsoleReporter reporter = ConsoleReporter.forRegistry(IrisMetrics.registry())
                                                   .convertRatesTo(TimeUnit.SECONDS)
                                                   .convertDurationsTo(TimeUnit.MILLISECONDS)
                                                   .build();
   @After
   public void finish() {
      System.out.print("\n--\n");
      reporter.report();
   }

   @Test
   public void testValidNames() throws Exception {
      IrisMetrics.metrics("s");
      IrisMetrics.metrics("short");
      IrisMetrics.metrics("medium.name");
      IrisMetrics.metrics("really.long.name");

      IrisMetrics.metrics("name-dash");
      IrisMetrics.metrics("medium-dash.name-dash");
      IrisMetrics.metrics("really-dash.long-dash.name-dash");

      IrisMetrics.metrics("name-dash0");
      IrisMetrics.metrics("medium-dash0.name-dash0");
      IrisMetrics.metrics("really-dash0.long-dash0.name-dash0");

      IrisMetrics.metrics(TestIrisMetrics.class, (String)null);
      IrisMetrics.metrics(TestIrisMetrics.class, (String)null, "name");

      IrisMetrics.metrics(null, (String)null);
      IrisMetrics.metrics(null, (String)null, "name");
   }

   @Test
   public void testInvalidNames() throws Exception {
      IrisMetrics.metrics("");
      IrisMetrics.metrics("-");
      IrisMetrics.metrics("0");
      IrisMetrics.metrics(".");

      IrisMetrics.metrics(TestIrisMetrics.class, "");
      IrisMetrics.metrics(TestIrisMetrics.class, "-");
      IrisMetrics.metrics(TestIrisMetrics.class, "0");
      IrisMetrics.metrics(TestIrisMetrics.class, ".");

      for (int i = 0; i < 1000; ++i) {
         String random = RandomStringUtils.random(120);
         IrisMetrics.metrics(TestIrisMetrics.class, random);
      }

      for (int i = 0; i < 1000; ++i) {
         String random2 = RandomStringUtils.random(120);
         IrisMetrics.metrics(TestIrisMetrics.class, "base****.name@%#$@").meter(random2);
      }
   }

   @Test
   public void testSimpleCounter() throws Exception {
      Counter counter = IrisMetrics.metrics(TestIrisMetrics.class, "test-counter").counter("test");
      for (long i = 0; i < 10; ++i) {
         assertEquals(i, counter.getCount());
         counter.inc();
      }
   }

   @Test
   public void testSimpleMeter() throws Exception {
      Meter meter = IrisMetrics.metrics(TestIrisMetrics.class, "test-meter").meter("test");
      assertNotNull(meter);
   }

   @Test
   public void testSimpleTimer() throws Exception {
      Timer timer = IrisMetrics.metrics(TestIrisMetrics.class, "test-timer").timer("test");
      assertNotNull(timer);
   }

   @Test
   public void testSimpleHistogram() throws Exception {
      Histogram hist = IrisMetrics.metrics(TestIrisMetrics.class, "test-hist").histogram("test");
      assertNotNull(hist);

   }

   @Test
   public void testSimpleRatio() throws Exception {
      final IrisMetricSet MS = IrisMetrics.metrics(TestIrisMetrics.class, "test-ratio");
      RatioGauge ratio1 = MS.ratio1("test1", MS.meter(), MS.meter());
      assertNotNull(ratio1);

      RatioGauge ratio5 = MS.ratio5("test5", MS.meter(), MS.meter());
      assertNotNull(ratio5);

      RatioGauge ratio15 = MS.ratio15("test15", MS.meter(), MS.meter());
      assertNotNull(ratio15);
   }

   @Test
   public void testSimpleCached() throws Exception {
      final IrisMetricSet MS = IrisMetrics.metrics(TestIrisMetrics.class, "test-cached");
      Gauge<Double> cached = MS.cache("test", MS.ratio1(MS.meter(), MS.meter()), 1, TimeUnit.MINUTES);
      assertNotNull(cached);
   }

   @Test
   public void testMetricSetNamesUnique() throws Exception {
      IrisMetricSet set1 = IrisMetrics.metrics(TestIrisMetrics.class, "test-unique");
      IrisMetricSet set2 = IrisMetrics.metrics(TestIrisMetrics.class, "test-unique");
      IrisMetricSet set3 = IrisMetrics.metrics(TestIrisMetrics.class, "test-unique2");

      Counter c11 = set1.counter("test1");
      Counter c12 = set1.counter("test2");
      Counter c21 = set2.counter("test1"); // SAME AS C11
      Counter c22 = set2.counter("test2"); // SAME AS C12
      Counter c31 = set3.counter("test1");
      Counter c32 = set3.counter("test2");

      c11.inc();
      c12.inc();
      c21.inc();
      c22.inc();
      c31.inc();
      c32.inc();

      assertEquals(2L, c11.getCount());
      assertEquals(2L, c12.getCount());
      assertEquals(2L, c21.getCount());
      assertEquals(2L, c22.getCount());
      assertEquals(1L, c31.getCount());
      assertEquals(1L, c32.getCount());
   }

   public static void main(String[] args) {
      TestIrisMetrics test = new TestIrisMetrics();
   }
}

