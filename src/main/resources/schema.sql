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

CREATE TABLE cube.customer (
  id BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL,
  email TEXT,
  domain_url TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TRIGGER set_timestamp_customer
BEFORE UPDATE ON cube.customer
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

CREATE TABLE cube.cubeuser (
  id BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL,
  email TEXT NOT NULL,
  password TEXT NOT NULL,
  customer_id BIGINT REFERENCES cube.customer(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TRIGGER set_timestamp_cubeuser
BEFORE UPDATE ON cube.cubeuser
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

CREATE TABLE cube.instance (
  id SERIAL PRIMARY KEY,
  name TEXT NOT NULL,
  app_id BIGINT REFERENCES cube.app(id) ON DELETE CASCADE,
  gateway_endpoint TEXT NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

create TABLE cube.instance_user (
  user_id BIGINT NOT NULL REFERENCES cube.cubeuser(id) ON DELETE CASCADE,
  instance_id BIGINT NOT NULL REFERENCES cube.instance(id) ON DELETE CASCADE,
  UNIQUE(user_id, instance_id)
);

CREATE INDEX instance_user_index ON cube.instance_user(user_id);

CREATE TABLE cube.app (
  id BIGSERIAL PRIMARY KEY,
  customer_id BIGINT REFERENCES cube.customer(id) ON DELETE CASCADE,
  name VARCHAR(200) NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE (customer_id, name)
);

CREATE INDEX app_index ON cube.app(customer_id);

create TABLE cube.app_user (
  user_id BIGINT NOT NULL REFERENCES cube.cubeuser(id) ON DELETE CASCADE,
  app_id BIGINT NOT NULL REFERENCES cube.app(id) ON DELETE CASCADE,
  UNIQUE(user_id, app_id)
);

CREATE INDEX app_user_index ON cube.app_user(user_id);

CREATE TRIGGER set_timestamp_app
BEFORE UPDATE ON cube.app
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

CREATE TYPE cube.service_type AS ENUM ('gateway', 'intermediate', 'virtualized');

CREATE TABLE cube.service_group (
  id BIGSERIAL PRIMARY KEY,
  app_id  BIGINT REFERENCES cube.app(id) ON DELETE CASCADE,
  name VARCHAR(200) NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE (app_id, name)
);

CREATE TABLE cube.service (
  id BIGSERIAL PRIMARY KEY,
  app_id  BIGINT REFERENCES cube.app(id) ON DELETE CASCADE,
  service_group_id BIGINT REFERENCES cube.service_group(id) ON DELETE CASCADE,
  name VARCHAR(200) NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE (service_group_id, app_id, name)
);

CREATE INDEX service_index ON cube.service(service_group_id, app_id);

CREATE TRIGGER set_timestamp_service
BEFORE UPDATE ON cube.service
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

CREATE INDEX service_group_index ON cube.service(app_id);

CREATE TRIGGER set_timestamp_service_group
BEFORE UPDATE ON cube.service_group
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

CREATE TABLE cube.path (
  id BIGSERIAL PRIMARY KEY,
  service_id  BIGINT REFERENCES cube.service(id) ON DELETE CASCADE,
  path text NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE (service_id, path)
);

CREATE INDEX path_index ON cube.path(service_id);

CREATE TRIGGER set_timestamp_path
BEFORE UPDATE ON cube.path
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

/* Thoughts on whether we can store the entire graph as a single json
(we really don't need to navigate nodes)
Just reconstruction of the graph is required at the UI end
Including the app id field so that the entire graph for an app can be retrieved in
one single select query
Also if we store in this format updates can be cumbersome*/
CREATE TABLE cube.service_graph (
  app_id BIGINT REFERENCES cube.app(id) ON DELETE CASCADE,
  from_service_id  BIGINT REFERENCES cube.service(id),
  to_service_id  BIGINT REFERENCES cube.service(id),
  UNIQUE (app_id, from_node_id, to_node_id)
);

CREATE INDEX service_graph_index ON cube.service_graph(app_id, from_service_id, to_service_id);

CREATE TYPE cube.recording_status AS ENUM ('Running' , 'Completed' , 'Error');

/*Not sure whether to name it collection or recording*/
CREATE TABLE cube.recording (
  id BIGSERIAL PRIMARY KEY,
  app_id BIGINT REFERENCES cube.app(id) ON DELETE CASCADE,
  instance_id BIGINT REFERENCES cube.instance(id) ON DELETE CASCADE,
  collection_name VARCHAR(200) NOT NULL,
  status cube.recording_status NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  completed_at TIMESTAMPTZ,
  UNIQUE(app_id, instance_id, collection_name)
);

CREATE INDEX recording_index ON cube.recording(app_id, instance_id, status);

CREATE TRIGGER set_timestamp_recording
BEFORE UPDATE ON cube.recording
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

/*can this collection be a range instead of a single collection*/
create TABLE cube.test_config (
  id BIGSERIAL PRIMARY KEY,
  test_config_name TEXT NOT NULL,
  app_id BIGINT REFERENCES cube.app(id) ON DELETE CASCADE,
  description TEXT,
  gateway_service_id BIGINT REFERENCES cube.service(id) ON DELETE CASCADE,
  gateway_req_selection JSON,
  max_run_time_min INTEGER,
  email_id TEXT,
  slack_id TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (app_id , test_config_name)
);

CREATE INDEX test_index ON cube.test_config(app_id);

CREATE TRIGGER set_timestamp_test
BEFORE UPDATE ON cube.test_config
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

/*need to somehow make sure that the testConfig id and service id correspond to the same app*/
create TABLE cube.test_virtualized_service (
  test_id BIGINT REFERENCES cube.test_config(id) ON DELETE CASCADE,
  service_id BIGINT REFERENCES cube.service(id) ON DELETE CASCADE,
  UNIQUE (test_id , service_id)
);

CREATE INDEX virtualized_service_index ON cube.test_virtualized_service(test_id);

/*need to somehow make sure that the testConfig id and service id correspond to the same app*/
create TABLE cube.test_intermediate_service (
  test_id BIGINT REFERENCES cube.test_config(id) ON DELETE CASCADE,
  service_id BIGINT REFERENCES cube.service(id) ON DELETE CASCADE,
  UNIQUE(test_id, service_id)
);

CREATE INDEX intermediate_service_index ON cube.test_intermediate_service(test_id);

/*need to somehow make sure that the testConfig id and path id correspond to the same app*/
create TABLE cube.test_path (
  test_id BIGINT NOT NULL REFERENCES cube.test_config(id) ON DELETE CASCADE,
  path_id BIGINT NOT NULL REFERENCES cube.path(id) ON DELETE CASCADE,
  UNIQUE(test_id, path_id)
);

CREATE INDEX test_path_index ON cube.test_path(test_id);

CREATE TYPE cube.template_type AS ENUM ('Request' , 'Response');

create TABLE cube.comparision_template (
  id BIGSERIAL PRIMARY KEY,
  app_id BIGINT REFERENCES cube.app(id) ON DELETE CASCADE,
  service_id BIGINT REFERENCES cube.service(id) ON DELETE CASCADE,
  path TEXT NOT NULL,
  template JSON NOT NULL,
  type cube.template_type NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(app_id, service_id, type, path)
);

CREATE INDEX compare_template_index ON cube.comparision_template(app_id, service_id, type, path);

CREATE TRIGGER set_timestamp_template
BEFORE UPDATE ON cube.comparision_template
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

CREATE TYPE cube.replay_status AS ENUM ('Init' , 'Running' , 'Completed' , 'Error');

/*
Not creating a separate analysis table, including analysis as a json here itself
*/
CREATE TABLE cube.replay (
  id BIGSERIAL PRIMARY KEY,
  replay_name VARCHAR(200) NOT NULL,
  test_config_id BIGINT REFERENCES cube.test_config(id) ON DELETE CASCADE,
  collection_id BIGINT REFERENCES cube.recording(id) ON DELETE CASCADE,
  status cube.replay_status NOT NULL,
  req_count INTEGER,
  req_sent INTEGER,
  req_failed INTEGER,
  analysis JSON,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  completed_at TIMESTAMPTZ,
  sample_rate REAL,
  UNIQUE(test_config_id, collection_id, replay_name)
);

CREATE INDEX replay_index ON cube.replay(test_config_id, collection_id, status);

CREATE TRIGGER set_timestamp_replay
BEFORE UPDATE ON cube.replay
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

/*
Ideally would not want to keep any request response level information in postgresql
(Including ReqRespMatchResult)
*/
