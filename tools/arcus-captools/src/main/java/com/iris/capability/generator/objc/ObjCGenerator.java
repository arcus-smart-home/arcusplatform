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
package com.iris.capability.generator.objc;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.iris.capability.definition.Definition;
import com.iris.capability.reader.GenericDefinitionReader;

public class ObjCGenerator {
   public final static Logger logger = LoggerFactory.getLogger(ObjCGenerator.class);

   public static class Arguments {
      @Parameter(names={ "-i", "--input"}, description="Input path for capability and service definiton xml files", arity=1, required=true)
      private String input;

      @Parameter(names={"-t", "--template"}, description="Name of a template to execute, template folder must be on the classpath", arity=1, required=true)
      private String template;

      @Parameter(names={ "-o", "--output"}, description="Output path for capability and service Objective-C files", arity=1, required=true)
      private String output;
   }

   public static void main(String[] args) throws Exception {
      Arguments arguments = new Arguments();
      JCommander jc = new JCommander(arguments);
      jc.setAcceptUnknownOptions(true);
      jc.parse(args);

      List<String> paths = Arrays.asList(arguments.input + "/capability", arguments.input + "/service");

      try {
         for(String path : paths) {
            List<Definition> definitions = readDefinitions(path);
            generateSource(arguments.template, arguments.output, definitions);
         }
      } catch(Exception e) {
         logger.error("Failed to read or generate source!", e);
         throw e;
      }
   }

   private static List<Definition> readDefinitions(String path) throws Exception {
      GenericDefinitionReader capReader = new GenericDefinitionReader();
      return capReader.readDefinitionsFromPath(path);
   }

   private static void generateSource(String template, String output, List<Definition> definitions) throws Exception {
      ObjCProcessor processor = new ObjCProcessor(template, output);
      processor.createClasses(definitions);
   }
}

