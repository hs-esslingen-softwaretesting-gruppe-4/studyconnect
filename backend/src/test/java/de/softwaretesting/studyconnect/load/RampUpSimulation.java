package de.softwaretesting.studyconnect.load;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

public class RampUpSimulation extends Simulation {

  // 1. HTTP Protocol configuration
  HttpProtocolBuilder httpProtocol =
      http.baseUrl("http://localhost:8080")
          .acceptHeader("application/json")
          .contentTypeHeader("application/json");

  // 2. Scenario: Database query of groups
  ScenarioBuilder scn =
      scenario("Ramp-Up Test: Group API")
          .exec(http("GET /api/groups").get("/api/groups").check(status().is(200)));

  {
    // 3. Load-Profile: Ramp-Up
    setUp(
            scn.injectOpen(
                // Starts at 1 User/Sec and increases linearly to 1000 Users/Sec
                // over a period of 60 seconds (1 minute)
                rampUsersPerSec(1).to(1000).during(60)))
        .protocols(httpProtocol);
  }
}
