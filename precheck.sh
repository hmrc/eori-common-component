#!/usr/bin/env bash

sbt clean scalafmt test:scalafmt coverage test coverageReport