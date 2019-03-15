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
package com.iris.platform.scheduler.cassandra;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class SchedulerAddressIndex {

   public static String NAME = "scheduler_address_index";

   public static class Columns {
      public static final String ADDRESS = "address";
      public static final String SCHEDULER_IDS = "schedulerIds";
      public static final String RELATIONSHIP = "relationship";

      public static final List<String> ALL = ImmutableList.of(
            ADDRESS,
            SCHEDULER_IDS,
            RELATIONSHIP
      );
   }

   public enum Relationship {
      REFERENCE, TARGET;
   }
}

