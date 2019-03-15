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
package com.iris.security.principal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.shiro.subject.SimplePrincipalCollection;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.iris.gson.GsonFactory;
import com.iris.util.IrisCollections;

public class TestJSON {
	
	private final String realm1 = "england";
	private final String realm2 = "canada";
	private final String realm3 = "india";
	
	private final UUID uuid1 = UUID.fromString("802a3b26-9945-40c3-a44e-28a0b1b554b2");
	private final UUID uuid2 = UUID.fromString("4a1aae4c-c9ac-4097-b403-9470d629568c");
	private final UUID uuid3 = UUID.fromString("996bdc62-c49e-45fc-996d-37dac7c6dcd4");
	private final UUID uuid4 = UUID.fromString("72cde7b3-e4ed-46c0-9fac-c88d0f34f841");
	
	private final String user1 = "Count Arnold Burke Kidney";
	private final String user2 = "Sir N. B. Potter-Pirbright Esq.";
	private final String user3 = "Lady Charlotte Bannim Duxberry";
	private final String user4 = "Lady Susannah Sessions Jeeves";
	
	private Gson gson;
	
	@Before
   public void setUp() throws Exception {
	   GsonFactory gsonFactory = new GsonFactory(
	   		null,
	   		null,
	   		IrisCollections.<JsonSerializer<?>>setOf(new DefaultPrincipalTypeAdapter(), 
	   				new PrincipalCollectionTypeAdapter()),
				IrisCollections.<JsonDeserializer<?>>setOf(new DefaultPrincipalTypeAdapter(), 
	   				new PrincipalCollectionTypeAdapter()));
	   gson = gsonFactory.get();
   }

	// This is the normal case.
	@Test
	public void testOneRealmOnePrincipal() {	
		SimplePrincipalCollection orig = new Builder().with(realm1, uuid1, user1)
														.build();
		runTest(orig);
	}
	
	@Test
	public void testNoRealms() {
		runTest(
				new Builder().build()
				);
	}
	
	@Test
	public void testOneRealmFourPrincipals() {
		runTest(
					new Builder().with(realm1, uuid1, user1)
							.with(realm1, uuid2, user3)
							.with(realm1, uuid3, user3)
							.with(realm1, uuid4, user4)
							.build()
				);
	}
	
	@Test
	public void testThreeRealms() {
		runTest(
					new Builder().with(realm1, uuid1, user1)
							.with(realm2, uuid2, user2)
							.with(realm2, uuid3, user3)
							.with(realm3, uuid4, user4)
							.build()
				);
	}
	
	private void runTest(SimplePrincipalCollection orig) {
		String json = gson.toJson(orig);
		System.out.println(orig);
		System.out.println(json);
		SimplePrincipalCollection deser = gson.fromJson(json, SimplePrincipalCollection.class);
		Assert.assertEquals(orig, deser);
	}
	
	private static class Builder {
		Map<String, List<DefaultPrincipal>> map = new LinkedHashMap<>();
		
	   Builder with(String realm, UUID id, String user) {
	   	List<DefaultPrincipal> list = map.get(realm);
	   	if (list == null) {
	   		list = new ArrayList<>();
	   		map.put(realm, list);
	   	}
	   	list.add(new DefaultPrincipal(user, id));
	   	return this;
	   }
	   
	   SimplePrincipalCollection build() {
	   	SimplePrincipalCollection pc = new SimplePrincipalCollection();
	   	for (String key : map.keySet()) {
	   		List<DefaultPrincipal> list = map.get(key);
	   		for (DefaultPrincipal dp : list) {
	   			pc.add(dp, key);
	   		}
	   	}
	   	return pc;
	   }
		
	}

}

