DROP SCHEMA cube CASCADE;
CREATE SCHEMA cube;

/*CREATE EXTENSION pgcrypto;*/

/*Taken from https://x-team.com/blog/automatic-timestamps-with-postgresql*/
CREATE OR REPLACE FUNCTION trigger_set_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE cube.cubeuser (
  id BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL,
  email TEXT NOT NULL,
  password TEXT NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TRIGGER set_timestamp_cubeuser
BEFORE UPDATE ON cube.cubeuser
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

CREATE TABLE cube.instance (
  id SERIAL PRIMARY KEY,
  name VARCHAR(30) NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE cube.app (
  id BIGSERIAL UNIQUE,
  customer_id BIGINT REFERENCES cube.cubeuser(id) ON DELETE CASCADE,
  instance_id INTEGER REFERENCES cube.instance(id) ON DELETE RESTRICT,
  name VARCHAR(200) NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  PRIMARY KEY (customer_id, instance_id, name)
);

CREATE TRIGGER set_timestamp_app
BEFORE UPDATE ON cube.app
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

CREATE TABLE cube.service (
  id BIGSERIAL UNIQUE,
  app_id  BIGINT REFERENCES cube.app(id) ON DELETE CASCADE,
  name VARCHAR(200) NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  PRIMARY KEY (app_id, name)
);

CREATE TRIGGER set_timestamp_service
BEFORE UPDATE ON cube.service
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

/* Thoughts on whether we can store the entire graph as a single json
(we really don't need to navigate nodes)
Just reconstruction of the graph is required at the UI end
Including the app id field so that the entire graph for an app can be retrieved in
one single select query
Also if we store in this format updates can be cumbersome*/
CREATE TABLE cube.servicegraph (
  parent_id BIGINT REFERENCES cube.service(id) ON DELETE SET NULL,
  child_id BIGINT REFERENCES cube.service(id) ON DELETE CASCADE,
  app_id BIGINT REFERENCES cube.app(id) ON DELETE CASCADE
);

/*Not sure whether to name it collection or recording*/
CREATE TABLE cube.recording (
  id BIGSERIAL UNIQUE,
  app_id BIGINT REFERENCES cube.app(id) ON DELETE CASCADE,
  collection_name VARCHAR(200) NOT NULL,
  status VARCHAR(50) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  completed_at TIMESTAMPTZ,
  PRIMARY KEY(app_id, collection_name)
);

CREATE TRIGGER set_timestamp_recording
BEFORE UPDATE ON cube.recording
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

/*can this collection be a range instead of a single collection*/
create TABLE cube.test (
  id BIGSERIAL UNIQUE,
  test_config_name TEXT NOT NULL,
  description TEXT,
  collection_id BIGINT REFERENCES cube.recording(id) ON DELETE CASCADE,
  gateway_service_id BIGINT REFERENCES cube.service(id) ON DELETE CASCADE,
  gateway_path_selection JSON,
  endpoint TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (collection_id , test_config_name)
);

CREATE TRIGGER set_timestamp_test
BEFORE UPDATE ON cube.test
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

create TABLE cube.test_virtualized_service (
  test_id BIGINT REFERENCES cube.test(id) ON DELETE CASCADE,
  service_id BIGINT REFERENCES cube.test(id) ON DELETE CASCADE,
  UNIQUE (test_id , service_id)
);

/*
Not creating a separate analysis table, including analysis as a json here itself
*/
CREATE TABLE cube.replay (
  id BIGSERIAL UNIQUE,
  replay_name VARCHAR(200) NOT NULL,
  test_id BIGINT REFERENCES cube.test(id) ON DELETE CASCADE,
  status VARCHAR(50) NOT NULL,
  req_count INTEGER,
  req_sent INTEGER,
  req_failed INTEGER,
  analysis JSON,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  completed_at TIMESTAMPTZ,
  sample_rate REAL,
  PRIMARY KEY(test_id, replay_name)
);

CREATE TRIGGER set_timestamp_replay
BEFORE UPDATE ON cube.replay
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

/*
Ideally would not want to keep any request response level information in postgresql
(Including ReqRespMatchResult)
*/
