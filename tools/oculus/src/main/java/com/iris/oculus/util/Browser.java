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
package com.iris.oculus.util;

import java.awt.Desktop;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;

import com.iris.oculus.Oculus;
import com.iris.oculus.widget.HyperLink;

public class Browser {

	public static void launch(String url) {
		try {
			Desktop.getDesktop().browse(new URI(url));
		}
		catch(URISyntaxException e) {
			Oculus.showError("Invalid Link " + url, e);
		}
		catch(Exception e) {
			Oculus.showError("Unable to Open Browser", e);
		}
	}
	
	public static HyperLink link(String url) {
		return new HyperLink(Actions.build(url, () -> Browser.launch(url)));
	}

	public static HyperLink link(String url, @Nullable String text) {
		return new HyperLink(Actions.build(StringUtils.isEmpty(text) ? url : text, () -> Browser.launch(url)));
	}
}

