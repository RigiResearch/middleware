# Continuous Software Evolution Middleware

[![CircleCI](https://circleci.com/gh/RigiResearch/middleware.svg?style=shield&circle-token=c63fe35fcbc059d103bf38b7938faae7a01ca65b)](https://circleci.com/gh/RigiResearch/middleware)

A middleware for continuous software evolution.

### Build from the sources

```bash
./gradlew publishMavenPublicationToMavenLocal # only once
./gradlew build
```
You can optionally skip the execution of static analysis and tests:

```bash
./gradlew build -x check
```

Some integration tests require that you have Docker installed. You can skip them as well:

```bash
./gradlew build -Dskip.integration=true
```
