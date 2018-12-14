FROM mysql/mysql-server:8.0

RUN mkdir /sakila-db
ADD sakila-db/sakila-schema.sql /sakila-db/sakila-schema.sql
ADD sakila-db/sakila-data.sql /sakila-db/sakila-data.sql
ADD sakila-cube-user.sql /docker-entrypoint-initdb.d/sakila-cube-user.sql
# Make sure the following <filename>.sh is lexicographically after sakila-cube-user.sql
ADD sakila-populate-data.sh /docker-entrypoint-initdb.d/sakila-populate-data.sh
RUN chmod 0755 /docker-entrypoint-initdb.d/sakila-populate-data.sh

