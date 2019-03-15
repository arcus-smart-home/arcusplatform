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

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.iris.modelmanager.Status;

public class ChangeSet {

   private String author;
   private String identifier;
   private String description;
   private String tracking;
   private List<Command> commands = new LinkedList<Command>();
   private String checksum;
   private String source;
   private String version;
   private Date timestamp;
   private Status status = Status.PENDING;

   public String getUniqueIdentifier() {
      return author + ":" + identifier;
   }
;
   public String getAuthor() {
      return author;
   }

   public void setAuthor(String author) {
      this.author = author;
   }

   public String getIdentifier() {
      return identifier;
   }

   public void setIdentifier(String identifier) {
      this.identifier = identifier;
   }

   public String getDescription() {
      return description;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public String getTracking() {
      return tracking;
   }

   public void setTracking(String tracking) {
      this.tracking = tracking;
   }

   public List<Command> getCommands() {
      return commands;
   }

   public void addCommand(Command command) {
      commands.add(command);
   }

   public String getChecksum() {
      return checksum;
   }

   public void setChecksum(String checksum) {
      this.checksum = checksum;
   }

   public String getSource() {
      return source;
   }

   public void setSource(String source) {
      this.source = source;
   }

   public String getVersion() {
      return version;
   }

   public void setVersion(String version) {
      this.version = version;
   }

   public Date getTimestamp() {
      return timestamp;
   }
   public void setTimestamp(Date timestamp) {
      this.timestamp = timestamp;
   }

   public Status getStatus() {
      return status;
   }

   public void setStatus(Status status) {
      this.status = status;
   }

   @Override
   public String toString() {
      return "ChangeSet [author=" + author + ", identifier=" + identifier
            + ", description=" + description + ", tracking=" + tracking
            + ", commands=" + commands + ", checksum=" + checksum + ", source="
            + source + ", version=" + version + ", timestamp=" + timestamp
            + ", status=" + status + "]";
   }
}

