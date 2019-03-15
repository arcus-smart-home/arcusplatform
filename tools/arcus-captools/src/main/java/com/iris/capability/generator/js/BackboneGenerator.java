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
package com.iris.capability.generator.js;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.iris.capability.definition.Definition;
import com.iris.capability.reader.GenericDefinitionReader;

/**
 * Created by markdeterman on 3/31/15.
 */
public class BackboneGenerator {
   public final static Logger logger = LoggerFactory.getLogger(BackboneGenerator.class);

   public static class Arguments {
      @Parameter(names={ "-i", "--input"}, description="Input path for capability definiton xml files", arity=1, required=true)
      private String input;

      @Parameter(names={ "-o", "--output"}, description="Output path for capability java classes", arity=1, required=true)
      private String output;
   }

   public static void main(String[] args) {
      Arguments arguments = new Arguments();
      JCommander jc = new JCommander(arguments);
      jc.setAcceptUnknownOptions(true);
      jc.parse(args);

      GenericDefinitionReader capReader = new GenericDefinitionReader();
      List<Definition> definitions = null;
      try {
         definitions = capReader.readDefinitionsFromPath(arguments.input);
      } catch (IOException e) {
         logger.error("Unable to read capability definitions");
         e.printStackTrace();
         System.exit(-1);
      }

      BackboneViewProcessor processor;

      try {
         processor = new BackboneViewProcessor(arguments.output);
         processor.create(definitions);
      } catch (IOException e) {
         logger.error("Could not create Backbone Models!");
         e.printStackTrace();
         System.exit(-1);
      }

   }
}

