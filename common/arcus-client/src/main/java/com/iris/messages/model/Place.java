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
package com.iris.messages.model;

import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import com.google.common.base.Optional;
import java.util.TimeZone;

import com.google.common.collect.ImmutableSet;
import com.iris.messages.MessageConstants;
import com.iris.messages.services.PlatformConstants;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Place extends BaseEntity<UUID, Place> {
   private static final Logger LOGGER = LoggerFactory.getLogger(Place.class);

    public static final String GEOPRECISION_NONE = "NONE";
    public static final String GEOPRECISION_UNKNOWN = "UNKNOWN";
    public static final String GEOPRECISION_ZIP5 = "ZIP5";
    public static final String GEOPRECISION_ZIP6 = "ZIP6";
    public static final String GEOPRECISION_ZIP7 = "ZIP7";
    public static final String GEOPRECISION_ZIP8 = "ZIP8";
    public static final String GEOPRECISION_ZIP9 = "ZIP9";
    

    private UUID accountId;
    private UUID populationId;
    private String name;
    private String state;
    private String streetAddress1;
    private String streetAddress2;
    private String city;
    private String stateProv;
    private String zipCode;
    private String zipPlus4;
    private String tzId;
    private String tzName;
    private Double tzOffset;                     // Hour offset from UTC
    private Boolean tzUsesDST;                  // True if locale follows daylight savings time; false otherwise
    private String country;
    private Boolean addrValidated;
    private String addrType;                    // [f=firm, g=general, h=high rise, p=p.o. box, r=rural route, s=street, null/empty=invalid address]
    private String addrZipType;                 // Zip code type [unique, military, pobox, standard]
    private Double addrLatitude;              // latitude of the address
    private Double addrLongitude;              // longitude of the address
    private String addrGeoPrecision;           // Precision of latitude,longitude [unknown, none, zip5, zip6, zip7, zip8, zip9]
    private String addrRDI;                     // Residential delivery indicator [Residential, Commercial, Unknown]
    private String addrCounty;                  // County in which address is located
    private String addrCountyFIPS;              // 5 digit FIPS code with 3 digit country code
    private ServiceLevel serviceLevel;
    private Date lastServiceLevelChange;
    private Set<String> serviceAddons;
    private boolean primary = false;
    private String population;

    @Override
    public String getType() {
        return PlatformConstants.SERVICE_PLACES;
    }

    @Override
    public String getAddress() {
        return MessageConstants.SERVICE + ":" + PlatformConstants.SERVICE_PLACES + ":" + getId();
    }

    @Override
    public Set<String> getCaps() {
        return ImmutableSet.of("base", getType());
    }

    /** @deprecated Replaced with getAccount to match the capability */
    @Deprecated
    public UUID getAccountId() {
        return accountId;
    }
    public UUID getAccount() { return getAccountId(); }
    /** @deprecated Replaced with setAccount to match the capability */
    @Deprecated
    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }
    public void setAccount(UUID accountId) { setAccountId(accountId); }

    public String getPopulation() {
        return population;
    }

    public void setPopulation(String population) {
        this.population = population;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getState() {
    	return state;
    }
    public void setState(String state) {
    	this.state = state;
    }

    public String getStreetAddress1() {
        return streetAddress1;
    }

    public void setStreetAddress1(String streetAddress1) {
        this.streetAddress1 = streetAddress1;
    }

    public String getStreetAddress2() {
        return streetAddress2;
    }

    public void setStreetAddress2(String streetAddress2) {
        this.streetAddress2 = streetAddress2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStateProv() {
        return stateProv;
    }

    public void setStateProv(String stateProv) {
        this.stateProv = stateProv;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

   @Deprecated
   public UUID getPopulationId() {
		return populationId;
	}

   @Deprecated
	public void setPopulationId(UUID populationId) {
		this.populationId = populationId;
	}
	
	public String getTzId() {
	   return tzId;
	}
	
	public void setTzId(String id) {
	   this.tzId = id;
	}

	public String getTzName() {
		return tzName;
	}

	public void setTzName(String tzName) {
		this.tzName = tzName;
	}

	public Double getTzOffset() {
		return tzOffset;
	}

	public void setTzOffset(Double tzOffset) {
		this.tzOffset = tzOffset;
	}

	public Boolean getTzUsesDST() {
		return tzUsesDST;
	}

	public void setTzUsesDST(Boolean tzUsesDST) {
		this.tzUsesDST = tzUsesDST;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public Boolean getAddrValidated() {
		return addrValidated;
	}

	public void setAddrValidated(Boolean addrValidated) {
		this.addrValidated = addrValidated;
	}

	public String getAddrType() {
		return addrType;
	}

	public void setAddrType(String addrType) {
		this.addrType = addrType;
	}

	public String getAddrZipType() {
		return addrZipType;
	}

	public void setAddrZipType(String addrZipType) {
		this.addrZipType = addrZipType;
	}

	public Double getAddrLatitude() {
		return addrLatitude;
	}

	public void setAddrLatitude(Double addrLatitude) {
		this.addrLatitude = addrLatitude;
	}

	public Double getAddrLongitude() {
		return addrLongitude;
	}

	public void setAddrLongitude(Double addrLongitude) {
		this.addrLongitude = addrLongitude;
	}

	public String getAddrGeoPrecision() {
		return addrGeoPrecision;
	}

	public void setAddrGeoPrecision(String addrGeoPrecision) {
		this.addrGeoPrecision = addrGeoPrecision;
	}

	public String getAddrRDI() {
		return addrRDI;
	}

	public void setAddrRDI(String addrRDI) {
		this.addrRDI = addrRDI;
	}

	public String getAddrCounty() {
		return addrCounty;
	}

	public void setAddrCounty(String addrCounty) {
		this.addrCounty = addrCounty;
	}

	public String getAddrCountyFIPS() {
		return addrCountyFIPS;
	}

	public void setAddrCountyFIPS(String addrCountyFIPS) {
		this.addrCountyFIPS = addrCountyFIPS;
	}

    public String getZipPlus4() {
        return zipPlus4;
    }

	public void setZipPlus4(String zipPlus4) {
		this.zipPlus4 = zipPlus4;
	}
	
	public Date getLastServiceLevelChange() {
		return lastServiceLevelChange;
	}
	
	public void setLastServiceLevelChange(Date lastServiceLevelChange) {
		this.lastServiceLevelChange = lastServiceLevelChange;
	}

	public ServiceLevel getServiceLevel() {
        return serviceLevel;
    }

    public void setServiceLevel(ServiceLevel serviceLevel) {
        this.serviceLevel = serviceLevel;
    }

    public Set<String> getServiceAddons() {
        if (serviceAddons == null) {
            return Collections.<String>emptySet();
        }
        return serviceAddons;
    }

    public void setServiceAddons(Set<String> serviceAddons) {
        this.serviceAddons = serviceAddons;
    }

	public boolean isPrimary() {
      return primary;
   }

   public void setPrimary(boolean primary) {
      this.primary = primary;
   }

   @Override
    protected Object clone() throws CloneNotSupportedException {
        Place place = (Place) super.clone();
        return place;
    }

   @Override
   public String toString() {
      return "Place [accountId=" + accountId + ", population=" + population 
            + ", name=" + name + ", state=" + state + ", streetAddress1="
            + streetAddress1 + ", streetAddress2=" + streetAddress2 + ", city="
            + city + ", stateProv=" + stateProv + ", zipCode=" + zipCode
            + ", zipPlus4=" + zipPlus4 + ", tzName=" + tzName + ", tzOffset="
            + tzOffset + ", tzUsesDST=" + tzUsesDST + ", country=" + country
            + ", addrValidated=" + addrValidated + ", addrType=" + addrType
            + ", addrZipType=" + addrZipType + ", addrLatitude=" + addrLatitude
            + ", addrLongitude=" + addrLongitude + ", addrGeoPrecision="
            + addrGeoPrecision + ", addrRDI=" + addrRDI + ", addrCounty="
            + addrCounty + ", addrCountyFIPS=" + addrCountyFIPS
            + ", serviceLevel=" + serviceLevel + ", serviceAddons="
            + serviceAddons + ", primary=" + primary + "]";
   }
   
   
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((accountId == null) ? 0 : accountId.hashCode());
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((state == null) ? 0 : state.hashCode());
      result = prime * result + ((streetAddress1 == null) ? 0 : streetAddress1.hashCode());
      result = prime * result + ((streetAddress2 == null) ? 0 : streetAddress2.hashCode());
      result = prime * result + ((city == null) ? 0 : city.hashCode());
      result = prime * result + ((stateProv == null) ? 0 : stateProv.hashCode());
      result = prime * result + ((zipCode == null) ? 0 : zipCode.hashCode());
      result = prime * result + ((zipPlus4 == null) ? 0 : zipPlus4.hashCode());
      result = prime * result + ((tzId == null) ? 0 : tzId.hashCode());
      result = prime * result + ((tzName == null) ? 0 : tzName.hashCode());
      result = prime * result + ((tzOffset == null) ? 0 : tzOffset.hashCode());
      result = prime * result + ((tzUsesDST == null) ? 0 : tzUsesDST.hashCode());
      result = prime * result + ((country == null) ? 0 : country.hashCode());
      result = prime * result + ((addrValidated == null) ? 0 : addrValidated.hashCode());
      result = prime * result + ((addrType == null) ? 0 : addrType.hashCode());
      result = prime * result + ((addrZipType == null) ? 0 : addrZipType.hashCode());
      result = prime * result + ((addrLatitude == null) ? 0 : addrLatitude.hashCode());
      result = prime * result + ((addrLongitude == null) ? 0 : addrLongitude.hashCode());
      result = prime * result + ((addrGeoPrecision == null) ? 0 : addrGeoPrecision.hashCode());
      result = prime * result + ((addrRDI == null) ? 0 : addrRDI.hashCode());
      result = prime * result + ((addrCounty == null) ? 0 : addrCounty.hashCode());
      result = prime * result + ((addrCountyFIPS == null) ? 0 : addrCountyFIPS.hashCode());
      result = prime * result + ((serviceLevel == null) ? 0 : serviceLevel.hashCode());
      result = prime * result + ((lastServiceLevelChange == null) ? 0 : lastServiceLevelChange.hashCode());
      result = prime * result + ((serviceAddons == null) ? 0 : serviceAddons.hashCode());
      result = prime * result + ((population == null) ? 0 : population.hashCode());
      result = prime * result + (primary ? 1231 : 1237);
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
      Place other = (Place) obj;
      if (primary != other.primary)
         return false;
      if (accountId == null) {
         if (other.accountId != null)
            return false;
      } else if (!accountId.equals(other.accountId))
         return false;
      if (name == null) {
         if (other.name != null)
            return false;
      } else if (!name.equals(other.name))
         return false;
      if (state == null) {
         if (other.state != null)
            return false;
      } else if (!state.equals(other.state))
         return false;
      if (streetAddress1 == null) {
         if (other.streetAddress1 != null)
            return false;
      } else if (!streetAddress1.equals(other.streetAddress1))
         return false;
      if (streetAddress2 == null) {
         if (other.streetAddress2 != null)
            return false;
      } else if (!streetAddress2.equals(other.streetAddress2))
         return false;
      if (city == null) {
         if (other.city != null)
            return false;
      } else if (!city.equals(other.city))
         return false;
      if (stateProv == null) {
         if (other.stateProv != null)
            return false;
      } else if (!stateProv.equals(other.stateProv))
         return false;
      if (zipCode == null) {
         if (other.zipCode != null)
            return false;
      } else if (!zipCode.equals(other.zipCode))
         return false;
      if (zipPlus4 == null) {
         if (other.zipPlus4 != null)
            return false;
      } else if (!zipPlus4.equals(other.zipPlus4))
         return false;
      if (tzId == null) {
         if (other.tzId != null)
            return false;
      } else if (!tzId.equals(other.tzId))
         return false;
      if (tzName == null) {
         if (other.tzName != null)
            return false;
      } else if (!tzName.equals(other.tzName))
         return false;
      if (tzOffset == null) {
         if (other.tzOffset != null)
            return false;
      } else if (!tzOffset.equals(other.tzOffset))
         return false;
      if (state == null) {
         if (other.state != null)
            return false;
      } else if (!state.equals(other.state))
         return false;
      if (tzUsesDST == null) {
         if (other.tzUsesDST != null)
            return false;
      } else if (!tzUsesDST.equals(other.tzUsesDST))
         return false;
      if (country == null) {
         if (other.country != null)
            return false;
      } else if (!country.equals(other.country))
         return false;
      if (addrValidated == null) {
         if (other.addrValidated != null)
            return false;
      } else if (!addrValidated.equals(other.addrValidated))
         return false;
      if (addrType == null) {
         if (other.addrType != null)
            return false;
      } else if (!addrType.equals(other.addrType))
         return false;
      if (addrZipType == null) {
         if (other.addrZipType != null)
            return false;
      } else if (!addrZipType.equals(other.addrZipType))
         return false;     
      if (addrLatitude == null) {
         if (other.addrLatitude != null)
            return false;
      } else if (!addrLatitude.equals(other.addrLatitude))
         return false;
      if (addrLongitude == null) {
         if (other.addrLongitude != null)
            return false;
      } else if (!addrLongitude.equals(other.addrLongitude))
         return false;
      if (addrGeoPrecision == null) {
         if (other.addrGeoPrecision != null)
            return false;
      } else if (!addrGeoPrecision.equals(other.addrGeoPrecision))
         return false;
      if (addrRDI == null) {
         if (other.addrRDI != null)
            return false;
      } else if (!addrRDI.equals(other.addrRDI))
         return false;
      if (addrCounty == null) {
         if (other.addrCounty != null)
            return false;
      } else if (!addrCounty.equals(other.addrCounty))
         return false;
      if (addrCounty == null) {
         if (other.addrCounty != null)
            return false;
      } else if (!addrCounty.equals(other.addrCounty))
         return false;
      if (addrCountyFIPS == null) {
         if (other.addrCountyFIPS != null)
            return false;
      } else if (!addrCountyFIPS.equals(other.addrCountyFIPS))
         return false;
      if (serviceLevel == null) {
         if (other.serviceLevel != null)
            return false;
      } else if (!serviceLevel.equals(other.serviceLevel))
         return false;
      if (lastServiceLevelChange == null) {
         if (other.lastServiceLevelChange != null)
            return false;
      } else if (!lastServiceLevelChange.equals(other.lastServiceLevelChange))
         return false;
      if (serviceAddons == null) {
         if (other.serviceAddons != null)
            return false;
      } else if (!serviceAddons.equals(other.serviceAddons))
         return false;
      if (population == null) {
         if (other.population != null)
            return false;
      } else if (!population.equals(other.population))
         return false;
      return true;
   }

   public Optional<TimeZone> getTimeZone() {
       String timeZoneId = getTzId();
       if (!StringUtils.isEmpty(timeZoneId)) {
          try {
             Optional<TimeZone> retval = Optional.of(TimeZone.getTimeZone(timeZoneId));
             return retval;
          }
          catch (Exception e) {
             LOGGER.debug("Place [{}] had an invalid time zone [{}]", getId(), timeZoneId);
             LOGGER.debug("Place had exception getting time zone", e);
          }
       }
       return Optional.absent();

   }
}

