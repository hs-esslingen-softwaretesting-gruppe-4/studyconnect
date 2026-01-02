package de.softwaretesting.studyconnect.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
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
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Security configuration for production profile. - Configures JWT-based authentication and
 * authorization. - Sets up CORS with allowed origins from environment properties. - Disables CSRF
 * protection for stateless REST APIs. - Defines password encoding using BCrypt.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Profile("!dev")
public class SecurityConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConfig.class);

  private final Environment env;

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http, Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter)
      throws Exception {
    http.csrf(csrf -> csrf.disable())
        // Keycloak's silent SSO check loads /assets/silent-check-sso.html in a hidden iframe.
        // Spring Security defaults to X-Frame-Options: DENY, which breaks this flow.
        .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
        .cors(Customizer.withDefaults())
        .authorizeHttpRequests(
            authorize -> {
              final String requiredAuthority =
                  env.getProperty("required.keycloak.role", "studyconnect");
              final String allowedOrigin =
                  Optional.ofNullable(env.getProperty("allowed.origin"))
                      .orElse("http://localhost:4200");

              // Public access to SPA assets and routes (served by Spring Boot static resources).
              // Keep API + actuator protected (except health), everything else is a frontend route.
              final RequestMatcher publicPaths =
                  request -> {
                    final String path = request.getRequestURI();
                    return path != null
                        && !path.startsWith("/api")
                        && !path.startsWith("/actuator");
                  };

              authorize
                  // Let browsers perform CORS preflight without authentication
                  .requestMatchers(HttpMethod.OPTIONS, "/**")
                  .permitAll()
                  // Docker healthcheck relies on this endpoint
                  .requestMatchers("/actuator/health/**")
                  .permitAll()
                  // Frontend (index.html, assets, SPA routes)
                  .requestMatchers(publicPaths)
                  .permitAll()
                  // Allow POST /api/users only when the Origin header matches allowed.origin
                  .requestMatchers(HttpMethod.POST, "/api/users")
                  .access(
                      (authentication, requestContext) -> {
                        String origin = requestContext.getRequest().getHeader(HttpHeaders.ORIGIN);
                        boolean allowed = origin != null && origin.equals(allowedOrigin);
                        return new AuthorizationDecision(allowed);
                      })
                  // All API requests require the configured authority
                  .requestMatchers("/api/**")
                  .hasAuthority(requiredAuthority)
                  // Keep actuator protected in general (except health above)
                  .requestMatchers("/actuator/**")
                  .hasAuthority(requiredAuthority)
                  .anyRequest()
                  .denyAll();
            });

    // Only configure OAuth2 if JwtDecoder is available
    try {
      if (jwtDecoder() != null) {
        http.oauth2ResourceServer(
            oauth2 ->
                oauth2.jwt(
                    jwtDecoder ->
                        jwtDecoder.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        LOGGER.info("OAuth2 JWT authentication enabled for production");
      } else {
        LOGGER.warn("OAuth2 JWT authentication disabled - issuer unreachable or not configured");
      }
    } catch (Exception e) {
      LOGGER.warn(
          "OAuth2 JWT authentication disabled due to configuration error: {}", e.getMessage());
    }

    return http.build();
  }

  @Bean
  UrlBasedCorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(
        Optional.ofNullable(env.getProperty("allowed.origin"))
            .map(origins -> Arrays.asList(origins.split(",")))
            .orElse(List.of("http://localhost:4200")));
    configuration.setAllowedMethods(
        Arrays.asList("GET", "POST", "DELETE", "OPTIONS", "PUT", "PATCH"));
    configuration.setAllowedHeaders(
        Arrays.asList(
            HttpHeaders.ORIGIN,
            HttpHeaders.CONTENT_TYPE,
            HttpHeaders.ACCEPT,
            HttpHeaders.AUTHORIZATION));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
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
      LOGGER.warn("Issuer URI not configured. OAuth2 will be disabled.");
      return null;
    }

    int maxRetries = 3;
    int retryCount = 0;

    while (retryCount < maxRetries) {
      try {
        LOGGER.info(
            "Attempting to configure JwtDecoder with issuer: {} (attempt {}/{})",
            issuerUri,
            retryCount + 1,
            maxRetries);
        JwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuerUri);
        LOGGER.info("Successfully configured JwtDecoder for issuer: {}", issuerUri);
        return decoder;
      } catch (Exception e) {
        retryCount++;
        if (retryCount < maxRetries) {
          LOGGER.warn(
              "Failed to configure JwtDecoder (attempt {}/{}). Retrying in 2 seconds. Error: {}",
              retryCount,
              maxRetries,
              e.getMessage());
          try {
            Thread.sleep(2000); // Wait 2 seconds before retrying
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            LOGGER.warn("JwtDecoder initialization interrupted");
            return null;
          }
        } else {
          LOGGER.warn(
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

  /**
   * Defines the PasswordEncoder bean using BCrypt hashing algorithm.
   *
   * @return PasswordEncoder instance
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  // Converter to extract authorities from JWT
  interface AuthoritiesConverter
      extends Converter<Map<String, Object>, Collection<GrantedAuthority>> {}

  /**
   * Converts Keycloak realm roles from JWT claims into GrantedAuthority collection.
   *
   * @return AuthoritiesConverter instance
   */
  @Bean
  AuthoritiesConverter realmRolesAuthoritiesConverter() {
    return claims -> {
      @SuppressWarnings("unchecked")
      var realmAccess = Optional.ofNullable((Map<String, Object>) claims.get("realm_access"));
      @SuppressWarnings("unchecked")
      var roles = realmAccess.flatMap(map -> Optional.ofNullable((List<String>) map.get("roles")));
      return roles
          .map(List::stream)
          .orElse(Stream.empty())
          .map(SimpleGrantedAuthority::new)
          .map(GrantedAuthority.class::cast)
          .toList();
    };
  }

  /**
   * Configures JwtAuthenticationConverter to use custom AuthoritiesConverter.
   *
   * @param authoritiesConverter Converter to extract authorities from JWT
   * @return JwtAuthenticationConverter instance
   */
  @Bean
  JwtAuthenticationConverter authenticationConverter(AuthoritiesConverter authoritiesConverter) {
    var authenticationConverter = new JwtAuthenticationConverter();
    authenticationConverter.setJwtGrantedAuthoritiesConverter(
        jwt -> authoritiesConverter.convert(jwt.getClaims()));
    return authenticationConverter;
  }
}
