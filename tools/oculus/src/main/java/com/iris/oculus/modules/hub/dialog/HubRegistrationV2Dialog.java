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
package com.iris.oculus.modules.hub.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.concurrent.CancellationException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.iris.bootstrap.ServiceLocator;
import com.iris.client.IrisClientFactory;
import com.iris.client.capability.Place.RegisterHubV2Response;
import com.iris.client.event.ClientFuture;
import com.iris.client.event.Futures;
import com.iris.client.model.HubModel;
import com.iris.client.model.PlaceModel;
import com.iris.oculus.Main;
import com.iris.oculus.Main.Arguments;
import com.iris.oculus.modules.place.PlaceController;
import com.iris.oculus.modules.session.SessionController;
import com.iris.oculus.modules.status.ShowExceptionAction;
import com.iris.oculus.util.Documents;
import com.iris.oculus.widget.Dialog;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

/**
 * 
 */
public class HubRegistrationV2Dialog extends Dialog<HubModel> {
	
	public static ClientFuture<HubModel> prompt(PlaceModel place) {
		HubRegistrationV2Dialog dialog = new HubRegistrationV2Dialog(place);
		return dialog.prompt();
	}
	
	
	private static final String ERROR = "Oops! Something has gone terribly wrong: %s";
	private static final String INSTRUCTIONS = "Enter your hub id in the format xxx-yyyy";
	private static final String SEARCHING = "Waiting for hub %s to connect to the platform, this may take a minute or two.";
	private static final String CONNECTED = "W00T! We found your hub, you're one quick(-ish) firmware download from being online.";
	private static final String DOWNLOADING = "Downloading the most recent firmware to your hub, it should be available shortly.";
	private static final String APPLYING = "Your hub is applying the firmware and will reconnect shortly -- you're almost done.";
	private static final String SUCCESSFULLY_PAIRED = "Hub %s has been paired to %s!";
	
	private RegistrationLooper poller;
	private JLabel instructions = new JLabel(INSTRUCTIONS);
	private JTextField hubId = new JTextField();
	private HubModel result;
	private JProgressBar progress = new JProgressBar(0, 100);
	private JButton action = new JButton();
	// TODO add log pane
	
	private HubRegistrationV2Dialog(PlaceModel place) {
		this.poller = new RegistrationLooper(place);
		init();
	}
	
	private void init() {
		setTitle("Enter Hub ID");
		setModal(true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		Documents.addDocumentChangeListener(hubId.getDocument(), this::updateHubId);
		action.setAction(cancelAction());
	}
	
	@Override
	protected HubModel getValue() {
		return result;
	}

	@Override
	protected void onShow() {
		super.onShow();
		hubId.requestFocusInWindow();
	}

	@Override
	protected void onHide() {
		poller.stop();
		super.onHide();
	}

	@Override
	protected Component createContents() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(instructions, BorderLayout.NORTH);
		panel.add(new JLabel("Hub Id: "), BorderLayout.WEST);
		panel.add(hubId, BorderLayout.CENTER);
		panel.add(action, BorderLayout.EAST);
		panel.add(progress, BorderLayout.SOUTH);
		return panel;
	}

	protected void updateHubId() {
		instructions.setText(INSTRUCTIONS);
		action.setAction(cancelAction());
		poller.setHubId(hubId.getText());
	}
	
	protected void startProgress() {
		progress.setIndeterminate(true);
		progress.setEnabled(true);
	}

	protected void stopProgress() {
		progress.setIndeterminate(false);
		progress.setValue(0);
	}

	protected void showOffline() {
		instructions.setText(String.format(SEARCHING, poller.hubId));
		progress.setIndeterminate(true);
		progress.setString("Waiting for Hub to Connect");
	}
	
	protected void showOnline() {
		instructions.setText(CONNECTED);
		progress.setIndeterminate(true);
		progress.setString("Found Hub -- Requesting Firmware Upgrade");
	}
	
	protected void showDownloadProgress(int percent) {
		progress.setIndeterminate(false);
		progress.setValue(percent);
		progress.setString("Downloading Firmware");
		instructions.setText(String.format(DOWNLOADING, poller.hubId));
	}
	
	protected void showApplying() {
		progress.setIndeterminate(true);
		progress.setString("Found Hub -- Requesting Firmware Upgrade");
		instructions.setText(String.format(APPLYING, poller.hubId));
	}
	
	protected void showSuccess(HubModel hub) {
		progress.setIndeterminate(false);
		progress.setValue(100);
		progress.setString("Your Hub is Registered!");
		instructions.setText(String.format(SUCCESSFULLY_PAIRED, hub.getId(), poller.place.getName()));
		action.setAction(submitAction());
		this.result = hub;
	}
	
	protected void showError(Throwable t) {
		instructions.setText(String.format(ERROR, t.getMessage()));
		setErrorMessage(t.getMessage(), new ShowExceptionAction(t));
	}
	
	private class RegistrationLooper {
		private Timer timer;
		private PlaceModel place;
		private String hubId;
		private ClientFuture<?> lastRequest = Futures.succeededFuture(null);
		
		public RegistrationLooper(PlaceModel place) {
			this.timer = new Timer(1000, (e) -> this.poll());
			this.timer.setRepeats(false);
			this.place = place;
		}
		
		public void stop() {
			stopProgress();
			timer.stop();
			lastRequest.cancel(false);
		}
		
		public void setHubId(String hubId) {
			stop();
			this.hubId = hubId;
			poll();
		}
		
		public void poll() {
			lastRequest =
				place
					.registerHubV2(hubId)
					.onSuccess(this::onResponse)
					.onFailure(this::onError);
		}
		
		protected void onResponse(RegisterHubV2Response response) {
			clearErrorMessage();
			switch(response.getState()) {
			case RegisterHubV2Response.STATE_OFFLINE:
				showOffline();
				break;
			case RegisterHubV2Response.STATE_ONLINE:
				showOnline();
				break;
			case RegisterHubV2Response.STATE_DOWNLOADING:
				showDownloadProgress(response.getProgress());
				break;
			case RegisterHubV2Response.STATE_APPLYING:
				showApplying();
				break;
			case RegisterHubV2Response.STATE_REGISTERED:
				HubModel hub = (HubModel) IrisClientFactory.getModelCache().addOrUpdate(response.getHub());
				showSuccess(hub);
				return; // don't restart the timer
				
			default:
				
			}
			
			timer.start();
			startProgress();
		}
		
		protected void onError(Throwable t) {
			if(t instanceof CancellationException) {
				// ignore
			}
			else {
				showError(t);
			}
			stopProgress();
		}
	}

	public static void main(String [] args) throws Exception {
		Main.bootstrap(new Arguments());
		SessionController controller = ServiceLocator.getInstance(SessionController.class);
		SwingUtilities.invokeAndWait(() -> {
			controller
				.login()
				.onSuccess((v) -> {
					AtomicBoolean fired = new AtomicBoolean(false);
					ServiceLocator
						.getInstance(PlaceController.class)
						.getActivePlace()
						.addSelectionListener((opm) -> {
							if(opm.isPresent() && fired.compareAndSet(false, true)) {
								HubRegistrationV2Dialog
									.prompt(opm.get())
									.onSuccess((hub) -> System.out.println("Registered: " + hub.getAddress()))
									.onFailure((error) -> { 
										System.out.println("Failed to register hub"); 
										error.printStackTrace(System.out);
									})
									.onCompletion((r) -> System.exit(r.isError() ? -1 : 0));
							}
						});
				})
				.onFailure((e) -> {
					e.printStackTrace();
					System.exit(-1);
				});
		});
	}

}

