DROP TABLE IF EXISTS ReadOnlyIdEntity;
DROP TABLE IF EXISTS PrimitiveIdEntity;
CREATE TABLE ReadOnlyIdEntity (ID BIGINT IDENTITY PRIMARY KEY, NAME VARCHAR(100));
CREATE TABLE PrimitiveIdEntity (ID BIGINT IDENTITY PRIMARY KEY, NAME VARCHAR(100));
