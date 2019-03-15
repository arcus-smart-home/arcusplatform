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
package com.iris.client;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class ClientRequest {
   private String address;
   private String command;
   private Map<String, Object> attributes;
   private int timeoutMs;
   private boolean restfulRequest = false;
   private String connectionURL;
   
   public String getAddress() {
      return address;
   }
   
   public void setAddress(String address) {
      this.address = address;
   }
   
   public String getCommand() {
      return command;
   }
   
   public void setCommand(String command) {
      this.command = command;
   }
   
   public Map<String, Object> getAttributes() {
      return attributes;
   }
   
   public Object getAttribute(String key) {
      return attributes.get(key);
   }
   
   public void setAttributes(Map<String, Object> attributes) {
      this.attributes = attributes;
   }

   public String getConnectionURL() {
		return connectionURL;
	}

	public void setConnectionURL(String connectionURL) {
		this.connectionURL = connectionURL;
	}

	public void setAttribute(String key, Object value) {
      if(attributes == null) {
         attributes = new HashMap<String, Object>();
      }
      attributes.put(key, value);
   }
   
   public int getTimeoutMs() {
      return timeoutMs;
   }
   
   public void setTimeoutMs(int timeoutMs) {
      this.timeoutMs = timeoutMs;
   }

	public boolean isRestfulRequest() {
		return restfulRequest;
	}

	public void setRestfulRequest(boolean restfulRequest) {
		this.restfulRequest = restfulRequest;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((address == null) ? 0 : address.hashCode());
		result = prime * result
				+ ((attributes == null) ? 0 : attributes.hashCode());
		result = prime * result + ((command == null) ? 0 : command.hashCode());
		result = prime * result
				+ ((connectionURL == null) ? 0 : connectionURL.hashCode());
		result = prime * result + (restfulRequest ? 1231 : 1237);
		result = prime * result + timeoutMs;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ClientRequest other = (ClientRequest) obj;
		if (address == null) {
			if (other.address != null)
				return false;
		} else if (!address.equals(other.address))
			return false;
		if (attributes == null) {
			if (other.attributes != null)
				return false;
		} else if (!attributes.equals(other.attributes))
			return false;
		if (command == null) {
			if (other.command != null)
				return false;
		} else if (!command.equals(other.command))
			return false;
		if (connectionURL == null) {
			if (other.connectionURL != null)
				return false;
		} else if (!connectionURL.equals(other.connectionURL))
			return false;
		if (restfulRequest != other.restfulRequest)
			return false;
		if (timeoutMs != other.timeoutMs)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ClientRequest [address=" + address + ", command=" + command
				+ ", attributes=" + attributes + ", timeoutMs=" + timeoutMs
				+ ", restfulRequest=" + restfulRequest + ", connectionURL="
				+ connectionURL + "]";
	}
}

