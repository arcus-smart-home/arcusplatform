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
package com.iris.bridge.server.shiro;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.shiro.session.SessionListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.server.netty.Constants;
import com.iris.bridge.server.session.Session;
import com.iris.bridge.server.session.SessionRegistry;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.service.SessionService;

// FIXME should emitting session expired just happen in the dao when the session is actually deleted?
@Singleton
public class ShiroSessionRegistryExpirer extends SessionListenerAdapter {
	
	@Inject 
	SessionRegistry registry;
	
	@Inject
	PlatformMessageBus bus;
	
	private static final Logger logger = LoggerFactory.getLogger(ShiroSessionRegistryExpirer.class);
	
 	
   @PostConstruct
   public void init() {
	   logger.debug("init SessionRegistry should not be null here [{}], and PlatformMessageBus should not be null [{}] ", registry != null, bus != null);
   }
   
    @Override
	public void onStop(org.apache.shiro.session.Session session) {
    	logger.debug("onStop called for session [{}]", session.getId());
    	List<Session> existingSessions = findSessionFromRegistry(session);    	
    	for(Session curSession : existingSessions) {    	
    		curSession.disconnect(Constants.SESSION_EXPIRED_STATUS); 		
    	}    	
    	emitSessionExpiredEvent(session);
	}    

	@Override
	public void onExpiration(org.apache.shiro.session.Session session) {
    	logger.debug("onExpiration called for session [{}]", session.getId());
    	List<Session> existingSessions = findSessionFromRegistry(session);    	  	
    	for(Session curSession : existingSessions) { 
    		curSession.disconnect(Constants.SESSION_EXPIRED_STATUS);    		
    	}    	
	}
      
    
	private List<Session> findSessionFromRegistry(org.apache.shiro.session.Session session) {
		List<Session> existingSessions = new ArrayList<Session>();		
	    if(session != null) {
	    	String sessionId = session.getId().toString();
	    	for(Session curSession : registry.getSessions()) {
	    		if(curSession.getClient() != null && sessionId.equals(curSession.getClient().getSessionId())) {	    		    			
	    			existingSessions.add(curSession);	    			 	    			
	    		}
	    	}   	    	
	    }
	    return existingSessions;
	}
	
	private void emitSessionExpiredEvent(org.apache.shiro.session.Session session) {		
		MessageBody body = SessionService.SessionExpiredEvent.builder().withSessionId(session.getId().toString()).build();
		PlatformMessage event = PlatformMessage.buildBroadcast(body, Address.platformService(SessionService.NAMESPACE)).create();
	    bus.send(event);
	    logger.debug("SessionExpiredEvent emitted for session [{}]", session.getId());
	}
}

