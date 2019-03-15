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
package com.iris.protoc.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RootNode extends AbstractNode implements Aliasing {
   private final Set<String> names;
   private final Map<String,String> options;
   private final Map<String,TypeNode> aliases;
   private final Map<String,StructNode> structs;
   private final Map<String,MessageNode> messages;
   private final Map<QualifiedName,StructNode> qualified;
   private final Map<String,List<QualifiedName>> qualifiedGroups;
   private final Set<String> groupings;
   private final Map<QualifiedName,ConstantsNode> constants;
   private String header;

   public RootNode() {
      names = new HashSet<String>();
      options = new HashMap<String,String>();
      aliases = new LinkedHashMap<String,TypeNode>();
      structs = new LinkedHashMap<String,StructNode>();
      messages = new LinkedHashMap<String,MessageNode>();
      constants= new LinkedHashMap<QualifiedName,ConstantsNode>();
      qualified = new LinkedHashMap<QualifiedName,StructNode>();
      qualifiedGroups = new LinkedHashMap<String,List<QualifiedName>>();
      groupings = new LinkedHashSet<String>();
   }

   public void setOption(String name, String value) {
      if (options.containsKey(name)) {
         throw new IllegalStateException("duplicate option: " + name);
      }

      options.put(name, value);
   }

   public String getOption(String name) {
      return options.get(name);
   }

   public void addStruct(String name, StructNode struct) {
      if (names.contains(name)) {
         throw new IllegalStateException("duplicate name: " + name);
      }

      names.add(name);
      structs.put(name, struct);
   }

   public void addMessage(String name, MessageNode message) {
      if (names.contains(name)) {
         throw new IllegalStateException("duplicate name: " + name);
      }

      Map<String,String> params = message.getParams();
      String grouping = params.get("group");
      if (grouping != null) {
         groupings.add(grouping);
      }


      names.add(name);
      messages.put(name, message);
   }

   public void addConstants(QualifiedName name, ConstantsNode consts) {
      if (names.contains(name.getQualifiedName())) {
         throw new IllegalStateException("duplicate name: " + name);
      }

      names.add(name.getQualifiedName());
      constants.put(name, consts);
   }

   public void addQualified(QualifiedName name, StructNode struct) {
      if (names.contains(name.getQualifiedName())) {
         throw new IllegalStateException("duplicate name: " + name);
      }

      if (struct instanceof MessageNode) {
         MessageNode message = (MessageNode)struct;
         Map<String,String> params = message.getParams();
         String grouping = params.get("group");
         if (grouping != null) {
            groupings.add(grouping);
         }
      }

      names.add(name.getQualifiedName());
      qualified.put(name, struct);

      String grouping = name.getQualifiedGroup();
      List<QualifiedName> group = qualifiedGroups.get(grouping);
      if (group == null) {
         group = new ArrayList<QualifiedName>();
         qualifiedGroups.put(grouping,group);
      }

      group.add(name);
   }

   public void addAlias(String name, TypeNode type) {
      if (names.contains(name)) {
         throw new IllegalStateException("duplicate name: " + name);
      }

      names.add(name);
      aliases.put(name, type);
   }

   @Override
   public TypeNode resolve(TypeNode type) {
      // TODO: this will enter an infinite loop
      //       when circular aliases are present.
      if (type instanceof UserTypeNode) {
         UserTypeNode utn = (UserTypeNode)type;
         if (aliases.containsKey(utn.getTypeName())) {
            return resolve(aliases.get(utn.getTypeName()));
         }
      }

      return type;
   }

   //@Override
   //public ValueNode resolve(String constant) {
   //   ValueNode vn = consts.get(constant);
   //   if (vn == null) {
   //      throw new IllegalStateException("unknown constant: " + constant);
   //   }

   //   return vn;
   //}

   @Override
   public String toString() {
      return "FILE NODE: " +
               "\noptions=" + options +
               //"\nconsts=" + consts +
               "\naliases=" + aliases +
               "\nstructs=" + structs +
               "\nmessages=" + messages;
   }

   public Map<String, String> getOptions() {
      return options;
   }

   public Map<QualifiedName, ConstantsNode> getConstants() {
      return constants;
   }

   public Map<String, TypeNode> getAliases() {
      return aliases;
   }

   public Map<String, StructNode> getStructs() {
      return structs;
   }

   public Map<String, MessageNode> getMessages() {
      return messages;
   }

   public Map<QualifiedName, StructNode> getQualified() {
      return qualified;
   }

   public Map<String, List<QualifiedName>> getQualifiedGroups() {
      return qualifiedGroups;
   }

   public Set<String> getGroupings() {
      return groupings;
   }

   public String getHeader() {
      if (header == null) return "";
      return header;
   }

   public void setHeader(String header) {
      this.header = header;
   }
}

