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
package com.iris.platform.subsystem.cassandra;


public class SubsystemTable {
   // TODO may change this to be a generic model table
   public static final String NAME = "subsystem";
   
   public static final class Columns {
      public static final String PLACE_ID     = "placeId";
      public static final String NAMESPACE    = "namespace";
      public static final String CREATED      = "created";
      public static final String MODIFIED     = "modified";
      public static final String ATTRIBUTES   = "attributes";
   }

}

