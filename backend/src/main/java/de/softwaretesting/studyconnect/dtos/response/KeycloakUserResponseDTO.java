package de.softwaretesting.studyconnect.dtos.response;

import java.util.List;
import java.util.Map;

import lombok.Value;

@Value
public class KeycloakUserResponseDTO {

    private String keycloakUUID;
    private Long createdTimestamp;
    private String username;
    private Boolean enabled;
    private Boolean emailVerified;
    private String firstName;
    private String lastName;
    private String email;
    private Map<String, List<String>> attributes;
    private List<String> realmRoles;
    private Map<String, List<String>> clientRoles;
}
