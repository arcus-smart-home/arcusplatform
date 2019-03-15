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
package com.iris.metrics.tag;

import com.codahale.metrics.Timer;
import com.iris.metrics.AsyncTimer;
import com.iris.metrics.IrisMetricSet;

/**
 * Doesn't extend {@link TaggingMetric} to make it clear that it's really a lightweight composite of two {@link
 * TaggingMetric}s that are registered directly.
 */
public class TaggingAsyncTimer
{
   private final TaggingMetric<Timer> successTaggingTimer;
   private final TaggingMetric<Timer> failureTaggingTimer;

   public TaggingAsyncTimer(IrisMetricSet irisMetricSet)
   {
      successTaggingTimer = irisMetricSet.taggingTimer();
      failureTaggingTimer = irisMetricSet.taggingTimer();
   }

   public TaggingAsyncTimer(String name, IrisMetricSet irisMetricSet)
   {
      successTaggingTimer = irisMetricSet.taggingTimer(name + ".success");
      failureTaggingTimer = irisMetricSet.taggingTimer(name + ".failure");
   }

   public AsyncTimer tag(String name, String value)
   {
      return tag(new TagValue(name, value));
   }

   public AsyncTimer tag(TagValue tag)
   {
      Timer successTimer = successTaggingTimer.tag(tag);
      Timer failureTimer = failureTaggingTimer.tag(tag);

      return new AsyncTimer(successTimer, failureTimer);
   }

   @Override
   public String toString()
   {
      return "TaggingAsyncTimer [success = " + successTaggingTimer + ", failure = " + failureTaggingTimer + "]";
   }
}

