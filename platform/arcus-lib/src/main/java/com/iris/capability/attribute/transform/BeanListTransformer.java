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
package com.iris.capability.attribute.transform;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class BeanListTransformer<B> {
   private final BeanAttributesTransformer<B> transformer;
   
   public BeanListTransformer(BeanAttributesTransformer<B> transformer) {
      this.transformer = transformer;
   }
   
   public List<Map<String, Object>> convertListToAttributes(List<B> beans) {
      if (beans == null) {
         return null;
      }
      List<Map<String, Object>> attrList = new LinkedList<>();
      for (B bean : beans) {
         attrList.add(transformer.transform(bean));
      }
      return attrList;
   }
   
   public List<B> convertListToBeans(List<Map<String, Object>> attrs) {
      if (attrs == null) {
         return null;
      }
      List<B> beanList = new LinkedList<>();
      for (Map<String, Object> attrMap : attrs) {
         beanList.add(transformer.transform(attrMap));
      }
      return beanList;
   }
}

