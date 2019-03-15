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
package com.iris.oculus.modules.pairing.wizard;

import java.awt.Component;
import java.util.Collection;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import org.jdesktop.swingx.VerticalLayout;

import com.iris.client.capability.PairingSubsystem;
import com.iris.client.event.ClientFuture;
import com.iris.oculus.util.Actions;
import com.iris.oculus.widget.Dialog;
import com.iris.oculus.widget.HyperLink;

/**
 * @author tweidlin
 *
 */
public class PostCustomizationDialog extends Dialog<PostCustomizationDialog.Action> {
	public enum Action {
		PAIR_ANOTHER,
		CUSTOMIZE_ANOTHER,
		DISMISS_ALL;
	}
	
	public static ClientFuture<Action> prompt(PairingInfo input) {
		return new PostCustomizationDialog(input).prompt();
	}
	
	private PairingInfo input;
	private Action action = Action.DISMISS_ALL;

	private PostCustomizationDialog(PairingInfo input) {
		this.input = input;
		this.setTitle("Customized Device!");
	}
	
	@Override
	protected Action getValue() {
		return action;
	}

	@Override
	protected Component createContents() {
		JPanel panel = new JPanel(new VerticalLayout());
		panel.add(new HyperLink(Actions.build("Pair Another Device", () -> submit(Action.PAIR_ANOTHER))).getComponent());
		int remainingDevices = ((Collection<?>) input.getPairingSubsystem().get(PairingSubsystem.ATTR_PAIRINGDEVICES)).size();
		if(remainingDevices > 0) {
			panel.add(new HyperLink(Actions.build(String.format("Customize %d Remaining Devices", remainingDevices), () -> submit(Action.CUSTOMIZE_ANOTHER))).getComponent());
		}
		panel.add(new JSeparator(JSeparator.HORIZONTAL));
		
		JButton dismissAll = new JButton(Actions.build("Dismiss All", () -> submit(Action.DISMISS_ALL)));
		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
		buttons.add(Box.createGlue());
		buttons.add(dismissAll);
		panel.add(buttons);
		
		return panel;
	}
	
	private void submit(Action action) {
		this.action = action;
		this.submit();
	}

}

