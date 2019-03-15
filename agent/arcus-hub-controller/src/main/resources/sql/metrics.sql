--------------------------------------------------------------------------------
-- SQL Schema for metrics
-- NOTE: There are no foreign keys for performance reasons
-- NOTE: There are no indexes for storage footprint reasons
--------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS metric (
   id INTEGER,
   name TEXT,

   PRIMARY KEY (id ASC)
);

CREATE TABLE IF NOT EXISTS counter (
   name INTEGER, 
   timestamp INTEGER,
   value INTEGER,

   FOREIGN KEY (name) REFERENCES metric(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS gauge (
   name INTEGER, 
   timestamp INTEGER,
   value INTEGER, -- May be type other than INTEGER

   FOREIGN KEY (name) REFERENCES metric(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS histogram (
   name INTEGER, 
   timestamp INTEGER,

   FOREIGN KEY (name) REFERENCES metric(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS timer (
   name INTEGER, 
   timestamp INTEGER,

   FOREIGN KEY (name) REFERENCES metric(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS meter (
   name INTEGER, 
   timestamp INTEGER,

   FOREIGN KEY (name) REFERENCES metric(id) ON DELETE CASCADE
);
