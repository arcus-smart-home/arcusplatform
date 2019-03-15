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
package com.iris.oculus.modules.session;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.iris.messages.service.AccountService;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.client.IrisClient;
import com.iris.client.IrisClientFactory;
import com.iris.client.connection.ConnectionEvent;
import com.iris.client.connection.ConnectionState;
import com.iris.client.event.ClientFuture;
import com.iris.client.event.Futures;
import com.iris.client.event.Listener;
import com.iris.client.event.ListenerList;
import com.iris.client.event.ListenerRegistration;
import com.iris.client.event.SettableClientFuture;
import com.iris.client.impl.netty.NettyIrisClientFactory;
import com.iris.client.service.AccountService.CreateAccountRequest;
import com.iris.client.service.PersonService;
import com.iris.client.service.PersonService.ChangePasswordResponse;
import com.iris.client.service.SessionService;
import com.iris.client.session.Credentials;
import com.iris.client.session.SessionActivePlaceSetEvent;
import com.iris.client.session.SessionInfo;
import com.iris.client.session.SessionInfo.PlaceDescriptor;
import com.iris.client.session.SessionPlaceClearedEvent;
import com.iris.client.session.SessionTokenCredentials;
import com.iris.client.session.UsernameAndPasswordCredentials;
import com.iris.io.json.JSON;
import com.iris.oculus.Oculus;
import com.iris.oculus.Oculus.ProgressOptions;
import com.iris.oculus.modules.place.dialog.SelectNewPlacePrompt;
import com.iris.oculus.modules.session.event.PlaceChangedEvent;
import com.iris.oculus.modules.session.event.SessionAuthenticatedEvent;
import com.iris.oculus.modules.session.event.SessionEvent;
import com.iris.oculus.modules.session.event.SessionExpiredEvent;
import com.iris.oculus.modules.session.ux.ChangePasswordDialog;
import com.iris.oculus.modules.session.ux.InvitationWizard;
import com.iris.oculus.modules.session.ux.LoginDialog;
import com.iris.oculus.modules.session.ux.ResetPasswordWizard;
import com.iris.oculus.util.Actions;
import com.iris.oculus.widget.ProgressDialog;
import com.iris.oculus.widget.ProgressDialog.Progress;
import com.iris.util.TypeMarker;

/**
 *
 */
@Singleton
public class SessionController {
   private static final Logger logger = LoggerFactory.getLogger(SessionController.class);
   private static final String PREF_AUTH_TOKEN = "auth.token";
   private static final String PREF_RECENT_SESSIONS = "auth.recentSessions";
   private static final TypeMarker<List<OculusSession>> TYPE_RECENT_SESSIONS = new TypeMarker<List<OculusSession>>() {};

   private final IrisClient client;
   private ListenerList<SessionEvent> sessionListeners = new ListenerList<SessionEvent>();
   private final AtomicReference<OculusSession> sessionInfoRef = new AtomicReference<>();
   private final AtomicReference<SettableClientFuture<Void>> loginPromptRef = new AtomicReference<>();

   private final Action changePassword = Actions.build("Change My Password", this, SessionController::promptChangePassword);

   @SuppressWarnings("serial")
   private final Action reconnect = new AbstractAction("Reconnect") {
      @Override
      public void actionPerformed(ActionEvent e) {
         if(isSessionActive()) {
            try {
               client.close();
            }
            catch (IOException ex) {
               logger.warn("Error closing socket", ex);
            }
            SwingUtilities.invokeLater(() -> login());
         }
         else {
            login();
         }
      }
   };

   private final Action launchBrowser  = Actions.build("Launch Browser", this::promptWebLaunch);
   private final Action logoutAndLogin = Actions.build("Login as a New User", (e) -> logout(false));
   private final Action logoutAndQuit  = Actions.build("Logout & Quit", (e) -> logout(true));
   private final Action quit           = Actions.build("Quit", this, SessionController::quit);

   @Inject
   public SessionController(IrisClient client) {
      this.client = client;
      this.client.addSessionListener((event) -> onSessionEvent(event));
   }

   public List<OculusSession> listRecentSessions() {
      return JSON.fromJson(Oculus.getPreference(PREF_RECENT_SESSIONS, "null"), TYPE_RECENT_SESSIONS);
   }
   
   public void rememberSession(OculusSession session) {
      try {
         List<OculusSession> sessions = listRecentSessions();
         removeSession(sessions, session);
         sessions.add(session);
         rememberSessions(sessions);
      }
      catch(Exception e) {
         logger.warn("Unable to remember session", e);
      }
   }

   public void forgetSession(OculusSession session) {
      List<OculusSession> sessions = listRecentSessions();
      removeSession(sessions, session);
      rememberSessions(sessions);
   }

   public Action getChangePasswordAction() {
      return changePassword;
   }

   public Action getReconnectAction() {
      return reconnect;
   }

   public Action getLaunchBrowser() {
      return launchBrowser;
   }

   public Action getLogoutAndLoginAction() {
      return logoutAndLogin;
   }

   public Action getLogoutAndQuitAction() {
      return logoutAndQuit;
   }

   public Action getQuitAction() {
      return quit;
   }

   public boolean isSessionActive() {
      return sessionInfoRef.get() != null;
   }

   public void changePlace(String placeId) {
   	if (!isSessionActive()) {
   		Oculus.warn("Cannot change places while not logged in", getReconnectAction());
   		return;
   	}
   	setActivePlace(placeId);
   }
   
   public void updateAccount(String accountId) {
   	if (!isSessionActive()) {
   		Oculus.warn("Cannot update account while not loggined in", getReconnectAction());
   		return;
   	}
   	OculusSession updated = new OculusSession(getSessionInfo());
   	updated.setAccountId(accountId);
   	sessionInfoRef.set(updated);
   }

   public OculusSession getSessionInfo() throws IllegalStateException {
      OculusSession info = sessionInfoRef.get();
      if(info == null) {
         throw new IllegalStateException("Session is not active!");
      }
      return info;
   }

   /**
    * Raises a login prompt, this will return successfully when the user
    * is logged in, or an exception if the user cancels the login
    * attempts.  Note that this
    * @return
    */
   public ClientFuture<Void> promptLogin() {
      return doPromptLogin(null);
   }

   public ClientFuture<Void> promptLogin(String errorMessage) {
      return doPromptLogin(errorMessage);
   }

   private ClientFuture<Void> doPromptLogin(String errorMessage) {
      SettableClientFuture<Void> temp = loginPromptRef.get();
      if(temp != null) {
         return temp;
      }
      temp = Futures.settableFuture();
      if(!loginPromptRef.compareAndSet(null, temp)) {
         return loginPromptRef.get();
      }
      SettableClientFuture<Void> loginPrompt = temp;
      (StringUtils.isEmpty(errorMessage) ? LoginDialog.prompt() : LoginDialog.prompt(errorMessage))
         .onCompletion((r) -> loginPromptRef.compareAndSet(loginPrompt, null))
         .onSuccess((credentials) -> onCredentials(credentials, loginPrompt))
         .onFailure((error) -> loginPrompt.setError(error));
      return loginPrompt;
   }

   public ClientFuture<Void> promptChangePassword() {
      SettableClientFuture<Void> result = Futures.settableFuture();
      promptChangePassword(null, result);

      return result;
   }

   /**
    * Attempts to login using the previous session, if this fails it
    * will fallback to promptLogin.
    * @return
    */
   public ClientFuture<Void> login() {
      OculusSession info = getLastSession();
      if(info == null || info.getSessionToken() == null) {
         return promptLogin();
      }
      else {
         SessionTokenCredentials creds = new SessionTokenCredentials();
         creds.setToken(info.getSessionToken());
         creds.setConnectionURL(info.getHost());
         SettableClientFuture<Void> result = Futures.settableFuture();
         doLogin(creds, result, info.getPlaceId());
         return result;
      }
   }

   public ClientFuture<Void> login(Credentials credentials) {
      SettableClientFuture<Void> result = Futures.settableFuture();
      doLogin(credentials, result);
      return result;
   }

   public ClientFuture<?> logout() {
      return client.logout();
   }

   // TODO expose this listener on the client instead?
   public ListenerRegistration addSessionListener(Listener<? super SessionEvent> listener) {
      return sessionListeners.addListener(listener);
   }

   protected void onSessionEvent(com.iris.client.session.SessionEvent event) {
      if(event instanceof com.iris.client.session.SessionAuthenticatedEvent) {
         SessionInfo info =
               ((com.iris.client.session.SessionAuthenticatedEvent) event).getSessionInfo();
         if(info.getPlaces().isEmpty()) {
            Oculus.warn("No places associated with the current account, this account will not be able to register hubs");
         }

         Oculus.setPreference("login.uri2", client.getConnectionURL());
         Oculus.setPreference(PREF_AUTH_TOKEN, info.getSessionToken());

         OculusSession newInfo = new OculusSession();
         newInfo.setPersonId(info.getPersonId());
         if(info.getUsername() != null && !"Unknown".equals(info.getUsername())) {
            newInfo.setUserName(info.getUsername());
         }
         newInfo.setHost(client.getConnectionURL());
         newInfo.setSessionToken(info.getSessionToken());
         newInfo.setBillingPublicKey(info.getBillingPublicKey());
         newInfo.setLastConnected(new Date());
         rememberSession(newInfo);
         sessionAuthenticated(newInfo);
      }
      if(event instanceof SessionActivePlaceSetEvent) {
         logger.warn("got session active place event");
         String placeId = ((SessionActivePlaceSetEvent) event).getPlaceId().toString();
         OculusSession info = getSessionInfo();
         OculusSession newInfo = new OculusSession(info);
         newInfo.setPlaceId(placeId);
         sessionAuthenticated(newInfo);
         rememberSession(newInfo);
      }
      if(event instanceof SessionPlaceClearedEvent) {
         OculusSession info = getSessionInfo();
         OculusSession newInfo = new OculusSession(info);
         newInfo.setPlaceId(null);
         rememberSession(newInfo);
         SelectNewPlacePrompt
            .prompt("Place or the current person's access has been removed.  Please select a new place:")
            .onSuccess((p) -> changePlace(p.getPlaceId()));
      }
      if (event instanceof com.iris.client.session.SessionExpiredEvent) {
         sessionExpired();
      }
   }

   protected void onConnectionEvent(ConnectionEvent event) {
      if(event.getState() == ConnectionState.CONNECTED) {
         OculusSession info = sessionInfoRef.get();
         if(info == null) {
            return;
         }

         String placeId = info.getPlaceId();
         if(placeId == null) {
            return;
         }

         setActivePlace(placeId);
      }
   }

   protected void promptLogin(String errorMessage, SettableClientFuture<Void> result) {
      ClientFuture<LoginCredentials> prompt;
      if(errorMessage == null) {
         prompt = LoginDialog.prompt();
      }
      else {
         prompt = LoginDialog.prompt(errorMessage);
      }
      prompt
         .onSuccess((credentials) -> onCredentials(credentials, result))
         .onFailure((error) -> result.setError(error));
   }

   protected ClientFuture<SessionInfo> promptResetPassword(String username) {
      return ResetPasswordWizard.show(username);
   }

   protected ClientFuture<LoginCredentials> promptInvitation() {
      return InvitationWizard.show();
   }

   protected void promptWebLaunch() {
      String path = JOptionPane.showInputDialog(
            Oculus.getMainWindow(), 
            "Deep Link:",
            "Launch Web At",
            JOptionPane.QUESTION_MESSAGE
      );
      if(path == null) {
         // cancelled
         return;
      }
      client
         .linkToWeb(path)
         .onSuccess((url) -> {
            try {
               Desktop.getDesktop().browse(new URI(url));
            }
            catch(URISyntaxException e) {
               Oculus.showError("Invalid Redirect", e);
            }
            catch(Exception e) {
               Oculus.showError("Unable to Open Browser", e);
            }
         })
         .onFailure((e) -> Oculus.showError("Unable to Load Web", e))
         ;
   }
   
   protected void onCredentials(LoginCredentials credentials, SettableClientFuture<Void> result) {
      if(credentials.getAction() == LoginCredentials.Action.CREATE_ACCOUNT) {
         doCreateAccount(credentials, result);
      }
      else if(credentials.getAction() == LoginCredentials.Action.ACCEPT_INVITE) {
         client.setConnectionURL(credentials.getServiceUri());
         promptInvitation()
            .onSuccess((l) -> { onCredentials(l, result); })
            .onFailure((e) -> promptLogin(null, result));
      }
      else if(credentials.getAction() == LoginCredentials.Action.RESET_PASSWORD) {
         client.setConnectionURL(credentials.getServiceUri());
         promptResetPassword(credentials.getUsername())
            .onSuccess((b) -> result.setValue(null))
            // cancellation
            .onFailure((e) -> promptLogin(null, result))
            ;
      }
      else {
         UsernameAndPasswordCredentials uap = new UsernameAndPasswordCredentials();
         uap.setUsername(credentials.getUsername());
         uap.setPassword(credentials.getPassword());
         uap.setConnectionURL(credentials.getServiceUri());

         doLogin(uap, result);
      }
   }

   protected void doLogin(Credentials credentials, SettableClientFuture<Void> result) {
      doLogin(credentials, result, null);
   }
   
   protected void doLogin(Credentials credentials, SettableClientFuture<Void> result, String placeId) {
      Progress p =
            ProgressDialog
               .builder()
               .indeterminate()
               .withCancelEnable(true)
               .withTitle("Authenticating...")
               .withMessage("Connecting to "  + credentials.getConnectionURL() + "...")
               .show();

      ClientFuture<?> op =
         client
            .login(credentials)
            .onSuccess((info) -> doSelectPlace(info, result, placeId))
            .onFailure((e) -> {
               logger.debug("Error connecting", e);
               promptLogin("Unable to login.\nDetails: " + e.getMessage() + "", result);
            });
      p.onFailure((e) -> op.cancel(true));
      result.onCompletion((e) -> p.complete());
   }

   protected void doChangePassword(ChangePasswordInfo credentials, SettableClientFuture<Void> result) {

      ClientFuture<ChangePasswordResponse> request =
         IrisClientFactory
            .getService(PersonService.class)
            .changePassword(credentials.getOldPassword(), credentials.getNewPassword(), credentials.getEmail())
            .onSuccess((e) -> {
               if(e.getSuccess()) {
                  result.setValue(null);
                  Oculus.info("Password changed");
               }
               else {
                  promptChangePassword("Error to changing password.\nPlease check that your old password is spelled correctly", result);
               }
            })
            .onFailure((e) -> {
               logger.debug("Error changing password", e);
               promptChangePassword("Error to changing password.\nDetails: " + e.getMessage() + "", result);
            });
      Oculus.showProgress(request, "Changing password...", ProgressOptions.NO_ERROR_DIALOG);
   }
   
   protected void doSelectPlace(SessionInfo info, SettableClientFuture<Void> result, String placeId) {
      List<PlaceDescriptor> pd = info.getPlaces();
      if(pd == null || pd.isEmpty()) {
         promptLogin("*** THIS LOGIN IS IN AN INVALID STATE*** There are no places associated with this account -- TERMINAL ERROR!", result);
      }
      else if(placeId != null && pd.stream().anyMatch((p) -> Objects.equals(p.getPlaceId(), placeId))) {
         doSetActivePlace(placeId, result);
      }
      else if(pd.size() == 1) {
         doSetActivePlace(pd.get(0).getPlaceId(), result);
      }
      else {
         doPromptSelectPlace(result);
      }
   }
   
   protected void doSetActivePlace(String placeId, SettableClientFuture<Void> result) {
      setActivePlace(placeId)
         .onSuccess((e) -> { 
            result.setValue(null); 
         })
         .onFailure((e) -> {
            Oculus.showError("Unable to Set Active Place", e);
            doPromptSelectPlace(result);
         });
   }
   
   protected void doPromptSelectPlace(SettableClientFuture<Void> result) {
      SelectNewPlacePrompt
         .prompt("Select Place:")
         .onSuccess((place) -> doSetActivePlace(place.getPlaceId(), result))
         .onFailure((e) -> {
            Oculus.showError("Unable to Set Active Place", e);
            doPromptSelectPlace(result);
         });
   }

   protected void promptChangePassword(String errorMessage, SettableClientFuture<Void> result) {
      ClientFuture<ChangePasswordInfo> prompt;
      if(errorMessage == null) {
         prompt = ChangePasswordDialog.prompt();
      }
      else {
         prompt = ChangePasswordDialog.prompt(errorMessage);
      }
      prompt
         .onSuccess((credentials) -> {
            doChangePassword(credentials, result);
         })
         .onFailure((error) -> result.setError(error));
   }

   protected void doCreateAccount(LoginCredentials credentials, SettableClientFuture<Void> result) {
      CreateAccountRequest request = new CreateAccountRequest();
      request.setAddress("SERV:" + AccountService.NAMESPACE + ":");
      request.setConnectionURL(credentials.getServiceUri());
      request.setEmail(credentials.getUsername());
      request.setPassword(new String(credentials.getPassword()));
      // TODO why isn't this set to true automatically?
      request.setRestfulRequest(true);

      Progress p =
         ProgressDialog
            .builder()
            .indeterminate()
            .withCancelEnable(true)
            .withMessage("Creating account for user " + credentials.getUsername() + "...")
            .show();

      client
         .request(request)
         .onCompletion((e) -> p.complete())
         .onSuccess((event) -> {
            Oculus.showDialog("Account Created", "New account created for " + credentials.getUsername(), JOptionPane.OK_OPTION);
            credentials.setAction(LoginCredentials.Action.LOGIN);
            onCredentials(credentials, result);
         })
         .onFailure((e) -> {
            logger.debug("Error connecting", e);
            promptLogin("Error creating account.\nDetails: " + e.getMessage() + "", result);
         });
   }

   protected void sessionAuthenticated(OculusSession info) {
      OculusSession old = sessionInfoRef.getAndSet(info);
      if(old == null) {
         sessionListeners.fireEvent(new SessionAuthenticatedEvent());
      }
      else if(!old.equals(info)) {
         sessionListeners.fireEvent(new SessionExpiredEvent());
         sessionListeners.fireEvent(new SessionAuthenticatedEvent());
      }
   }

   protected void sessionExpired() {
      IrisClientFactory.getModelCache().clearCache();
      com.iris.oculus.Main.onLogout();
      OculusSession info = sessionInfoRef.getAndSet(null);
      if(info != null) {
         info.setSessionToken(null);
         rememberSession(info);
         sessionListeners.fireEvent(new SessionExpiredEvent());
      }
      promptLogin("Session expired")
         .onSuccess(com.iris.oculus.Main::onAuthenticated)
         .onFailure((e) -> {
            logger.info("Login window cancelled, exiting...");
            quit();
         });
   }

   protected ClientFuture<?> setActivePlace(String placeId) {
      return
         client
            .setActivePlace(placeId)
            .onSuccess((pId) -> {
               IrisClientFactory.getService(SessionService.class).log("oculus", "setActivePlace.success", "Set active place to " + pId);
               OculusSession old = getSessionInfo();
               OculusSession updated = new OculusSession(old);
               updated.setPlaceId(placeId);
               rememberSession(updated);
               if(sessionInfoRef.getAndSet(updated).getPlaceId() == null) {
                  sessionListeners.fireEvent(new SessionAuthenticatedEvent());
               }
               else {
                  sessionListeners.fireEvent(new PlaceChangedEvent());
               }
               logger.debug(
                     "Sending PlaceChangedEvent. Acct: {}, Place: {}, Old Acct: {}, Old Place: {}",
                     sessionInfoRef.get().getAccountId(),
                     sessionInfoRef.get().getPlaceId(),
                     old.getAccountId(),
                     old.getPlaceId()
               );
            })
            .onFailure((error) -> {
               IrisClientFactory.getService(SessionService.class).log("oculus", "setActivePlace.failed", error.getMessage());
            });
   }

   protected void logout(boolean exit) {
      logout()
         .onFailure((error) -> Oculus.showError("Unable to logout", error))
         .onCompletion((e) -> {
            if (exit) { // Logout & exit.
               quit();
            }
            else {
               promptLogin();
            }
         });
   }

   protected void quit() {
      System.exit(0);
   }
   
   @Nullable
   private OculusSession getLastSession() {
      List<OculusSession> info = listRecentSessions();
      if(info.isEmpty()) {
         return null;
      }
      return info.get(0);
   }
   
   private void removeSession(Iterable<OculusSession> sessions, OculusSession session) {
      Iterator<OculusSession> it = sessions.iterator();
      while(it.hasNext()) {
         OculusSession other = it.next();
         if(
               Objects.equals(session.getUserName(), other.getUserName()) &&
               Objects.equals(session.getHost(), other.getHost())
         ) {
            it.remove();
         }
      }
   }
   
   private void rememberSessions(List<OculusSession> sessions) {
      Collections.sort(sessions, Comparator.comparing(OculusSession::getLastConnected).reversed());
      while(sessions.size() > 0) {
         try {
            Oculus.setPreference(PREF_RECENT_SESSIONS, JSON.toJson(sessions));
            return;
         }
         catch(IllegalArgumentException e) {
            sessions.remove(sessions.size() - 1);
         }
      }
   }
   
   public static void main(String [] args) throws Exception {
      IrisClientFactory.init(new NettyIrisClientFactory());
      SessionController controller = new SessionController(IrisClientFactory.getClient());
      SwingUtilities.invokeAndWait(() -> {
         controller
            .login()
            .onSuccess((b) -> {
               int result = JOptionPane.showConfirmDialog(null, "Login Succeed! Logout?", "Logout", JOptionPane.YES_NO_OPTION);
               if(result == JOptionPane.YES_OPTION) {
                  controller.logout();
               }
               System.exit(0);
            })
            .onFailure((e) -> {
               JOptionPane.showMessageDialog(null, "Login Failed: " + e.getMessage());
               System.exit(-1);
            });
      });
   }
}

