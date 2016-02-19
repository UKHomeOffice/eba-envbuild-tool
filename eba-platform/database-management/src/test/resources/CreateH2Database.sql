-- These statements can be used to initialise an empty H2 database
-- the unit test database is initialised to this level.
-- log in as the admin user using the web ui for H2, connect to the file database.
-- Copy and paste all the statements below.

-- This is a ficticious database purely used for unit testing
DROP SCHEMA IF EXISTS DB_MANAGEMENT;
DROP USER IF EXISTS DB_MANAGEMENT;
CREATE USER IF NOT EXISTS DB_MANAGEMENT password 'DB_MANAGEMENT';
ALTER USER DB_MANAGEMENT ADMIN TRUE;
CREATE SCHEMA IF NOT EXISTS DB_MANAGEMENT AUTHORIZATION DB_MANAGEMENT;

DROP TABLE IF EXISTS DB_MANAGEMENT.Test;
CREATE TABLE DB_MANAGEMENT.Test (
  id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (id)
) ;
