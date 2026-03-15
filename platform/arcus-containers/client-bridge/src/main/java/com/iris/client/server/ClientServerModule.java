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
package com.iris.client.server;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.bridge.bus.PlatformBusListener;
import com.iris.bridge.bus.PlatformBusService;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.BridgeConfigModule;
import com.iris.bridge.server.config.BridgeServerConfig;
import com.iris.bridge.server.config.RESTHandlerConfig;
import com.iris.bridge.server.http.RequestAuthorizer;
import com.iris.bridge.server.http.RequestHandler;
import com.iris.bridge.server.http.RequestMatcher;
import com.iris.bridge.server.http.Responder;
import com.iris.bridge.server.http.handlers.CheckPage;
import com.iris.bridge.server.http.handlers.IndexPage;
import com.iris.bridge.server.http.handlers.LoginPage;
import com.iris.bridge.server.http.handlers.RootRedirect;
import com.iris.bridge.server.http.handlers.SessionLogin;
import com.iris.bridge.server.http.handlers.SessionLogout;
import com.iris.bridge.server.http.handlers.WebAppStaticResources;
import com.iris.bridge.server.http.impl.auth.SessionAuth;
import com.iris.bridge.server.http.impl.matcher.WebSocketUpgradeMatcher;
import com.iris.bridge.server.http.impl.responder.SessionLoginResponder;
import com.iris.bridge.server.http.impl.responder.SessionLogoutResponder;
import com.iris.bridge.server.message.DeviceMessageHandler;
import com.iris.bridge.server.netty.Authenticator;
import com.iris.bridge.server.netty.WebSocketServerHandlerProvider;
import com.iris.bridge.server.session.DefaultSessionFactoryImpl;
import com.iris.bridge.server.session.DefaultSessionRegistryImpl;
import com.iris.bridge.server.session.SessionFactory;
import com.iris.bridge.server.session.SessionListener;
import com.iris.bridge.server.session.SessionRegistry;
import com.iris.bridge.server.shiro.ShiroAuthenticator;
import com.iris.bridge.server.shiro.ShiroModule;
import com.iris.bridge.server.ssl.BridgeServerTlsContext;
import com.iris.bridge.server.ssl.BridgeServerTlsContextImpl;
import com.iris.bridge.server.ssl.BridgeServerTrustManagerFactory;
import com.iris.bridge.server.ssl.NullTrustManagerFactoryImpl;
import com.iris.client.bounce.AppFallbackHandler;
import com.iris.client.bounce.AppLaunchHandler;
import com.iris.client.bounce.AppleAppSiteAssociationHandler;
import com.iris.client.bounce.WebLaunchHandler;
import com.iris.client.bounce.WebRunHandler;
import com.iris.client.eas.EasCodeManager;
import com.iris.client.nws.SameCodeManager;
import com.iris.client.security.SubscriberAuthorizationContextLoader;
import com.iris.client.server.rest.AcceptInvitationCreateLoginRESTHandler;
import com.iris.client.server.rest.ChangePasswordRESTHandler;
import com.iris.client.server.rest.ChangePinRESTHandler;
import com.iris.client.server.rest.ChangePinV2RESTHandler;
import com.iris.client.server.rest.CreateAccountRESTHandler;
import com.iris.client.server.rest.FindProductsRESTHandler;
import com.iris.client.server.rest.GetBrandsRESTHandler;
import com.iris.client.server.rest.GetCategoriesRESTHandler;
import com.iris.client.server.rest.GetInvitationRESTHandler;
import com.iris.client.server.rest.GetInvoiceRESTHandler;
import com.iris.client.server.rest.GetProductCatalogRESTHandler;
import com.iris.client.server.rest.GetProductRESTHandler;
import com.iris.client.server.rest.GetProductsByBrandRESTHandler;
import com.iris.client.server.rest.GetProductsByCategoryRESTHandler;
import com.iris.client.server.rest.GetProductsRESTHandler;
import com.iris.client.server.rest.GetSameCodeRESTHandler;
import com.iris.client.server.rest.ListEasCodesRESTHandler;
import com.iris.client.server.rest.ListSameCountiesRESTHandler;
import com.iris.client.server.rest.ListSameStatesRESTHandler;
import com.iris.client.server.rest.ListTimezonesRESTHandler;
import com.iris.client.server.rest.LoadLocalizedStringsRESTHandler;
import com.iris.client.server.rest.LockDeviceRESTHandler;
import com.iris.client.server.rest.RequestEmailVerificatonRESTHandler;
import com.iris.client.server.rest.ResetPasswordRESTHandler;
import com.iris.client.server.rest.SendPasswordResetRESTHandler;
import com.iris.client.server.rest.SessionLogRESTHandler;
import com.iris.client.server.rest.VerifyEmailRESTHandler;
import com.iris.client.server.rest.VerifyPinRESTHandler;
import com.iris.client.server.session.HandshakeSessionListener;
import com.iris.client.server.session.StopCameraPreviewsUploadSessionListener;
import com.iris.core.template.TemplateModule;
import com.iris.messages.MessageConstants;
import com.iris.messages.address.Address;
import com.iris.messages.services.PlatformConstants;
import com.iris.netty.bus.IrisNettyPlatformBusListener;
import com.iris.netty.bus.IrisNettyPlatformBusServiceImpl;
import com.iris.netty.security.IrisNettyAuthorizationContextLoader;
import com.iris.netty.security.IrisNettyNoopAuthorizationContextLoader;
import com.iris.netty.server.message.GetPreferencesHandler;
import com.iris.netty.server.message.IrisNettyMessageHandler;
import com.iris.netty.server.message.ResetPreferenceHandler;
import com.iris.netty.server.message.SetActivePlaceHandler;
import com.iris.netty.server.message.SetPreferencesHandler;
import com.iris.netty.server.netty.IrisNettyCORSChannelInitializer;
import com.iris.platform.location.TimezonesModule;
import com.iris.platform.notification.audit.CassandraAuditor;
import com.iris.platform.notification.audit.NotificationAuditor;
import com.iris.platform.subscription.IrisSubscriptionModule;
import com.iris.population.PlacePopulationCacheModule;
import com.iris.prodcat.ProductCatalogModule;
import com.iris.prodcat.ProductCatalogReloadListener;
import com.iris.resource.Resource;
import com.iris.resource.Resources;
import com.iris.util.ThreadPoolBuilder;
import com.iris.bootstrap.annotations.Modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

@Modules(include={
		ProductCatalogModule.class,
		TimezonesModule.class,
		PlacePopulationCacheModule.class
})
public class ClientServerModule extends AbstractIrisModule {
   private static final Logger log = LoggerFactory.getLogger(ClientServerModule.class);

   public static final String AUTHZ_LOADER_PROP = "authz.contextLoader";
   public static final String AUTHZ_LOADER_NONE = "none";
   public static final String DEBUG_PROP = "deploy.debug";
   public static final String TIMEZONE_PATH_PROP = "timezones.path";
   public static final String SAMECODE_PATH_PROP = "samecodes.path";
   public static final String EASCODE_PATH_PROP = "eascodes.path";
   
   /* 
    * I Want to be specific here because I have found cases where the NWS State Codes don't always
    * match globally recognized abbreviations for territories outside the United States
    */
   public static final String SAMECODE_STATEMAPPINGS_PATH_PROP = "samecodes.statename.mappings.path";
   
   @Inject(optional = true)
   @Named(AUTHZ_LOADER_PROP)
   private String algorithm = AUTHZ_LOADER_NONE;

   @Inject(optional = true)
   @Named(DEBUG_PROP)
   private boolean debug = false;

   @Inject(optional = true)
   @Named(SAMECODE_PATH_PROP)
   private String sameCodesPath = "conf/same-codes.csv";
   
   @Inject(optional = true)
   @Named(SAMECODE_STATEMAPPINGS_PATH_PROP)
   private String sameCodeStateMappingsPath = "conf/samecode-statename-mappings.csv";

   @Inject(optional = true)
   @Named(EASCODE_PATH_PROP)
   private String easCodesPath = "conf/eas-event-codes.csv";
   
	@Inject(optional=true)
	@Named("client.background.threads")
	private int threads = 100;

	@Inject(optional=true)
	@Named("client.background.threadKeepAliveMs")
	private long threadKeepAliveMs = 10000;

   @Inject(optional=true)
   @Named("client.disabled.handlers")
   private String disabledHandlersConfig = "";

   private Set<String> disabledHandlers = Collections.emptySet();
   private ExecutorService executor;
   
   @Inject
   public ClientServerModule(BridgeConfigModule bridge, ShiroModule shiro, TemplateModule template, IrisSubscriptionModule subscription) {
   	this.executor = 
   			new ThreadPoolBuilder()
   				.withMaxPoolSize(threads)
   				.withKeepAliveMs(threadKeepAliveMs)
   				.withNameFormat("async-request-%d")
   				.withMetrics("client.background")
   				.build();
   }

   private void bindHandler(Multibinder<RequestHandler> rhBindings, Class<? extends RequestHandler> handler) {
      if (disabledHandlers.contains(handler.getSimpleName())) {
         log.warn("Handler {} is disabled via configuration", handler.getSimpleName());
         return;
      }
      rhBindings.addBinding().to(handler);
   }

   @Override
   protected void configure() {
      if (disabledHandlersConfig != null && !disabledHandlersConfig.isEmpty()) {
         disabledHandlers = Arrays.stream(disabledHandlersConfig.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());
      }
      bind(BridgeServerConfig.class);
      bind(BridgeServerTlsContext.class).to(BridgeServerTlsContextImpl.class);
      bind(BridgeServerTrustManagerFactory.class).to(NullTrustManagerFactoryImpl.class);
      bind(PlatformBusService.class).to(IrisNettyPlatformBusServiceImpl.class).asEagerSingleton();
      bindSetOf(PlatformBusListener.class)
         .addBinding()
         .to(IrisNettyPlatformBusListener.class);      
      bind(new TypeLiteral<DeviceMessageHandler<String>>(){}).to(IrisNettyMessageHandler.class);
      bind(Authenticator.class).to(ShiroAuthenticator.class);
      bind(SessionFactory.class).to(DefaultSessionFactoryImpl.class);
      bind(SessionRegistry.class).to(DefaultSessionRegistryImpl.class);      
      bind(NotificationAuditor.class).to(CassandraAuditor.class);

      if(algorithm.equalsIgnoreCase(AUTHZ_LOADER_NONE)) {
         bind(IrisNettyAuthorizationContextLoader.class).to(IrisNettyNoopAuthorizationContextLoader.class);
      } else {
         bind(IrisNettyAuthorizationContextLoader.class).to(SubscriberAuthorizationContextLoader.class);
      }

      
      Multibinder<SessionListener> slBindings = Multibinder.newSetBinder(binder(), SessionListener.class);
      slBindings.addBinding().to(HandshakeSessionListener.class);
      slBindings.addBinding().to(StopCameraPreviewsUploadSessionListener.class);

      bind(ChannelInboundHandler.class).toProvider(WebSocketServerHandlerProvider.class);
      bind(new TypeLiteral<ChannelInitializer<SocketChannel>>(){})
         .to(IrisNettyCORSChannelInitializer.class);

      bind(RequestMatcher.class).annotatedWith(Names.named("WebSocketUpgradeMatcher")).to(WebSocketUpgradeMatcher.class);
      bind(RequestAuthorizer.class).annotatedWith(Names.named("SessionAuthorizer")).to(SessionAuth.class);

      //Bind Login and Logout
      bind(Responder.class).annotatedWith(Names.named("SessionLogin")).to(SessionLoginResponder.class);
      bind(Responder.class).annotatedWith(Names.named("SessionLogout")).to(SessionLogoutResponder.class);
      
      // Bind Http Handlers
      Multibinder<RequestHandler> rhBindings = Multibinder.newSetBinder(binder(), RequestHandler.class);
      rhBindings.addBinding().to(SessionLogin.class);
      rhBindings.addBinding().to(RootRedirect.class);
      rhBindings.addBinding().to(CheckPage.class);
      rhBindings.addBinding().to(IndexPage.class);
      rhBindings.addBinding().to(LoginPage.class);
      
      /* Bounce handlers */
      bindHandler(rhBindings, AppleAppSiteAssociationHandler.class);
      bindHandler(rhBindings, AppFallbackHandler.class);
      bindHandler(rhBindings, AppLaunchHandler.class);
      bindHandler(rhBindings, WebLaunchHandler.class);
      bindHandler(rhBindings, WebRunHandler.class);

      /* ProductCatalog RestHandlers */
      bindHandler(rhBindings, GetProductCatalogRESTHandler.class);
      bindHandler(rhBindings, GetProductsRESTHandler.class);
      bindHandler(rhBindings, FindProductsRESTHandler.class);
      bindHandler(rhBindings, GetBrandsRESTHandler.class);
      bindHandler(rhBindings, GetCategoriesRESTHandler.class);
      bindHandler(rhBindings, GetProductRESTHandler.class);
      bindHandler(rhBindings, GetProductsByBrandRESTHandler.class);
      bindHandler(rhBindings, GetProductsByCategoryRESTHandler.class);

      // NWS SAME and EAS Code REST Handlers
      bindHandler(rhBindings, GetSameCodeRESTHandler.class);
      bindHandler(rhBindings, ListSameCountiesRESTHandler.class);
      bindHandler(rhBindings, ListSameStatesRESTHandler.class);
      bindHandler(rhBindings, ListEasCodesRESTHandler.class);

      /* Other RestHandlers */
      bindHandler(rhBindings, ChangePinRESTHandler.class);
      bindHandler(rhBindings, ChangePinV2RESTHandler.class);
      bindHandler(rhBindings, VerifyPinRESTHandler.class);
      bindHandler(rhBindings, CreateAccountRESTHandler.class);
      bindHandler(rhBindings, ChangePasswordRESTHandler.class);
      bindHandler(rhBindings, LoadLocalizedStringsRESTHandler.class);
      bindHandler(rhBindings, ResetPasswordRESTHandler.class);
      bindHandler(rhBindings, SendPasswordResetRESTHandler.class);
      bindHandler(rhBindings, SessionLogout.class);
      bindHandler(rhBindings, SessionLogRESTHandler.class);
      bindHandler(rhBindings, ListTimezonesRESTHandler.class);
      bindHandler(rhBindings, AcceptInvitationCreateLoginRESTHandler.class);
      bindHandler(rhBindings, GetInvitationRESTHandler.class);
      bindHandler(rhBindings, GetInvoiceRESTHandler.class);
      bindHandler(rhBindings, LockDeviceRESTHandler.class);
      bindHandler(rhBindings, RequestEmailVerificatonRESTHandler.class);
      bindHandler(rhBindings, VerifyEmailRESTHandler.class);

      /* Static resource handler */
      // should be bound last because its matcher is super-greedy 
      rhBindings.addBinding().to(WebAppStaticResources.class);
   }
   
   @Provides
   @Singleton
   public RESTHandlerConfig provideRestHandlerConfig() {
	   RESTHandlerConfig restHandlerConfig = new RESTHandlerConfig();
	   restHandlerConfig.setSendChunked(false);
	   return restHandlerConfig;
   }
   
   @Provides
   @Singleton
   @Named("UseChunkedSender")
   public RESTHandlerConfig provideChunkedRestHandlerConfig() {
	   RESTHandlerConfig restHandlerConfig = new RESTHandlerConfig();
	   restHandlerConfig.setSendChunked(true);
	   return restHandlerConfig;
   }
   
   @Provides
   @Singleton
   @Named(ProductCatalogReloadListener.GENERIC_MESSAGE_BUS_ACTOR_ADDRESS)
   public Address provideMessageBusActorAddress() {
      return Address.fromString(MessageConstants.SERVICE + ":" + PlatformConstants.SERVICE_CLIENTBRIDGE + ":");
   }
   
   @Provides @Singleton
   public BridgeMetrics provideBridgeMetrics() {
      return new BridgeMetrics("client");
   }
   
   @Provides
   @Singleton
   public SameCodeManager provideSameCodeManager() throws IllegalArgumentException, URISyntaxException {
      Resource sameCodesDirectory = Resources.getResource(sameCodesPath);
      Resource sameCodesStateMappingsDirectory = Resources.getResource(sameCodeStateMappingsPath);
      return new SameCodeManager(sameCodesDirectory, sameCodesStateMappingsDirectory);
   }   
   
   @Provides
   @Singleton
   public EasCodeManager provideEasCodeManager() throws IllegalArgumentException, URISyntaxException {
      Resource easCodesDirectory = Resources.getResource(easCodesPath);
      return new EasCodeManager(easCodesDirectory);
   }
   
   @Provides @Singleton @Named(GetInvoiceRESTHandler.NAME_EXECUTOR)
   public Executor getInvoiceExecutor() {
   	return executor;
   }
   
   
   @Provides @Singleton @Named(SetActivePlaceHandler.NAME_EXECUTOR)
   public Executor setActivePlaceExecutor() {
   	return executor;
   }
   
   @Provides @Singleton @Named(GetPreferencesHandler.NAME_EXECUTOR)
   public Executor getPreferencesExecutor() {
   	return executor;
   }
   
   @Provides @Singleton @Named(SetPreferencesHandler.NAME_EXECUTOR)
   public Executor setPreferencesExecutor() {
   	return executor;
   }
   
   @Provides @Singleton @Named(ResetPreferenceHandler.NAME_EXECUTOR)
   public Executor resetPreferenceExecutor() {
   	return executor;
   }
}

