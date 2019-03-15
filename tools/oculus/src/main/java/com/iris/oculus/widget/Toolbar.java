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

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

public class Toolbar {

	public static Toolbar.Builder builder() {
		return new Builder();
	}
	
	public static class Builder {
		private Region left = new Region(this);
		private Region right = new Region(this);
		private Region center = new Region(this);
		
		public Region left() {
			return left;
		}
		
		public Region right() {
			return right;
		}
		
		public Region center() {
			return center;
		}
		
		public JPanel build() {
			JPanel panel = new JPanel(new BorderLayout());
			if(!left.components.isEmpty()) {
				panel.add(left.build(), BorderLayout.WEST);
			}
			if(!center.components.isEmpty()) {
				JPanel centroid = new JPanel();
				centroid.setLayout(new BoxLayout(centroid, BoxLayout.X_AXIS));
				centroid.add(Box.createHorizontalGlue());
				for(Component c: center.components) {
					centroid.add(c);
				}
				centroid.add(Box.createHorizontalGlue());
				panel.add(centroid, BorderLayout.CENTER);
			}
			if(!right.components.isEmpty()) {
				panel.add(right.build(), BorderLayout.EAST);
			}
			return panel;
		}
	}
	
	public static class Region {
		private Builder parent;
		private List<Component> components = new ArrayList<>();
		
		private Region(Builder builder) {
			this.parent = builder;
		}
		
		public Builder addComponent(Component component) {
			components.add(component);
			return parent;
		}
		
		public Builder addButton(Action action) {
			return addComponent(new JButton(action));
		}
		
		private Component build() {
			JPanel panel = new JPanel();
			components.stream().forEach(panel::add);
			return panel;
		}
	}
}

