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
package com.iris.capability.generator.html;

import java.util.ArrayList;
import java.util.List;

public class DefinitionIndex {
   private final List<DefinitionIndexEntry> capabilities = new ArrayList<DefinitionIndexEntry>();
   private final List<DefinitionIndexEntry> services = new ArrayList<DefinitionIndexEntry>();

   public void addCapability(String name, String htmlFile) {
      capabilities.add(new DefinitionIndexEntry(htmlFile, name));
   }

   public void addService(String name, String htmlFile) {
      services.add(new DefinitionIndexEntry(htmlFile, name));
   }

   public List<DefinitionIndexEntry> getCapabilities() {
      return new ArrayList<DefinitionIndexEntry>(capabilities);
   }

   public List<DefinitionIndexEntry> getServices() {
      return new ArrayList<DefinitionIndexEntry>(services);
   }

   public static class DefinitionIndexEntry {
      private final String htmlFile;
      private final String name;

      public DefinitionIndexEntry(String htmlFile, String name) {
         this.htmlFile = htmlFile;
         this.name = name;
      }

      public String getHtmlFile() {
         return htmlFile;
      }

      public String getName() {
         return name;
      }
   }
}

