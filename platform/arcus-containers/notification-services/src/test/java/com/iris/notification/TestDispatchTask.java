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
package com.iris.notification;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.iris.notification.dispatch.DispatchTask;
import com.iris.notification.dispatch.Dispatcher;
import com.iris.platform.notification.Notification;

@RunWith(MockitoJUnitRunner.class)
public class TestDispatchTask {
	@Mock
	private Dispatcher dispatcher;

	@Mock
	private Notification notification;
	@InjectMocks
	private DispatchTask dispatchTask;

	@Test
	public void testRun() throws Exception {		
		dispatchTask = new DispatchTask(notification, dispatcher);		
		dispatchTask.run();
		Mockito.verify(dispatcher).dispatch(notification);
	}

}

