/* Creates the database table needed when using Oracle DB for Orion storage. */

CREATE TABLE store (
  key char(60) primary key,
  value blob
);