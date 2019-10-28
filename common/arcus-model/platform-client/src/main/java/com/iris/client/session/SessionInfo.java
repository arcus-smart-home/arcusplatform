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
package com.iris.client.session;

import java.util.*;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.iris.client.ClientEvent;

public class SessionInfo {
	private final String billingPublicKey;
	private final String tokenURL;
	private final String sessionToken;
	private final String username;
	private final String personId;
	private final List<PlaceDescriptor> places;
	private final String smartyAuthToken;
	private final String smartyAuthID;
	private final String previewURLBase;
	private final String staticResourceBaseUrl;
	private final String secureStaticResourceBaseUrl;
	private final String honeywellRedirectUri;
	private final String honeywellLoginBaseUrl;
	private final String honeywellClientId;
	private final String lutronLoginBaseUrl;
	private final String webLaunchUrl;
	private final String androidLaunchUrl;
	
	private Boolean requiresTermsAndConditionsConsent;
	private Boolean requiresPrivacyPolicyConsent;
	private final String promonAdUrl;
	private final String redirectBaseUrl;   

   public SessionInfo(
			String sessionToken,
			String username, String personId,
			List<PlaceDescriptor> places,
			String billingPublicKey,
			String tokenURL) {
		Preconditions.checkNotNull(sessionToken, "Token cannot be null.");
		Preconditions.checkNotNull(username, "Username cannot be null.");

		this.sessionToken = sessionToken;
		this.username = username;
		this.personId = Strings.nullToEmpty(personId);
		this.places = new ArrayList<PlaceDescriptor>();
		if (places != null && places.size() > 0) {
			this.places.addAll(places);
		}
		this.billingPublicKey = billingPublicKey;
		this.tokenURL = tokenURL;
		this.smartyAuthToken = null;
		this.smartyAuthID = null;
		this.previewURLBase = null;
		this.staticResourceBaseUrl = null;
		this.honeywellLoginBaseUrl = null;
		this.honeywellRedirectUri = null;
		this.honeywellClientId = null;
		this.lutronLoginBaseUrl = null;
		this.promonAdUrl = null;
		this.redirectBaseUrl = null;
		this.webLaunchUrl = null;
		this.androidLaunchUrl = null;
		this.secureStaticResourceBaseUrl = null;
	}

	@SuppressWarnings("unchecked")
	public SessionInfo(String username, String tokenID, ClientEvent event) {
		if (event.getAttributes() == null || event.getAttributes().isEmpty()) {
			throw new RuntimeException("Session Object cannot be null or empty.");
		}

		sessionToken = tokenID;
		this.username = username;
		personId = (String) event.getAttribute("personId");

		List<Map<String, Object>> placesObjMap = (List<Map<String, Object>>) event.getAttribute("places");
		places = new ArrayList<PlaceDescriptor>();
	    if(placesObjMap != null) {
	       for(Map<String, Object> placeDescriptor : placesObjMap) {
	          places.add(new PlaceDescriptor(placeDescriptor));
	       }
	    }

		billingPublicKey = (String) event.getAttribute("publicKey");
		tokenURL = (String) event.getAttribute("tokenURL");
		smartyAuthToken = (String) event.getAttribute("smartyAuthToken");
		smartyAuthID = (String) event.getAttribute("smartyAuthID");
		previewURLBase = (String) event.getAttribute("cameraPreviewBaseUrl");
		staticResourceBaseUrl = (String) event.getAttribute("staticResourceBaseUrl");
		secureStaticResourceBaseUrl = (String) event.getAttribute("secureStaticResourceBaseUrl");
		this.honeywellLoginBaseUrl = (String) event.getAttribute("honeywellLoginBaseUrl");
		this.honeywellRedirectUri = (String) event.getAttribute("honeywellRedirectUri");
		this.honeywellClientId = (String) event.getAttribute("honeywellClientId");
		this.lutronLoginBaseUrl = (String) event.getAttribute("lutronLoginBaseUrl");
		this.requiresPrivacyPolicyConsent = (Boolean)event.getAttribute("requiresPrivacyPolicyConsent");
		this.requiresTermsAndConditionsConsent = (Boolean)event.getAttribute("requiresTermsAndConditionsConsent");
		this.promonAdUrl = (String)event.getAttribute("promonAdUrl");
		this.redirectBaseUrl = (String)event.getAttribute("redirectBaseUrl");
		this.webLaunchUrl = (String)event.getAttribute("webLaunchUrl");
		this.androidLaunchUrl = (String)event.getAttribute("androidLaunchUrl");
	}

	public String getSmartyAuthToken() {
		return smartyAuthToken;
	}

	public String getSmartyAuthID() {
		return smartyAuthID;
	}

	public String getTokenURL() {
		return tokenURL;
	}

	public String getBillingPublicKey() {
		return billingPublicKey;
	}

	public String getSessionToken() {
		return sessionToken;
	}

	public String getUsername() {
		return username;
	}

	public String getPersonId() {
		return personId;
	}

	public List<PlaceDescriptor> getPlaces() {
		if (places == null || places.isEmpty()) {
			return Collections.<PlaceDescriptor>emptyList();
		}

		return Collections.unmodifiableList(places);
	}

	public String getPreviewBaseUrl() {
		return previewURLBase;
	}

	public String getStaticResourceBaseUrl() {
	   return staticResourceBaseUrl;
	}

	public final String getHoneywellRedirectUri() {
		return honeywellRedirectUri;
	}

	public final String getHoneywellLoginBaseUrl() {
		return honeywellLoginBaseUrl;
	}

	public final String getHoneywellClientId() {
		return honeywellClientId;
	}
	
	public final String getLutronLoginBaseUrl() {
		return lutronLoginBaseUrl;
	}

	public Boolean getRequiresTermsAndConditionsConsent() {
		return requiresTermsAndConditionsConsent;
	}


	public Boolean getRequiresPrivacyPolicyConsent() {
		return requiresPrivacyPolicyConsent;
	}
	
	public String getPromonAdUrl()
   {
      return promonAdUrl;
   }
	
	public String getRedirectBaseUrl()
   {
      return redirectBaseUrl;
   }

	public String getWebLaunchUrl() {
   		return this.webLaunchUrl;
	}

	public String getAndroidLaunchUrl() {
   		return this.androidLaunchUrl;
	}
	
	public String getSecureStaticResourceBaseUrl() {
		return secureStaticResourceBaseUrl;
	}

	@Override
	public String toString() {
		return "SessionInfo{" +
				"billingPublicKey='" + billingPublicKey + '\'' +
				", tokenURL='" + tokenURL + '\'' +
				", sessionToken='" + sessionToken + '\'' +
				", username='" + username + '\'' +
				", personId='" + personId + '\'' +
				", places=" + places +
				", smartyAuthToken='" + smartyAuthToken + '\'' +
				", smartyAuthID='" + smartyAuthID + '\'' +
				", previewURLBase='" + previewURLBase + '\'' +
				", staticResourceBaseUrl='" + staticResourceBaseUrl + '\'' +
				", secureStaticResourceBaseUrl='" + secureStaticResourceBaseUrl + '\'' +
				", honeywellRedirectUri='" + honeywellRedirectUri + '\'' +
				", honeywellLoginBaseUrl='" + honeywellLoginBaseUrl + '\'' +
				", honeywellClientId='" + honeywellClientId + '\'' +
				", lutronLoginBaseUrl='" + lutronLoginBaseUrl + '\'' +
				", webLaunchUrl='" + webLaunchUrl + '\'' +
				", androidLaunchUrl='" + androidLaunchUrl + '\'' +
				", requiresTermsAndConditionsConsent=" + requiresTermsAndConditionsConsent +
				", requiresPrivacyPolicyConsent=" + requiresPrivacyPolicyConsent +
				", promonAdUrl='" + promonAdUrl + '\'' +
				", redirectBaseUrl='" + redirectBaseUrl + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		SessionInfo that = (SessionInfo) o;

		if (billingPublicKey != null ? !billingPublicKey.equals(that.billingPublicKey) : that.billingPublicKey != null)
			return false;
		if (tokenURL != null ? !tokenURL.equals(that.tokenURL) : that.tokenURL != null) return false;
		if (sessionToken != null ? !sessionToken.equals(that.sessionToken) : that.sessionToken != null) return false;
		if (username != null ? !username.equals(that.username) : that.username != null) return false;
		if (personId != null ? !personId.equals(that.personId) : that.personId != null) return false;
		if (places != null ? !places.equals(that.places) : that.places != null) return false;
		if (smartyAuthToken != null ? !smartyAuthToken.equals(that.smartyAuthToken) : that.smartyAuthToken != null)
			return false;
		if (smartyAuthID != null ? !smartyAuthID.equals(that.smartyAuthID) : that.smartyAuthID != null) return false;
		if (previewURLBase != null ? !previewURLBase.equals(that.previewURLBase) : that.previewURLBase != null)
			return false;
		if (staticResourceBaseUrl != null ? !staticResourceBaseUrl.equals(that.staticResourceBaseUrl) : that.staticResourceBaseUrl != null)
			return false;
		if (secureStaticResourceBaseUrl != null ? !secureStaticResourceBaseUrl.equals(that.secureStaticResourceBaseUrl) : that.secureStaticResourceBaseUrl != null)
			return false;		
		if (honeywellRedirectUri != null ? !honeywellRedirectUri.equals(that.honeywellRedirectUri) : that.honeywellRedirectUri != null)
			return false;
		if (honeywellLoginBaseUrl != null ? !honeywellLoginBaseUrl.equals(that.honeywellLoginBaseUrl) : that.honeywellLoginBaseUrl != null)
			return false;
		if (honeywellClientId != null ? !honeywellClientId.equals(that.honeywellClientId) : that.honeywellClientId != null)
			return false;
		if (lutronLoginBaseUrl != null ? !lutronLoginBaseUrl.equals(that.lutronLoginBaseUrl) : that.lutronLoginBaseUrl != null)
			return false;
		if (webLaunchUrl != null ? !webLaunchUrl.equals(that.webLaunchUrl) : that.webLaunchUrl != null) return false;
		if (androidLaunchUrl != null ? !androidLaunchUrl.equals(that.androidLaunchUrl) : that.androidLaunchUrl != null)
			return false;
		if (requiresTermsAndConditionsConsent != null ? !requiresTermsAndConditionsConsent.equals(that.requiresTermsAndConditionsConsent) : that.requiresTermsAndConditionsConsent != null)
			return false;
		if (requiresPrivacyPolicyConsent != null ? !requiresPrivacyPolicyConsent.equals(that.requiresPrivacyPolicyConsent) : that.requiresPrivacyPolicyConsent != null)
			return false;
		if (promonAdUrl != null ? !promonAdUrl.equals(that.promonAdUrl) : that.promonAdUrl != null) return false;
		return redirectBaseUrl != null ? redirectBaseUrl.equals(that.redirectBaseUrl) : that.redirectBaseUrl == null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(billingPublicKey, tokenURL, sessionToken, username, personId, places, smartyAuthToken, smartyAuthID, previewURLBase, staticResourceBaseUrl, secureStaticResourceBaseUrl, honeywellRedirectUri, honeywellLoginBaseUrl, honeywellClientId, lutronLoginBaseUrl, webLaunchUrl, androidLaunchUrl, requiresTermsAndConditionsConsent, requiresPrivacyPolicyConsent, promonAdUrl, redirectBaseUrl);
	}

	public static class PlaceDescriptor {

	   public static final String ROLE_OWNER = "OWNER";
	   public static final String ROLE_OTHER = "OTHER";

	   private String placeId;
	   private String placeName;
	   private String accountId;
	   private String role;
	   private String promonAdEnabled = Boolean.FALSE.toString();

	   public PlaceDescriptor() {
	   }

      public PlaceDescriptor(Map<String, Object> attributes) {
         this.placeId = (String) attributes.get("placeId");
         this.placeName = (String) attributes.get("placeName");
         this.accountId = (String) attributes.get("accountId");
         this.role = (String) attributes.get("role");
         this.promonAdEnabled = Boolean.valueOf((String)attributes.get("promonAdEnabled")).toString();
      }

      public PlaceDescriptor(String placeId, String placeName, String accountId, String role) {
         this(placeId, placeName, accountId, role, false);
      }
      
      public PlaceDescriptor(String placeId, String placeName, String accountId, String role, boolean promonAdEnabled) {
         this.placeId = placeId;
         this.placeName = placeName;
         this.accountId = accountId;
         this.role = role;
         this.promonAdEnabled = Boolean.valueOf(promonAdEnabled).toString();
      }

	   public String getPlaceId() {
	      return placeId;
	   }

	   public void setPlaceId(String placeId) {
	      this.placeId = placeId;
	   }

	   public String getPlaceName() {
	      return placeName;
	   }

	   public void setPlaceName(String placeName) {
	      this.placeName = placeName;
	   }

	   public String getAccountId() {
	      return accountId;
	   }

	   public void setAccountId(String accountId) {
	      this.accountId = accountId;
	   }

	   public String getRole() {
	      return role;
	   }

	   public void setRole(String role) {
	      this.role = role;
	   }
	   
	   public String getPromonAdEnabled()
      {
         return promonAdEnabled;
      }

      public void setPromonAdEnabled(String promonAdEnabled)
      {
         this.promonAdEnabled = promonAdEnabled;
      }

	   @Override
	   public int hashCode() {
	      final int prime = 31;
	      int result = 1;
	      result = prime * result
	            + ((accountId == null) ? 0 : accountId.hashCode());
	      result = prime * result + ((placeId == null) ? 0 : placeId.hashCode());
	      result = prime * result
	            + ((placeName == null) ? 0 : placeName.hashCode());
	      result = prime * result + ((role == null) ? 0 : role.hashCode());
	      result = prime * result + ((promonAdEnabled == null) ? 0 : promonAdEnabled.hashCode());
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
	      PlaceDescriptor other = (PlaceDescriptor) obj;
	      if (accountId == null) {
	         if (other.accountId != null)
	            return false;
	      } else if (!accountId.equals(other.accountId))
	         return false;
	      if (placeId == null) {
	         if (other.placeId != null)
	            return false;
	      } else if (!placeId.equals(other.placeId))
	         return false;
	      if (placeName == null) {
	         if (other.placeName != null)
	            return false;
	      } else if (!placeName.equals(other.placeName))
	         return false;
	      if (role == null) {
	         if (other.role != null)
	            return false;
	      } else if (!role.equals(other.role))
	         return false;
	      if (promonAdEnabled == null) {
            if (other.promonAdEnabled != null)
               return false;
         } else if (!promonAdEnabled.equals(other.promonAdEnabled))
            return false;
	      return true;
	   }

	   @Override
	   public String toString() {
	      return "PlaceDescriptor [placeId=" + placeId + ", placeName=" + placeName
	            + ", accountId=" + accountId + ", role=" + role 
	            + ", promonAdEnabled=" + promonAdEnabled
	            + "]";
	   }
	}
}
