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
package com.iris.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ByteSet implements Serializable {
   private static final long serialVersionUID = 390839606608388064L;
   
   private final Set<Byte> bytes = new HashSet<>();
   
   public void addAll(Collection<Byte> byteCollection) {
      bytes.addAll(byteCollection);
   }
   
   public void add(Byte b) {
      bytes.add(b);
   }
   
   public void remove(Byte b) {
      bytes.remove(b);
   }
   
   public boolean contains(Byte b) {
      return bytes.contains(b);
   }
   
   public Iterator<Byte> iterator() {
      return bytes.iterator();
   }
}

