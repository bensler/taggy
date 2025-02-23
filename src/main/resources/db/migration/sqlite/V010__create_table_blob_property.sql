
CREATE TABLE "blob_property" (
  "blob_id"   INTEGER      NOT NULL,
  "name"      TEXT         NOT NULL,
  "value"     TEXT         NOT NULL,
  PRIMARY KEY("blob_id", "name"),
  FOREIGN KEY("blob_id") REFERENCES "blob"("id") ON DELETE CASCADE
);

CREATE INDEX "idx_blob_property__blob_id" ON "blob_property" ( "blob_id" );
 