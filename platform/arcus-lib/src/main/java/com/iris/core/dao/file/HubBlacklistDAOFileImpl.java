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
package com.iris.core.dao.file;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.iris.core.dao.HubBlacklistDAO;
import com.iris.gson.DateTypeAdapter;
import com.iris.messages.model.BlacklistedHub;
import com.iris.resource.Resource;
import com.iris.resource.ResourceListener;
import com.iris.resource.Resources;
import com.iris.resource.manager.ResourceParser;
import com.iris.resource.manager.SingleFileResourceManager;

public class HubBlacklistDAOFileImpl implements HubBlacklistDAO, ResourceListener {
	
	private static final Logger logger = LoggerFactory.getLogger(HubBlacklistDAOFileImpl.class);
	
	@Inject(optional = true)
    @Named("hubblacklist.db.uri")
    private String hubblacklistFile = "classpath:/hubblacklist.json";
    Resource resource = null;
    private Map<String, BlacklistedHub> cachedMap = null;

    private HubBlacklistResourceWatcher watcher;
   
    public HubBlacklistDAOFileImpl() {
      
    }
   
   @PostConstruct
   public void init() {
      resource = Resources.getResource(hubblacklistFile);
      watcher = new HubBlacklistResourceWatcher(resource, new GsonParser());   
      watcher.addListener(this);
   }

   @Override
   public BlacklistedHub findBlacklistedHubByCertSn(String certSn) {	   
      return certSn == null ? null : getCachedData().get(certSn);
   }

   /**
    * Note, Only save to the cache, not the actual file.  So if there is a file update or container restart, the saved BlacklistedHub will be 
    * lost.
    */
   @Override
   public BlacklistedHub save(BlacklistedHub hub) {
      if (hub != null) {
         Date blacklistedDate = hub.getBlacklistedDate() != null ? hub.getBlacklistedDate() : new Date();
         BlacklistedHub newHub = hub.copy();
         newHub.setBlacklistedDate(blacklistedDate);
         getCachedData().put(hub.getCertSn(), newHub);
         return newHub;
      }
      return null;
   }

   /**
    * Note, Only delete from the cache, not the actual file.  So if there is a file update or container restart, the deleted BlacklistedHub will be 
    * restored.
    */
   @Override
   public void delete(BlacklistedHub hub) {
      if (hub != null && hub.getCertSn() != null) {
    	  getCachedData().remove(hub.getCertSn());
      }
      
   }
   
   @Override
   public void onChange() {
   		cachedMap = null;   	
   }
   
   
   private Map<String, BlacklistedHub> getCachedData() {
	   if(cachedMap == null) {
		   List<BlacklistedHub> cachedList = watcher.getParsedData();
		   cachedMap = new HashMap<String, BlacklistedHub>();
		   if(cachedList != null) {
			   cachedList.forEach((cur) -> {
				   cachedMap.put(cur.getCertSn(), cur);
			   });
		   }
	   }
	   return cachedMap!=null?cachedMap:new HashMap<>(0);
   }
   
   
  
   private static class GsonParser implements ResourceParser<List<BlacklistedHub>> {
	   private Gson gson;
	   
	   GsonParser() {
		   GsonBuilder builder = new GsonBuilder();
		   builder.registerTypeAdapter(Date.class, new DateTypeAdapter());
		   gson = builder.create();
	   }

	   @Override
	   public List<BlacklistedHub> parse(InputStream input) {
		   if (input != null) {
			   try(InputStreamReader reader = new InputStreamReader(input, "UTF-8")) {
				   String jsonString = CharStreams.toString(reader);
				   BlacklistedHub[] hubArray = gson.fromJson(jsonString, BlacklistedHub[].class);
				   if(hubArray != null) {
					   return Arrays.asList(hubArray);
				   }
			   }catch(Exception e) {
				   logger.error("Fail to parse json file", e);
			   }
			   
	       } 
	       return null;
	   }
	   
   }
   
   
   private static class HubBlacklistResourceWatcher extends SingleFileResourceManager<List<BlacklistedHub>> {

		public HubBlacklistResourceWatcher(Resource managedResource, ResourceParser<List<BlacklistedHub>> parser) {
			super(managedResource, parser);
		}
	
		@Override
		public List<BlacklistedHub> getParsedData() {
			List<BlacklistedHub> cachedData = getCachedData();
		    return cachedData;
		}
	   
   }




}

