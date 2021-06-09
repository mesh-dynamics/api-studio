# mysql -uroot -ppassword

CREATE USER 'cube'@'%' IDENTIFIED BY 'cubeio';
GRANT ALL PRIVILEGES ON * . * TO 'cube'@'%';

# from the host.
# copy sakila files from the source folder (change as appropriate)
mysql -ucube -pcubeio -s < /sakila-db/sakila-schema.sql
mysql -ucube -pcubeio -s < /sakila-db/sakila-data.sql
