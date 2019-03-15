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

import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.platform.history.appender.subsys.CareSubsystemEventsAppender;
import com.iris.platform.history.appender.subsys.PlaceMonitorSubsystemAppender;
import com.iris.platform.history.appender.subsys.SecuritySubsystemEventsAppender;
import com.iris.platform.history.appender.subsys.WaterSubsystemEventsAppender;

public class TestSubsystemAppenderModule extends AbstractIrisModule {

	@Override
   protected void configure() {
	   bind(SecuritySubsystemEventsAppender.class);
	   bind(CareSubsystemEventsAppender.class);
      bind(WaterSubsystemEventsAppender.class);
      bind(PlaceMonitorSubsystemAppender.class);
   }

}

