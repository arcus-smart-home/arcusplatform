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
package com.iris.type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.iris.messages.address.Address;
import com.iris.util.IrisCollections;

import static com.iris.type.TypeFixtures.*;

public class TestCollectionCoerce extends TypeCoercerTestCase {
   private static final Address[] addressArray = new Address[] { TEST_ADDRESS, TEST_ADDRESS_SERVICE };
   private static final String[] stringArray = new String[] {TEST_STRING_ADDRESS, TEST_STRING_ADDRESS_SERVICE };
   private static final String listString = TEST_STRING_ADDRESS + "," + TEST_STRING_ADDRESS_SERVICE;
   private static final Set<Address> addressSet = IrisCollections.setOf(addressArray);
   private static final List<Address> addressList = Arrays.asList(addressArray);
   private static final Set<String> stringSet = IrisCollections.setOf(stringArray);
   private static final List<String> stringList = Arrays.asList(stringArray);

   @Test
   public void testAddressListFromArray() {
      checkList(addressArray);
   }
   
   @Test
   public void testAddressListFromList() {
      checkList(addressList);
   }
   
   @Test
   public void testAddressListFromSet() {
      checkList(addressSet);
   }
   
   @Test
   public void testAddressSetFromArray() {
      checkSet(addressArray);
   }
   
   @Test
   public void testAddressSetFromList() {
      checkSet(addressList);
   }
   
   @Test
   public void testAddressSetFromSet() {
      checkSet(addressSet);
   }
   
   @Test
   public void testStringToAddressListFromArray() {
      checkList(stringArray);
   }
   
   @Test
   public void testStringToAddressListFromString() {
      checkList(listString);
   }
   
   @Test
   public void testStringToAddressListFromList() {
      checkList(stringList);
   }
   
   @Test
   public void testStringToAddressListFromSet() {
      checkList(stringSet);
   }
   
   @Test
   public void testStringToAddressSetFromArray() {
      checkSet(stringArray);
   }
   
   @Test
   public void testStringToAddressSetFromString() {
      checkSet(listString);
   }
   
   @Test
   public void testStringToAddressSetFromList() {
      checkSet(stringList);
   }
   
   @Test
   public void testStringToAddressSetFromSet() {
      checkSet(stringSet);
   }
   
   @Test
   public void testEmptyArray() {
      String[] nothing = new String[] {};
      Assert.assertTrue(typeCoercer.isSupportedCollectionType(Address.class, nothing.getClass()));
      Assert.assertTrue(typeCoercer.isCoercibleCollection(Address.class, nothing));
      Set<Address> set = typeCoercer.coerceSet(Address.class, nothing);
      Assert.assertTrue(set.isEmpty());
   }
   
   @Test
   public void testEmptyCollection() {
      List<String> nothing = new ArrayList<>();
      Assert.assertTrue(typeCoercer.isSupportedCollectionType(Address.class, nothing.getClass()));
      Assert.assertTrue(typeCoercer.isCoercibleCollection(Address.class, nothing));
      Set<Address> set = typeCoercer.coerceSet(Address.class, nothing);
      Assert.assertTrue(set.isEmpty());
   }
   
   @Test
   public void testNull() {
      Assert.assertFalse(typeCoercer.isSupportedCollectionType(Address.class, null));
      Assert.assertTrue(typeCoercer.isCoercibleCollection(Address.class, null));
      Set<Address> set = typeCoercer.coerceSet(Address.class, null);
      Assert.assertNull(set);
   }
   
   @Test
   public void testInvalidCollection() {
      Integer bogus = 1;
      Assert.assertFalse(typeCoercer.isSupportedCollectionType(Address.class, bogus.getClass()));
      Assert.assertFalse(typeCoercer.isCoercibleCollection(Address.class, bogus));
      try {
         typeCoercer.coerceList(Address.class, bogus);
      }
      catch (IllegalArgumentException iae) {
         // Expected
         return;
      }
      Assert.fail("Should have exited with an IllegalArgumentException");
   }
   
   @Test
   public void testInvalidCoerce() {
      // This is a bit odd, it can only tell the type is an array, not check the underlying type.
      Assert.assertTrue(typeCoercer.isSupportedCollectionType(Date.class, addressArray.getClass()));
      // With an actual object it can figure out it can't do this.
      Assert.assertFalse(typeCoercer.isCoercibleCollection(Date.class, addressArray));
      try {
         typeCoercer.coerceList(Date.class, addressArray);
      }
      catch (IllegalArgumentException iae) {
         // Expected
         return;
      }
      Assert.fail("Should have exited with an IllegalArgumentException");
   }
   
   private void checkSet(Object test) {
      Assert.assertTrue(typeCoercer.isSupportedCollectionType(Address.class, test.getClass()));
      Assert.assertTrue(typeCoercer.isCoercibleCollection(Address.class, test));
      Set<Address> set = typeCoercer.coerceSet(Address.class, test);
      verify(set);
   }
   
   private void checkList(Object test) {
      Assert.assertTrue(typeCoercer.isSupportedCollectionType(Address.class, test.getClass()));
      Assert.assertTrue(typeCoercer.isCoercibleCollection(Address.class, test));
      List<Address> list = typeCoercer.coerceList(Address.class, test);
      verify(list);
   }
   
   private void verify(Collection<Address> collection) {
      Set<Address> actual = new HashSet<>(collection);
      Assert.assertEquals(addressSet, actual);
   }
}

