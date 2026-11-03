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
package com.iris.security.authz;

import java.util.EnumSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.messages.PlatformMessage;

/**
 * The {@link RiskyOperation}s this instance refuses from clients. Nothing is disabled unless
 * configured. Only client requests pass through here, so a service deleting a hub as part of
 * deleting its place isn't affected.
 */
@Singleton
public class DisabledRiskyOperations {
   public static final String PROP_DISABLED_RISKY_OPERATIONS = "authz.disabled.risky.operations";
   public static final String ALL = "all";

   private static final Logger logger = LoggerFactory.getLogger(DisabledRiskyOperations.class);

   private Set<RiskyOperation> disabled = EnumSet.noneOf(RiskyOperation.class);

   @Inject(optional = true)
   public void setDisabled(@Named(PROP_DISABLED_RISKY_OPERATIONS) String config) {
      EnumSet<RiskyOperation> operations = EnumSet.noneOf(RiskyOperation.class);
      for(String name : Splitter.on(',').trimResults().omitEmptyStrings().split(config)) {
         if(ALL.equalsIgnoreCase(name)) {
            operations.addAll(EnumSet.allOf(RiskyOperation.class));
         }
         else {
            operations.add(RiskyOperation.fromConfigName(name));
         }
      }
      operations.forEach((operation) -> logger.warn("Operation {} is disabled via configuration", operation.getConfigName()));
      this.disabled = operations;
   }

   public boolean isDisabled(PlatformMessage message) {
      return disabled.stream().anyMatch((operation) -> operation.matches(message));
   }
}
