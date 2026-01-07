package de.softwaretesting.studyconnect.load;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

public class ConstantLoadSimulation extends Simulation {

  // 1. HTTP Protokol definition
  HttpProtocolBuilder httpProtocol =
      http.baseUrl("http://localhost:8080")
          .acceptHeader("application/json")
          .contentTypeHeader("application/json");

  // 2. Scenario: We request all public groups
  // this endpoint triggers groupService.getAllPublicGroups() -> DB load!
  ScenarioBuilder scn =
      scenario("Constant Load: Get All Groups")
          .exec(
              http("GET /api/groups")
                  .get("/api/groups")
                  .check(status().is(200))); // We check, if the API respose with OK

  {
    // 3. load-Profile (Setup)
    // Here we set the constant load as required in the task.
    setUp(
            scn.injectOpen(
                // 150 users per second, who request simultaneously
                // This over a period of 300 seconds (5 minutes)
                constantUsersPerSec(150).during(300)))
        .protocols(httpProtocol);
  }
}
