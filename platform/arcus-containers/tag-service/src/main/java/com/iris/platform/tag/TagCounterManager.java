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
package com.iris.platform.tag;

import static java.lang.String.format;
import static org.apache.commons.collections.ListUtils.subtract;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.eclipse.jdt.annotation.Nullable;

import com.codahale.metrics.Counter;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.metrics.tag.TaggingMetric;
import com.iris.platform.util.LazyReference;
import com.iris.resource.Resource;
import com.iris.resource.Resources;

@Singleton
public class TagCounterManager
{
   private static final IrisMetricSet METRICS = IrisMetrics.metrics("tags");

   private final Resource tagIncludesResource;

   private final LazyReference<ImmutableMap<String, TaggingMetric<Counter>>> tagCountersRef;

   @Inject
   public TagCounterManager(TagServiceConfig config)
   {
      tagIncludesResource = Resources.getResource(config.getTagIncludesPath());

      tagCountersRef = LazyReference.fromCallable(this::loadTagCounters);

      // Load eagerly at startup to make any config problems fail-fast
      tagCountersRef.get();

      if (tagIncludesResource.isWatchable())
      {
         tagIncludesResource.addWatch(tagCountersRef::reset);
      }
   }

   @Nullable
   public TaggingMetric<Counter> getTagCounter(String tagName)
   {
      return tagCountersRef.get().get(tagName);
   }

   private ImmutableMap<String, TaggingMetric<Counter>> loadTagCounters()
   {
      List<String> tagNames;
      try (InputStream in = tagIncludesResource.open())
      {
         tagNames = IOUtils.readLines(in);
      }
      catch (IOException e)
      {
         throw new RuntimeException("Unable to read tag includes at: " + tagIncludesResource.getRepresentation(), e);
      }

      @SuppressWarnings("unchecked")
      List<String> duplicates = subtract(tagNames, new ArrayList<String>(new HashSet<String>(tagNames)));
      if (!duplicates.isEmpty())
      {
         throw new IllegalStateException(format("Tag includes file at [%s] has duplicates: %s",
            tagIncludesResource.getRepresentation(), duplicates));
      }

      ImmutableMap.Builder<String, TaggingMetric<Counter>> tagCountersBuilder = ImmutableMap.builder();

      for (String tagName : tagNames)
      {
         tagCountersBuilder.put(tagName, METRICS.taggingCounter(tagName));
      }

      return tagCountersBuilder.build();
   }
}

