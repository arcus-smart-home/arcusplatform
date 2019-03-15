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
package com.iris.platform.metrics;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import static com.iris.platform.metrics.MetricUtils.*;

public final class MetricFilters {
   private static final Logger log = LoggerFactory.getLogger(MetricFilters.class);

   private MetricFilters() {
   }

   /////////////////////////////////////////////////////////////////////////////
   // Match on host name
   /////////////////////////////////////////////////////////////////////////////

   public static final Predicate<JsonObject> fromHost(String host) {
      log.info("filtering on host name: {}", host);
      return (report) -> host.equals(getTagProperty(report,"host"));
   }

   public static final Predicate<JsonObject> fromHost(String... hosts) {
      return fromHost(Arrays.asList(hosts));
   }

   public static final Predicate<JsonObject> fromHost(List<String> hosts) {
      return hosts.stream()
         .map((host) -> fromHost(host))
         .reduce(Predicate::or)
         .orElse((report) -> false);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Match on container name
   /////////////////////////////////////////////////////////////////////////////

   public static final Predicate<JsonObject> fromContainer(String ctn) {
      log.info("filtering on container name: {}", ctn);
      return (report) -> ctn.equals(getTagProperty(report,"container"));
   }

   public static final Predicate<JsonObject> fromContainer(String... ctns) {
      return fromContainer(Arrays.asList(ctns));
   }

   public static final Predicate<JsonObject> fromContainer(List<String> ctns) {
      return ctns.stream()
         .map((ctn) -> fromContainer(ctn))
         .reduce(Predicate::or)
         .orElse((report) -> false);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Match on service name
   /////////////////////////////////////////////////////////////////////////////

   public static final Predicate<JsonObject> fromService(String svc) {
      log.info("filtering on service name: {}", svc);
      return (report) -> svc.equals(getTagProperty(report,"service"));
   }

   public static final Predicate<JsonObject> fromService(String... svcs) {
      return fromService(Arrays.asList(svcs));
   }

   public static final Predicate<JsonObject> fromService(List<String> svcs) {
      return svcs.stream()
         .map((svc) -> fromService(svc))
         .reduce(Predicate::or)
         .orElse((report) -> false);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Exact match on metric name
   /////////////////////////////////////////////////////////////////////////////

   public static final Predicate<JsonObject> nameEquals(String name) {
      log.info("filtering on metric name exact match: {}", name);
      return (report) -> name.equals(getAsStringOrNull(report,"name"));
   }

   public static final Predicate<JsonObject> nameEquals(String... names) {
      return nameEquals(Arrays.asList(names));
   }

   public static final Predicate<JsonObject> nameEquals(List<String> names) {
      return names.stream()
         .map((name) -> nameEquals(name))
         .reduce(Predicate::or)
         .orElse((report) -> false);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Prefix match on metric name
   /////////////////////////////////////////////////////////////////////////////

   public static final Predicate<JsonObject> namePrefixedBy(String prefix) {
      log.info("filtering on metric name prefix match: {}", prefix);
      return (report) -> {
         String name = getAsStringOrNull(report,"name");
         return (name == null) ? false : name.startsWith(prefix);
      };
   }

   public static final Predicate<JsonObject> namePrefixedBy(String... prefixes) {
      return namePrefixedBy(Arrays.asList(prefixes));
   }

   public static final Predicate<JsonObject> namePrefixedBy(List<String> prefixes) {
      return prefixes.stream()
         .map((prefix) -> namePrefixedBy(prefix))
         .reduce(Predicate::or)
         .orElse((report) -> false);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Regex match on metric name
   /////////////////////////////////////////////////////////////////////////////

   public static final Predicate<JsonObject> nameMatches(String regex) {
      return nameMatches(Pattern.compile(regex));
   }

   public static final Predicate<JsonObject> nameMatches(String regex, int flags) {
      return nameMatches(Pattern.compile(regex,flags));
   }

   public static final Predicate<JsonObject> nameMatches(Pattern pattern) {
      log.info("filtering on metric name regex match: {}", pattern);
      return (report) -> {
         String name = getAsStringOrNull(report, "name");
         return (name == null) ? false : pattern.matcher(name).matches();
      };
   }

   public static final Predicate<JsonObject> nameMatches(String... regexes) {
      return nameMatchesList(Arrays.asList(regexes));
   }

   public static final Predicate<JsonObject> nameMatches(Pattern... patterns) {
      return nameMatches(Arrays.asList(patterns));
   }

   public static final Predicate<JsonObject> nameMatchesList(List<String> regexes) {
      List<Pattern> patterns = regexes.stream()
         .map((regex) -> Pattern.compile(regex))
         .collect(Collectors.toList());

      return nameMatches(patterns);
   }

   public static final Predicate<JsonObject> nameMatches(List<Pattern> patterns) {
      return patterns.stream()
         .map((pattern) -> nameMatches(pattern))
         .reduce(Predicate::or)
         .orElse((report) -> false);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Utilities
   /////////////////////////////////////////////////////////////////////////////

   @Nullable
   private static JsonObject getTags(JsonObject report) {
      JsonElement elem = report.get("tags");
      if (elem == null || !elem.isJsonObject()) {
         return null;
      }

      return elem.getAsJsonObject();
   }

   @Nullable
   private static String getTagProperty(JsonObject report, String name) {
      JsonObject tags = getTags(report);
      if (tags == null) {
         return null;
      }

      return getAsStringOrNull(tags, name);
   }
}

