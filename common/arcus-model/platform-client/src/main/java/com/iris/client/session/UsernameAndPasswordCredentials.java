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
/**
 * 
 */
package com.iris.client.session;

import java.util.Arrays;

public class UsernameAndPasswordCredentials implements Credentials {
	private String username;
	private char[] password;
	private String connectionURL;

	public UsernameAndPasswordCredentials() {}
	
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return String.valueOf(password);
	}

	public void setPassword(char[] password) {
		this.password = password;
	}
	
	// TODO: Validate?
	public void setConnectionURL(String url) {
		this.connectionURL = url;
	}
	
	public String getConnectionURL() {
		return this.connectionURL;
	}

	/**
	 * This should be called after the password has been
	 * sent to remove it from memory.
	 */
	public void clearPassword() {
		Arrays.fill(password, (char) 0);
	}

	@Override
	public String toString() {
		return "Credentials [username=" + username + ", password=****, connectionURL=" + connectionURL + "]";
	}
}

