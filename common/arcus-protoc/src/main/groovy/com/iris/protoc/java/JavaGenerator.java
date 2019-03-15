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
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.github.jknack.handlebars.EscapingStrategy;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.cache.ConcurrentMapTemplateCache;
import com.google.common.base.CaseFormat;
import com.iris.protoc.ProtocGenerator;
import com.iris.protoc.ProtocGeneratorOptions;
import com.iris.protoc.ast.ArrayTypeNode;
import com.iris.protoc.ast.ConstantsNode;
import com.iris.protoc.ast.FieldNode;
import com.iris.protoc.ast.FixedArrayTypeNode;
import com.iris.protoc.ast.MessageNode;
import com.iris.protoc.ast.PrimitiveTypeNode;
import com.iris.protoc.ast.QualifiedName;
import com.iris.protoc.ast.RootNode;
import com.iris.protoc.ast.SizedArrayTypeNode;
import com.iris.protoc.ast.StructNode;
import com.iris.protoc.ast.TypeNode;
import com.iris.protoc.ast.UserTypeNode;
import com.iris.protoc.ast.ValueNode;
import com.iris.protoc.ast.VariableArrayTypeNode;

public class JavaGenerator implements ProtocGenerator {
   protected File outputDir;
   protected File testDir;
   protected File mainPkgDir;
   protected File testPkgDir;
   protected RootNode ast;
   protected String tab = "";

   protected final Handlebars handlebars;

   public JavaGenerator() {
      handlebars = new Handlebars().prettyPrint(true).with(new ConcurrentMapTemplateCache()).with(new EscapingStrategy() {
         @Override
         public String escape(CharSequence value) {
            return value.toString();
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
   }

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

      generateConstants(options.getPackageName(), ast.getConstants());
      generateStructs(options.getPackageName(), ast.getStructs(), false);
      generateStructs(options.getPackageName(), ast.getMessages(), true);

      for (List<QualifiedName> group : ast.getQualifiedGroups().values()) {
         generateStructGroup(options, group, ast.getQualified(), ast.getConstants());
      }

      for (String group : ast.getGroupings()) {
         generateGrouping(options.getPackageName(), group, ast.getMessages());
      }
   }

   protected Map<String,Object> getFieldContext(String fieldname, FieldNode field, List<String> previousFields) {
      Map<String,Object> context = new HashMap<String,Object>();
      context.put("name", fieldname);
      context.put("type", type(field));
      context.put("vartype", vartype(field));
      context.put("comment", typeComment(field));
      context.put("when", field.getWhen());
      context.put("methodName", lowerCamelToUpperCamel(fieldname));

      context.put("maxSize", fieldMaxSize(field));
      context.put("minSize", fieldMinSize(field));
      context.put("byteSize", fieldByteSize(field));
      context.put("byteSizeExtra", fieldByteSizeExtra(field));
      context.put("extraSize", field.getExtraSize());
      context.put("byteOffset", fieldByteOffset(field,previousFields));
      context.put("byteSizeContext", fieldByteSizeContext(field));

      context.put("getterType", getterType(field));
      context.put("getterValue", getterValue(field));
      context.put("hasRawGetter", hasRawGetter(field));
      context.put("rawGetterValue", rawGetterValue(field));

      context.put("hasExtraSetter", hasExtraSetter(field));
      context.put("hasFixedArraySetter", hasFixedArraySetter(field));
      context.put("hasSizedArraySetter", hasSizedArraySetter(field));
      context.put("isPayload", isPayload(field));

      context.put("equalToTest", equalToTest(field));
      context.put("printField", printField(field));
      context.put("computeHashCode", hashCodeField(field));
      context.put("assign", assignField(field));
      context.put("assignFixedArray", assignFixedArrayField(field));
      context.put("assignSizedArray", assignSizedArrayField(field));
      context.put("validateField", validateField(field));
      context.put("validateDefault", validateDefault(field));

      if (field.getEncoding() != null) {
         context.put("ioEncoding", field.getEncoding() + ";");
         context.put("nioEncoding", field.getEncoding() + ";");
         context.put("nettyEncoding", field.getEncoding() + ";");
      } else {
         context.put("ioEncoding", ioEncode(field));
         context.put("nioEncoding", nioEncode(field));
         context.put("nettyEncoding", nettyEncode(field));
      }

      if (field.getDecoding() != null) {
         String decoding = fieldname + " = " + field.getDecoding() + ";";
         context.put("ioDecoding", decoding);
         context.put("nioDecoding", decoding);
         context.put("nettyDecoding", decoding);
      } else {
         String decoding = ioDecodePrefix(field);
         decoding += " " + fieldname + " = ";
         decoding += ioDecode(field) + ";";
         decoding += ioDecodeExtra(field);
         context.put("ioDecoding", decoding);

         decoding = nioDecodePrefix(field);
         decoding += " " + fieldname + " = ";
         decoding += nioDecode(field) + ";";
         decoding += nioDecodeExtra(field);
         context.put("nioDecoding", decoding);

         decoding = nettyDecodePrefix(field);
         decoding += " " + fieldname + " = ";
         decoding += nettyDecode(field) + ";";
         decoding += nettyDecodeExtra(field);
         context.put("nettyDecoding", decoding);
      }

      context.put("ioDecodeDefault", fieldname + " = " + ioDecodeDefault(field) + ";");
      context.put("nioDecodeDefault", fieldname + " = " + nioDecodeDefault(field) + ";");
      context.put("nettyDecodeDefault", fieldname + " = " + nettyDecodeDefault(field) + ";");
      context.put("random", field.getRandom());
      context.put("randomValue", randomValue(field));
      context.put("randomExtra", randomValueExtra(field));

      return context;
   }

   protected Map<String,Object> getConstantContext(String constname, ValueNode value) {
      Map<String,Object> context = new HashMap<String,Object>();
      context.put("name", constname);
      context.put("type", type(value));
      context.put("value", value(value));

      return context;
   }

   protected Map<String,Object> getStructContext(String pkgname, String classname, StructNode struct) {
      boolean isMessage = (struct instanceof MessageNode);

      Map<String,Object> context = new HashMap<String,Object>();
      context.put("classname", classname);
      context.put("superclass", isMessage ? "com.iris.protoc.runtime.ProtocMessage" : "com.iris.protoc.runtime.ProtocStruct");
      context.put("message", isMessage);
      context.put("minLength", structMinSize(struct));
      context.put("maxLength", structMaxSize(struct));
      context.put("hashCodeHeader", hashCodeHeader(pkgname, classname));
      context.put("hashCodeFooter", hashCodeFooter());

      if (isMessage) {
         Map<String,String> params = ((MessageNode)struct).getParams();
         String msgId = params.get("id");
         if (msgId != null && msgId.trim().length() != 0) {
            context.put("messageId", msgId.trim());
            context.put("hasMessageId", true);
         } else {
            context.put("messageId", "0");
            context.put("hasMessageId", false);
         }
         String group = params.get("group");
         if (group != null && group.trim().length() != 0) {
            context.put("group", group);
            context.put("isServer", "server".equals(group));
            context.put("isClient", "client".equals(group));
            context.put("isGeneral", "general".equals(group));
         }
         else {
            context.put("isServer", false);
            context.put("isClient", false);
            context.put("isGeneral", true);
            context.put("group", "");
         }
      }

      boolean constFirst = true;
      List<Object> consts = new ArrayList<>();
      for (Map.Entry<String,ValueNode> cnext : struct.getConstants().entrySet()) {
         String key = cnext.getKey();
         Map<String,Object> constContext = getConstantContext(key, cnext.getValue());
         constContext.put("first", constFirst);
         constContext.put("comma", constFirst ? "" : ",");
         constContext.put("semi", constFirst ? "" : ";");

         consts.add(constContext);
         constFirst = false;
      }

      boolean fieldsFirst = true;
      boolean storedFirst = true;
      List<Object> fields = new ArrayList<>();


      String lastField = null;
      List<String> previous = new ArrayList<>();
      for (Map.Entry<String,FieldNode> snext : struct.getFields().entrySet()) {
         String key = snext.getKey();
         boolean isStored = isFieldStored(snext.getValue());

         Map<String,Object> fieldContext = getFieldContext(key, snext.getValue(), previous);
         lastField = key;

         fieldContext.put("first", fieldsFirst);
         fieldContext.put("comma", fieldsFirst ? "" : ",");
         fieldContext.put("storedComma", (!isStored || storedFirst) ? "" : ",");
         fieldContext.put("semi", fieldsFirst ? "" : ";");
         fieldContext.put("stored", isStored);

         fields.add(fieldContext);

         storedFirst = isStored ? false : storedFirst;
         fieldsFirst = false;
         previous.add(key);
      }

      context.put("constants", consts);
      context.put("fields", fields);
      context.put("lastfield", lastField);
      return context;
   }

   protected List<Object> getMessageGroup(String group, Map<String,? extends StructNode> structs, boolean groupNotEmpty) {
      Map<String,String> known = new HashMap<>();
      List<Object> messages = new ArrayList<>();
      for (Map.Entry<String,? extends StructNode> mnext : structs.entrySet()) {
         StructNode struct = mnext.getValue();
         if (!(struct instanceof MessageNode)) {
            continue;
         }

         MessageNode msg = (MessageNode)struct;
         Map<String,String> params = msg.getParams();

         String msgId = params.get("id");
         if (msgId == null || msgId.trim().length() == 0) {
            continue;
         }

         String msgGroup = params.get("group");
         if (groupNotEmpty) {
            if (!group.equals(msgGroup)) {
               continue;
            }
         } else {
            if (msgGroup != null && !msgGroup.isEmpty()) {
               continue;
            }
         }

         String id = msgId.trim();
         String name = mnext.getKey();
         if (known.containsKey(id)) {
            String other = known.get(id);
            throw new IllegalStateException("duplicate message id '" + msgId.trim() + "' in group " + group + " caused by: " + name + " and " + other);
         }

         known.put(id, name);

         HashMap<String,Object> gcontext = new HashMap<>();
         gcontext.put("id", id);
         gcontext.put("classname", name);

         messages.add(gcontext);
      }

      return messages;
   }

   public void generateStructGroup(ProtocGeneratorOptions options, List<QualifiedName> group, Map<QualifiedName,StructNode> structs, Map<QualifiedName,ConstantsNode> consts) throws Exception {
      if (group == null || group.isEmpty()) {
         return;
      }

      String pkg = group.get(0).getPkg();
      String clazz = group.get(0).getClazz();
      String fullPkg = (pkg == null) ? options.getPackageName() : options.getPackageName() + "." + pkg;

      List<Object> constants = new ArrayList<>();
      QualifiedName constantsName = new QualifiedName(pkg, null, clazz);
      if (consts.containsKey(constantsName)) {
         ConstantsNode cnode = consts.get(constantsName);
         for (Map.Entry<String,ValueNode> entry : cnode.getConstants().entrySet()) {
            constants.add(getConstantContext(entry.getKey(), entry.getValue()));
         }
      }

      File pkgDir = new File(outputDir, fullPkg.replaceAll("\\.", "/"));
      pkgDir.mkdirs();

      File testPkgDir = new File(testDir, fullPkg.replaceAll("\\.", "/"));
      testPkgDir.mkdirs();

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
         mcontext.put("serde", "serde");
         mcontext.put("serdeclass", "Serde");
         mcontext.put("ioSerDe", "ioSerDe");
         mcontext.put("nioSerDe", "nioSerDe");
         mcontext.put("nettySerDe", "nettySerDe");
         mcontext.put("messages", emptyGroupMessages);

         messages.add(mcontext);
      }

      for (String grp : groups) {
         List<Object> grpMessages = getMessageGroup(grp, namedStructs, true);
         Map<String,Object> mcontext = new HashMap<>();
         mcontext.put("serde", grp + "SerDe");
         mcontext.put("serdeclass", lowerCamelToUpperCamel(grp) + "SerDe");
         mcontext.put("ioSerDe", grp + "IoSerDe");
         mcontext.put("nioSerDe", grp + "NioSerDe");
         mcontext.put("nettySerDe", grp + "NettySerDe");
         mcontext.put("messages", grpMessages);

         messages.add(mcontext);
      }

      try (Writer out = new BufferedWriter(new FileWriter(new File(pkgDir, clazz + ".java")))) {
         Map<String,Object> context = new HashMap<String,Object>();
         context.put("package", fullPkg);
         context.put("header", ast.getHeader());
         context.put("classname", clazz);
         context.put("messages", messages);
         context.put("structs", structContexts);
         context.put("constants", constants);

         com.github.jknack.handlebars.Template template = handlebars.compile("StructGroup");
         template.apply(context, out);
      }

      List<Map<String,Object>> testStructContexts = new ArrayList<>();
      for (QualifiedName next : group) {
         Map<String,Object> tcontext = getStructContext(fullPkg, clazz + "." + next.getMessage(), structs.get(next));
         tcontext.put("simpleclassname", next.getMessage());
         tcontext.put("outerclassname", clazz);

         testStructContexts.add(tcontext);
      }

      try (Writer out = new BufferedWriter(new FileWriter(new File(testPkgDir, "Test" + clazz + ".java")))) {
         Map<String,Object> context = new HashMap<String,Object>();
         context.put("package", fullPkg);
         context.put("header", ast.getHeader());
         context.put("classname", clazz);
         context.put("structs", testStructContexts);

         com.github.jknack.handlebars.Template template = handlebars.compile("StructGroupTest");
         template.apply(context, out);
      }
   }

   public void generateStructs(String pkg, Map<String,? extends StructNode> structs, boolean isMessage) throws Exception {
      for (Map.Entry<String,? extends StructNode> next : structs.entrySet()) {
         String name = next.getKey();
         StructNode struct = next.getValue();
         validateStruct(name, struct);

         Map<String,Object> context = getStructContext(pkg, name, struct);
         context.put("package", pkg);
         context.put("header", ast.getHeader());

         try (Writer out = new BufferedWriter(new FileWriter(new File(mainPkgDir, name + ".java")))) {
            com.github.jknack.handlebars.Template template = handlebars.compile("Struct");
            template.apply(context, out);
         }

         try (Writer out = new BufferedWriter(new FileWriter(new File(testPkgDir, "Test" + name + ".java")))) {
            com.github.jknack.handlebars.Template template = handlebars.compile("StructTest");
            template.apply(context, out);
         }
      }
   }

   public void generateConstants(String opkg, Map<QualifiedName,ConstantsNode> consts) throws Exception {
      for (Map.Entry<QualifiedName,ConstantsNode> entry : consts.entrySet()) {
         QualifiedName qname = entry.getKey();
         String pkg = qname.getPkg();
         String fullPkg = (pkg == null) ? opkg : opkg + "." + pkg;

         File pkgDir = new File(outputDir, fullPkg.replaceAll("\\.", "/"));
         pkgDir.mkdirs();

         List<Object> constants = new ArrayList<>();
         for (Map.Entry<String,ValueNode> centry : entry.getValue().getConstants().entrySet()) {
            constants.add(getConstantContext(centry.getKey(), centry.getValue()));
         }

         Map<String,Object> context = new HashMap<>();
         context.put("package", fullPkg);
         context.put("classname", qname.getMessage());
         context.put("constants", constants);
         context.put("header", ast.getHeader());

         try (Writer out = new BufferedWriter(new FileWriter(new File(pkgDir, qname.getMessage() + ".java")))) {
            com.github.jknack.handlebars.Template template = handlebars.compile("Constants");
            template.apply(context, out);
         }
      }
   }

   public void generateGrouping(String pkg, String group, Map<String,? extends MessageNode> msgs) throws Exception {
      List<Object> messages = getMessageGroup(group, msgs, true);
      if (messages.isEmpty()) {
         return;
      }

      Map<String,Object> context = new HashMap<>();
      context.put("package", pkg);
      context.put("groupname", group);
      context.put("serde", "serde");
      context.put("serdeclass", "Serde");
      context.put("ioSerDe", "ioSerDe");
      context.put("nioSerDe", "nioSerDe");
      context.put("nettySerDe", "nettySerDe");
      context.put("messages", messages);

      try (Writer out = new BufferedWriter(new FileWriter(new File(mainPkgDir, group + ".java")))) {
         com.github.jknack.handlebars.Template template = handlebars.compile("MessageGroup");
         template.apply(context, out);
      }
   }

   public void validateStruct(String name, StructNode node) throws Exception {
      // Recursive structures are not supported right now
      for (FieldNode fieldNode : node.getFields().values()) {
         TypeNode fieldType = fieldNode.getType(ast);
         ensureTypeNotOfUserType(fieldType, name);
      }
   }

   public void ensureTypeNotOfUserType(TypeNode type, String name) throws Exception {
      if (type instanceof UserTypeNode) {
         UserTypeNode utn = (UserTypeNode)type;
         if (name.equals(utn.getTypeName())) {
            throw new IllegalStateException("recursive types are not supported: " + name);
         }
      }

      if (type instanceof ArrayTypeNode) {
         ArrayTypeNode atn = (ArrayTypeNode)type;
         ensureTypeNotOfUserType(atn.getValueType(ast), name);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Misc support
   /////////////////////////////////////////////////////////////////////////////

   public void tab() {
      tab = tab + "   ";
   }

   public void tab(int num) {
      for (int i = 0; i < num; ++i)
         tab();
   }

   public void untab() {
      tab = (tab.length() <= 3) ? "" : tab.substring(3);
   }

   public void untab(int num) {
      for (int i = 0; i < num; ++i)
         untab();
   }

   public String lowerCamelToUpperCamel(Object str) {
      String s = String.valueOf(str);
      return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, s);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Type generation support
   /////////////////////////////////////////////////////////////////////////////

   public String type(TypeNode type) {
      return type(type, false);
   }

   public String type(TypeNode type, boolean arraysAsVarargs) {
      if (type instanceof PrimitiveTypeNode) {
         PrimitiveTypeNode prim = (PrimitiveTypeNode)type;
         switch (prim.getPrimitiveType()) {
         case U8:  return "byte";
         case U16: return "short";
         case U32: return "int";
         case U64: return "long";

         case I8:  return "byte";
         case I16: return "short";
         case I32: return "int";
         case I64: return "long";

         case F32: return "float";
         case F64: return "double";
         default: throw new IllegalStateException("unknown primitive type: " + type);
         }
      }

      if (type instanceof ArrayTypeNode) {
         ArrayTypeNode atn = (ArrayTypeNode)type;
         return type(atn.getValueType(ast)) + (arraysAsVarargs ? "..." : "[]");
      }

      // TODO: this will enter an infinite loop
      //       when circular aliases are present.
      UserTypeNode user = (UserTypeNode)type;
      String name = user.getTypeName();
      if (ast.getAliases().containsKey(name)) {
         return type(ast.getAliases().get(name));
      }

      return name;
   }

   public String type(ValueNode value) {
      return type(value.getType(ast));
   }

   public String type(FieldNode field) {
      return type(field.getType(ast));
   }

   public String vartype(FieldNode field) {
      return type(field.getType(ast), true /* Arrays as var arg */);
   }

   public String objType(TypeNode type) {
      if (type instanceof PrimitiveTypeNode) {
         PrimitiveTypeNode prim = (PrimitiveTypeNode)type;
         switch (prim.getPrimitiveType()) {
         case U8:  return "Byte";
         case U16: return "Short";
         case U32: return "Integer";
         case U64: return "Long";

         case I8:  return "Byte";
         case I16: return "Short";
         case I32: return "Integer";
         case I64: return "Long";

         case F32: return "Float";
         case F64: return "Double";
         default: throw new IllegalStateException("unknown primitive type: " + type);
         }
      }

      return type(type);
   }

   public String objType(ValueNode value) {
      return type(value.getType(ast));
   }

   public String objType(FieldNode field) {
      return type(field.getType(ast));
   }

   /////////////////////////////////////////////////////////////////////////////
   // Value generation support
   /////////////////////////////////////////////////////////////////////////////

   public String value(ValueNode value) {
      String ival = value.getValue();

      return "((" + type(value) + ")" + ival + ")";
   }

   /////////////////////////////////////////////////////////////////////////////
   // Field generation support
   /////////////////////////////////////////////////////////////////////////////

   public boolean isFieldStored(FieldNode field) {
      return !field.isArraySizer();
   }

   public String typeComment(FieldNode fieldNode) {
      if (fieldNode.getWhen() != null) {
         StringBuilder bld = new StringBuilder();
         bld.append("// ");
         bld.append(typeCommentAppend(fieldNode.getType(ast)));

         if (bld.length() != 3) {
            bld.append(", ");
         }

         bld.append("when ").append(fieldNode.getWhen());
         return bld.toString();
      }

      return typeComment(fieldNode.getType(ast));
   }

   public String typeComment(TypeNode type) {
      String append = typeCommentAppend(type);
      if (append.isEmpty()) {
         return "";
      }

      return "// " + append;
   }

   public String typeCommentAppend(TypeNode type) {
      if (type instanceof FixedArrayTypeNode) {
         FixedArrayTypeNode atn = (FixedArrayTypeNode)type;
         return "length = " + atn.getLength(ast);
      }

      if (type instanceof SizedArrayTypeNode) {
         SizedArrayTypeNode atn = (SizedArrayTypeNode)type;
         return "length = " + atn.getLength(ast);
      }

      return "";
   }

   /////////////////////////////////////////////////////////////////////////////
   // Byte offset support
   /////////////////////////////////////////////////////////////////////////////
   //
   public String fieldByteOffset(FieldNode field, List<String> previous) {
      if (previous.isEmpty()) {
         return "0";
      }

      String last = previous.get(previous.size()-1);
      return last + "Offset(context) + " + last + "Size(context)";
   }
   
   /////////////////////////////////////////////////////////////////////////////
   // Byte size support
   /////////////////////////////////////////////////////////////////////////////

   public String fieldByteSizeContext(FieldNode field) {
      try {
         TypeNode type = field.getType(ast);
         String name = field.getName();
         return fieldByteSizeContext(name, type);
      } catch (Exception ex) {
         return null;
      }
   }

   public String fieldByteSizeContext(String name, TypeNode type) {
      if (type instanceof ArrayTypeNode) {
         ArrayTypeNode atn = (ArrayTypeNode)type;
         TypeNode vt = atn.getValueType(ast);
         return "java.lang.reflect.Array.getLength(context.get(\"" + name + "\")) * " + fieldByteSizeContext("", vt);
      }

      if (type instanceof UserTypeNode) {
         throw new IllegalStateException();
      }

      return typeByteSize(name, type);
   }

   public String fieldByteSize(FieldNode field) {
      TypeNode fieldType = field.getType(ast);
      return typeByteSize(field.getName(), fieldType);
   }

   public String fieldByteSizeExtra(FieldNode field) {
      TypeNode fieldType = field.getType(ast);
      String name = field.getName();

      if (!(fieldType instanceof ArrayTypeNode)) {
         return "";
      }

      ArrayTypeNode atn = (ArrayTypeNode)fieldType;
      TypeNode vt = atn.getValueType(ast);
      if (!(vt instanceof UserTypeNode)) {
         return "";
      }

      return "\n" +
         tab + "for(int i = 0; i < " + name + ".length; ++i)\n" +
         tab + "   size += " + name + "[i].getByteSize();\n";
   }

   public String typeByteSize(String name, TypeNode type) {
      if (type instanceof PrimitiveTypeNode) {
         PrimitiveTypeNode prim = (PrimitiveTypeNode)type;
         switch (prim.getPrimitiveType()) {
         case U8:  case I8:  return "1";
         case U16: case I16: return "2";
         case U32: case I32: return "4";
         case U64: case I64: return "8";
         case F32: return "4";
         case F64: return "8";
         default:  throw new IllegalStateException("cannot determine byte size of: " + type);
         }
      }

      if (type instanceof ArrayTypeNode) {
         ArrayTypeNode atn = (ArrayTypeNode)type;
         TypeNode vt = atn.getValueType(ast);
         if (vt instanceof UserTypeNode) {
            return "0";
         }

         return typeByteSize(name, atn.getValueType(ast)) + "*" + name + ".length";
      }

      if (type instanceof SizedArrayTypeNode) {
         return "0";
      }

      if (type instanceof UserTypeNode) {
         return name + ".getByteSize()";
      }

      throw new IllegalStateException("cannot determine size of: " + type);
   }

   public String structMinSize(StructNode struct) {
      StringBuilder bld = new StringBuilder();
      for (Map.Entry<String,FieldNode> entry : struct.getFields().entrySet()) {
         String minSize = fieldMinSize(entry.getValue());
         if ("0".equals(minSize)) {
            continue;
         }

         if (bld.length() != 0) {
            bld.append(" + ");
         }

         bld.append(minSize);
      }

      if (bld.length() == 0) {
         bld.append("0");
      }

      return bld.toString();
   }

   public String fieldMinSize(FieldNode field) {
      if (field.getWhen() != null) {
         return "0";
      }

      TypeNode fieldType = field.getType(ast);
      return typeMinSize(fieldType);
   }

   public String typeMinSize(TypeNode type) {
      if (type instanceof PrimitiveTypeNode) {
         return typeByteSize("", type);
      }

      if (type instanceof FixedArrayTypeNode) {
         FixedArrayTypeNode atn = (FixedArrayTypeNode)type;
         String length = atn.getLength(ast);
         if (length == null) return "0";

         return typeMinSize(atn.getValueType(ast)) + "*" + length;
      }

      if (type instanceof SizedArrayTypeNode || type instanceof VariableArrayTypeNode) {
         return "0";
      }

      if (type instanceof UserTypeNode) {
         UserTypeNode utn = (UserTypeNode)type;
         return utn.getTypeName() + ".LENGTH_MIN";
      }

      throw new IllegalStateException("cannot determine size of: " + type);
   }

   public String structMaxSize(StructNode struct) {
      StringBuilder bld = new StringBuilder();
      for (Map.Entry<String,FieldNode> entry : struct.getFields().entrySet()) {
         if (bld.length() != 0) {
            bld.append(" + ");
         }

         String fieldSize = fieldMaxSize(entry.getValue());
         if ("-1".equals(fieldSize)) {
            return "-1";
         }

         bld.append(fieldMaxSize(entry.getValue()));
      }

      if (bld.length() == 0) {
         return "0";
      }

      String computedSize = "(" + bld.toString() + ")";
      bld = new StringBuilder(computedSize);
      for (Map.Entry<String,FieldNode> entry : struct.getFields().entrySet()) {
         bld.append(fieldMaxMask(entry.getValue()));
      }

      return bld.toString();
   }

   public String fieldMaxSize(FieldNode field) {
      TypeNode fieldType = field.getType(ast);
      return typeMaxSize(fieldType);
   }

   public String fieldMaxMask(FieldNode field) {
      TypeNode fieldType = field.getType(ast);
      return typeMaxMask(fieldType);
   }

   public String typeMaxSize(TypeNode type) {
      if (type instanceof SizedArrayTypeNode || type instanceof VariableArrayTypeNode) {
         return "-1";
      }

      if (type instanceof UserTypeNode) {
         UserTypeNode utn = (UserTypeNode)type;
         return utn.getTypeName() + ".LENGTH_MAX";
      }

      return typeMinSize(type);
   }

   public String typeMaxMask(TypeNode type) {
      if (type instanceof UserTypeNode) {
         UserTypeNode utn = (UserTypeNode)type;
         return " | (" + utn.getTypeName() + ".LENGTH_MAX & 0x80000000)";
      }

      return "";
   }

   /////////////////////////////////////////////////////////////////////////////
   // com.iris.protocol.zigbee.TestZdpUserDescSet
   // com.iris.protocol.zigbee.TestZdpUserDescRsp
   // com.iris.protocol.zigbee.TestZclAttributeReport
   // com.iris.protocol.zigbee.TestZclReadAttributeRecord
   // com.iris.protocol.zigbee.TestZclWriteAttributeRecord
   // com.iris.protocol.zigbee.TestZclWriteStatusRecord
   // Getter generation support
   /////////////////////////////////////////////////////////////////////////////

   public String getterValue(FieldNode field) {
      String name = "this." + field.getName();
      TypeNode fieldType = field.getType(ast);

      if (field.isArraySizer()) {
         return "this." + field.getArraySized().getName() + ".length";
      }

      if (fieldType instanceof PrimitiveTypeNode) {
         PrimitiveTypeNode prim = (PrimitiveTypeNode)fieldType;
         switch (prim.getPrimitiveType()) {
         case U8:  return name + " & 0xFF";
         case U16: return name + " & 0xFFFF";
         default:  return name;
         }
      }

      return name;
   }

   public String rawGetterValue(FieldNode field) {
      String name = "this." + field.getName();
      TypeNode fieldType = field.getType(ast);

      if (field.isArraySizer()) {
         String sname = "this." + field.getArraySized().getName() + ".length";
         if (fieldType instanceof PrimitiveTypeNode) {
            PrimitiveTypeNode prim = (PrimitiveTypeNode)fieldType;
            switch (prim.getPrimitiveType()) {
            case U8:  return "(byte)" + sname;
            case I8:  return "(byte)" + sname;
            case U16: return "(short)" + sname;
            case I16: return "(short)" + sname;
            default:  return sname;
            }
         }

         return sname;
      }

      return name;
   }

   public String getterType(FieldNode field) {
      TypeNode fieldType = field.getType(ast);

      if (fieldType instanceof PrimitiveTypeNode) {
         PrimitiveTypeNode prim = (PrimitiveTypeNode)fieldType;
         switch (prim.getPrimitiveType()) {
         case U8:  return "int";
         case U16: return "int";
         case I8:  return "int";
         case I16: return "int";
         default:  return type(fieldType);
         }
      }

      return type(fieldType);
   }

   public boolean hasRawGetter(FieldNode field) {
      TypeNode fieldType = field.getType(ast);

      if (fieldType instanceof PrimitiveTypeNode) {
         PrimitiveTypeNode prim = (PrimitiveTypeNode)fieldType;
         switch (prim.getPrimitiveType()) {
         case U8:  return true;
         case U16: return true;
         case I8:  return true;
         case I16: return true;
         default:  return false;
         }
      }

      return false;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Builder generation support
   /////////////////////////////////////////////////////////////////////////////

   public String assignField(FieldNode field) {
      TypeNode fieldType = field.getType(ast);
      String name = field.getName();

      if (fieldType instanceof PrimitiveTypeNode) {
         return name;
      }

      if (fieldType instanceof FixedArrayTypeNode) {
         FixedArrayTypeNode atn = (FixedArrayTypeNode)fieldType;
         String length = atn.getLength(ast);
         if (length == null) length = "0";
         return "Arrays.copyOf(" + name + "," + length + ")";
      }

      if (fieldType instanceof SizedArrayTypeNode) {
         return "Arrays.copyOf(" + name + "," + name + ".length)";
      }

      return name;
   }

   public String assignFixedArrayField(FieldNode field) {
      if (!(field.getType(ast) instanceof FixedArrayTypeNode)) {
         return "";
      }

      FixedArrayTypeNode fieldType = (FixedArrayTypeNode)field.getType(ast);
      String name = field.getName();

      FixedArrayTypeNode atn = (FixedArrayTypeNode)fieldType;
      String length = atn.getLength(ast);
      if (length == null) length = "0";

      return "Arrays.copyOfRange(" + name + ",offset,offset + " + length + ")";
   }

   public String assignSizedArrayField(FieldNode field) {
      String name = field.getName();
      return "Arrays.copyOfRange(" + name + ",offset,offset + length)";
   }

   public boolean hasFixedArraySetter(FieldNode field) {
      return (field.getType(ast) instanceof FixedArrayTypeNode);
   }

   public boolean hasSizedArraySetter(FieldNode field) {
      return (field.getType(ast) instanceof SizedArrayTypeNode);
   }

   public String validateField(FieldNode field) {
      TypeNode fieldType = field.getType(ast);
      String name = field.getName();

      if (fieldType instanceof FixedArrayTypeNode) {
         FixedArrayTypeNode atn = (FixedArrayTypeNode)fieldType;
         String typ = type(atn.getValueType(ast));
         String length = atn.getLength(ast);
         return tab + "this." + name + " = (" + name + " == null) ? " + "new " + typ + "[" + length + "] : " + name + ";\n";
      }

      if (fieldType instanceof SizedArrayTypeNode) {
         SizedArrayTypeNode atn = (SizedArrayTypeNode)fieldType;
         String typ = type(atn.getValueType(ast));
         return tab + "this." + name + " = (" + name + " == null) ? " + "new " + typ + "[0] : " + name + ";\n";
      }

      if (fieldType instanceof VariableArrayTypeNode) {
         VariableArrayTypeNode atn = (VariableArrayTypeNode)fieldType;
         String typ = type(atn.getValueType(ast));
         return tab + "this." + name + " = (" + name + " == null) ? " + "new " + typ + "[0] : " + name + ";\n";
      }

      if (fieldType instanceof UserTypeNode) {
         UserTypeNode utn = (UserTypeNode)fieldType;
         String typ = utn.getTypeName();
         return tab + "this." + name + " = (" + name + " == null) ? " + typ + ".builder().create() : " + name + ";\n";
      }

      return "";
   }

   public String validateDefault(FieldNode field) {
      TypeNode fieldType = field.getType(ast);
      String name = field.getName();

      if (fieldType instanceof FixedArrayTypeNode) {
         FixedArrayTypeNode atn = (FixedArrayTypeNode)fieldType;
         String typ = type(atn.getValueType(ast));
         String length = atn.getLength(ast);
         return tab + "this." + name + " = new " + typ + "[" + length + "];\n";
      }

      if (fieldType instanceof SizedArrayTypeNode) {
         SizedArrayTypeNode atn = (SizedArrayTypeNode)fieldType;
         String typ = type(atn.getValueType(ast));
         return tab + "this." + name + " = new " + typ + "[0];\n";
      }

      if (fieldType instanceof VariableArrayTypeNode) {
         VariableArrayTypeNode atn = (VariableArrayTypeNode)fieldType;
         String typ = type(atn.getValueType(ast));
         return tab + "this." + name + " = new " + typ + "[0];\n";
      }

      if (fieldType instanceof UserTypeNode) {
         UserTypeNode utn = (UserTypeNode)fieldType;
         String typ = utn.getTypeName();
         return tab + "this." + name + " = " + typ + ".builder().create();\n";
      }

      if (fieldType instanceof PrimitiveTypeNode) {
         return tab + "this." + name + " = 0;\n";
      }

      return "";
   }

   public boolean hasExtraSetter(FieldNode field) {
      return hasRawGetter(field);
   }

   /////////////////////////////////////////////////////////////////////////////
   // String generation support
   /////////////////////////////////////////////////////////////////////////////

   public String printField(FieldNode field) {
      String name = field.getName();
      TypeNode fieldType = field.getType(ast);

      // Array sizers aren't actually stored so we
      // print out the size of the backing array instead
      if (field.isArraySizer()) {
         return field.getArraySized().getName() + ".length";
      }

      // Primitive integer fields are printed out in hex
      if (fieldType instanceof PrimitiveTypeNode) {
         PrimitiveTypeNode ptn = (PrimitiveTypeNode)fieldType;
         switch (ptn.getPrimitiveType()) {
         case F32: case F64: break;
         default: return "\"0x\" + ProtocUtil.toHexString(" + name + ")";
         }
      }

      // Arrays of primitive types are printed out in hex
      if (fieldType instanceof ArrayTypeNode) {
         ArrayTypeNode atn = (ArrayTypeNode)fieldType;
         TypeNode vt = atn.getValueType(ast);
         if (vt instanceof PrimitiveTypeNode) {
            PrimitiveTypeNode ptn = (PrimitiveTypeNode)vt;
            switch (ptn.getPrimitiveType()) {
            case F32: case F64: break;
            default: return "ProtocUtil.toHexString(" + name + ")";
            }
         }

         return "Arrays.toString(" + name + ")";
      }

      // Everything else uses simple toString()
      return name;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Equality support
   /////////////////////////////////////////////////////////////////////////////

   public String equalToTest(FieldNode field) {
      String name = field.getName();

      TypeNode fieldType = field.getType(ast);
      if (fieldType instanceof ArrayTypeNode) {
         return "!Arrays.equals(" + name + ", other." + name + ")";
      }

      if (fieldType instanceof PrimitiveTypeNode) {
         PrimitiveTypeNode ptype = (PrimitiveTypeNode)fieldType;
         switch (ptype.getPrimitiveType()) {
         case F32:
            return "Float.floatToIntBits(" + name + ") != Float.floatToIntBits(other." + name + ")";

         case F64:
            return "Double.doubleToLongBits(" + name + ") != Double.doubleToLongBits(other." + name + ")";

         default:
            return name + " != other." + name;
         }
      }

      return "!" + name + ".equals(other." + name + ")";
   }

   /////////////////////////////////////////////////////////////////////////////
   // Hash code generation support
   /////////////////////////////////////////////////////////////////////////////

   public String hashCodeHeader(String packageName, String className) {
      int seed = (packageName + "." + className).hashCode();
      if (seed == 0) seed = 1;

      return "final int prime = 31;\n" +
       tab + "int result = " + seed + ";";
   }

   public String hashCodeField(FieldNode field) {
      String name = "this." + field.getName();

      TypeNode fieldType = field.getType(ast);
      if (fieldType instanceof ArrayTypeNode) {
         return "result = prime * result + Arrays.hashCode(" + name + ");";
      }

      if (fieldType instanceof PrimitiveTypeNode) {
         PrimitiveTypeNode ptype = (PrimitiveTypeNode)fieldType;
         switch (ptype.getPrimitiveType()) {
         case U64:
         case I64:
            return "result = prime * result + (int) (" + name + " ^ (" + name + " >>> 32));";

         case F32:
            return "result = prime * result + Float.floatToIntBits(" + name + ")";

         case F64:
            return "long temp_" + name + " = Double.doubleToLongBits(" + name + ");\n" +
             "      result = prime * result + (int) (temp_" + name +" ^ (temp_" + name + " >>> 32));";

         default:
            return "result = prime * result + " + name + ";";
         }
      }

      return "result = prime * result + " + name + ".hashCode();";
   }

   public String hashCodeFooter() {
      return "return result;";
   }

   /////////////////////////////////////////////////////////////////////////////
   // Java IO SerDe support
   /////////////////////////////////////////////////////////////////////////////

   public String ioDecode(FieldNode field) {
      TypeNode fieldType = field.getType(ast);
      return ioDecode(field.getName(), fieldType);
   }

   public String ioDecode(String fname, TypeNode fieldType) {
      if (fieldType instanceof PrimitiveTypeNode) {
         PrimitiveTypeNode ptype = (PrimitiveTypeNode)fieldType;
         switch (ptype.getPrimitiveType()) {
         case I8:  case U8:  return "input.readByte()";
         case I16: case U16: return "input.readShort()";
         case I32: case U32: return "input.readInt()";
         case I64: case U64: return "input.readLong()";
         case F32: return "input.readFloat()";
         case F64: return "input.readDouble()";
         default: throw new IllegalStateException("cannot decode type: " + fieldType);
         }
      }

      if (fieldType instanceof UserTypeNode) {
         UserTypeNode utn = (UserTypeNode)fieldType;
         String name = utn.getTypeName();
         return name + ".serde().ioSerDe().decode(input)";
      }

      if (fieldType instanceof FixedArrayTypeNode) {
         FixedArrayTypeNode atn = (FixedArrayTypeNode)fieldType;
         String length = atn.getLength(ast);
         String type = type(atn.getValueType(ast));
         return "new " + type + "[" + length + "]";
      }

      if (fieldType instanceof SizedArrayTypeNode) {
         SizedArrayTypeNode atn = (SizedArrayTypeNode)fieldType;
         String length = atn.getLength(ast);
         String type = type(atn.getValueType(ast));
         return "new " + type + "[" + length + "]";
      }

      if (fieldType instanceof VariableArrayTypeNode) {
         String readName = "readAll" + lowerCamelToUpperCamel(fname);
         VariableArrayTypeNode atn = (VariableArrayTypeNode)fieldType;
         String vt = type(atn.getValueType(ast));
         if (atn.getValueType(ast) instanceof PrimitiveTypeNode) {
            PrimitiveTypeNode ptn = (PrimitiveTypeNode)atn.getValueType(ast);

            switch (ptn.getPrimitiveType()) {
            case U8:  case I8:  return readName;
            case U16: case I16: return "com.iris.protoc.runtime.ProtocUtil.toShortArray(" + readName + ")";
            case U32: case I32: return "com.iris.protoc.runtime.ProtocUtil.toIntArray(" + readName + ")";
            case U64: case I64: return "com.iris.protoc.runtime.ProtocUtil.toLongArray(" + readName + ")";
            case F32: return "com.iris.protoc.runtime.ProtocUtil.toFloatArray(" + readName + ")";
            case F64: return "com.iris.protoc.runtime.ProtocUtil.toDoubleArray(" + readName + ")";
            default: throw new IllegalStateException("unknown primitive type: " + ptn);
            }
         }

         return readName + ".toArray(new " + vt + "[" + readName + ".size()])";
      }

      throw new IllegalStateException("cannot decode type: " + fieldType);
   }

   public String ioDecodeDefault(FieldNode field) {
      return ioDecodeDefault(field.getType(ast));
   }

   public String ioDecodeDefault(TypeNode fieldType) {
      if (fieldType instanceof UserTypeNode) {
         UserTypeNode utn = (UserTypeNode)fieldType;
         return utn.getTypeName() + ".getEmptyInstance()";
      }

      if (fieldType instanceof ArrayTypeNode) {
         ArrayTypeNode atn = (ArrayTypeNode)fieldType;
         TypeNode valueType = atn.getValueType(ast);
         return "new " + type(valueType) + "[0]";
      }

      return "0";
   }

   public String ioDecodePrefix(FieldNode field) {
      String name = field.getName();
      TypeNode fieldType = field.getType(ast);

      if (fieldType instanceof VariableArrayTypeNode) {
         ArrayTypeNode atn = (ArrayTypeNode)fieldType;
         TypeNode valueType = atn.getValueType(ast);
         String vt = objType(valueType);
         String readName = "readAll" + lowerCamelToUpperCamel(name);

         String read = null;
         if (valueType instanceof PrimitiveTypeNode) {
            PrimitiveTypeNode ptn = (PrimitiveTypeNode)valueType;

            switch (ptn.getPrimitiveType()) {
            case U8: case I8: read = "input.readByte()";
            default: read = ioDecode(field.getName(), ptn);
            }
         }

         if (valueType instanceof UserTypeNode) {
            UserTypeNode utn = (UserTypeNode)valueType;
            read = utn.getTypeName() + ".serde().ioSerDe().decode(input)";
         }

         if ("input.readByte()".equals(read)) {
            String readNameTmp = readName + "Tmp";
            return
                     "ArrayList<" + vt + "> " + readNameTmp + " = new ArrayList<" + vt  + ">();\n" +
               tab + "try {\n" +
               tab + "   while (true) {\n" +
               tab + "      " + readNameTmp + ".add(" + read + ");\n" +
               tab + "   }\n" +
               tab + "} catch (EOFException ex) {\n" +
               tab + "}\n" +
               tab + "byte[] " + readName + " = com.iris.protoc.runtime.ProtocUtil.toByteArray(" + readNameTmp + ");\n" + tab;
         } else {
            return
                     "ArrayList<" + vt + "> " + readName + " = new ArrayList<" + vt  + ">();\n" +
               tab + "try {\n" +
               tab + "   while (true) {\n" +
               tab + "      " + readName + ".add(" + read + ");\n" +
               tab + "   }\n" +
               tab + "} catch (EOFException ex) {\n" +
               tab + "}\n" + tab;
         }
      }

      return "";
   }

   public String ioDecodeExtra(FieldNode field) {
      String name = field.getName();
      TypeNode fieldType = field.getType(ast);

      if (fieldType instanceof VariableArrayTypeNode) {
         return "";
      }

      if (fieldType instanceof ArrayTypeNode) {
         ArrayTypeNode atn = (ArrayTypeNode)fieldType;
         TypeNode valueType = atn.getValueType(ast);
         String length = atn.getLength(ast);

         String read = null;
         if (valueType instanceof PrimitiveTypeNode) {
            PrimitiveTypeNode ptn = (PrimitiveTypeNode)valueType;

            switch (ptn.getPrimitiveType()) {
            case U8: case I8: return "\n         input.readFully(" + name + ");";
            default: read = ioDecode(field.getName(), ptn);
            }
         }

         if (valueType instanceof UserTypeNode) {
            UserTypeNode utn = (UserTypeNode)valueType;
            read = utn.getTypeName() + ".serde().ioSerDe().decode(input);";
         }

         return "\n" +
            tab + "for(int i = 0; i < " + length + "; ++i)\n" +
            tab + "   " + name + "[i] = " + read + ";";
      }

      return "";
   }

   public String ioEncode(FieldNode field) {
      TypeNode fieldType = field.getType(ast);
      String name = field.getName();

      FieldNode sized = field.getArraySized();
      if (sized != null) {
         name = sized.getName() + ".length";
      }

      if (fieldType instanceof PrimitiveTypeNode) {
         PrimitiveTypeNode ptype = (PrimitiveTypeNode)fieldType;
         switch (ptype.getPrimitiveType()) {
         case I8:  case U8:  return "output.writeByte((byte)" + name + ");";
         case I16: case U16: return "output.writeShort((short)" + name + ");";
         case I32: case U32: return "output.writeInt(" + name + ");";
         case I64: case U64: return "output.writeLong(" + name + ");";
         case F32: return "output.writeFloat(" + name + ");";
         case F64: return "output.writeDouble(" + name + ");";
         default: return field.getName();
         }
      }

      if (fieldType instanceof UserTypeNode) {
         UserTypeNode utn = (UserTypeNode)fieldType;
         String tname = utn.getTypeName();
         return tname + ".serde().ioSerDe().encode(output," + name + ");";
      }

      if (fieldType instanceof ArrayTypeNode) {
         ArrayTypeNode atn = (ArrayTypeNode)fieldType;
         TypeNode valueType = atn.getValueType(ast);
         String length = name + ".length";

         String write = null;
         if (valueType instanceof PrimitiveTypeNode) {
            PrimitiveTypeNode ptn = (PrimitiveTypeNode)valueType;

            switch (ptn.getPrimitiveType()) {
            case U8: case I8: return "output.write(" + name + ");";
            case I16: case U16: write = "output.writeShort("; break;
            case I32: case U32: write = "output.writeInt("; break;
            case I64: case U64: write = "output.writeLong("; break;
            case F32: write = "output.writeFloat("; break;
            case F64: write = "output.writeDouble("; break;
            default: throw new IllegalStateException("cannot encode field: " + field);
            }
         }

         if (valueType instanceof UserTypeNode) {
            UserTypeNode utn = (UserTypeNode)valueType;
            write = utn.getTypeName() + ".serde().ioSerDe().encode(output,";
         }

         return "for(int i = 0; i < " + length + "; ++i)\n" +
            tab + "   " + write + name + "[i]);";
      }

      throw new IllegalStateException("cannot decode field: " + field);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Java NIO SerDe support
   /////////////////////////////////////////////////////////////////////////////

   public String nioDecode(FieldNode field) {
      TypeNode fieldType = field.getType(ast);
      return nioDecode(field.getName(), fieldType);
   }

   public String nioDecode(String fname, TypeNode fieldType) {
      if (fieldType instanceof UserTypeNode) {
         UserTypeNode utn = (UserTypeNode)fieldType;
         String name = utn.getTypeName();
         return name + ".serde().nioSerDe().decode(input)";
      }

      // This seems kind of dirty, but it seems to work
      String decode = ioDecode(fname, fieldType);
      decode = decode.replace(".read", ".get");
      return decode.replace(".getByte", ".get");
   }

   public String nioDecodeDefault(FieldNode field) {
      return nioDecodeDefault(field.getType(ast));
   }

   public String nioDecodeDefault(TypeNode fieldType) {
      return ioDecodeDefault(fieldType);
   }

   public String nioDecodePrefix(FieldNode field) {
      String name = field.getName();
      TypeNode fieldType = field.getType(ast);

      if (fieldType instanceof VariableArrayTypeNode) {
         ArrayTypeNode atn = (ArrayTypeNode)fieldType;
         TypeNode valueType = atn.getValueType(ast);
         String vt = objType(valueType);
         String readName = "readAll" + lowerCamelToUpperCamel(name);

         String read = null;
         if (valueType instanceof PrimitiveTypeNode) {
            PrimitiveTypeNode ptn = (PrimitiveTypeNode)valueType;

            switch (ptn.getPrimitiveType()) {
            case U8: case I8: read = "input.get()";
            default: read = nioDecode(field.getName(), ptn);
            }
         }

         if (valueType instanceof UserTypeNode) {
            UserTypeNode utn = (UserTypeNode)valueType;
            read = utn.getTypeName() + ".serde().nioSerDe().decode(input)";
         }

         if ("input.get()".equals(read)) {
            return
                     "byte[] " + readName + " = new byte[input.remaining()];\n" +
               tab + "input.get(" + readName + ");\n" + tab;
         } else {
            return
                     "ArrayList<" + vt + "> " + readName + " = new ArrayList<" + vt  + ">();\n" +
               tab + "try {\n" +
               tab + "   while (true) {\n" +
               tab + "      " + readName + ".add(" + read + ");\n" +
               tab + "   }\n" +
               tab + "} catch (BufferUnderflowException ex) {\n" +
               tab + "}\n" + tab;
         }
      }

      return "";
   }

   public String nioDecodeExtra(FieldNode field) {
      String name = field.getName();
      TypeNode fieldType = field.getType(ast);

      if (fieldType instanceof VariableArrayTypeNode) {
         return "";
      }

      if (fieldType instanceof ArrayTypeNode) {
         ArrayTypeNode atn = (ArrayTypeNode)fieldType;
         TypeNode valueType = atn.getValueType(ast);
         String length = atn.getLength(ast);

         String read = null;
         if (valueType instanceof PrimitiveTypeNode) {
            PrimitiveTypeNode ptn = (PrimitiveTypeNode)valueType;

            switch (ptn.getPrimitiveType()) {
            case U8: case I8: return "\n         input.get(" + name + ");";
            default: read = nioDecode(field.getName(), ptn);
            }
         }

         if (valueType instanceof UserTypeNode) {
            UserTypeNode utn = (UserTypeNode)valueType;
            read = utn.getTypeName() + ".serde().nioSerDe().decode(input);";
         }

         return "\n" +
            tab + "for(int i = 0; i < " + length + "; ++i)\n" +
            tab + "   " + name + "[i] = " + read + ";";
      }

      return ioDecodeExtra(field);
   }

   public String nioEncode(FieldNode field) {
      TypeNode fieldType = field.getType(ast);
      String name = field.getName();

      FieldNode sized = field.getArraySized();
      if (sized != null) {
         name = sized.getName() + ".length";
      }

      if (fieldType instanceof UserTypeNode) {
         UserTypeNode utn = (UserTypeNode)fieldType;
         String tname = utn.getTypeName();
         return tname + ".serde().nioSerDe().encode(output," + name + ");";
      }

      if (fieldType instanceof ArrayTypeNode) {
         ArrayTypeNode atn = (ArrayTypeNode)fieldType;
         TypeNode valueType = atn.getValueType(ast);
         String length = name + ".length";

         String write = null;
         if (valueType instanceof PrimitiveTypeNode) {
            PrimitiveTypeNode ptn = (PrimitiveTypeNode)valueType;

            switch (ptn.getPrimitiveType()) {
            case U8: case I8: return "output.put(" + name + ");";
            default: {
               // This seems kind of dirty, but it seems to work
               String encode = ioEncode(field);
               encode = encode.replace(".write", ".put");
               return encode.replace(".putByte", ".put");
            }
            }
         }

         if (valueType instanceof UserTypeNode) {
            UserTypeNode utn = (UserTypeNode)valueType;
            write = utn.getTypeName() + ".serde().nioSerDe().encode(output,";
         }

         return "for(int i = 0; i < " + length + "; ++i)\n" +
            tab + "   " + write + name + "[i]);";
      }

      // This seems kind of dirty, but it seems to work
      String encode = ioEncode(field);
      encode = encode.replace(".write", ".put");
      return encode.replace(".putByte", ".put");
   }

   /////////////////////////////////////////////////////////////////////////////
   // Java Netty SerDe support
   /////////////////////////////////////////////////////////////////////////////

   public String nettyDecode(FieldNode field) {
      return nettyDecode(field.getName(), field.getType(ast));
   }

   public String nettyDecode(String fname, TypeNode fieldType) {
      if (fieldType instanceof UserTypeNode) {
         UserTypeNode utn = (UserTypeNode)fieldType;
         String name = utn.getTypeName();
         return name + ".serde().nettySerDe().decode(input)";
      }

      return ioDecode(fname, fieldType);
   }

   public String nettyDecodeDefault(FieldNode field) {
      return nettyDecodeDefault(field.getType(ast));
   }

   public String nettyDecodeDefault(TypeNode fieldType) {
      return ioDecodeDefault(fieldType);
   }

   public String nettyDecodePrefix(FieldNode field) {
      String name = field.getName();
      TypeNode fieldType = field.getType(ast);

      if (fieldType instanceof VariableArrayTypeNode) {
         ArrayTypeNode atn = (ArrayTypeNode)fieldType;
         TypeNode valueType = atn.getValueType(ast);
         String vt = objType(valueType);
         String readName = "readAll" + lowerCamelToUpperCamel(name);

         String read = null;
         if (valueType instanceof PrimitiveTypeNode) {
            PrimitiveTypeNode ptn = (PrimitiveTypeNode)valueType;

            switch (ptn.getPrimitiveType()) {
            case U8: case I8: read = "input.get()";
            default: read = nettyDecode(field.getName(), ptn);
            }
         }

         if (valueType instanceof UserTypeNode) {
            UserTypeNode utn = (UserTypeNode)valueType;
            read = utn.getTypeName() + ".serde().nettySerDe().decode(input)";
         }

         if ("input.readByte()".equals(read)) {
            return
                     "byte[] " + readName + " = new byte[input.readableBytes()];\n" +
               tab + "input.readBytes(" + readName + ");\n" + tab;
         } else {
            return
                     "ArrayList<" + vt + "> " + readName + " = new ArrayList<" + vt  + ">();\n" +
               tab + "try {\n" +
               tab + "   while (true) {\n" +
               tab + "      " + readName + ".add(" + read + ");\n" +
               tab + "   }\n" +
               tab + "} catch (IndexOutOfBoundsException ex) {\n" +
               tab + "}\n" + tab;
         }
      }

      return "";
   }

   public String nettyDecodeExtra(FieldNode field) {
      String name = field.getName();
      TypeNode fieldType = field.getType(ast);

      if (fieldType instanceof VariableArrayTypeNode) {
         return "";
      }

      if (fieldType instanceof ArrayTypeNode) {
         ArrayTypeNode atn = (ArrayTypeNode)fieldType;
         TypeNode valueType = atn.getValueType(ast);
         String length = atn.getLength(ast);

         String read = null;
         if (valueType instanceof PrimitiveTypeNode) {
            PrimitiveTypeNode ptn = (PrimitiveTypeNode)valueType;

            switch (ptn.getPrimitiveType()) {
            case U8: case I8: return "\n         input.readBytes(" + name + ");";
            default: return ioDecodeExtra(field);
            }
         }

         if (valueType instanceof UserTypeNode) {
            UserTypeNode utn = (UserTypeNode)valueType;
            read = utn.getTypeName() + ".serde().nettySerDe().decode(input);";
         }

         return "\n" +
            tab + "for(int i = 0; i < " + length + "; ++i)\n" +
            tab + "   " + name + "[i] = " + read + ";";
      }

      return ioDecodeExtra(field);
   }

   public String nettyEncode(FieldNode field) {
      TypeNode fieldType = field.getType(ast);
      String name = field.getName();

      FieldNode sized = field.getArraySized();
      if (sized != null) {
         name = sized.getName() + ".length";
      }

      if (fieldType instanceof UserTypeNode) {
         UserTypeNode utn = (UserTypeNode)fieldType;
         String tname = utn.getTypeName();
         return tname + ".serde().nettySerDe().encode(output," + name + ");";
      }

      if (fieldType instanceof ArrayTypeNode) {
         ArrayTypeNode atn = (ArrayTypeNode)fieldType;
         TypeNode valueType = atn.getValueType(ast);
         String length = name + ".length";

         String write = null;
         if (valueType instanceof PrimitiveTypeNode) {
            PrimitiveTypeNode ptn = (PrimitiveTypeNode)valueType;

            switch (ptn.getPrimitiveType()) {
            case U8: case I8: return "output.writeBytes(" + name + ");";
            default: return ioEncode(field);
            }
         }

         if (valueType instanceof UserTypeNode) {
            UserTypeNode utn = (UserTypeNode)valueType;
            write = utn.getTypeName() + ".serde().nettySerDe().encode(output,";
         }

         return "for(int i = 0; i < " + length + "; ++i)\n" +
            tab + "   " + write + name + "[i]);";
      }

      return ioEncode(field);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Support for payload fields
   /////////////////////////////////////////////////////////////////////////////

   public boolean isPayload(FieldNode field) {
      TypeNode fieldType = field.getType(ast);
      if (fieldType instanceof VariableArrayTypeNode ||
          fieldType instanceof SizedArrayTypeNode) {
         ArrayTypeNode atn = (ArrayTypeNode)fieldType;
         TypeNode containedType = atn.getValueType(ast);
         if (containedType instanceof PrimitiveTypeNode) {
            PrimitiveTypeNode ptn = (PrimitiveTypeNode)containedType;
            if (ptn.getPrimitiveType() == PrimitiveTypeNode.PRIM.U8 ||
                ptn.getPrimitiveType() == PrimitiveTypeNode.PRIM.I8) {
               return true;
            }
         }
      }

      return false;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Random value support (mainly for testing).
   /////////////////////////////////////////////////////////////////////////////

   public String randomValue(FieldNode field) {
      TypeNode fieldType = field.getType(ast);
      return randomValue(field.getName(), fieldType);
   }

   public String randomValueExtra(FieldNode field) {
      TypeNode fieldType = field.getType(ast);
      return randomValueExtra(field.getName(), fieldType);
   }

   public String randomValue(String name, TypeNode type) {
      if (type instanceof PrimitiveTypeNode) {
         PrimitiveTypeNode ptn = (PrimitiveTypeNode)type;
         switch (ptn.getPrimitiveType()) {
         case U8:  case I8:  return "(byte)r.nextInt()";
         case U16: case I16: return "(short)r.nextInt()";
         case U32: case I32: return "r.nextInt()";
         case U64: case I64: return "r.nextLong()";
         default: throw new IllegalStateException("can't generate random value for: " + type);
         }
      }

      if (type instanceof UserTypeNode) {
         UserTypeNode utn = (UserTypeNode)type;
         return utn.getTypeName() + ".getRandomInstance()";
      }

      if (type instanceof ArrayTypeNode) {
         String rname = lowerCamelToUpperCamel(name);
         return "random" + rname;
      }

      throw new IllegalStateException("can't generate random value for: " + type);
   }

   public String randomValueExtra(String name, TypeNode type) {
      if (type instanceof ArrayTypeNode) {
         ArrayTypeNode atn = (ArrayTypeNode)type;
         String rname = "random" + lowerCamelToUpperCamel(name);
         String rtype = type(atn.getValueType(ast));
         String length;
         if (type instanceof VariableArrayTypeNode) {
            length = "r.nextInt() & 0xF";
         } else {
            length = atn.getLength(ast);
         }

         return tab + rtype + "[] " + rname + " = new " + rtype +  "[" + length + "];\n" +
                tab + "for(int i = 0; i < " + rname + ".length; ++i)\n" +
                tab + "   " + rname + "[i] = " + randomValue("", atn.getValueType(ast)) + ";\n";
      }

      return "";
   }
}

