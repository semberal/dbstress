# Contributing to dbstress

Thank you for considering a new contribution to dbstress!

## Important commands

```bash
sbt test # Run unit tests
sbt it:test # Run integration tests (see below how to setup postgres)
sbt packArchive # Build distribution archive

sbt scalafmtAll scalafmtSbt # Reformat source code before submitting a new pull request

sbt dependencyUpdates # Check for updated dependencies
```

## Running integrations tests

For integration tests (`sbt it:test`), the following docker container must be running:
```bash
docker run -e POSTGRES_USER=dbstress -e POSTGRES_PASSWORD=dbstress -e POSTGRES_DB=dbstress -p 5432:5432 -d postgres:latest
```
