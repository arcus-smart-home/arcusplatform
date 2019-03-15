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

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import com.iris.protoc.ast.RootNode;
import com.iris.protoc.java.BindingGenerator;
import com.iris.protoc.java.JavaGenerator;
import com.iris.protoc.java.NamingGenerator;
import com.iris.protoc.parser.ProtocLexer;
import com.iris.protoc.parser.ProtocParser;
import com.iris.protoc.parser.ProtocParser.FileContext;

public class IrisProtoCompiler {
   private final Set<File> source;
   private final ProtocGeneratorOptions options;

   public IrisProtoCompiler(Set<File> source, ProtocGeneratorOptions options) {
      this.source = source;
      this.options = options;
   }

   private static void parse(ASTBuilder builder, String path) throws Exception {
      ProtocLexer lexer = new ProtocLexer(new ANTLRFileStream(path));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      ProtocParser parser = new ProtocParser(tokens);
      FileContext file = parser.file();

      ParseTreeWalker walker = new ParseTreeWalker();
      walker.walk(builder, file);
   }

   public void run() throws Exception {
      if (source == null || source.isEmpty()) {
         return;
      }

      ASTBuilder builder = new ASTBuilder();

      LinkedList<File> next = new LinkedList<File>();
      next.addAll(source);

      Set<File> parsed = new HashSet<File>();
      while (!next.isEmpty()) {
         File path = next.removeFirst();
         File current = path.getCanonicalFile();
         File parent = current.getParentFile();

         if (parsed.contains(current)) {
            continue;
         }

         parsed.add(current);
         parse(builder, current.getPath());

         List<String> includes = builder.getAndResetIncludes();
         for (String inc : includes) {
            File loc;
            if (inc.startsWith("/")) loc = new File(inc);
            else loc = new File(parent,inc);

            next.add(loc);
         }
      }

      RootNode ast = builder.getAST();
      ProtocGenerator generator;
      switch (options.getType()) {
      case BINDING:
         generator = new BindingGenerator();
         break;

      case NAMING:
         generator = new NamingGenerator();
         break;

      case JAVA:
      default:
         generator = new JavaGenerator();
         break;
      }

      generator.generate(options, ast);
   }
}

