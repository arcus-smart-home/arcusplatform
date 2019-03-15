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
package com.iris.platform.history.appender.scene;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.Utils;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.SceneCapability;
import com.iris.platform.history.HistoryAppenderDAO;
import com.iris.platform.history.HistoryLogEntry;
import com.iris.platform.history.appender.MessageContext;
import com.iris.platform.history.appender.ObjectNameCache;
import com.iris.platform.history.appender.matcher.AnyValueChangeMatcher;
import com.iris.platform.history.appender.matcher.MatchResults;
import com.iris.platform.history.appender.matcher.Matcher;
import com.iris.platform.history.appender.translator.EntryTemplate;
import com.iris.platform.history.appender.translator.Translator;

@Singleton
public class SceneFiredAppender extends SceneValueChangedAppender {
	private static final String TEMPLATE_SCENE_RUN = "scene.run";

	/*
	 * When the scene is fired a message of SceneCapability.ATTR_LASTFIRETIME is
	 * sent out
	 */
	private static final Matcher matcher = new AnyValueChangeMatcher(SceneCapability.ATTR_LASTFIRETIME);

	@Inject
	protected SceneFiredAppender(HistoryAppenderDAO appender, ObjectNameCache cache) {
		super(appender, cache, matcher, new SceneFiredTranslator(cache));
	}

	@Override
	protected List<HistoryLogEntry> translate(PlatformMessage message, MessageContext context,
			MatchResults matchResults) {
		List<HistoryLogEntry> entries = translator.generateEntries(message, context, matchResults);

		return entries;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.iris.platform.history.appender.BaseHistoryAppender#matches(com.iris.
	 * messages.PlatformMessage)
	 */
	@Override
	protected MatchResults matches(PlatformMessage message) {
		boolean match = SceneCapability.NAMESPACE.equals(message.getSource().getGroup());
		if (!match) {
			return MatchResults.FALSE;
		} else {
			return matcher.matches(message.getValue());
		}
	}

	private static class SceneFiredTranslator extends Translator {

		private final ObjectNameCache cache;

		public SceneFiredTranslator(ObjectNameCache cache) {
			super();
			this.cache = cache;
		}

		@Override
		protected EntryTemplate selectTemplate(MatchResults matchResults) {
			return new EntryTemplate(TEMPLATE_SCENE_RUN, true);
		}

		@Override
		public List<String> generateValues(PlatformMessage message, MessageContext context, MatchResults matchResults) {
			List<String> values = new ArrayList<String>();

			String sceneName = cache.getSceneName(message.getSource());
			Utils.assertNotNull(sceneName, "Scene Name is Required");
			values.add(sceneName.toString());

			return values;
		}
	}
}

