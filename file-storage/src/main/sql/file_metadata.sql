CREATE TABLE file_metadata (
  id UUID NOT NULL,
  name text NOT NULL,
  mime_type text NOT NULL,
  content_length integer,
  generated timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE INDEX file_metadata_index ON file_metadata (generated);

EXPLAIN SELECT id, name, mime_type, content_length, generated from file_metadata ORDER BY generated DESC;
