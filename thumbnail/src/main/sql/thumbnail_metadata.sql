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

CREATE INDEX thumbnail_metadata_index ON thumbnail_metadata (generated);

CREATE EXTENSION pgcrypto;
EXPLAIN SELECT id, video_id, mime_type, content_length, width, height, milliseconds FROM thumbnail_metadata WHERE video_id = gen_random_uuid() ORDER BY generated DESC;
