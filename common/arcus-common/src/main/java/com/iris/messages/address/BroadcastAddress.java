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
package com.iris.messages.address;

import com.iris.messages.MessageConstants;

/**
 *
 */
public class BroadcastAddress extends Address {
   private static final long serialVersionUID = 8408439386651117839L;

   BroadcastAddress() {}

   @Override
   public boolean isBroadcast() {
      return true;
   }
   
   @Override
   public boolean isHubAddress() {
      return false;
   }

   @Override
   public String getNamespace() {
      return MessageConstants.BROADCAST;
   }

   @Override
   public Object getGroup() {
      return null;
   }

   @Override
   public Object getId() {
      return null;
   }

   @Override
   public String getRepresentation() {
      return "";
   }

   @Override
   public byte[] getBytes() {
      // have to return a new instance every time since arrays aren't
      // immutable
      return new byte[ADDRESS_LENGTH];
   }

   @Override
   public String toString() {
      return getClass().getSimpleName();
   }

   @Override
   public int hashCode() {
      return 42;
   }
   
   @Override
   public boolean equals(Object o) {
      // this *should* be singleton, but can't guarantee that b/c of Java serialization
      return o != null && BroadcastAddress.class.equals(o.getClass());
   }

}

