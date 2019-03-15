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
package com.iris.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.messages.PlatformMessage;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;

public class IrisCorrelator<V> {
   private static final Logger log = LoggerFactory.getLogger(IrisCorrelator.class);

   private final Timer timer;
   private final Map<String,Action<V>> inflight;

   public IrisCorrelator() {
      this(new HashedWheelTimer());
   }

   public IrisCorrelator(Timer timer) {
      this.timer = timer;
      this.inflight = new ConcurrentHashMap<>();
   }

   public void track(PlatformMessage request, long timeout, TimeUnit unit, Action<V> action) {
      final String corr = request.getCorrelationId();
      if (StringUtils.isEmpty(corr)) {
         return;
      }

      Action<V> old = inflight.put(corr, action);
      if (old != null) {
         log.warn("conflicting requests correlation ids, terminating out old request via timeout handler");
         doTimeout(old);
      }

      timer.newTimeout(new TimerTask() {
         @Override
         public void run(@Nullable Timeout to) {
            Action<V> match = inflight.remove(corr);
            if (match != null) {
               log.info("timed out waiting for response to {}", corr);
               doTimeout(match);
            }
         }
      }, timeout, unit);
   }

   @Nullable
   public Result<V> correlate(PlatformMessage response) {
      String corr = response.getCorrelationId();
      if (StringUtils.isEmpty(corr)) {
         return null;
      }

      Action<V> match = inflight.remove(corr);
      if (match != null) {
         return doResponse(match, response);
      }

      return null;
   }

   private Result<V> doResponse(Action<V> action, PlatformMessage response) {
      try {
         return Results.fromValue(action.onResponse(response));
      } catch (Exception ex) {
         log.warn("correlator action generated unexpected exception:", ex);
         return Results.fromError(ex);
      }
   }

   private void doTimeout(Action<?> action) {
      try {
         action.onTimeout();
      } catch (Exception ex) {
         log.warn("correlator action generated unexpected exception:", ex);
      }
   }

   public interface Action<V> {
      V onResponse(PlatformMessage response);
      void onTimeout();
   }
}

