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
import java.awt.event.ItemEvent;
import java.beans.PropertyChangeEvent;
import java.util.Collection;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JToolTip;

import org.apache.commons.lang3.StringUtils;
import org.jdesktop.swingx.VerticalLayout;

import com.google.common.collect.ImmutableSet;
import com.iris.bootstrap.ServiceLocator;
import com.iris.client.bean.PairingCustomizationStep;
import com.iris.client.capability.PairingDevice;
import com.iris.client.event.ClientFuture;
import com.iris.client.event.ListenerRegistration;
import com.iris.client.event.Listeners;
import com.iris.messages.capability.ContactCapability;
import com.iris.oculus.modules.pairing.PairingDeviceController;
import com.iris.oculus.util.BaseComponentWrapper;
import com.iris.oculus.util.Documents;
import com.iris.oculus.widget.FieldWrapper;
import com.iris.oculus.widget.Fields;
import com.iris.oculus.widget.wizard.Wizard;
import com.iris.oculus.widget.wizard.Wizard.Transition;

/**
 * @author tweidlin
 *
 */
public class CustomizePage extends BaseComponentWrapper<Component> implements Transition<PairingInfo, PairingInfo> {
	private Wizard<?, ?> dialog;
	private PairingInfo input;
	private JLabel customizations = new JLabel("Custom");
	private ListenerRegistration reg;
	
	@Override
	public void update(Wizard<?, ?> dialog, PairingInfo input) {
		this.input = input;
		this.dialog = dialog;
		this.dialog.setNextEnabled(false);
		this.customizations.setText(String.format("Customizations: %s", StringUtils.join(input.getPairingDevice().getCustomizations(), ',')));
		dispose(); // reset UI
	}

	@Override
	public Component show(Wizard<?, ?> dialog) {
		Listeners.unregister(reg);
		this.reg = input.getPairingDevice().addListener(this::onPropertyChanged);
		dialog.setNextEnabled(true);
		return getComponent();
	}

	@Override
	public PairingInfo getValue() {
		return input;
	}

	@Override
	public ClientFuture<PairingInfo> commit() {
		return input.getPairingDevice().dismiss().onSuccess((e) -> reg = Listeners.unregister(reg)).transform((e) -> input);
	}

	@Override
	protected Component createComponent() {
		JPanel panel = new JPanel(new VerticalLayout());
		panel.add(customizations);
		for(Map<String, Object> data: input.getCustomizations().getSteps()) {
			panel.add(new JSeparator(JSeparator.HORIZONTAL));
			PairingCustomizationStep step = new PairingCustomizationStep(data);
			panel.add(new JLabel(String.format("%d) %s", step.getOrder(), step.getId())));
			panel.add(render(step));
		}
		return panel;
	}

	@SuppressWarnings("unchecked")
	private void onPropertyChanged(PropertyChangeEvent event) {
		if(PairingDevice.ATTR_CUSTOMIZATIONS.equals(event.getPropertyName())) {
			customizations.setText(String.format("Customizations: %s", StringUtils.join((Collection<String>) event.getNewValue(), ',')));
		}
	}
	
	private PairingDeviceController controller() {
		return ServiceLocator.getInstance(PairingDeviceController.class);
	}
	
	private Component render(PairingCustomizationStep step) {
		switch(step.getAction()) {
		case PairingCustomizationStep.ACTION_FAVORITE:
			return createFavoritePanel(step);
		case PairingCustomizationStep.ACTION_NAME:
			return createNamePanel(step);
		case PairingCustomizationStep.ACTION_CONTACT_TYPE:
			return createContactTypeSelector(step);
		case PairingCustomizationStep.ACTION_RULES:
			return createRuleActionPanel(step);
		default:
			return createDefaultActionPanel(step);
		}
	}
	
	private Component createRuleActionPanel(PairingCustomizationStep step) {
		JLabel label = new JLabel(String.format("<html><p>Action: %s</p><p>Title: %s</p><p>Header: %s</p><p>Rules:%s</p></html>", 
				step.getAction(), step.getTitle(), step.getHeader(),
				StringUtils.join( step.getChoices(), ',')));
		return label;
	}

	private Component createDefaultActionPanel(PairingCustomizationStep step) {
		JLabel label = new JLabel(String.format("<html><pre>Action: %s\nTitle: %s\nHeader: %s\nChoices: %s</pre></html>", step.getAction(), step.getTitle(), step.getHeader(), step.getChoices()));
		return label;
	}

	private Component createFavoritePanel(PairingCustomizationStep step) {
		JCheckBox box = new JCheckBox("Favorite");
		box.addChangeListener((e) -> controller().customizeFavorite(input.getPairingDevice(), box.isSelected()));
		return box;
	}

	private Component createNamePanel(PairingCustomizationStep step) {
		JTextField name = new JTextField();
		Documents.addDocumentChangeListener(name.getDocument(), (e) -> controller().customizeName(input.getPairingDevice(), name.getText()));
		return name;
	}

	private Component createContactTypeSelector(PairingCustomizationStep step) {
		FieldWrapper<JComboBox<String>, String> combo =
			Fields
				.<String>comboBoxBuilder()
				.withValues(ImmutableSet.<String>of(ContactCapability.USEHINT_DOOR, ContactCapability.USEHINT_WINDOW, ContactCapability.USEHINT_OTHER))
				.noteditable()
				.withRenderer((String useHint) -> {
					if(useHint == null) {
						return "Not Set";
					}
					switch(useHint) {
					case ContactCapability.USEHINT_DOOR:
						return "Door";
					case ContactCapability.USEHINT_WINDOW:
						return "Window";
					case ContactCapability.USEHINT_OTHER:
						return "Other";
					case ContactCapability.USEHINT_UNKNOWN:
					default:
						return "Not Set";
					}
				}, "Not Set")
				.labelled("Contact Type")
				.build();
		combo.getComponent().addItemListener((ItemEvent e) -> {
			if(e.getStateChange() == ItemEvent.SELECTED) {
				controller().customizeContactType(input.getPairingDevice(), combo.getValue());
			}
		});
		return combo.getComponent();
	}

}

