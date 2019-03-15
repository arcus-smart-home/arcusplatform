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
package com.iris.platform.scene.catalog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Singleton;

@Singleton
public class SceneCatalog {
   private final Map<String, SceneTemplate> templates;

   public SceneCatalog(Collection<SceneTemplate> templates) {
      this.templates = toMap(templates);
   }

   public SceneTemplate getById(String id) {
      return templates.get(id);
   }

   public List<SceneTemplate> getTemplates() {
      return new ArrayList<SceneTemplate>(templates.values());
   }

   private Map<String, SceneTemplate> toMap(Collection<SceneTemplate> templates) {
      Map<String, SceneTemplate> map = new LinkedHashMap<>(templates.size());
      for(SceneTemplate template: templates) {
         map.put(template.getId(), template);
      }
      return map;
   }

}

