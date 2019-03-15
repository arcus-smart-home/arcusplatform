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
package com.iris.video.purge;

import org.apache.commons.lang3.StringUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.iris.video.VideoDaoConfig;

public class VideoPurgeTaskConfig extends VideoDaoConfig {

   @Inject(optional = true) @Named("video.purge.maximum")
   protected int max = 10;

   @Inject(optional = true) @Named("video.purge.concurrency")
   protected int concurrency = 4;

   @Inject(optional = true) @Named("video.purge.dryrun")
   protected boolean purgeDryRun = true;

   @Inject(optional = true) @Named("video.purge.send.deleted")
   protected boolean sendDeleted = false;
   
   @Inject(optional = true) @Named("video.purge.mode")
   protected String purgeModeStr = PurgeMode.DELETED.name();   

	private PurgeMode purgeMode;
   

   public int getMax() {
      return max;
   }

   public boolean hasMax() {
      return max >= 0;
   }

   public void setMax(int max) {
      this.max = max;
   }

   public int getConcurrency() {
      return concurrency;
   }

   public void setConcurrency(int concurrency) {
      this.concurrency = concurrency;
   }

   public boolean isPurgeDryRun() {
      return purgeDryRun;
   }

   public void setPurgeDryRun(boolean purgeDryRun) {
      this.purgeDryRun = purgeDryRun;
   }

   public boolean isSendDeleted() {
      return sendDeleted;
   }

   public void setSendDeleted(boolean sendDeleted) {
      this.sendDeleted = sendDeleted;
   }
   
   public String getPurgeModeStr() {
		return purgeModeStr;
	}

	public void setPurgeModeStr(String purgeModeStr) {
		this.purgeModeStr = purgeModeStr;
		purgeMode = null;
	}
	
   public PurgeMode getPurgeMode() {
   	if(purgeMode == null) {
   		if(!StringUtils.isBlank(purgeModeStr)) {
   			try{
   				purgeMode = PurgeMode.valueOf(purgeModeStr);
   				return purgeMode;
   			}catch(Exception e) {}
   		}
   		purgeMode = PurgeMode.DELETED;
   	}
   	return purgeMode;
   }
}

