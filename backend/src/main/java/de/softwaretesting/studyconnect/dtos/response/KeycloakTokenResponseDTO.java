package de.softwaretesting.studyconnect.dtos.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Value;

@Value
public class KeycloakTokenResponseDTO {

    @JsonProperty("access_token")
    public String accessToken;

    @JsonProperty("refresh_token")
    public String refreshToken;

    @JsonProperty("expires_in")
    public Long expiresIn;

    @JsonProperty("refresh_expires_in")
    public Long refreshExpiresIn;

    @JsonProperty("token_type")
    public String tokenType;
}
