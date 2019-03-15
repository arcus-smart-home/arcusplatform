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
package com.iris.driver.reflex;

import org.eclipse.jdt.annotation.Nullable;

public final class ReflexActionAlertmeLifesign implements ReflexAction {
   public static enum Type { BATTERY, SIGNAL, TEMPERATURE }

   private final Type type;
   private final @Nullable Double minimum;
   private final @Nullable Double nominal;

   public ReflexActionAlertmeLifesign(Type type) {
      this.type = type;
      this.minimum = null;
      this.nominal = null;
   }

   public ReflexActionAlertmeLifesign(Type type, double minimum, double nominal) {
      this.type = type;
      this.minimum = minimum;
      this.nominal = nominal;
   }

   public Type getType() {
      return type;
   }

   public Double getMinimum() {
      return minimum;
   }

   public Double getNominal() {
      return nominal;
   }

   @Override
   public String toString() {
      return "ReflexActionAlertmeLifesign [" +
         "type=" + type +
         ",min=" + minimum + 
         ",nom=" + nominal +
      "]";
   }

   @SuppressWarnings("null")
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((minimum == null) ? 0 : minimum.hashCode());
      result = prime * result + ((nominal == null) ? 0 : nominal.hashCode());
      result = prime * result + ((type == null) ? 0 : type.hashCode());
      return result;
   }

   @SuppressWarnings("null")
   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      ReflexActionAlertmeLifesign other = (ReflexActionAlertmeLifesign) obj;
      if (minimum == null) {
         if (other.minimum != null)
            return false;
      } else if (!minimum.equals(other.minimum))
         return false;
      if (nominal == null) {
         if (other.nominal != null)
            return false;
      } else if (!nominal.equals(other.nominal))
         return false;
      if (type != other.type)
         return false;
      return true;
   }
}

