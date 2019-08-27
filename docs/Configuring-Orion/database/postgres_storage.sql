/* Creates the database table needed when using PostgreSQL for Orion storage. */

CREATE TABLE store (
  key varchar(120),
  value bytea,
  primary key(key)
);