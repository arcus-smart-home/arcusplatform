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
package com.iris.platform.rule;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.iris.platform.scene.SceneDefinition;

/**
 * 
 */
public class RuleEnvironment {
   private UUID placeId;
   private Map<Integer, RuleDefinition> rules = null;
   private Map<Integer, SceneDefinition> scenes = null;
   // TODO collapse actions into RuleDefinition
   private Map<Integer, ActionDefinition> actions = null;
   
   public @Nullable UUID getPlaceId() {
      return placeId;
   }
   
   public void setPlaceId(@Nullable UUID placeId) {
      this.placeId = placeId;
   }
   
   public Collection<RuleDefinition> getRules() {
      return getValues(rules);
   }
   
   public void setRules(Collection<RuleDefinition> rules) {
      this.rules = toMap(rules);
   }
   
   public @Nullable RuleDefinition getRule(Integer id) {
      return getValue(this.rules, id);
   }

   public void addRule(RuleDefinition definition) {
      addValue(this.rules, definition);
   }

   public Collection<SceneDefinition> getScenes() {
      return getValues(this.scenes);
   }
   
   public void setScenes(Collection<SceneDefinition> scenes) {
      this.scenes = toMap(scenes);
   }
   
   public @Nullable SceneDefinition getScene(Integer id) {
      return getValue(scenes, id);
   }

   public void addScene(SceneDefinition definition) {
      addValue(this.scenes, definition);
   }

   public Collection<ActionDefinition> getActions() {
      return getValues(this.actions);
   }
   
   public void setActions(Collection<ActionDefinition> actions) {
      this.actions = toMap(actions);
   }
   
   public @Nullable ActionDefinition getAction(Integer id) {
      return getValue(this.actions, id);
   }

   public void addAction(ActionDefinition definition) {
      addValue(this.actions, definition);
   }
   
   private <T extends PlaceEntity<T>> Collection<T> getValues(Map<Integer, T> entities) {
      if(entities == null || entities.isEmpty()) {
         return ImmutableList.<T>of();
      }
      
      return ImmutableList.copyOf(entities.values());
   }

   private <T extends PlaceEntity<T>> T getValue(Map<Integer, T> entities, int id) {
      if(entities == null || entities.isEmpty()) {
         return null;
      }
      
      return entities.get(id);
   }
   
   private <T extends PlaceEntity<T>> void addValue(Map<Integer, T> definitions, T definition) {
      Preconditions.checkNotNull(definition, "definition may not be null");
      
      if(definitions == null) {
         definitions = new LinkedHashMap<Integer, T>();
      }
      definitions.put(definition.getSequenceId(), definition);
   }

   private <T extends PlaceEntity<T>> Map<Integer, T> toMap(Collection<T> entities) {
      if(entities == null || entities.isEmpty()) {
         return null;
      }
      Map<Integer, T> map = new HashMap<Integer, T>(entities.size());
      for(T entity: entities) {
         map.put(entity.getSequenceId(), entity);
      }
      return map;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "RuleEnvironment [placeId=" + placeId + ", rules=" + rules
            + ", actions=" + actions + "]";
   }

   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((actions == null) ? 0 : actions.hashCode());
      result = prime * result + ((placeId == null) ? 0 : placeId.hashCode());
      result = prime * result + ((rules == null) ? 0 : rules.hashCode());
      return result;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      RuleEnvironment other = (RuleEnvironment) obj;
      if (actions == null) {
         if (other.actions != null) return false;
      }
      else if (!actions.equals(other.actions)) return false;
      if (placeId == null) {
         if (other.placeId != null) return false;
      }
      else if (!placeId.equals(other.placeId)) return false;
      if (rules == null) {
         if (other.rules != null) return false;
      }
      else if (!rules.equals(other.rules)) return false;
      return true;
   }

}

