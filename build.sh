#!/bin/sh
set -e

mkdir -p build
javac -d build src/CronExpr.java src/CronPeek.java test/CronPeekTest.java

echo "running checks"
java -cp build CronPeekTest
