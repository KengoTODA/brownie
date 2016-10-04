CREATE TABLE video_uploaded_event (
  id UUID NOT NULL,
  uploaded_file_name text NOT NULL,
  resolutions text NOT NULL,
  generated timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
