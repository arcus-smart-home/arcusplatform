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
package com.iris.voice.proactive;

import java.util.Map;
import java.util.concurrent.ExecutorService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.messages.MessageBody;
import com.iris.messages.model.Model;
import com.iris.voice.context.VoiceContext;

@Singleton
public class ProactiveReporter {

   public static final String EXECUTOR_NAME = "VoiceService#proactiveReporterExecutor";

   private final ExecutorService executor;
   private final Map<String, ProactiveReportHandler> handlers;

   @Inject
   public ProactiveReporter(
      @Named(EXECUTOR_NAME) ExecutorService executor,
      Map<String, ProactiveReportHandler> handlers
   ) {
      this.executor = executor;
      this.handlers = handlers;
   }

   public void onMessage(VoiceContext context, Model m, MessageBody body) {
      if(!context.hasAssistants()) {
         return;
      }
      context.getAssistants().forEach(assistant -> {
         ProactiveReportHandler handler = handlers.get(assistant);
         if(handler != null) {
            executor.execute(() -> {
               if(handler.isInterestedIn(context, m, body)) {
                  handler.report(context, m, body);
               }
            });
         }
      });

   }

}

