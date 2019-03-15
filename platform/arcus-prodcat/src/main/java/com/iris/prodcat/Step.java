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
package com.iris.prodcat;

import java.util.ArrayList;
import java.util.List;

public class Step {

	public enum StepType { TEXT, INPUT, EXTERNAL_APP };
	
	private StepType type;
	private String img;
	private String text;
	private String subText;
	private String target;
	private String message;
	private boolean showInstallManual;
	private String linkText;
	private String linkUrl;
	private List<Input> inputs = new ArrayList<Input>();
	private List<ExternalApplication> apps = new ArrayList<ExternalApplication>();
	private int order;
	
	public StepType getType() {
		return type;
	}
	public void setType(StepType type) {
		this.type = type;
	}
	public String getImg() {
		return img;
	}
	public void setImg(String img) {
		this.img = img;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public String getSubText() {
		return subText;
	}
	public void setSubText(String subText) {
		this.subText = subText;
	}
	public String getTarget() {
		return target;
	}
	public void setTarget(String target) {
		this.target = target;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public List<Input> getInputs() {
		return inputs;
	}
	
	public void addInput(Input input) {
		this.inputs.add(input);
	}
	public void setInputs(List<Input> inputs){
		this.inputs = inputs;
	}
	
	public List<ExternalApplication> getExternalApplications() {
		return apps;
	}
	
	public void addExternalApplication(ExternalApplication app) {
		this.apps.add(app);
	}
	public void setExternalApplications(List<ExternalApplication> apps){
		this.apps = apps;
	}
	
	public boolean isShowInstallManual() {
		return showInstallManual;
	}
	public void setShowInstallManual(boolean showInstallManual) {
		this.showInstallManual = showInstallManual;
	}
	
	public String getLinkText() {
		return linkText;
	}
	public void setLinkText(String linkText) {
		this.linkText = linkText;
	}
	
	public String getLinkUrl() {
		return linkUrl;
	}
	public void setLinkUrl(String linkUrl) {
		this.linkUrl = linkUrl;
	}
	
	public int getOrder() {
		return order;
	}
	public void setOrder(int order) {
		this.order = order;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((img == null) ? 0 : img.hashCode());
		result = prime * result + ((inputs == null) ? 0 : inputs.hashCode());
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		result = prime * result + ((subText == null) ? 0 : subText.hashCode());
		result = prime * result + ((target == null) ? 0 : target.hashCode());
		result = prime * result + ((text == null) ? 0 : text.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((linkText == null) ? 0 : linkText.hashCode());
		result = prime * result + ((linkUrl == null) ? 0 : linkUrl.hashCode());
		result = prime * result + (showInstallManual ? 1231 : 1237);
		result = prime * result + ((apps == null) ? 0 : apps.hashCode());
		result = prime * result + order;
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Step other = (Step) obj;
		if (img == null) {
			if (other.img != null)
				return false;
		} else if (!img.equals(other.img))
			return false;
		if (inputs == null) {
			if (other.inputs != null)
				return false;
		} else if (!inputs.equals(other.inputs))
			return false;
		if (message == null) {
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
		if (subText == null) {
			if (other.subText != null)
				return false;
		} else if (!subText.equals(other.subText))
			return false;
		if (target == null) {
			if (other.target != null)
				return false;
		} else if (!target.equals(other.target))
			return false;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		if (type != other.type)
			return false;
		if (showInstallManual != other.showInstallManual)
			return false;
		if (linkText == null) {
			if (other.linkText != null)
				return false;
		} else if (!linkText.equals(other.linkText))
			return false;
		if (linkUrl == null) {
			if (other.linkUrl != null)
				return false;
		} else if (!linkUrl.equals(other.linkUrl))
			return false;
		if (apps == null) {
			if (other.apps != null)
				return false;
		} else if (!apps.equals(other.apps))
			return false;
		if(order != other.order) {
			return false;
		}
		return true;
	}
	@Override
	public String toString() {
		return "Step [type=" + type + ", img=" + img + ", text=" + text
				+ ", subText=" + subText + ", target=" + target + ", message="
				+ message + ", inputs=" + inputs 
				+ ", showInstallManual=" + showInstallManual
				+ ", linkText=" + linkText
				+ ", linkUrl=" + linkUrl
				+ ", apps=" + apps
				+ ", order=" + order
				+ "]";
	}
	
	
	
	

   
	
	
}



