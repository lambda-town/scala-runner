#!/usr/bin/env sh

docker login -u $GITHUB_USER -p $GITHUB_TOKEN docker.pkg.github.com
sbt runtime/dockerBuildAndPush server/dockerBuildAndPush