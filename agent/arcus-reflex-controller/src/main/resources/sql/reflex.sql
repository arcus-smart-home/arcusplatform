CREATE TABLE reflexconfig (key TEXT PRIMARY KEY, value TEXT);
CREATE TABLE reflexes (addr TEXT, key TEXT, value TEXT, PRIMARY KEY(addr,key) ON CONFLICT REPLACE);
CREATE TABLE drivers (addr TEXT, key TEXT, value TEXT, PRIMARY KEY(addr,key) ON CONFLICT REPLACE);

INSERT INTO reflexconfig (key, value) VALUES ('schema', '1');
INSERT INTO reflexconfig (key, value) VALUES ('reflexesschema', '1');
INSERT INTO reflexconfig (key, value) VALUES ('driversschema', '1');
