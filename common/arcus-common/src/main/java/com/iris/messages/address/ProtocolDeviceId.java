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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import com.iris.Utils;

/**
 *
 */
public class ProtocolDeviceId implements Serializable {
   private static final long serialVersionUID = -3445377949469559879L;

   private static final int LENGTH = 20;

   public static ProtocolDeviceId fromBytes(byte [] bytes) {
      if(bytes.length > LENGTH) throw new IllegalArgumentException("Identifier may be no more than " + LENGTH + " bytes");
      byte [] id = new byte[LENGTH];
      System.arraycopy(bytes, 0, id, 0, bytes.length);
      return new ProtocolDeviceId(id, Utils.b64Encode(id));
   }

   public static ProtocolDeviceId fromRepresentation(String representation) {
      byte [] id = Utils.b64Decode(representation);
      if(id.length > LENGTH) throw new IllegalArgumentException("Identifier may be no more than " + LENGTH + " bytes");
      if(id.length < LENGTH) {
         // always pad id to 20 bytes
         return fromBytes(id);
      }
      return new ProtocolDeviceId(id, representation);
   }

   public static ProtocolDeviceId hashDeviceId(String rawId) {
      return fromBytes(Utils.hash(rawId));
   }

   private final byte [] bytes;
   // marked as non-final transient for serialization reasons
   private transient String representation;

   private ProtocolDeviceId(byte [] bytes, String representation) {
      this.bytes = bytes;
      this.representation = representation;
   }

   public byte[] getBytes() {
      byte [] b = new byte[bytes.length];
      System.arraycopy(bytes, 0, b, 0, bytes.length);
      return b;
   }

   public int copyBytesTo(byte [] dest) {
      return copyBytesTo(dest, 0);
   }

   public int copyBytesTo(byte [] dest, int offset) {
      System.arraycopy(bytes, 0, dest, offset, bytes.length);
      return bytes.length;
   }

   public String getRepresentation() {
      return representation;
   }

   public int getByteLength() {
      return bytes.length;
   }

   @Override
   public String toString() {
      return representation;
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
      ProtocolDeviceId other = (ProtocolDeviceId) obj;
      if (representation == null) {
         if (other.representation != null) return false;
      }
      else if (!representation.equals(other.representation)) return false;
      return true;
   }

   private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
      ois.defaultReadObject();
      this.representation = Utils.b64Encode(bytes);
   }

}

