package de.softwaretesting.studyconnect.services;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import de.softwaretesting.studyconnect.dtos.request.UserRequestDTO;
import de.softwaretesting.studyconnect.dtos.response.UserResponseDTO;
import de.softwaretesting.studyconnect.mappers.response.UserResponseMapper;
import de.softwaretesting.studyconnect.mappers.request.UserRequestMapper;
import de.softwaretesting.studyconnect.exceptions.NotFoundException;
import de.softwaretesting.studyconnect.models.User;
import de.softwaretesting.studyconnect.repositories.UserRepository;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserResponseMapper userResponseMapper;
    private final UserRequestMapper userRequestMapper;
    
    /**
     * Retrieves a user by ID and maps it to a UserResponseDTO.
     * 
     * @param userId the ID of the user to retrieve
     * @param taskRequestDTO the user request DTO containing user details
     * @return a ResponseEntity containing the user's response DTO
     * @throws NotFoundException if the user is not found
     */
    public ResponseEntity<UserResponseDTO> getUserWithId(Long userId) {
        // Map DTO to entity
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));

        // Return user
        User savedUser = userRepository.save(user);
        UserResponseDTO userResponseDTO = userResponseMapper.toDto(savedUser);
        return new ResponseEntity<>(userResponseDTO, HttpStatus.CREATED);
    }

    /**
     * Updates a user by ID with the provided UserRequestDTO.
     * 
     * @param userId the ID of the user to update
     * @param userRequestDTO the user request DTO containing updated user details
     * @return a ResponseEntity containing the updated user's response DTO
     * @throws NotFoundException if the user is not found
     */
    public ResponseEntity<UserResponseDTO> updateUserWithId(Long userId, UserRequestDTO userRequestDTO) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        User updatedUser = userRequestMapper.toEntity(userRequestDTO);
        updatedUser.setId(existingUser.getId());
        updatedUser.setEmail(existingUser.getEmail());
        updatedUser.setCreatedAt(existingUser.getCreatedAt());

        User savedUser = userRepository.save(updatedUser);
        UserResponseDTO userResponseDTO = userResponseMapper.toDto(savedUser);
        return ResponseEntity.ok(userResponseDTO);
    }
}
