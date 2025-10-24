package de.softwaretesting.studyconnect.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

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
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

@Configuration
@EnableWebSecurity
@Profile("prod")
public class SecurityConfig {

    @Autowired
    private Environment env;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(request -> {
                CorsConfiguration config = new CorsConfiguration();
                config.addAllowedOrigin(env.getProperty("allowed.origin"));
                config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                config.addAllowedHeader("*");
                return config;
            }))
            .authorizeHttpRequests(authorize -> {
                final String requiredAuthority = env.getProperty("required.keycloak.role", "studyconnect");
                authorize
                    .anyRequest().hasAuthority(requiredAuthority);
            })
            .httpBasic(Customizer.withDefaults()) // Enable HTTP Basic authentication
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwtDecoder -> jwtDecoder
                    .jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }

    /*@Bean
    // ToDo: Uncomment once Keycloak is set up and the issuer-uri is set in the .env-file
    public JwtDecoder jwtDecoder() {
        return JwtDecoders.fromIssuerLocation(env.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri"));
    }*/

    // Password encoder bean
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Converter to extract authorities from JWT
    interface AuthoritiesConverter extends Converter<Map<String, Object>, Collection<GrantedAuthority>> {
    }

    /*
     * Extracts roles from the "realm_access" claim in the JWT and converts them to GrantedAuthority objects.
     * This is used to enforce authorization rules based on the user's roles.
     */
    @Bean
    AuthoritiesConverter realmRolesAuthoritiesConverter() {
        return claims -> {
            var realmAccess = Optional.ofNullable((Map<String, Object>) claims.get("realm_access"));
            var roles = realmAccess.flatMap(map -> Optional.ofNullable((List<String>) map.get("roles")));
            return roles.map(List::stream)
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
        authenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> authoritiesConverter.convert(jwt.getClaims()));
        return authenticationConverter;
    }

}