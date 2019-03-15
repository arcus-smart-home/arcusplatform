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
package com.iris.modelmanager.changelog;

public class CQLCommand implements Command {

   private String updateCql;
   private String rollbackCql;

   public CQLCommand() {
   }

   public CQLCommand(String updateCql, String rollbackCql) {
      this.updateCql = updateCql;
      this.rollbackCql = rollbackCql;
   }

   public void setUpdateCql(String updateCql) {
      this.updateCql = updateCql;
   }

   public void setRollbackCql(String rollbackCql) {
      this.rollbackCql = rollbackCql;
   }

   public String getUpdateCql() {
      return updateCql;
   }

   public String getRollbackCql() {
      return rollbackCql;
   }

   @Override
   public String toString() {
      return "CQLCommand [updateCql=" + updateCql + ", rollbackCql="
            + rollbackCql + "]";
   }
}

