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
package com.iris.oculus.modules.pairing.wizard;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.client.capability.PairingSubsystem.CancelS2PinRequest;
import com.iris.client.capability.PairingSubsystem.GetS2PinRequestRequest;
import com.iris.client.capability.PairingSubsystem.GetS2PinRequestResponse;
import com.iris.client.capability.PairingSubsystem.SubmitS2PinRequest;
import com.iris.client.exception.ErrorResponseException;
import com.iris.client.model.Model;
import com.iris.messages.errors.Errors;
import com.iris.oculus.Oculus;
import com.iris.oculus.util.Actions;
import com.iris.protocol.zwave.S2Pin;

/**
 * Asks for the PIN printed on a Z-Wave device that wants S2 Authenticated or
 * Access Control. Non-modal, so the pairing wizard stays usable behind it. This
 * class doesn't log the PIN or DSK, but the client library's debug logging
 * prints requests and responses as they are sent.
 */
@SuppressWarnings("serial")
final class S2PinDialog extends JDialog {
   private static final Logger logger = LoggerFactory.getLogger(S2PinDialog.class);

   private final Model subsystem;
   private final JLabel dskLabel = new JLabel("Loading...");
   private final JLabel timeLabel = new JLabel(" ");
   private final JLabel message = new JLabel(" ");
   private final JTextField pinField = new JTextField(6);
   private final JButton submit = new JButton(Actions.build("Submit", this::onSubmit));
   private final JButton decline = new JButton(Actions.build("Decline", this::onDecline));
   private final Timer countdown = new Timer(1000, (event) -> onTick());

   private String requestId;
   private Date expires;
   /** A submit or decline is waiting for its answer. */
   private boolean busy;
   /** The request can't be answered any more. */
   private boolean spent;

   private S2PinDialog(Model subsystem) {
      super(Oculus.getMainWindow(), "Z-Wave PIN");
      this.subsystem = subsystem;
      setModal(false);
      setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
      // Closing the window declines, so the device isn't left waiting for nothing
      addWindowListener(new WindowAdapter() {
         @Override
         public void windowClosing(WindowEvent e) {
            onDecline();
         }
      });

      JPanel fields = new JPanel(new GridLayout(0, 1, 0, 4));
      fields.setBorder(BorderFactory.createEmptyBorder(10, 12, 6, 12));
      fields.add(new JLabel("Check these numbers match the device label:"));
      fields.add(dskLabel);
      fields.add(new JLabel("Then enter the 5 digit PIN, the first group on the label:"));
      JPanel pinRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
      pinRow.add(pinField);
      fields.add(pinRow);
      fields.add(timeLabel);
      fields.add(message);

      JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      buttons.add(decline);
      buttons.add(submit);

      add(fields, BorderLayout.CENTER);
      add(buttons, BorderLayout.SOUTH);
      setEnabledButtons(false);
      pack();
      setLocationRelativeTo(Oculus.getMainWindow());
   }

   /** Reads the waiting request and asks for its PIN. */
   static S2PinDialog show(Model subsystem) {
      S2PinDialog dialog = new S2PinDialog(subsystem);
      dialog.setVisible(true);
      dialog.load();
      return dialog;
   }

   /** Closes the prompt straight away, for a request that has been replaced. */
   void close() {
      countdown.stop();
      dispose();
   }

   /**
    * The platform has stopped waiting. It does so as soon as a PIN is sent, so
    * an answer still on its way decides when the prompt goes, and whether it
    * has an error to show first.
    */
   void requestEnded() {
      spent = true;
      if (!busy) {
         close();
      }
   }

   private void load() {
      GetS2PinRequestRequest request = new GetS2PinRequestRequest();
      request.setAddress(subsystem.getAddress());
      subsystem
         .request(request)
         .onSuccess((event) -> {
            GetS2PinRequestResponse response = new GetS2PinRequestResponse(event);
            requestId = response.getRequestId();
            expires = response.getExpires();
            dskLabel.setText(S2Pins.groupsToCheck(response.getDsk()));
            setEnabledButtons(true);
            onTick();
            countdown.start();
            pack();
         })
         .onFailure((error) -> {
            logger.warn("Unable to read the S2 PIN request: {}", error.getMessage());
            close();
         });
   }

   private void onTick() {
      long seconds = S2Pins.secondsLeft(expires, System.currentTimeMillis());
      timeLabel.setText(seconds + " seconds left");
      if (seconds == 0 && !busy) {
         close();
      }
   }

   private void onSubmit() {
      String pin = pinField.getText().trim();
      if (!S2Pin.isValidPin(pin)) {
         showError("A PIN is five digits, at most 65535");
         return;
      }

      busy = true;
      setEnabledButtons(false);
      countdown.stop();
      timeLabel.setText("Sending...");
      SubmitS2PinRequest request = new SubmitS2PinRequest();
      request.setAddress(subsystem.getAddress());
      request.setRequestId(requestId);
      request.setPin(pin);
      subsystem
         .request(request)
         .onSuccess((event) -> close())
         .onFailure((error) -> {
            busy = false;
            logger.warn("Unable to submit the S2 PIN: {}", error.getMessage());
            boolean badPin =
                  error instanceof ErrorResponseException &&
                  Errors.CODE_INVALID_PARAM.equals(((ErrorResponseException) error).getCode());
            showError(error instanceof ErrorResponseException ? ((ErrorResponseException) error).getErrorMessage() : error.getMessage());
            // Only a malformed PIN leaves the request open; anything else used it up
            if (badPin && !spent) {
               setEnabledButtons(true);
               onTick();
               countdown.start();
            }
            else {
               showSpent();
            }
         });
   }

   private void onDecline() {
      if (requestId == null || spent) {
         close();
         return;
      }
      busy = true;
      setEnabledButtons(false);
      CancelS2PinRequest request = new CancelS2PinRequest();
      request.setAddress(subsystem.getAddress());
      request.setRequestId(requestId);
      subsystem
         .request(request)
         .onSuccess((event) -> close())
         .onFailure((error) -> {
            logger.warn("Unable to decline the S2 PIN: {}", error.getMessage());
            close();
         });
   }

   /** Leaves the error on screen with only a way to close it. */
   private void showSpent() {
      spent = true;
      timeLabel.setText(" ");
      pinField.setEnabled(false);
      submit.setEnabled(false);
      decline.setText("Close");
      decline.setEnabled(true);
   }

   private void setEnabledButtons(boolean enabled) {
      submit.setEnabled(enabled);
      decline.setEnabled(enabled);
      pinField.setEnabled(enabled);
   }

   private void showError(String text) {
      message.setText(text == null ? "Request failed" : text);
   }
}
