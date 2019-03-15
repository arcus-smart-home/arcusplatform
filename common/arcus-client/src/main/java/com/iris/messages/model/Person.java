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

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.iris.messages.MessageConstants;
import com.iris.messages.model.support.TransientMutator;
import com.iris.messages.services.PlatformConstants;

public class Person extends BaseEntity<UUID, Person> {

	private UUID accountId;
	private String firstName;
	private String lastName;
	private Date termsAgreed;
	private Date privacyPolicyAgreed;
	private String email;
	private Date emailVerified;
	private String mobileNumber;
	private Date mobileVerified;
	private List<String> mobileNotificationEndpoints;
	private UUID currPlace;
	private String currPlaceMethod;
	private String currLocation;
	private Date currLocationTime;
	private String currLocationMethod;
	private Date consentOffersPromotions;
	private Date consentStatement;
	private Map<String,String> pinPerPlace = new HashMap<>();
	private Map<String,String> securityAnswers;
	private boolean hasLogin = false;
	private String emailVerificationToken = null;
	private boolean owner = false;

	@Override
   public String getType() {
	   return PlatformConstants.SERVICE_PEOPLE;
   }

   @Override
   public String getAddress() {
      return MessageConstants.SERVICE + ":" + PlatformConstants.SERVICE_PEOPLE + ":" + getId();
   }

   @Override
   public Set<String> getCaps() {
      return ImmutableSet.of("base", getType());
   }

   public UUID getAccountId() {
		return accountId;
	}
	public void setAccountId(UUID accountId) {
		this.accountId = accountId;
	}
	public String getFirstName() {
      return firstName;
   }
   public void setFirstName(String firstName) {
      this.firstName = firstName;
   }
   public String getLastName() {
      return lastName;
   }
   public void setLastName(String lastName) {
      this.lastName = lastName;
   }
   public String getFullName()
   {
      if (firstName == null)
      {
         return lastName;
      }
      else if (lastName == null)
      {
         return firstName;
      }
      else
      {
         return (firstName + " " + lastName).trim();
      }
   }
   public Date getTermsAgreed() {
      return termsAgreed;
   }
   public void setTermsAgreed(Date termsAgreed) {
      this.termsAgreed = termsAgreed;
   }
   public Date getPrivacyPolicyAgreed() {
      return privacyPolicyAgreed;
   }
   public void setPrivacyPolicyAgreed(Date privacyPolicyAgreed) {
      this.privacyPolicyAgreed = privacyPolicyAgreed;
   }
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public Date getEmailVerified() {
		return emailVerified;
	}
	public void setEmailVerified(Date emailVerified) {
		this.emailVerified = emailVerified;
	}
	public String getMobileNumber() {
		return mobileNumber;
	}
	public void setMobileNumber(String mobileNumber) {
		this.mobileNumber = mobileNumber;
	}
	public Date getMobileVerified() {
		return mobileVerified;
	}
	public void setMobileVerified(Date mobileVerified) {
		this.mobileVerified = mobileVerified;
	}
	public List<String> getMobileNotificationEndpoints() {
		return mobileNotificationEndpoints;
	}
	public void setMobileNotificationEndpoints(
			List<String> mobileNotificationEndpoints) {
		this.mobileNotificationEndpoints = mobileNotificationEndpoints;
	}
	public UUID getCurrPlace() {
		return currPlace;
	}
	public void setCurrPlace(UUID currPlace) {
		this.currPlace = currPlace;
	}
	public String getCurrPlaceMethod() {
		return currPlaceMethod;
	}
	public void setCurrPlaceMethod(String currPlaceMethod) {
		this.currPlaceMethod = currPlaceMethod;
	}
	public String getCurrLocation() {
		return currLocation;
	}
	public void setCurrLocation(String currLocation) {
		this.currLocation = currLocation;
	}
	public Date getCurrLocationTime() {
		return currLocationTime;
	}
	public void setCurrLocationTime(Date currLocationTime) {
		this.currLocationTime = currLocationTime;
	}
	public String getCurrLocationMethod() {
		return currLocationMethod;
	}
	public void setCurrLocationMethod(String currLocationMethod) {
		this.currLocationMethod = currLocationMethod;
	}

	/**
	 * @deprecated Replaced with getPinAtPlace(UUID) or getPinAtPlace(String)
	 */
	@Deprecated
	public String getPin() {
	   return getPinAtPlace(currPlace);
	}

	public String getPinAtPlace(UUID placeId) {
	   if(placeId == null) {
	      return null;
	   }
	   return getPinAtPlace(placeId.toString());
	}

	public String getPinAtPlace(String placeId) {
	   return pinPerPlace.get(placeId);
	}

	/**
	 * @deprecated Replaced with setPinAtPlace(UUID, String) or setPinAtPlace(String, String)
	 */
	@Deprecated
   @TransientMutator(persistedBy = "PersonDAO.updatePinAtPlace()")
	public void setPin(String pin) {
	   setPinAtPlace(currPlace, pin);
	}

   @TransientMutator(persistedBy = "PersonDAO.updatePinAtPlace()")
	public void setPinAtPlace(UUID placeId, String pin) {
	   Preconditions.checkNotNull(placeId, "placeId is required");
	   setPinAtPlace(placeId.toString(), pin);
	}

   @TransientMutator(persistedBy = "PersonDAO.updatePinAtPlace()")
	public void setPinAtPlace(String placeId, String pin) {
	   Preconditions.checkNotNull(placeId, "placeId is required");
      pinPerPlace.put(placeId, pin);
	}

	/**
	 * @deprecated Replaced with hasPin(UUID) or hasPin(String)
	 */
	@Deprecated
	public boolean getHasPin() {
	   return getPin() != null;
	}

	public boolean hasPinAtPlace(UUID placeId) {
	   Preconditions.checkNotNull(placeId);
	   return hasPinAtPlace(placeId.toString());
	}

	public boolean hasPinAtPlace(String placeId) {
	   return getPinAtPlace(placeId) != null;
	}

   public Map<String, String> getPinPerPlace() {
      return pinPerPlace;
   }

   @TransientMutator(persistedBy = "PersonDAO.updatePinAtPlace()")
   public void setPinPerPlace(Map<String, String> pinPerPlace) {
      this.pinPerPlace.clear();
      if(pinPerPlace != null) {
         this.pinPerPlace.putAll(pinPerPlace);
      }
   }

   @TransientMutator(persistedBy = "PersonDAO.deletePinAtPlace()")
   public void clearPin(String placeId) {
      Preconditions.checkNotNull(placeId, "placeId is required");
      this.pinPerPlace.remove(placeId);
   }

   @TransientMutator(persistedBy = "PersonDAO.deletePinAtPlace()")
   public void clearPin(UUID placeId) {
      Preconditions.checkNotNull(placeId, "placeId is required");
      clearPin(placeId.toString());
   }

   public Set<String> getPlacesWithPin() {
      return new HashSet<>(this.pinPerPlace.keySet());
   }

   public Map<String, String> getSecurityAnswers() {
      return securityAnswers;
   }
   public void setSecurityAnswers(Map<String, String> securityAnswers) {
      this.securityAnswers = securityAnswers;
   }
   public Date getConsentOffersPromotions() {
		return consentOffersPromotions;
	}
	public void setConsentOffersPromotions(Date consentOffersPromotions) {
		this.consentOffersPromotions = consentOffersPromotions;
	}
	public Date getConsentStatement() {
		return consentStatement;
	}
	public void setConsentStatement(Date consentStatement) {
		this.consentStatement = consentStatement;
	}

   public boolean getHasLogin() {
      return hasLogin;
   }

   public void setHasLogin(boolean hasLogin) {
      this.hasLogin = hasLogin;
   }
   
   public int getSecurityAnswerCount() {
   	if(this.securityAnswers != null) {
   		return this.securityAnswers.size();
   	}else{
   		return 0;
   	}
   }
   
   public String getEmailVerificationToken() {
		return emailVerificationToken;
	}

	public void setEmailVerificationToken(String emailVerificationToken) {
		this.emailVerificationToken = emailVerificationToken;
	}

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result
            + ((accountId == null) ? 0 : accountId.hashCode());
      result = prime
            * result
            + ((consentOffersPromotions == null) ? 0 : consentOffersPromotions
                  .hashCode());
      result = prime * result
            + ((consentStatement == null) ? 0 : consentStatement.hashCode());
      result = prime * result
            + ((currLocation == null) ? 0 : currLocation.hashCode());
      result = prime
            * result
            + ((currLocationMethod == null) ? 0 : currLocationMethod.hashCode());
      result = prime * result
            + ((currLocationTime == null) ? 0 : currLocationTime.hashCode());
      result = prime * result
            + ((currPlace == null) ? 0 : currPlace.hashCode());
      result = prime * result
            + ((currPlaceMethod == null) ? 0 : currPlaceMethod.hashCode());
      result = prime * result + ((email == null) ? 0 : email.hashCode());
      result = prime * result
            + ((emailVerified == null) ? 0 : emailVerified.hashCode());
      result = prime * result
            + ((firstName == null) ? 0 : firstName.hashCode());
      result = prime * result + (hasLogin ? 1231 : 1237);
      result = prime * result + ((lastName == null) ? 0 : lastName.hashCode());
      result = prime
            * result
            + ((mobileNotificationEndpoints == null) ? 0
                  : mobileNotificationEndpoints.hashCode());
      result = prime * result
            + ((mobileNumber == null) ? 0 : mobileNumber.hashCode());
      result = prime * result
            + ((mobileVerified == null) ? 0 : mobileVerified.hashCode());
      result = prime * result
            + ((pinPerPlace == null) ? 0 : pinPerPlace.hashCode());
      result = prime
            * result
            + ((privacyPolicyAgreed == null) ? 0 : privacyPolicyAgreed
                  .hashCode());
      result = prime * result
            + ((securityAnswers == null) ? 0 : securityAnswers.hashCode());
      result = prime * result
            + ((termsAgreed == null) ? 0 : termsAgreed.hashCode());
      result = prime * result
         + ((emailVerificationToken == null) ? 0 : emailVerificationToken.hashCode());
      result = prime * result + (owner ? 1231 : 1237);      
      
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
      Person other = (Person) obj;
      if (accountId == null) {
         if (other.accountId != null)
            return false;
      } else if (!accountId.equals(other.accountId))
         return false;
      if (consentOffersPromotions == null) {
         if (other.consentOffersPromotions != null)
            return false;
      } else if (!consentOffersPromotions.equals(other.consentOffersPromotions))
         return false;
      if (consentStatement == null) {
         if (other.consentStatement != null)
            return false;
      } else if (!consentStatement.equals(other.consentStatement))
         return false;
      if (currLocation == null) {
         if (other.currLocation != null)
            return false;
      } else if (!currLocation.equals(other.currLocation))
         return false;
      if (currLocationMethod == null) {
         if (other.currLocationMethod != null)
            return false;
      } else if (!currLocationMethod.equals(other.currLocationMethod))
         return false;
      if (currLocationTime == null) {
         if (other.currLocationTime != null)
            return false;
      } else if (!currLocationTime.equals(other.currLocationTime))
         return false;
      if (currPlace == null) {
         if (other.currPlace != null)
            return false;
      } else if (!currPlace.equals(other.currPlace))
         return false;
      if (currPlaceMethod == null) {
         if (other.currPlaceMethod != null)
            return false;
      } else if (!currPlaceMethod.equals(other.currPlaceMethod))
         return false;
      if (email == null) {
         if (other.email != null)
            return false;
      } else if (!email.equals(other.email))
         return false;
      if (emailVerified == null) {
         if (other.emailVerified != null)
            return false;
      } else if (!emailVerified.equals(other.emailVerified))
         return false;
      if (firstName == null) {
         if (other.firstName != null)
            return false;
      } else if (!firstName.equals(other.firstName))
         return false;
      if (hasLogin != other.hasLogin)
         return false;
      if (lastName == null) {
         if (other.lastName != null)
            return false;
      } else if (!lastName.equals(other.lastName))
         return false;
      if (mobileNotificationEndpoints == null) {
         if (other.mobileNotificationEndpoints != null)
            return false;
      } else if (!mobileNotificationEndpoints
            .equals(other.mobileNotificationEndpoints))
         return false;
      if (mobileNumber == null) {
         if (other.mobileNumber != null)
            return false;
      } else if (!mobileNumber.equals(other.mobileNumber))
         return false;
      if (mobileVerified == null) {
         if (other.mobileVerified != null)
            return false;
      } else if (!mobileVerified.equals(other.mobileVerified))
         return false;
      if (pinPerPlace == null) {
         if (other.pinPerPlace != null)
            return false;
      } else if (!pinPerPlace.equals(other.pinPerPlace))
         return false;
      if (privacyPolicyAgreed == null) {
         if (other.privacyPolicyAgreed != null)
            return false;
      } else if (!privacyPolicyAgreed.equals(other.privacyPolicyAgreed))
         return false;
      if (securityAnswers == null) {
         if (other.securityAnswers != null)
            return false;
      } else if (!securityAnswers.equals(other.securityAnswers))
         return false;
      if (termsAgreed == null) {
         if (other.termsAgreed != null)
            return false;
      } else if (!termsAgreed.equals(other.termsAgreed))
         return false;
      if (emailVerificationToken == null) {
         if (other.emailVerificationToken != null)
            return false;
      } else if (!emailVerificationToken.equals(other.emailVerificationToken))
         return false;     
      if(owner != other.owner)
      	return false;
      
      return true;
   }

	@Override
	public String toString() {
		return "Person [accountId=" + accountId + ", firstName=" + firstName
				+ ", lastName=" + lastName + ", termsAgreed=" + termsAgreed
				+ ", privacyPolicyAgreed=" + privacyPolicyAgreed + ", email="
				+ email + ", emailVerified=" + emailVerified + ", mobileNumber="
				+ mobileNumber + ", mobileVerified=" + mobileVerified
				+ ", mobileNotificationEndpoints=" + mobileNotificationEndpoints
				+ ", currPlace=" + currPlace + ", currPlaceMethod="
				+ currPlaceMethod + ", currLocation=" + currLocation
				+ ", currLocationTime=" + currLocationTime
				+ ", currLocationMethod=" + currLocationMethod
				+ ", consentOffersPromotions=" + consentOffersPromotions
				+ ", consentStatement=" + consentStatement + ", pinPerPlace=" + pinPerPlace
				+ ", securityAnswers=" + securityAnswers + ", hasLogin=" + hasLogin 
				+ ", owner=" + owner 
				+ ", emailVerificationToken=" + emailVerificationToken + "]";
	}

	@Override
   protected Object clone() throws CloneNotSupportedException {
      Person person = (Person) super.clone();
      person.currLocationTime = this.currLocationTime == null ? null : (Date) this.currLocationTime.clone();
      person.emailVerified = this.emailVerified == null ? null : (Date) this.emailVerified.clone();
      person.termsAgreed = this.termsAgreed == null ? null : (Date) this.termsAgreed.clone();
      person.privacyPolicyAgreed = this.privacyPolicyAgreed == null ? null : (Date) this.privacyPolicyAgreed.clone();
      person.consentOffersPromotions = this.consentOffersPromotions == null ? null : (Date) this.consentOffersPromotions.clone();
      person.consentStatement = this.consentStatement == null ? null : (Date) this.consentStatement.clone();
      person.mobileNotificationEndpoints = this.mobileNotificationEndpoints == null ? null : new LinkedList<String>(this.mobileNotificationEndpoints);
      person.mobileVerified = this.mobileVerified == null ? null : (Date) this.mobileVerified.clone();
      person.securityAnswers = this.securityAnswers == null ? null : new HashMap<>(this.securityAnswers);
      person.pinPerPlace = this.pinPerPlace == null ? null : new HashMap<>(this.pinPerPlace);
      return person;
   }

	public boolean getOwner() {
		return owner;
	}

	public void setOwner(boolean owner) {
		this.owner = owner;
	}	

	
}

