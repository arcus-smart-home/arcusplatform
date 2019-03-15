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
package com.iris.agent.spy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Predicate;
import com.iris.messages.PlatformMessage;

/**
 * The Spy Service is used to manage diagnostic information about the hub mostly
 * for the purposes of development.
 * 
 * In order to activate the Spy Service the environment variable IRIS_HUB_SPY_ACTIVE
 * must be defined.
 * 
 * Environment Variables:
 * 
 * IRIS_HUB_SPY_ACTIVE - If defined, then the spy service will be active.
 * 
 * IRIS_HUB_SPY_PLATFORM_MSG_CAP - Sets the cap on number of platform messages 
 *     tracked by the spy service. The default value is 20.
 * 
 * @author Erik Larson
 *
 */
public final class SpyService {
   public static final String PLUGINS = "_plugins";
   
	public static SpyService INSTANCE = new SpyService();
	
	private final boolean active;
	private final SpyStore store;
	private final List<SpyPlugIn> plugins = new ArrayList<>();
	private final Map<String, Supplier<Object>> context = new HashMap<>();

	private SpyService() {
		active = System.getenv("IRIS_HUB_SPY_ACTIVE") != null;
		store = active ? new ActiveSpyStore() : new InactiveSpyStore();
		context.put(PLUGINS, () -> shownPlugIns());
	}
	
	public Map<String, Supplier<Object>> getContext() {
	   return context;
	}
	
	public void registerPlugin(SpyPlugIn plugIn) {
	   plugins.add(plugIn);
	}
	
	public SpyPlugIn getPlugInByPage(String page) {
	   return findOne(plugins, p -> p.pageName().equalsIgnoreCase(page));
	}
	
	public boolean isActive() {
		return active;
	}
	
	public Stream<PlatformMessage> incomingPlatformMsgs() {
		return store.streamIncomingPlatformMsgs();
	}
	
	public Stream<PlatformMessage> outgoingPlatformMsgs() {
		return store.streamOutgoingPlatformMsgs();
	}
	
	public List<SpyPlugIn> shownPlugIns() {
	   List<SpyPlugIn> spi = plugins.stream().filter((p) -> p.showLink()).collect(Collectors.toList());
	   return spi;
	}
	
	public List<SpyPlugIn> plugIns() {
	   return Collections.unmodifiableList(plugins);
	}
	
	public void spyOnPlatformMessage(PlatformMessage msg) {
	   if (msg != null) {
   		if (msg.getDestination() != null && msg.getDestination().isHubAddress()) {
   			store.storeIncomingPlatformMsg(msg);
   		}
   		else {
   			store.storeOutgoingPlatformMsg(msg);
   		}
	   }
	}
	
	private SpyPlugIn findOne(Collection<SpyPlugIn> collection, Predicate<SpyPlugIn> test) {
      if (collection != null) {
         for (SpyPlugIn item : collection) {
            if (test.apply(item)) {
               return item;
            }
         }
      }
      return null;
   }
	
}


