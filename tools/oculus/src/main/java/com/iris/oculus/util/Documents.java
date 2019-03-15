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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import com.iris.client.event.Listener;
import com.iris.util.Subscription;

public class Documents {

	public static DocumentSubscription addDocumentChangeListener(Document document, Runnable listener) {
		return addDocumentChangeListener(document, (d) -> listener.run());
	}
	
	public static DocumentSubscription addDocumentChangeListener(Document document, Listener<Document> listener) {
		return new DocumentSubscriptionImpl(document, listener);
	}
	
	public interface DocumentSubscription extends Subscription {
		
		DocumentSubscription debounce(int delayMs);
	}
	
	private static class DocumentSubscriptionImpl implements ActionListener, DocumentListener, DocumentSubscription {
		private final Document document;
		private final Listener<Document> listener;
		private Timer timer = null;

		DocumentSubscriptionImpl(Document document, Listener<Document> listener) {
			this.document = document;
			this.listener = listener;
			this.document.addDocumentListener(this);
		}
		
		private void defer() {
			if(timer == null) {
				fire();
			}
			else {
				timer.restart();
			}
		}
		
		private void fire() {
			listener.onEvent(document);
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			fire();
		}

		@Override
		public void remove() {
			document.removeDocumentListener(this);
		}

		@Override
		public DocumentSubscription debounce(int delayMs) {
			if(timer == null) {
				timer = new Timer(delayMs, this);
				timer.setRepeats(false);
			}
			else {
				timer.setDelay(delayMs);
			}
			return this;
		}

		@Override
		public void insertUpdate(DocumentEvent e) {
			defer();
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			defer();
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
			defer();
		}
		
	}
}

