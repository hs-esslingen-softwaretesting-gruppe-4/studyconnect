package de.softwaretesting.studyconnect.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
public class UserCreateRequestDTO {
    @Size(max = 100, message = "Surname must not exceed 100 characters")
    String surname;

    @Size(max = 100, message = "Lastname must not exceed 100 characters")
    String lastname;

    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Pattern(regexp = ".*[A-Z].*", message = "Password must contain at least one uppercase letter")
    @Pattern(regexp = ".*[a-z].*", message = "Password must contain at least one lowercase letter")
    @Pattern(regexp = ".*[0-9].*", message = "Password must contain at least one digit")
    @Pattern(regexp = ".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*", message = "Password must contain at least one special character")
    String password;

    @Email(message = "Please provide a valid email address")
    String email;
}
