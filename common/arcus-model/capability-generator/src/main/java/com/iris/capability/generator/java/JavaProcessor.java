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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.cache.ConcurrentMapTemplateCache;
import com.iris.capability.definition.AttributeType;
import com.iris.capability.definition.AttributeTypes;
import com.iris.capability.definition.CapabilityDefinition;
import com.iris.capability.definition.Definition;
import com.iris.capability.definition.ProtocolDefinition;
import com.iris.capability.definition.ServiceDefinition;
import com.iris.capability.definition.TypeDefinition;

public class JavaProcessor {
   private final static Logger logger = LoggerFactory.getLogger(JavaProcessor.class);

   private String templateName;
   private final File outputDirectory;
   private final Handlebars handlebars;

   public JavaProcessor(String templateName, String outputDirectoryName) throws IOException {
      handlebars = 
            new Handlebars()
               .with(new ConcurrentMapTemplateCache())
               ;
      handlebars.registerHelper("toUpperCase", new Helper<String>() {
         @Override
         public CharSequence apply(String context, Options options) throws IOException {
            return context.toUpperCase();
         }
      });
      handlebars.registerHelper("toLowerCase", new Helper<String>() {
         @Override
         public CharSequence apply(String context, Options options) throws IOException {
            return context.toLowerCase();
         }
      });
      handlebars.registerHelper("capitalize", new Helper<String>() {
         @Override
         public CharSequence apply(String context, Options options) throws IOException {
            return StringUtils.capitalize(context);
         }
      });
      handlebars.registerHelper("uncapitalize", new Helper<String>() {
         @Override
         public CharSequence apply(String context, Options options) throws IOException {
            return StringUtils.uncapitalize(context);
         }
      });
      handlebars.registerHelper("toConstantName", new Helper<String>() {
         @Override
         public CharSequence apply(String context, Options options) throws IOException {
            return context.toUpperCase().replaceAll("[\\W]", "_");
         }
      });
      handlebars.registerHelper("javaTypeOf", new Helper<AttributeType>() {
         @Override
         public CharSequence apply(AttributeType context, Options options) throws IOException {
            return typeOf(context);
         }
      });
      handlebars.registerHelper("toGetter", new Helper<String>() {
         @Override
         public CharSequence apply(String context, Options options) throws IOException {
            return "get" + StringUtils.capitalize(context) + "()";
         }
      });
      handlebars.registerHelper("packageOf", new Helper<CapabilityDefinition>() {

         @Override
         public CharSequence apply(CapabilityDefinition context, Options options)
               throws IOException {
            if(StringUtils.isEmpty(context.getEnhances()) || "Base".equals(context.getEnhances())) {
               return packageOf(context.getName());
            }
            return packageOf(context.getEnhances());
         }
         
      });
      handlebars.registerHelper("length", new Helper<Collection<?>>() {
         @Override
         public CharSequence apply(Collection<?> context, Options options) throws IOException {
            return String.valueOf(((Collection<?>)context).size());
         }
      });
      handlebars.registerHelper("hashsize", new Helper<Collection<?>>() {
         @Override
         public CharSequence apply(Collection<?> context, Options options) throws IOException {
            int hs;
            int size = context.size();
            if (size < 3) {
               hs = size + 1;
            } else if (size < 1073741824) { // 2 ^ 30
               hs = (int) ((float)size/0.75f + 1.0f);
            } else {
               hs = Integer.MAX_VALUE;
            }

            return String.valueOf(hs);
         }
      });
      handlebars.registerHelper("file", new Helper() {

         @Override
         public CharSequence apply(Object context, Options options) throws IOException {
            if(options.params.length != 1 || !(options.params[0] instanceof String)) {
               throw new IllegalArgumentException("#file requires one and only one file name as an argument.\nShould be use like {{#javaFile . 'someFile.java'}}");
            }
            String fileNameTpl = (String) options.params[0];
            String fileName = String.valueOf(handlebars.compileInline(fileNameTpl).apply(context));
            JavaProcessor.this.apply(context, options.fn, fileName);
            return "Created file " + fileName + "...";
         }
         
      });
      handlebars.registerHelper("typeRepresentationToJavaType", new Helper<String>() {
         @Override
         public CharSequence apply(String context, Options options) throws IOException {
            return typeOf(AttributeTypes.parse(context));
         }
      });
      
      this.templateName = templateName;
      
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

   protected String packageOf(String capabilityName) {
      switch(capabilityName) {
      case "Device":
         return "dev";
      case "Hub":
         return "hub";
      case "Subsystem":
         return "subs";
      default:
         // TODO add base types for these?
         if(capabilityName.startsWith("Support")) {
            return "support";
         }
         else {
            return "serv";
         }
      }
   }
   
   protected CharSequence typeOf(AttributeType attributeType) {
      // TODO handle enums better
      if(attributeType.isEnum()) {
         return "java.lang.String";
      }
      String javaTypeName = attributeType.getJavaType().toString();
      if(javaTypeName.startsWith("class ")) {
         return javaTypeName.substring("class ".length());
      }
      return javaTypeName;
   }
   
   protected void applyTemplates(
         List<CapabilityDefinition> capabilities, 
         List<ServiceDefinition> services,
         List<ProtocolDefinition> protocols,
         List<TypeDefinition> types
   ) throws IOException {
      Map<String, List<? extends Definition>> definitions 
         = new HashMap<String, List<? extends Definition>>();
      definitions.put("capabilities", capabilities);
      definitions.put("services", services);
      definitions.put("protocols", protocols);
      definitions.put("types", types);
            
      Template template = handlebars.compile(templateName);
      OutputStreamWriter writer = new OutputStreamWriter(System.out);
      try {
         template.apply(definitions, writer);
         writer.flush();
      }
      finally {
         writer.close();
      }
   }

   public void createClasses(List<Definition> definitions) throws IOException {
      List<CapabilityDefinition> capabilities = new ArrayList<>();
      List<ServiceDefinition> services = new ArrayList<>();
      List<ProtocolDefinition> protocols = new ArrayList<>();
      // TODO should this include the static/primitive types?
      List<TypeDefinition> types = new ArrayList<>();
      for(Definition definition: definitions) {
         if(definition instanceof CapabilityDefinition) {
            capabilities.add((CapabilityDefinition) definition);
         }
         else if(definition instanceof ServiceDefinition) {
            services.add((ServiceDefinition) definition);
         }
         else if(definition instanceof ProtocolDefinition) {
            protocols.add((ProtocolDefinition) definition);
         }
         else if(definition instanceof TypeDefinition) {
            types.add((TypeDefinition) definition);
         }
      }
      applyTemplates(capabilities, services, protocols, types);
   }

   public String apply(Object context, Template template, String fileName)
         throws IOException {
            try {
               String java = template.apply(context);
               writeFile(fileName, java);
               logger.info("Class generated [{}]", fileName);
               return fileName;
               
            } catch (IOException e) {
               logger.error("Could not create capability file for " + context, e);
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

