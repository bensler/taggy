CREATE TABLE "tag" (
  "id"        INTEGER      PRIMARY KEY,
  "name"      VARCHAR(128) NOT NULL,
  "parent_id" INTEGER,
  UNIQUE("name", "parent_id"),
  FOREIGN KEY("parent_id") REFERENCES "tag"("id") ON DELETE RESTRICT
);

CREATE TABLE "blob" (
  "id"        INTEGER      PRIMARY KEY,
  "filename"  TEXT         UNIQUE NOT NULL
);

CREATE TABLE "blob_tag_xref" (
  "blob_id"   INTEGER      NOT NULL,
  "tag_id"    INTEGER      NOT NULL,
  PRIMARY KEY("blob_id", "tag_id"),
  FOREIGN KEY("blob_id") REFERENCES "blob"("id") ON DELETE CASCADE,
  FOREIGN KEY("tag_id")  REFERENCES "tag"("id")  ON DELETE CASCADE
);
