# Scala Runner

This app is responsible for compiling and executing Scala code.
It is organized in various sub-projects:

- The `server` project defines a straight-forward TCP server that can receive Scala code and
dependencies references, compile Scala code, and launch them by delegating to the external `java` command.
- The `client` project is an API to call the server
- The `messages` project defines the exchange protocol between the server and the client
- The `scala-utils` project defines a library that will be accessible from any Scala process
launched through the runner, making it easier to share behavior between courses

---

## Starting locally

Simply run

```
sbt server/run
```

The server will start on port 2003, and will store temporary files in the `tmp` folder of
the current working directory.

## Starting the docker container

```
docker run -p 2003:2003 -e TMP_ROOT_PATH=/app/tmp docker.pkg.github.com/lambda-town/scala-runner/scala-runner-server:LATEST
```
