CREATE TABLE zigbee_attribute (
   id INTEGER,
   clusterId INTEGER,
   attributeId INTEGER,
   attributeDt INTEGER,
   attributeLastValue BLOB,

   PRIMARY KEY (id ASC),
   FOREIGN KEY (clusterId) REFERENCES zigbee_cluster(id) ON DELETE CASCADE
);
CREATE INDEX ix_zigbee_attribute_cluster_id ON zigbee_attribute (clusterId);

CREATE TABLE zigbee_cluster (
   id INTEGER,
   endpointId INTEGER,
   clusterId INTEGER,
   server INTEGER,

   PRIMARY KEY (id ASC),
   FOREIGN KEY (endpointId) REFERENCES zigbee_endpoint(id) ON DELETE CASCADE
);
CREATE INDEX ix_zigbee_cluster_endpoint_id ON zigbee_cluster (endpointId);

CREATE TABLE zigbee_config (
   key TEXT PRIMARY KEY,
   value TEXT
);

CREATE TABLE zigbee_endpoint (
   id INTEGER,
   profileId INTEGER,
   endpointId INTEGER,

   -- From Zigbee Simple Descriptor
   deviceId INTEGER,
   deviceVersion INTEGER,

   -- From Zigbee Basic Cluster
   zclVersion INTEGER,
   appVersion INTEGER,
   stkVersion INTEGER,
   hwVersion INTEGER,
   manufacturerName TEXT,
   modelIdentifier TEXT,
   dateCode TEXT,
   powerSource INTEGER,

   PRIMARY KEY (id ASC),
   FOREIGN KEY (profileId) REFERENCES zigbee_profile(id) ON DELETE CASCADE
);
CREATE INDEX ix_zigbee_endpoint_profile_id ON zigbee_endpoint (profileId);

CREATE TABLE zigbee_node (
   ieeeAddr INTEGER,
   nwkAddr INTEGER,
   parentAddr INTEGER,
   state INTEGER,

   -- From Zigbee Node Descriptor
   maximumIncomingTransferSize INTEGER,
   maximumOutgoingTransferSize INTEGER,
   nodeFlags INTEGER,
   serverMask INTEGER,
   manufacturerCode INTEGER,
   descriptorCapability INTEGER,
   maximumBufferSize INTEGER,
   macCapabilityFlags INTEGER,

   -- From Zigbee Power Descriptor
   powerDescriptor INTEGER,

   -- From Zigbee Device Announce
   deviceCapability INTEGER, online INTEGER DEFAULT 1, offlineTimeout INTEGER DEFAULT 0,

   PRIMARY KEY (ieeeAddr ASC)
);

CREATE TABLE zigbee_profile (
   id INTEGER,
   nodeId INTEGER,
   profileId INTEGER,

   PRIMARY KEY (id ASC),
   FOREIGN KEY (nodeId) REFERENCES zigbee_node(ieeeAddr) ON DELETE CASCADE
);
CREATE INDEX ix_zigbee_profile_node_id ON zigbee_profile (nodeId);
