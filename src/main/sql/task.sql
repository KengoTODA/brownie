CREATE TABLE task (
  id UUID NOT NULL,
  uploaded_file_name text NOT NULL,
  resolutions text NOT NULL,
  generated timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
