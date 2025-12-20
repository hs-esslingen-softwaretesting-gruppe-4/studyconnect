package de.softwaretesting.studyconnect.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import de.softwaretesting.studyconnect.utils.JwtTestUtil;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/**
 * Unit tests for SecurityConfig to verify individual components and beans. Integration tests are
 * located in the integrations package.
 */
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

  @ParameterizedTest
  @ValueSource(strings = {"", "http://unreachable-server:8080/auth/realms/test"})
  void jwtDecoder_ShouldReturnNullWhenIssuerUriNotConfiguredOrUnreachable(String issuerUri) {
    // Given
    when(env.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri"))
        .thenReturn("http://unreachable-server:8080/auth/realms/test");

    // When
    JwtDecoder decoder = securityConfig.jwtDecoder();

    // Then
    assertThat(decoder).isNull();
  }
}
