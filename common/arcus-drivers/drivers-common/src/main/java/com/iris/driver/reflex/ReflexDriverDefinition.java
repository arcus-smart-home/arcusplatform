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
package com.iris.driver.reflex;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.iris.device.model.CapabilityDefinition;
import com.iris.model.Version;
import com.iris.regex.Regex;
import com.iris.regex.RegexDfa;
import com.iris.regex.RegexDfaByte;
import com.iris.regex.RegexNfa;
import com.iris.regex.RegexUtil;

public final class ReflexDriverDefinition {
   private static final Logger log = LoggerFactory.getLogger(ReflexDriverDefinition.class);

   private final String name;
   private final Version version;
   private final String hash;
   private final long offlineTimeout;
   private final Set<String> capabilities;
   private final List<ReflexDefinition> reflexes;
   private final @Nullable ReflexDriverDFA dfa;
   private final ReflexRunMode mode;

   public ReflexDriverDefinition(String name, Version version, String hash, long offlineTimeout, Set<CapabilityDefinition> capabilities, ReflexRunMode mode, List<ReflexDefinition> reflexes) {
      if (reflexes == null) {
         reflexes = ImmutableList.of();
      }

      List<ReflexDefinition> updated = new ArrayList<>();
      RegexNfa<Byte,List<ReflexAction>> nfa = null;
      long start = System.nanoTime();
      for (ReflexDefinition reflex : reflexes) {
         if (reflex.getActions().isEmpty()) {
            continue;
         }

         List<ReflexMatch> updatedMatches = new ArrayList<>();
         for (ReflexMatch match : reflex.getMatchers()) {
            if (match instanceof ReflexMatchRegex) {
               ReflexMatchRegex regex = (ReflexMatchRegex)match;
               RegexNfa<Byte,List<ReflexAction>> cur = Regex.parseByteRegex(regex.getRegex() + " .*", reflex.getActions());
               nfa = (nfa == null) ? cur : RegexNfa.append(nfa, cur);
            } else {
               updatedMatches.add(match);
            }
         }

         updated.add(new ReflexDefinition(updatedMatches, reflex.getActions()));
      }

      RegexDfaByte<List<ReflexAction>> dfa = null;
      if (nfa != null) {
         RegexDfa<Byte,List<List<ReflexAction>>> dfa1 = RegexUtil.nfaConvertToDfa(nfa);
         RegexDfa<Byte,List<ReflexAction>> dfa2 = RegexUtil.dfaConvertValueSpace(dfa1, new Function<List<List<ReflexAction>>, List<ReflexAction>>() {
            @Override
            public List<ReflexAction> apply(@Nullable List<List<ReflexAction>> input) {
               if (input == null || input.isEmpty()) {
                  return ImmutableList.of();
               }

               ImmutableList.Builder<ReflexAction> result = ImmutableList.builder();
               for (List<ReflexAction> next : input) {
                  result.addAll(next);
               }

               return result.build();
            }
         });

         RegexDfa<Byte,List<ReflexAction>> dfa3 = RegexUtil.dfaMinimize(dfa2);
         dfa = RegexUtil.dfaToByteRep(dfa3);
         long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
         if (log.isTraceEnabled()) {
            log.trace("took {}ms to convert regexes to nfa, convert nfa to dfa, perform minimization, and perform dfa optimization", elapsed);
         }
      }

      Set<String> caps = new HashSet<>();
      for (CapabilityDefinition cdef : capabilities) {
         caps.add(cdef.getCapabilityName());
      }

      this.name = name;
      this.version = version;
      this.hash = hash;
      this.capabilities = ImmutableSet.copyOf(caps);
      this.offlineTimeout = offlineTimeout;
      this.mode = mode;
      this.reflexes = updated;
      this.dfa = (dfa == null) ? null : new ReflexDriverDFA(dfa);

      if (reflexes.size() > 0) {
         log.debug("{} defined {} reflexes running {}", name, reflexes.size(), mode);
      }
   }

   public ReflexDriverDefinition(String name, Version version, String hash, long offlineTimeout, Set<String> capabilities, ReflexRunMode mode, List<ReflexDefinition> reflexes, @Nullable RegexDfaByte<List<ReflexAction>> dfa) {
      this(name, version, hash, offlineTimeout, capabilities, mode, reflexes, (dfa == null) ? null : new ReflexDriverDFA(dfa));
   }

   public ReflexDriverDefinition(String name, Version version, String hash, long offlineTimeout, Set<String> capabilities, ReflexRunMode mode, List<ReflexDefinition> reflexes, @Nullable ReflexDriverDFA dfa) {
      this.name = name;
      this.version = version;
      this.hash = hash;
      this.offlineTimeout = offlineTimeout;
      this.capabilities = capabilities;
      this.mode = mode;
      this.reflexes = reflexes;
      this.dfa = dfa;
   }

   public String getName() {
      return name;
   }

   public Version getVersion() {
      return version;
   }

   public String getHash() {
      return hash;
   }

   public long getOfflineTimeout() {
      return offlineTimeout;
   }

   public Set<String> getCapabilities() {
      return capabilities;
   }

   public ReflexRunMode getMode() {
      return mode;
   }

   public List<ReflexDefinition> getReflexes() {
      return reflexes;
   }

   public @Nullable ReflexDriverDFA getDfa() {
      return dfa;
   }
}

