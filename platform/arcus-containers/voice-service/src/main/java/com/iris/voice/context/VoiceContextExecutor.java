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
package com.iris.voice.context;

import java.util.Set;
import java.util.function.Consumer;

import com.iris.core.messaging.SingleThreadDispatcher;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.Capability;
import com.iris.messages.model.Model;
import com.iris.voice.VoiceProvider;
import com.iris.voice.exec.ResponseCompleter;
import com.iris.voice.proactive.ProactiveReporter;

public class VoiceContextExecutor implements Consumer<PlatformMessage> {

   private final VoiceContext context;
   private final ProactiveReporter proactiveReporter;
   private final ResponseCompleter responseCompleter;
   private final SingleThreadDispatcher<PlatformMessage> dispatcher;
   private final Set<VoiceProvider> providers;

   VoiceContextExecutor(VoiceContext context, ProactiveReporter proactiveReporter, ResponseCompleter responseCompleter, Set<VoiceProvider> providers, int queueDepth) {
      this.context = context;
      this.proactiveReporter = proactiveReporter;
      this.responseCompleter = responseCompleter;
      this.dispatcher = new SingleThreadDispatcher<PlatformMessage>(this, queueDepth);
      this.providers = providers;
   }

   public VoiceContext context() {
      return context;
   }

   public void onMessage(PlatformMessage msg) {
      dispatcher.dispatchOrQueue(msg);
   }

   @Override
   public void accept(PlatformMessage platformMessage) {
      Model deleted = null;
      if(Capability.EVENT_DELETED.equals(platformMessage.getMessageType())) {
         deleted = context.models().getModelByAddress(platformMessage.getSource());
      }

      context.models().update(platformMessage);
      for(VoiceProvider provider : providers) {
         if(provider.address().equals(platformMessage.getDestination())) {
            provider.onMessage(platformMessage);
            return;
         }
      }

      Model m = context.models().getModelByAddress(platformMessage.getSource());
      if(deleted != null) {
         m = deleted;
      }

      if(m != null) {
         responseCompleter.onMessage(m, platformMessage.getValue());
         proactiveReporter.onMessage(context, m, platformMessage.getValue());
      }
   }
}

