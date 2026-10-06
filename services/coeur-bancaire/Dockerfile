# syntax=docker/dockerfile:1
# Construction et exécution du service coeur-bancaire ; contexte de construction : racine du dépôt.
FROM eclipse-temurin:21 AS construction
ARG MAVEN_VERSION=3.9.16
ARG MAVEN_SHA512=831a8591fe20c8243b1dbe7d71e3244f31d1665b0804b2e825e38cbbe5ce0cafb8338851f90780735568773e0a6cd07bbec107cda0b896b008b861075358b6f6
RUN curl -fsSL -o /tmp/maven.tar.gz \
      "https://repo1.maven.org/maven2/org/apache/maven/apache-maven/${MAVEN_VERSION}/apache-maven-${MAVEN_VERSION}-bin.tar.gz" \
 && echo "${MAVEN_SHA512}  /tmp/maven.tar.gz" | sha512sum -c - \
 && tar -xzf /tmp/maven.tar.gz -C /opt \
 && ln -s /opt/apache-maven-${MAVEN_VERSION}/bin/mvn /usr/local/bin/mvn \
 && rm /tmp/maven.tar.gz
WORKDIR /construction
COPY pom.xml ./
COPY services/coeur-bancaire/pom.xml services/coeur-bancaire/
COPY services/notifications/pom.xml services/notifications/
COPY services/identite-clients/pom.xml services/identite-clients/
COPY services/audit/pom.xml services/audit/
COPY services/bourse/pom.xml services/bourse/
COPY services/coeur-bancaire/src services/coeur-bancaire/src
# Les tests (Testcontainers) tournent dans scripts/gates/build.sh, pas dans l'image.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl services/coeur-bancaire -am package -DskipTests \
 && cp services/coeur-bancaire/target/coeur-bancaire-*.jar /construction/service.jar

FROM eclipse-temurin:21-jre
ENV TZ=Europe/Paris
RUN useradd --system --uid 10001 service
COPY --from=construction /construction/service.jar /opt/service/service.jar
USER service
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/opt/service/service.jar"]
