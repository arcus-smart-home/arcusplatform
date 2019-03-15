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
package com.iris.platform.manufacture.kitting.kit;

import org.apache.commons.lang3.StringUtils;

public class KitDevice {
	private final String euid;
	private final String installCode;
	private final String type;
	
	public KitDevice(String euid, String installCode, String type) {
		this.euid = euid;
		this.installCode = installCode;
		this.type = type;
	}
	
	public static class Builder {
		private String euid;
		private String installCode;
		private String type;

		public Builder() {
		}
		
		public Builder withEuid(String euid) {
			this.euid = euid;
			return this;
		}
		
		public Builder withInstallCode(String installCode) {
			this.installCode = installCode;
			return this;
		}
		
		public Builder withType(String type) {
			this.type = type;
			return this;
		}
		
		public KitDevice build() {
			return new KitDevice(euid,installCode,type);
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	public String getEuid() {
		return euid;
	}

	public String getInstallCode() {
		return installCode;
	}

	public String getType() {
		return type;
	}

	@Override
	public String toString() {
		return "KitDevice [euid=" + euid + ", installCode=" + installCode + ", type=" + type + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((euid == null) ? 0 : euid.hashCode());
		result = prime * result + ((installCode == null) ? 0 : installCode.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		KitDevice other = (KitDevice) obj;
		if (euid == null) {
			if (other.euid != null)
				return false;
		} else if (!euid.equals(other.euid))
			return false;
		if (installCode == null) {
			if (other.installCode != null)
				return false;
		} else if (!installCode.equals(other.installCode))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}
	
	public enum Error {
		OK("Everything is good"),
		EUID_IS_MISSING("EUID is missing"),
		EUID_IS_INCORRECT_LENGTH("EUID is the wrong length"),
		INSTALL_CODE_IS_MISSING("Install code is missing"),
		INSTALL_CODE_IS_INCORRECT_LENGTH("Install code is the wrong length should be 36 bytes long"),
		TYPE_IS_MISSING("No type of device specified");
		
		private final String message;
		
		Error(String message) {
			this.message = message;
		}
		
		public String getMessage() {
			return message;
		}
	}

	public Error isValid() {
		if (StringUtils.isBlank(euid)) return Error.EUID_IS_MISSING;
		if (euid.length() != 16) return Error.INSTALL_CODE_IS_INCORRECT_LENGTH;
		if (StringUtils.isBlank(installCode)) return Error.INSTALL_CODE_IS_MISSING;
		if (installCode.length() != 36) return Error.INSTALL_CODE_IS_INCORRECT_LENGTH;

		// TODO check CRC16 need a CRC16 Utility... Why doesn't this exist?
		if (StringUtils.isBlank(type)) return Error.TYPE_IS_MISSING;
		return Error.OK;
	}	
}

