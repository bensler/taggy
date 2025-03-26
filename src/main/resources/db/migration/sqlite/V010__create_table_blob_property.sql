
CREATE TABLE "blob_property" (
  "blob_id"   INTEGER      NOT NULL,
  "name"      TEXT         NOT NULL,
  "value"     TEXT         NOT NULL,
  PRIMARY KEY("blob_id", "name"),
  FOREIGN KEY("blob_id") REFERENCES "blob"("id") ON DELETE CASCADE
);

CREATE INDEX "idx_blob_property__blob_id" ON "blob_property" ( "blob_id" );
 
 
CREATE TABLE "tag_property" (
  "tag_id"    INTEGER      NOT NULL,
  "name"      TEXT         NOT NULL,
  "value"     TEXT         NOT NULL,
  PRIMARY KEY("tag_id", "name"),
  FOREIGN KEY("tag_id") REFERENCES "tag"("id") ON DELETE CASCADE
);

CREATE INDEX "idx_tag_property__tag_id" ON "tag_property" ( "tag_id" );

CREATE INDEX "idx_tag_property__name"   ON "tag_property" ( "name" );
 