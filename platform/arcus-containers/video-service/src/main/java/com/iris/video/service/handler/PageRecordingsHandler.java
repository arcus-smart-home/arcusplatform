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
package com.iris.video.service.handler;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.listener.annotation.Request;
import com.iris.messages.service.VideoService;
import com.iris.messages.service.VideoService.PageRecordingsRequest;
import com.iris.messages.service.VideoService.PageRecordingsResponse;
import com.iris.platform.PagedResults;
import com.iris.video.VideoQuery;
import com.iris.video.service.VideoServiceMetrics;
import com.iris.video.service.VideoServiceUtil;
import com.iris.video.service.dao.VideoServiceDao;

public class PageRecordingsHandler {
	public static final int DFLT_LIMIT = 100;
	public static final boolean DFLT_ALL = false;
	public static final boolean DFLT_INPROGRESS = true;
	public static final String DFLT_TYPE = PageRecordingsRequest.TYPE_ANY;
	public static final Set<String> DFLT_CAMERAS = ImmutableSet.of();
	public static final Set<String> DFLT_TAGS = ImmutableSet.of();

	private final VideoServiceDao videoDao;
	
	@Inject
	public PageRecordingsHandler(VideoServiceDao videoDao) {
		this.videoDao = videoDao;
	}
	
	@Request(value=VideoService.PageRecordingsRequest.NAME, service=true)
	public MessageBody pageRecordings(
			PlatformMessage message,
			MessageBody request,
			@Named(PageRecordingsRequest.ATTR_TOKEN)       String token,
			@Named(PageRecordingsRequest.ATTR_LIMIT)       Integer limit,
			@Named(PageRecordingsRequest.ATTR_LATEST)      Date latest,
			@Named(PageRecordingsRequest.ATTR_EARLIEST)    Date earliest,
			@Named(PageRecordingsRequest.ATTR_ALL)         Boolean all,
			@Named(PageRecordingsRequest.ATTR_INPROGRESS)  Boolean inprogress,
			@Named(PageRecordingsRequest.ATTR_TYPE)        String type,
			@Named(PageRecordingsRequest.ATTR_CAMERAS)     Set<String> cameras,
			@Named(PageRecordingsRequest.ATTR_TAGS)        Set<String> tags
	) throws Exception {
		UUID placeId = VideoServiceUtil.getPlaceId(message, request);
		
		VideoQuery query = new VideoQuery();
		query.setPlaceId(placeId);
		query.setToken(token);
		query.setLimit(Optional.ofNullable(limit).orElse(DFLT_LIMIT));
		query.setLatest(latest);
		query.setEarliest(earliest);
		query.setListDeleted(Optional.ofNullable(all).orElse(DFLT_ALL));
		query.setListInProgress(Optional.ofNullable(inprogress).orElse(DFLT_INPROGRESS));
		query.setRecordingType(Optional.ofNullable(type).orElse(DFLT_TYPE));
		query.setCameras(
				Optional
					.ofNullable(cameras)
					.orElse(DFLT_CAMERAS)
					.stream()
					.map(this::toCameraId)
					.collect(Collectors.toSet())
		);
		query.setTags(Optional.ofNullable(tags).orElse(DFLT_TAGS));
		
		PagedResults<Map<String, Object>> recordings = videoDao.listPagedRecordingsForPlace(query);
		VideoServiceMetrics.PAGE_RECORDINGS_NUM.update(recordings.getResults().size());
		return
				PageRecordingsResponse
					.builder()
					.withNextToken(recordings.getNextToken())
					.withRecordings(recordings.getResults())
					.build();
	}
	
	private String toCameraId(String address) {
		if(address.startsWith(MessageConstants.DRIVER)) {
			return Address.fromString(address).getId().toString();
		}
		else {
			return address;
		}
	}
}

