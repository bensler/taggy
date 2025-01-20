
ALTER TABLE "blob" 
  RENAME COLUMN "type" TO "typeold";

ALTER TABLE "blob" 
  ADD COLUMN "type" VARCHAR(100) NOT NULL DEFAULT 'x';

UPDATE "blob" SET "type"="typeold";

ALTER TABLE "blob"
  DROP COLUMN "typeold";

