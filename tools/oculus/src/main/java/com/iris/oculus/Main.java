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
package com.iris.oculus;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.lang.Thread.UncaughtExceptionHandler;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.base.Preconditions;
import com.iris.bootstrap.Bootstrap;
import com.iris.bootstrap.ServiceLocator;
import com.iris.bootstrap.guice.GuiceServiceLocator;
import com.iris.client.ClientEvent;
import com.iris.client.IrisClientFactory;
import com.iris.client.session.Credentials;
import com.iris.client.session.HandoffTokenCredentials;
import com.iris.client.session.SessionTokenCredentials;
import com.iris.core.IrisApplicationModule;
import com.iris.io.json.gson.GsonModule;
import com.iris.oculus.modules.session.SessionController;
import com.iris.oculus.modules.session.ux.StatusBar;
import com.iris.oculus.modules.status.StatusController;
import com.iris.oculus.widget.NotificationPanel;
import com.iris.oculus.widget.ProgressDialog;
import com.iris.oculus.widget.ProgressDialog.Progress;

/**
 *
 */
public class Main {
   private static final Logger logger = LoggerFactory.getLogger(Main.class);
   private static final Logger sessionLog = LoggerFactory.getLogger("com.iris.oculus.session");
   private static final Logger messageLog = LoggerFactory.getLogger("com.iris.oculus.message");
   private static JFrame window;

   public static class Arguments {
      @Parameter(names={ "-p", "--prompt-login"}, description="Display login screen immediately (don't try auto-login)", arity=0, required=false)
      private boolean login = false;
      @Parameter(names={ "-s", "--skip-login"}, description="EXPERT -- Skip initial login but show Oculus as _if_ login had succeeded. This is deprecated but still works, but you don't want to use it", arity=0, required=false)
      private boolean skipLogin = false;
      @Parameter(names={"--credentials"}, description="EXPERT -- Login with a JSON string that happens to decode to the expected format. If you don't know what this message means, back away slowly and don't specify this option", arity=1, required=false)
      private String credentials = null;
      @Parameter(
            names = { "-c", "--config" },
            description = "Absolute path to the configuration file.  Optional",
            arity = 1)
      private String configFile;
   }

   public static void setLookAndFeel() {
      // TODO support other OS'
      try {
         System.setProperty("apple.laf.useScreenMenuBar", "true");
         System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Oculus");
      }
      catch(Exception e) {
         logger.debug("Unable to set Mac properties", e);
      }

      try {
         if ("Linux".equals(System.getProperty("os.name"))) {            
            // Nimbus works much better than the system look and feel or metal.
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
         }
         else {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
         }         
      }
      catch (Exception e) {
         logger.warn("Unable to use system look and feel", e);
      }
   }

   public static JFrame launch(Component contents) {
      return launch(contents, null);
   }

   public static JFrame launch(Component contents, JMenuBar menuBar) {
      window = new JFrame("Oculus");
      window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      if(menuBar != null) {
         window.setJMenuBar(menuBar);
      }
      window.setLayout(new BorderLayout());
      window.getContentPane().add(createNotificationPanel(), BorderLayout.NORTH);
      window.getContentPane().add(contents, BorderLayout.CENTER);
      window.getContentPane().add(createStatusPanel(), BorderLayout.SOUTH);

      int width = 1400;
      int height = 1000;
      Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
      window.setBounds((int)screen.getWidth()/2 - width/2, (int)screen.getHeight()/2 - height/2, width, height);
      window.setLocationByPlatform(true);

      window.setVisible(true);
      Oculus.setMainWindow(window);
      return window;
   }

   public static void onLogout() {
      window.dispose();
   }

   private static Component createNotificationPanel() {
      try {
         NotificationPanel notification = new NotificationPanel();
         ServiceLocator.getInstance(StatusController.class).addMessageListener((event) -> {
            switch(event.getLevel()) {
            case ERROR:
               notification.error(event.getMessage(), event.getAction());
               break;
            case WARN:
               notification.warn(event.getMessage(), event.getAction());
               break;
            case INFO:
               notification.info(event.getMessage(), event.getAction());
               break;
            default:
               notification.show(event.getMessage(), event.getAction());
            }
         });
         return notification;
      }
      catch(Exception e) {
         logger.warn("No status service defined, notifiation panel won't work");
         // empty
         return new JLabel();
      }
   }
   
   private static Component createStatusPanel() {
      StatusBar status = new StatusBar();
      status.bind(IrisClientFactory.getClient());
      return status.getComponent();
   }

   public static void onAuthenticated(Object o) {
      try {
         OculusViewBuilder builder = ServiceLocator.getInstance(OculusViewBuilder.class);
         launch(builder.create(), builder.createMenuBar());
         StatusController status = ServiceLocator.getInstance(StatusController.class);
         status
            .pingPlatformService()
            .onSuccess(Main::onPlatformPingSucceeded)
            .onFailure(Main::onPlatformPingFailed);
      }
      catch(Exception e) {
         logger.warn("Error launching process", e);
         System.exit(-1);
      }
   }

   public static void onPlatformPingSucceeded(ClientEvent response) {
      logger.debug("Platform services are online: {}", response);
   }

   public static void onPlatformPingFailed(Throwable cause) {
      Oculus.warn(
            "Unable to ping platform services, please verify that the service is online.\nDetails: " + cause.getMessage(),
            cause
      );
   }
   
   public static void bootstrap(Arguments arguments) throws Exception {
      //IrisObjectTypeAdapterFactory.install();
      Thread.setDefaultUncaughtExceptionHandler(new OculusUncaughtExceptionHandler());
      System.setProperty("sun.awt.exception.handler", OculusUncaughtExceptionHandler.class.getName());

      SwingUtilities.invokeAndWait(Main::setLookAndFeel);
      Progress p =
            ProgressDialog
               .builder()
               .withTitle("Oculus")
               .withMessage("Loading configuration...") // TODO proper splash screen
               .show();

      try {
         File oculusPropertyFile = arguments.configFile != null
               ? new File(arguments.configFile)
               : getDefaultConfigFile();

         Bootstrap.Builder builder = Bootstrap
               .builder()
               .withBootstrapModules(new IrisApplicationModule())
               .withModuleClasses(OculusModule.class, GsonModule.class);
         if(oculusPropertyFile.isFile()) {
            builder.withConfigFiles(oculusPropertyFile);
         }
         Bootstrap bootstrap = builder.build();
         ServiceLocator.init(GuiceServiceLocator.create(bootstrap.bootstrap()));
         String version = ServiceLocator.getNamedInstance(String.class, IrisApplicationModule.NAME_APPLICATION_VERSION);
         IrisClientFactory.getClient().setClientAgent(String.format("Oculus/%s (Java %s)", version, Runtime.class.getPackage().getImplementationVersion()));
         IrisClientFactory.getClient().setClientVersion(version);
         IrisClientFactory.getClient().addSessionListener((event) -> sessionLog.info("Session Event: {}", event));
         IrisClientFactory.getClient().addRequestListener((event) -> messageLog.info("Sent     : {}", event));
         IrisClientFactory.getClient().addMessageListener((event) -> messageLog.info("Received : {}", event));

         Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
               System.out.println("Shutting down...");
               ServiceLocator.destroy();
            }
         }));

         p.complete();
      }
      catch(Exception e) {
         p.complete();
         Oculus.showError("Unable to start app", e);
         System.exit(-1);
      }
   }

   private static File getDefaultConfigFile() {
      File defaultConfigPath = new File(System.getProperty("user.home"), ".oculus");
      return new File(defaultConfigPath, "oculus.properties");
   }
   
   public static class OculusUncaughtExceptionHandler implements UncaughtExceptionHandler {

      @Override
      public void uncaughtException(Thread t, Throwable e) {
         logger.warn("Uncaught Exception", e);
         Oculus.warn("Uncaught Exception", e); 
      }
      
   }

   @SuppressWarnings("unchecked")
   public static void main(String [] args) throws Exception {
      Arguments arguments = new Arguments();
      new JCommander(arguments, args);
      bootstrap(arguments);
      
      SessionController controller = ServiceLocator.getInstance(SessionController.class);
      SwingUtilities.invokeAndWait(() -> {
         if(arguments.credentials != null) {
            Credentials creds = null;
            String [] parts = arguments.credentials.split("\\|");
            if("session".equals(parts[0])) {
               Preconditions.checkArgument(parts.length == 3, "Expected session|{host}|{token}");
               SessionTokenCredentials sessionCreds = new SessionTokenCredentials();
               sessionCreds.setConnectionURL(parts[1]);
               sessionCreds.setToken(parts[2]);
               creds = sessionCreds;
            }
            else if("token".equals(parts[0])) {
               Preconditions.checkArgument(parts.length == 3, "Expected token|{host}|{token}");
               HandoffTokenCredentials sessionCreds = new HandoffTokenCredentials();
               sessionCreds.setConnectionURL(parts[1]);
               sessionCreds.setToken(parts[2]);
               creds = sessionCreds;
            }
            else {
               System.err.println("Oculus did not understand credentials of type: " + parts[0]);
               System.exit(-1);
            }
            controller
               .login(creds)
               .onSuccess(Main::onAuthenticated)
               .onFailure((e) -> System.exit(-1))
               ;
         }
         else if(arguments.login) {
            controller
               .promptLogin()
               .onSuccess(Main::onAuthenticated)
               .onFailure((e) -> System.exit(-1))
               ;
         }
         else if(!arguments.skipLogin) {
            controller
               .login()
               .onSuccess(Main::onAuthenticated)
               .onFailure((e) -> System.exit(-1))
               ;
         }
         else {
            Main.onAuthenticated(null);
         }
      });
   }
}

