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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;
import com.iris.capability.definition.CapabilityDefinition;
import com.iris.capability.definition.ObjectDefinition;
import com.iris.capability.definition.ParameterDefinition;
import com.iris.capability.definition.ServiceDefinition;

public class HTMLProcessor {
   private final static Logger logger = LoggerFactory.getLogger(HTMLProcessor.class);
   private final File outputDirectory;
   private final Handlebars handlebars;

   public HTMLProcessor(String outputDirectoryName) throws IOException {
      handlebars = new Handlebars();
      handlebars.registerHelper("returnList", new Helper<List<ParameterDefinition>>() {

         @Override
         public CharSequence apply(List<ParameterDefinition> context, Options options) throws IOException {

            if(context.size() == 0 || (context.size() == 1 && context.get(0).getType().getRepresentation().equalsIgnoreCase("attributemap"))) {
               return null;
            }

            String header = options.param(0, "Returned keys:");
            String sectionClass = options.param(1, "method-body");
            String listClass = options.param(2, "return-attributes");
            StringBuilder sb = new StringBuilder("<p class=\"").append(sectionClass).append("\">").append(header);
            sb.append("<dl class=\"").append(listClass).append("\">");
            for(ParameterDefinition param : context) {
               sb.append("<dt>")
                  .append(param.getName())
                  .append(":  ")
                  .append(StringEscapeUtils.escapeHtml4(param.getType().getRepresentation()))
                  .append("</dt>")
                  .append("<dd>")
                  .append(param.getDescription())
                  .append("</dd>");
            }
            sb.append("</dl></p>");

            return new Handlebars.SafeString(sb.toString());
         }
      });
      outputDirectory = new File(outputDirectoryName);
      if (outputDirectory.isFile()) {
         throw new IOException("Output directory already exists as file.");
      }
      if (!outputDirectory.exists()) {
         boolean madeDirectory = outputDirectory.mkdirs();
         if (!madeDirectory) {
            throw new IOException("Output directory could not be created.");
         }
      }
      if (!outputDirectory.isDirectory()) {
         throw new IOException("Could not find or create output directory [" + outputDirectoryName + "]");
      }
      if (!outputDirectory.canWrite()) {
         throw new IOException("Do not have write permissions for directory [" + outputDirectoryName + "]");
      }
   }

   private static final Comparator<? super ObjectDefinition> DEF_COMPARATOR = new Comparator<ObjectDefinition>() {
      @Override
      public int compare(ObjectDefinition o1, ObjectDefinition o2) {
         return o1.getName().compareToIgnoreCase(o2.getName());
      }
   };

   public void createDocs(List<CapabilityDefinition> capDefs, List<ServiceDefinition> serviceDefs) throws IOException {
      List<CapabilityDefinition> caps = new ArrayList<>(capDefs);
      Collections.sort(caps, DEF_COMPARATOR);
      List<ServiceDefinition> services = serviceDefs == null ? Collections.<ServiceDefinition>emptyList() : new ArrayList<>(serviceDefs);
      Collections.sort(services, DEF_COMPARATOR);

      DefinitionIndex index = new DefinitionIndex();
      for (CapabilityDefinition capDef : caps) {
         String htmlFile = createDefinitionPage(capDef);
         index.addCapability(capDef.getName(), htmlFile);
      }

      for(ServiceDefinition service : services) {
         String htmlFile = createDefinitionPage(service);
         index.addService(service.getName(), htmlFile);
      }

      if (index.getCapabilities().size() > 0 || index.getServices().size() > 0) {
         createStylesheet();
         createIndexFrame(index);
         createFrameSet(index);
      }

   }

   public void createStylesheet() throws IOException {
      BufferedReader reader = null;
      try {
         reader = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("capability.css")));
         writeFile("stylesheet/capability.css", reader);
         logger.info("Sytlesheet created");
      }
      finally {
         if (reader != null) {
            reader.close();
         }
      }
   }

   public void createFrameSet(DefinitionIndex index) throws IOException {
      try {
         Template template = handlebars.compile("capframe");
         String html = template.apply(index.getCapabilities().get(0).getHtmlFile());
         writeFile("index.html", html);
         logger.info("Frameset created");
      } catch (IOException e) {
         logger.error("Could not create fameset page");
         throw e;
      }
   }

   public void createIndexFrame(DefinitionIndex index) throws IOException {
      try {
         Template template = handlebars.compile("capindex");
         String html = template.apply(index);
         writeFile("capability-index.html", html);
         logger.info("Index frame created");
      } catch (IOException e) {
         logger.error("Could not create capability index page");
         throw e;
      }
   }

   public String createDefinitionPage(ObjectDefinition def) throws IOException {
      try {
         Template template = handlebars.compile(def instanceof CapabilityDefinition ? "capability" : "service");
         String html = template.apply(def);
         String htmlFileName = def.getName().toLowerCase() + ".html";
         writeFile(htmlFileName, html);
         logger.info("Page generated [{}]", htmlFileName);
         return htmlFileName;

      } catch (IOException e) {
         logger.error("Could not create capability file for " + def.getName(), e);
         throw e;
      }
   }

   private void writeFile(String fileName, String contents) throws IOException {
      BufferedReader reader = null;
      try {
         reader = new BufferedReader(new StringReader(contents));
         writeFile(fileName, reader);
      }
      finally {
         if (reader != null) {
            reader.close();
         }
      }
   }

   private void writeFile(String fileName, BufferedReader contents) throws IOException {
      File outputFile = new File(outputDirectory, fileName);
      if (outputFile.getParentFile() != null) {
         outputFile.getParentFile().mkdirs();
      }
      Writer writer = null;
      try {
         writer = new FileWriter(outputFile);
         String line = contents.readLine();
         while (line != null) {
            writer.write(line + "\n");
            line = contents.readLine();
         }
      }
      finally {
         if (writer != null) {
            writer.close();
         }
      }
   }
}

