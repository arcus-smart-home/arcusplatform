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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import com.iris.capability.definition.*;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;


public class BackboneViewProcessor {

    private final static Logger logger = LoggerFactory.getLogger(BackboneViewProcessor.class);

    private final File outputDirectory;
    private final Handlebars handlebars;

    public BackboneViewProcessor(String outputDirectoryName) throws IOException {
        handlebars = new Handlebars();
        handlebars.registerHelper("docs", new Helper<AttributeDefinition>() {
            @Override
            public CharSequence apply(AttributeDefinition context, Options options) throws IOException {
                StringBuilder out = new StringBuilder("\t/**");
                out.append(System.getProperty("line.separator")).append("\t * @property {").append(context.getType()).append("}");
                if (context.getType().equals("enum")) {
                    out.append(System.getProperty("line.separator")).append("\t * @enum {").append(context.getEnumValues()).append("}");
                }
                if (!context.isWritable()) {
                    out.append(System.getProperty("line.separator")).append("\t * @readonly");
                }
                out.append(System.getProperty("line.separator")).append("\t */");
                return out;
            }
        });
        handlebars.registerHelper("validate", new Helper<AttributeDefinition>() {
            @Override
            public CharSequence apply(AttributeDefinition context, Options options) throws IOException {
                StringBuilder out = new StringBuilder();
                if (context.isWritable()) {
                    out.append("if(attr.").append(context.getName()).append(" == undefined) return '").append(context.getName()).append(" is undefined';");
                } else {
                    out.append("// readonly ").append(context.getName());
                }
                return out.toString();
            }
        });
        handlebars.registerHelper("mixin", new Helper<String>() {
            @Override
            public CharSequence apply(String context, Options options) throws IOException {
                StringBuilder out = new StringBuilder();
                String[] strings = StringUtils.split(context, ',');
                for (String s : strings) {
                    out.append(s);
                }
                return out.toString();
            }
        });
        handlebars.registerHelper("values", new Helper<AttributeDefinition>() {
            @Override
            public CharSequence apply(AttributeDefinition context, Options options) throws IOException {
                return new Handlebars.SafeString(typeOf(context));
            }
        });

        handlebars.registerHelper("type", new Helper<AttributeDefinition>() {

            @Override
            public CharSequence apply(AttributeDefinition context, Options options) throws IOException {
                return parsedType(context);
            }
        });
        handlebars.registerHelper("cleandescription", new Helper<AttributeDefinition>() {
            @Override
            public CharSequence apply(AttributeDefinition context, Options options) throws IOException {
                String returnVal = context.getDescription().toString();
                return new Handlebars.SafeString(StringEscapeUtils.escapeEcmaScript(returnVal));
            }
        });
        handlebars.registerHelper("supportingdata", new Helper<AttributeDefinition>() {
            @Override
            public CharSequence apply(AttributeDefinition context, Options options) throws IOException {
                StringBuilder out = new StringBuilder();

                boolean hasMin = !context.getMax().isEmpty();
                boolean hasUnit = !context.getUnit().isEmpty();
                boolean hasRange = !context.getRange().isEmpty();
                boolean carriage = false;

                if (hasUnit) {
                    out.append(", unit: '" + context.getUnit() + "'");
                    carriage = true;
                }
                if (hasRange) {
                    if (carriage) {
                        out.append("\n\t\t");
                    }
                    out.append(", range: '" + context.getRange() + "'");
                }
                if (hasMin) {
                    if (carriage) {
                        out.append("\n\t\t");
                    }
                    out.append(", min: '" + context.getMin() + "'");
                    out.append("\n\t\t");
                    out.append(", max: '" + context.getMax() + "'");

                }

                return new Handlebars.SafeString(out.toString());
            }
        });

        handlebars.registerHelper("readwrite", new Helper<AttributeDefinition>() {
            @Override
            public CharSequence apply(AttributeDefinition context, Options options) throws IOException {
                StringBuilder out = new StringBuilder();
                if (context.isWritable()) {
                    out.append("true");
                } else
                    out.append("false");
                return out.toString();
            }

        });

        handlebars.registerHelper("requires", new Helper<List<String>>() {
            @Override
            public CharSequence apply(List<String> context, Options options) throws IOException {
                StringBuilder out = new StringBuilder();
                boolean comma = false;
                for (String name : context) {

                    if (comma) {
                        out.append(",");
                    } else {
                        comma = true;
                    }
                    out.append("require('./" + name + "') ");
                    out.append("\n\t");
                }
                return new Handlebars.SafeString(out.toString());
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


    protected CharSequence parsedType(AttributeDefinition attributeDef) {
        return attributeDef.getType().getRepresentation();

    }


    protected CharSequence typeOf(AttributeDefinition attributeDef) {
        StringBuilder out = new StringBuilder();
        if (attributeDef.getType().isEnum()) {
            out.append("\n\t\t");
            out.append(", values: ");
            out.append("[ ");
            String s;
            int size = attributeDef.getEnumValues().size();
            for (int i = 0; i < size; i++) {
                s = attributeDef.getEnumValues().get(i);

                out.append("'" + s + "'");

                if (i + 1 != size) {
                    out.append(",");
                }
            }
            out.append(" ] ");

        }


        return out.toString();

    }

    public void create(List<Definition> definitions) throws IOException {
        List<CapabilityDefinition> capabilities = new ArrayList<>();
        List<ServiceDefinition> services = new ArrayList<>();
        for (Definition definition : definitions) {
            if (definition instanceof CapabilityDefinition) {
                capabilities.add((CapabilityDefinition) definition);
            } else if (definition instanceof ServiceDefinition) {
                services.add((ServiceDefinition) definition);
            }
        }
        if (!capabilities.isEmpty()) {
            Template template = handlebars.compile("js/View");
            Template arrayTemplate = handlebars.compile("js/RequiresArray");
            Template npmrcTemplate = handlebars.compile("js/npmrc");
            String destDir = "/i2-capabilities/";
            List<String> capabilitiesList = new ArrayList();

            for (CapabilityDefinition definition : capabilities) {

                apply(definition, template, destDir + "lib/" + definition.getName() + ".js");
                capabilitiesList.add("lib/" + definition.getName());
            }

            apply(capabilitiesList, arrayTemplate, destDir + "index.js");
            apply("", npmrcTemplate, destDir + ".npmrc");
        }

        if (!services.isEmpty()) {
            for (ServiceDefinition definition : services) {
                System.out.println(definition.getName());
            }

        }
    }

    public String apply(Object context, Template template, String fileName) throws IOException {
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
        } finally {
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
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
}

