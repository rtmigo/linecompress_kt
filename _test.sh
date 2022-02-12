#!/bin/bash
set -e && cd "${0%/*}"

./gradlew test --tests "*"
