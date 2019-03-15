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
package com.iris.platform.history.appender.matcher;

import org.apache.commons.lang3.StringUtils;

import com.iris.messages.MessageBody;

public class MatchResults {

	public final static MatchResults FALSE = new MatchResults(false);

	private boolean match;
	private String foundAttrib;
	private String foundInstance;
	private Object foundValue;
	private String baseAttrib;
	private MessageBody body;

	public MatchResults(boolean match) {
		this.match = match;
	}

	public MatchResults(boolean match, MessageBody body) {
	   this.match = match;
	   this.body = body;
	}

	public MatchResults(String baseAttrib, String foundAttrib, Object foundValue, MessageBody body) {
		this.match = true;
		this.baseAttrib = baseAttrib;
		this.foundAttrib = foundAttrib;
		this.foundValue = foundValue;
		this.foundInstance = extractInstance(baseAttrib, foundAttrib);
		this.body = body;
	}

	private String extractInstance(String baseAttrib,String foundAttrib) {
		if (foundAttrib.length() > baseAttrib.length()) {
			String instance = foundAttrib.substring(baseAttrib.length() + 1);
			return StringUtils.capitalize(instance);
		}
		return null;
	}

	public boolean isMatch() {
		return match;
	}

	public String getFoundAttrib() {
		return foundAttrib;
	}

	public String getFoundInstance() {
		return foundInstance;
	}

	public Object getFoundValue() {
		return foundValue;
	}

	public String getBaseAttrib() {
		return baseAttrib;
	}

	public MessageBody getBody() {
		return body;
	}

	public void setValue(Object value) {
		this.foundValue = value;
	}

}

