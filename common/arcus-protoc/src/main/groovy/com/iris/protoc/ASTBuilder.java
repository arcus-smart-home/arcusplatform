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
package com.iris.protoc;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.tree.ErrorNode;

import com.iris.protoc.ast.ConstantsNode;
import com.iris.protoc.ast.FieldNode;
import com.iris.protoc.ast.FieldValueNode;
import com.iris.protoc.ast.FixedArrayTypeNode;
import com.iris.protoc.ast.IntegerValueNode;
import com.iris.protoc.ast.MessageNode;
import com.iris.protoc.ast.NamedValueNode;
import com.iris.protoc.ast.QualifiedName;
import com.iris.protoc.ast.RootNode;
import com.iris.protoc.ast.SizedArrayTypeNode;
import com.iris.protoc.ast.StructNode;
import com.iris.protoc.ast.TypeNode;
import com.iris.protoc.ast.ValueNode;
import com.iris.protoc.ast.VariableArrayTypeNode;
import com.iris.protoc.parser.ProtocParser.AliasContext;
import com.iris.protoc.parser.ProtocParser.ConstantContext;
import com.iris.protoc.parser.ProtocParser.ConstsContext;
import com.iris.protoc.parser.ProtocParser.FieldContext;
import com.iris.protoc.parser.ProtocParser.FileContext;
import com.iris.protoc.parser.ProtocParser.IncludeContext;
import com.iris.protoc.parser.ProtocParser.MessageContext;
import com.iris.protoc.parser.ProtocParser.Opt_exprContext;
import com.iris.protoc.parser.ProtocParser.OptsContext;
import com.iris.protoc.parser.ProtocParser.ParamsContext;
import com.iris.protoc.parser.ProtocParser.QualifiedContext;
import com.iris.protoc.parser.ProtocParser.StructContext;
import com.iris.protoc.parser.ProtocParser.TypeContext;
import com.iris.protoc.parser.ProtocParser.ValueContext;
import com.iris.protoc.parser.ProtocParserBaseListener;

public class ASTBuilder extends ProtocParserBaseListener {
   private final RootNode rootNode = new RootNode();
   private ConstantsNode currentConstants;
   private StructNode currentStruct;
   private List<String> includes = new LinkedList<String>();

   public RootNode getAST() {
      return rootNode;
   }

   public List<String> getAndResetIncludes() {
      List<String> result = includes;
      includes = new LinkedList<String>();
      return result;
   }

   @Override
   public void enterInclude(IncludeContext ctx) {
      String include = ctx.STRING().getText();
      if (include == null || include.length() <= 2) {
         throw new IllegalStateException("include string too short");
      }

      String path = include.substring(1, include.length() - 1);
      includes.add(path);
   }

   @Override
   public void enterFile(FileContext ctx) {
   }

   @Override
   public void exitFile(FileContext ctx) {
      if (ctx.HEADER() != null) {
         String header = ctx.HEADER().getText();
         int idx1 = header.indexOf('{');
         int idx2 = header.lastIndexOf('}');
         String hdr = header.substring(idx1+1,idx2);
         rootNode.setHeader(hdr);
      }
   }

   @Override
   public void enterAlias(AliasContext ctx) {
      String name = ctx.ID().getText();
      TypeNode type = toType(ctx.type());
      rootNode.addAlias(name, type);
   }

   @Override
   public void enterConstant(ConstantContext ctx) {
      String name = ctx.ID().getText();
      TypeNode type = toType(ctx.type());
      ValueNode value = toValue(type, ctx.value());

      if (currentStruct == null && currentConstants != null) {
         currentConstants.addConst(name, value);
      } else if (currentStruct != null && currentConstants == null) {
         currentStruct.addConst(name, value);
      } else {
         throw new IllegalStateException();
      }
   }

   @Override
   public void exitField(FieldContext ctx) {
      if (currentStruct == null) throw new IllegalStateException();

      if (ctx.constant() != null) {
         //ValueNode node = toValue(ctx.constant());
         return;
      }

      String name = ctx.ID().getText();
      TypeNode type = toType(ctx.type());

      String when = null;
      if (ctx.WHEN() != null) {
         String wstr = ctx.WHEN().getText();
         wstr = wstr.substring("when".length(), wstr.length() - 1);
         when = wstr.trim();
      }

      String enc = null;
      if (ctx.ENCODING() != null) {
         String estr = ctx.ENCODING().getText();
         estr = estr.substring("encoding".length(), estr.length()-1);
         enc = estr.trim();
      }

      String dec = null;
      if (ctx.ENCODING() != null) {
         String dstr = ctx.DECODING().getText();
         dstr = dstr.substring("decoding".length(), dstr.length()-1);
         dec = dstr.trim();
      }

      String rnd = null;
      if (ctx.RANDOM() != null) {
         String rstr = ctx.RANDOM().getText();
         rstr = rstr.substring("random".length(), rstr.length()-1);
         rnd = rstr.trim();
      }

      String sze = null;
      if (ctx.SIZE() != null) {
         String sstr = ctx.SIZE().getText();
         sstr = sstr.substring("size".length(), sstr.length()-1);
         sze = sstr.trim();
      }

      currentStruct.addField(rootNode, name, new FieldNode(name,type,when,enc,dec,rnd,sze));
   }

   private void toParams(Map<String,String> params, ParamsContext ctx) {
      if (ctx == null) {
         return;
      }

      toParams(params, ctx.params());

      String value = ctx.STRING().getText();
      value = value.substring(1, value.length()-1);
      params.put(ctx.ID().getText(), value);
   }

   private Map<String,String> toParams(ParamsContext ctx) {
      Map<String,String> results = new HashMap<String,String>();
      toParams(results, ctx);
      return results;
   }

   @Override
   public void enterMessage(MessageContext ctx) {
      Map<String,String> params = toParams(ctx.params());
      this.currentStruct = new MessageNode(params);
   }

   @Override
   public void exitMessage(MessageContext ctx) {
      QualifiedName qualified = toQualifiedName(ctx.qualified());
      if (qualified.getQualifiedGroup() != null) {
         rootNode.addQualified(qualified, currentStruct);
      } else {
         rootNode.addMessage(qualified.getMessage(), (MessageNode)currentStruct);
      }

      this.currentStruct = null;
   }

   @Override
   public void enterStruct(StructContext ctx) {
      this.currentStruct = new StructNode();
   }

   @Override
   public void exitStruct(StructContext ctx) {
      QualifiedName qualified = toQualifiedName(ctx.qualified());
      if (qualified.getQualifiedGroup() != null) {
         rootNode.addQualified(qualified, currentStruct);
      } else {
         rootNode.addStruct(qualified.getMessage(), currentStruct);
      }

      this.currentStruct = null;
   }

   @Override
   public void enterConsts(ConstsContext ctx) {
      this.currentConstants = new ConstantsNode();
   }

   @Override
   public void exitConsts(ConstsContext ctx) {
      QualifiedName qualified = toQualifiedName(ctx.qualified());
      rootNode.addConstants(qualified, this.currentConstants);
      this.currentConstants = null;
   }

   @Override
   public void visitErrorNode(ErrorNode node) {
   }

   @Override
   public void exitOpts(OptsContext ctx) {
      for (Opt_exprContext opt : ctx.opt_expr()) {
         String name = opt.ID().getText();
         String value = opt.STRING().getText();
         String val = value.substring(1, value.length() - 1);
         rootNode.setOption(name, val);
      }
   }

   private TypeNode toType(TypeContext type) {
      if (type.type() != null) {
         TypeNode valueType = toType(type.type());
         if (type.value() != null) {
            if (type.value().ID() != null) {
               String named = type.value().ID().getText();
               if (currentStruct.getFields().containsKey(named)) {
                  FieldNode field = currentStruct.getFields().get(named);
                  return new SizedArrayTypeNode(valueType, new FieldValueNode(field));
               } else {
                  return new FixedArrayTypeNode(valueType, new NamedValueNode(named));
               }
            } else {
               ValueNode vn = toValue(type.value());
               return new FixedArrayTypeNode(valueType, vn);
            }
         } else {
            return new VariableArrayTypeNode(valueType);
         }
      }

      String prim = type.PRIMITIVE_TYPE() == null ? null : type.PRIMITIVE_TYPE().getText();
      String named = type.ID() == null ? null : type.ID().getText();
      return (prim == null) ? TypeNode.fromUser(named) : TypeNode.fromPrimitive(prim);
   }

   private ValueNode toValue(ValueContext value) {
      return toValue(TypeNode.fromPrimitive("u64"), value);
   }

   private ValueNode toValue(TypeNode type, ValueContext value) {
      if (value.ID() != null) {
         String constant = value.ID().getText();
         return new NamedValueNode(constant);
      }

      if (type != null && value.INT() != null) {
         if (!type.isPrimitive()) {
            throw new IllegalStateException("only primitive types currently supported");
         }

         int offset = 0;
         int radix = 10;
         String val = value.INT().getText();
         if (val.startsWith("0x"))      { offset = 2; radix = 16; }
         else if (val.startsWith("0X")) { offset = 2; radix = 16; }
         else if (val.startsWith("0b")) { offset = 2; radix = 2; }
         else if (val.startsWith("0B")) { offset = 2; radix = 2; }
         else if (val.startsWith("0") && val.length() > 1)  { offset = 1; radix = 8; }

         String val2 = val.substring(offset, val.length());
         BigInteger ivalue = new BigInteger(val2, radix);

         return new IntegerValueNode(ivalue, type);
      }

      throw new IllegalStateException("unknown value: " + value);
   }

   private QualifiedName toQualifiedName(QualifiedContext ctx) {
      List<String> result = toQualifiedList(ctx);

      String pkg = null;
      String clazz = null;
      String message = result.get(0);
      for (int i = 1; i < result.size(); ++i) {
         pkg = (pkg == null) ? clazz : pkg + "." + clazz;
         clazz = message;
         message = result.get(i);
      }

      return new QualifiedName(pkg, clazz, message);
   }

   private List<String> toQualifiedList(QualifiedContext ctx) {
      List<String> result;
      if (ctx.qualified() != null) {
         result = toQualifiedList(ctx.qualified());
      } else {
         result = new ArrayList<String>();
      }

      result.add(ctx.ID().getText());
      return result;
   }
}

