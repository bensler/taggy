DROP TABLE IF EXISTS entity_relationship;
DROP TABLE IF EXISTS entity_relationship_type;
DROP TABLE IF EXISTS property_blob;
DROP TABLE IF EXISTS property_entity;
DROP TABLE IF EXISTS property_integer;
DROP TABLE IF EXISTS property_string;
DROP TABLE IF EXISTS property_optional;
DROP TABLE IF EXISTS entity;
DROP TABLE IF EXISTS entity_property;
DROP TABLE IF EXISTS entity_type;
DROP TABLE IF EXISTS entity_property_type;

CREATE TABLE "entity_property_type" (
  "name" VARCHAR(128) PRIMARY KEY
);

CREATE TABLE "entity_type" (
  "name"        VARCHAR(128) PRIMARY KEY,
  "parent_name" INTEGER,
  FOREIGN KEY("parent_name") REFERENCES "entity_type"("name") ON DELETE RESTRICT
);

CREATE TABLE "entity_property" (
  "id"                        INTEGER      PRIMARY KEY,
  "entity_type_name"          VARCHAR(128) NOT NULL,
  "name"                      VARCHAR(128) NOT NULL,
  "entity_property_type_name" VARCHAR(128) NOT NULL,
  FOREIGN KEY("entity_type_name")          REFERENCES "entity_type"         ("name") ON DELETE RESTRICT,
  FOREIGN KEY("entity_property_type_name") REFERENCES "entity_property_type"("name") ON DELETE RESTRICT,
  UNIQUE("entity_type_name", "name")
);

CREATE TABLE "entity" (
  "id"               INTEGER      PRIMARY KEY,
  "entity_type_name" VARCHAR(128) NOT NULL,
  FOREIGN KEY("entity_type_name") REFERENCES "entity_type" ("name") ON DELETE RESTRICT
);

CREATE INDEX "idx_entity_id" ON "entity" ("id");

CREATE TABLE "property_optional" (
  "entity_id"          INTEGER      NOT NULL,
  "name"               VARCHAR(128) NOT NULL,
  "value"              VARCHAR(255) NOT NULL,
  PRIMARY KEY("entity_id", "name"),
  FOREIGN KEY("entity_id")          REFERENCES "entity"("id")          ON DELETE CASCADE
);

CREATE INDEX "idx_property_optional_entity_id" ON "property_optional" ("entity_id");

CREATE TABLE "property_string" (
  "entity_id"          INTEGER      NOT NULL,
  "entity_property_id" INTEGER      NOT NULL,
  "value"              VARCHAR(255) NOT NULL,
  PRIMARY KEY("entity_id", "entity_property_id"),
  FOREIGN KEY("entity_id")          REFERENCES "entity"("id")          ON DELETE CASCADE,
  FOREIGN KEY("entity_property_id") REFERENCES "entity_property"("id") ON DELETE RESTRICT
);

CREATE INDEX "idx_property_string_entity_id"          ON "property_string" ("entity_id");
CREATE INDEX "idx_property_string_entity_property_id" ON "property_string" ("entity_property_id");

CREATE TABLE "property_integer" (
  "entity_id"          INTEGER NOT NULL,
  "entity_property_id" INTEGER NOT NULL,
  "value"              INTEGER NOT NULL,
  PRIMARY KEY("entity_id", "entity_property_id"),
  FOREIGN KEY("entity_id")          REFERENCES "entity"("id")          ON DELETE CASCADE,
  FOREIGN KEY("entity_property_id") REFERENCES "entity_property"("id") ON DELETE RESTRICT
);

CREATE INDEX "idx_property_integer_entity_id"          ON "property_integer" ("entity_id");
CREATE INDEX "idx_property_integer_entity_property_id" ON "property_integer" ("entity_property_id");

CREATE TABLE "property_entity" (
  "entity_id"          INTEGER NOT NULL,
  "entity_property_id" INTEGER NOT NULL,
  "value"              INTEGER NOT NULL,
  PRIMARY KEY("entity_id", "entity_property_id"),
  FOREIGN KEY("entity_id")          REFERENCES "entity"("id")          ON DELETE CASCADE,
  FOREIGN KEY("entity_property_id") REFERENCES "entity_property"("id") ON DELETE RESTRICT,
  FOREIGN KEY("value")              REFERENCES "entity"("id")          ON DELETE CASCADE
);

CREATE INDEX "idx_property_entity_entity_id"          ON "property_entity" ("entity_id");
CREATE INDEX "idx_property_entity_entity_property_id" ON "property_entity" ("entity_property_id");
CREATE INDEX "idx_property_entity_value"              ON "property_entity" ("value");

CREATE TABLE "property_blob" (
  "entity_id"          INTEGER     NOT NULL,
  "entity_property_id" INTEGER     NOT NULL,
  "value"              VARCHAR(64) NOT NULL,
  PRIMARY KEY("entity_id", "entity_property_id"),
  FOREIGN KEY("entity_id")          REFERENCES "entity"("id")          ON DELETE CASCADE,
  FOREIGN KEY("entity_property_id") REFERENCES "entity_property"("id") ON DELETE RESTRICT
);

CREATE INDEX "idx_property_blob_entity_id"          ON "property_blob" ("entity_id");
CREATE INDEX "idx_property_blob_entity_property_id" ON "property_blob" ("entity_property_id");

CREATE TABLE "entity_relationship_type" (
  "name" VARCHAR(128) PRIMARY KEY
);

CREATE TABLE "entity_relationship" (
  "type_name"          VARCHAR(128) NOT NULL,
  "source_entity_id"   INTEGER      NOT NULL,
  "target_entity_id"   INTEGER      NOT NULL,
  PRIMARY KEY("type_name", "source_entity_id", "target_entity_id"),
  FOREIGN KEY("source_entity_id")   REFERENCES "entity"("id")                     ON DELETE CASCADE,
  FOREIGN KEY("target_entity_id")   REFERENCES "entity"("id")                     ON DELETE CASCADE,
  FOREIGN KEY("type_name")          REFERENCES "entity_relationship_type"("name") ON DELETE RESTRICT
);

CREATE INDEX "idx_entity_relationship_type_name"        ON "entity_relationship" ("type_name");
CREATE INDEX "idx_entity_relationship_source_entity_id" ON "entity_relationship" ("source_entity_id");
CREATE INDEX "idx_entity_relationship_target_entity_id" ON "entity_relationship" ("target_entity_id");

--INSERT INTO "entity_relationship_type" (
--  "name"
--) VALUES
-- ("tagged_with");

