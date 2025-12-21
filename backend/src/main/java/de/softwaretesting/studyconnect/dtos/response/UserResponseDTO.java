package de.softwaretesting.studyconnect.dtos.response;

import java.io.Serializable;
import lombok.Value;

/** Data Transfer Object for user responses. */
@Value
public class UserResponseDTO implements Serializable {
  private Long id;
  private String email;
  private String lastname;
  private String surname;
}
