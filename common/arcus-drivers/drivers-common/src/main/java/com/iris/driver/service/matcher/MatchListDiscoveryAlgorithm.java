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
package com.iris.driver.service.matcher;

import java.util.Map;

import com.iris.device.attributes.AttributeMap;
import com.iris.messages.model.DriverId;
import com.iris.util.MatchList;

public class MatchListDiscoveryAlgorithm implements DiscoveryAlgorithm {
   private final Map<String, MatchList<ReflexVersionAndAttributes, DriverId>> matchers;
   
   public MatchListDiscoveryAlgorithm(Map<String,MatchList<ReflexVersionAndAttributes, DriverId>> matchers) {
      this.matchers = matchers;
   }

   @Override
   public DriverId discover(String population, AttributeMap protocolAttributes, Integer maxReflexVersion) {
      MatchList<ReflexVersionAndAttributes, DriverId> matcher = matchers.get(population);
      return (matcher != null) ? matcher.match(new ReflexVersionAndAttributes(protocolAttributes,maxReflexVersion)) : null;
   }

   public static final class ReflexVersionAndAttributes {
      private final AttributeMap protocolAttributes;
      private final int maxReflexVersion;

      public ReflexVersionAndAttributes(AttributeMap protocolAttributes, Integer maxReflexVersion) {
         this.protocolAttributes = protocolAttributes;
         this.maxReflexVersion = (maxReflexVersion == null) ? 0 : maxReflexVersion;
      }

      public AttributeMap getProtocolAttributes() {
         return protocolAttributes;
      }

      public int getMaxReflexVersion() {
         return maxReflexVersion;
      }
   }
}

