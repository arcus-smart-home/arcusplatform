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
package com.iris.capability.util;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * 
 */
public class TestAddresses {

   @Test
   public void testToServiceAddress() {
      assertEquals("SERV:person:", Addresses.toServiceAddress("person"));
      assertEquals("DRIV:dev:", Addresses.toServiceAddress("dev"));
   }

   @Test
   public void testToObjectAddress() {
      assertEquals("SERV:person:", Addresses.toObjectAddress("person", null));
      assertEquals("SERV:person:", Addresses.toObjectAddress("person", ""));
      assertEquals("SERV:person:test", Addresses.toObjectAddress("person", "test"));
      
      assertEquals("DRIV:dev:", Addresses.toObjectAddress("dev", null));
      assertEquals("DRIV:dev:", Addresses.toObjectAddress("dev", ""));
      assertEquals("DRIV:dev:test", Addresses.toObjectAddress("dev", "test"));
   }
   
   @Test
   public void testToInvalidAddress() {
      try {
         Addresses.toServiceAddress(null);
         fail();
      }
      catch(IllegalArgumentException e) {
         // expected
      }
      try {
         Addresses.toObjectAddress(null, "test");
         fail();
      }
      catch(IllegalArgumentException e) {
         // expected
      }
   }

   @Test
   public void testGetId() {
      assertEquals("", Addresses.getId(null));
      assertEquals("", Addresses.getId(""));
      assertEquals("", Addresses.getId("SERV:person:"));
      assertEquals("id", Addresses.getId("id"));
      assertEquals("id", Addresses.getId("SERV:person:id"));
   }
}

