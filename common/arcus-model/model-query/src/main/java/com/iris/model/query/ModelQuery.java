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
/**
 * 
 */
package com.iris.model.query;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Predicate;
import com.iris.messages.model.Model;

/**
 * 
 */
public class ModelQuery {
   private final Predicate<Model> predicate;
   
   public ModelQuery(Predicate<Model> predicate) {
      this.predicate = predicate;
   }
   
   public List<Model> select(Iterable<Model> models) {
      List<Model> matches = new ArrayList<Model>();
      for(Model model: models) {
         if(matches(model)) {
            matches.add(model);
         }
      }
      return matches;
   }
   
   public @Nullable Model selectFirst(Iterable<Model> models) {
      for(Model model: models) {
         if(matches(model)) {
            return model;
         }
      }
      return null;
   }
   
   public boolean matchesAny(Iterable<Model> models) {
      for(Model model: models) {
         if(matches(model)) {
            return true;
         }
      }
      return false;
   }
   
   public boolean matchesAll(Iterable<Model> models) {
      for(Model model: models) {
         if(!matches(model)) {
            return false;
         }
      }
      return true;
   }
   
   public boolean matches(Model model) {
      return predicate.apply(model);
   }

}

