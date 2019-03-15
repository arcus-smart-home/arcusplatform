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
package com.iris.oculus.modules.video.dialog;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.eclipse.jdt.annotation.Nullable;

import com.iris.client.event.ClientFuture;
import com.iris.oculus.modules.video.VideoFilter;
import com.iris.oculus.modules.video.ux.VideoFilterForm;
import com.iris.oculus.widget.Dialog;

public class VideoFilterDialog extends Dialog<VideoFilter> {

	public static ClientFuture<VideoFilter> prompt(@Nullable VideoFilter filter) {
		return new VideoFilterDialog(filter).prompt();
	}

	private VideoFilterForm form = new VideoFilterForm();
	
	private VideoFilterDialog(@Nullable VideoFilter filter) {
		if(filter != null) {
			form.setValues(filter);
		}
	}
	
	@Override
	protected VideoFilter getValue() {
		return form.toFilter();
	}

	@Override
	protected Component createContents() {
		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
		buttons.add(Box.createHorizontalGlue());
		buttons.add(new JButton(cancelAction()));
		buttons.add(new JButton(submitAction("Apply")));

		JPanel contents = new JPanel(new BorderLayout());
		contents.add(form.getComponent(), BorderLayout.CENTER);
		contents.add(buttons, BorderLayout.SOUTH);
		return contents;
	}

}

