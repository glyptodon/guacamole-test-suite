#!/bin/bash

BIN="$(dirname "$0")"

java -jar "$BIN/../target/guacamole-test-suite-0.8.0-jar-with-dependencies.jar" "$@"

