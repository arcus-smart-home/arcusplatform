CREATE TABLE IF NOT EXISTS config (key TEXT PRIMARY KEY, value TEXT); 
CREATE TABLE IF NOT EXISTS migration (key TEXT PRIMARY KEY, value TEXT); 

INSERT INTO config (key, value) VALUES ('schema', '1');
