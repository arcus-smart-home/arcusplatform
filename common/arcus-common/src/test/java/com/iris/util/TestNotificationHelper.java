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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.iris.messages.address.Address;

public class TestNotificationHelper {
	private static final Address PERSON_ADDR_1 = Address.fromString("SERV:person:83f4407a-ed2a-4d22-b648-c74081ec6c12");
	private static final Address PERSON_ADDR_2 = Address.fromString("SERV:person:94f4407a-ed2a-4d22-b648-c74081ec6c12");
	private static final Address PERSON_ADDR_3 = Address.fromString("SERV:person:a5f4407a-ed2a-4d22-b648-c74081ec6c12");
	private static final Address PERSON_ADDR_4 = Address.fromString("SERV:person:b6f4407a-ed2a-4d22-b648-c74081ec6c12");
	private static final String ATTR = "firstname";
	
	@Test
	public void testMultipleAddresses() {
		String expected = "[{" + PERSON_ADDR_1.getRepresentation() + "}." + ATTR + 
								",{" + PERSON_ADDR_2.getRepresentation() + "}." + ATTR +
								",{" + PERSON_ADDR_3.getRepresentation() + "}." + ATTR +
								",{" + PERSON_ADDR_4.getRepresentation() + "}." + ATTR + "]";
		List<Address> addrs = ImmutableList.of(PERSON_ADDR_1, PERSON_ADDR_2, PERSON_ADDR_3, PERSON_ADDR_4);
		String actual = NotificationHelper.addrParams(ATTR, addrs);
		System.out.println(actual);
		Assert.assertEquals(expected, actual);
	}
}

