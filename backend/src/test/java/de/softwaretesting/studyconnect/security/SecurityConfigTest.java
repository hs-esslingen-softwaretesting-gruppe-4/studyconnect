package de.softwaretesting.studyconnect.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.softwaretesting.studyconnect.utils.JwtTestUtil;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.cors.CorsConfiguration;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

  @Mock private Environment env;

  @InjectMocks private SecurityConfig securityConfig;

  @Test
  void passwordEncoder_ShouldReturnBCryptPasswordEncoder() {
    // When
    PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();

    // Then
    assertThat(passwordEncoder).isNotNull();
    assertThat(passwordEncoder.getClass().getSimpleName()).isEqualTo("BCryptPasswordEncoder");

    // Verify it can encode and match passwords
    String rawPassword = "testPassword123!";
    String encodedPassword = passwordEncoder.encode(rawPassword);
    assertThat(passwordEncoder.matches(rawPassword, encodedPassword)).isTrue();
    assertThat(passwordEncoder.matches("wrongPassword", encodedPassword)).isFalse();
  }

  @Test
  void realmRolesAuthoritiesConverter_ShouldExtractRolesFromRealmAccess() {
    // Given
    Map<String, Object> claims =
        Map.of("realm_access", Map.of("roles", List.of("admin", "user", "studyconnect")));

    // When
    SecurityConfig.AuthoritiesConverter converter = securityConfig.realmRolesAuthoritiesConverter();
    Collection<GrantedAuthority> authorities = converter.convert(claims);

    // Then
    assertThat(authorities).hasSize(3);
    assertThat(authorities)
        .extracting(GrantedAuthority::getAuthority)
        .containsExactlyInAnyOrder("admin", "user", "studyconnect");
  }

  @Test
  void realmRolesAuthoritiesConverter_ShouldReturnEmptyWhenNoRealmAccess() {
    // Given
    Map<String, Object> claims = Map.of("other_claim", "value");

    // When
    SecurityConfig.AuthoritiesConverter converter = securityConfig.realmRolesAuthoritiesConverter();
    Collection<GrantedAuthority> authorities = converter.convert(claims);

    // Then
    assertThat(authorities).isEmpty();
  }

  @Test
  void realmRolesAuthoritiesConverter_ShouldReturnEmptyWhenNoRoles() {
    // Given
    Map<String, Object> claims = Map.of("realm_access", Map.of("other_field", "value"));

    // When
    SecurityConfig.AuthoritiesConverter converter = securityConfig.realmRolesAuthoritiesConverter();
    Collection<GrantedAuthority> authorities = converter.convert(claims);

    // Then
    assertThat(authorities).isEmpty();
  }

  @Test
  void realmRolesAuthoritiesConverter_ShouldHandleNullRealmAccess() {
    // Given
    Map<String, Object> claims = new HashMap<>();
    claims.put("realm_access", null);

    // When
    SecurityConfig.AuthoritiesConverter converter = securityConfig.realmRolesAuthoritiesConverter();
    Collection<GrantedAuthority> authorities = converter.convert(claims);

    // Then
    assertThat(authorities).isEmpty();
  }

  @Test
  void authenticationConverter_ShouldConfigureJwtGrantedAuthoritiesConverter() {
    // Given
    SecurityConfig.AuthoritiesConverter authoritiesConverter =
        securityConfig.realmRolesAuthoritiesConverter();
    Jwt jwt = JwtTestUtil.createJwtToken(List.of("admin", "studyconnect"), "testuser");

    // When
    JwtAuthenticationConverter authenticationConverter =
        securityConfig.authenticationConverter(authoritiesConverter);

    // Then
    assertThat(authenticationConverter).isNotNull();

    // Test that the converter actually works by converting a JWT
    var authentication = authenticationConverter.convert(jwt);
    assertThat(authentication).isNotNull();
    assertThat(authentication.getAuthorities()).hasSize(2);
    assertThat(authentication.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactlyInAnyOrder("admin", "studyconnect");
  }

  @Test
  void jwtDecoder_ShouldReturnNullWhenIssuerUriNotConfigured() {
    // Given
    when(env.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri")).thenReturn(null);

    // When
    JwtDecoder decoder = securityConfig.jwtDecoder();

    // Then
    assertThat(decoder).isNull();
  }

  @Test
  void jwtDecoder_ShouldReturnNullWhenIssuerUriIsBlank() {
    // Given
    when(env.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri")).thenReturn("");

    // When
    JwtDecoder decoder = securityConfig.jwtDecoder();

    // Then
    assertThat(decoder).isNull();
  }

  @Test
  void jwtDecoder_ShouldReturnNullWhenIssuerIsUnreachable() {
    // Given
    when(env.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri"))
        .thenReturn("http://unreachable-server:8080/auth/realms/test");

    // When
    JwtDecoder decoder = securityConfig.jwtDecoder();

    // Then
    assertThat(decoder).isNull();
  }

  @SpringBootTest
  @ActiveProfiles("prod")
  static class SecurityConfigIntegrationTest {

    @Test
    void contextLoads() {
      // Test that the security configuration loads without errors in prod profile
      // This will fail if there are any bean creation issues
      assertThat(true).isTrue();
    }
  }

  @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
  @ActiveProfiles("test")
  @Import(TestSecurityConfig.class)
  static class SecurityFilterChainTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void shouldRequireAuthentication() throws Exception {
      mockMvc.perform(get("/api/test")).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowCorsFromAllowedOrigin() throws Exception {
      mockMvc
          .perform(
              options("/api/test")
                  .header("Origin", "http://localhost:3000")
                  .header("Access-Control-Request-Method", "POST"))
          .andExpect(status().isOk())
          .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    @Test
    @WithMockJwtUser(roles = {"studyconnect"})
    void shouldAllowAccessWithRequiredRole() throws Exception {
      mockMvc
          .perform(get("/api/test"))
          .andExpect(status().isNotFound()); // 404 because endpoint doesn't exist, but auth passed
    }

    @Test
    @WithMockJwtUser(roles = {"other-role"})
    void shouldDenyAccessWithoutRequiredRole() throws Exception {
      mockMvc.perform(get("/api/test")).andExpect(status().isForbidden());
    }
  }

  /** Test configuration for security tests */
  @TestConfiguration
  static class TestSecurityConfig {

    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
      return http.csrf(csrf -> csrf.disable())
          .cors(
              cors ->
                  cors.configurationSource(
                      request -> {
                        CorsConfiguration config = new CorsConfiguration();
                        config.addAllowedOrigin("http://localhost:3000");
                        config.setAllowedMethods(
                            Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                        config.addAllowedHeader("*");
                        return config;
                      }))
          .authorizeHttpRequests(authorize -> authorize.anyRequest().hasAuthority("studyconnect"))
          .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
          .build();
    }

    @Bean
    @Primary
    public JwtDecoder testJwtDecoder() {
      return jwt -> {
        // Simple mock implementation for tests
        return JwtTestUtil.createStudyConnectUserToken("testuser");
      };
    }

    @Bean
    public MockMvc mockMvc(WebApplicationContext context) {
      return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
  }

  /** Custom annotation for mocking JWT authentication in tests */
  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @WithSecurityContext(factory = WithMockJwtUserSecurityContextFactory.class)
  public @interface WithMockJwtUser {
    String username() default "testuser";

    String[] roles() default {"studyconnect"};
  }

  /** Security context factory for JWT mock user */
  static class WithMockJwtUserSecurityContextFactory
      implements WithSecurityContextFactory<WithMockJwtUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockJwtUser annotation) {
      Jwt jwt =
          JwtTestUtil.createJwtToken(Arrays.asList(annotation.roles()), annotation.username());

      JwtAuthenticationToken authentication =
          new JwtAuthenticationToken(
              jwt,
              Arrays.stream(annotation.roles())
                  .map(SimpleGrantedAuthority::new)
                  .collect(Collectors.toList()));

      SecurityContext context = SecurityContextHolder.createEmptyContext();
      context.setAuthentication(authentication);
      return context;
    }
  }
}
