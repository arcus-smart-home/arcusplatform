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
package com.iris.platform.address;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * TestStreetAddressComparator
 * 
 * Junit test over <code>StreetAddressEqualityComparator</code> on
 * <code>StreetAddress</code>
 * 
 * The test validates the correct operation of the comparator for use as a
 * conditional equality test in <code>PremiseService#validateAddress</code> and
 * <code>SmartyStreetsValidator#validate</code>
 * 
 * Basic tests should guarantee that two <code>StreetAddress</code> are
 * conditionally equal if the backing fields meet the requirements of equality
 * without consideration for case (case insensitive).
 * 
 * An additional requirement is that zip-9 zip codes are to be considered equal
 * to zip-5 addresses if the zip-9 and zip-5 address share the same 5 digit
 * prefix.
 * 
 * @author Trip, Terry Trippany
 */
public class TestStreetAddressComparator {

	private StreetAddress s1;
	private StreetAddress s1PlusNine;
	private StreetAddress s2;
	private StreetAddress s3;
	private StreetAddress s4;

	private List<StreetAddress> streetAddresses = new ArrayList<>();

	private StreetAddressLenientComparator comparator = new StreetAddressLenientComparator();

	@Before
	public void init() {
		s1 = new StreetAddress();
		s1.setCity("Chicago");
		s1.setState("IL");
		s1.setLine1("222 S Riverside Plaza");
		s1.setZip("60606");

		s1PlusNine = s1.copy();
		s1PlusNine.setZip("60606-1234");

		s2 = new StreetAddress();
		s2.setCity("Chicago");
		s2.setState("IL");
		s2.setLine1("223 S Riverside Plaza");
		s2.setZip("60607");

		s3 = new StreetAddress();
		s3.setCity("Chicago");
		s3.setState("IL");
		s3.setLine1("224 S Riverside Plaza");
		s3.setLine2("28th Floor");
		s3.setZip("60608");

		s4 = s3.copy();
		s4.setCity("ABCD");

		streetAddresses.add(s1);
		streetAddresses.add(s2);
		streetAddresses.add(s3);
	}

	/**
	 * Test that equality fails due to zip=9 and that the comparator returns 0
	 */
	@Test
	public void testZipPlusNineEquality() {
		assertNotEquals(s1, s1PlusNine);
		assertTrue(comparator.compare(s1, s1PlusNine) == 0);
	}

	/**
	 * Test that <code>List#contains</code> fails due to zip=9 differences and
	 * that the comparator returns 0 in a short-circuiting terminal operation.
	 * This mimics contains using the comparator for an equality like test as in
	 * <code>SmartyStreetsValidator#validate</code>
	 */
	@Test
	public void testListOperations() {
		assertFalse(streetAddresses.contains(s1PlusNine));
		assertTrue(streetAddresses.stream().anyMatch(s -> comparator.compare(s1PlusNine, s) == 0));
	}

	/**
	 * Test that equals and compare operations fail as expected for
	 * <code>StreetAddress<c/ode> objects that do not match.
	 */
	@Test
	public void testNotAMatchOfAnyKind() {
		assertFalse(s4.equals(s3));
		assertFalse(streetAddresses.contains(s4));
		assertFalse(streetAddresses.stream().anyMatch(s -> comparator.compare(s4, s) == 0));
	}

	/**
	 * Test that equals fails and compare succeeds as expected for
	 * <code>StreetAddress<c/ode> that differ only by case.
	 * 
	 * Add an additional zip-9 test.
	 */
	@Test
	public void testCaseInsensitiveCompare() {
		StreetAddress s5 = new StreetAddress();

		s5.setCity(s4.getCity().toLowerCase());
		s5.setState(s4.getState().toLowerCase());
		s5.setLine1(s4.getLine1().toLowerCase());
		s5.setLine2(s4.getLine2().toLowerCase());
		s5.setZip(s4.getZip());
		assertFalse(s4.equals(s5));
		assertTrue(comparator.compare(s4, s5) == 0);

		// for the sake of comparison zip+9 should conceptually equal zip-5 for
		// all zip-9 zip codes that have the same zip-5 prefix

		s5.setZip(s4.getZip() + "-1234");
		assertTrue(comparator.compare(s4, s5) == 0);
	}
}

