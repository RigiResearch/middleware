# Continuous Software Evolution Middleware

[![CircleCI](https://circleci.com/gh/RigiResearch/middleware.svg?style=shield&circle-token=c63fe35fcbc059d103bf38b7938faae7a01ca65b)](https://circleci.com/gh/RigiResearch/middleware)

A middleware for continuous software evolution.

### Build from the sources

```bash
./gradlew publishMavenPublicationToMavenLocal # only once
./gradlew build
sh docker.build.sh
sh docker.push.sh
```
You can optionally skip the execution of static analysis and tests:

```bash
./gradlew build -x check
```

Some integration tests require that you have Docker installed. You can skip them as well:

```bash
./gradlew build -Dskip.integration=true
```

### Run the containers:

**historian**

```bash
docker run --rm -it jachinte/historian --help
```

**coordinator**

```bash
docker run --rm -it \
  -e COORDINATOR_PORT="5050" \
  -e REPOSITORY_URL="https://default.repository.url/repo" \
  -e REPOSITORY_TOKEN="<repository-token>" \ 
  jachinte/coordinator
```

**vmware.hcl.agent**

```bash
docker run --rm -it \
  -e COORDINATOR_URL="http://localhost:5050" \
  -e API_BASE_URL="http://default.vsphere.url/rest" \
  -e API_AUTH_USERNAME="" \
  -e API_AUTH_PASSWORD="" \
  -e API_AUTH_TOKEN_PERIODICITY="0 * * * *" \
  -e API_REQUEST_PERIODICITY="*/5 * * * *" \ 
  jachinte/vmware
```
