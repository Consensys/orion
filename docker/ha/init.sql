CREATE USER root;

CREATE DATABASE payloaddb;
GRANT ALL PRIVILEGES ON DATABASE payloaddb TO root;

\connect payloaddb
CREATE TABLE store (
  key char(60),
  value bytea,
  primary key(key)
);
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO root;


CREATE DATABASE knownnodes;
GRANT ALL PRIVILEGES ON DATABASE knownnodes TO root;

\connect knownnodes
CREATE TABLE store (
  key char(60),
  value bytea,
  primary key(key)
);
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO root;