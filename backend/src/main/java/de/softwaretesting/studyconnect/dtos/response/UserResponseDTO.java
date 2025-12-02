package de.softwaretesting.studyconnect.dtos.response;

import java.io.Serializable;

import lombok.Value;

@Value
public class UserResponseDTO implements Serializable{
        Long id;
        String email;
        String lastname;
        String surname;
    }
