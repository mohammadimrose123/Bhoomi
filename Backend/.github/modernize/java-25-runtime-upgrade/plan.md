# Modernization Plan: Java 25 Runtime Upgrade

**Project**: Bhoomi Backend

## Technical Framework

- **Language**: Java 21
- **Framework**: Spring Boot 4.1.2
- **Build Tool**: Maven Wrapper
- **Database**: PostgreSQL/PostGIS
- **Key Dependencies**: Spring Data JPA, Flyway, Spring MVC

## Overview

Upgrade the Backend runtime from Java 21 to Java 25 LTS while preserving
application behavior. Keep the work limited to the Backend and its directly
related Java build/runtime configuration and documentation. Spring Boot 4.1.2
is retained; the Java upgrade guideline lists Spring Boot 4.x as compatible
with Java 25, so no framework or Jakarta migration is included.

The assessment's `Java Version is not the latest LTS` finding is confirmed by
`pom.xml` specifying Java 21 and `README.md` documenting JDK 21. Update those
references and validate the application using the Java 25 runtime. No
application runtime Dockerfile was found; the existing Dockerfile is for the
PostgreSQL/PostGIS database and is outside this Java-runtime change.

## Scope and Validation

- Upgrade the Backend Java target and applicable Java runtime configuration
  to Java 25 LTS.
- Update Backend developer documentation and any directly relevant Java
  container configuration if one is introduced or discovered during execution.
- Preserve existing application and database behavior. Do not change framework
  versions, cloud-readiness findings, Frontend files, or the database image.
- With JDK 25 selected, run the Maven Wrapper's `verify` lifecycle
  (`.\mvnw.cmd clean verify`) as the narrow build-and-test validation.

## Assessment Evidence

- **Java Version is not the latest LTS** — `pom.xml`, line 30, sets Java 21;
  `README.md` also lists JDK 21 as the prerequisite.
- The assessment reports JDK 21 and identifies OpenJDK 25 capability.
- Other assessment findings are outside this plan's requested scope.

## Planned Work

Upgrade the Backend Java runtime from 21 to 25 LTS, update directly relevant
build/runtime configuration and documentation, preserve current behavior, and
confirm the build and existing tests pass under Java 25. After the upgrade,
scan and remediate known CVEs only in Backend direct dependencies relevant to
the updated runtime/build, without addressing unrelated cloud-readiness issues.
