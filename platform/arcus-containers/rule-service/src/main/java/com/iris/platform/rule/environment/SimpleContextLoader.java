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
package com.iris.platform.rule.environment;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.common.rule.RuleContext;
import com.iris.common.rule.simple.SimpleContext;
import com.iris.messages.address.Address;
import com.iris.messages.model.Model;
import com.iris.platform.model.ModelDao;

/**
 * This version does not cache and is only used for resolve requests.
 */
@Singleton
public class SimpleContextLoader implements ContextLoader {
   private static final Logger logger = LoggerFactory.getLogger(SimpleContextLoader.class);
   private final ModelDao modelDao;
   
   @Inject
   public SimpleContextLoader(ModelDao modelDao) {
      this.modelDao = modelDao;
   }
   
   @Override
   public RuleContext load(UUID placeId) {
      Collection<Model> models = modelDao.loadModelsByPlace(placeId, RuleModelStore.TRACKED_TYPES);
      SimpleContext context = new SimpleContext(placeId, Address.platformService("rule"), LoggerFactory.getLogger("rules." + placeId));
      models.stream()
      	.filter(Objects::nonNull)
      	.forEach((m) -> {
      		try{
      			context.putModel(m);
      		}catch(IllegalArgumentException e) {
      			logger.warn("Error putting model in RuleContext for place [{}], model [{}:{}]", placeId, m!=null?m.getType():"<null>", m!=null?m.getId():"<null>", e);
      		}      		      	
      	});	
      return context;
   }

}

