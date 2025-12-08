# JaCoCo Configuration Documentation
## Overview

This document describes the JaCoCo configuration used for the StudyConnect backend application.
JaCoCo is a code coverage analysis tool that instruments Java bytecode to measure how much of the application is executed during automated tests.
Maintaining a minimum code coverage threshold helps ensure that the backend logic is sufficiently validated by tests.

**Date Configured:** December 8, 2025
**JaCoCo Version:** 0.8.12
**Java Version:** 21
**Spring Boot Version:** 3.5.6

---

## Table of Contents

1. [Configuration Files](#configuration-files)
2. [Maven Plugin Configuration](#maven-plugin-configuration)
3. [Running JaCoCo](#running-jacoco)
4. [Current JaCoCo Results](#current-jacoco-results)

---

## Configuration Files

### 1. `pom.xml`

Location: `/backend/pom.xml`

**Key JaCoCo Goals**
| Goal              | Purpose                                                                 |
| ----------------- | ----------------------------------------------------------------------- |
| **prepare-agent** | Attaches the JaCoCo agent before tests to collect coverage data         |
| **report**        | Generates the HTML coverage report after the test phase                 |
| **check**         | Fails the build if the configured coverage threshold is not met (≥ 80%) |
age is below given treshhold (80% or 0.80)

**File Exclusions (Optional)**
These can be added under the plugin’s <configuration> section to exclude files that should not be considered for coverage (e.g., generated classes, DTOs).

Example:
```xml
<configuration>
    <excludes>
        <exclude>**/generated/**</exclude>
        <exclude>**/dto/**</exclude>
    </excludes>
</configuration>
```
---

## Maven Plugin Configuration

### Plugin Declaration in `pom.xml`

```xml
</build>
    </plugins>
    <plugin>
        <groupId>org.jacoco</groupId>
        <artifactId>jacoco-maven-plugin</artifactId>
        <version>0.8.12</version>
            <executions>
            <!-- 1. Prepare agent before tests to start evaluating the coverage -->
            <execution>
                <goals>
                    <goal>prepare-agent</goal>
                </goals>
            </execution>

            <!-- 2. Generate the html coverage report after tests -->
            <execution>
                <id>report</id>
                <phase>test</phase>
                <goals>
                    <goal>report</goal>
                </goals>
            </execution>
            <!-- 3. Enforce minimum coverage rules (build fail) -->
            <execution>
                <id>check</id>
                <phase>verify</phase>
                <goals>
                    <goal>check</goal>
                </goals>
                <configuration>
                    <rules>
                        <rule>
                            <element>BUNDLE</element>
                            <limits>
                                <limit>
                                    <counter>LINE</counter>
                                    <value>COVEREDRATIO</value>
                                    <minimum>0.80</minimum>
                                </limit>
                            </limits>
                        </rule>
                    </rules>
                </configuration>
            </execution>
        </executions>
        <configuration>
            <excludes>
            <!-- Exclude Files which are generally not tested 
                or should not be included due to ... reason -->
            </excludes>
        </configuration>
    </plugin>
    </plugins>
</build>

```

**Configuration Options:**
- "Needs further research"

**Execution Phase:**
- Gets activated during the **test** phase to generate the test report
- **Check** goal runs during the **validate** phase of the Maven lifecycle

---

## Running JaCoCo
## Quick Commands
```bash
# Run tests and generate coverage report
mvn clean test

# Run report + enforce coverage threshold (80%)
mvn clean verify
```

### Build Behavior
- **Coverage ≥ 80%** → Build succeeds
- **Coverage < 80%** → Build fails during verify with an error such as:

---

## Integration with Build Process

### Maven Lifecycle Integration

JaCoCo is integrated into the Maven build lifecycle as follows:

```
validate → compile → test → package → verify → install → deploy
                      |        |
                prepare-agent  |
                               └── coverage check (verify)

```

### Interaction with Other Plugins

#### Maven Surefire Plugin

- **Purpose**: Runs unit tests  
- **Phase**: `test`
- JaCoCo collects coverage during Surefire execution.

---

## Appendix

### Quick Reference Commands

```bash
# Run Tests and generate Report
mvn clean test

# Generate report and check coverage
mvn clean verify
```

### File Locations

- HTML Report:
`backend/target/site/jacoco/index.html`

- CSV Report:
`backend/target/site/jacoco/jacoco.csv`

- Configuration File:
`backend/pom.xml`

### Refferences

- [JaCoCo Official Documentation](https://www.jacoco.org/)

---

*Document Version: 1.1*  
*Last Updated: December 8, 2025*
