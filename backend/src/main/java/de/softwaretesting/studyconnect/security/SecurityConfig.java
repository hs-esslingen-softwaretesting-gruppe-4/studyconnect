package de.softwaretesting.studyconnect.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

/**
 * Security configuration for production profile. - Configures JWT-based authentication and
 * authorization. - Sets up CORS with allowed origins from environment properties. - Disables CSRF
 * protection for stateless REST APIs. - Defines password encoding using BCrypt.
 */
@Configuration
@EnableWebSecurity
@Profile("prod")
public class SecurityConfig {

  private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

  @Autowired private Environment env;

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http, Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter)
      throws Exception {
    http.csrf(csrf -> csrf.disable())
        .cors(
            cors ->
                cors.configurationSource(
                    request -> {
                      CorsConfiguration config = new CorsConfiguration();
                      config.addAllowedOrigin(env.getProperty("allowed.origin"));
                      config.setAllowedMethods(
                          Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                      config.addAllowedHeader("*");
                      return config;
                    }))
        .authorizeHttpRequests(
            authorize -> {
              final String requiredAuthority =
                  env.getProperty("required.keycloak.role", "studyconnect");
              authorize.anyRequest().hasAuthority(requiredAuthority);
            })
        .httpBasic(Customizer.withDefaults()); // Enable HTTP Basic authentication

    // Only configure OAuth2 if JwtDecoder is available
    try {
      if (jwtDecoder() != null) {
        http.oauth2ResourceServer(
            oauth2 ->
                oauth2.jwt(
                    jwtDecoder ->
                        jwtDecoder.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        logger.info("OAuth2 JWT authentication enabled for production");
      } else {
        logger.warn("OAuth2 JWT authentication disabled - issuer unreachable or not configured");
      }
    } catch (Exception e) {
      logger.warn(
          "OAuth2 JWT authentication disabled due to configuration error: {}", e.getMessage());
    }

    return http.build();
  }

  @Bean
  /**
   * Configures the JwtDecoder bean using issuer URI from environment properties. Attempts to
   * initialize with retries and graceful fallback if issuer is unreachable at startup.
   *
   * @return JwtDecoder instance, or null if issuer is unreachable after retries
   */
  public JwtDecoder jwtDecoder() {
    String issuerUri = env.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri");
    if (issuerUri == null || issuerUri.isBlank()) {
      logger.warn("Issuer URI not configured. OAuth2 will be disabled.");
      return null;
    }

    int maxRetries = 3;
    int retryCount = 0;

    while (retryCount < maxRetries) {
      try {
        logger.info(
            "Attempting to configure JwtDecoder with issuer: {} (attempt {}/{})",
            issuerUri,
            retryCount + 1,
            maxRetries);
        JwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuerUri);
        logger.info("Successfully configured JwtDecoder for issuer: {}", issuerUri);
        return decoder;
      } catch (Exception e) {
        retryCount++;
        if (retryCount < maxRetries) {
          logger.warn(
              "Failed to configure JwtDecoder (attempt {}/{}). Retrying in 2 seconds. Error: {}",
              retryCount,
              maxRetries,
              e.getMessage());
          try {
            Thread.sleep(2000); // Wait 2 seconds before retrying
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            logger.warn("JwtDecoder initialization interrupted");
            return null;
          }
        } else {
          logger.warn(
              "Failed to configure JwtDecoder for issuer '{}' after {} attempts. OAuth2 "
                  + "authentication will be disabled. Error: {}",
              issuerUri,
              maxRetries,
              e.getMessage());
          return null;
        }
      }
    }

    return null;
  }

  // Password encoder bean
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  // Converter to extract authorities from JWT
  interface AuthoritiesConverter
      extends Converter<Map<String, Object>, Collection<GrantedAuthority>> {}

  /*
   * Extracts roles from the "realm_access" claim in the JWT and converts them to GrantedAuthority objects.
   * This is used to enforce authorization rules based on the user's roles.
   */
  @Bean
  AuthoritiesConverter realmRolesAuthoritiesConverter() {
    return claims -> {
      var realmAccess = Optional.ofNullable((Map<String, Object>) claims.get("realm_access"));
      var roles = realmAccess.flatMap(map -> Optional.ofNullable((List<String>) map.get("roles")));
      return roles
          .map(List::stream)
          .orElse(Stream.empty())
          .map(SimpleGrantedAuthority::new)
          .map(GrantedAuthority.class::cast)
          .toList();
    };
  }

  /*
   * Configures a JwtAuthenticationConverter that uses the AuthoritiesConverter to extract authorities from the JWT.
   */
  @Bean
  JwtAuthenticationConverter authenticationConverter(AuthoritiesConverter authoritiesConverter) {
    var authenticationConverter = new JwtAuthenticationConverter();
    authenticationConverter.setJwtGrantedAuthoritiesConverter(
        jwt -> authoritiesConverter.convert(jwt.getClaims()));
    return authenticationConverter;
  }
}
