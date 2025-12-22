package de.softwaretesting.studyconnect.dtos.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

/** Data Transfer Object for Keycloak token responses. */
@Value
public class KeycloakTokenResponseDTO {

  @JsonProperty("access_token")
  private String accessToken;

  @JsonProperty("refresh_token")
  private String refreshToken;

  @JsonProperty("expires_in")
  private Long expiresIn;

  @JsonProperty("refresh_expires_in")
  private Long refreshExpiresIn;

  @JsonProperty("token_type")
  private String tokenType;
}
