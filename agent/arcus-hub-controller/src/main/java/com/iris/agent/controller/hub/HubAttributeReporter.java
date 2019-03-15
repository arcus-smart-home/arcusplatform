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
package com.iris.agent.controller.hub;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.iris.agent.attributes.HubAttributesService;
import com.iris.agent.attributes.HubAttributesService.Attribute;
import com.iris.agent.attributes.HubAttributesService.UpdatedAttribute;
import com.iris.agent.exec.ExecService;
import com.iris.agent.lifecycle.LifeCycleService;
import com.iris.agent.router.Port;
import com.iris.agent.util.RxIris;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.Capability;

public class HubAttributeReporter {
   private static final Logger log = LoggerFactory.getLogger(HubAttributeReporter.class);
   private static final long AGGREGATE_VALUE_CHANGES_INTERVAL = TimeUnit.MILLISECONDS.toNanos(100);

   private final Port port;

   private final Lock expectedSetAttributesLock;
   private final AtomicReference<Map<String,Object>> expectedSetAttributes;
   private final PeriodicReporter reporter;
   private Map<String,Object> aggregateReports;

   public HubAttributeReporter(Port port) {
      this.port = port;
      this.reporter = new PeriodicReporter();

      this.expectedSetAttributesLock = new ReentrantLock();
      this.expectedSetAttributes = new AtomicReference<>();
      this.aggregateReports = new HashMap<>();

      HubAttributesService.updates().subscribeOn(RxIris.io).retry().subscribe(new rx.Subscriber<HubAttributesService.UpdatedAttribute>() {
         @Override
         public void onNext(@Nullable UpdatedAttribute updated) {
            HubAttributesService.Attribute<?> attr = (updated != null) ? updated.getAttr() : null;
            if (attr != null) {
               hubAttributeUpdated(attr);
            }
         }

         @Override public void onError(@Nullable Throwable e) { }
         @Override public void onCompleted() { }
      });
   }

   private void hubAttributeUpdated(HubAttributesService.Attribute<?> attr) {
      if (!LifeCycleService.isAuthorized()) {
         return;
      }

      if (!attr.isReportedOnValueChange()) {
         attr.markReported();
         return;
      }

      String name = attr.name();
      Map<String,Object> ignore = expectedSetAttributes.get();
      if (ignore != null) {
         for (String ignoreName : ignore.keySet()) {
            if (ignoreName != null && name.startsWith(ignoreName)) {
               return;
            }
         }
      }

      Object old;
      synchronized (aggregateReports) {
         old = aggregateReports.put(name, attr.get());

         // We need to make sure that this value change is emitted before value we just added to the map
         if (old != null) {
            MessageBody message = MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, ImmutableMap.of(
               name, old
            ));

            port.sendEvent(message);
         }
      }

      attr.markReported();
      reporter.schedule();
   }

   AutoCloseable setExpectedSetAttributes(Map<String,Object> expected) {
      expectedSetAttributesLock.lock();
      expectedSetAttributes.set(expected);
      return new SetAttributesLocker();
   }

   private void clearExpectedSetAttributes() {
      try {
         Map<String,Object> expected = expectedSetAttributes.getAndSet(null);
         if (expected == null || expected.isEmpty()) {
            return;
         }

         Map<String,Object> updates = HubAttributesService.asAttributeMap(expected.keySet(), true);
         if (updates != null && !updates.isEmpty()) {
            MessageBody vc = MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, updates);
            port.sendEvent(vc);
         }
      } finally {
         expectedSetAttributesLock.unlock();
      }
   }

   public final class PeriodicReporter implements Runnable {
      private long lastReportTime = Long.MIN_VALUE;

      @Override
      public void run() {
         long elapsed = System.nanoTime() - lastReportTime;
         if (lastReportTime != Long.MIN_VALUE && elapsed < AGGREGATE_VALUE_CHANGES_INTERVAL) {
            return;
         }

         Map<String,Object> vc;
         synchronized (aggregateReports) {
            vc = aggregateReports;
            aggregateReports = new HashMap<>();
         }

         if (vc == null || vc.isEmpty()) {
            return;
         }
         
         MessageBody message = MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, vc);
         port.sendEvent(message);
      }

      void schedule() {
         ExecService.periodic().schedule(this, AGGREGATE_VALUE_CHANGES_INTERVAL, TimeUnit.NANOSECONDS);
      }
   }

   public final class SetAttributesLocker implements AutoCloseable {
      @Override
      public void close() throws Exception {
         clearExpectedSetAttributes();
      }
   }
}

