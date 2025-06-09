INSERT INTO blob_property (blob_id, name, value) SELECT b.id, 'bin.filename' as propery_name, b.filename FROM "blob" b;

CREATE TABLE "new_blob" (
  "id"            INTEGER      PRIMARY KEY,
  "sha256sum"     VARCHAR(64)  NOT NULL,
  "thumbnail_sha" VARCHAR(64)  NOT NULL,
  "type"          VARCHAR(100) NOT NULL
);

INSERT INTO "new_blob" ("id", "sha256sum", "thumbnail_sha", "type") 
                  SELECT b.id, b.sha256sum, b.thumbnail_sha, b."type" FROM "blob" b;

DROP TABLE "blob";

ALTER TABLE "new_blob" RENAME TO "blob";
