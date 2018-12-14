# mysql -uroot -ppassword

CREATE USER 'cube'@'%' IDENTIFIED BY 'cubeio';
GRANT ALL PRIVILEGES ON * . * TO 'cube'@'%';

