
#stage: building
FROM maven:3.9-eclipse-temurin-21-jammy AS build-base
WORKDIR /build

FROM build-base AS build
COPY pom.xml .
COPY src/main ./src/main

RUN --mount=type=cache,id=dailycode-maven,target=/root/.m2,sharing=locked \
    mvn --batch-mode --no-transfer-progress -Dmaven.test.skip=true package \
    && java -Djarmode=tools -jar target/dailycode-*.jar extract \
        --layers --destination /build/extracted --application-filename app.jar

FROM build-base AS java-runtime

RUN jlink \
    --add-modules java.compiler,java.desktop,java.instrument,java.management,java.net.http,java.prefs,java.rmi,java.scripting,java.security.jgss,java.sql.rowset,jdk.jfr,jdk.unsupported,jdk.crypto.ec,jdk.charsets,jdk.localedata,jdk.naming.dns,jdk.management \
    --strip-debug \
    --no-header-files \
    --no-man-pages \
    --compress=2 \
    --output /opt/java-runtime

#stage : runtime
FROM ubuntu:22.04 AS runtime
RUN apt-get update \
    && apt-get install --yes --no-install-recommends ca-certificates libstdc++6 zlib1g \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/opt/java-runtime
ENV PATH="${JAVA_HOME}/bin:${PATH}"

WORKDIR /app

COPY --from=java-runtime /opt/java-runtime /opt/java-runtime
COPY --from=build --chown=10001:10001 /build/extracted/dependencies/ ./
COPY --from=build --chown=10001:10001 /build/extracted/spring-boot-loader/ ./
COPY --from=build --chown=10001:10001 /build/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=10001:10001 /build/extracted/application/ ./

USER 10001:10001

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
