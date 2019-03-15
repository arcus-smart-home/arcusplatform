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

import java.util.Collection;
import java.util.Iterator;

import com.google.common.base.Joiner;
import com.iris.messages.address.Address;

public class NotificationHelper {
	private final static Joiner joiner = Joiner.on(',');
	
	public static String multiParams(String... params) {
		if (params == null) {
			return "";
		}
		if (params.length == 1) {
			return params[0];
		}
		return "[" + joiner.join(params) + "]";
	}
	
	public static String multiParams(Collection<String> params) {
		if (params == null) {
			return "";
		}
		return multiParams(params.toArray(new String[params.size()]));
	}
	
	public static String addrParam(String attribute, Address addr) {
		return addrParam(attribute, addr.getRepresentation());
	}
	
	public static String addrParams(String attribute, Collection<Address> addrs) {
		if (addrs == null || addrs.isEmpty()) {
			return "";
		}
		String[] params = new String[addrs.size()];
		int index = 0;
		Iterator<Address> it = addrs.iterator();
		while (it.hasNext()) {
			params[index++] = addrParam(attribute, it.next());
		}
		return multiParams(params);
	}
	
	public static String addrParam(String attribute, String addr) {
		return "{" + addr.trim() + "}." + attribute;
	}
	
	public static String addrParamsAsStrings(String attribute, Collection<String> addrs) {
		if (addrs == null || addrs.isEmpty()) {
			return "";
		}
		String[] params = new String[addrs.size()];
		int index = 0;
		Iterator<String> it = addrs.iterator();
		while (it.hasNext()) {
			params[index++] = addrParam(attribute, it.next());
		}
		return multiParams(params);
	}
}

