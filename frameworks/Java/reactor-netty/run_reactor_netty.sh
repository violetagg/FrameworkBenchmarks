#!/bin/bash

JAVA_OPTIONS="--enable-native-access=ALL-UNNAMED \
  -Dio.netty.noUnsafe=false \
  --sun-misc-unsafe-memory-access=allow \
  --add-opens=java.base/java.lang=ALL-UNNAMED \
  -XX:+UseNUMA \
  -XX:+UseParallelGC \
  -Dio.netty.buffer.checkBounds=false \
  -Dio.netty.buffer.checkAccessible=false \
  -Dio.netty.iouring.iosqeAsyncThreshold=32000 \
  -Dreactor.netty.http.server.lastFlushWhenNoRead=true \
  $@"

java $JAVA_OPTIONS -jar app.jar
