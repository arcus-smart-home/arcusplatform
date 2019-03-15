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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;

import com.iris.modelmanager.changelog.checksum.ChecksumInvalidException;
import com.iris.modelmanager.changelog.checksum.ChecksumUtil;
import com.iris.modelmanager.version.Version;

public class ChangeLogSet {

   public static ChangeLogSet fromChangesets(List<ChangeSet> changeSets) {

      Collections.sort(changeSets, (ChangeSet cs1, ChangeSet cs2) -> ObjectUtils.compare(cs1.getTimestamp(), cs2.getTimestamp()));

      List<ChangeLog> derivedChangeLogs = new LinkedList<ChangeLog>();

      ChangeLog curChangeLog = null;

      for(ChangeSet cs : changeSets) {
         if(curChangeLog == null || !cs.getSource().equals(curChangeLog.getSource())) {
            curChangeLog = new ChangeLog();
            curChangeLog.setSource(cs.getSource());
            curChangeLog.setVersion(cs.getVersion());
            derivedChangeLogs.add(curChangeLog);
         }
         curChangeLog.addChangeSet(cs);
      }

      return new ChangeLogSet(derivedChangeLogs);
   }

   private final List<ChangeLog> changeLogs = new LinkedList<ChangeLog>();
   private Map<String,ChangeLog> bySource = new LinkedHashMap<String,ChangeLog>();

   public ChangeLogSet(List<ChangeLog> changeLogs) {
      this.changeLogs.addAll(changeLogs);
      Collections.sort(this.changeLogs, ((ChangeLog log1, ChangeLog log2) -> log1.getVersion().compareTo(log2.getVersion())));
      for(ChangeLog cl : changeLogs) {
         bySource.put(cl.getSource(), cl);
      }
   }

   public List<ChangeLog> getChangeLogs() {
      return changeLogs;
   }

   public List<ChangeLog> diff(ChangeLogSet model) {
      for(ChangeLog changeLog : model.changeLogs) {
         ChangeLog myChangeLog = bySource.get(changeLog.getSource());

         if(myChangeLog != null) {
            for(ChangeSet changeSet : changeLog.getChangeSets()) {
               if(!myChangeLog.removeChangeSet(changeSet.getIdentifier())) {
                  myChangeLog.addChangeSet(changeSet);
               }
            }

            if(myChangeLog.getChangeSets().isEmpty()) {
               changeLogs.remove(myChangeLog);
               bySource.remove(myChangeLog.getSource());
            }
         } else {
            changeLogs.add(changeLog);
            bySource.put(changeLog.getSource(), changeLog);
         }
      }

      return changeLogs;
   }

   public List<ChangeLog> prune(Version version) {
      if(version == null) {
         return changeLogs;
      }

      Iterator<ChangeLog> iterator = this.changeLogs.iterator();
      while(iterator.hasNext()) {
         ChangeLog changelog = iterator.next();
         Version changeLogVersion = Version.valueOf(changelog.getVersion());
         if(changeLogVersion.compareTo(version) <= 0) {
            iterator.remove();
         }
      }

      return changeLogs;
   }

   public void verify(ChangeLogSet model) throws ChecksumInvalidException {
   	verify(model, true);
   }
   
   public void verify(ChangeLogSet model, boolean verifyChecksums) throws ChecksumInvalidException {
      for(ChangeLog changeLog : model.changeLogs) {
         ChangeLog myChangeLog = bySource.get(changeLog.getSource());

         if(myChangeLog == null) { continue; }

         for(ChangeSet changeSet : changeLog.getChangeSets()) {
            ChangeSet myChangeSet = myChangeLog.findChangeSetById(changeSet.getIdentifier());
            if(myChangeSet == null) { continue; }
            if(verifyChecksums) ChecksumUtil.verifyChecksums(changeSet, myChangeLog.findChangeSetById(changeSet.getIdentifier()));
         }
      }
   }
}

