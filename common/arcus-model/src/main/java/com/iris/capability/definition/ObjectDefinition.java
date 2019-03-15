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
package com.iris.capability.definition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ObjectDefinition extends Definition {
   protected String namespace;
   protected String version;
   protected final List<MethodDefinition> methods;
   protected final List<EventDefinition> events;
   
   ObjectDefinition(
         String name,
         String description,
         String namespace,
         String version,
         List<MethodDefinition> methods,
         List<EventDefinition> events
   ) {
      super(name, description);
      this.namespace = namespace;
      this.version = version;
      this.methods = Collections.unmodifiableList(methods);
      this.events = Collections.unmodifiableList(events);
   }

   public String getNamespace() {
      return namespace;
   }

   public String getVersion() {
      return version;
   }

   public List<MethodDefinition> getMethods() {
      return new ArrayList<MethodDefinition>(methods);
   }

   public List<EventDefinition> getEvents() {
      return new ArrayList<EventDefinition>(events);
   }

}

