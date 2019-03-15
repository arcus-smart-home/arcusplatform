--------------------------------------------------------------------------------
-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! --
--
-- The statements in this file are executed on hub agent startup if the
-- key "schema" is <= 1 in the zip_config table.
--
-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! --
--------------------------------------------------------------------------------

--------------------------------------------------------------------------------
-- The zip_config table stores key/value pairs for the zip stack. It
-- is used to persist simple network properties.
--------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS zip_config (
   key TEXT PRIMARY KEY, 
   value TEXT
);

--------------------------------------------------------------------------------
-- The zip_node table stores all of the information about nodes that
-- have joined the system, including the hub itself. The primary key is the
-- assigned id of the node, which does not change for a given node.
--------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS zip_node (
   node_id INTEGER,

   basic_type INTEGER,
   generic_type INTEGER,
   specific_type INTEGER,

   man_id INTEGER,
   type_id INTEGER,
   product_id INTEGER,
   
   cmdclasses TEXT,
   online INTEGER DEFAULT 1,
   offline_timeout INTEGER DEFAULT 0,
   
   PRIMARY KEY (node_id ASC)
);

--------------------------------------------------------------------------------
-- Indexes required to make startup queries fast
--------------------------------------------------------------------------------

INSERT INTO zip_config (key, value) VALUES ('schema', '1');

