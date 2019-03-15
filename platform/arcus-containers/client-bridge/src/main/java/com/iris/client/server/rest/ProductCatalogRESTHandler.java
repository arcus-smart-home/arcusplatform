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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.iris.Utils;
import com.iris.bridge.server.config.RESTHandlerConfig;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.handlers.RESTHandler;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;
import com.iris.core.dao.HubDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.dao.PopulationDAO;
import com.iris.messages.address.Address;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Hub;
import com.iris.messages.model.Place;
import com.iris.messages.type.Population;
import com.iris.model.Version;
import com.iris.prodcat.ProductCatalog;
import com.iris.prodcat.ProductCatalogManager;

public abstract class ProductCatalogRESTHandler extends RESTHandler {
   private final static String PARAM_PLACE = "place";
	private final ProductCatalogManager manager;
	private final PopulationDAO populationDao;
	private Population defaultPopulation;
	private final PlaceDAO placeDao;
	private final HubDAO hubDao;
   private LoadingCache<String, Population> populationCache;
	 

	public ProductCatalogRESTHandler(AlwaysAllow alwaysAllow, HttpSender httpSender, ProductCatalogManager manager,
			PopulationDAO populationDao, PlaceDAO placeDao, HubDAO hubDao, RESTHandlerConfig restHandlerConfig) {
		super(alwaysAllow, httpSender,restHandlerConfig);
		this.manager = manager;
		this.populationDao = populationDao;
		this.placeDao = placeDao;
		this.hubDao = hubDao;
	}

	@Override
	protected void init() {
		super.init();
		//TODO - Maybe we should preload all the entries
		populationCache = CacheBuilder.newBuilder()
         .build(new CacheLoader<String,Population>() {
            @Override
            public Population load(String name) throws Exception
            {
               return populationDao.findByName(name);
            }            
         });
		defaultPopulation = populationDao.getDefaultPopulation();
		if (defaultPopulation == null) {
			throw new RuntimeException("No default population defined.");
		}
	}

	protected ProductCatalog getDefaultCatalog() {
		ProductCatalog catalog = manager.getCatalog(defaultPopulation.getName());
		Utils.assertNotNull(catalog, "The product catalog is required");
		return catalog;
	}
	
	protected ProductCatalog getCatalog(Population population) {
	   ProductCatalog catalog = null;
	   if(population == null) {	      
	      catalog = manager.getCatalog(defaultPopulation.getName());
	   }else{
	      catalog = manager.getCatalog(population.getName());	      
	   }
	   Utils.assertNotNull(catalog, "The product catalog is required");
      return catalog;
   }
	
	/**
	 * If place address is missing from the request, the default population will be returned.  Otherwise, 
	 * it will lookup the population on the place model.  Again, the the population field is not set, the default 
	 * population will be returned.
	 * @param placeAddressStr
	 * @return
	 */
	protected Population determinePopulationFromRequest(String placeAddressStr) {
	   if(StringUtils.isBlank(placeAddressStr)) {
	      return defaultPopulation;
	   }else{
	      try{
	         UUID placeId = (UUID) Address.fromString(placeAddressStr).getId();
	         Place place = placeDao.findById(placeId);
	         if(place == null) {
	            throw new ErrorEventException(Errors.invalidParam(PARAM_PLACE));
	         }else{
	            String population = place.getPopulation();
	            if(population == null) {
	               //population not set on the place, use default
	               return defaultPopulation;
	            }else{
	               return populationCache.get(population);
	            }
	         }
	      }catch(Exception e) {
	         throw new ErrorEventException(Errors.invalidParam(PARAM_PLACE), e);
	      }
	   }
	}
	
	@Nullable
	protected Version determineHubFirmwareVersionFromRequest(String placeAddressStr) {
	   if(StringUtils.isNotBlank(placeAddressStr)) {
	      UUID placeId = null;
	      try{
            placeId = (UUID) Address.fromString(placeAddressStr).getId();
	      }catch(Exception e) {
            throw new ErrorEventException(Errors.invalidParam(PARAM_PLACE), e);
         }
         Hub hub = hubDao.findHubForPlace(placeId);
         if(hub != null) {
            String osVersion = hub.getOsver();
            if(StringUtils.isNotBlank(osVersion)) {
               return Version.fromRepresentation(osVersion);
            }
         }                  
      }
	   return null;
	}

	protected ProductCatalogManager getManager() {
		return manager;
	}

	protected PopulationDAO getPopulationDao() {
		return populationDao;
	}

	protected Population getDefaultPopulation() {
		return defaultPopulation;
	}
}

