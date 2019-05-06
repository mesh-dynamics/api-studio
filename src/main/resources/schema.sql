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

CREATE TYPE cube.instace_name AS ENUM ('Prod' , 'Dev' , 'Staging');

CREATE TABLE cube.instance (
  id SERIAL PRIMARY KEY,
  name cube.instace_name NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE cube.app (
  id BIGSERIAL PRIMARY KEY,
  customer_id BIGINT REFERENCES cube.cubeuser(id) ON DELETE CASCADE,
  instance_id INTEGER REFERENCES cube.instance(id) ON DELETE RESTRICT,
  name VARCHAR(200) NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE (customer_id, instance_id, name)
);

CREATE INDEX app_index ON cube.app(customer_id, instance_id);

CREATE TRIGGER set_timestamp_app
BEFORE UPDATE ON cube.app
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

CREATE TABLE cube.service (
  id BIGSERIAL PRIMARY KEY,
  app_id  BIGINT REFERENCES cube.app(id) ON DELETE CASCADE,
  name VARCHAR(200) NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE (app_id, name)
);

CREATE INDEX service_index ON cube.service(app_id);

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
CREATE TABLE cube.service_graph (
  parent_id BIGINT REFERENCES cube.service(id) ON DELETE SET NULL,
  child_id BIGINT REFERENCES cube.service(id) ON DELETE CASCADE,
  app_id BIGINT REFERENCES cube.app(id) ON DELETE CASCADE
);

CREATE INDEX service_graph_index ON cube.service_graph(app_id);

CREATE TYPE cube.recording_status AS ENUM ('Running' , 'Completed' , 'Error');

/*Not sure whether to name it collection or recording*/
CREATE TABLE cube.recording (
  id BIGSERIAL PRIMARY KEY,
  app_id BIGINT REFERENCES cube.app(id) ON DELETE CASCADE,
  collection_name VARCHAR(200) NOT NULL,
  status cube.recording_status NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  completed_at TIMESTAMPTZ,
  UNIQUE(app_id, collection_name)
);

CREATE INDEX recording_index ON cube.recording(app_id, status);

CREATE TRIGGER set_timestamp_recording
BEFORE UPDATE ON cube.recording
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

/*can this collection be a range instead of a single collection*/
create TABLE cube.test (
  id BIGSERIAL PRIMARY KEY,
  test_config_name TEXT NOT NULL,
  description TEXT,
  collection_id BIGINT REFERENCES cube.recording(id) ON DELETE CASCADE,
  gateway_service_id BIGINT REFERENCES cube.service(id) ON DELETE CASCADE,
  gateway_path_selection JSON NOT NULL,
  endpoint TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (collection_id , test_config_name)
);

CREATE INDEX test_index ON cube.test(collection_id);

CREATE TRIGGER set_timestamp_test
BEFORE UPDATE ON cube.test
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

/*need to somehow make sure that the test id and service id correspond to the same app*/
create TABLE cube.test_virtualized_service (
  test_id BIGINT REFERENCES cube.test(id) ON DELETE CASCADE,
  service_id BIGINT REFERENCES cube.test(id) ON DELETE CASCADE,
  UNIQUE (test_id , service_id)
);

CREATE INDEX virtualized_service_index ON cube.test_virtualized_service(test_id);

/*need to somehow make sure that the test id and service id correspond to the same app*/
create TABLE cube.test_intermediate_service (
  test_id BIGINT REFERENCES cube.test(id) ON DELETE CASCADE,
  service_id BIGINT REFERENCES cube.test(id) ON DELETE CASCADE,
  UNIQUE(test_id, service_id)
);

CREATE INDEX intermediate_service_index ON cube.test_intermediate_service(test_id);

CREATE TYPE cube.template_type AS ENUM ('Request' , 'Response');

create TABLE cube.compare_template (
  id BIGSERIAL PRIMARY KEY,
  test_id BIGINT REFERENCES cube.test(id) ON DELETE CASCADE,
  path TEXT NOT NULL,
  template JSON NOT NULL,
  type cube.template_type NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(test_id, type, path)
);

CREATE INDEX compare_template_index ON cube.compare_template(test_id, type, path);

CREATE TRIGGER set_timestamp_template
BEFORE UPDATE ON cube.compare_template
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

CREATE TYPE cube.replay_status AS ENUM ('Init' , 'Running' , 'Completed' , 'Error');

/*
Not creating a separate analysis table, including analysis as a json here itself
*/
CREATE TABLE cube.replay (
  id BIGSERIAL PRIMARY KEY,
  replay_name VARCHAR(200) NOT NULL,
  test_id BIGINT REFERENCES cube.test(id) ON DELETE CASCADE,
  status cube.replay_status NOT NULL,
  req_count INTEGER,
  req_sent INTEGER,
  req_failed INTEGER,
  analysis JSON,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  completed_at TIMESTAMPTZ,
  sample_rate REAL,
  UNIQUE(test_id, replay_name)
);

CREATE INDEX replay_index ON cube.replay(test_id, status);

CREATE TRIGGER set_timestamp_replay
BEFORE UPDATE ON cube.replay
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

/*
Ideally would not want to keep any request response level information in postgresql
(Including ReqRespMatchResult)
*/
