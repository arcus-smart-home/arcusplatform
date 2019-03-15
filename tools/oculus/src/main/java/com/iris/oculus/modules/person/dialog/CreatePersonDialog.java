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
package com.iris.oculus.modules.person.dialog;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;
import org.jdesktop.swingx.HorizontalLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.Utils;
import com.iris.bootstrap.Bootstrap;
import com.iris.bootstrap.ServiceLocator;
import com.iris.bootstrap.guice.GuiceServiceLocator;
import com.iris.client.IrisClientFactory;
import com.iris.client.capability.Person;
import com.iris.client.event.ClientFuture;
import com.iris.client.impl.I18NServiceImpl;
import com.iris.client.service.I18NService;
import com.iris.oculus.Oculus;
import com.iris.oculus.OculusModule;
import com.iris.oculus.widget.Dialog;
import com.iris.oculus.widget.Fields;

public class CreatePersonDialog {
   private static final Logger logger = LoggerFactory.getLogger(CreatePersonDialog.class);

	   private static class InstanceRef {
	      private static final CreatePersonDialogImpl INSTANCE = new CreatePersonDialogImpl();
	   }

	   public static ClientFuture<Map<String, Object>> prompt() {
	      Utils.assertTrue(SwingUtilities.isEventDispatchThread());
	      return InstanceRef.INSTANCE.prompt();
	   }

	   public static ClientFuture<Map<String, Object>> prompt(String errorMessage) {
	      Utils.assertTrue(SwingUtilities.isEventDispatchThread());
	      return InstanceRef.INSTANCE.prompt(errorMessage);
	   }

	   protected static void hide() {
	      InstanceRef.INSTANCE.setVisible(false);
	   }

	   @SuppressWarnings("serial")
		private static class CreatePersonDialogImpl extends Dialog<Map<String, Object>> {
	   	Pattern emailPattern = Pattern.compile(
	   			"^[_A-Z0-9-\\+]+(\\.[_A-Z0-9-]+)*@[A-Z0-9-]+(\\.[A-Z0-9]+)*(\\.[A-Z]{2,})$",
	   			Pattern.CASE_INSENSITIVE);
	   	JTextField firstName = new JTextField();
	   	JTextField lastName = new JTextField();

	   	JTextField email = new JTextField();
	   	JTextField mobileNumber;

	   	// commented out becuase halflings don't have passwords
	   	/*
	   	JPasswordField password = new JPasswordField();
	   	*/
//	   	JSpinner seqQuestion1 = new JSpinner();
//	   	JSpinner seqQuestion2 = new JSpinner();
//	   	JSpinner seqQuestion3 = new JSpinner();
	   	Map<String, String> questions = new HashMap<>();

	      CreatePersonDialogImpl() {
	         super();
	         setTitle("Create Person");
	         setModal(true);
	         setResizable(false);
	         setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

	         SwingUtilities.invokeLater(() -> {
	         	I18NService secQuestions = new I18NServiceImpl(IrisClientFactory.getClient());
	         	secQuestions.loadLocalizedStrings(null, "en-US").onCompletion((result) -> {
	         		if (result.isError()) {
	         			Oculus.warn("Could not load security questions.", result.getError());
	         			return;
	         		}

	         		I18NService.LoadLocalizedStringsResponse response = new I18NService.LoadLocalizedStringsResponse(result.getValue());
	         		questions.putAll(response.getLocalizedStrings());
	         		logger.info("Security Questions received.");
	         	});
	         });

	         firstName.setColumns(50);
	         lastName.setColumns(50);
	         email.setColumns(50);
	         mobileNumber = Fields.formattedTextFieldBuilder("(###) ###-####")
	         		.labelled("Mobile Number")
	         		.build()
	         		.getComponent();
	      }

	      @Override
         public ClientFuture<Map<String, Object>> prompt() {
	         clearErrorMessage();
	         return super.prompt();
	      }

	      public ClientFuture<Map<String, Object>> prompt(String errorMessage) {
	         setErrorMessage(errorMessage);
	         return super.prompt();
	      }

	      // TODO: Security Answers.
	      // TODO: Opt-in is not getting set.
	      @Override
         protected Map<String, Object> getValue() {
	      	Map<String, Object> thePerson = new HashMap<>();

	      	if(!StringUtils.isBlank(email.getText())) {
	      	   if (emailPattern.matcher(email.getText()).matches()) {
	      	      thePerson.put(Person.ATTR_EMAIL, email.getText());
	      	   } else {
	      	      throw new RuntimeException("Email format is invalid.");
	      	   }
	      	}

	      	thePerson.put(Person.ATTR_FIRSTNAME, firstName.getText());
				thePerson.put(Person.ATTR_LASTNAME, lastName.getText());

				if(!"()-".equals(StringUtils.deleteWhitespace(mobileNumber.getText()))) {
				   thePerson.put(Person.ATTR_MOBILENUMBER, mobileNumber.getText());
				}
				// commneted out because halflings don't have passwords
				/*

	      	if (password.getPassword().length > 0) {
	      		thePerson.put("password", new String(password.getPassword()));
	      	}
	      	*/

	         return thePerson;
	      }

	      @Override
         protected JPanel createContents() {
	         JPanel panel = new JPanel();
	         panel.setLayout(new GridBagLayout());

	         GridBagConstraints labels = new GridBagConstraints();
	         labels.gridy = 0;
	         labels.fill = GridBagConstraints.NONE;
	         labels.anchor = GridBagConstraints.EAST;

	         GridBagConstraints fields = new GridBagConstraints();
	         fields.gridy = 0;
	         fields.fill = GridBagConstraints.HORIZONTAL;
	         fields.weightx = 1;

	         panel.add(new JLabel("Email"), labels.clone());
	         panel.add(email, fields.clone());
	         labels.gridy++;
	         fields.gridy++;

	         panel.add(new JLabel("First Name"), labels.clone());
	         panel.add(firstName, fields.clone());
	         labels.gridy++;
	         fields.gridy++;

	         panel.add(new JLabel("Last Name"), labels.clone());
	         panel.add(lastName, fields.clone());
	         labels.gridy++;
	         fields.gridy++;

	         panel.add(new JLabel("Mobile Number"), labels.clone());
	         panel.add(mobileNumber, fields.clone());
	         labels.gridy++;
	         fields.gridy++;

	         panel.add(new JSeparator(SwingConstants.HORIZONTAL), labels.clone());
	         labels.gridy++;
	         fields.gridy++;

	         // commented out because halflings don't have passwords
	         /*
	         panel.add(Box.createHorizontalBox(), labels.clone());
	         panel.add(new JLabel("If user is to allowed to login, enter a password."), fields.clone());
	         labels.gridy++;
	         fields.gridy++;

	         panel.add(new JLabel("Password"), labels.clone());
	         panel.add(password, fields.clone());
	         labels.gridy++;
	         fields.gridy++;
	         */

	         JPanel buttonPanel = new JPanel();
	         buttonPanel.setLayout(new HorizontalLayout());
	         GridBagConstraints buttons = new GridBagConstraints();
	         buttons.gridy = labels.gridy + 1;
	         buttons.anchor = GridBagConstraints.NORTHEAST;
	         buttons.gridwidth = 2;
	         buttons.weighty = 1.0;

	         buttonPanel.add(new JButton(new AbstractAction("Cancel") {
					@Override
					public void actionPerformed(ActionEvent e) {
						CreatePersonDialogImpl.this.dispose();
					}
				}));
	         buttonPanel.add(new JButton(submitAction("Create User")));
	         panel.add(buttonPanel, buttons);

	         return panel;
	      }

	   }

		@SuppressWarnings("unchecked")
		public static void main(String [] args) throws Exception {
			Bootstrap bootstrap =
		         Bootstrap
		            .builder()
		            .withModuleClasses(OculusModule.class)
		            .build();
		      ServiceLocator.init(GuiceServiceLocator.create(bootstrap.bootstrap()));
	   	SwingUtilities.invokeAndWait(CreatePersonDialog::prompt);
	   }
}

