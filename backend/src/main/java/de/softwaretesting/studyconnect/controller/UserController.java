package de.softwaretesting.studyconnect.controller;

import de.softwaretesting.studyconnect.dtos.request.UserCreateRequestDTO;
import de.softwaretesting.studyconnect.dtos.request.UserUpdateRequestDTO;
import de.softwaretesting.studyconnect.dtos.response.UserResponseDTO;
import de.softwaretesting.studyconnect.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

  private final UserService userService;

  /**
   * Retrieves a user by their ID.
   *
   * @param userId the id of the user
   * @return the user with the given id
   */
  @GetMapping("/{userId}")
  public ResponseEntity<UserResponseDTO> getUserById(@PathVariable("userId") Long userId) {
    return userService.getUserById(userId);
  }

  @PutMapping("/{userId}")
  public ResponseEntity<UserResponseDTO> updateUser(
      @PathVariable("userId") Long userId,
      @Valid @RequestBody UserUpdateRequestDTO userRequestDTO) {
    return userService.updateUserWithId(userId, userRequestDTO);
  }

  @PostMapping
  public ResponseEntity<UserResponseDTO> createUser(
      @Valid @RequestBody UserCreateRequestDTO userRequestDTO) {
    return userService.createUser(userRequestDTO);
  }
}
