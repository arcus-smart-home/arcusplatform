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
package com.iris.capability.generator.html;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.iris.capability.definition.CapabilityDefinition;
import com.iris.capability.definition.ServiceDefinition;
import com.iris.capability.reader.CapabilityReader;
import com.iris.capability.reader.ServiceReader;

public class HtmlGenerator {
   public final static Logger logger = LoggerFactory.getLogger(HtmlGenerator.class);

   public static class Arguments {
      @Parameter(names={ "-i", "--input"}, description="Input path for capability definiton xml files", arity=1, required=true)
      private String input;

      @Parameter(names={"-s", "--services"}, description="Input path for service definition xml files", arity=1, required=false)
      private String services;

      @Parameter(names={ "-o", "--output"}, description="Output path for capabilty html documentation", arity=1, required=true)
      private String output;
   }

   public static void main(String[] args) {
      Arguments arguments = new Arguments();
      JCommander jc = new JCommander(arguments);
      jc.setAcceptUnknownOptions(true);
      jc.parse(args);

      CapabilityReader capReader = new CapabilityReader();
      List<CapabilityDefinition> capDefs = null;
      try {
         capDefs = capReader.readDefinitionsFromPath(arguments.input);
      } catch (IOException e) {
         logger.error("Unable to read capability definitions");
         e.printStackTrace();
         System.exit(-1);

      }

      List<ServiceDefinition> servDefs = null;

      if(arguments.services != null) {
         ServiceReader servReader = new ServiceReader();
         try {
            servDefs = servReader.readDefinitionsFromPath(arguments.services);
         } catch(IOException e) {
            logger.error("Unable to read service definitions");
            e.printStackTrace();
            System.exit(-1);
         }
      }

      HTMLProcessor htmlProcessor;
      try {
         htmlProcessor = new HTMLProcessor(arguments.output);
         htmlProcessor.createDocs(capDefs, servDefs);
      } catch (IOException e) {
         logger.error("Could not create HTML documentation!");
         e.printStackTrace();
         System.exit(-1);
      }


   }
}

