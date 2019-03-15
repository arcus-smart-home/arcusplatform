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
package com.iris.protocol.ipcd.definition.generator;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.iris.protocol.ipcd.adapter.context.AdapterContext;
import com.iris.protocol.ipcd.adapter.reader.AdapterReader;

public class AosGenerator {
public final static Logger logger = LoggerFactory.getLogger(AosGenerator.class);
   
   public static class Arguments {
      @Parameter(names={ "-i", "--input"}, description="Input path for adapter definiton xml files", arity=1, required=true)
      private String input;
      
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
      
      String path = System.getProperty("user.dir");
      
      File input = arguments.input.startsWith("/") ? new File(arguments.input) : new File(path, arguments.input);
      File output = arguments.output.startsWith("/") ? new File(arguments.output) : new File(path, arguments.output);
      
      AdapterReader bindingReader = new AdapterReader();
      AdapterContext context = bindingReader.readBindings(input);
            
      try {
         JavaProcessor processor = new JavaProcessor(arguments.template, output.getAbsolutePath());
         processor.createClasses(context);
      } catch (IOException e) {
         logger.error("Could not create ipcd aos adapter classes!");
         e.printStackTrace();
         System.exit(-1);
      }
   }
}

