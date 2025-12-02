package de.softwaretesting.studyconnect.controller;

import de.softwaretesting.studyconnect.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import de.softwaretesting.studyconnect.dtos.request.UserRequestDTO;
import de.softwaretesting.studyconnect.dtos.response.UserResponseDTO;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class UserController {

    UserService UserService;

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable("userId") Long userId) {
        return UserService.getUserWithId(userId);
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable("userId") Long userId, @Valid @RequestBody UserRequestDTO userRequestDTO) {
        return UserService.updateUserWithId(userId, userRequestDTO);
    }
}
