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


public class ResetPasswordCredentials implements Credentials {
	private String username;
	private String token;
	private String password;
	private String connectionURL;

	public ResetPasswordCredentials() {}
	
   public ResetPasswordCredentials(String username, String token, String password) {
      this.username = username;
      this.token = token;
      this.password = password;
   }
   
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	/**
    * @return the token
    */
   public String getToken() {
      return token;
   }

   /**
    * @param token the token to set
    */
   public void setToken(String token) {
      this.token = token;
   }

   public String getPassword() {
		return String.valueOf(password);
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
	// TODO: Validate?
	public void setConnectionURL(String url) {
		this.connectionURL = url;
	}
	
	public String getConnectionURL() {
		return this.connectionURL;
	}

	@Override
	public String toString() {
		return "Credentials [username=" + username + ", token=" + token + ", password=****, connectionURL=" + connectionURL + "]";
	}
}

