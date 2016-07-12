#!/bin/bash

docker pull mtirsel/mysql-5.1

docker run -p 3306:3306 -e MYSQL_ROOT_PASSWORD=welcome123 -e MYSQL_DATABASE=test -d mtirsel/mysql-5.1
