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
package com.iris.capability.generator.swift;

import com.github.jknack.handlebars.*;
import com.github.jknack.handlebars.cache.ConcurrentMapTemplateCache;
import com.iris.capability.definition.*;
import com.iris.capability.definition.AttributeType.CollectionType;
import com.iris.capability.definition.AttributeType.RawType;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class SwiftProcessor {
   private final static Logger logger = LoggerFactory.getLogger(SwiftProcessor.class);

   private String templateName;
   private final File outputDirectory;
   private final Handlebars handlebars;

   public SwiftProcessor(String templateName, String outputDirectoryName) throws IOException {
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
      handlebars.registerHelper("cleanse", new Helper<String>() {

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
      handlebars.registerHelper("attributeKey", new Helper<String>() {
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
              context = name+StringUtils.capitalize(context);
              return context;
          }
      });

      handlebars.registerHelper("commandKey", new Helper<String>() {
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
              context = name+StringUtils.capitalize(context);
              return context;
          }
      });

      handlebars.registerHelper("eventKey", new Helper<String>() {
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
              context = name+StringUtils.capitalize(context);
              return context;
          }
      });

      handlebars.registerHelper("responseProperty", new Helper<String>() {
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
              String name = StringUtils.capitalize(((Definition)topParent.model()).getName());
              context = StringUtils.capitalize(context);
              return context;
          }
      });

      handlebars.registerHelper("swiftTypeOf", new Helper<AttributeType>() {
          @Override
          public CharSequence apply(AttributeType context, Options options) throws IOException {
             RawType rawType = context.getRawType();
             switch(rawType) {
                case ENUM: return typeOfEnum(options.context); // ? These are Types such as classes?
                default: return typeOf(context);
             }
          }
       });
      handlebars.registerHelper("swiftRequestTypeOf", new Helper<AttributeType>() {
          @Override
          public CharSequence apply(AttributeType context, Options options) throws IOException {
             RawType rawType = context.getRawType();
             switch(rawType) {
                case ENUM: return typeOfRequestEnum(options.context); // ? These are Types such as classes?
                default: return typeOf(context);
             }
          }
       });
      handlebars.registerHelper("swiftTypeOfParent", new Helper<AttributeDefinition>() {
           @Override
           public CharSequence apply(AttributeDefinition context, Options options) throws IOException {
              RawType rawType = context.getType().getRawType();
              switch(rawType) {
                  case ENUM: return typeOfEnum(options.context.parent()); // ? These are Types such as classes?
                  default: return typeOf(context.getType());
              }
           }
        });
      handlebars.registerHelper("swiftTypeOfParam", new Helper<AttributeType>() {
         @Override
         public CharSequence apply(AttributeType context, Options options) throws IOException {
             RawType rawType = context.getRawType();
             switch(rawType) {
                 case ENUM: return typeOfEnum(options.context); // ? These are Types such as classes?
                 default: return typeOfAsParam(context);
             }
        }
       });
      handlebars.registerHelper("swiftTypeOfRequestParam", new Helper<AttributeType>() {
          @Override
          public CharSequence apply(AttributeType context, Options options) throws IOException {
              RawType rawType = context.getRawType();
              switch(rawType) {
              	case ENUM: return "String";
                default: return typeOfAsParam(context);
              }
         }
      });
      handlebars.registerHelper("swiftTypeOfParamLegacy", new Helper<AttributeType>() {
          @Override
          public CharSequence apply(AttributeType context, Options options) throws IOException {
              RawType rawType = context.getRawType();
              switch(rawType) {
              	case ENUM: return "String";
              	case TIMESTAMP: return "Double";
                default: return typeOfAsParam(context);
              }
         }
      });
      handlebars.registerHelper("swiftTypeOfReturnLegacy", new Helper<AttributeType>() {
          @Override
          public CharSequence apply(AttributeType context, Options options) throws IOException {
              RawType rawType = context.getRawType();
              switch(rawType) {
              	case BOOLEAN:
           		case DOUBLE:
           		case INT:
           		case LONG: return "NSNumber";
              	case ENUM: return "String";
                default: return typeOfAsParam(context);
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
      handlebars.registerHelper("isEnum", new Helper<AttributeType>() {
          @Override
          public CharSequence apply(AttributeType context, Options options) throws IOException {
             if(context.getRawType() == RawType.ENUM) {
                return options.fn();
             }
             return options.inverse();
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
      handlebars.registerHelper("isCoercionRequired", new Helper<AttributeType>() {
         @Override
         public CharSequence apply(AttributeType context, Options options) throws IOException {
            RawType type = context.getRawType();
            switch(type) {
            case BOOLEAN:
            case BYTE:
            case DOUBLE:
            case INT:
            case TIMESTAMP:
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
      handlebars.registerHelper("ifNotAny", new Helper<AttributeType>() {

          @Override
          public CharSequence apply(AttributeType context, Options options) throws IOException {
              RawType type = context.getRawType();
              switch(type) {
                  case ANY:
                  case OBJECT: return options.inverse();
                  default: return options.fn();
              }
          }
      }) ;
      handlebars.registerHelper("preCoercion", new Helper<AttributeType>() {
         @Override
         public CharSequence apply(AttributeType context, Options options) throws IOException {
            RawType type = context.getRawType();
            switch(type) {
               case ATTRIBUTES:
               case LIST:
               case MAP:
               case SET: return "[";
               case TIMESTAMP: return "Date(milliseconds: ";
               default: return "";
            }
         }
      });
      handlebars.registerHelper("postCoercion", new Helper<AttributeType>() {
         @Override
         public CharSequence apply(AttributeType context, Options options) throws IOException {
            RawType type = context.getRawType();
            switch(type) {
               case ATTRIBUTES:
               case LIST:
               case MAP:
               case SET: return "]";
               case TIMESTAMP: return ")";
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
            SwiftProcessor.this.apply(context, options.fn, fileName);
            return "Created file " + fileName + "...";
         }

      });
      handlebars.registerHelper("year", new Helper() {

         @Override
         public CharSequence apply(Object context, Options options) throws IOException {
            return String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
         }
      });
      handlebars.registerHelper("today", new Helper() {

         @Override
         public CharSequence apply(Object context, Options options) throws IOException {
            DateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");
            Calendar cal = Calendar.getInstance();
            return dateFormat.format(cal.getTime());
         }
      });
      handlebars.registerHelper("closeCurly", new Helper() {

          @Override
          public CharSequence apply(Object context, Options options) throws IOException {
             return "}";
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

   protected String getContainedTypeRepr(AttributeType attributeType) {
      if(attributeType.isCollection()) {
         return ((CollectionType) attributeType).getContainedType().getRepresentation();
      }
      return attributeType.getRepresentation();
   }

    protected CharSequence typeOf(AttributeType attributeType) {
        RawType rawType = attributeType.getRawType();
        switch(rawType) {
            case ANY: return "Any";
            case BOOLEAN: return "Bool";
            case BYTE: return "UInt8";
            case DOUBLE: return "Double";
            case INT: return "Int";
            case LONG: return "Int";
            case OBJECT: return "Any";
            case STRING: return "String";
            case TIMESTAMP: return "Double";
            case VOID: return "Void";
            case ATTRIBUTES: return typeOfMap(attributeType); // ? These are defined with associated types [String: String]
            case LIST: return typeOfList(attributeType); // ? These are defined with associated type [String]
            case MAP: return typeOfMap(attributeType); // ? These are defined with associated type [String: String]
            case SET: return typeOfList(attributeType); // ? These are defined with associated type [String]
            default: return "Any";
        }
    }

   protected CharSequence typeOfAsParam(AttributeType attributeType) {
      // TODO handle enums better
      RawType rawType = attributeType.getRawType();
      switch(rawType) {
         case ANY: return "Any";
         case BOOLEAN: return "Bool";
         case BYTE: return "UInt8";
         case DOUBLE: return "Double";
         case INT: return "Int";
         case LONG: return "Int";
         case OBJECT: return "Any";
         case STRING: return "String";
         case TIMESTAMP: return "Date";
         case VOID: return "Void";
         case ATTRIBUTES: return typeOfMap(attributeType); // ? These are defined with associated types [String: String]
         case LIST: return typeOfList(attributeType); // ? These are defined with associated type [String]
         case MAP: return typeOfMap(attributeType); // ? These are defined with associated type [String: String]
         case SET: return typeOfList(attributeType); // ? These are defined with associated type [String]
         default: return "Any";
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

   protected CharSequence typeOfRequestEnum(Context parent) {
	   Context topParent = parent.parent();
       while (topParent.parent() != null) {
           topParent = topParent.parent();
       }
       return StringUtils.capitalize(((Definition)parent.model()).getName());
   }

   protected CharSequence typeOfList(AttributeType context) {
       String type = "[";
       AttributeType containedType = ((CollectionType)context).getContainedType();

       type = type + typeOf(containedType);

        return type + "]";
   }

   protected CharSequence typeOfMap(AttributeType context) {
       String type = "[String: ";
       AttributeType containedType = ((CollectionType)context).getContainedType();

        type = type + typeOf(containedType);

        return type + "]";
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

