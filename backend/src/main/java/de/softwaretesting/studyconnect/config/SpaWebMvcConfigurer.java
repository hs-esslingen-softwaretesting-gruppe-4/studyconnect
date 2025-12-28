package de.softwaretesting.studyconnect.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration to support Single Page Application (SPA) routing.
 *
 * <p>When a user refreshes on an Angular route (e.g. {@code /groups}), the server must still return
 * {@code index.html} so the client-side router can handle navigation.
 *
 * <p>Spring Boot 3+ uses {@code PathPatternParser} by default, which does not allow patterns like
 * {@code /**&#47;{path}}. We therefore register a small set of "no dot" route patterns (1-4 path
 * segments) to avoid hijacking static resources like {@code /assets/logo.png}.
 */
@Configuration
public class SpaWebMvcConfigurer implements WebMvcConfigurer {

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    // Exclude backend endpoints and actuator endpoints from SPA forwarding.
    final String topLevelRoute = "/{route:^(?!api$|actuator$)[^\\.]*$}";

    registry.addViewController(topLevelRoute).setViewName("forward:/index.html");
    registry
        .addViewController(topLevelRoute + "/{route2:[^\\.]*}")
        .setViewName("forward:/index.html");
    registry
        .addViewController(topLevelRoute + "/{route2:[^\\.]*}/{route3:[^\\.]*}")
        .setViewName("forward:/index.html");
    registry
        .addViewController(topLevelRoute + "/{route2:[^\\.]*}/{route3:[^\\.]*}/{route4:[^\\.]*}")
        .setViewName("forward:/index.html");
  }
}
