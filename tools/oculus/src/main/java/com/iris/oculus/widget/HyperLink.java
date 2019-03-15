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
package com.iris.oculus.widget;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JLabel;

import com.google.common.collect.ImmutableMap;
import com.iris.oculus.util.Actions;
import com.iris.oculus.util.BaseComponentWrapper;

public class HyperLink extends BaseComponentWrapper<JLabel> {
	private Action action;

	public HyperLink() {
		this(Actions.build("", () -> {}));
	}
	
	public HyperLink(String text) {
		this(Actions.build(text, () -> {}));
	}
	
	public HyperLink(Action action) {
		this.action = action;
	}
	
	@Override
	protected JLabel createComponent() {
		// TODO Auto-generated method stub
		JLabel component = new JLabel(getText());
		component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		component.setForeground(Color.BLUE);
		component.setFont(component.getFont().deriveFont(ImmutableMap.of(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON)));
		component.setBorder(BorderFactory.createEmptyBorder(1, 3, 1, 3));
		component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		component.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				action.actionPerformed(new ActionEvent(e.getSource(), e.getID(), "click"));
			}
		});
		action.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if(Action.NAME.equals(evt.getPropertyName())) {
					component.setText((String) evt.getNewValue());
				}
			}
		});
		return component;
	}

	public String getText() {
		return (String) action.getValue(Action.NAME);
	}

	public void setText(String text) {
		this.action.putValue(Action.NAME, text);
	}

}

