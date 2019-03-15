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
package com.iris.voice.alexa;

import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.messages.service.VoiceService;
import com.iris.voice.VoiceProvider;
import com.iris.voice.alexa.reporting.AlexaProactiveReportHandler;
import com.iris.voice.proactive.ProactiveReportHandler;

public class AlexaModule extends AbstractIrisModule {

   private static final TypeLiteral<String> KEY_LITERAL = new TypeLiteral<String>(){};
   private static final TypeLiteral<ProactiveReportHandler> VALUE_LITERAL = new TypeLiteral<ProactiveReportHandler>(){};

   @Override
   protected void configure() {
      MapBinder<String, ProactiveReportHandler> handlers = MapBinder.newMapBinder(binder(), KEY_LITERAL, VALUE_LITERAL);
      handlers.addBinding(VoiceService.StartPlaceRequest.ASSISTANT_ALEXA).to(AlexaProactiveReportHandler.class);

      Multibinder<VoiceProvider> providerMultibinder = bindSetOf(VoiceProvider.class);
      providerMultibinder.addBinding().to(AlexaService.class);
   }
}

