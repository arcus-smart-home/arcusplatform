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
package com.iris.messages.model;

import java.util.regex.Matcher;

import org.apache.commons.lang3.StringUtils;

import com.iris.Utils;
import com.iris.model.Version;

/**
 * An immutable tuple of the capability name,
 * the implementation name, and the version.
 */
public class CapabilityId {
   
   public static CapabilityId fromRepresentation(String capabilityId) throws IllegalArgumentException {
      if(capabilityId == null) {
         return null;
      }
      
      String [] parts = StringUtils.split(capabilityId, ':');
      if(parts.length != 3) {
         throw new IllegalArgumentException("Invalid capabilityId, should be of the form: <capability name>:<implementation name>:<version>");
      }
      
      // note we don't pass in the original representation and instead regenerate it, since
      // split will mask a variety of slightly incorrect representations, like multiple ':'
      return new CapabilityId(parts[0], parts[1], Version.fromRepresentation(parts[2]));
   }
   
   private final String representation;
   // transient is a hint for hashcode and equals
   private final transient String capabilityName;
   private final transient String implementationName;
   private final transient Version version;
   
   public CapabilityId(String capabilityName, String implementationName, Version version) {
      Utils.assertNotEmpty(capabilityName, "Must specify a capability name");
      Utils.assertNotEmpty(implementationName, "Must specify an implementation name");
      Utils.assertNotNull(version, "Must specify a version");
      this.capabilityName = capabilityName;
      this.implementationName = implementationName;
      this.version = version;
      this.representation = capabilityName + ":" + implementationName + ":" + version.getRepresentation();
   }
   
   public String getCapabilityName() {
      return capabilityName;
   }
   
   public String getImplementationName() {
      return implementationName;
   }
   
   public Version getVersion() {
      return version;
   }
   
   public String getRepresentation() {
      return representation;
   }
   
   @Override
   public String toString() {
      return "CapabilityId [" + getRepresentation() + "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
            + ((representation == null) ? 0 : representation.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      CapabilityId other = (CapabilityId) obj;
      if (representation == null) {
         if (other.representation != null) return false;
      }
      else if (!representation.equals(other.representation)) return false;
      return true;
   }

}

