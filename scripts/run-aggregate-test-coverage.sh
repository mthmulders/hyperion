#!/bin/bash

sbt clean coverage test
sbt coverageAggregate