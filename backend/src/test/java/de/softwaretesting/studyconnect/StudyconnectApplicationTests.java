package de.softwaretesting.studyconnect;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import de.softwaretesting.studyconnect.config.KeycloakTestLifecycleConfig;

@SpringBootTest
@ActiveProfiles("test")
@Import(KeycloakTestLifecycleConfig.class)
class StudyconnectApplicationTests {

	@Test
	void contextLoads() {
	}
}
