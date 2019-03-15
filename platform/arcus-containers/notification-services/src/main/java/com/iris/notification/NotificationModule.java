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
package com.iris.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.core.platform.PlatformService;
import com.iris.notification.dispatch.Dispatcher;
import com.iris.notification.dispatch.NotificationDispatcher;
import com.iris.notification.message.NotificationMessageRenderer;
import com.iris.notification.message.TemplateMessageRenderer;
import com.iris.notification.provider.ApnsProvider;
import com.iris.notification.provider.EmailProvider;
import com.iris.notification.provider.GCMProvider;
import com.iris.notification.provider.IVRProvider;
import com.iris.notification.provider.LogProvider;
import com.iris.notification.provider.MapNotificationProviderRegistry;
import com.iris.notification.provider.NotificationProvider;
import com.iris.notification.provider.NotificationProviderRegistry;
import com.iris.notification.provider.WebhookProvider;
import com.iris.notification.provider.apns.ApnsSender;
import com.iris.notification.provider.apns.NoopApnsSender;
import com.iris.notification.provider.apns.PushyApnsSender;
import com.iris.notification.provider.gcm.GcmSender;
import com.iris.notification.provider.gcm.NoopGcmSender;
import com.iris.notification.provider.gcm.SmackGcmSender;
import com.iris.notification.retry.BackoffRetryManager;
import com.iris.notification.retry.RetryManager;
import com.iris.notification.retry.RetryProcessor;
import com.iris.notification.retry.ScheduledRetryProcessor;
import com.iris.notification.upstream.IrisUpstreamNotificationResponder;
import com.iris.notification.upstream.UpstreamNotificationResponder;
import com.iris.platform.notification.audit.CassandraAuditor;
import com.iris.platform.notification.audit.NotificationAuditor;

public class NotificationModule extends AbstractIrisModule {
   private static final Logger logger = LoggerFactory.getLogger(NotificationModule.class);

   @Inject(optional=true)
   @Named("notificationservice.sender.apns")
   private String apnsSender = "default";

   @Inject(optional=true)
   @Named("notificationservice.sender.gcm")
   private String gcmSender = "default";

    @Override
    protected void configure() {
        bind(Dispatcher.class).to(NotificationDispatcher.class);
        bind(NotificationAuditor.class).to(CassandraAuditor.class);
        bind(RetryManager.class).to(BackoffRetryManager.class);
        bind(RetryProcessor.class).to(ScheduledRetryProcessor.class);
        bind(NotificationMessageRenderer.class).to(TemplateMessageRenderer.class);
        bind(NotificationProviderRegistry.class).to(MapNotificationProviderRegistry.class);

        switch (apnsSender) {
        default:
           logger.warn("unknown apns sender implementation {}: using default instead");
           // fall through
           
        case "default":
        case "pushy":
           logger.info("using pushy apns sender");
           bind(ApnsSender.class).to(PushyApnsSender.class);
           break;

        case "noop":
           logger.warn("using noop apns sender");
           bind(ApnsSender.class).to(NoopApnsSender.class);
           break;
        }

        switch (gcmSender) {
        default:
           logger.warn("unknown gcm sender implementation {}: using default instead");
           // fall through
           
        case "default":
        case "smack":
           logger.info("using smack gcm sender");
           bind(GcmSender.class).to(SmackGcmSender.class);
           break;

        case "noop":
           logger.warn("using noop gcm sender");
           bind(GcmSender.class).to(NoopGcmSender.class);
           break;
        }

        bind(UpstreamNotificationResponder.class).to(IrisUpstreamNotificationResponder.class);
        bind(NotificationService.class).asEagerSingleton();

        MapBinder<String, NotificationProvider> registryBinder = MapBinder.newMapBinder(binder(), String.class, NotificationProvider.class);
        registryBinder.addBinding("LOG").to(LogProvider.class);
        registryBinder.addBinding("GCM").to(GCMProvider.class);
        registryBinder.addBinding("APNS").to(ApnsProvider.class);
        registryBinder.addBinding("EMAIL").to(EmailProvider.class);
        registryBinder.addBinding("IVR").to(IVRProvider.class);
        registryBinder.addBinding("WEBHOOK").to(WebhookProvider.class);
    }
}

