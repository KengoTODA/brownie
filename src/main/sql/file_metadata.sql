CREATE TABLE file_metadata (
  id UUID NOT NULL,
  name text NOT NULL,
  mime_type text NOT NULL,
  content_length integer,
  generated timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
