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

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;

public class Main {

   public static void main(String[] args) throws Exception {
      Options options = new Options();
      options.addOption("o", true, "The output directory for the generated files.");
      options.addOption("t", true, "The output directory for the generated test files.");
      options.addOption("p", true, "The package name to use for the generated files.");
      options.addOption("b", false, "Generate bindings");
      options.addOption("n", false, "Generate name bindings");

      CommandLineParser cliParser = new GnuParser();
      CommandLine cmd = cliParser.parse(options, args);

      String testPath = cmd.getOptionValue('t', null);
      String outputPath = cmd.getOptionValue('o', null);
      if (outputPath == null) {
         throw new IllegalArgumentException("You must specify the output directory using -o");
      }

      String packageName = cmd.getOptionValue('p', null);
      if (packageName == null) {
         throw new IllegalArgumentException("You must specify the package name using -p");
      }
      
      boolean generateBindings = cmd.hasOption('b');
      boolean generateNameBindings = cmd.hasOption('n');
      if (generateBindings && generateNameBindings) {
         throw new IllegalArgumentException("The -b and -n options are mutually exclusive");
      }

      String[] files = cmd.getArgs();
      if (files == null || files.length == 0) {
         throw new IllegalArgumentException("IRP compiler must be given one file to compile");
      }

      Set<File> source = new LinkedHashSet<File>();
      for (String file : files) {
         source.add(new File(file));
      }

      ProtocGeneratorOptions.Type type = ProtocGeneratorOptions.Type.JAVA;
      if (generateBindings) {
         type = ProtocGeneratorOptions.Type.BINDING;
      } else if (generateNameBindings) {
         type = ProtocGeneratorOptions.Type.NAMING;
      }

      ProtocGeneratorOptions genOpts = new ProtocGeneratorOptions(outputPath, packageName, testPath, type);
      IrisProtoCompiler compiler = new IrisProtoCompiler(source, genOpts);
      compiler.run();
   }
}

