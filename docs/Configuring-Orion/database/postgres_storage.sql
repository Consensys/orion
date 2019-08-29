/* Creates the database table needed when using PostgreSQL for Orion storage. */

CREATE TABLE store (
  key char(60),
  value bytea,
  primary key(key)
);