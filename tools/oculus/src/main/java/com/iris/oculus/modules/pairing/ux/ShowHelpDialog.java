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
package com.iris.oculus.modules.pairing.ux;

import java.awt.Component;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import org.apache.commons.lang3.StringUtils;
import org.jdesktop.swingx.VerticalLayout;

import com.iris.client.bean.PairingHelpStep;
import com.iris.client.capability.PairingSubsystem.ListHelpStepsResponse;
import com.iris.client.event.ClientFuture;
import com.iris.oculus.util.Browser;
import com.iris.oculus.widget.Dialog;
import com.iris.oculus.widget.HyperLink;

public class ShowHelpDialog extends Dialog<Void> {
	
	public static ClientFuture<Void> prompt(ListHelpStepsResponse help) {
		return new ShowHelpDialog(help).prompt();
	}
	
	private ListHelpStepsResponse help;
	
	private ShowHelpDialog(ListHelpStepsResponse help) {
		this.help = help;
	}
	
	@Override
	protected Void getValue() {
		return null;
	}

	@Override
	protected Component createContents() {
		boolean added = false;
		JPanel panel = new JPanel(new VerticalLayout());
		for(Map<String, Object> value: help.getSteps()) {
			if(added) {
				panel.add(new JSeparator(JSeparator.HORIZONTAL));
			}
			PairingHelpStep step = new PairingHelpStep(value);
			panel.add(new JLabel(String.format("%d) %s", step.getOrder(), step.getId())));
			panel.add(new JLabel(String.format("Type: %s", step.getAction())));
			if(!StringUtils.isEmpty(step.getLinkUrl())) {
				panel.add(Browser.link(step.getLinkUrl(), step.getLinkText()).getComponent());
			}
			panel.add(new JLabel(step.getMessage()));
			added = true;
		}
		return panel;
	}

}

