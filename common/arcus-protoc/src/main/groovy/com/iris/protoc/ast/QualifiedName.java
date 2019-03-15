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
package com.iris.protoc.ast;

public class QualifiedName {
   private final String pkg;
   private final String clazz;
   private final String message;

   public QualifiedName(String pkg, String clazz, String message) {
      if (pkg == null && clazz != null && !clazz.isEmpty() && Character.isLowerCase(clazz.charAt(0))) {
         this.pkg = clazz;
         this.clazz = null;
         this.message = message;
      } else {
         this.pkg = pkg;
         this.clazz = clazz;
         this.message = message;
      }
   }

   public String getPkg() {
      return pkg;
   }

   public String getClazz() {
      return clazz;
   }

   public String getMessage() {
      return message;
   }

   public String getQualifiedGroup() {
      if (pkg == null && clazz == null) {
         return null;
      }

      if (pkg == null && clazz != null) {
         return clazz;
      }

      if (pkg != null && clazz == null) {
         return pkg;
      }

      return pkg + "." + clazz;
   }

   public String getQualifiedName() {
      String qualifier = getQualifiedGroup();
      if (qualifier == null) {
         return message;
      }

      return qualifier + "." + message;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
      result = prime * result + ((message == null) ? 0 : message.hashCode());
      result = prime * result + ((pkg == null) ? 0 : pkg.hashCode());
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
      QualifiedName other = (QualifiedName) obj;
      if (clazz == null) {
         if (other.clazz != null)
            return false;
      } else if (!clazz.equals(other.clazz))
         return false;
      if (message == null) {
         if (other.message != null)
            return false;
      } else if (!message.equals(other.message))
         return false;
      if (pkg == null) {
         if (other.pkg != null)
            return false;
      } else if (!pkg.equals(other.pkg))
         return false;
      return true;
   }

   @Override
   public String toString() {
      return "QualifiedName [pkg=" + pkg + ", clazz=" + clazz + ", message=" + message + "]";
   }
}

