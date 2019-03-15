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
package com.iris.security.dao;

import java.util.Optional;
import java.util.UUID;

public interface AppHandoffDao {

	/**
	 * Creates a new handoff token for the user,
	 * this will remove any existing tokens.
	 * @param handoff
	 * @return
	 */
	String newToken(SessionHandoff handoff);

	/**
	 * Retrieves the session info related to the token
	 * if it exists.
	 * This method additionally destroys the token. 
	 * @param token
	 * @param ip
	 * @return
	 */
	Optional<SessionHandoff> validate(String token);
	
	public static class SessionHandoff {
		private UUID personId;
		private String url;
		private String ip;
		private String username;
		
		public UUID getPersonId() {
			return personId;
		}
		
		public void setPersonId(UUID personId) {
			this.personId = personId;
		}
		
		public String getUrl() {
			return url;
		}
		
		public void setUrl(String url) {
			this.url = url;
		}
		
		public String getIp() {
			return ip;
		}
		
		public void setIp(String ip) {
			this.ip = ip;
		}
		
		public String getUsername() {
			return username;
		}

		public void setUsername(String username){
			this.username = username;
		}

		@Override
		public String toString() {
			return "HandoffToken [personId=" + personId + ", url=" + url + ", ip=" + ip + ", username=" + username + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((ip == null) ? 0 : ip.hashCode());
			result = prime * result + ((personId == null) ? 0 : personId.hashCode());
			result = prime * result + ((url == null) ? 0 : url.hashCode());
			result = prime * result + ((username == null) ? 0 : username.hashCode());
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
			SessionHandoff other = (SessionHandoff) obj;
			if (ip == null) {
				if (other.ip != null)
					return false;
			} else if (!ip.equals(other.ip))
				return false;
			if (personId == null) {
				if (other.personId != null)
					return false;
			} else if (!personId.equals(other.personId))
				return false;
			if (url == null) {
				if (other.url != null)
					return false;
			} else if (!url.equals(other.url))
				return false;
			if (username == null) {
				if (other.username != null)
					return false;
			} else if (!username.equals(other.username))
				return false;
			return true;
		}

		
		
	}
}

