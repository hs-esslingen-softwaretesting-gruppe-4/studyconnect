package de.softwaretesting.studyconnect.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Value;

/** Data Transfer Object for creating a new user. */
@Value
public class UserCreateRequestDTO {

  @NotBlank(message = "Firstname is required")
  @Size(max = 100, message = "Firstname must not exceed 100 characters")
  private String firstname;

  @NotBlank(message = "Lastname is required")
  @Size(max = 100, message = "Lastname must not exceed 100 characters")
  private String lastname;

  @Size(
      min = 8,
      max = 256,
      message = "Password must be at least 8 characters and at most 256 characters")
  @Pattern(regexp = ".*[A-Z].*", message = "Password must contain at least one uppercase letter")
  @Pattern(regexp = ".*[a-z].*", message = "Password must contain at least one lowercase letter")
  @Pattern(regexp = ".*\\d.*", message = "Password must contain at least one digit")
  @Pattern(
      regexp = ".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*",
      message = "Password must contain at least one special character")
  @NotBlank(message = "Password is required")
  private String password;

  @NotBlank(message = "Email is required")
  @Email(message = "Please provide a valid email address")
  private String email;
}
