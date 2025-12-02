package de.softwaretesting.studyconnect.dtos.request;

import java.io.Serializable;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
public class UserRequestDTO implements Serializable {

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "Surname is required")
    @Size(max = 100, message = "Title must not exceed 100 characters")
    private String surname;

    @NotBlank(message = "Lastname is required")
    @Size(max = 100, message = "Title must not exceed 100 characters")
    private String lastname;

}