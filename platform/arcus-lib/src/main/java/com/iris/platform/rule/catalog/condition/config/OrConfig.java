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
package com.iris.platform.rule.catalog.condition.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.condition.OrCondition;

/**
 * 
 */
public class OrConfig implements ConditionConfig {
   private static final Logger logger = LoggerFactory.getLogger(OrConfig.class);
   
   public static final String TYPE = "or";
   
   private List<ConditionConfig> configs;
   
   /**
    * 
    */
   public OrConfig() {
      this(null);
   }

   public OrConfig(List<ConditionConfig> configs) {
      this.configs = configs == null ? ImmutableList.of() : ImmutableList.copyOf(configs);
   }

   /**
    * @return the configs
    */
   public List<ConditionConfig> getConfigs() {
      return configs;
   }

   /**
    * @param configs the configs to set
    */
   public void setConfigs(List<ConditionConfig> configs) {
      this.configs = configs == null ? ImmutableList.of() : ImmutableList.copyOf(configs);
   }

   @Override
   public String getType() {
      return TYPE;
   }

   @Override
   public Condition generate(Map<String, Object> values) {
      Preconditions.checkArgument(!configs.isEmpty(), "Must assign at least one statement");
      List<Condition> conditions = new ArrayList<>(configs.size());
      for(ConditionConfig config: configs) {
         Condition condition = config.generate(values);
         conditions.add(condition);
      }
      if(conditions.size() == 1) {
         logger.warn("Mis-use of OrConfig, only one condition specified");
         return conditions.get(0);
      }
      else {
         return new OrCondition(conditions);
      }
   }

   @Override
   public String toString() {
      return "OrConfig [configs=" + configs + "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((configs == null) ? 0 : configs.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      OrConfig other = (OrConfig) obj;
      if (configs == null) {
         if (other.configs != null) return false;
      }
      else if (!configs.equals(other.configs)) return false;
      return true;
   }

}

