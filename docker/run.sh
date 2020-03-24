#!/usr/bin/env sh

/scala/scala-2.12.11/bin/scala \
  -J-Xmx100m \
  -Djava.security.policy==./jvm-security.policy \
  -classpath "/app/classpath:/app/classpath/*:/app/dependencies/*" \
  Main
