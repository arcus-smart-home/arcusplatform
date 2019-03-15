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

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class ModelManagerConfig
{	
	static final String DEFAULT_SCHEMA = "platform";
	
	@Inject(optional = true) @Named("modelmanager.schema")
   private String schema = DEFAULT_SCHEMA;	

	@Inject(optional = true) @Named("modelmanager.changelog")
   private String changeLog = "changelog-master.xml";
	
	@Inject(optional = true) @Named("modelmanager.profile")
   private String profile = "production";
	
	@Inject(optional = false) @Named("modelmanager.home")
   private String homeDirectory;
	
	@Inject(optional = true) @Named("modelmanager.auto")
   private boolean auto = false;
	
	@Inject(optional = true) @Named("modelmanager.rollback")
   private boolean rollback = false;
	
	@Inject(optional = true) @Named("modelmanager.rollback.target")
   private String rollbackTarget;
	
	
	public ModelManagerConfig() {
		
	}
	


	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema){
		this.schema = schema;
	}	
	
	public String getChangeLog() {
		return changeLog;
	}

	public void setChangeLog(String changeLog) {
		this.changeLog = changeLog;
	}

	public String getProfile() {
		return profile;
	}

	public void setProfile(String profile) {
		this.profile = profile;
	}

	public String getHomeDirectory() {
		return homeDirectory;
	}

	public void setHomeDirectory(String homeDirectory) {
		this.homeDirectory = homeDirectory;
	}

	public boolean isAuto() {
		return auto;
	}

	public void setAuto(boolean auto) {
		this.auto = auto;
	}

	public boolean isRollback() {
		return rollback;
	}

	public void setRollback(boolean rollback) {
		this.rollback = rollback;
	}

	public String getRollbackTarget() {
		return rollbackTarget;
	}

	public void setRollbackTarget(String rollbackTarget) {
		this.rollbackTarget = rollbackTarget;
	}
	
}

