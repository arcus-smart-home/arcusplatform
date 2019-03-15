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
package com.iris.capability.generator.java;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.iris.capability.definition.Definition;
import com.iris.capability.definition.MergeableDefinition;
import com.iris.capability.reader.GenericDefinitionReader;

public class Generator {
   public final static Logger logger = LoggerFactory.getLogger(Generator.class);
   
   public static class Arguments {
      @Parameter(names={ "-i", "--input"}, description="Input path for capability definition xml files", variableArity =  true, required = true)
      private List<String> input;
      
      @Parameter(names={"-t", "--template"}, description="Name of a template to execute, template folder must be on the classpath", arity=1, required=true)
      private String template;
      
      @Parameter(names={ "-o", "--output"}, description="Output path for capability java classes", arity=1, required=true)
      private String output;
   }
   
   public static void main(String[] args) {
      Arguments arguments = new Arguments();
      JCommander jc = new JCommander(arguments);
      jc.setAcceptUnknownOptions(true);
      jc.parse(args);
      
      GenericDefinitionReader capReader = new GenericDefinitionReader();
      Map<String, Definition> definitionMap = new HashMap<>();
      try {
         for(String input : arguments.input) {
            List<Definition> definitions = capReader.readDefinitionsFromPath(input);
            for(Definition definition : definitions) {
               Definition d = definitionMap.get(definition.getName());
               if(d != null && d instanceof MergeableDefinition) {
                  if(!d.getClass().equals(definition.getClass())) {
                     throw new RuntimeException("definitions with the same name " + definition.getName() + " but differing types (" + definition.getClass() + "/" + d.getClass() + ") cannot be merged.");
                  }
                  d = ((MergeableDefinition)d).merge(definition);
               } else {
                  d = definition;
               }
               definitionMap.put(definition.getName(), d);
            }
         }
      } catch (IOException e) {
         logger.error("Unable to read capability definitions");
         e.printStackTrace();
         System.exit(-1);
      }

      try {
         JavaProcessor processor = new JavaProcessor(arguments.template, arguments.output);
         processor.createClasses(new LinkedList<>(definitionMap.values()));
      } catch (IOException e) {
         logger.error("Could not create HTML documentation!");
         e.printStackTrace();
         System.exit(-1);
      }
   }
}

