# Timestamping Java: Example Service

Timestamping example on EJB for Exonum CIS documentation

## Install and run

## Getting started

Be sure you installed necessary packages:

- Linux or macOS.
- Exonum CIS Java application.
- [JDK 1.8+](http://jdk.java.net/12/).
- [Maven 3.5+](https://maven.apache.org/download.cgi).
- [git](https://git-scm.com/downloads)
- [Exonum Launcher][exonum-launcher] python application.
- Exonum Launcher Plugins.

#### Install and run

Below you will find a step-by-step guide to start the service on 1 node on the local machine.

Build the project:

```sh
source ./tests_profile
mvn install
```

Generate blockchain configuration:

```sh
mkdir example
exonum-java generate-template --validators-count=1 example/common.toml
```

Generate templates of nodes configurations:

<!-- markdownlint-disable MD013 -->

```sh
exonum-java generate-config example/common.toml example --no-password --peer-address 127.0.0.1:6300
```

Finalize generation of nodes configurations:

```sh

exonum-java finalize example/sec.toml example/node.toml --public-configs example/pub.toml
```

Run nodes:

```sh
export RUST_LOG="${RUST_LOG-error,exonum=info,exonum-java=info,java_bindings=info}"
exonum-java run --node-config example/node.toml --artifacts-path target --db-path example/db --master-key-pass pass --public-api-address 127.0.0.1:8200 --private-api-address 127.0.0.1:8091  --ejb-port 7000
```

Before service deploy make sure that you have pure python implementation of protobuf:

```sh
pip uninstall protobuf
pip install --no-binary=protobuf protobuf
```

Deploy timestamping service.

```sh
python3 -m exonum_launcher -i ../timestamping.yaml
```