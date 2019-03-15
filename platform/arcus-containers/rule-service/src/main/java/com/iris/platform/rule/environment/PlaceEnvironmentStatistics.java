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
package com.iris.platform.rule.environment;

public class PlaceEnvironmentStatistics {
   private int rules;
   private int scenes;
   private int activeRules;
   private int activeScenes;

   public PlaceEnvironmentStatistics() {
      // TODO Auto-generated constructor stub
   }

   /**
    * @return the rules
    */
   public int getRules() {
      return rules;
   }

   /**
    * @param rules the rules to set
    */
   public void setRules(int rules) {
      this.rules = rules;
   }

   /**
    * @return the scenes
    */
   public int getScenes() {
      return scenes;
   }

   /**
    * @param scenes the scenes to set
    */
   public void setScenes(int scenes) {
      this.scenes = scenes;
   }

   /**
    * @return the activeRules
    */
   public int getActiveRules() {
      return activeRules;
   }

   /**
    * @param activeRules the activeRules to set
    */
   public void setActiveRules(int activeRules) {
      this.activeRules = activeRules;
   }

   /**
    * @return the activeScenes
    */
   public int getActiveScenes() {
      return activeScenes;
   }

   /**
    * @param activeScenes the activeScenes to set
    */
   public void setActiveScenes(int activeScenes) {
      this.activeScenes = activeScenes;
   }

}

