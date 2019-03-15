CREATE TABLE IF NOT EXISTS zip_backup (
   key TEXT PRIMARY KEY, 
   value BLOB
);

INSERT INTO zip_backup (key, value) VALUES ('schema', '1');

