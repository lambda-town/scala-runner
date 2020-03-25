# Scala Runner

This app is responsible for compiling and executing Scala code. It will every Scala process in
a Docker container with proper isolation.

In production, it is deployed a Docker container ... so you have a Docker container, launching
Docker containers.

It is organized in various sub-projects:

- The `runtime` project defines the Docker image that will be used to execute Scala processes,
as well as the code required to compile Scala code, and launch the processes
- The `server` project defines a straight-forward TCP server that can receive Scala code and
dependencies references, and launch processes by delegating to the `runtime` package.
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
docker run -p 2003:2003 -v /var/run/docker.sock:/var/run/docker.sock -v "$PWD/tmp:/app/tmp" -e "TMP_ROOT_HOST_PATH=$PWD/tmp" -e TMP_ROOT_CONTAINER_PATH=/app/tmp  docker.pkg.github.com/lambdacademy-dev/scala-runner/scala-runner-server:LATEST
```

Because the app needs to launch Docker containers, the Docker socket must be accessible from within
the server container.

A folder for temporary files must also be defined.