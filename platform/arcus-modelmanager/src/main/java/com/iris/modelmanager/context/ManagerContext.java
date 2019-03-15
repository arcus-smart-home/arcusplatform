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
package com.iris.modelmanager.context;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import com.datastax.driver.core.ConsistencyLevel;
import com.iris.modelmanager.resource.ClasspathResourceLocator;
import com.iris.modelmanager.resource.HomeDirectoryResourceLocator;
import com.iris.modelmanager.resource.ResourceLocator;

public class ManagerContext {

   public static final String CHANGELOG_DIRECTORY = "changelogs";
   private static final String PROFILES_DIRECTORY = "profiles";
   private static final String DEFAULT_CHANGELOG = "changelog-master.xml";

   
   public static class Builder {

      private final ManagerContext context = new ManagerContext();
      private String homeDirectoryStr;
      private Profile profile;

      public ManagerContext.Builder setHomeDirectory(String homeDirectory) {
         this.homeDirectoryStr = StringUtils.isBlank(homeDirectory) ? null : homeDirectory;
         return this;
      }

      public ManagerContext.Builder setProfile(Profile profile) {
         this.profile = profile;
         return this;
      }

      public ManagerContext.Builder setChangelog(String changelog) {
         context.changelog = StringUtils.isBlank(changelog) ? DEFAULT_CHANGELOG : changelog;
         return this;
      }

      public ManagerContext.Builder setRollback(boolean rollback) {
         context.operation = rollback ? Operation.ROLLBACK : Operation.UPGRADE;
         return this;
      }

      public ManagerContext.Builder setRollbackTarget(String rollbackTarget) {
         context.rollbackTarget = StringUtils.isBlank(rollbackTarget) ? null : rollbackTarget;
         return this;
      }

      public ManagerContext.Builder setAuto(boolean auto) {
         context.auto = auto;
         return this;
      }

      public ManagerContext build() throws Exception {

         validateHomeDirectory();

         ResourceLocator resourceLocator = homeDirectoryStr == null
               ? new ClasspathResourceLocator()
               : new HomeDirectoryResourceLocator(new File(homeDirectoryStr));

         context.resourceLocator = resourceLocator;   
         context.profile = profile;
         return context;
      }

      private void validateHomeDirectory() {

         if(homeDirectoryStr == null) {
            return;
         }

         File homeDirectory = new File(homeDirectoryStr);
         if(!homeDirectory.exists()) {
            throw new IllegalArgumentException("Home directory not found @ " + homeDirectory);
         }
         if(!homeDirectory.isDirectory()) {
            throw new IllegalArgumentException("Path to home directory @ " + homeDirectory + " is not a directory");
         }
         if(!homeDirectory.canRead()) {
            throw new IllegalArgumentException("Home directory @ " + homeDirectory + ", is not readable");
         }

         File changeLogPath = new File(homeDirectory, CHANGELOG_DIRECTORY);

         if(!changeLogPath.exists()) {
            throw new IllegalArgumentException("Home directory @ " + homeDirectory + " is missing the " + CHANGELOG_DIRECTORY + " directory");
         }

      }

   }

   private String changelog = DEFAULT_CHANGELOG;
   private ResourceLocator resourceLocator;
   private Profile profile;
   private Operation operation = Operation.UPGRADE;
   private String rollbackTarget;
   private boolean auto = false;

   ManagerContext() {
   }

   public String getChangelog() {
      return changelog;
   }

   public ResourceLocator getResourceLocator() {
      return resourceLocator;
   }

   public Profile getProfile() {
      return profile;
   }

   public Operation getOperation() {
      return operation;
   }

   public String getRollbackTarget() {
      return this.rollbackTarget;
   }

   public boolean isAuto() {
      return auto;
   }
}

