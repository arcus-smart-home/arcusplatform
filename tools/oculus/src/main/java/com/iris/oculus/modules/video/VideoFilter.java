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
package com.iris.oculus.modules.video;

import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;

import com.iris.client.service.VideoService.PageRecordingsRequest;

public class VideoFilter {
	private Optional<Date> newest = Optional.empty();
	private Optional<Date> oldest = Optional.empty();
	private String type = PageRecordingsRequest.TYPE_ANY;
	private boolean includeDeleted = true;
	private boolean includeInProgress = true;
	private Set<String> cameras = new HashSet<>();
	private Set<String> tags = new HashSet<>();
	
	public Optional<Date> getNewest() {
		return newest;
	}
	
	public void setNewest(@Nullable Date newest) {
		this.newest = Optional.ofNullable(newest);
	}
	
	public Optional<Date> getOldest() {
		return oldest;
	}
	
	public void setOldest(@Nullable Date oldest) {
		this.oldest = Optional.ofNullable(oldest);
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = Optional.ofNullable(type).orElse(PageRecordingsRequest.TYPE_ANY);
	}
	
	public boolean isIncludeDeleted() {
		return includeDeleted;
	}
	
	public void setIncludeDeleted(boolean includeDeleted) {
		this.includeDeleted = includeDeleted;
	}
	
	public boolean isIncludeInProgress() {
		return includeInProgress;
	}

	public void setIncludeInProgress(boolean includeInProgress) {
		this.includeInProgress = includeInProgress;
	}

	public Set<String> getCameras() {
		return cameras;
	}
	
	public void setCameras(Set<String> cameras) {
		this.cameras = cameras == null ? new HashSet<>() : new HashSet<>(cameras);
	}
	
	public Set<String> getTags() {
		return tags;
	}
	
	public void setTags(Set<String> tags) {
		this.tags = tags == null ? new HashSet<>() : new HashSet<>(tags);
	}

}

