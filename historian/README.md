# Historian

Given an OpenAPI specification, this project generates a set of cloud monitors that keep track of the change history of deployed resources.

### Prerequisites

Clone and install (`mvn install`) the following dependencies:

- https://github.com/vmware/xpath-for-json

### Run the generator

```bash
gradle :historian:run --args="/path/to/openapi-spec.json /path/to/target/directory"
```

Where the target directory is where the Java project will be generated.

### Run the generated project

```java
./gradlew run
```
