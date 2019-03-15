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
package com.iris.core.template;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jknack.handlebars.io.AbstractTemplateLoader;
import com.github.jknack.handlebars.io.StringTemplateSource;
import com.github.jknack.handlebars.io.TemplateSource;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.iris.resource.Resource;
import com.iris.resource.Resources;

public class IrisResourceLoader extends AbstractTemplateLoader {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(IrisResourceLoader.class);
	private int cacheSize=1000; 
	
	public IrisResourceLoader(String prefix,int cacheSize) {
		super();
		this.setPrefix(prefix);
		this.cacheSize = cacheSize;
	}

	private LoadingCache<String, TemplateSource> resourceCache = CacheBuilder.newBuilder()
		       .maximumSize(cacheSize)
		       .removalListener(removal->{LOGGER.info("Removing template with key {} from cache",removal.getKey());})
		       .build(
		           new CacheLoader<String, TemplateSource>() {
		             public TemplateSource load(String location) throws IOException {
		               return createStringTemplateSource(location);
		             }
		           });
	
	@Override
	public TemplateSource sourceAt(String location) throws IOException {
		try {
			return resourceCache.get(location);
		} catch (ExecutionException e) {
			LOGGER.error("Error loading template from iris resource",e);
			throw new RuntimeException(e);
		}
	}
	
	private TemplateSource createStringTemplateSource(String location) throws IOException {
		
		String uri = getPrefix() + location + getSuffix();
		LOGGER.info("Loading Resource {}",uri);
		
		Resource resource = Resources.getResource(uri);		
		String content = null;
		
		try (InputStream is = resource.open()) {
		    content = IOUtils.toString(is);
		}
		
		if(resource.isWatchable()){
			resource.addWatch(()->{resourceCache.invalidate(location);});
		}

		return new StringTemplateSource(location, content);
	}
}

