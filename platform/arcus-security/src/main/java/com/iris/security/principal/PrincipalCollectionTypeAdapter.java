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

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.shiro.subject.SimplePrincipalCollection;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class PrincipalCollectionTypeAdapter implements JsonSerializer<SimplePrincipalCollection>, JsonDeserializer<SimplePrincipalCollection> {

	private static final String ATTR_PRINCIPAL_MAP = "principals";
	
	@SuppressWarnings("rawtypes")
   @Override
   public JsonElement serialize(SimplePrincipalCollection src, Type typeOfSrc, JsonSerializationContext context) {
	   JsonObject response = new JsonObject();
	   JsonArray principals = new JsonArray();
	   Set<String> realms = src.getRealmNames();
	   if (realms != null) {
		   for (String realm : realms) {
		   	JsonObject jsonRealm = new JsonObject();
		   	JsonArray realmPrincipals = new JsonArray();
		   	Collection principalCollection = src.fromRealm(realm);
		   	if (principalCollection != null && !principalCollection.isEmpty()) {
		   		for (Object value : principalCollection) {
		   			realmPrincipals.add(context.serialize(value));
		   		}
		   	}
		      jsonRealm.add(realm, realmPrincipals);
		      principals.add(jsonRealm);
		   }
	   }
	   response.add(ATTR_PRINCIPAL_MAP, principals);
	   return response;
   }
	
	@Override
   public SimplePrincipalCollection deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
	   JsonObject src = json.getAsJsonObject();
	   SimplePrincipalCollection spc = new SimplePrincipalCollection();
	   
	   JsonElement principalsElement = src.get(ATTR_PRINCIPAL_MAP);
	   if (principalsElement.isJsonArray()) {
	   	JsonArray principalsArray = principalsElement.getAsJsonArray();
		   Iterator<JsonElement> principalsIterator = principalsArray.iterator();
		   while (principalsIterator.hasNext()) {
		   	JsonElement realmElement = principalsIterator.next();
		   	if (realmElement.isJsonObject()) {
		   		JsonObject realmObject = realmElement.getAsJsonObject();
		   		for (Map.Entry<String, JsonElement> entry : realmObject.entrySet()) {
		   			if (entry.getValue().isJsonArray()) {
		   				Iterator<JsonElement> realmIterator = entry.getValue().getAsJsonArray().iterator();
		   				while (realmIterator.hasNext()) {
		   					spc.add(context.deserialize(realmIterator.next(), DefaultPrincipal.class), entry.getKey());
		   				}
		   			}
		   		}
		   	}
		   }
	   }
	   
	   return spc;
   }
}

