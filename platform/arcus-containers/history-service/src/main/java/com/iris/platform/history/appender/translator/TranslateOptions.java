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
package com.iris.platform.history.appender.translator;

public class TranslateOptions {
	private final String template;
	private final boolean critical;
	
	private TranslateOptions(String template, boolean critical) {
		this.template = template;
		this.critical = critical;
	}
	
	public String getTemplate() {
		return template;
	}
	
	public boolean isCritical() {
		return critical;
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder {
		private String template = null;
		private boolean critical = false;
		
		public Builder withTemplate(String template) {
			this.template = template;
			return this;
		}
		
		public Builder withCritical(boolean critical) {
			this.critical = critical;
			return this;
		}
		
		public TranslateOptions build() {
			return new TranslateOptions(template, critical);
		}
	}
}

