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
package com.iris.platform.location;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class StreetSuffix
{
   private String   code;
   private String   name;
   private String[] variants;

   public StreetSuffix() { }

   public StreetSuffix(String code, String name, String[] variants)
   {
      this.code     = code;
      this.name     = name;
      this.variants = variants;
   }

   public String getCode()
   {
      return code;
   }

   public void setCode(String code)
   {
      this.code = code;
   }

   public String getName()
   {
      return name;
   }

   public void setName(String name)
   {
      this.name = name;
   }

   public String[] getVariants()
   {
      return variants;
   }

   public void setVariants(String[] variants)
   {
      this.variants = variants;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj == null) return false;
      if (this == obj) return true;
      if (getClass() != obj.getClass()) return false;

      StreetSuffix other = (StreetSuffix) obj;

      return new EqualsBuilder()
         .append(code,     other.code)
         .append(name,     other.name)
         .append(variants, other.variants)
         .isEquals();
   }

   @Override
   public int hashCode()
   {
      return new HashCodeBuilder()
         .append(code)
         .append(name)
         .append(variants)
         .toHashCode();
   }

   @Override
   public String toString()
   {
      return new ToStringBuilder(this)
         .append("code",     code)
         .append("name",     name)
         .append("variants", variants)
         .toString();
   }
}

