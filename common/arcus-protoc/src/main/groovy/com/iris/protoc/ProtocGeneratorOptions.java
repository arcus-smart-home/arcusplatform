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
package com.iris.protoc;

public class ProtocGeneratorOptions {
   public static enum Type { JAVA, BINDING, NAMING }

   private final String outputPath;
   private final String packageName;
   private final String testPath;
   private final Type type;

   public ProtocGeneratorOptions(String outputPath, String packageName, String testPath, boolean bindings) {
      this(outputPath, packageName, testPath, bindings ? Type.BINDING : Type.JAVA);
   }

   public ProtocGeneratorOptions(String outputPath, String packageName, String testPath, Type type) {
      this.outputPath = outputPath;
      this.packageName = packageName;
      this.testPath = testPath;
      this.type = type;
   }

   public String getOutputPath() {
      return outputPath;
   }

   public String getPackageName() {
      return packageName;
   }

   public String getTestOutputPath() {
      return testPath;
   }

   public Type getType() {
      return type;
   }
}

