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
package com.iris.driver.reflex.generator;

import java.util.List;

import com.beust.jcommander.Parameter;

public class ReflexGeneratorOptions {
   @Parameter(description="files...", required=true)
   private List<String> inputFiles;

   @Parameter(names={"--output", "-o"}, description="The output file to produce.", required=true)
   private String outputFile;

   @Parameter(names={"--version", "-v"}, description="The version number to embed in the database.", required=true)
   private String version;

   @Parameter(names={"--verbose"}, description="Output more verbose information during reflex generation", required=false)
   private boolean verbose = false;

   @Parameter(names={"--help"}, description="Show this help", required=false)
   private boolean help = false;

   public List<String> getInputFiles() {
      return inputFiles;
   }

   public void setInputFiles(List<String> inputFiles) {
      this.inputFiles = inputFiles;
   }

   public String getOutputFile() {
      return outputFile;
   }

   public void setOutputFile(String outputFile) {
      this.outputFile = outputFile;
   }

   public String getVersion() {
      return version;
   }

   public void setVersion(String version) {
      this.version = version;
   }

   public boolean isVerbose() {
      return verbose;
   }

   public void setVerbose(boolean verbose) {
      this.verbose = verbose;
   }

   public boolean isHelp() {
      return help;
   }

   public void setHelp(boolean help) {
      this.help = help;
   }
}

