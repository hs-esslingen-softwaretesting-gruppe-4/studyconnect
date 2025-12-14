package de.softwaretesting.studyconnect.utils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.springframework.security.oauth2.jwt.Jwt;

/** Utility class for creating test JWT tokens for security testing. */
public class JwtTestUtil {

  private JwtTestUtil() {
    // Utility class
  }

  /**
   * Creates a test JWT token with specified roles.
   *
   * @param roles List of roles to include in the token
   * @param subject Subject (username) for the token
   * @return Jwt token for testing
   */
  public static Jwt createJwtToken(List<String> roles, String subject) {
    Instant now = Instant.now();
    Instant expiry = now.plus(1, ChronoUnit.HOURS);

    Map<String, Object> claims =
        Map.of(
            "sub",
            subject,
            "iat",
            now.getEpochSecond(),
            "exp",
            expiry.getEpochSecond(),
            "realm_access",
            Map.of("roles", roles),
            "email",
            subject + "@test.com",
            "preferred_username",
            subject);

    Map<String, Object> headers = Map.of("alg", "RS256", "typ", "JWT");

    return new Jwt("test-token", now, expiry, headers, claims);
  }

  /**
   * Creates a test JWT token with studyconnect role.
   *
   * @param username Username for the token
   * @return Jwt token for testing
   */
  public static Jwt createStudyConnectUserToken(String username) {
    return createJwtToken(List.of("studyconnect"), username);
  }

  /**
   * Creates a test JWT token with admin and studyconnect roles.
   *
   * @param username Username for the token
   * @return Jwt token for testing
   */
  public static Jwt createAdminToken(String username) {
    return createJwtToken(List.of("admin", "studyconnect"), username);
  }

  /**
   * Creates a test JWT token without required roles.
   *
   * @param username Username for the token
   * @return Jwt token for testing
   */
  public static Jwt createUnauthorizedToken(String username) {
    return createJwtToken(List.of("other-role"), username);
  }
}
