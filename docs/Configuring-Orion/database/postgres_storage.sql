/* Creates the database table needed when using PostgreSQL for Orion storage. */

CREATE TABLE store (
  key bytea,
  value bytea,
  primary key(key)
);