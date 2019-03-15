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
package com.iris.oculus.modules.device.mockaction;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.iris.client.ClientRequest;
import com.iris.client.capability.DeviceMock;
import com.iris.client.event.Listener;
import com.iris.client.model.DeviceModel;
import com.iris.resource.Resource;
import com.iris.type.TypeUtil;
import com.iris.util.IrisCollections;

public class MockActionsNexus {
   // Delay between requests in seconds
   private final static int DEFAULT_DELAY = 2;
   private final static String ACTION_TYPE_ATTR = "attr";
   private final static String ACTION_TYPE_DELAY = "delay";
	
   private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private MockActions mockActions;
	
	public MockActionsNexus(Resource resource) {
		Gson gson = new Gson();
		try {
			String json = new BufferedReader(new InputStreamReader(resource.open())).lines().collect(Collectors.joining("\n"));
			mockActions = gson.fromJson(json, MockActions.class);
		}
		catch (Exception ex) {
		   System.out.println("JSON reading exception: " + ex.getMessage());
		   ex.printStackTrace();
			mockActions = null;
		}		
	}
	
	public void performAction(MockAction mockAction, DeviceModel model, Listener<Throwable> failure) {
	   int delay = 0;
	   for (Step step : mockAction.getSteps()) {
	      if (ACTION_TYPE_ATTR.equalsIgnoreCase(step.getType())) {
	         doAttrStep(step, delay, model, failure);
	      }
	      else if (ACTION_TYPE_DELAY.equalsIgnoreCase(step.getType())){
	         delay += (doDelayStep(step) - DEFAULT_DELAY);
	      }
	      delay += DEFAULT_DELAY;    
	   }
	}
	
	public Map<String,List<MockAction>> getAllMockActions() {
	   return mockActions.getActionMap();
	}
	
	public List<MockAction> getMockActions(String cap) {
		return mockActions != null ? mockActions.getActionMap().get(cap) : null;
	}
	
	private int doDelayStep(Step step) {
	   try {
	      return TypeUtil.INSTANCE.attemptCoerce(Integer.class, step.getValue());
	   }
	   catch (Throwable t) {
	      return DEFAULT_DELAY;
	   }
	}
	
	private void doAttrStep(Step step, int delay, DeviceModel model, Listener<Throwable> failure) {
	   scheduler.schedule(() -> {
   	   ClientRequest request = new DeviceMock.SetAttributesRequest();
         request.setAttribute(DeviceMock.SetAttributesRequest.ATTR_ATTRS, IrisCollections.<String,Object>immutableMap().put(step.getAttr(), getValue(step)).create());         
         model.request(request)
            .onFailure(failure)
            ;
	   }, delay, TimeUnit.SECONDS);
	}	
	
	private Object getValue(Step step) {
		if (Step.FORM_OF_INTEGER.equalsIgnoreCase(step.getForm())) {
			return TypeUtil.INSTANCE.attemptCoerce(Integer.class, step.getValue());
		}
		if (Step.FORM_OF_DOUBLE.equalsIgnoreCase(step.getForm())) {
			return TypeUtil.INSTANCE.attemptCoerce(Double.class, step.getValue());
		}
		if (Step.FORM_OF_STRING.equalsIgnoreCase(step.getForm())) {
			return TypeUtil.INSTANCE.attemptCoerce(String.class, step.getValue());
		}
		return step.getValue();
	}
}

