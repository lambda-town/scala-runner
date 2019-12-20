#!/usr/bin/env sh

scala \
  -J-Xmx100m \
  -Djava.security.policy==./jvm-security.policy \
  -classpath "/app/classpath:/app/dependencies/*.jar" \
  Main
