package de.softwaretesting.studyconnect.runner;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * Cucumber integration test runner for executing BDD feature files using the Failsafe/verify phase.
 * This class intentionally ends with 'IT' so the Maven Failsafe plugin picks it up during 'mvn verify'.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = "cucumber.glue", value = "de.softwaretesting.studyconnect")
@ConfigurationParameter(key = "cucumber.plugin", value = "pretty")
public class StudyconnectCucumberIT {
    // Entry point for executing Cucumber features on the JUnit Platform during integration-test/verify.
}
