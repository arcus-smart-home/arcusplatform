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
package com.iris.modelmanager.changelog;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ChangeLog {

   private String version;
   private String source;
   private List<ChangeSet> changeSets = new LinkedList<ChangeSet>();
   private Map<String,ChangeSet> lookupById = new LinkedHashMap<String,ChangeSet>();

   public String getVersion() {
      return version;
   }

   public void setVersion(String version) {
      this.version = version;
   }

   public String getSource() {
      return source;
   }

   public void setSource(String source) {
      this.source = source;
   }

   public List<ChangeSet> getChangeSets() {
      return changeSets;
   }

   public void addChangeSet(ChangeSet changeSet) {
      changeSet.setSource(source);
      changeSet.setVersion(version);
      this.changeSets.add(changeSet);
      this.lookupById.put(changeSet.getIdentifier(), changeSet);
   }

   public ChangeSet findChangeSetById(String id) {
      return this.lookupById.get(id);
   }

   public boolean removeChangeSet(String id) {
      ChangeSet cs = lookupById.remove(id);
      return changeSets.remove(cs);
   }

   @Override
   public String toString() {
      return "ChangeLog [version=" + version + ", source=" + source
            + ", changeSets=" + changeSets + "]";
   }
}

