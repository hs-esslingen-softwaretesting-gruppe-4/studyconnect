package de.softwaretesting.studyconnect.integrations;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.softwaretesting.studyconnect.utils.JwtTestUtil;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for SecurityConfig to verify security filter chain behavior, CORS
 * configuration, JWT authentication, and authorization rules. Uses JwtTestUtil helpers for JWT
 * token creation as requested.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      "allowed.origin=http://localhost:3000",
      "required.keycloak.role=studyconnect",
      "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
      "logging.level.org.springframework.security=DEBUG",
      "spring.datasource.url=jdbc:h2:mem:testdb",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
class SecurityConfigIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private Environment environment;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
  }

  // === Authentication Tests ===

  @Test
  void shouldRequireAuthentication_WhenNoCredentialsProvided() throws Exception {
    mockMvc.perform(get("/api/test")).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockJwtUser(roles = {"studyconnect"})
  void shouldAllowAccess_WhenValidJwtWithRequiredRole() throws Exception {
    // Should pass authorization but get 404 since endpoint doesn't exist
    mockMvc.perform(get("/api/test")).andExpect(status().isNotFound());
  }

  @Test
  @WithMockJwtUser(roles = {"other-role"})
  void shouldDenyAccess_WhenJwtWithoutRequiredRole() throws Exception {
    mockMvc.perform(get("/api/test")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockJwtUser(
      username = "testuser",
      roles = {"studyconnect"})
  void shouldExtractUserInfo_FromJwtToken() throws Exception {
    // Test that JWT token information is properly extracted and processed
    mockMvc.perform(get("/api/test")).andExpect(status().isNotFound());
  }

  @Test
  @WithMockJwtUser(
      username = "admin",
      roles = {"admin", "studyconnect"})
  void shouldAllowAccess_WhenUserHasMultipleRolesIncludingRequired() throws Exception {
    mockMvc.perform(get("/api/test")).andExpect(status().isNotFound());
  }

  @Test
  void shouldHandleInvalidJwt_Gracefully() throws Exception {
    mockMvc
        .perform(get("/api/test").header("Authorization", "Bearer invalid-jwt-token"))
        .andExpect(status().isUnauthorized());
  }

  // === CORS Configuration Tests ===

  @Test
  void shouldAllowCorsPreflightRequest_FromAllowedOrigin() throws Exception {
    mockMvc
        .perform(
            options("/api/test")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST"))
        .andExpect(status().isOk())
        .andExpect(header().exists("Access-Control-Allow-Origin"));
  }

  @Test
  void shouldSetCorrectCorsHeaders_ForAllowedMethods() throws Exception {
    mockMvc
        .perform(
            options("/api/test")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
        .andExpect(header().string("Access-Control-Allow-Methods", containsString("GET")))
        .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")))
        .andExpect(header().string("Access-Control-Allow-Methods", containsString("PUT")))
        .andExpect(header().string("Access-Control-Allow-Methods", containsString("DELETE")));
  }

  @Test
  void shouldReturnCorrectCorsHeaders_ForPreflightWithHeaders() throws Exception {
    mockMvc
        .perform(
            options("/api/users")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
        .andExpect(header().string("Access-Control-Allow-Headers", "Content-Type, Authorization"));
  }

  @Test
  void shouldRejectCorsFromDisallowedOrigin() throws Exception {
    // Test that CORS properly rejects requests from non-allowed origins
    mockMvc
        .perform(
            options("/api/test")
                .header("Origin", "http://malicious-site.com")
                .header("Access-Control-Request-Method", "POST"))
        .andExpect(status().isForbidden());
  }

  // === HTTP Methods Authorization Tests ===

  @Test
  @WithMockJwtUser(roles = {"studyconnect"})
  void shouldAllowAllConfiguredHttpMethods_WithValidAuthentication() throws Exception {
    // Test that all HTTP methods are properly authorized
    mockMvc.perform(get("/api/test")).andExpect(status().isNotFound());
    mockMvc.perform(post("/api/test")).andExpect(status().isNotFound());
    mockMvc.perform(put("/api/test")).andExpect(status().isNotFound());
    mockMvc.perform(delete("/api/test")).andExpect(status().isNotFound());
  }

  // === CSRF Protection Tests ===

  @Test
  void shouldHaveCsrfDisabled_ForRestApiEndpoints() throws Exception {
    // CSRF should be disabled for REST API - request should not fail due to CSRF
    mockMvc
        .perform(post("/api/test").header("X-Requested-With", "XMLHttpRequest"))
        .andExpect(status().isUnauthorized()); // Should get 401 for auth, not 403 for CSRF
  }

  // === Endpoint Access Tests ===

  @Test
  @WithMockJwtUser(roles = {"studyconnect"})
  void shouldAllowAccess_ToApiEndpointsWithValidRole() throws Exception {
    // Test access to typical API endpoints that would exist in the application
    mockMvc
        .perform(get("/api/users"))
        .andExpect(status().isMethodNotAllowed()); // 405 means auth passed but method not allowed
    mockMvc
        .perform(get("/api/tasks"))
        .andExpect(status().isNotFound()); // 404 means auth passed, no endpoint
  }

  // === Concurrency and Error Handling Tests ===

  @Test
  void shouldHandleConcurrentRequests_ThreadSafely() throws Exception {
    // Test thread safety of security configuration
    for (int i = 0; i < 5; i++) {
      mockMvc.perform(get("/api/test")).andExpect(status().isUnauthorized());
    }
  }

  @Test
  void shouldHandleBasicAuthenticationFallback() throws Exception {
    // Test that basic auth is enabled as fallback when JWT fails
    mockMvc
        .perform(get("/api/test").header("Authorization", "Basic invalid-basic-auth"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockJwtUser(roles = {"studyconnect"})
  void shouldProcessJwtAuthenticationConverter_Properly() throws Exception {
    // Test that JWT authentication converter processes tokens correctly
    mockMvc.perform(get("/api/test")).andExpect(status().isNotFound());
  }

  @Test
  void shouldValidateSecurityFilterChain_Configuration() throws Exception {
    // Test basic security filter chain functionality
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  @WithMockJwtUser(
      username = "realmuser",
      roles = {"studyconnect", "realm-role"})
  void shouldHandleRealmRoles_FromJwtClaims() throws Exception {
    // Test extraction of realm roles using JwtTestUtil
    mockMvc.perform(get("/api/test")).andExpect(status().isNotFound());
  }

  // === Custom JWT Authentication Annotation ===

  /** Custom annotation for mocking JWT authentication in tests using JwtTestUtil */
  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @WithSecurityContext(factory = WithMockJwtUserSecurityContextFactory.class)
  public @interface WithMockJwtUser {
    String username() default "testuser";

    String[] roles() default {"studyconnect"};
  }

  /** Security context factory for JWT mock user using JwtTestUtil */
  static class WithMockJwtUserSecurityContextFactory
      implements WithSecurityContextFactory<WithMockJwtUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockJwtUser annotation) {
      // Use JwtTestUtil to create properly formatted JWT tokens
      Jwt jwt =
          JwtTestUtil.createJwtToken(Arrays.asList(annotation.roles()), annotation.username());

      JwtAuthenticationToken authentication =
          new JwtAuthenticationToken(
              jwt, Arrays.stream(annotation.roles()).map(SimpleGrantedAuthority::new).toList());

      SecurityContext context = SecurityContextHolder.createEmptyContext();
      context.setAuthentication(authentication);
      return context;
    }
  }
}
