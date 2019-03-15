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

import java.util.Collections;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.iris.core.IrisAbstractApplication;
import com.iris.modelmanager.context.ManagerContext;
import com.iris.modelmanager.context.Profile;
import com.iris.modelmanager.engine.ExecutionEngine;

public class ModelManager extends IrisAbstractApplication{

	private static final Logger logger = LoggerFactory.getLogger(ModelManager.class);
	private final ModelManagerConfig config;
	private final Profile profile;	
   
   @Inject
   public ModelManager(Profile profile, ModelManagerConfig config) {
      this.config = config;
      this.profile = profile;
   }
   
   @Override
   protected void start() throws Exception {
   	ExecutionEngine engine = new ExecutionEngine(createContext(config.getSchema()));
   	engine.execute();
   }
   
   public static void main(String... args) {
      IrisAbstractApplication.exec(ModelManager.class, Collections.emptyList(), args);
      System.exit(0);
   }
   
   protected ManagerContext createContext(String schema) throws Exception {
      return createContextBuilder(schema).build();
   }
   
   public ManagerContext.Builder createContextBuilder(String schema) throws Exception {
   	ManagerContext.Builder builder = new ManagerContext.Builder();
      builder.setAuto(config.isAuto());
      builder.setChangelog(config.getChangeLog());
      builder.setHomeDirectory(config.getHomeDirectory());
      builder.setProfile(profile);

      if(config.isRollback()) {
         builder.setRollback(true);
         builder.setRollbackTarget(config.getRollbackTarget());
      }

      return builder;
   }
   
   
}

