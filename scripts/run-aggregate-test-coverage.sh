#!/bin/bash

sbt clean coverage test coverageReport
sbt coverageAggregate