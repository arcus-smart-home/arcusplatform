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
package com.iris.protoc.java;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.iris.protoc.ProtocGeneratorOptions;
import com.iris.protoc.ast.ConstantsNode;
import com.iris.protoc.ast.MessageNode;
import com.iris.protoc.ast.QualifiedName;
import com.iris.protoc.ast.RootNode;
import com.iris.protoc.ast.StructNode;
import com.iris.protoc.ast.ValueNode;

public class BindingGenerator extends JavaGenerator {

   @Override
   public void generate(ProtocGeneratorOptions options, RootNode ast) throws Exception {
      this.ast = ast;

      outputDir = new File(options.getOutputPath());
      mainPkgDir = new File(outputDir, options.getPackageName().replaceAll("\\.", "/"));
      mainPkgDir.mkdirs();

      if (options.getTestOutputPath() != null) {
         testDir = new File(options.getTestOutputPath());
         testPkgDir = new File(testDir, options.getPackageName().replaceAll("\\.", "/"));
         testPkgDir.mkdirs();
      }

      Set<String> bindings = new HashSet<>();
      List<Map<String, Object>> dataBindings = new ArrayList<>();

      generateConstantBindings(options.getPackageName(), ast.getConstants(), bindings);

      String pkg = null;
      for (List<QualifiedName> group : ast.getQualifiedGroups().values()) {
          if (pkg == null && group != null && !group.isEmpty()) {
             pkg = group.get(0).getPkg();
          }
          generateMessageBindings(options, group, ast.getQualified(), ast.getConstants(), bindings, dataBindings);
      }

      generateBindingsList(options.getPackageName(), pkg, bindings);

      generateDataBindings(options.getPackageName(), pkg, dataBindings);
   }

   private void generateBindingsList(String packageName, String subpackage, Set<String> bindings) throws Exception {
      Map<String, Object> bindingsContext = new HashMap<>();
      String fullPackageName = (subpackage == null) ? packageName : packageName + "." + subpackage;
      bindingsContext.put("bindings", bindings);
      bindingsContext.put("package", fullPackageName);
      bindingsContext.put("subpackage", subpackage);
      bindingsContext.put("isZcl", "zcl".equals(subpackage) || "zha".equals(subpackage) || "alertme".equals(subpackage));
      bindingsContext.put("isZdp", "zdp".equals(subpackage));

      File pkgDir = new File(outputDir, fullPackageName.replaceAll("\\.", "/"));
      pkgDir.mkdirs();

      String type = StringUtils.capitalize(subpackage);
      try (Writer out = new BufferedWriter(new FileWriter(new File(pkgDir, "Zigbee" + type + "Clusters.java")))) {
         com.github.jknack.handlebars.Template template = handlebars.compile("BindingClusters");
         template.apply(bindingsContext, out);
      }
   }

   private void generateDataBindings(String packageName, String subpackage, List<Map<String,Object>> dataBindings) throws Exception {
      Map<String, Object> bindingsContext = new HashMap<>();
      String fullPackageName = (subpackage == null) ? packageName : packageName + "." + subpackage;
      bindingsContext.put("bindings", dataBindings);
      bindingsContext.put("package", fullPackageName);
      bindingsContext.put("subpackage", subpackage);

      File pkgDir = new File(outputDir, fullPackageName.replaceAll("\\.", "/"));
      pkgDir.mkdirs();

      String type = StringUtils.capitalize(subpackage);
      try (Writer out = new BufferedWriter(new FileWriter(new File(pkgDir, "Zigbee" + type + "DataBinding.java")))) {
         com.github.jknack.handlebars.Template template = handlebars.compile("BindingData");
         template.apply(bindingsContext, out);
      }
   }

   public void generateConstantBindings(String opkg, Map<QualifiedName,ConstantsNode> consts, Set<String> bindings) throws Exception {
      for (Map.Entry<QualifiedName,ConstantsNode> entry : consts.entrySet()) {
         QualifiedName qname = entry.getKey();
         String subpackage = qname.getPkg();
         String fullPkg = (subpackage == null) ? opkg : opkg + "." + subpackage;

         File pkgDir = new File(outputDir, fullPkg.replaceAll("\\.", "/"));
         pkgDir.mkdirs();

         List<Object> constants = new ArrayList<>();
         boolean hasClusterId = false;
         for (Map.Entry<String,ValueNode> centry : entry.getValue().getConstants().entrySet()) {
            constants.add(getConstantContext(centry.getKey(), centry.getValue()));
            if (centry.getKey().equals("CLUSTER_ID")) {
               hasClusterId = true;
            }
         }

         if (hasClusterId || "zdp".equals(subpackage)) {
            bindings.add(qname.getMessage());
         }

         Map<String,Object> context = new HashMap<>();
         context.put("package", fullPkg);
         context.put("subpackage", subpackage);
         context.put("classname", qname.getMessage());
         context.put("constants", constants);
         context.put("header", ast.getHeader());
         context.put("hasClusterId", hasClusterId);

         try (Writer out = new BufferedWriter(new FileWriter(new File(pkgDir, qname.getMessage() + "Binding.java")))) {
            com.github.jknack.handlebars.Template template = handlebars.compile("Binding");
            template.apply(context, out);
         }
      }
   }

   public void generateMessageBindings(ProtocGeneratorOptions options,
         List<QualifiedName> group,
         Map<QualifiedName,StructNode> structs,
         Map<QualifiedName,ConstantsNode> consts,
         Set<String> bindings,
         List<Map<String,Object>> dataBindings) throws Exception {
      if (group == null || group.isEmpty()) {
         return;
      }

      String subpackage = group.get(0).getPkg();
      String clazz = group.get(0).getClazz();
      String fullPkg = (subpackage == null) ? options.getPackageName() : options.getPackageName() + "." + subpackage;

      List<Object> constants = new ArrayList<>();
      QualifiedName constantsName = new QualifiedName(subpackage, null, clazz);
      boolean hasClusterId = false;
      if (consts.containsKey(constantsName)) {
         ConstantsNode cnode = consts.get(constantsName);
         for (Map.Entry<String,ValueNode> entry : cnode.getConstants().entrySet()) {
            constants.add(getConstantContext(entry.getKey(), entry.getValue()));
            if (entry.getKey().equals("CLUSTER_ID")) {
               hasClusterId = true;
            }
         }
      }

      File pkgDir = new File(outputDir, fullPkg.replaceAll("\\.", "/"));
      pkgDir.mkdirs();

      Map<String,StructNode> namedStructs = new HashMap<>();
      for (QualifiedName next : group) {
         StructNode struct = structs.get(next);
         namedStructs.put(next.getMessage(), struct);
      }

      List<Map<String,Object>> structContexts = new ArrayList<>();
      for (QualifiedName next : group) {
         structContexts.add(getStructContext(fullPkg, next.getMessage(), structs.get(next)));
      }

      Set<String> groups = new HashSet<>();
      for (QualifiedName next : group) {
         StructNode struct = structs.get(next);
         if (!(struct instanceof MessageNode)) {
            continue;
         }

         MessageNode msg = (MessageNode)struct;
         String grp = msg.getParams().get("group");
         if (grp == null || grp.isEmpty()) {
            continue;
         }

         groups.add(grp);
      }

      List<Object> messages = new ArrayList<>();
      List<Object> emptyGroupMessages = getMessageGroup(clazz, namedStructs, false);
      if (emptyGroupMessages != null && !emptyGroupMessages.isEmpty()) {
         Map<String,Object> mcontext = new HashMap<>();
         mcontext.put("messages", emptyGroupMessages);
         messages.add(mcontext);
      }

      for (String grp : groups) {
         List<Object> grpMessages = getMessageGroup(grp, namedStructs, true);
         Map<String,Object> mcontext = new HashMap<>();
         mcontext.put("messages", grpMessages);
         messages.add(mcontext);
      }

      if (hasClusterId || "zdp".equals(subpackage)) {
         bindings.add(clazz);
      }

      try (Writer out = new BufferedWriter(new FileWriter(new File(pkgDir, clazz + "Binding.java")))) {
         Map<String,Object> context = new HashMap<String,Object>();
         context.put("package", fullPkg);
         context.put("subpackage", subpackage);
         context.put("header", ast.getHeader());
         context.put("classname", clazz);
         context.put("hasClusterId", hasClusterId);
         context.put("messages", messages);
         context.put("structs", structContexts);
         context.put("constants", constants);

         dataBindings.add(context);

         com.github.jknack.handlebars.Template template = handlebars.compile("Binding");
         template.apply(context, out);
      }
   }
}

