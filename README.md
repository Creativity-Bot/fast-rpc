# Faster RPC

Faster RPC is a small Java RPC framework. A call on a local interface is sent over one Netty connection, matched by request id, and invoked on the provider by reflection. Services are registered in ZooKeeper. A Spring Boot starter wires the client proxy, the server, and the registry into an application without a hand-written `main`.

The example application is both a provider and a caller. It exposes `GET /hello` on Tomcat, then calls the same service through the RPC client.

## What it does

- Custom binary frames: magic number, version, message type, flags, and a length field.
- One TCP connection per provider address, with many in-flight calls demultiplexed by `requestId`.
- Hessian serialization of the request and the response.
- JDK dynamic proxy. `@FrpcClient` injects the proxy. `@FrpcService` exports an implementation.
- ZooKeeper registration with ephemeral nodes, and a watch that updates the address list.
- Round-robin load balancing over the current address list.
- Client timeout of 3 seconds. On timeout the client sends a cancel flag, and the server interrupts the in-flight business task.
- Retry up to 3 times, with exponential backoff, for connection failure, write failure, and timeout. Business exceptions are returned as-is and are not retried.
- Idle detection and ping/pong heartbeats.
- A bounded business thread pool, separate from Netty I/O threads. A full queue rejects the call.
- A per-service token bucket on the provider.
- Protocol checks for magic, version, type, and flags before the call reaches user code.
- Spring Boot auto-configuration. The server socket is bound from a `SmartLifecycle` after `@FrpcService` beans are registered.

Not included yet, and reasonable to add later:

- More than one connection per address, and a shared event-loop group.
- An asynchronous or streaming API.
- An IDL and a cross-language codec such as Protobuf.
- TLS and authentication.
- Per-method timeouts, and retries that run only for idempotent calls.
- Circuit breaking and temporary removal of a failing address.
- Graceful shutdown that leaves the registry first and waits for in-flight calls.
- Metrics and distributed tracing.
- Weights, groups, and versions in the registry.

## Requirements

- JDK 17
- Maven 3.6.3 or later
- A ZooKeeper server whose client port is reachable from the application (default `2181`)

Hessian 4.0.38 reflects into JDK internals. On JDK 17 the process needs `--add-opens`. `mvn -pl faster-rpc-example spring-boot:run` already passes `--add-opens java.base/java.lang=ALL-UNNAMED`. If you start the app from an IDE, set this environment variable on the run configuration:

```text
JAVA_TOOL_OPTIONS=--add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.math=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.text=ALL-UNNAMED --add-opens=java.base/java.time=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED
```

## Run the example

Start ZooKeeper, then point `faster-rpc-example/src/main/resources/application.yml` at it. `fastrpc.server.ip` must be an address other processes can use to open a TCP connection. `localhost` is published as `127.0.0.1`, which is reachable only on the same machine.

```yaml
fastrpc:
  registry:
    address: 127.0.0.1:2181
  server:
    enabled: true
    ip: 192.168.1.10
    port: 9527
```

From the repository root:

```shell
mvn -pl faster-rpc-example spring-boot:run
```

Tomcat listens on port 8080. Open:

```text
http://localhost:8080/hello?name=faster-rpc
```

A successful call returns `hello faster-rpc`. The HTTP handler calls `HelloService` through `@FrpcClient`. The provider is the `@FrpcService` implementation in the same process, reached over the Netty port.

Run the example test (ZooKeeper must be up, and `fastrpc.server.ip` must be dialable from this machine):

```shell
mvn -pl faster-rpc-example -am test
```

## Use the starter

Install the modules locally:

```shell
mvn install
```

In an application that already uses Spring Boot:

```xml
<dependency>
    <groupId>com.srpc</groupId>
    <artifactId>faster-rpc-spring-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

Export a service:

```java
public interface HelloService {
    String hello(String name);
}

@FrpcService
public class HelloServiceImpl implements HelloService {
    @Override
    public String hello(String name) {
        return "hello " + name;
    }
}
```

Call it from another bean. Do not put `@FrpcClient` on the same field that is also the local implementation you want to invoke in-process. The annotation always injects a remote proxy.

```java
public class HelloConsumer {
    @FrpcClient
    private HelloService helloService;
}
```

A provider needs `fastrpc.server.enabled=true`. A process that only calls other services can leave it `false`. The application must not component-scan `com.fastrpc`; the starter registers its own beans.

| Property | Default | Meaning |
| --- | --- | --- |
| `fastrpc.registry.address` | `127.0.0.1:2181` | ZooKeeper connect string |
| `fastrpc.server.enabled` | `false` | Bind the Netty server and register services |
| `fastrpc.server.ip` | `localhost` | Address written to ZooKeeper |
| `fastrpc.server.port` | `9527` | Netty listen port |
| `fastrpc.server.worker-threads` | `4` | Netty worker threads |
| `fastrpc.server.handler-threads` | `16` | I/O handler threads |
| `fastrpc.server.service-threads` | `16` | Business pool size |
| `fastrpc.rate-limit.permits-per-second` | `100` | Token refill rate per service |
| `fastrpc.rate-limit.capacity` | `100` | Token bucket size per service |

## Modules

| Module | Role |
| --- | --- |
| `faster-rpc-core` | Protocol, Netty transport, registry, proxy |
| `faster-rpc-spring-boot-autoconfigure` | Auto-configuration and `@ConfigurationProperties` |
| `faster-rpc-spring-boot-starter` | Dependency entry for applications |
| `faster-rpc-example` | Spring Boot provider, caller, and HTTP endpoint |
