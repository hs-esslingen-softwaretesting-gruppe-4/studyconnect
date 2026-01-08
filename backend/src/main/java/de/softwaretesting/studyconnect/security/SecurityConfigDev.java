package de.softwaretesting.studyconnect.security;

import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Profile("dev")
public class SecurityConfigDev {

  private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConfigDev.class);

  private final Environment env;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        // Keycloak's silent SSO check loads /assets/silent-check-sso.html in a hidden iframe.
        // Spring Security defaults to X-Frame-Options: DENY, which breaks this flow.
        .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
        .cors(
            cors ->
                cors.configurationSource(
                    request -> {
                      CorsConfiguration config = new CorsConfiguration();
                      config.addAllowedOrigin(env.getProperty("allowed.origin"));
                      config.setAllowedMethods(
                          Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                      config.addAllowedHeader("*");
                      return config;
                    }))
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers(HttpMethod.GET, "/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.PUT, "/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.DELETE, "/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.PATCH, "/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated());

    // Only configure OAuth2 if JwtDecoder is available (issuer is reachable)
    try {
      if (jwtDecoder() != null) {
        http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        LOGGER.info("OAuth2 JWT authentication enabled");
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
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  /**
   * Configures the JwtDecoder bean using issuer URI from environment properties. In dev mode, if
   * the issuer is unreachable, logs a warning and returns a non-functional decoder (OAuth2
   * disabled).
   *
   * @return JwtDecoder instance, or null if issuer is unreachable (OAuth2 will be bypassed in dev)
   */
  public JwtDecoder jwtDecoder() {
    String issuerUri = env.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri");
    if (issuerUri == null || issuerUri.isBlank()) {
      LOGGER.warn("Issuer URI not configured. OAuth2 will be disabled.");
      return null;
    }

    try {
      LOGGER.info("Attempting to configure JwtDecoder with issuer: {}", issuerUri);
      return JwtDecoders.fromIssuerLocation(issuerUri);
    } catch (Exception e) {
      LOGGER.warn(
          "Failed to configure JwtDecoder for issuer '{}'. OAuth2 authentication will be "
              + "disabled in dev mode. Error: {}",
          issuerUri,
          e.getMessage());
      return null;
    }
  }
}
