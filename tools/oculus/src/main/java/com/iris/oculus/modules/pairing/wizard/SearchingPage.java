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

import java.awt.BorderLayout;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.util.Date;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.google.common.collect.ImmutableMap;
import com.iris.bootstrap.ServiceLocator;
import com.iris.client.capability.PairingSubsystem;
import com.iris.client.event.ClientFuture;
import com.iris.client.event.ListenerRegistration;
import com.iris.client.event.Listeners;
import com.iris.client.model.PairingDeviceModel;
import com.iris.oculus.modules.pairing.PairingDeviceController;
import com.iris.oculus.util.Actions;
import com.iris.oculus.util.BaseComponentWrapper;
import com.iris.oculus.util.DefaultSelectionModel;
import com.iris.oculus.util.SelectionModel;
import com.iris.oculus.widget.Fields;
import com.iris.oculus.widget.FormView;
import com.iris.oculus.widget.table.Table;
import com.iris.oculus.widget.table.TableModel;
import com.iris.oculus.widget.table.TableModelBuilder;
import com.iris.oculus.widget.wizard.Wizard;
import com.iris.oculus.widget.wizard.Wizard.Transition;

/**
 * @author tweidlin
 *
 */
public class SearchingPage extends BaseComponentWrapper<Component> implements Transition<PairingInfo, PairingInfo> {
	private Wizard<?, ?> dialog;
	private PairingInfo input;
	
	private ListenerRegistration reg = Listeners.unregistered();
	private SelectionModel<PairingDeviceModel> selectionModel = new DefaultSelectionModel<>();
	private FormView status;
	
	public SearchingPage() {
		selectionModel.addSelectionListener((e) -> onSelected());
	}
	
	@Override
	public void update(Wizard<?, ?> dialog, PairingInfo input) {
		this.input = input;
		this.dialog = dialog;
	}

	@Override
	public Component show(Wizard<?, ?> dialog) {
		this.dialog.setNextEnabled(selectionModel.hasSelection());
		ServiceLocator.getInstance(PairingDeviceController.class).reload();
		Component c = getComponent();
		Listeners.unregister(reg);
		reg = input.getPairingSubsystem().addListener(this::onPairingSubsystemChange);
		syncStatus();
		return c;
	}

	@Override
	public PairingInfo getValue() {
		return input;
	}

	@Override
	protected Component createComponent() {
		JPanel panel = new JPanel(new BorderLayout());
		
		panel.add(createStatusPanel(), BorderLayout.NORTH);
		panel.add(createPairingQueue(), BorderLayout.CENTER);
		return panel;
	}

	@Override
	public ClientFuture<PairingInfo> commit() {
		PairingDeviceModel model = selectionModel.getSelectedItem().get();
		return ServiceLocator.getInstance(PairingDeviceController.class).customize(model).transform((r) -> {
			reg = Listeners.unregister(reg);
			input.setCustomizations(r);
			input.setPairingDevice(model);
			return input;
		});
	}

	private Component createStatusPanel() {
		status = new FormView();
		status.addField(
				Fields
					.textFieldBuilder()
					.notEditable()
					.named(PairingSubsystem.ATTR_PAIRINGMODE)
					.labelled("Pairing Mode: ")
					.build()
		);
		status.addField(
				Fields
					.textFieldBuilder()
					.notEditable()
					.transform(Fields::stringToDate, Fields::dateToString)
					.named(PairingSubsystem.ATTR_SEARCHIDLETIMEOUT)
					.labelled("Help Timeout: ")
					.build()
		);
		status.addField(
				Fields
					.textFieldBuilder()
					.notEditable()
					.transform(Fields::stringToDate, Fields::dateToString)
					.named(PairingSubsystem.ATTR_SEARCHTIMEOUT)
					.labelled("Pairing Timeout: ")
					.build()
		);
		return status.getComponent();
	}

	private Component createPairingQueue() {
		TableModel<PairingDeviceModel> model =
			TableModelBuilder
				.builder(ServiceLocator.getInstance(PairingDeviceController.class).getStore())
				.columnBuilder()
					.withName("Product")
					.withGetter(PairingDeviceModel::getProductAddress)
					.add()
				.columnBuilder()
					.withName("Pairing State")
					.withGetter(PairingDeviceModel::getPairingState)
					.add()
				.columnBuilder()
					.withName("Pairing Phase")
					.withGetter(PairingDeviceModel::getPairingPhase)
					.add()
				.build();
		Table<PairingDeviceModel> table = new Table<>(model);
		table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if(e.getValueIsAdjusting()) {
					return;
				}
				int selected = table.getSelectedRow();
				if(selected == -1) {
					selectionModel.clearSelection();
				}
				else {
					selectionModel.setSelection(table.getModel().getValue(selected));
				}
			}
		});
		return new JScrollPane(table);
	}

	private void onSelected() {
		if(dialog != null) {
			dialog.setNextEnabled(selectionModel.hasSelection());
		}
	}

	private void syncStatus() {
		status.setValues(ImmutableMap.of(
				PairingSubsystem.ATTR_PAIRINGMODE, input.getPairingSubsystem().get(PairingSubsystem.ATTR_PAIRINGMODE),
				PairingSubsystem.ATTR_SEARCHIDLETIMEOUT, new Date((long) input.getPairingSubsystem().get(PairingSubsystem.ATTR_SEARCHIDLETIMEOUT)),
				PairingSubsystem.ATTR_SEARCHTIMEOUT, new Date((long) input.getPairingSubsystem().get(PairingSubsystem.ATTR_SEARCHTIMEOUT))
		));
	}
	
	private void onPairingSubsystemChange(PropertyChangeEvent event) {
		switch(event.getPropertyName()) {
		case PairingSubsystem.ATTR_PAIRINGMODE:
			if(PairingSubsystem.PAIRINGMODE_IDLE.equals(event.getNewValue())) {
				searchTimeout();
			}
			// fall through
		case PairingSubsystem.ATTR_SEARCHIDLETIMEOUT:
		case PairingSubsystem.ATTR_SEARCHTIMEOUT:
			syncStatus();
			break;
			
		case PairingSubsystem.ATTR_SEARCHIDLE:
			if(Boolean.TRUE.equals(event.getNewValue())) {
				searchIdle();
			}
			break;
		}
	}
	
	private void searchIdle() {
		dialog.setErrorMessage("This is taking longer than usual", Actions.build("Show Help", this::showHelp));
	}
	
	private void searchTimeout() {
		dialog.setErrorMessage("Search has timed out", Actions.build("<< Search Again", this::goBack));
	}
	
	private void showHelp() {
		ServiceLocator.getInstance(PairingDeviceController.class).showHelpSteps();
	}
	
	private void goBack() {
		this.dialog.prev();
	}
}

