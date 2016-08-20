CREATE TABLE thumbnail_metadata (
  id UUID NOT NULL,
  video_id UUID NOT NULL,
  mime_type text NOT NULL,
  content_length integer,
  width integer,
  height integer,
  milliseconds integer,
  generated timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
