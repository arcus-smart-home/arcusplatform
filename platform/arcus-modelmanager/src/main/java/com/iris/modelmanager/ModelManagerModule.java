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
package com.iris.modelmanager;

import org.apache.commons.lang3.StringUtils;

import com.google.inject.Inject;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.modelmanager.context.ModelManagerBaseCassandraModule;
import com.netflix.governator.configuration.ConfigurationProvider;


public class ModelManagerModule extends AbstractIrisModule {

	private final String schema;
	private final ModelManagerConfig config;
	private final ConfigurationProvider configProvider;
	
	@Inject
	public ModelManagerModule(ModelManagerConfig config, ConfigurationProvider configProvider) {
		schema = config.getSchema();
		this.config = config;
		this.configProvider = configProvider;
	}
	
	
	/* (non-Javadoc)
	 * @see com.google.inject.AbstractModule#configure()
	 */
	@Override
	protected void configure() {
		if(ModelManagerConfig.DEFAULT_SCHEMA.equals(schema) || StringUtils.isBlank(schema)) {
			//default
			ModelManagerBaseCassandraModule cassandraModule = new ModelManagerBaseCassandraModule(configProvider, null) {};
			binder().requestInjection(cassandraModule);
   		binder().install(cassandraModule);
		}else {
			ModelManagerBaseCassandraModule cassandraModule = new ModelManagerBaseCassandraModule(configProvider, schema) {};
			binder().requestInjection(cassandraModule);
   		binder().install(cassandraModule);
		}
		
	}
	
}

