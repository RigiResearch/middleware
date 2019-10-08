# Historian

Given an OpenAPI specification and a dependency graph, Historian generates a gradle project to keep track of the change history of deployed resources.

Historian is a command-line utility. Once you build the project, a `.zip` file will be created under `build/distributions`. As an alternative, you can use gradle to execute the commands. For example, `../gradlew :historian:run --args="--help"`.

```bash
# replace VERSION with the current version
cd build/distributions && unzip historian-VERSION.zip
cd historian-VERSION/bin && ./historian --help
```

### Generating a monitoring project

```bash
./historian generate --input /path/to/openapi-spec.json --output /path/to/target/directory --type project
```

The target directory is where the Java project will be generated.

Historian can also generate `dot` and `cxl` ([cmaptools](https://cmap.ihmc.us/)) specifications to visualize a dependency graph:

```bash
./historian generate --input /path/to/dependency-graph.xml --output /path/to/target/directory --type dot
```

### Runing the generated project

```java
./gradlew run
```

### Dependency graphs

TBD

In the meantime, A sample graph can be found [here](../historian.runtime/src/test/resources/simple/graph.xml).
