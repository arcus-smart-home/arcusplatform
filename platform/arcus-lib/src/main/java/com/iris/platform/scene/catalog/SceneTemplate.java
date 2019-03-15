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
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.iris.platform.scene.resolver.ActionResolver;

public class SceneTemplate {
   public static final String CUSTOM_TEMPLATE = "custom";
   
   private String id;
   private String name;
   private String description;
   private Date created;
   private Date modified;
   private Map<String, ActionResolver> actions = ImmutableMap.of();

   public SceneTemplate() {
      // TODO Auto-generated constructor stub
   }

   /**
    * @return the id
    */
   public String getId() {
      return id;
   }

   /**
    * @param id the id to set
    */
   public void setId(String id) {
      this.id = id;
   }

   /**
    * @return the name
    */
   public String getName() {
      return name;
   }

   /**
    * @param name the name to set
    */
   public void setName(String name) {
      this.name = name;
   }

   /**
    * @return the description
    */
   public String getDescription() {
      return description;
   }

   /**
    * @param description the description to set
    */
   public void setDescription(String description) {
      this.description = description;
   }

   /**
    * @return the created
    */
   public Date getCreated() {
      return created;
   }

   /**
    * @param created the created to set
    */
   public void setCreated(Date created) {
      this.created = created;
   }

   /**
    * @return the modified
    */
   public Date getModified() {
      return modified;
   }

   /**
    * @param modified the modified to set
    */
   public void setModified(Date modified) {
      this.modified = modified;
   }

   /**
    * @return the actions
    */
   public List<ActionResolver> getActions() {
      return new ArrayList<ActionResolver>(actions.values());
   }

   /**
    * @param actions the actions to set
    */
   public void setActions(List<ActionResolver> actions) {
      if(actions == null || actions.isEmpty()) {
         this.actions = ImmutableMap.of();
         return;
      }
      
      // allow efficient value iteration
      this.actions = new LinkedHashMap<>(actions.size());
      for(ActionResolver action: actions) {
         this.actions.put(action.getId(), action);
      }
   }
   
   public ActionResolver getAction(String template) {
      return actions.get(template);
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "SceneTemplate [id=" + id + ", name=" + name + ", description="
            + description + ", created=" + created + ", modified=" + modified
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
      result = prime * result + ((created == null) ? 0 : created.hashCode());
      result = prime * result
            + ((description == null) ? 0 : description.hashCode());
      result = prime * result + ((id == null) ? 0 : id.hashCode());
      result = prime * result + ((modified == null) ? 0 : modified.hashCode());
      result = prime * result + ((name == null) ? 0 : name.hashCode());
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
      SceneTemplate other = (SceneTemplate) obj;
      if (actions == null) {
         if (other.actions != null) return false;
      }
      else if (!actions.equals(other.actions)) return false;
      if (created == null) {
         if (other.created != null) return false;
      }
      else if (!created.equals(other.created)) return false;
      if (description == null) {
         if (other.description != null) return false;
      }
      else if (!description.equals(other.description)) return false;
      if (id == null) {
         if (other.id != null) return false;
      }
      else if (!id.equals(other.id)) return false;
      if (modified == null) {
         if (other.modified != null) return false;
      }
      else if (!modified.equals(other.modified)) return false;
      if (name == null) {
         if (other.name != null) return false;
      }
      else if (!name.equals(other.name)) return false;
      return true;
   }

}

