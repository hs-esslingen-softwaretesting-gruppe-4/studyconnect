package de.softwaretesting.studyconnect.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import de.softwaretesting.studyconnect.StudyconnectApplication;
import io.cucumber.spring.CucumberContextConfiguration;

@CucumberContextConfiguration
@SpringBootTest(classes = StudyconnectApplication.class)
@ActiveProfiles("test")
public class CucumberSpringConfiguration {
    // Shared Spring Boot test context configuration for Cucumber step definitions.
}
