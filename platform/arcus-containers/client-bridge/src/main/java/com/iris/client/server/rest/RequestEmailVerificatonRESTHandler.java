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
package com.iris.client.server.rest;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.config.RESTHandlerConfig;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.annotation.HttpPost;
import com.iris.bridge.server.http.handlers.RESTHandler;
import com.iris.bridge.server.http.impl.auth.SessionAuth;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.notification.Notifications;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.ClientMessage;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.PersonCapability.SendVerificationEmailRequest;
import com.iris.messages.errors.NotFoundException;
import com.iris.messages.model.Person;
import com.iris.messages.services.PlatformConstants;
import com.iris.platform.person.PersonHandlerHelper;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.util.TokenUtil;

import io.netty.channel.ChannelHandlerContext;

@Singleton
@HttpPost("/" + PersonCapability.NAMESPACE + "/SendVerificationEmail")
public class RequestEmailVerificatonRESTHandler extends RESTHandler {

	@Inject(optional = true)
	@Named("person.verificationtoken.length")
	private int tokenLength = 12;
	private static final Logger logger = LoggerFactory.getLogger(RequestEmailVerificatonRESTHandler.class);
	
   private final PersonDAO personDao;
   private final PlatformMessageBus platformBus;
   private final ClientFactory clientFactory;
   private final PersonPlaceAssocDAO personPlaceAssocDao;
   private final PlacePopulationCacheManager populationCacheMgr;

   @Inject
   public RequestEmailVerificatonRESTHandler(
         SessionAuth auth,
         BridgeMetrics metrics,
         PersonDAO personDao,
         PlatformMessageBus platformBus,
         RESTHandlerConfig restHandlerConfig,
         ClientFactory factory,
         PersonPlaceAssocDAO personPlaceAssocDao,
         PlacePopulationCacheManager populationCacheMgr) {

      super(auth, new HttpSender(RequestEmailVerificatonRESTHandler.class, metrics),restHandlerConfig);
      this.personDao = personDao;
      this.platformBus = platformBus;
      this.clientFactory = factory;
      this.personPlaceAssocDao = personPlaceAssocDao;
      this.populationCacheMgr = populationCacheMgr;
   }
   
   @Override
   protected MessageBody doHandle(ClientMessage request, ChannelHandlerContext ctx) throws Exception {
   	
   	String destinationStr = request.getDestination();  	
   	
   	//ensure person exists
   	Person curPerson = loadPerson(destinationStr);
   	if(curPerson == null) {
   		throw new NotFoundException(Address.fromString(destinationStr));
   	}
   	/**
   	 * The following security check is commented out due to I2-3123.  Ideally we would like the client to re-authenticate 
   	 * after email address change since email address is considered to be the username.  But it is too much work to 
   	 * make the change at this moment.  4/5/18
   	 */
   	//ensure person's email matches with the session principal name.  Only the person himself can perform this operation
   	/* Client client = clientFactory.get(ctx.channel());
   	String actor = client.getPrincipalName();   	
   	if(!Objects.equal(actor, curPerson.getEmail())) {
   		throw new ErrorEventException(Errors.invalidRequest());
   	}*/	
   	
   	//generate token and update person
   	String newToken = TokenUtil.randomTokenString(tokenLength);
   	curPerson.setEmailVerificationToken(newToken);
   	boolean valueChange = false;
   	if(curPerson.getEmailVerified() != null) {
   		curPerson.setEmailVerified(null);
   		valueChange = true;
   	}
   	personDao.update(curPerson);
   	
   	//send notification
   	MessageBody reqBody = request.getPayload();
   	String source = SendVerificationEmailRequest.getSource(reqBody, SendVerificationEmailRequest.SOURCE_WEB);
   	sendNotification(curPerson, source);
   	
   	//send value change event
   	if(valueChange) {
   		if(PersonHandlerHelper.getInstance().sendPersonValueChangesToPlaces(personPlaceAssocDao, platformBus, populationCacheMgr, Address.fromString(request.getDestination()), curPerson.getId(), ImmutableMap.<String, Object>of(PersonCapability.ATTR_EMAILVERIFIED, Boolean.FALSE))){
   			//ok
   		}else{
	         logger.warn("This should not happen, but person [{}] has no places associated with it.  Possibly index out of sync", curPerson.getId());   			
   		}
   	}

      return PersonCapability.SendVerificationEmailResponse.instance();
	}
   
   @Override
	protected MessageBody doHandle(ClientMessage request) throws Exception {
		//no op
		return null;
	}
   
   private void sendNotification(Person curPerson, String source) {
      PlatformMessage msg  = Notifications.builder()
            .withSource(Address.platformService(PlatformConstants.SERVICE_PEOPLE))
            .withPersonId(curPerson.getId())
            .withPriority(NotificationCapability.NotifyRequest.PRIORITY_LOW)
            .withMsgKey(Notifications.AccountEmailVerify.KEY)
            .addMsgParam(Notifications.AccountEmailVerify.PARAM_TOKEN, curPerson.getEmailVerificationToken())
            .addMsgParam(Notifications.AccountEmailVerify.PARAM_PLATFORM, source.toLowerCase())
            .create();
      platformBus.send(msg);
		
	}

	private Person loadPerson(String destination) {
      Address addr = Address.fromString(destination);
      if(addr == null) {
         return null;
      }
      return personDao.findById((UUID) addr.getId());
   }

	

}

