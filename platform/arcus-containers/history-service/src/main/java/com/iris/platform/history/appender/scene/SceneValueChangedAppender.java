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
package com.iris.platform.history.appender.scene;

import java.util.List;

import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.SceneCapability;
import com.iris.platform.history.HistoryAppenderDAO;
import com.iris.platform.history.HistoryLogEntry;
import com.iris.platform.history.appender.BaseHistoryAppender;
import com.iris.platform.history.appender.MessageContext;
import com.iris.platform.history.appender.ObjectNameCache;
import com.iris.platform.history.appender.matcher.MatchResults;
import com.iris.platform.history.appender.matcher.Matcher;
import com.iris.platform.history.appender.translator.Translator;

/**
 * 
 */
public abstract class SceneValueChangedAppender extends BaseHistoryAppender {

   protected Matcher matcher;
   protected Translator translator;
   
   protected SceneValueChangedAppender(HistoryAppenderDAO appender, ObjectNameCache cache, Matcher matcher, Translator translator) {
   	super(appender, cache);
   	this.matcher = matcher;
   	this.translator = translator;
   }
   
   protected MatchResults matches(MessageBody value) {
   	return matcher.matches(value);
   }
   
   /* (non-Javadoc)
    * @see com.iris.platform.history.appender.BaseHistoryAppender#matches(com.iris.messages.PlatformMessage)
    */
   @Override
   protected MatchResults matches(PlatformMessage message) {
	      boolean match = 
	              (SceneCapability.NAMESPACE.equals(message.getSource().getGroup()) &&
	              Capability.EVENT_VALUE_CHANGE.equals(message.getMessageType()));
		if (!match) {
			return MatchResults.FALSE;
		} else {
			return matcher.matches(message.getValue());
		}
   }
   
   @Override
   protected List<HistoryLogEntry> translate(PlatformMessage message, MessageContext context, MatchResults matchResults) {

      return doTranslate(message, context, matchResults);
   }
   
   protected List<HistoryLogEntry> doTranslate(PlatformMessage message, MessageContext context, MatchResults matchResults) {
   	return translator.generateEntries( message,  context,  matchResults);
   }
   
}

