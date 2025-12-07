package de.softwaretesting.studyconnect;

import de.softwaretesting.studyconnect.config.KeycloakTestLifecycleConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(KeycloakTestLifecycleConfig.class)
class StudyconnectApplicationTests {

  @Test
  void contextLoads() {}
}
