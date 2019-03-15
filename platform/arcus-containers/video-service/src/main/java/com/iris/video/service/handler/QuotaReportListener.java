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

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.messages.PlatformMessage;
import com.iris.messages.listener.annotation.OnMessage;
import com.iris.messages.service.VideoService;
import com.iris.messages.service.VideoService.QuotaReportEvent;
import com.iris.video.cql.PlaceQuota.Unit;
import com.iris.video.service.quota.QuotaManager;

@Singleton
public class QuotaReportListener {
	private static final Logger logger = LoggerFactory.getLogger(QuotaReportListener.class);
	private final QuotaManager quotaManager;
	
	@Inject
	public QuotaReportListener(QuotaManager quotaManager) {
		this.quotaManager = quotaManager;
	}
	
	@OnMessage(types = QuotaReportEvent.NAME, from = "SERV:" + VideoService.NAMESPACE + ":")
	public void onQuotaReport(
			PlatformMessage message, 
			@Named(QuotaReportEvent.ATTR_USED) long used,
			@Named(QuotaReportEvent.ATTR_UNIT) String unitStr,
			@Named(QuotaReportEvent.ATTR_FAVORITE) boolean favorite
	) {
		if(message.getPlaceId() == null) {
			return;
		}
		if(!favorite) {
			logger.warn("Non favorite video clips byte quota report is no longer supported.  Ignore event from source [{}]", message.getSource());
			return;
		}
		UUID placeId = UUID.fromString(message.getPlaceId());
		long timestamp = message.getTimestamp().getTime();
		Unit unit = Unit.Bytes;
		if(QuotaReportEvent.UNIT_NUMBER.equals(unitStr)) {
			unit = Unit.Number;
		}
		quotaManager.updateQuotaIf(placeId, timestamp, used, unit, favorite);
	}
}

