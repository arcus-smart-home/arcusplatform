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
package com.iris.client.model.device;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.iris.Utils;
import com.iris.messages.MessageBody;

public class ClientDeviceModel {

   public interface Base {
      public static final String NAMESPACE = "base";
      public static final String ATTR_ID = Utils.namespace(NAMESPACE, "id");
      public static final String ATTR_TAGS = Utils.namespace(NAMESPACE, "tags");
      public static final String ATTR_ADDRESS = Utils.namespace(NAMESPACE, "address");
      public static final String ATTR_CAPS = Utils.namespace(NAMESPACE, "caps");
      public static final String ATTR_TYPE = Utils.namespace(NAMESPACE, "type");
      public static final String ATTR_IMAGES = Utils.namespace(NAMESPACE, "images");
   }

	public interface DeviceAdvanced {
	   public static final String NAMESPACE = "devadv";
	   public static final String ATTR_DRIVERNAME = Utils.namespace(NAMESPACE, "drivername");
	   public static final String ATTR_DRIVERVERSION = Utils.namespace(NAMESPACE, "driverversion");
	   public static final String ATTR_PROTOCOL = Utils.namespace(NAMESPACE, "protocol");
	   public static final String ATTR_SUBPROTOCOL = Utils.namespace(NAMESPACE, "subprotocol");
	   public static final String ATTR_PROTOCOLID = Utils.namespace(NAMESPACE, "protocolid");
	   public static final String ATTR_ADDED = Utils.namespace(NAMESPACE, "added");

	}

	public interface DeviceBase {
	    public static final String NAMESPACE = "dev";
	    public static final String ATTR_ACCOUNT = Utils.namespace(NAMESPACE, "account");
	    public static final String ATTR_DEVTYPEHINT = Utils.namespace(NAMESPACE, "devtypehint");
	    public static final String ATTR_NAME = Utils.namespace(NAMESPACE, "name");
	    public static final String ATTR_VENDOR = Utils.namespace(NAMESPACE, "vendor");
	    public static final String ATTR_MODEL = Utils.namespace(NAMESPACE, "model");
	    public static final String ATTR_PRODUCTID = Utils.namespace(NAMESPACE, "productid");
	    public static final String ARG_GET_ATTRIBUTES_NAMES = "names";
	    public static final String RET_GET_ATTRIBUTES = "response";
	    public static final String NAME_GET_ATTRIBUTES = "GetAttributes";
	    public static final String EVENT_GET_ATTRIBUTES_RESPONSE = Utils.namespace(NAMESPACE, "GetAttributesResponse");
	}

	public interface HubBase {
		public static final String NAMESPACE = "hub";
		public static final String ATTR_ACCOUNT = Utils.namespace(NAMESPACE, "account");
		public static final String ATTR_PLACE = Utils.namespace(NAMESPACE, "place");
		public static final String ATTR_NAME = Utils.namespace(NAMESPACE, "name");
		public static final String ATTR_VENDOR = Utils.namespace(NAMESPACE, "vendor");
		public static final String ATTR_MODEL = Utils.namespace(NAMESPACE, "model");
		public static final String ATTR_STATE = Utils.namespace(NAMESPACE, "state");
	}

    private String id;
    private long created;
    private long modified;
    private Set<String> tags;
    private String state;
    private String accountId;
    private String protocol;
    private String protocolId;
    private String driverName;
    private String driverVersion;
    private String driverAddress;
    private String protocolAddress;
    private String hubId;
    private String placeId;
    private Set<String> caps = new HashSet<>();
    private String devtypeHint;
    private String name;
    private Map<String,String> images = new HashMap<>();
    private String vendor;
    private String model;
    private String productId;
    private String subprotocol;
    private long lastUpdate;
    private final Map<String,Object> attributes = new HashMap<>();

    public static ClientDeviceModel fromPlatformDevice(com.iris.messages.model.Device platformDevice) {
        ClientDeviceModel device = new ClientDeviceModel();
        device.accountId = platformDevice.getAccount().toString();
        device.id = platformDevice.getId().toString();
        device.protocol = platformDevice.getProtocol();
        device.protocolId = platformDevice.getProtocolid();

        if(platformDevice.getCreated() != null) {
            device.created = platformDevice.getCreated().getTime();
        }

        if(platformDevice.getModified() != null) {
            device.modified = platformDevice.getModified().getTime();
        }
        device.tags = platformDevice.getTags();
        device.state = platformDevice.getState();
        device.driverName = platformDevice.getDriverId() == null ? null : platformDevice.getDriverId().getName();

        if(platformDevice.getDriverId() != null) {
            device.driverName = platformDevice.getDriverId().getName();
            device.driverVersion = platformDevice.getDriverId().getVersion().getRepresentation();
        }
        device.driverAddress = platformDevice.getDriverAddress();
        device.protocolAddress = platformDevice.getProtocolAddress();
        device.hubId = platformDevice.getHubId() == null ? null : platformDevice.getHubId().toString();
        device.placeId = platformDevice.getPlace() == null ? null : platformDevice.getPlace().toString();
        device.caps = platformDevice.getCaps();
        device.devtypeHint = platformDevice.getDevtypehint();

        // FIXME:  not sure what we want to do here, but for now to distinguish devices, we'll use
        // the protocol ID for the name if the device doesn't have one
        device.name = platformDevice.getName() == null ? platformDevice.getProtocolid() : platformDevice.getName();
        if(platformDevice.getImages() != null) {
           for(Map.Entry<String,UUID> imageEntry : platformDevice.getImages().entrySet()) {
              device.images.put(imageEntry.getKey(), imageEntry.getValue().toString());
           }
        }

        device.vendor = platformDevice.getVendor();
        device.model = platformDevice.getModel();
        device.productId = platformDevice.getProductId();
        device.subprotocol = platformDevice.getSubprotocol();
        return device;
    }

    public static ClientDeviceModel fromHubAddedEvent(MessageBody event) {
    	ClientDeviceModel model = new ClientDeviceModel();
    	Map<String, Object> attribs = event.getAttributes();
    	model.setId(attribs.get(Base.ATTR_ID).toString());
    	model.setPlaceId(attribs.get(HubBase.ATTR_PLACE).toString());
    	model.setDevtypeHint("hub");

    	if (attribs.get(Base.ATTR_IMAGES) != null) {
    	   model.setImages((Map<String,String>) attribs.get(Base.ATTR_IMAGES));
    	}
    	if (attribs.get(HubBase.ATTR_MODEL) != null) {
    		model.setModel(attribs.get(HubBase.ATTR_MODEL).toString());
    	}
    	if (attribs.get(HubBase.ATTR_NAME) != null) {
    		model.setName(attribs.get(HubBase.ATTR_NAME).toString());
    	}
    	if (attribs.get(HubBase.ATTR_STATE) != null) {
    		model.setState(attribs.get(HubBase.ATTR_STATE).toString());
    	}
    	if (attribs.get(HubBase.ATTR_VENDOR) != null) {
    		model.setVendor(attribs.get(HubBase.ATTR_VENDOR).toString());
    	}
    	
    	return model;
    }

    public ClientDeviceModel() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getProtocolId() {
        return protocolId;
    }

    public void setProtocolId(String protocolId) {
        this.protocolId = protocolId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public long getModified() {
        return modified;
    }

    public void setModified(long modified) {
        this.modified = modified;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getDriverVersion() {
        return driverVersion;
    }

    public void setDriverVersion(String driverVersion) {
        this.driverVersion = driverVersion;
    }

    public String getDriverAddress() {
        return driverAddress;
    }

    public void setDriverAddress(String driverAddress) {
        this.driverAddress = driverAddress;
    }

    public String getProtocolAddress() {
        return protocolAddress;
    }

    public void setProtocolAddress(String protocolAddress) {
        this.protocolAddress = protocolAddress;
    }

    public String getHubId() {
        return hubId;
    }

    public void setHubId(String hubId) {
        this.hubId = hubId;
    }

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    public Set<String> getCaps() {
        return caps;
    }

    public void setCaps(Set<String> caps) {
        this.caps = caps;
    }

    public String getDevtypeHint() {
        return devtypeHint;
    }

    public void setDevtypeHint(String devtypeHint) {
        this.devtypeHint = devtypeHint;
    }
    
    public String getProductId() {
    	return productId;
    }
    
    public void getProductId(String productId) {
    	this.productId = productId;
    }

    public Map<String,String> getImages() {
        return images;
    }

    public void setImages(Map<String,String> image) {
        this.images = images;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSubprotocol() {
        return subprotocol;
    }

    public void setSubprotocol(String subprotocol) {
        this.subprotocol = subprotocol;
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public void putAttribute(String key, Object value) {
        internalPut(key, value);
    }

    @SuppressWarnings("unchecked")
    private void internalPut(String name, Object value) {
        if(Base.ATTR_CAPS.equals(name)) {
            caps.addAll((Collection<String>)value);
        } else if(DeviceBase.ATTR_DEVTYPEHINT.equals(name)) {
            devtypeHint = String.valueOf(value);
        } else if(DeviceBase.ATTR_ACCOUNT.equals(name)) {
            accountId = String.valueOf(value);
        } else if(Base.ATTR_ID.equals(name)) {
            id = String.valueOf(value);
        } else if(DeviceBase.ATTR_MODEL.equals(name)) {
            model = String.valueOf(value);
        } else if(DeviceBase.ATTR_PRODUCTID.equals(name)) {
            productId = String.valueOf(value);
        } else if(DeviceBase.ATTR_NAME.equals(name)) {
            this.name = String.valueOf(value);
        } else if(DeviceBase.ATTR_VENDOR.equals(name)) {
            vendor = String.valueOf(value);
        } else if(DeviceAdvanced.ATTR_DRIVERNAME.equals(name)) {
            driverName = String.valueOf(value);
        } else if(DeviceAdvanced.ATTR_PROTOCOL.equals(name)) {
            protocol = String.valueOf(value);
        } else if(DeviceAdvanced.ATTR_PROTOCOLID.equals(name)) {
            protocolId = String.valueOf(value);
        } else if(DeviceAdvanced.ATTR_SUBPROTOCOL.equals(name)) {
            subprotocol = String.valueOf(value);
        } else {
            attributes.put(name, value);
        }
    }

    public void putAttributes(Map<String,Object> attributes) {
        for(Map.Entry<String,Object> entry : attributes.entrySet()) {
            internalPut(entry.getKey(), entry.getValue());
        }
    }

    public void removeAttribute(String key) {
        attributes.remove(key);
    }

    public void clearAttributes() {
        attributes.clear();
    }

    @Override
    public String toString() {
        return "Device{" +
                "id='" + id + '\'' +
                ", created=" + created +
                ", modified=" + modified +
                ", tags=" + tags +
                ", state='" + state + '\'' +
                ", accountId='" + accountId + '\'' +
                ", protocol='" + protocol + '\'' +
                ", protocolId='" + protocolId + '\'' +
                ", driverName='" + driverName + '\'' +
                ", driverVersion=" + driverVersion +
                ", driverAddress='" + driverAddress + '\'' +
                ", protocolAddress='" + protocolAddress + '\'' +
                ", hubId='" + hubId + '\'' +
                ", placeId='" + placeId + '\'' +
                ", caps=" + caps +
                ", devtypeHint='" + devtypeHint + '\'' +
                ", name='" + name + '\'' +
                ", images='" + images + '\'' +
                ", vendor='" + vendor + '\'' +
                ", model='" + model + '\'' +
                ", productid='" + productId + '\'' +
                ", subprotocol='" + subprotocol + '\'' +
                ", lastUpdate=" + lastUpdate +
                ", attributes=" + attributes +
                '}';
    }
}

