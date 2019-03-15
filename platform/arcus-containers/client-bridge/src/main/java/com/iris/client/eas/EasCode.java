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
package com.iris.client.eas;

import com.iris.Utils;

public class EasCode implements Comparable<EasCode> {

   /* NWR-SAME-CODE EAS */
   private final String eas;
   
   /* EAS Event Name */
   private final String name;
   
   private final String group;
   
   public EasCode(String eas, String name, String group) {
      // do validations
      Utils.assertNotEmpty(eas, "Eas code cannot be null or empty");
      Utils.assertNotEmpty(name, "Alert name cannot be null or empty");
      Utils.assertNotNull(group, "Group cannot be null");

      this.eas = eas;
      this.name = name;
      this.group = group;
   }

   public String getName() {
      return name;
   }

   public String getEas() {
      return eas;
   }

   public String getGroup() {
      return group;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((eas == null) ? 0 : eas.hashCode());
      result = prime * result + ((group == null) ? 0 : group.hashCode());
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      EasCode other = (EasCode) obj;
      if (eas == null) {
         if (other.eas != null)
            return false;
      }else if (!eas.equals(other.eas))
         return false;
      if (group == null) {
         if (other.group != null)
            return false;
      }else if (!group.equals(other.group))
         return false;
      if (name == null) {
         if (other.name != null)
            return false;
      }else if (!name.equals(other.name))
         return false;
      return true;
   }

   @Override
   public int compareTo(EasCode o) {
      // will facilitate ascending based sorting
      return this.name.compareTo(o.name);
   }

}

