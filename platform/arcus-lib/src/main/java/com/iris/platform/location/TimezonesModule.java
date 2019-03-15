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
/**
 *
 */
package com.iris.platform.location;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.resource.Resource;
import com.iris.resource.Resources;

/**
 *
 */
public class TimezonesModule extends AbstractIrisModule {
	
	public static final String TIMEZONE_PATH_PROP = "timezones.path";
	public static final String TIMEZONE_RESOURCE = "timezones.resource";
	
	@Inject(optional = true)
	@Named(TIMEZONE_PATH_PROP)
	private String timezonePath = "classpath:/timezones.json";
	
	
   
   @Override
   protected void configure() {
      bind(TimezonesManager.class);
   }
   
   @Provides @Singleton @Named(TimezonesModule.TIMEZONE_RESOURCE)
   public Resource provideTimezones() {
      return Resources.getResource(timezonePath);
   }

}

