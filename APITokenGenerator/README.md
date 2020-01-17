What does it do?

This is a small tool to genreate a Personal Access Token for a given customer and adds it to the DB.

Why do we need this?

Mesh Dynamics UI is obtains a token by giving username/password and the token has an expiry. And this is fine for the UI Application as UI application is interactive. But API clients to talk to cube backend, we need an authentication mechanism which is non-interactive. To acheive the same, we need to generate a token offline for each customer and keep it in DB and the API clients of a customer can use the same to work with the Mesh Dynamics server. This token will not expire and if there is a need to revoke the token for a specific customer, we can simply delete that entry from the DB.

How to run this to generate the API token for a customer?

  1. Checkout this repo
  2. build the jar by running "mvn clean compile assembly:single"
  3. run the jar "java -jar target/pat-generator-1.0-SNAPSHOT-jar-with-dependencies.jar"
  Enter the config file path. The file with values pointing to dev/staging DB is available under src/resources
  4. Feed the customer id for which the token has to be generated. The token will be printed in the console.

Where to use this token?

In the fluentd configuration that sends the request/response to the cube server, the token has to be configured
Example:

  ```<match cube.movieinfo.envoy.staging-mn>
      @type http
      endpoint https://staging-mn.dev.cubecorp.io/api/cs/rrbatch
      headers { "Authorization":"Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJNZXNoREFnZW50VXNlciIsInJvbGVzIjpbIlJPTEVfVVNFUiJdLCJ0eXBlIjoicGF0IiwicmFuZG9tc3RyIjoiYVhGdmFrQzlJMCIsImlhdCI6MTU3OTE3Mjk4MywiZXhwIjoxODk0NTMyOTgzfQ.4ainHhElKLNHqmz5XqWa9HHfErHSTrXJ6W1DNjlTzz0"}
      tls_verify_mode none
      open_timeout 5
      <format>
        @type json
      </format>
      <buffer log>
        @type memory
        chunk_limit_records 10
        flush_interval 10
      </buffer>
    </match>
    
    ```
