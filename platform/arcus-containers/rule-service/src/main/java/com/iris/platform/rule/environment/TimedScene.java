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
package com.iris.platform.rule.environment;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.commons.lang3.StringUtils.defaultString;

import com.codahale.metrics.Timer;
import com.iris.common.rule.action.Action;
import com.iris.common.scene.SceneContext;
import com.iris.common.scene.SceneImpl;
import com.iris.messages.address.Address;
import com.iris.messages.model.serv.SceneModel;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.metrics.tag.TaggingMetric;

/**
 * Wraps a SceneImpl to time how long it takes to fire, tagged by scene template.
 *
 */
public class TimedScene extends SceneImpl {

   private static final IrisMetricSet metrics = IrisMetrics.metrics("rule.service");
   private static final TaggingMetric<Timer> sceneFiredDurationTimer = metrics.taggingTimer("scene.fired.duration");

   private static final String SCENE_TEMPLATE_GENERIC = "generic";

   public TimedScene(Address address, SceneContext context, Action action) {
      super(address, context, action);
   }

   @Override
   public void execute() {
      long startTime = System.currentTimeMillis();
      try {
         super.execute();
      }
      finally {
         String sceneTemplate = defaultString(SceneModel.getTemplate(getContext().model()), SCENE_TEMPLATE_GENERIC);
         sceneFiredDurationTimer.tag("sceneTemplate", sceneTemplate).update(System.currentTimeMillis() - startTime, MILLISECONDS);
      }
   }

}
