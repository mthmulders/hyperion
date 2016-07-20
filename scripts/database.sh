#!/bin/bash

docker pull postgres:9.3

docker run --name hyperion-db -p 5432:5432 \
    -e POSTGRES_DB=hyperion \
    -e POSTGRES_USER=hyperion \
    -e POSTGRES_PASSWORD=welcome123 \
    -d postgres:9.3