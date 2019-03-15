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
package com.iris.billing.client;

import java.util.Random;
import java.util.UUID;

import com.iris.billing.client.model.SubscriptionAddon;
import com.iris.billing.client.model.request.BillingInfoRequest;
import com.iris.billing.client.model.request.SubscriptionRequest;

public class TestUtils {

	public static BillingInfoRequest createFakeBillingInfo() {
		BillingInfoRequest info = new BillingInfoRequest();
		info.setFirstName(UUID.randomUUID().toString());
		info.setLastName("Last");
		info.setYear(2020);
		info.setMonth(10);
		info.setAddress1("123 Street Drive.");
		info.setCity("Billing City");
		info.setState("NC");
		info.setPostalCode("12345");
		info.setVatNumber("VAT12345");
		info.setCountry("USA");
		info.setCardNumber("4111-1111-1111-1111");
		return info;
	}
	
	public static BillingInfoRequest createBadBillingInfo() {
		BillingInfoRequest info = new BillingInfoRequest();
		info.setFirstName(UUID.randomUUID().toString());
		info.setLastName("Last");
		info.setYear(2020);
		info.setMonth(10);
		info.setAddress1("123 Street Drive.");
		info.setCity("Billing City");
		info.setState("NC");
		info.setPostalCode("12345");
		info.setVatNumber("VAT12345");
		info.setCountry("USA");
		info.setCardNumber("4000-0000-0000-0002");
		return info;
	}

	public static SubscriptionRequest createBasicSubscriptionUpdate() {
		SubscriptionRequest sub = new SubscriptionRequest();
		sub.setPlanCode("basic");
		sub.setQuantity(1000);
		return sub;
	}
	
	public static SubscriptionRequest createSubscriptionUsing(String planCode, Integer qty, String currency) {
		SubscriptionRequest sub = new SubscriptionRequest();
		sub.setPlanCode(planCode);
		sub.setQuantity(qty);
		sub.setCurrency(currency);
		return sub;
	}

	public static SubscriptionAddon createSubscriptionAddon() {
		SubscriptionAddon addon = new SubscriptionAddon();
		addon.setQuantity(pseudoRandom(1, 10));
		addon.setAddonCode("cellbackupbasic");
		addon.setUnitAmountInCents("99999"); // doesn't get serialized.
		return addon;
	}

	public static int pseudoRandom(int min, int max) {
	    Random rand = new Random();
	    int randomNum = rand.nextInt((max - min) + 1) + min;
	    return randomNum;
	}
}

