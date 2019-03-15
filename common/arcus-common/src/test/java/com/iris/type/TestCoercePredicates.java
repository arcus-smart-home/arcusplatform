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

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Predicate;
import com.iris.messages.address.Address;
import com.iris.util.IrisCollections;

import static com.iris.type.TypeFixtures.*;

public class TestCoercePredicates extends TypeCoercerTestCase {
   private static final Address[] addressArray = new Address[] { TEST_ADDRESS, TEST_ADDRESS_SERVICE };
   private static final String[] stringArray = new String[] {TEST_STRING_ADDRESS, TEST_STRING_ADDRESS_SERVICE };
   private static final String listString = TEST_STRING_ADDRESS + "," + TEST_STRING_ADDRESS_SERVICE;
   private static final Set<Address> addressSet = IrisCollections.setOf(addressArray);
   private static final List<Address> addressList = Arrays.asList(addressArray);
   private static final Set<String> stringSet = IrisCollections.setOf(stringArray);
   private static final List<String> stringList = Arrays.asList(stringArray);
   private static final String[] falseStringArray = new String[] {TEST_STRING_ADDRESS_SERVICE };

   @Test
   public void testAddressPredicate() {
      Predicate<Object> isAddressSame = typeCoercer.createPredicate(Address.class, new Predicate<Address>() {
         @Override
         public boolean apply(Address input) {
            return TEST_ADDRESS.equals(input);
         }        
      });
      
      Assert.assertTrue(isAddressSame.apply(TEST_ADDRESS));
      Assert.assertFalse(isAddressSame.apply(TEST_ADDRESS_SERVICE));
      Assert.assertTrue(isAddressSame.apply(TEST_STRING_ADDRESS));
      Assert.assertFalse(isAddressSame.apply(TEST_STRING_ADDRESS_SERVICE));
      
      try {
         isAddressSame.apply(5);
      }
      catch(IllegalArgumentException iae) {
         // Expected
         return;
      }
      Assert.fail("Should have thrown IllegalArguementException");
   }
   
   @Test
   public void testAddressPredicateWithFailOnException() {
      Predicate<Object> isAddressSame = typeCoercer.createPredicate(Address.class, new Predicate<Address>() {
         @Override
         public boolean apply(Address input) {
            return TEST_ADDRESS.equals(input);
         }        
      }, true);
      
      Assert.assertTrue(isAddressSame.apply(TEST_ADDRESS));
      Assert.assertFalse(isAddressSame.apply(TEST_ADDRESS_SERVICE));
      Assert.assertTrue(isAddressSame.apply(TEST_STRING_ADDRESS));
      Assert.assertFalse(isAddressSame.apply(TEST_STRING_ADDRESS_SERVICE));
      
      Assert.assertFalse(isAddressSame.apply(5));
   }
   
   @Test
   public void testAddressListPredicate() {
      Predicate<Object> areAddressesSame = typeCoercer.createListPredicate(Address.class, new Predicate<List<Address>>() {
         @Override
         public boolean apply(List<Address> input) {
            return verify(input);
         }
      });
      
      Assert.assertTrue(areAddressesSame.apply(addressArray));
      Assert.assertTrue(areAddressesSame.apply(addressList));
      Assert.assertTrue(areAddressesSame.apply(addressSet));
      Assert.assertTrue(areAddressesSame.apply(stringArray));
      Assert.assertTrue(areAddressesSame.apply(stringList));
      Assert.assertTrue(areAddressesSame.apply(stringSet));
      Assert.assertTrue(areAddressesSame.apply(listString));
      Assert.assertFalse(areAddressesSame.apply(falseStringArray));
   }
   
   @Test
   public void testAddressSetPredicate() {
      Predicate<Object> areAddressesSame = typeCoercer.createSetPredicate(Address.class, new Predicate<Set<Address>>() {
         @Override
         public boolean apply(Set<Address> input) {
            return verify(input);
         }
      });
      
      Assert.assertTrue(areAddressesSame.apply(addressArray));
      Assert.assertTrue(areAddressesSame.apply(addressList));
      Assert.assertTrue(areAddressesSame.apply(addressSet));
      Assert.assertTrue(areAddressesSame.apply(stringArray));
      Assert.assertTrue(areAddressesSame.apply(stringList));
      Assert.assertTrue(areAddressesSame.apply(stringSet));
      Assert.assertTrue(areAddressesSame.apply(listString));
      Assert.assertFalse(areAddressesSame.apply(falseStringArray));
   }
   
   @Test
   public void testSupportedCollectionPredicate() {
      Predicate<Type> isSupportedCollection = typeCoercer.createSupportedCollectionPredicate(Address.class);
      Assert.assertTrue(isSupportedCollection.apply(addressArray.getClass()));
      Assert.assertTrue(isSupportedCollection.apply(addressList.getClass()));
      Assert.assertTrue(isSupportedCollection.apply(addressSet.getClass()));
      Assert.assertTrue(isSupportedCollection.apply(stringArray.getClass()));
      Assert.assertTrue(isSupportedCollection.apply(stringList.getClass()));
      Assert.assertTrue(isSupportedCollection.apply(stringSet.getClass()));
      Assert.assertTrue(isSupportedCollection.apply(listString.getClass()));
      Assert.assertTrue(isSupportedCollection.apply(falseStringArray.getClass()));
      Assert.assertFalse(isSupportedCollection.apply(Integer.class));
   }
   
   private boolean verify(Collection<Address> collection) {
      Set<Address> actual = new HashSet<>(collection);
      return addressSet.equals(actual);
   }
}

