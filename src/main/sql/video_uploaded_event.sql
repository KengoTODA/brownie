CREATE TABLE video_uploaded_event (
  id UUID NOT NULL,
  uploaded_file_name text NOT NULL,
  resolutions text NOT NULL,
  generated timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE INDEX video_uploaded_event_index ON video_uploaded_event (generated);

EXPLAIN SELECT id, uploaded_file_name, resolutions, generated FROM video_uploaded_event ORDER BY generated DESC;
