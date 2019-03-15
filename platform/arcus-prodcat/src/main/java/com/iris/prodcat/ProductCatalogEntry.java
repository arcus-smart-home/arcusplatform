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
package com.iris.prodcat;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableSet;
import com.iris.messages.MessageConstants;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.ProductCapability;
import com.iris.model.Version;

public class ProductCatalogEntry implements Comparable<ProductCatalogEntry> {

	private String id;
	private String name;
	private String shortName;
	private String description;
	private String manufacturer;
	private String vendor;
	private String addDevImg;
	private Cert cert;
	private Boolean canBrowse;
	private Boolean canSearch;
	private String helpUrl;
	private String pairVideoUrl;
	private String instructionsUrl;
	private BatterySize batteryPrimSize;
	private Integer batteryPrimNum;
	private BatterySize batteryBackSize;
	private Integer batteryBackNum;
	private String keywords;
	private Boolean OTA;
	private String protoFamily;
	private String protoSpec;
	private String driver;
	private Date added;
	private Date lastChanged;
	private List<String> categories = new ArrayList<String>();
	private List<Step> pair = new ArrayList<Step>();
	private List<Step> removal = new ArrayList<Step>();
	private List<Step> reset = new ArrayList<Step>();
	private List<Step> reconnect = new ArrayList<Step>();
	private List<String> populations = new ArrayList<String>();
	private String screen;
	private Boolean blacklisted;
	private Boolean hubRequired;
	private String minAppVersion;
	private String devRequired;
	private Version minHubFirmware;
	private Boolean canDiscover = Boolean.TRUE;
	private Boolean appRequired = Boolean.FALSE;
	private String installManualUrl;
	private PairingMode pairingMode = PairingMode.HUB;
	private Integer pairingIdleTimeoutMs;
	private Integer pairingTimeoutMs;

   public String getType() {
      return ProductCapability.NAMESPACE;
   }

   public String getAddress() {
      return MessageConstants.SERVICE + ":" + getType() + ":" + getId();
   }

   public Set<String> getCaps() {
      return ImmutableSet.of(Capability.NAMESPACE, getType());
   }

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getShortName() {
      return shortName;
   }

   public void setShortName(String shortName) {
      this.shortName = shortName;
   }

   public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getManufacturer() {
		return manufacturer;
	}

	public void setManufacturer(String manufacturer) {
		this.manufacturer = manufacturer;
	}

	public String getVendor() {
		return vendor;
	}

	public void setVendor(String vendor) {
		this.vendor = vendor;
	}

	public String getAddDevImg() {
		return addDevImg;
	}

	public void setAddDevImg(String addDevImg) {
		this.addDevImg = addDevImg;
	}

	public Cert getCert() {
		return cert;
	}

	public void setCert(Cert cert) {
		this.cert = cert;
	}

	public Boolean getCanBrowse() {
		return canBrowse;
	}

	public void setCanBrowse(Boolean canBrowse) {
		this.canBrowse = canBrowse;
	}

	public Boolean getCanSearch() {
		return canSearch;
	}

	public void setCanSearch(Boolean canSearch) {
		this.canSearch = canSearch;
	}

	public String getHelpUrl() {
		return helpUrl;
	}

	public void setHelpUrl(String helpUrl) {
		this.helpUrl = helpUrl;
	}

	public String getPairVideoUrl() {
		return pairVideoUrl;
	}

	public void setPairVideoUrl(String pairVideoUrl) {
		this.pairVideoUrl = pairVideoUrl;
	}

	public String getInstructionsUrl() {
		return instructionsUrl;
	}

	public void setInstructionsUrl(String instructionsUrl) {
		this.instructionsUrl = instructionsUrl;
	}

	public BatterySize getBatteryPrimSize() {
		return batteryPrimSize;
	}

	public void setBatteryPrimSize(BatterySize batteryPrimSize) {
		this.batteryPrimSize = batteryPrimSize;
	}

	public Integer getBatteryPrimNum() {
		return batteryPrimNum;
	}

	public void setBatteryPrimNum(Integer batteryPrimNum) {
		this.batteryPrimNum = batteryPrimNum;
	}

	public BatterySize getBatteryBackSize() {
		return batteryBackSize;
	}

	public void setBatteryBackSize(BatterySize batteryBackSize) {
		this.batteryBackSize = batteryBackSize;
	}

	public Integer getBatteryBackNum() {
		return batteryBackNum;
	}

	public void setBatteryBackNum(Integer batteryBackNum) {
		this.batteryBackNum = batteryBackNum;
	}

	public String getKeywords() {
		return keywords;
	}

	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}

	public Boolean getOTA() {
		return OTA;
	}

	public void setOTA(Boolean oTA) {
		OTA = oTA;
	}

	public String getProtoFamily() {
		return protoFamily;
	}

	public void setProtoFamily(String protoFamily) {
		this.protoFamily = protoFamily;
	}

	public String getProtoSpec() {
		return protoSpec;
	}

	public void setProtoSpec(String protoSpec) {
		this.protoSpec = protoSpec;
	}

	public String getDriver() {
		return driver;
	}

	public void setDriver(String driver) {
		this.driver = driver;
	}

	public Date getAdded() {
		return added;
	}

	public void setAdded(Date added) {
		this.added = added;
	}

	public Date getLastChanged() {
		return lastChanged;
	}

	public void setLastChanged(Date lastChanged) {
		this.lastChanged = lastChanged;
	}

	public List<String> getCategories() {
		return categories;
	}

	public void setCategories(List<String> categories) {
		this.categories = categories;
	}

	public List<String> getPopulations() {
		return populations;
	}

	public void setPopulations(List<String> populations) {
		this.populations = populations;
	}

	public List<Step> getPair() {
		return pair;
	}

	public void setPair(List<Step> pair) {
		this.pair = pair;
	}
	
	public List<Step> getRemoval() {
		return removal;
	}

	public void setRemoval(List<Step> removal) {
		this.removal = removal;
	}

	public List<Step> getReset() {
		return reset;
	}

	public void setReset(List<Step> reset) {
		this.reset = reset;
	}
	
	public List<Step> getReconnect() {
		return reconnect;
	}

	public void setReconnect(List<Step> reset) {
		this.reconnect = reset;
	}

	public String getScreen() {
      return screen;
   }

   public void setScreen(String screen) {
      this.screen = screen;
   }

	public Boolean getHubRequired() {
		return hubRequired;
	}

	public void setHubRequired(Boolean hubRequired) {
		this.hubRequired = hubRequired;
	}
	
	public String getDevRequired() {
		return devRequired;
	}
	
	public void setDevRequired(String devRequired) {
		this.devRequired = devRequired;
	}
	
	public String getMinAppVersion() {
		return minAppVersion;
	}
	

	public void setMinAppVersion(String minAppVersion) {
		this.minAppVersion = minAppVersion;
	}

	public Version getMinHubFirmware(){
      return minHubFirmware;
   }

   public void setMinHubFirmware(Version minHubFirmware){
      this.minHubFirmware = minHubFirmware;
   };
   
   public Boolean getCanDiscover() {
      return canDiscover;
   }

   public void setCanDiscover(Boolean canDiscover) {
      this.canDiscover = canDiscover;
   }
   
   public Boolean getAppRequired() {
		return appRequired;
	}

	public void setAppRequired(Boolean appRequired) {
		this.appRequired = appRequired;
	}
	
	public String getInstallManualUrl() {
		return installManualUrl;
	}

	public void setInstallManualUrl(String installManualUrl) {
		this.installManualUrl = installManualUrl;
	}

	public PairingMode getPairingMode() {
		return pairingMode;
	}

	public void setPairingMode(PairingMode pairingMode) {
		this.pairingMode = pairingMode;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((OTA == null) ? 0 : OTA.hashCode());
		result = prime * result
				+ ((addDevImg == null) ? 0 : addDevImg.hashCode());
		result = prime * result + ((added == null) ? 0 : added.hashCode());
		result = prime * result
				+ ((batteryBackNum == null) ? 0 : batteryBackNum.hashCode());
		result = prime * result
				+ ((batteryBackSize == null) ? 0 : batteryBackSize.hashCode());
		result = prime * result
				+ ((batteryPrimNum == null) ? 0 : batteryPrimNum.hashCode());
		result = prime * result
				+ ((batteryPrimSize == null) ? 0 : batteryPrimSize.hashCode());
		result = prime * result
				+ ((blacklisted == null) ? 0 : blacklisted.hashCode());
		result = prime * result
				+ ((canBrowse == null) ? 0 : canBrowse.hashCode());
		result = prime * result
				+ ((canSearch == null) ? 0 : canSearch.hashCode());
		result = prime * result
				+ ((categories == null) ? 0 : categories.hashCode());
		result = prime * result + ((cert == null) ? 0 : cert.hashCode());
		result = prime * result
				+ ((description == null) ? 0 : description.hashCode());
		result = prime * result
				+ ((devRequired == null) ? 0 : devRequired.hashCode());
		result = prime * result + ((driver == null) ? 0 : driver.hashCode());
		result = prime * result + ((helpUrl == null) ? 0 : helpUrl.hashCode());
		result = prime * result
				+ ((hubRequired == null) ? 0 : hubRequired.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((keywords == null) ? 0 : keywords.hashCode());
		result = prime * result
				+ ((lastChanged == null) ? 0 : lastChanged.hashCode());
		result = prime * result
				+ ((manufacturer == null) ? 0 : manufacturer.hashCode());
		result = prime * result
				+ ((minAppVersion == null) ? 0 : minAppVersion.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((pair == null) ? 0 : pair.hashCode());
		result = prime * result
				+ ((pairVideoUrl == null) ? 0 : pairVideoUrl.hashCode());
		result = prime * result
				+ ((populations == null) ? 0 : populations.hashCode());
		result = prime * result
				+ ((protoFamily == null) ? 0 : protoFamily.hashCode());
		result = prime * result
				+ ((protoSpec == null) ? 0 : protoSpec.hashCode());
		result = prime * result + ((removal == null) ? 0 : removal.hashCode());
		result = prime * result + ((reset == null) ? 0 : reset.hashCode());
		result = prime * result + ((reconnect == null) ? 0 : reconnect.hashCode());
		result = prime * result + ((screen == null) ? 0 : screen.hashCode());
		result = prime * result
				+ ((shortName == null) ? 0 : shortName.hashCode());
		result = prime * result + ((vendor == null) ? 0 : vendor.hashCode());
		result = prime * result + ((minHubFirmware == null) ? 0 : minHubFirmware.hashCode());
		result = prime * result + ((canDiscover == null) ? 0 : canDiscover.hashCode());
		result = prime * result + ((appRequired == null) ? 0 : appRequired.hashCode());
		result = prime * result + ((installManualUrl == null) ? 0 : installManualUrl.hashCode());
		result = prime * result + ((pairingMode == null) ? 0 : pairingMode.hashCode());
		result = prime * result + ((pairingIdleTimeoutMs == null) ? 0 : pairingIdleTimeoutMs.hashCode());
		result = prime * result + ((pairingTimeoutMs == null) ? 0 : pairingTimeoutMs.hashCode());
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
		ProductCatalogEntry other = (ProductCatalogEntry) obj;
		if (OTA == null) {
			if (other.OTA != null)
				return false;
		} else if (!OTA.equals(other.OTA))
			return false;
		if (addDevImg == null) {
			if (other.addDevImg != null)
				return false;
		} else if (!addDevImg.equals(other.addDevImg))
			return false;
		if (added == null) {
			if (other.added != null)
				return false;
		} else if (!added.equals(other.added))
			return false;
		if (batteryBackNum == null) {
			if (other.batteryBackNum != null)
				return false;
		} else if (!batteryBackNum.equals(other.batteryBackNum))
			return false;
		if (batteryBackSize != other.batteryBackSize)
			return false;
		if (batteryPrimNum == null) {
			if (other.batteryPrimNum != null)
				return false;
		} else if (!batteryPrimNum.equals(other.batteryPrimNum))
			return false;
		if (batteryPrimSize != other.batteryPrimSize)
			return false;
		if (blacklisted == null) {
			if (other.blacklisted != null)
				return false;
		} else if (!blacklisted.equals(other.blacklisted))
			return false;
		if (canBrowse == null) {
			if (other.canBrowse != null)
				return false;
		} else if (!canBrowse.equals(other.canBrowse))
			return false;
		if (canSearch == null) {
			if (other.canSearch != null)
				return false;
		} else if (!canSearch.equals(other.canSearch))
			return false;
		if (categories == null) {
			if (other.categories != null)
				return false;
		} else if (!categories.equals(other.categories))
			return false;
		if (cert != other.cert)
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (devRequired == null) {
			if (other.devRequired != null)
				return false;
		} else if (!devRequired.equals(other.devRequired))
			return false;
		if (driver == null) {
			if (other.driver != null)
				return false;
		} else if (!driver.equals(other.driver))
			return false;
		if (helpUrl == null) {
			if (other.helpUrl != null)
				return false;
		} else if (!helpUrl.equals(other.helpUrl))
			return false;
		if (hubRequired == null) {
			if (other.hubRequired != null)
				return false;
		} else if (!hubRequired.equals(other.hubRequired))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (keywords == null) {
			if (other.keywords != null)
				return false;
		} else if (!keywords.equals(other.keywords))
			return false;
		if (lastChanged == null) {
			if (other.lastChanged != null)
				return false;
		} else if (!lastChanged.equals(other.lastChanged))
			return false;
		if (manufacturer == null) {
			if (other.manufacturer != null)
				return false;
		} else if (!manufacturer.equals(other.manufacturer))
			return false;
		if (minAppVersion == null) {
			if (other.minAppVersion != null)
				return false;
		} else if (!minAppVersion.equals(other.minAppVersion))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (pair == null) {
			if (other.pair != null)
				return false;
		} else if (!pair.equals(other.pair))
			return false;
		if (pairVideoUrl == null) {
			if (other.pairVideoUrl != null)
				return false;
		} else if (!pairVideoUrl.equals(other.pairVideoUrl))
			return false;
		if (populations == null) {
			if (other.populations != null)
				return false;
		} else if (!populations.equals(other.populations))
			return false;
		if (protoFamily == null) {
			if (other.protoFamily != null)
				return false;
		} else if (!protoFamily.equals(other.protoFamily))
			return false;
		if (protoSpec == null) {
			if (other.protoSpec != null)
				return false;
		} else if (!protoSpec.equals(other.protoSpec))
			return false;
		if (removal == null) {
			if (other.removal != null)
				return false;
		} else if (!removal.equals(other.removal))
			return false;
		if (reset == null) {
			if (other.reset != null)
				return false;
		} else if (!reset.equals(other.reset))
			return false;
		if (reconnect == null) {
			if (other.reconnect != null)
				return false;
		} else if (!reconnect.equals(other.reconnect))
			return false;
		if (screen == null) {
			if (other.screen != null)
				return false;
		} else if (!screen.equals(other.screen))
			return false;
		if (shortName == null) {
			if (other.shortName != null)
				return false;
		} else if (!shortName.equals(other.shortName))
			return false;
		if (vendor == null) {
			if (other.vendor != null)
				return false;
		} else if (!vendor.equals(other.vendor))
			return false;
		if (minHubFirmware == null) {
         if (other.minHubFirmware != null)
            return false;
      } else if (!minHubFirmware.equals(other.minHubFirmware))
         return false;
		if (canDiscover == null) {
         if (other.canDiscover != null)
            return false;
      } else if (!canDiscover.equals(other.canDiscover))
         return false;
	  if (appRequired == null) {
         if (other.appRequired != null)
            return false;
      } else if (!appRequired.equals(other.appRequired))
         return false;
	  if (installManualUrl == null) {
         if (other.installManualUrl != null)
            return false;
	  } else if (!installManualUrl.equals(other.installManualUrl))
		  return false;
		if (pairingMode == null) {
			if (other.pairingMode != null)
				return false;
		} else if (!pairingMode.equals(other.pairingMode))
			return false;
		if (pairingIdleTimeoutMs == null) {
			if (other.pairingIdleTimeoutMs != null)
				return false;
		} else if (!pairingIdleTimeoutMs.equals(other.pairingIdleTimeoutMs))
			return false;
		if (pairingTimeoutMs == null) {
			if (other.pairingTimeoutMs != null)
				return false;
		} else if (!pairingTimeoutMs.equals(other.pairingTimeoutMs))
			return false;
	  return true;
	}


	public enum Cert {
		WORKS("workswith"),
		COMPATIBLE("compatiblewith"),
		NONE("none");

		private String value;
		private Cert(String value) {
			this.value = value;
		}
		public String getValue() { return value; }
		public static Cert fromValue(String value) {
			if (value.equalsIgnoreCase(WORKS.value)) return WORKS;
			if (value.equalsIgnoreCase(COMPATIBLE.value)) return COMPATIBLE;
			return NONE;
		}

	};

   public enum PairingMode {
   	EXTERNAL_APP("external_app"),
		WIFI("wifi"),
		HUB("hub"),
		IPCD("ipcd"),
		OAUTH("oauth"),
		BRIDGED_DEVICE("bridged_device");

		private String value;

   	private PairingMode(String value) {
   		this.value = value;
		}

		public String getValue() {
			return value;
		}

		public static PairingMode fromValue(String value) {
   		for(PairingMode mode : PairingMode.values()) {
   			if(StringUtils.equalsIgnoreCase(mode.value, value)) {
   				return mode;
				}
			}
			return HUB;
		}
	}

	public enum BatterySize {
		_9V, AAAA, AAA, AA, C, D, CR123, CR2, CR2032, CR2430, CR2450, CR14250
	}

	@Override
	public int compareTo(ProductCatalogEntry o) {
	   if(o == null) {
	      return -1;
	   }

	   String myName = shortName == null ? name : shortName;

	   if(myName == null) {
	      return 1;
	   }

	   String theirName = o.shortName == null ? o.name : o.shortName;

	   return myName.compareTo(theirName);
	}

	public Boolean getBlacklisted() {
		return blacklisted;
	}

	public void setBlacklisted(Boolean blacklisted) {
		this.blacklisted = blacklisted;
	}

	public Integer getPairingIdleTimeoutMs() {
		return pairingIdleTimeoutMs;
	}

	public void setPairingIdleTimeoutMs(Integer pairingIdleTimeoutMs) {
		this.pairingIdleTimeoutMs = pairingIdleTimeoutMs;
	}

	public Integer getPairingTimeoutMs() {
		return pairingTimeoutMs;
	}

	public void setPairingTimeoutMs(Integer pairingTimeoutMs) {
		this.pairingTimeoutMs = pairingTimeoutMs;
	}

   
}

