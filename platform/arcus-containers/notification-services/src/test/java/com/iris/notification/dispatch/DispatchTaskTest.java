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
package com.iris.notification.dispatch;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.iris.notification.dispatch.DispatchTask;
import com.iris.notification.dispatch.Dispatcher;
import com.iris.platform.notification.Notification;

@RunWith(MockitoJUnitRunner.class)
public class DispatchTaskTest {
    @Mock
    private Dispatcher dispatcher;

    @Mock
    private Notification notification;

    @Test
    public void runShouldInvokeDispatcherDispatchMethod() throws Exception {
        new DispatchTask(notification, dispatcher).run();
        Mockito.verify(dispatcher).dispatch(notification);
    }

}

