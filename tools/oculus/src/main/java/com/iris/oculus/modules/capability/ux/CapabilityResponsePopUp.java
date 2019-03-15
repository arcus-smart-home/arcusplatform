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
package com.iris.oculus.modules.capability.ux;

import java.awt.Dialog.ModalityType;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;

import com.google.common.collect.ImmutableMap;
import com.iris.client.ClientEvent;
import com.iris.client.exception.ErrorResponseException;
import com.iris.oculus.util.BaseComponentWrapper;
import com.iris.oculus.util.JsonPrettyPrinter;
import com.iris.oculus.widget.Fields;
import com.iris.oculus.widget.FormView;
import com.iris.oculus.widget.FormView.LabelLocation;

/**
 * 
 */
public class CapabilityResponsePopUp extends BaseComponentWrapper<Window> {

	private String command;
	private Map<String, Object> attributes = ImmutableMap.of();
	private String text = "";

	private FormView form;
	private JScrollPane scroller;
	private JTextPane response;

	private static final KeyStroke escapeStroke =
			KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
	public static final String dispatchWindowClosingActionMapKey =
			"com.spodding.tackline.dispatch:WINDOW_CLOSING";


	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	public void onResponse(ClientEvent response) {
		this.getComponent();
		this.form.setValue("status", "SUCCESS");
		this.form.setValue("from", response.getSourceAddress());
		this.form.setValue("type", response.getType());
		this.response.setContentType("application/json");
		this.response.setText(JsonPrettyPrinter.prettyPrint(response.getAttributes(), JsonPrettyPrinter.Format.PLAIN_TEXT));
		this.response.setCaretPosition(0);
		show();
	}
	
	public void onError(Throwable cause) {
		getComponent();
		this.form.setValue("status", "ERROR");
		this.form.setValue("from", "");
		if(cause instanceof ErrorResponseException) {
			ErrorResponseException ere = (ErrorResponseException) cause;
			this.form.setValue("type", ere.getCode());
		}
		else {
			this.form.setValue("type", cause.getClass().getName());
		}
		StringWriter writer = new StringWriter();
      PrintWriter pw = new PrintWriter(writer);
      cause.printStackTrace(pw);
      pw.flush();
      this.response.setContentType("text/plain");
      this.response.setText(writer.toString());
      this.response.setCaretPosition(0);
      show();
	}

	public void show() {
      this.getComponent().setVisible(true);
   }
   
   public void hide() {
      if(this.isActive()) {
         this.getComponent().setVisible(false);
         this.dispose();
      }
   }
   
   @Override
   protected Window createComponent() {
      JDialog window = new JDialog(null, "Response to " + command, ModalityType.MODELESS);
      window.setAlwaysOnTop(false);
      window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      // TODO remember dimensions
      window.setSize(800, 600);
	   installEscapeCloseOperation(window);

	   response = new JTextPane();
      response.setEditable(false);
      response.setContentType("text/html");
   	response.setText(text);
   	response.setCaretPosition(0);
   	
   	scroller = new JScrollPane(this.response);
   	
   	form = new FormView();
   	form.addField(
   			Fields
   				.textFieldBuilder()
   				.notEditable()
   				.labelled("Status")
   				.named("status")
   				.build()
		);
   	form.addField(
   			Fields
   				.textFieldBuilder()
   				.notEditable()
   				.labelled("From")
   				.named("from")
   				.build()
		);
   	form.addField(
   			Fields
   				.textFieldBuilder()
   				.notEditable()
   				.labelled("Type")
   				.named("type")
   				.build()
		);
   	form.addField(
   			new JLabel("Attributes"),
   			scroller,
   			LabelLocation.TOP
		);
      
      window.add(form.getComponent());
      return window;
   }

	public static void installEscapeCloseOperation(final JDialog dialog) {
		Action dispatchClosing = new AbstractAction() {
			public void actionPerformed(ActionEvent event) {
				dialog.dispatchEvent(new WindowEvent(
						dialog, WindowEvent.WINDOW_CLOSING
				));
			}
		};
		JRootPane root = dialog.getRootPane();
		root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				escapeStroke, dispatchWindowClosingActionMapKey
		);
		root.getActionMap().put( dispatchWindowClosingActionMapKey, dispatchClosing
		);
	}


}

