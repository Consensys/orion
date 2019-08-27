/* Creates the database table needed when using Oracle DB for Orion storage. */

CREATE TABLE store (
  key varchar2(120) primary key,
  value blob
);