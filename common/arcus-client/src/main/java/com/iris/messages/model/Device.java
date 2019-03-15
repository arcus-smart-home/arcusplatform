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
package com.iris.messages.model;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.iris.device.attributes.AttributeMap;
import com.iris.model.Version;

/**
 *
 */
public class Device extends BaseEntity<UUID, Device> {
   // see https://eyeris.atlassian.net/wiki/display/I2D/Device+-+States for a list of states
   public static final String STATE_CREATED                = "created";
   public static final String STATE_ACTIVE_PROVISIONING    = "provisioning";
   public static final String STATE_ACTIVE_SUPPORTED       = "active";
   public static final String STATE_ACTIVE_UNSUPPORTED     = "unsupported";
   public static final String STATE_LOST_RECOVERABLE       = "recoverable";
   public static final String STATE_LOST_UNRECOVERABLE     = "unrecoverable";
   public static final String STATE_TOMBSTONED             = "tombstoned";
   private static final String DFLT_NAME                   = "New Device";

   public static final String DEGRADED_CODE_NONE           = "none";
   public static final String DEGRADED_CODE_HUB_FIRMWARE   = "hub.firmware";
   
   private UUID accountId;
   private String protocol;
   private String protocolId;
   private String driverName;
   private Version driverVersion = Version.UNVERSIONED;
   private String driverAddress;
   private String protocolAddress;
   private String hubId;
   private UUID placeId;
   private Set<String> caps;
   private String devTypeHint;
   private String name = DFLT_NAME;
   private String vendor;
   private String model;
   private String productId;
   private String subprotocol;
	private String state;
	private String degradedCode;
	private boolean hubLocal;
	private AttributeMap protocolAttributes = AttributeMap.emptyMap();

	
	
	public Device() {
	   this.degradedCode = DEGRADED_CODE_NONE;
	}
	
   @Override
   public String getType() {
      return "dev";
   }

   @Override
   public String getAddress() {
      return getDriverAddress();
   }
   public void setAddress(String address) {
      setDriverAddress(address);
   }

   /** @deprecated Use getAccount */
	@Deprecated
   public UUID getAccountId() {
	   return accountId;
   }
	public UUID getAccount() { return getAccountId(); }

	/** @deprecated Use setAccount */
   @Deprecated
   public void setAccountId(UUID accountId) {
	   this.accountId = accountId;
   }
	public void setAccount(UUID accountId) { setAccountId(accountId); }

   public String getProtocol() {
      return protocol;
   }

   public void setProtocol(String protocol) {
      this.protocol = protocol;
   }

   /** @deprecated Use getProtocolid */
   @Deprecated
   public String getProtocolId() {
      return protocolId;
   }
   public String getProtocolid() { return getProtocolId(); }

   /** @deprecated Use setProtocolid */
   @Deprecated
   public void setProtocolId(String protocolId) {
      this.protocolId = protocolId;
   }
   public void setProtocolid(String protocolId) { setProtocolId(protocolId); }

   public DriverId getDriverId() {
      return new DriverId(driverName, driverVersion);
   }

   public void setDriverId(DriverId driverId) {
      this.driverName = driverId.getName();
      this.driverVersion = driverId.getVersion();
   }

   public String getDrivername() { return this.driverName; }

   /** @deprecated Use setDrivername */
   @Deprecated
   public void setDriverName(String driverName) {
      this.driverName = driverName;
   }
   public void setDrivername(String driverName) { setDriverName(driverName); }

   public Version getDriverversion() { return this.driverVersion; }
   public void setDriverversion(Version version) { this.driverVersion = version; }

   /** @deprecated use getAddress() */
   @Deprecated
   public String getDriverAddress() {
      return driverAddress;
   }

   /** @deprecated use setAddress() */
	@Deprecated
   public void setDriverAddress(String driverAddress) {
		this.driverAddress = driverAddress;
	}

	public String getProtocolAddress() {
		return protocolAddress;
	}

	public void setProtocolAddress(String protocolAddress) {
		this.protocolAddress = protocolAddress;
	}

	public String getHubId() {
		return hubId;
	}

	public void setHubId(String hubId) {
		this.hubId = hubId;
	}

	/** @deprecated Use getPlace */
	@Deprecated
   public UUID getPlaceId() {
		return placeId;
	}
	public UUID getPlace() { return getPlaceId(); }

	/** @deprecated Use setPlace */
   @Deprecated
   public void setPlaceId(UUID placeId) {
		this.placeId = placeId;
	}
	public void setPlace(UUID placeId) { setPlaceId(placeId); }

	@Override
   public Set<String> getCaps() {
		return caps;
	}

	public void setCaps(Set<String> caps) {
		this.caps = caps;
	}

	/** @deprecated Use getDevtypehint */
   @Deprecated
   public String getDevTypeHint() {
		return devTypeHint;
	}
	public String getDevtypehint() { return getDevTypeHint(); }

	/** @deprecated Use setDevtypehint */
   @Deprecated
   public void setDevTypeHint(String devTypeHint) {
		this.devTypeHint = devTypeHint;
	}
	public void setDevtypehint(String devTypeHint) { setDevTypeHint(devTypeHint); }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVendor() {
		return vendor;
	}

	public void setVendor(String vendor) {
		this.vendor = vendor;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}
	
	public String getProductId() {
		return productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	public String getSubprotocol() {
		return subprotocol;
	}

	public void setSubprotocol(String subprotocol) {
		this.subprotocol = subprotocol;
	}

	public String getDegradedCode() {
		return degradedCode;
	}

	public void setDegradedCode(String degradedCode) {
		this.degradedCode = (degradedCode == null) 
		   ? DEGRADED_CODE_NONE
		   : degradedCode;
	}

	public boolean isHubLocal() {
		return hubLocal;
	}

	public void setHubLocal(boolean hubLocal) {
	   this.hubLocal = hubLocal;
	}

	public final String getState() {
		return state;
	}

	public final void setState(String state) {
		this.state = state;
	}
	
   public boolean isActive() {
      return Device.STATE_ACTIVE_PROVISIONING.equals(state) || Device.STATE_ACTIVE_SUPPORTED.equals(state) || Device.STATE_ACTIVE_UNSUPPORTED.equals(state);
   }

   public boolean isTombstoned() {
      return Device.STATE_TOMBSTONED.equals(state);
   }

   public boolean isLost() {
      return Device.STATE_LOST_RECOVERABLE.equals(state) || Device.STATE_LOST_UNRECOVERABLE.equals(state);
   }

	public AttributeMap getProtocolAttributes() {
      return AttributeMap.copyOf(protocolAttributes);
   }

   public void setProtocolAttributes(AttributeMap protocolAttributes) {
      this.protocolAttributes = AttributeMap.copyOf(protocolAttributes);
   }

   public Date getAdded() { return getCreated(); }
   public void setAdded(Date date) { setCreated(date); }

   /* (non-Javadoc)
    * @see com.iris.messages.model.BaseEntity#copy()
    */
   @Override
   public Device copy() {
      Device copy = super.copy();
      if(caps != null) {
         copy.setCaps(new HashSet<>(caps));
      }
      copy.setProtocolAttributes(AttributeMap.copyOf(protocolAttributes));
      return copy;
   }

   @Override
   public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((accountId == null) ? 0 : accountId.hashCode());
		result = prime * result + ((caps == null) ? 0 : caps.hashCode());
		result = prime * result
				+ ((devTypeHint == null) ? 0 : devTypeHint.hashCode());
		result = prime * result
				+ ((driverAddress == null) ? 0 : driverAddress.hashCode());
		result = prime * result
				+ ((driverName == null) ? 0 : driverName.hashCode());
		result = prime * result
				+ ((driverVersion == null) ? 0 : driverVersion.hashCode());
		result = prime * result + ((hubId == null) ? 0 : hubId.hashCode());
		result = prime * result + ((model == null) ? 0 : model.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((placeId == null) ? 0 : placeId.hashCode());
		result = prime * result
				+ ((productId == null) ? 0 : productId.hashCode());
		result = prime * result + ((protocol == null) ? 0 : protocol.hashCode());
		result = prime * result
				+ ((protocolAddress == null) ? 0 : protocolAddress.hashCode());
		result = prime
				* result
				+ ((protocolAttributes == null) ? 0 : protocolAttributes.hashCode());
		result = prime * result
				+ ((protocolId == null) ? 0 : protocolId.hashCode());
		result = prime * result + ((state == null) ? 0 : state.hashCode());
		result = prime * result
				+ ((subprotocol == null) ? 0 : subprotocol.hashCode());
		result = prime * result + ((vendor == null) ? 0 : vendor.hashCode());
		result = prime * result
				+ ((degradedCode == null) ? 0 : degradedCode.hashCode());
		result = prime * result + (hubLocal ? 1 : 0);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Device other = (Device) obj;
		if (accountId == null) {
			if (other.accountId != null)
				return false;
		} else if (!accountId.equals(other.accountId))
			return false;
		if (caps == null) {
			if (other.caps != null)
				return false;
		} else if (!caps.equals(other.caps))
			return false;
		if (devTypeHint == null) {
			if (other.devTypeHint != null)
				return false;
		} else if (!devTypeHint.equals(other.devTypeHint))
			return false;
		if (driverAddress == null) {
			if (other.driverAddress != null)
				return false;
		} else if (!driverAddress.equals(other.driverAddress))
			return false;
		if (driverName == null) {
			if (other.driverName != null)
				return false;
		} else if (!driverName.equals(other.driverName))
			return false;
		if (driverVersion == null) {
			if (other.driverVersion != null)
				return false;
		} else if (!driverVersion.equals(other.driverVersion))
			return false;
		if (hubId == null) {
			if (other.hubId != null)
				return false;
		} else if (!hubId.equals(other.hubId))
			return false;
		if (model == null) {
			if (other.model != null)
				return false;
		} else if (!model.equals(other.model))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (placeId == null) {
			if (other.placeId != null)
				return false;
		} else if (!placeId.equals(other.placeId))
			return false;
		if (productId == null) {
			if (other.productId != null)
				return false;
		} else if (!productId.equals(other.productId))
			return false;
		if (protocol == null) {
			if (other.protocol != null)
				return false;
		} else if (!protocol.equals(other.protocol))
			return false;
		if (protocolAddress == null) {
			if (other.protocolAddress != null)
				return false;
		} else if (!protocolAddress.equals(other.protocolAddress))
			return false;
		if (protocolAttributes == null) {
			if (other.protocolAttributes != null)
				return false;
		} else if (!protocolAttributes.equals(other.protocolAttributes))
			return false;
		if (protocolId == null) {
			if (other.protocolId != null)
				return false;
		} else if (!protocolId.equals(other.protocolId))
			return false;
		if (state == null) {
			if (other.state != null)
				return false;
		} else if (!state.equals(other.state))
			return false;
		if (subprotocol == null) {
			if (other.subprotocol != null)
				return false;
		} else if (!subprotocol.equals(other.subprotocol))
			return false;
		if (vendor == null) {
			if (other.vendor != null)
				return false;
		} else if (!vendor.equals(other.vendor))
			return false;
		if (degradedCode == null) {
			if (other.degradedCode != null)
				return false;
		} else if (!degradedCode.equals(other.degradedCode))
			return false;
		if (hubLocal != other.hubLocal)
		   return false;
		return true;
	}

	@Override
	public String toString() {
		return "Device [accountId=" + accountId + ", protocol=" + protocol
			+ ", protocolId=" + protocolId + ", driverName=" + driverName
			+ ", driverVersion=" + driverVersion + ", driverAddress="
			+ driverAddress + ", protocolAddress=" + protocolAddress
			+ ", hubId=" + hubId + ", placeId=" + placeId + ", caps=" + caps
			+ ", devTypeHint=" + devTypeHint + ", name=" + name + ", vendor="
			+ vendor + ", model=" + model + ", productId=" + productId
			+ ", subprotocol=" + subprotocol + ", state=" + state
			+ ", protocolAttributes=" + protocolAttributes
			+ ", degradedCode=" + degradedCode + ", hubLocal=" + hubLocal
			+ ", getId()=" + getId() + ", getCreated()=" + getCreated()
			+ ", getModified()=" + getModified() + ", getTags()=" + getTags()
			+ ", getImages()=" + getImages() + "]";
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		Device device = (Device) super.clone();
		device.caps = this.caps == null ? null : new HashSet<String>(caps);
		device.protocolAttributes = AttributeMap.copyOf(this.protocolAttributes);
		return device;
	}

}

