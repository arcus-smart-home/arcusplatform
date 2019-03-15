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
package com.iris.platform.history.appender;

import java.util.List;

import com.iris.messages.PlatformMessage;
import com.iris.platform.history.HistoryAppenderDAO;
import com.iris.platform.history.HistoryLogEntry;
import com.iris.platform.history.appender.matcher.MatchResults;

public abstract class AnnotatedDeviceValueChangeAppender extends AnnotatedAppender {
	
	protected AnnotatedDeviceValueChangeAppender(HistoryAppenderDAO appender, ObjectNameCache cache) {
   	super(appender, cache);
   }

	@Override
   protected List<HistoryLogEntry> translate(PlatformMessage message, MessageContext context, MatchResults matchResults) {
		context.setDeviceId(getIdFromSource(message));
      context.setDeviceName(getDeviceNameFromSource(message));
	   return super.translate(message, context, matchResults);
   }
}

