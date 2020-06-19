#!/usr/bin/env sh

docker login rg.fr-par.scw.cloud/lambda -u nologin -p "$SCW_SECRET_TOKEN"
sbt server/dockerBuildAndPush