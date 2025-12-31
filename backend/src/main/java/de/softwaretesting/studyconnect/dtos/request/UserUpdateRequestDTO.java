package de.softwaretesting.studyconnect.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import lombok.Value;

/** Data Transfer Object for updating an existing user. */
@Value
public class UserUpdateRequestDTO implements Serializable {

  @NotBlank(message = "Email is required")
  @Email(message = "Please provide a valid email address")
  private String email;

  @NotBlank(message = "Firstname is required")
  @Size(max = 100, message = "Firstname must not exceed 100 characters")
  private String firstname;

  @NotBlank(message = "Lastname is required")
  @Size(max = 100, message = "Lastname must not exceed 100 characters")
  private String lastname;
}
