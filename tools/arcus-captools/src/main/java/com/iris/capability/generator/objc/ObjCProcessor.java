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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.cache.ConcurrentMapTemplateCache;
import com.iris.capability.definition.AttributeType;
import com.iris.capability.definition.AttributeType.CollectionType;
import com.iris.capability.definition.AttributeType.RawType;
import com.iris.capability.definition.CapabilityDefinition;
import com.iris.capability.definition.Definition;
import com.iris.capability.definition.ProtocolDefinition;
import com.iris.capability.definition.ServiceDefinition;

public class ObjCProcessor {
   private final static Logger logger = LoggerFactory.getLogger(ObjCProcessor.class);

   private String templateName;
   private final File outputDirectory;
   private final Handlebars handlebars;

   public ObjCProcessor(String templateName, String outputDirectoryName) throws IOException {
      handlebars =
            new Handlebars()
               .with(new ConcurrentMapTemplateCache())
               ;
      handlebars.registerHelper("equals", new Helper<String>() {
         @Override
         public CharSequence apply(String context, Options options) throws IOException {
            String param = options.param(0);
            if(StringUtils.equals(context, param)) {
               return options.fn();
            }
            return options.inverse();
         }
      });
      handlebars.registerHelper("toUpperCase", new Helper<String>() {
         @Override
         public CharSequence apply(String context, Options options) throws IOException {
            return context.toUpperCase();
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
      handlebars.registerHelper("parent", new Helper<String>() {

          @Override
          public CharSequence apply(String context, Options options) throws IOException {
              Context topParent = options.context;
              
              while (topParent.parent() != null) {
                  topParent = topParent.parent();
              }
              
              // Parent Name
              String name = StringUtils.capitalize(((Definition)topParent.model()).getName());

              switch (context) {
              	default: return name+context;
              }
          }
      });
      handlebars.registerHelper("parentUncap", new Helper<String>() {

          @Override
          public CharSequence apply(String context, Options options) throws IOException {
              Context topParent = options.context;
              
              while (topParent.parent() != null) {
                  topParent = topParent.parent();
              }
              
              // Parent Name
              String name = StringUtils.uncapitalize(((Definition)topParent.model()).getName());

              switch (context) {
              	default: return name+context;
              }
          }
      });
      handlebars.registerHelper("cleanseForSwift", new Helper<String>() {

          @Override
          public CharSequence apply(String context, Options options) throws IOException {
              // Cleans Numbers
              if (Character.isDigit(context.charAt(0))) {
                  return "_" + context;
              }

              // Cleanse Keywords With Name
              Context topParent = options.context;
              while (topParent.parent() != null) {
                  topParent = topParent.parent();
              }
              // Parent Name
              String name = StringUtils.uncapitalize(((Definition)topParent.model()).getName());

              switch (context) {
                  case "protocol": return name+"Protocol";
                  case "attributes": return name+"Attributes";
                  case "address": return name+"Address";
                  case "break" : return name+"Break";
                  case "in" : return name+"In";
                  case "default" : return name+"Default";
                  case "init" : return name+"Init";
                  default: return context;
              }
          }
      });
      handlebars.registerHelper("cleanseForSwiftParam", new Helper<String>() {

          @Override
          public CharSequence apply(String context, Options options) throws IOException {
              // Cleans Numbers
              if (Character.isDigit(context.charAt(0))) {
                  return "_" + context;
              }

              // Cleanse Keywords With Name
              Context topParent = options.context;
              while (topParent.parent() != null) {
                  topParent = topParent.parent();
              }
              // Parent Name
              String name = StringUtils.uncapitalize(((Definition)topParent.model()).getName());

              switch (context) {
                  case "break" : return name+"Break";
                  case "in" : return name+"In";
                  case "default" : return name+"Default";
                  case "init" : return name+"Init";
                  default: return context;
              }
          }
      });
      handlebars.registerHelper("enumType", new Helper<AttributeType>() {
          @Override
          public CharSequence apply(AttributeType context, Options options) throws IOException {
             RawType rawType = context.getRawType();
             switch(rawType) {
                case ENUM: return typeOfEnum(options.context.parent()); // ? These are Types such as classes?
                default: return "";
             }
          }
      });  
      handlebars.registerHelper("objCTypeOf", new Helper<AttributeType>() {
         @Override
         public CharSequence apply(AttributeType context, Options options) throws IOException {
            return typeOf(context);
         }
      });
      handlebars.registerHelper("objCTypeOfParam", new Helper<AttributeType>() {
         @Override
         public CharSequence apply(AttributeType context, Options options) throws IOException {
            return typeOfAsParam(context);
        }
       });
      handlebars.registerHelper("isIterable", new Helper<AttributeType>() {
         @Override
         public CharSequence apply(AttributeType context, Options options) throws IOException {
            if(context.getRawType() == RawType.LIST || context.getRawType() == RawType.SET) {
               return options.fn();
            }
            return options.inverse();
         }
      });
      handlebars.registerHelper("getContainedType", new Helper<AttributeType>() {
         @Override
         public CharSequence apply(AttributeType context, Options options) throws IOException {
            return getContainedTypeRepr(context);
         }
      });
      handlebars.registerHelper("isModelObject", new Helper<AttributeType>() {
         @Override
         public CharSequence apply(AttributeType context, Options options) throws IOException {
            String type = getContainedTypeRepr(context);
            switch(type) {
            case "Device":
            case "Hub":
            case "Account":
            case "Place":
            case "Person":
            case "Rule":
               return options.fn();
            default:
               return options.inverse();
            }
         }
      });
      handlebars.registerHelper("isNumeric", new Helper<AttributeType>() {
          @Override
          public CharSequence apply(AttributeType context, Options options) throws IOException {
              RawType rawType = context.getRawType();
              switch(rawType) {
              	case BOOLEAN: 
           		case DOUBLE:
           		case INT:
           		case LONG: return options.fn();
                default: return options.inverse();
              }
         }
      });
      handlebars.registerHelper("isCoercionRequired", new Helper<AttributeType>() {
         @Override
         public CharSequence apply(AttributeType context, Options options) throws IOException {
            RawType type = context.getRawType();
            switch(type) {
            case BOOLEAN:
            case BYTE:
            case DOUBLE:
            case INT:
            case LONG: return options.fn();
            default: return options.inverse();
            }
         }
      });
      handlebars.registerHelper("isBoxingRequired", new Helper<AttributeType>() {
         @Override
         public CharSequence apply(AttributeType context, Options options) throws IOException {
            RawType type = context.getRawType();
            switch(type) {
            case BOOLEAN:
            case BYTE:
            case DOUBLE:
            case INT:
            case LONG: return options.fn();
            default: return options.inverse();
            }
         }
      });
      handlebars.registerHelper("isTimestamp", new Helper<AttributeType>() {
         @Override
         public CharSequence apply(AttributeType context, Options options) throws IOException {
            RawType type = context.getRawType();
            return type == RawType.TIMESTAMP ? options.fn() : options.inverse();
         }
      });
      handlebars.registerHelper("preCoercion", new Helper<AttributeType>() {
         @Override
         public CharSequence apply(AttributeType context, Options options) throws IOException {
            RawType type = context.getRawType();
            switch(type) {
            case BYTE:
               return "(uint_8) [";
            case BOOLEAN:
            case DOUBLE:
            case INT:
            case LONG: return "[";
            case TIMESTAMP: return "[NSDate dateWithTimeIntervalSince1970:([";
            default: return "";
            }
         }
      });
      handlebars.registerHelper("postCoercion", new Helper<AttributeType>() {
         @Override
         public CharSequence apply(AttributeType context, Options options) throws IOException {
            RawType type = context.getRawType();
            switch(type) {
            case BYTE: return " unsignedCharValue]";
            case BOOLEAN: return " boolValue]";
            case DOUBLE: return " doubleValue]";
            case INT: return " intValue]";
            case TIMESTAMP: return " doubleValue] / 1000)]";
            case LONG: return " longValue]";
            default: return "";
            }
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
            ObjCProcessor.this.apply(context, options.fn, fileName);
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

   protected CharSequence typeOfEnum(Context parent) {
	   Context topParent = parent.parent();
       while (topParent.parent() != null) {
           topParent = topParent.parent();
       }
       String name = StringUtils.capitalize(((Definition)topParent.model()).getName());
       return name + StringUtils.capitalize(((Definition)parent.model()).getName());
   }
   
   protected String getContainedTypeRepr(AttributeType attributeType) {
      if(attributeType.isCollection()) {
         return ((CollectionType) attributeType).getContainedType().getRepresentation();
      }
      return attributeType.getRepresentation();
   }
    
    protected CharSequence typeOf(AttributeType attributeType) {
        // TODO handle enums better
        RawType rawType = attributeType.getRawType();
        switch(rawType) {
            case ANY: return "id";
            case ATTRIBUTES: return "NSDictionary *";
            case BOOLEAN: return "BOOL";
            case BYTE: return "uint8_t";
            case DOUBLE: return "double";
            case ENUM: return "NSString *";
            case INT: return "int";
            case LIST: return "NSArray *";
            case LONG: return "long";
            case MAP: return "NSDictionary *";
            case OBJECT: return "id";
            case SET: return "NSArray *";
            case STRING: return "NSString *";
            case TIMESTAMP: return "NSDate *";
            case VOID: return "void";
            default: return "id";
        }
    }

   protected CharSequence typeOfAsParam(AttributeType attributeType) {
      // TODO handle enums better
      RawType rawType = attributeType.getRawType();
      switch(rawType) {
         case ANY: return "id";
         case ATTRIBUTES: return "NSDictionary *";
         case BOOLEAN: return "BOOL";
         case BYTE: return "uint8_t";
         case DOUBLE: return "double";
         case ENUM: return "NSString *";
         case INT: return "int";
         case LIST: return "NSArray *";
         case LONG: return "long";
         case MAP: return "NSDictionary *";
         case OBJECT: return "id";
         case SET: return "NSArray *";
         case STRING: return "NSString *";
         case TIMESTAMP: return "double";
         case VOID: return "void";
         default: return "id";
      }
   }

   protected void applyTemplates(List<CapabilityDefinition> capabilities,
         List<ServiceDefinition> services,
         List<ProtocolDefinition> protocols) throws IOException {
      Map<String, List<? extends Definition>> definitions
         = new HashMap<String, List<? extends Definition>>();
      definitions.put("capabilities", capabilities);
      definitions.put("services", services);
      definitions.put("protocols", protocols);

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
      }
      applyTemplates(capabilities, services, protocols);
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

