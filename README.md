# Metrics Example

This example exports data to STAGING. You will need the latest code from OTel Java - as SNAPSHOTs are broken as this moment, please get `opentelemetry-java`, build it, and then type this to install 0.11.0-SNAPSHOT to the local repo:

```sh
./gradlew publishToMavenLocal
```

Once that is done, build and run this example:

```sh
export ACCESS_TOKEN=my-access-token-for-staging-etc
mvn compile
mvn exec:java -Dexec.mainClass=com.lightstep.otel.App
```

Once that is done, you should be seeing metrics in the UI.

**Note**: At this moment, only `long_observer` will **always** work in the UI. The other metrics fail to render most of the time, although that's (now) a known bug and should be fixed soon.
