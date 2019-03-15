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
package com.iris.modelmanager.changelog.deserializer;

public class Constants {

   private Constants() {
   }

   public static class Elements {
      public static final String CHANGELOG = "changelog";
      public static final String CHANGESET = "changeset";
      public static final String DESCRIPTION = "description";
      public static final String TRACKING = "tracking";
      public static final String UPDATE = "update";
      public static final String CQL = "cql";
      public static final String ROLLBACK = "rollback";
      public static final String IMPORT = "import";
      public static final String JAVA = "java";
   }

   public static class Attributes {
      public static final String VERSION = "version";
      public static final String AUTHOR = "author";
      public static final String IDENTIFIER = "identifier";
      public static final String FILE = "file";
      public static final String CLASS = "class";
   }
}

