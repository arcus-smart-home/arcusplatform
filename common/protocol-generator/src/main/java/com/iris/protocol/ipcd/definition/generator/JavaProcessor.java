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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;

public class JavaProcessor {
   private final static Logger logger = LoggerFactory.getLogger(JavaProcessor.class);

   private String templateName;
   private final File outputDirectory;
   private final Handlebars handlebars;

   public JavaProcessor(String templateName, String outputDirectoryName) throws IOException {
      handlebars = 
            new Handlebars()
               //.with(new ConcurrentMapTemplateCache())
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
      handlebars.registerHelper("toGetter", new Helper<String>() {
         @Override
         public CharSequence apply(String context, Options options) throws IOException {
            return "get" + StringUtils.capitalize(context) + "()";
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
   
   public void createClasses(Object context) throws IOException {
      Template template = handlebars.compile(templateName);
      OutputStreamWriter writer = new OutputStreamWriter(System.out);
      try {
         template.apply(context, writer);
         writer.flush();
      }
      finally {
         writer.close();
      }
   }

   public String apply(Object context, Template template, String fileName)
         throws IOException {
            try {
               String java = template.apply(context);
               writeFile(fileName, java);
               logger.info("Class generated [{}]", fileName);
               return fileName;
               
            } catch (IOException e) {
               logger.error("Could not create java file for " + context, e);
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

