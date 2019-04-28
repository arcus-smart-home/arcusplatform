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
package com.iris.client.server.session;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.bridge.server.session.Session;
import com.iris.bridge.server.session.SessionListener;
import com.iris.client.bounce.BounceConfig;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.io.json.JSON;
import com.iris.messages.ClientMessage;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.model.Person;
import com.iris.messages.model.Place;
import com.iris.messages.model.PlaceDescriptor;
import com.iris.security.authz.AuthorizationContext;
import com.iris.security.authz.AuthorizationGrant;
import com.iris.security.principal.Principal;

@Singleton
public class HandshakeSessionListener implements SessionListener {
	private static final Logger logger = LoggerFactory.getLogger(HandshakeSessionListener.class);
	
	@Inject
	@Named( value = "smarty.auth.id")
	private String smartyAuthID;

	@Inject
	@Named( value = "smarty.auth.token")
	private String smartyAuthToken;

	@Inject
	@Named(value = "billing.public.api.key")
	private String recurlyPublicAPIKey;

	@Inject
	@Named(value = "billing.token.url")
	private String recurlyTokenURL;

	@Inject
	@Named(value = "camera.preview.baseurl")
	private String cameraPreviewBaseUrl;

	@Inject
	@Named(value = "static.resource.base.url")
	private String staticResourceBaseUrl;
	
	@Inject
	@Named(value = "secure.static.resource.base.url")
	private String secureStaticResourceBaseUrl;

	@Inject
	@Named(value = "termsAndConditions.version")
	private String termsConditionsVersionString;

	@Inject
	@Named(value = "privacyPolicy.version")
	private String privacyPolicyVersionString;
	
   @Inject
   @Named(value = "redirect.base.url")
   private String redirectBaseUrl;
   
   private BounceConfig bounceConfig;
	
		
	private final PersonDAO personDao;
	private final PlaceDAO placeDao;
	private Date termsConditionsVersion;
	private Date privacyPolicyVersion;
		
	@Inject
	public HandshakeSessionListener(
			BounceConfig bounceConfig,
			PersonDAO personDao, 
			PlaceDAO placeDao
	) {
		this.bounceConfig = bounceConfig;
		this.personDao = personDao;
		this.placeDao = placeDao;
	}
		
	@PostConstruct
	public void init() throws ParseException {
		this.termsConditionsVersion = new SimpleDateFormat("MM-dd-yyyy", Locale.US).parse(termsConditionsVersionString);
		this.privacyPolicyVersion = new SimpleDateFormat("MM-dd-yyyy", Locale.US).parse(privacyPolicyVersionString);
	}

   @Override
   public void onSessionCreated(Session session) {
      AuthorizationContext context = session.getAuthorizationContext();
      Principal principal = context.getPrincipal();
      
      Person curPerson = personDao.findById(principal.getUserId());
      if(curPerson == null) {
         logger.debug("Received session with for principal id: [{}] name: [{}] but user could not be loaded, logging out client", principal.getUserId(), principal.getUsername());
         session.getClient().logout();
         return;
      }
      
      Boolean requiresTermsAndConditionsConsent = isRequiresConsent(curPerson.getTermsAgreed(), termsConditionsVersion);
      Boolean requiresPrivacyPolicyConsent = isRequiresConsent(curPerson.getPrivacyPolicyAgreed(), privacyPolicyVersion);

      Map<String, Object> entries = new HashMap<>();
      entries.put("smartyAuthID", String.valueOf(smartyAuthID));
      entries.put("smartyAuthToken", String.valueOf(smartyAuthToken));
      entries.put("tokenURL", String.valueOf(recurlyTokenURL));
      entries.put("publicKey", String.valueOf(recurlyPublicAPIKey));
      entries.put("personId", String.valueOf(principal.getUserId()));
      entries.put("cameraPreviewBaseUrl", String.valueOf(cameraPreviewBaseUrl));
      entries.put("staticResourceBaseUrl", String.valueOf(staticResourceBaseUrl));
      entries.put("secureStaticResourceBaseUrl", String.valueOf(secureStaticResourceBaseUrl));
      entries.put("places", listPlaceDescriptors(context));
      if(isAccountOwner(curPerson)) {
         entries.put("requiresTermsAndConditionsConsent", requiresTermsAndConditionsConsent);
         entries.put("requiresPrivacyPolicyConsent", requiresPrivacyPolicyConsent);
      }
      else {
         entries.put("requiresTermsAndConditionsConsent", false);
         entries.put("requiresPrivacyPolicyConsent", false);
      }
      
      entries.put("redirectBaseUrl", redirectBaseUrl);
      entries.put("androidLaunchUrl", bounceConfig.getRedirectUrl() + "/android/launch");
      entries.put("iosLaunchUrl", bounceConfig.getRedirectUrl() + "/ios/launch");
      entries.put("webLaunchUrl", bounceConfig.getRedirectUrl() + "/web/launch");

      MessageBody body = MessageBody.buildMessage(MessageConstants.MSG_SESSION_CREATED,
      		ImmutableMap.copyOf(entries)
   		);
      ClientMessage msg = ClientMessage.builder().withPayload(body).create();
      session.sendMessage(JSON.toJson(msg));
   }

   @Override
   public void onSessionDestroyed(Session session) {
      // no op
   }

   private Set<PlaceDescriptor> listPlaceDescriptors(AuthorizationContext context) {
      
      return context.getGrants().stream()
            .map((g) -> {
               return createPlaceDescriptor(g);
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
   }
   
   protected PlaceDescriptor createPlaceDescriptor(AuthorizationGrant g) {
      Place place = placeDao.findById(g.getPlaceId());
      if(place != null) {
         return new PlaceDescriptor(
            String.valueOf(g.getPlaceId()),
            g.getPlaceName(),
            String.valueOf(g.getAccountId()),
            g.getRole()
			);
      }else{
         logger.warn("place with id {} no longer exists", g.getPlaceId());
         return null;
      }
   }
   
   protected boolean isAccountOwner(Person person) {
      return person.getAccountId() != null;
   }
   
   protected boolean isRequiresConsent(Date lastConsentDate, Date minConsentDate) {
	   if(lastConsentDate != null) {
		   if(minConsentDate != null) {
			   if(minConsentDate.compareTo(new Date()) > 0) {
				   //minConsentDate is in the future, we will ignore it for now
				   return false;
			   }else {
				   return lastConsentDate.compareTo(minConsentDate) < 0;
			   }
		   }else {
			   return false;
		   }
	   }else {
		   return true;
	   }
	   
   }
   
}

