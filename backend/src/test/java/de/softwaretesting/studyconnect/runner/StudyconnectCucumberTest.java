package de.softwaretesting.studyconnect.runner;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;


/**
 * Cucumber test runner for executing BDD feature files using JUnit Platform.
 * Renamed to end with 'Test' so Surefire runs it as part of the default `mvn test` goal.
 */
@Suite
@IncludeEngines("cucumber")
@SelectPackages("de.softwaretesting.studyconnect.features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "de.softwaretesting.studyconnect")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
public class StudyconnectCucumberTest {
    // Entry point for executing Cucumber features on the JUnit Platform.
}
