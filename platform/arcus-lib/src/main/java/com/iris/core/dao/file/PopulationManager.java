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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.iris.core.dao.PopulationDAO;
import com.iris.messages.type.Population;
import com.iris.resource.Resource;
import com.iris.resource.Resources;
import com.iris.resource.manager.ResourceParser;
import com.iris.resource.manager.SingleFileResourceManager;

/**
 * This implementation assume there are not too many entries for the population.  So most of the find operation is to loop through the entire list.
 * @author daniellep
 *
 */
public class PopulationManager  implements PopulationDAO {

	private static final Logger logger = LoggerFactory.getLogger(PopulationManager.class);

    @Inject(optional = true)
    @Named("population.db.uri")
    private String populationXmlFile = "classpath:/populations.xml";
    Resource resource = null;
    private PopulationWatcher watcher;

    @PostConstruct
    public void init() {
       resource = Resources.getResource(populationXmlFile);
       watcher = new PopulationWatcher(resource, new PopulationDeserializer());      
    }
	   
	@Override
	public Population save(Population population) {
		throw new UnsupportedOperationException("This implementation does not support modification");
	}

	@Override
	public void delete(Population population) {
		throw new UnsupportedOperationException("This implementation does not support delete.");
	}
	

	@Override
	public Population findByName(String populationName) {
		List<Population> populations = watcher.getParsedData();
		if(populations != null) {
			Iterator<Population> it = populations.iterator();
			while( it.hasNext() ) {
				Population cur = it.next();
				if(cur.getName() != null && cur.getName().equals(populationName)) {
					return cur;
				}
			}
		}
		return null;
	}

	@Override
	public Population getDefaultPopulation() {
		return findByName(Population.NAME_GENERAL);
	}

	@Override
	public List<Population> listPopulations() {
		return Collections.unmodifiableList(watcher.getParsedData());
	}
	
	
	private static class PopulationWatcher extends SingleFileResourceManager<List<Population>> {

		public PopulationWatcher(Resource managedResource, ResourceParser<List<Population>> parser) {
			super(managedResource, parser);
		}

		@Override
		public List<Population> getParsedData() {
			List<Population> cachedData = getCachedData();
		    return cachedData != null ? cachedData : new ArrayList<Population>(0);
		}
		
	}

}

