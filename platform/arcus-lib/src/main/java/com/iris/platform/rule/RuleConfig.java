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

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * 
 */
public class RuleConfig {

   @Inject(optional = true)
   @Named("rule.catalog.path")
   private String catalogPath = "conf/rule-catalog.xml";

   /**
    * @return the catalogPath
    */
   public String getCatalogPath() {
      return catalogPath;
   }

   /**
    * @param catalogPath the catalogPath to set
    */
   public void setCatalogPath(String catalogPath) {
      this.catalogPath = catalogPath;
   }

}

