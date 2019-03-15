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
package com.iris.client.bounce;

import java.util.List;
import java.util.Map;

import com.iris.bridge.server.netty.BridgeHeaders.OsType;

import io.netty.handler.codec.http.QueryStringEncoder;

public class RequestInfo {
	private OsType type;
	private String subpath;
	private String token;
	private Map<String, List<String>> queryParameters;
	
	public OsType getType() {
		return type;
	}
	
	public void setType(OsType type) {
		this.type = type;
	}
	
	public String getSubpath() {
		return subpath;
	}
	
	public void setSubpath(String subpath) {
		this.subpath = subpath;
	}
	
	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public Map<String, List<String>> getQueryParameters() {
		return queryParameters;
	}
	
	public void setQueryParameters(Map<String, List<String>> queryParameters) {
		this.queryParameters = queryParameters;
	}

	public void addQueryParam(String key, List<String> value) {
		if(queryParameters == null) {
			queryParameters = new java.util.LinkedHashMap<>();
		}
		queryParameters.put(key, value);
	}

	public QueryStringEncoder toQueryString(String prefix) {
		QueryStringEncoder encoder = new QueryStringEncoder(prefix + subpath);
		if(queryParameters != null) {
			for(Map.Entry<String, List<String>> e: queryParameters.entrySet()) {
				if(e.getValue().isEmpty()) {
					encoder.addParam(e.getKey(), "");
				}
				else {
					e.getValue().stream().forEach((v) -> encoder.addParam(e.getKey(), v));
				}
			}
		}
		return encoder;
	}

}

