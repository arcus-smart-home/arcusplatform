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
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.apache.commons.lang3.StringUtils;
import org.jdesktop.swingx.VerticalLayout;

import com.iris.client.bean.PairingInput;
import com.iris.client.bean.PairingStep;
import com.iris.client.capability.PairingSubsystem;
import com.iris.client.capability.PairingSubsystem.StartPairingResponse;
import com.iris.client.event.ClientFuture;
import com.iris.client.model.Model;
import com.iris.oculus.util.BaseComponentWrapper;
import com.iris.oculus.util.Browser;
import com.iris.oculus.widget.FieldWrapper;
import com.iris.oculus.widget.Fields;
import com.iris.oculus.widget.Fields.TextFieldBuilder;
import com.iris.oculus.widget.FormView;
import com.iris.oculus.widget.FormView.LabelLocation;
import com.iris.oculus.widget.HyperLink;
import com.iris.oculus.widget.wizard.Wizard;
import com.iris.oculus.widget.wizard.Wizard.Transition;

/**
 * @author tweidlin
 *
 */
public class PairingInstructionsPage extends BaseComponentWrapper<Component> implements Transition<PairingInfo, PairingInfo> {
	private Wizard<?, ?> dialog;
	private PairingInfo input;
	private FormView form;

	@Override
	public void update(Wizard<?, ?> dialog, PairingInfo input) {
		this.input = input;
		this.dialog = dialog;
		this.dispose(); // regenerate the view
	}

	@Override
	public Component show(Wizard<?, ?> dialog) {
		this.dialog.setNextEnabled(true);
		return getComponent();
	}

	@Override
	public PairingInfo getValue() {
		return input;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" }) // only text fields are exposed so this is a safe cast
	@Override
	public ClientFuture<PairingInfo> commit() {
		if(!StringUtils.isBlank(input.getPairingInstructions().getOauthUrl())) {
			Browser.launch(input.getPairingInstructions().getOauthUrl());
		}
		
		PairingSubsystem.SearchRequest search = new PairingSubsystem.SearchRequest();
		search.setForm((Map) form.getValues());
		search.setProductAddress(input.getProduct().transform(Model::getAddress).orNull());
		return 
			input.getPairingSubsystem()
				.request(search)
				.transform((event) -> {
					// FIXME do we need to do anything with the result?
					return input;
				});
	}

	@Override
	protected Component createComponent() {
		form = new FormView();
		form.addField(new JLabel("Pairing Mode: "), new JLabel(input.getPairingInstructions().getMode()));
		form.addField(new JLabel("Video: "), getVideoLink(input.getPairingInstructions()));
		// FIXME show OAuth information
		form.addField(new JLabel("Instructions"), new JScrollPane(getInstructions(input.getPairingInstructions()), JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), LabelLocation.TOP);
		addFormFields(input.getPairingInstructions());
		return form.getComponent();
	}

	private void addFormFields(StartPairingResponse pairingInstructions) {
		if(pairingInstructions.getForm() == null) {
			return;
		}
		for(Map<String, Object> input: pairingInstructions.getForm()) {
			PairingInput pi = new PairingInput(input);
			TextFieldBuilder builder = Fields.textFieldBuilder();
			if(PairingInput.TYPE_HIDDEN.equals(pi.getType())) {
				builder = builder.notEditable();
			}
			else {
				builder = builder.editable();
			}
			FieldWrapper<?, String> field =
					builder
						.named(pi.getName())
						.labelled(StringUtils.isEmpty(pi.getLabel()) ? pi.getName() : pi.getLabel())
						.build();
			if(!StringUtils.isEmpty(pi.getValue())) {
				field.setValue(pi.getValue());
			}
			form.addField(field);
		}
	}

	private Component getInstructions(StartPairingResponse pairingInstructions) {
		JPanel panel = new JPanel(new VerticalLayout());
		for(Map<String, Object> s: input.getPairingInstructions().getSteps()) {
			PairingStep step = new PairingStep(s);
			panel.add(new JLabel(step.getId()));
			if(!StringUtils.isEmpty(step.getTitle())) {
				// FIXME bold
				panel.add(new JLabel(step.getTitle()));
			}
			if(!StringUtils.isEmpty(step.getInfo())) {
				// FIXME italic
				panel.add(new JLabel(step.getInfo()));
			}
			if(!StringUtils.isEmpty(step.getLinkUrl())) {
				panel.add(new HyperLink(StringUtils.isEmpty(step.getLinkText()) ? step.getLinkUrl() : step.getLinkText()).getComponent());
			}
			JTextArea instructions = new JTextArea(StringUtils.join(step.getInstructions(), "\n"));
			instructions.setEditable(false);
			instructions.setLineWrap(true);
			panel.add(instructions);
		}
		return panel;
	}

	private Component getVideoLink(StartPairingResponse pairingInstructions) {
		if(StringUtils.isBlank(pairingInstructions.getVideo())) {
			return new JLabel("No Video");
		}
		else {
			return Browser.link(pairingInstructions.getVideo()).getComponent();
		}
	}

}

